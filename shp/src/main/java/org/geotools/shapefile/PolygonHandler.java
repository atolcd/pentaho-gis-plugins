package org.geotools.shapefile;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.algorithm.RobustCGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jump.io.EndianDataInputStream;
import com.vividsolutions.jump.io.EndianDataOutputStream;

/**
 * Wrapper for a Shapefile Polygon.
 */
@SuppressWarnings("deprecation")
public class PolygonHandler implements ShapeHandler {

    protected static CGAlgorithms cga = new RobustCGAlgorithms();

    int myShapeType;

    public PolygonHandler() {
        myShapeType = 5;
    }

    public PolygonHandler(int type) throws InvalidShapefileException {
        if ((type != 5) && (type != 15) && (type != 25)) {
            throw new InvalidShapefileException("PolygonHandler constructor - expected type to be 5, 15, or 25.");
        }
        myShapeType = type;
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "static-access", "unused" })
    public Geometry read(EndianDataInputStream file, GeometryFactory geometryFactory, int contentLength) throws IOException, InvalidShapefileException {

        int actualReadWords = 0; // actual number of 16 bits words read
        Geometry geom = null;

        int shapeType = file.readIntLE();
        actualReadWords += 2;

        if (shapeType == 0) {
            geom = geometryFactory.createMultiPolygon(new Polygon[0]); // null
                                                                       // shape
        }

        else if (shapeType != myShapeType) {
            throw new InvalidShapefileException("PolygonHandler.read() - got shape type " + shapeType + " but was expecting " + myShapeType);
        }

        else {

            // bounds
            file.readDoubleLE();
            file.readDoubleLE();
            file.readDoubleLE();
            file.readDoubleLE();
            actualReadWords += 4 * 4;

            int partOffsets[];

            int numParts = file.readIntLE();
            int numPoints = file.readIntLE();
            actualReadWords += 4;

            partOffsets = new int[numParts];

            for (int i = 0; i < numParts; i++) {
                partOffsets[i] = file.readIntLE();
                actualReadWords += 2;
            }

            ArrayList<LinearRing> shells = new ArrayList<LinearRing>();
            ArrayList<LinearRing> holes = new ArrayList<LinearRing>();
            // Bad rings are CCW rings not nested in another ring
            // and rings with more than 0 and less than 4 points
            ArrayList<LineString> badRings = new ArrayList<LineString>();
            Coordinate[] coords = new Coordinate[numPoints];

            for (int t = 0; t < numPoints; t++) {
                coords[t] = new Coordinate(file.readDoubleLE(), file.readDoubleLE());
                actualReadWords += 8;
            }

            if (myShapeType == 15) { // PolygonZ
                file.readDoubleLE(); // zmin
                file.readDoubleLE(); // zmax
                actualReadWords += 8;
                for (int t = 0; t < numPoints; t++) {
                    coords[t].z = file.readDoubleLE();
                    actualReadWords += 4;
                }
            }

            if (myShapeType >= 15) { // PolygonM or PolygonZ
                int fullLength;
                if (myShapeType == 15) { // polyZ (with M)
                    fullLength = 22 + (2 * numParts) + (8 * numPoints) + 8 + (4 * numPoints) + 8 + (4 * numPoints);
                } else { // polyM (with M)
                    fullLength = 22 + (2 * numParts) + (8 * numPoints) + 8 + (4 * numPoints);
                }
                if (contentLength >= fullLength) {
                    file.readDoubleLE(); // mmin
                    file.readDoubleLE(); // mmax
                    actualReadWords += 8;
                    for (int t = 0; t < numPoints; t++) {
                        file.readDoubleLE();
                        actualReadWords += 4;
                    }
                }
            }

            int offset = 0;
            int start, finish, length;
            for (int part = 0; part < numParts; part++) {
                start = partOffsets[part];
                if (part == numParts - 1) {
                    finish = numPoints;
                } else {
                    finish = partOffsets[part + 1];
                }
                length = finish - start;
                Coordinate points[] = new Coordinate[length];
                for (int i = 0; i < length; i++) {
                    points[i] = coords[offset];
                    offset++;
                }
                // REVISIT: polygons with only 1 or 2 points are not polygons -
                // geometryFactory will bomb so we skip if we find one.
                if ((points.length == 0 || points.length > 3) && points[0].equals(points[points.length - 1])) {
                    try {
                        LinearRing ring = geometryFactory.createLinearRing(points);
                        if (CGAlgorithms.isCCW(points)) {
                            holes.add(ring);
                        } else {
                            shells.add(ring);
                        }
                    } catch (IllegalArgumentException iae) {
                        LineString ring = geometryFactory.createLineString(points);
                        badRings.add(ring);
                    }
                } else {
                    LineString ring = geometryFactory.createLineString(points);
                    badRings.add(ring);
                }
            }

            if ((shells.size() > 1) && (holes.size() == 0)) {
                // some shells may be CW holes - esri tolerates this
                holes = findCWHoles(shells, geometryFactory); // find all rings
                                                              // contained in
                                                              // others
                if (holes.size() > 0) {
                    shells.removeAll(holes);
                    ArrayList ccwHoles = new ArrayList(holes.size());
                    for (int i = 0; i < holes.size(); i++) {
                        ccwHoles.add(reverseRing((LinearRing) holes.get(i)));
                    }
                    holes = ccwHoles;
                }
            }

            // now we have a list of all shells and all holes
            ArrayList holesForShells = new ArrayList(shells.size());
            ArrayList holesWithoutShells = new ArrayList();

            for (int i = 0; i < shells.size(); i++) {
                holesForShells.add(new ArrayList());
            }

            // Improve performance of complex polygon reading (already
            // implemented in geotools). See also Martin's mail at :
            // http://www.mail-archive.com/jump-pilot-devel@lists.sourceforge.net/msg10788.html
            // If shell is unique, don't check if holes are included :
            if (shells.size() == 1) {
                ((ArrayList) holesForShells.get(0)).addAll(holes);
            } else {
                // find holes
                for (int i = 0; i < holes.size(); i++) {
                    LinearRing testRing = (LinearRing) holes.get(i);
                    LinearRing minShell = null;
                    Envelope minEnv = null;
                    Envelope testEnv = testRing.getEnvelopeInternal();
                    Coordinate testPt = testRing.getCoordinateN(0);
                    LinearRing tryRing;
                    for (int j = 0; j < shells.size(); j++) {
                        tryRing = (LinearRing) shells.get(j);
                        Envelope tryEnv = tryRing.getEnvelopeInternal();
                        if (minShell != null)
                            minEnv = minShell.getEnvelopeInternal();
                        boolean isContained = false;
                        Coordinate[] coordList = tryRing.getCoordinates();
                        // Change test order to perform PiP test as few as
                        // possible
                        if (tryEnv.contains(testEnv) && (minShell == null || minEnv.contains(tryEnv)) && (cga.isPointInRing(testPt, coordList))) {
                            minShell = tryRing;
                        }
                    }

                    if (minShell == null) {
                        holesWithoutShells.add(testRing);
                    } else {
                        ((ArrayList) holesForShells.get(findIndex(shells, minShell))).add(testRing);
                    }
                }
            }

            Polygon[] polygons = new Polygon[shells.size() + holesWithoutShells.size()];

            for (int i = 0; i < shells.size(); i++) {
                polygons[i] = geometryFactory.createPolygon((LinearRing) shells.get(i), (LinearRing[]) ((ArrayList) holesForShells.get(i)).toArray(new LinearRing[0]));
            }

            for (int i = 0; i < holesWithoutShells.size(); i++) {
                polygons[shells.size() + i] = geometryFactory.createPolygon((LinearRing) holesWithoutShells.get(i), null);
                badRings.add((LinearRing) holesWithoutShells.get(i));
            }

            if (polygons.length == 1) { // it's a simple Polygon
                geom = polygons[0];
            } else { // its a multi part
                geom = geometryFactory.createMultiPolygon(polygons);
            }
            // add bad rings as Geometry userData so that advanced users can
            // retrieve them
            if (badRings.size() > 0) {
                geom.setUserData(geometryFactory.createMultiLineString(badRings.toArray(new LineString[0])));
            }
            holesForShells = null;
            holesWithoutShells = null;
            shells = null;
            holes = null;
        }
        // verify that we have read everything we need
        while (actualReadWords < contentLength) {
            int junk = file.readShortBE();
            actualReadWords += 1;
        }
        return geom;
    }

    /**
     * Finds a object in a list using == instead of equals. Should be much
     * faster than indexof
     */
    @SuppressWarnings("rawtypes")
    private static int findIndex(ArrayList list, Object o) {
        for (int i = 0, n = list.size(); i < n; i++) {
            if (list.get(i) == o)
                return i;
        }
        return -1;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    ArrayList findCWHoles(ArrayList shells, GeometryFactory geometryFactory) {
        ArrayList holesCW = new ArrayList(shells.size());
        LinearRing[] noHole = new LinearRing[0];
        for (int i = 0; i < shells.size(); i++) {
            LinearRing iRing = (LinearRing) shells.get(i);
            Envelope iEnv = iRing.getEnvelopeInternal();
            Coordinate[] coordList = iRing.getCoordinates();
            LinearRing jRing;
            for (int j = 0; j < shells.size(); j++) {
                if (i == j)
                    continue;
                jRing = (LinearRing) shells.get(j);
                Envelope jEnv = jRing.getEnvelopeInternal();
                Coordinate jPt = jRing.getCoordinateN(0);
                Coordinate jPt2 = jRing.getCoordinateN(1);
                if (iEnv.contains(jEnv)
                // && (CGAlgorithms.isPointInRing(jPt, coordList) ||
                // pointInList(jPt, coordList))
                // && (CGAlgorithms.isPointInRing(jPt2, coordList) ||
                // pointInList(jPt2, coordList))) {
                        && (CGAlgorithms.isPointInRing(jPt, coordList)) && (CGAlgorithms.isPointInRing(jPt2, coordList))) {
                    if (findIndex(holesCW, jRing) == -1) {
                        Polygon iPoly = geometryFactory.createPolygon(iRing, noHole);
                        Polygon jPoly = geometryFactory.createPolygon(jRing, noHole);
                        if (iPoly.contains(jPoly))
                            holesCW.add(jRing);
                    }
                }
            }
        }
        return holesCW;
    }

    /**
     * reverses the order of points in lr (is CW -> CCW or CCW->CW)
     */

    LinearRing reverseRing(LinearRing lr) {
        int numPoints = lr.getNumPoints();
        Coordinate[] newCoords = new Coordinate[numPoints];
        for (int t = 0; t < numPoints; t++) {
            newCoords[t] = lr.getCoordinateN(numPoints - t - 1);
        }
        return new LinearRing(newCoords, new PrecisionModel(), 0);
    }

    public void write(Geometry geometry, EndianDataOutputStream file) throws IOException {

        if (geometry.isEmpty()) {
            file.writeIntLE(0);
            return;
        }

        MultiPolygon multi;
        if (geometry instanceof MultiPolygon) {
            multi = (MultiPolygon) geometry;
        } else {
            multi = new MultiPolygon(new Polygon[] { (Polygon) geometry }, geometry.getPrecisionModel(), geometry.getSRID());
        }

        file.writeIntLE(getShapeType());

        Envelope box = multi.getEnvelopeInternal();
        file.writeDoubleLE(box.getMinX());
        file.writeDoubleLE(box.getMinY());
        file.writeDoubleLE(box.getMaxX());
        file.writeDoubleLE(box.getMaxY());

        // need to find the total number of rings and points
        int nrings = 0;
        for (int t = 0; t < multi.getNumGeometries(); t++) {
            Polygon p;
            p = (Polygon) multi.getGeometryN(t);
            nrings = nrings + 1 + p.getNumInteriorRing();
        }

        int u = 0;
        int[] pointsPerRing = new int[nrings];
        for (int t = 0; t < multi.getNumGeometries(); t++) {
            Polygon p;
            p = (Polygon) multi.getGeometryN(t);
            pointsPerRing[u] = p.getExteriorRing().getNumPoints();
            u++;
            for (int v = 0; v < p.getNumInteriorRing(); v++) {
                pointsPerRing[u] = p.getInteriorRingN(v).getNumPoints();
                u++;
            }
        }

        int npoints = multi.getNumPoints();

        file.writeIntLE(nrings);
        file.writeIntLE(npoints);

        int count = 0;
        for (int t = 0; t < nrings; t++) {
            file.writeIntLE(count);
            count = count + pointsPerRing[t];
        }

        // write out points here!
        Coordinate[] coords = multi.getCoordinates();
        int num;
        num = Array.getLength(coords);
        for (int t = 0; t < num; t++) {
            file.writeDoubleLE(coords[t].x);
            file.writeDoubleLE(coords[t].y);
        }

        if (myShapeType == 15) { // z
            double[] zExtreame = zMinMax(multi);
            if (Double.isNaN(zExtreame[0])) {
                file.writeDoubleLE(0.0);
                file.writeDoubleLE(0.0);
            } else {
                file.writeDoubleLE(zExtreame[0]);
                file.writeDoubleLE(zExtreame[1]);
            }
            for (int t = 0; t < npoints; t++) {
                double z = coords[t].z;
                if (Double.isNaN(z))
                    file.writeDoubleLE(0.0);
                else
                    file.writeDoubleLE(z);
            }
        }

        if (myShapeType >= 15) { // m
            file.writeDoubleLE(-10E40);
            file.writeDoubleLE(-10E40);
            for (int t = 0; t < npoints; t++) {
                file.writeDoubleLE(-10E40);
            }
        }
    }

    public int getShapeType() {
        return myShapeType;
    }

    public int getLength(Geometry geometry) {

        if (geometry.isEmpty())
            return 2;

        MultiPolygon multi;
        if (geometry instanceof MultiPolygon) {
            multi = (MultiPolygon) geometry;
        } else {
            multi = new MultiPolygon(new Polygon[] { (Polygon) geometry }, geometry.getPrecisionModel(), geometry.getSRID());
        }
        int nrings = 0;
        for (int t = 0; t < multi.getNumGeometries(); t++) {
            Polygon p;
            p = (Polygon) multi.getGeometryN(t);
            nrings = nrings + 1 + p.getNumInteriorRing();
        }
        int npoints = multi.getNumPoints();
        if (myShapeType == 15) {
            return 22 + (2 * nrings) + 8 * npoints + 4 * npoints + 8 + 4 * npoints + 8;
        }
        if (myShapeType == 25) {
            return 22 + (2 * nrings) + 8 * npoints + 4 * npoints + 8;
        }
        return 22 + (2 * nrings) + 8 * npoints;
    }

    double[] zMinMax(Geometry g) {

        double zmin = Double.NaN;
        double zmax = Double.NaN;
        boolean validZFound = false;
        Coordinate[] cs = g.getCoordinates();
        double z;

        for (int t = 0; t < cs.length; t++) {
            z = cs[t].z;
            if (!(Double.isNaN(z))) {
                if (validZFound) {
                    if (z < zmin)
                        zmin = z;
                    if (z > zmax)
                        zmax = z;
                } else {
                    validZFound = true;
                    zmin = z;
                    zmax = z;
                }
            }
        }
        return new double[] { zmin, zmax };
    }

    /**
     * Return a empty geometry.
     */
    public Geometry getEmptyGeometry(GeometryFactory factory) {
        return factory.createMultiPolygon(new Polygon[0]);
    }

}

