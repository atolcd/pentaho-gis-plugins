package org.geotools.shapefile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.io.EndianDataInputStream;
import com.vividsolutions.jump.io.EndianDataOutputStream;

/**
 * This class represents an ESRI Shape file.
 * <p>
 * You construct it with a file name, and later you can read the file's
 * properties, i.e. Sizes, Types, and the data itself.
 * <p>
 * Copyright 1998 by James Macgill.
 * <p>
 *
 * Version 1.0beta1.1 (added construct with inputstream) 1.0beta1.2 (made Shape
 * type constants public 18/Aug/98)
 *
 * This class supports the Shape file as set out in :-<br>
 * <a href="http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf"><b>
 * "ESRI(r) Shapefile - A Technical Description"</b><br>
 * <i>'An ESRI White Paper . May 1997'</i></a>
 * <p>
 *
 * This code is coverd by the LGPL.
 *
 * <a href="mailto:j.macgill@geog.leeds.ac.uk">Mail the Author</a>
 */

// TODO: Replace system.out by log messages
public class Shapefile {

    static final int SHAPEFILE_ID = 9994;
    static final int VERSION = 1000;

    public static final int NULL = 0;
    public static final int POINT = 1;
    public static final int POINTZ = 11;
    public static final int POINTM = 21;
    public static final int ARC = 3;
    public static final int ARCM = 23;
    public static final int ARCZ = 13;
    public static final int POLYGON = 5;
    public static final int POLYGONM = 25;
    public static final int POLYGONZ = 15;
    public static final int MULTIPOINT = 8;
    public static final int MULTIPOINTM = 28;
    public static final int MULTIPOINTZ = 18;
    public static final int MULTIPATCH = 31;
    public static final int UNDEFINED = -1;
    // Types 2,4,6,7 and 9 were undefined at time or writeing

    // private URL baseURL;
    private String fileName;
    private InputStream myInputStream;
    private int errors;

    /**
     * Creates and initialises a shapefile from a url
     * 
     * @param url
     *            The url of the shapefile
     */
    // public Shapefile(URL url) {
    public Shapefile(String fileName) {
        // baseURL=url;
        this.fileName = fileName;
        myInputStream = null;
        try {
            // URLConnection uc = baseURL.openConnection();
            // a 16 kb buffer may be up to 20% faster than the default 2 kb
            // buffer
            // myInputStream = new BufferedInputStream(uc.getInputStream(),
            // 16*1024);
            myInputStream = new BufferedInputStream(new FileInputStream(this.fileName), 16 * 1024);
        } catch (Exception e) {
        }
    }

    public Shapefile(InputStream IS) {
        myInputStream = IS;
    }

    public void close() {
        try {
            myInputStream.close();
        } catch (IOException ex) {
        }
    }

    private EndianDataInputStream getInputStream() throws IOException {
        if (myInputStream == null) {
            // throw new IOException("Could make a connection to the URL: " +
            // baseURL);
            throw new IOException("Could make a connection to the file : " + fileName);
        }
        return new EndianDataInputStream(myInputStream);
    }

    private EndianDataOutputStream getOutputStream() throws IOException {
        // BufferedOutputStream in = new BufferedOutputStream(new
        // FileOutputStream(baseURL.getFile()));
        BufferedOutputStream in = new BufferedOutputStream(new FileOutputStream(fileName));
        return new EndianDataOutputStream(in);
    }

