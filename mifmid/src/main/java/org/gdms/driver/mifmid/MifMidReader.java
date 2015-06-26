/**
 * The GDMS library (Generic Datasource Management System)
 * is a middleware dedicated to the management of various kinds of
 * data-sources such as spatial vectorial data or alphanumeric. Based
 * on the JTS library and conform to the OGC simple feature access
 * specifications, it provides a complete and robust API to manipulate
 * in a SQL way remote DBMS (PostgreSQL, H2...) or flat files (.shp,
 * .csv...).
 *
 * Gdms is distributed under GPL 3 license. It is produced by the "Atelier SIG"
 * team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR 2488.
 *
 * Copyright (C) 2007-2014 IRSTV FR CNRS 2488
 *
 * This file is part of Gdms.
 *
 * Gdms is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Gdms is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Gdms. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 *
 * or contact directly:
 * info@orbisgis.org
 */
package org.gdms.driver.mifmid;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.GeometricShapeFactory;

public final class MifMidReader {

    private static final DateFormat DATE_PARSER = new SimpleDateFormat("yyyyMMdd");
    private final File mifFile;
    private final File midFile;
    private final RandomAccessFile mifRaf;
    private final RandomAccessFile midRaf;
    private int nbFeatures = 0;
    private String version = "300";
    // private String charset = "WindowsLatin1";
    private String delimiter = "\t";
    private ArrayList<String> headerLines = new ArrayList<String>();
    private ArrayList<String> schemaLines = new ArrayList<String>();
    private GeometryFactory factory = new GeometryFactory();
    private List<Long> mifAdresses = null;
    private List<Long> midAdresses = null;
    private Pattern pCSV = Pattern.compile("  \\G            #fin du match prï¿½cï¿½dent                          \n"
            + "  (?:^|\\t)      #dï¿½but de ligne ou tabulation                    \n" + "  (?:            #champ entre guillemets                          \n"
            + "     \"                                                           \n" + "     ( (?>[^\"]*+) (?>\"\"[^\"]*+)*+ )                            \n"
            + "     \"                                                           \n" + "  |              # ou sans guillemet                              \n"
            + "     ([^\"\\t]*+)                                                 \n" + "  )                                                               \n", Pattern.COMMENTS);
    private Pattern pQuote = Pattern.compile("\"\"");
    private LinkedHashMap<String, String> columns = new LinkedHashMap<String, String>();
    private Charset charset;

    /**
     * Creates and initialises a MifMid object with the name of the Mif file to
     * read from ot to write to.
     *
     *
     * @param mifFile
     * @param storeSymbols
     * @param schema
     * @throws Exception
     */
    public MifMidReader(File mifFile, Charset charset) throws Exception {
        this.mifFile = mifFile;
        char[] midc = mifFile.getCanonicalPath().toCharArray();
        midc[midc.length - 1] = midc[midc.length - 1] == 'f' ? 'd' : 'D';
        midFile = new File(new String(midc));
        mifRaf = new RandomAccessFile(mifFile, "rw");
        midRaf = new RandomAccessFile(midFile, "rw");

        this.charset = charset;

        readMMFileProperties();
        populateMMFileFeatureSchema();

    }

    public void close() throws IOException {
        mifRaf.close();
        midRaf.close();
    }

    /**
     * Read global settings in the header of a Mif file, and return them as a
     * Properties object.
     *
     * @return a Properties object
     * @throws IOException
     * @throws FileNotFoundException
     */
    public void readMMFileProperties() throws IOException {
        String line;
        mifRaf.seek(0);
        while (null != (line = mifRaf.readLine())) {

            String lineU = line.toUpperCase().trim();
            if (lineU.startsWith("VERSION")) {
                headerLines.add(line);
                String[] versionP = line.split(" ");
                if (versionP.length > 1) {
                    version = versionP[1];
                }
            } else if (lineU.startsWith("CHARSET")) {
                headerLines.add(line);
                /*
                 * String[] charsetP = line.split(" "); if (charsetP.length > 1)
                 * { charset = charsetP[1]; }
                 */
            } else if (lineU.startsWith("DELIMITER")) {
                headerLines.add(line);
                StringTokenizer st = new StringTokenizer(line);
                st.nextToken();
                delimiter = st.nextToken().substring(1, 2);
                setDelimiter(delimiter);
            } else if (lineU.startsWith("UNIQUE")) {
                headerLines.add(line);

            } else if (lineU.startsWith("INDEX")) {
                headerLines.add(line);

            } else if (lineU.startsWith("COORDSYS")) {
                headerLines.add(line);
            } else if (lineU.startsWith("TRANSFORM")) {
                headerLines.add(line);

            } else if (lineU.startsWith("DATA")) {
                break;
            } else {
                return;
            }
        }
    }