/*
 * $Log$ Revision 1.7 2009/05/10 michaudm Fix a bug in findCWHoles. Could create
 * a 'outer hole' because the test to check if a ring contains another ring was
 * a quick and dirty test.
 * 
 * Revision 1.6 2008/04/22 20:55:36 beckerl Restored the original inline code in
 * read() and added the CW hole detection. The new geotools routines always
 * created Multipolygons.
 * 
 * Revision 1.3 2007/01/03 22:43:17 rlittlefield changed so that the
 * holesWithoutShells array initialized to zero length
 * 
 * Revision 1.2 2007/01/03 16:48:43 rlittlefield modified code so that holes
 * without shells are not excluded
 * 
 * Revision 1.1 2006/11/28 22:30:57 beckerl First SkyJUMP commit. Prior version
 * numbers lost.
 * 
 * Revision 1.1 2006/02/28 22:42:14 ashsdesigner Initial commit of larry's
 * jump/org Eclipse project folder
 * 
 * Revision 1.5 2003/09/23 17:15:26 dblasby *** empty log message ***
 * 
 * Revision 1.4 2003/07/25 18:49:15 dblasby Allow "extra" data after the
 * content. Fixes the ICI shapefile bug.
 * 
 * Revision 1.3 2003/02/04 02:10:37 jaquino Feature: EditWMSQuery dialog
 * 
 * Revision 1.2 2003/01/22 18:31:05 jaquino Enh: Make About Box configurable
 * 
 * Revision 1.2 2002/09/09 20:46:22 dblasby Removed LEDatastream refs and
 * replaced with EndianData[in/out]putstream
 * 
 * Revision 1.1 2002/08/27 21:04:58 dblasby orginal
 * 
 * Revision 1.3 2002/03/05 10:51:01 andyt removed use of factory from write
 * method
 * 
 * Revision 1.2 2002/03/05 10:23:59 jmacgill made sure geometries were created
 * using the factory methods
 * 
 * Revision 1.1 2002/02/28 00:38:50 jmacgill Renamed files to more intuitve
 * names
 * 
 * Revision 1.4 2002/02/13 00:23:53 jmacgill First semi working JTS version of
 * Shapefile code
 * 
 * Revision 1.3 2002/02/11 18:44:22 jmacgill replaced geometry constructions
 * with calls to geometryFactory.createX methods
 * 
 * Revision 1.2 2002/02/11 18:28:41 jmacgill rewrote to have static read and
 * write methods
 * 
 * Revision 1.1 2002/02/11 16:54:43 jmacgill added shapefile code and
 * directories
 */