    /**
     * Initialises a shapefile from disk. Use Shapefile(String) if you don't
     * want to use LEDataInputStream directly (recommended)
     * 
     * @param geometryFactory
     *            the geometry factory to use to read the shapes
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GeometryCollection read(GeometryFactory geometryFactory) throws IOException, ShapefileException, Exception {

        EndianDataInputStream file = getInputStream();
        // if(file==null) throw new
        // IOException("Failed connection or no content for " + baseURL);
        if (file == null)
            throw new IOException("Failed connection or no content for " + fileName);

        ShapefileHeader mainHeader = new ShapefileHeader(file);
        if (mainHeader.getVersion() < VERSION) {
            System.err.println("Sf-->Warning, Shapefile format (" + mainHeader.getVersion() + ") older that supported (" + VERSION + "), attempting to read anyway");
        }
        if (mainHeader.getVersion() > VERSION) {
            System.err.println("Sf-->Warning, Shapefile format (" + mainHeader.getVersion() + ") newer that supported (" + VERSION + "), attempting to read anyway");
        }

        Geometry body;
        ArrayList list = new ArrayList();
        int type = mainHeader.getShapeType();
        ShapeHandler handler = getShapeHandler(type);
        if (handler == null)
            throw new ShapeTypeNotSupportedException("Unsuported shape type:" + type);

        int recordNumber = 0;
        int contentLength = 0;
        errors = 0;
        try {
            while (true) {
                recordNumber = file.readIntBE();
                contentLength = file.readIntBE();
                try {
                    body = handler.read(file, geometryFactory, contentLength);
                    list.add(body);
                    if (body.getUserData() != null)
                        errors++;
                    // System.out.println("Done record: " + recordNumber);
                } catch (IllegalArgumentException r2d2) {
                    System.err.println("Error processing record " + recordNumber + " : " + r2d2.getMessage());
                    System.err.println("   an empty Geometry has been returned");
                    r2d2.printStackTrace();
                    list.add(handler.getEmptyGeometry(geometryFactory));
                    errors++;
                } catch (Exception c3p0) {
                    System.err.println("Error processing record " + recordNumber + " : " + c3p0.getMessage());
                    System.err.println("   an empty Geometry has been returned");
                    c3p0.printStackTrace();
                    list.add(handler.getEmptyGeometry(geometryFactory));
                    errors++;
                }
                // System.out.println("processing:" +recordNumber);
            }
        } catch (EOFException e) {
        }

        return geometryFactory.createGeometryCollection((Geometry[]) list.toArray(new Geometry[] {}));
    }

    /**
     * Get the number of errors found after a read.
     */
    public int getErrorNumber() {
        return errors;
    }

    /**
     * Saves a shapefile to an output stream.
     * 
     * @param geometries
     *            geometry collection to write
     * @param ShapeFileDimension
     *            shapefile dimension (2=x,y ; 3=x,y,m ; 4=x,y,z,m)
     */
    @SuppressWarnings("unused")
    public void write(GeometryCollection geometries, int ShapeFileDimension) throws IOException, Exception {
        EndianDataOutputStream file = getOutputStream();
        ShapefileHeader mainHeader = new ShapefileHeader(geometries, ShapeFileDimension);
        mainHeader.write(file);
        int pos = 50; // header length in WORDS

        int numShapes = geometries.getNumGeometries();
        Geometry body;
        ShapeHandler handler;

        if (geometries.getNumGeometries() == 0) {
            handler = new PointHandler(); // default
        } else {
            handler = Shapefile.getShapeHandler(geometries.getGeometryN(0), ShapeFileDimension);
        }

        for (int i = 0; i < numShapes; i++) {
            body = geometries.getGeometryN(i);
            file.writeIntBE(i + 1);
            file.writeIntBE(handler.getLength(body));
            pos += 4; // length of header in WORDS
            handler.write(body, file);
            pos += handler.getLength(body); // length of shape in WORDS
        }
        file.flush();
        file.close();
    }

    // ShapeFileDimension => 2=x,y ; 3=x,y,m ; 4=x,y,z,m
    /**
     * Saves a shapefile index (shx) to an output stream.
     * 
     * @param geometries
     *            geometry collection to write
     * @param file
     *            file to write to
     * @param ShapeFileDimension
     *            shapefile dimension (2=x,y ; 3=x,y,m ; 4=x,y,z,m)
     */
    public synchronized void writeIndex(GeometryCollection geometries, EndianDataOutputStream file, int ShapeFileDimension) throws IOException, Exception {
        Geometry geom;

        ShapeHandler handler;
        int nrecords = geometries.getNumGeometries();
        ShapefileHeader mainHeader = new ShapefileHeader(geometries, ShapeFileDimension);

        if (geometries.getNumGeometries() == 0) {
            handler = new PointHandler(); // default
        } else {
            handler = Shapefile.getShapeHandler(geometries.getGeometryN(0), ShapeFileDimension);
        }

        mainHeader.writeToIndex(file);
        int pos = 50;
        int len = 0;

        for (int i = 0; i < nrecords; i++) {
            geom = geometries.getGeometryN(i);
            len = handler.getLength(geom);
            file.writeIntBE(pos);
            file.writeIntBE(len);
            pos = pos + len + 4;
        }
        file.flush();
        file.close();
    }