    public Map<String, String> getColumns() {
        return columns;
    }

    public File getMifFile() {
        return mifFile;
    }

    public File getMidFile() {
        return midFile;
    }

    public String getVersion() {
        return version;
    }

    /*
     * public String getCharset() { return charset; }
     */

    public String getDelimiter() {
        return delimiter;
    }

    public int getFeatureNumber() {
        return nbFeatures;
    }

    private void setDelimiter(String delimiter) {
        // Regex for CSV file, taken from "Mastering Regular Expression"
        // O'Reilly - Jeffrey E.F. Friedl
        this.delimiter = delimiter;
        String sep = delimiter.equals("\t") ? "\\t" : delimiter;
        String regex = "  \\G                 #fin du match pr�c�dent                \n" + "  (?:^|" + sep + ")   #d�but de ligne ou virgule             \n"
                + "  (?:                 #champ entre guillements               \n" + "     \"                                                      \n"
                + "     ( (?>[^\"]*+) (?>\"\"[^\"]*+)*+ )                       \n" + "     \"                                                      \n"
                + "  |                   # ou sans guillemet                    \n" + "     ([^\"" + sep + "]*+)                                        \n"
                + "  )                                                          \n";
        pCSV = Pattern.compile(regex, Pattern.COMMENTS);
    }

    public void populateMMFileFeatureSchema() throws IOException, FileNotFoundException, Exception {
        String line;
        mifRaf.seek(0);
        while (null != (line = mifRaf.readLine())) {
            if (line.toUpperCase().startsWith("COLUMNS")) {
                schemaLines.add(line);
                StringTokenizer st = new StringTokenizer(line);
                if (st.hasMoreTokens()) {
                    st.nextToken();
                }
                int nbColumns = 0;
                if (st.hasMoreTokens()) {
                    nbColumns = Integer.parseInt(st.nextToken());
                }

                for (int i = 0; i < nbColumns; i++) {
                    line = mifRaf.readLine();
                    schemaLines.add(line);
                    st = new StringTokenizer(line, " \t\n\r\f(,)");
                    String name = st.nextToken();
                    String typeA = st.nextToken().toUpperCase();
                    if (typeA.startsWith("CHAR")) {
                        columns.put(name, "STRING");
                    } else if (typeA.startsWith("INTEGER")) {
                        columns.put(name, "INTEGER");
                    } else if (typeA.startsWith("SMALLINT")) {
                        columns.put(name, "INTEGER");
                    } else if (typeA.startsWith("DECIMAL")) {
                        columns.put(name, "DOUBLE");
                    } else if (typeA.startsWith("FLOAT")) {
                        columns.put(name, "DOUBLE");
                    } else if (typeA.startsWith("DATE")) {
                        columns.put(name, "DATE");
                    } else if (typeA.startsWith("LOGICAL")) {
                        columns.put(name, "BOOLEAN");
                    } else {
                        throw new Exception("Unknown attribute type : " + typeA);
                    }
                }

                break;
            } else if (line.toUpperCase().startsWith("DATA")) {
                schemaLines.add(line);
                break;
            }
        }
    }

    public Object[] getValues(int index) throws IOException, ParseException {
        Object[] values = new Object[columns.size()];
        long addressAttributes = midAdresses.get(index).longValue();
        String attribute = null;

        midRaf.seek(addressAttributes);
        String[] attributes = parseMidLine(midRaf.readLine());

        int i = 0;
        for (Entry<String, String> entry : columns.entrySet()) {

            String miType = entry.getValue();
            attribute = attributes[i];
            if (miType.equalsIgnoreCase("STRING")) {
                values[i] = String.valueOf(attributes[i]);
            } else if (miType.equalsIgnoreCase("INTEGER")) {
                values[i] = Long.valueOf(attributes[i]);
            } else if (miType.equalsIgnoreCase("DATE")) {

                if (attributes[i].equals("\"\"") || attributes[i].isEmpty()) {
                    values[i] = null;
                } else if (attributes[i].startsWith("\"")) {
                    values[i] = DATE_PARSER.parse(attribute.trim().substring(1, attribute.trim().length() - 1));
                } else {
                    values[i] = DATE_PARSER.parse(attribute.trim());
                }

            } else if (miType.equalsIgnoreCase("DOUBLE")) {
                values[i] = Double.valueOf(attributes[i]);
            } else if (miType.equalsIgnoreCase("BOOLEAN")) {
                values[i] = String.valueOf(attributes[i]);
            } else {
                values[i] = String.valueOf(attributes[i]);
            }

            i++;

        }
        return values;

    }

    public Geometry getGeometry(int index) throws IOException, Exception {

        Geometry geometry = null;
        long addressGeometry = mifAdresses.get(index).longValue();
        mifRaf.seek(addressGeometry);
        String mifGeometry = mifRaf.readLine();
        StringTokenizer st = new StringTokenizer(mifGeometry);
        String type = st.nextToken().toUpperCase();
        if (type.equals("NONE")) {
            geometry = factory.createGeometryCollection(new Geometry[0]);
        } else if (type.equals("POINT")) {
            double x = Double.parseDouble(st.nextToken());
            double y = Double.parseDouble(st.nextToken());
            geometry = factory.createPoint(new Coordinate(x, y, Double.NaN));
        } else if (type.equals("LINE")) {
            double x1 = Double.parseDouble(st.nextToken());
            double y1 = Double.parseDouble(st.nextToken());
            double x2 = Double.parseDouble(st.nextToken());
            double y2 = Double.parseDouble(st.nextToken());
            geometry = factory.createLineString(new Coordinate[] { new Coordinate(x1, y1, Double.NaN), new Coordinate(x2, y2, Double.NaN), });

        } else if (type.equals("PLINE")) {
            int numSections = 1;
            int numPoints = -1;
            if (st.hasMoreTokens()) {
                String secondToken = st.nextToken();
                if (secondToken.equalsIgnoreCase("MULTIPLE")) {
                    numSections = Integer.parseInt(st.nextToken());
                } else {
                    numPoints = Integer.parseInt(secondToken);
                }
            }

            LineString[] lines = new LineString[numSections];
            for (int i = 0; i < numSections; i++) {
                if (numPoints == -1 || i > 0) {
                    numPoints = Integer.parseInt(mifRaf.readLine().trim());
                }
                Coordinate[] coordinates = new Coordinate[numPoints];
                for (int j = 0; j < numPoints; j++) {
                    st = new StringTokenizer(mifRaf.readLine());
                    double x = Double.parseDouble(st.nextToken());
                    double y = Double.parseDouble(st.nextToken());
                    coordinates[j] = new Coordinate(x, y, Double.NaN);
                }
                lines[i] = factory.createLineString(coordinates);
            }
            if (numSections == 1) {
                geometry = lines[0];
            } else if (numSections > 1) {
                geometry = factory.createMultiLineString(lines);
            }
        } else if (type.equals("REGION")) {
            int numPolygons = 1;
            if (st.hasMoreTokens()) {
                numPolygons = Integer.parseInt(st.nextToken());
            }
            Polygon[] polygons = new Polygon[numPolygons];
            for (int i = 0; i < numPolygons; i++) {
                int numPoints = Integer.parseInt(mifRaf.readLine().trim());
                Coordinate[] coordinates = new Coordinate[numPoints];
                for (int j = 0; j < numPoints; j++) {
                    st = new StringTokenizer(mifRaf.readLine());
                    double x = Double.parseDouble(st.nextToken());
                    double y = Double.parseDouble(st.nextToken());
                    coordinates[j] = new Coordinate(x, y, Double.NaN);
                }
                CoordinateList cl = new CoordinateList(coordinates);
                cl.closeRing();
                coordinates = cl.toCoordinateArray();
                LinearRing ring = factory.createLinearRing(coordinates);
                polygons[i] = factory.createPolygon(ring);

            }
            if (polygons[0] == null) {
                geometry = null;
            } else {
                geometry = region2MultiPolygon(polygons);
            }

        } else if (type.equals("ARC")) {
            double x1 = Double.parseDouble(st.nextToken());
            double y1 = Double.parseDouble(st.nextToken());
            double x2 = Double.parseDouble(st.nextToken());
            double y2 = Double.parseDouble(st.nextToken());
            st = new StringTokenizer(mifRaf.readLine());
            double angleIni = Double.parseDouble(st.nextToken()) * Math.PI / 180;
            double angleFin = Double.parseDouble(st.nextToken()) * Math.PI / 180;
            double a = Math.max((x2 - x1), (x1 - x2));
            double b = Math.max((y2 - y1), (y1 - y2));
            double e = Math.sqrt(1 - ((b * b) / (a * a)));
            ArrayList<Coordinate> cc = new ArrayList<Coordinate>();
            double r = a * Math.sqrt((1 - (e * e)) / (1 - (e * e * Math.cos(angleIni) * Math.cos(angleIni))));
            Coordinate coord = new Coordinate((((x1 + x2) / 2) + r * Math.cos(angleIni)), (((y1 + y2) / 2) + r * Math.sin(angleIni)), Double.NaN);
            cc.add(coord);
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * (double) i / 12.0;
                if ((angleFin > angleIni && angle > angleIni && angle < angleFin) || (angleFin < angleIni && (angle >= angleIni || angle < angleFin))) {
                    coord = new Coordinate((((x1 + x2) / 2) + r * Math.cos(Math.PI * i / 12.0)), (((y1 + y2) / 2) + r * Math.sin(Math.PI * i / 12.0)), Double.NaN);
                    cc.add(coord);
                }
            }

            geometry = factory.createLineString(cc.toArray(new Coordinate[cc.size()]));
        } else if (type.equals("TEXT")) {
            String text = st.nextToken();
            st = new StringTokenizer(mifRaf.readLine());
            double x1 = Double.parseDouble(st.nextToken());
            double y1 = Double.parseDouble(st.nextToken());
            geometry = factory.createPoint(new Coordinate(x1, y1));
            geometry.setUserData(text);

        } else if (type.equals("RECT")) {
            double x1 = Double.parseDouble(st.nextToken());
            double y1 = Double.parseDouble(st.nextToken());
            double x2 = Double.parseDouble(st.nextToken());
            double y2 = Double.parseDouble(st.nextToken());
            GeometricShapeFactory gsf = new GeometricShapeFactory(new GeometryFactory());
            gsf.setCentre(new Coordinate((x1 + x2) / 2, (y1 + y2) / 2));
            gsf.setWidth(Math.abs(x2 - x1));
            gsf.setHeight(Math.abs(x2 - x1));
            geometry = gsf.createRectangle();

        } else if (type.equals("ROUNDRECT")) {
            double x1 = Double.parseDouble(st.nextToken());
            double y1 = Double.parseDouble(st.nextToken());
            double x2 = Double.parseDouble(st.nextToken());
            double y2 = Double.parseDouble(st.nextToken());
            GeometricShapeFactory gsf = new GeometricShapeFactory(new GeometryFactory());
            gsf.setCentre(new Coordinate((x1 + x2) / 2, (y1 + y2) / 2));
            gsf.setWidth(Math.abs(x2 - x1));
            gsf.setHeight(Math.abs(x2 - x1));
            geometry = gsf.createRectangle();

        } else if (type.equals("ELLIPSE")) {
            double x1 = Double.parseDouble(st.nextToken());
            double y1 = Double.parseDouble(st.nextToken());
            double x2 = Double.parseDouble(st.nextToken());
            double y2 = Double.parseDouble(st.nextToken());
            GeometricShapeFactory gsf = new GeometricShapeFactory(new GeometryFactory());
            gsf.setCentre(new Coordinate((x1 + x2) / 2, (y1 + y2) / 2));
            gsf.setWidth(Math.abs(x2 - x1));
            gsf.setHeight(Math.abs(x2 - x1));
            gsf.setNumPoints(32);
            geometry = gsf.createCircle();

        } // Multi Points are not handled
        else if (type.equals("MULTIPOINT")) {

            int numPoints = -1;
            if (st.hasMoreTokens()) {
                numPoints = Integer.parseInt(st.nextToken());
            }
            Point[] pp = new Point[numPoints];
            for (int i = 0; i < numPoints;) {
                // Other points to read on the same line
                if (st.hasMoreTokens()) {
                    double x = Double.parseDouble(st.nextToken());
                    double y = Double.parseDouble(st.nextToken());
                    pp[i++] = factory.createPoint(new Coordinate(x, y));
                } // Other points to read on the following line
                else if (i < numPoints) {
                    st = new StringTokenizer(mifRaf.readLine());
                } // No more point
                else {
                    break;
                }
            }
            geometry = factory.createMultiPoint(pp);

        } // Collections are not handled
        else if (type.equals("COLLECTION")) {
            throw new Exception("An error occured reading the object " + index + " of the mif file (byte " + addressGeometry + ")\n"
                    + "The parser for collection type objects has not been implemented");
        }