    /**
     * Returns a string describing the shape type.
     * 
     * @param index
     *            An int coresponding to the shape type to be described
     * @return A string describing the shape type
     */
    public static String getShapeTypeDescription(int index) {
        switch (index) {
        case (NULL):
            return ("Null Shape");
        case (POINT):
            return ("Point");
        case (POINTZ):
            return ("PointZ");
        case (POINTM):
            return ("PointM");
        case (ARC):
            return ("PolyLine");
        case (ARCM):
            return ("PolyLineM");
        case (ARCZ):
            return ("PolyLineZ");
        case (POLYGON):
            return ("Polygon");
        case (POLYGONM):
            return ("PolygonM");
        case (POLYGONZ):
            return ("PolygonZ");
        case (MULTIPOINT):
            return ("MultiPoint");
        case (MULTIPOINTM):
            return ("MultiPointM");
        case (MULTIPOINTZ):
            return ("MultiPointZ");
        default:
            return ("Undefined");
        }
    }

    public static ShapeHandler getShapeHandler(Geometry geom, int ShapeFileDimension) throws Exception {
        return getShapeHandler(getShapeType(geom, ShapeFileDimension));
    }

    public static ShapeHandler getShapeHandler(int type) throws Exception {
        switch (type) {
        case Shapefile.NULL:
            return new NullShapeHandler();
        case Shapefile.POINT:
            return new PointHandler();
        case Shapefile.POINTZ:
            return new PointHandler(Shapefile.POINTZ);
        case Shapefile.POINTM:
            return new PointHandler(Shapefile.POINTM);
        case Shapefile.POLYGON:
            return new PolygonHandler();
        case Shapefile.POLYGONM:
            return new PolygonHandler(Shapefile.POLYGONM);
        case Shapefile.POLYGONZ:
            return new PolygonHandler(Shapefile.POLYGONZ);
        case Shapefile.ARC:
            return new MultiLineHandler();
        case Shapefile.ARCM:
            return new MultiLineHandler(Shapefile.ARCM);
        case Shapefile.ARCZ:
            return new MultiLineHandler(Shapefile.ARCZ);
        case Shapefile.MULTIPOINT:
            return new MultiPointHandler();
        case Shapefile.MULTIPOINTM:
            return new MultiPointHandler(Shapefile.MULTIPOINTM);
        case Shapefile.MULTIPOINTZ:
            return new MultiPointHandler(Shapefile.MULTIPOINTZ);
        }
        return null;
    }

    /**
     * Returns the Shape Type corresponding to geometry geom of dimension
     * ShapeFileDimension.
     * 
     * @param geom
     *            the geom
     * @param ShapeFileDimension
     *            the dimension of the geom (2=x,y ; 3=x,y,m ; 4=x,y,z,m)
     * @return A int representing the Shape Type
     */
    public static int getShapeType(Geometry geom, int ShapeFileDimension) throws ShapefileException {

        if ((ShapeFileDimension != 2) && (ShapeFileDimension != 3) && (ShapeFileDimension != 4)) {
            throw new ShapefileException("invalid ShapeFileDimension for getShapeType - expected 2,3,or 4 but got " + ShapeFileDimension + "  (2=x,y ; 3=x,y,m ; 4=x,y,z,m)");
        }

        if (geom instanceof Point) {
            switch (ShapeFileDimension) {
            case 2:
                return Shapefile.POINT;
            case 3:
                return Shapefile.POINTM;
            case 4:
                return Shapefile.POINTZ;
            }
        }

        if (geom instanceof MultiPoint) {
            switch (ShapeFileDimension) {
            case 2:
                return Shapefile.MULTIPOINT;
            case 3:
                return Shapefile.MULTIPOINTM;
            case 4:
                return Shapefile.MULTIPOINTZ;
            }
        }

        if ((geom instanceof Polygon) || (geom instanceof MultiPolygon)) {
            switch (ShapeFileDimension) {
            case 2:
                return Shapefile.POLYGON;
            case 3:
                return Shapefile.POLYGONM;
            case 4:
                return Shapefile.POLYGONZ;
            }
        }

        if ((geom instanceof LineString) || (geom instanceof MultiLineString)) {
            switch (ShapeFileDimension) {
            case 2:
                return Shapefile.ARC;
            case 3:
                return Shapefile.ARCM;
            case 4:
                return Shapefile.ARCZ;
            }
        }

        if ((geom instanceof GeometryCollection) && (geom.isEmpty())) {
            return Shapefile.NULL;
        }

        return Shapefile.UNDEFINED;
    }

    @SuppressWarnings("unused")
    public synchronized void readIndex(InputStream is) throws IOException {
        EndianDataInputStream file = null;
        try {
            BufferedInputStream in = new BufferedInputStream(is);
            file = new EndianDataInputStream(in);
        } catch (Exception e) {
            System.err.println(e);
        }
        ShapefileHeader head = new ShapefileHeader(file);
        int pos = 0, len = 0;
        file.close();
    }
}