        return geometry;
    }

    private Geometry region2MultiPolygon(Polygon[] polygons) throws Exception {
        if (polygons == null || polygons.length == 0) {
            throw new Exception("Try to convert a null Region into a Polygon");
        }
        if (polygons[0].isEmpty()) {
            throw new Exception("First Region has been converted into an empty Polygon");
        }
        ArrayList<Polygon> finalPolys = new ArrayList<Polygon>();
        finalPolys.add(polygons[0]);
        for (int i = 1; i < polygons.length; i++) {
            if (polygons[i] == null) {
                continue;
            } // continue if the polygon has been made invalif by
              // the parser
            for (int p = 0; p < finalPolys.size(); p++) {
                Polygon currentPoly = finalPolys.get(p);
                if (currentPoly.contains(polygons[i])) {
                    LinearRing[] holes = new LinearRing[currentPoly.getNumInteriorRing() + 1];
                    for (int h = 0; h < holes.length - 1; h++) {
                        holes[h] = (LinearRing) currentPoly.getInteriorRingN(h);
                    }
                    holes[holes.length - 1] = (LinearRing) polygons[i].getExteriorRing();
                    finalPolys.set(p, new GeometryFactory().createPolygon((LinearRing) currentPoly.getExteriorRing(), holes));
                    polygons[i] = null;
                    break;
                }
            }
            if (polygons[i] != null) {
                finalPolys.add(polygons[i]);
            }
        }
        if (finalPolys.size() == 1) {
            return finalPolys.get(0);
        } else {
            return new GeometryFactory().createMultiPolygon(finalPolys.toArray(new Polygon[finalPolys.size()]));
        }
    }

    public String[] parseMidLine(String line) {

        line = new String(line.getBytes(charset));

        ArrayList<String> list = new ArrayList<String>();
        // String regex = "[\\\"].*?[\\\"]|[^"+sep+"\\\"]*";
        // Regex for CSV file, taken from "Mastering Regular Expression"
        // O'Reilly - Jeffrey E.F. Friedl
        /*
         * sep = sep=="\t"?"\\t":sep; String regex =
         * "  \\G            #fin du match prï¿½cï¿½dent                     \n"
         * + "  (?:^|" + sep +
         * ")        #dï¿½but de ligne ou virgule                       	\n"+
         * "  (?:            #champ entre guillements                         \n"
         * +
         * "     \"                                                           \n"
         * +
         * "     ( (?>[^\"]*+) (?>\"\"[^\"]*+)*+ )                            \n"
         * +
         * "     \"                                                           \n"
         * +
         * "  |              # ou sans guillemet                              \n"
         * +"     ([^\""+sep+
         * "]*+)                                                   \n"+
         * "  )                                                               \n"
         * ; System.out.println("sep = '" + sep + "'");
         * System.out.println(regex); Pattern pCSV = Pattern.compile(regex,
         * Pattern.COMMENTS); Pattern pQuote = Pattern.compile("\"\"");
         */
        Matcher mQuote = pQuote.matcher("");
        Matcher mCSV = pCSV.matcher("");
        mCSV.reset(line);

        while (mCSV.find()) {
            String token;
            String first = mCSV.group(2);
            // test si token contient des guillemets
            if (first != null) {
                token = first;
            } else {
                mQuote.reset(mCSV.group(1));
                token = mQuote.replaceAll("\"");

            }
            list.add(token);
        }
        return list.toArray(new String[list.size()]);
    }

    public int createIndexes() throws IOException, FileNotFoundException {
        if (mifAdresses != null || midAdresses != null) {
            nbFeatures = 0;
            return mifAdresses.size();
        }

        // mifAdresses = new ArrayList<Long>();
        // midAdresses = new ArrayList<Long>();

        mifAdresses = new ArrayList<Long>();
        midAdresses = new ArrayList<Long>();

        String line;
        long offsetMif = 0;
        mifRaf.seek(offsetMif);
        while (null != (line = mifRaf.readLine())) {
            String type = line.trim().toUpperCase();
            if (type.startsWith("DATA")) {
                offsetMif = mifRaf.getFilePointer();
                while (null != (line = mifRaf.readLine())) {
                    type = line.trim().toUpperCase();
                    if (type.startsWith("POINT") || type.startsWith("PLINE") || type.startsWith("REGION") || type.startsWith("LINE") || type.startsWith("NONE")
                            || type.startsWith("ARC") || type.startsWith("TEXT") || type.startsWith("RECT") || type.startsWith("ROUNDRECT") || type.startsWith("ELLIPSE")
                            || type.startsWith("MULTIPOINT") || type.startsWith("COLLECTION")) {
                        mifAdresses.add(Long.valueOf(offsetMif));
                    }
                    offsetMif = mifRaf.getFilePointer();

                }
            }
            offsetMif = mifRaf.getFilePointer();

        }
        long offsetMid = 0;
        midRaf.seek(offsetMid);
        while (null != (line = midRaf.readLine())) {
            midAdresses.add(Long.valueOf(offsetMid));
            offsetMid = midRaf.getFilePointer();
        }
        nbFeatures = mifAdresses.size();
        return nbFeatures;
    }

}
