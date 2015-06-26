package org.geotools.shapefile;

import java.io.IOException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jump.io.EndianDataInputStream;
import com.vividsolutions.jump.io.EndianDataOutputStream;

/**
 * Wrapper for a Shapefile Point.
 */
// getLength() modified by Micha&euml;l MICHAUD on 3 nov. 2004 to handle
// Point, PointM and PointZ length properly
public class PointHandler implements ShapeHandler {

    int Ncoords = 2; // 2 = x,y ; 3= x,y,m ; 4 = x,y,z,m
    int myShapeType = -1;

    public PointHandler(int type) throws InvalidShapefileException {
        if ((type != 1) && (type != 11) && (type != 21)) {
            throw new InvalidShapefileException("PointHandler constructor: expected a type of 1, 11 or 21");
        }
        myShapeType = type;
    }

    public PointHandler() {
        myShapeType = 1; // 2d
    }

    public Geometry read(EndianDataInputStream file, GeometryFactory geometryFactory, int contentLength) throws IOException, InvalidShapefileException {

        int actualReadWords = 0; // actual number of 16 bits words
        Geometry geom = null;

        int shapeType = file.readIntLE();
        actualReadWords += 2;

        if (shapeType == 0) {
            geom = geometryFactory.createPoint(new CoordinateArraySequence(0));
        } else if (shapeType != myShapeType) {
            throw new InvalidShapefileException("pointhandler.read() - handler's shapetype doesnt match file's");
        } else {
            double x = file.readDoubleLE();
            double y = file.readDoubleLE();
            @SuppressWarnings("unused")
            double m, z = Double.NaN;
            actualReadWords += 8;

            if (shapeType == 21) {
                m = file.readDoubleLE();
                actualReadWords += 4;
            }

            else if (shapeType == 11) {
                z = file.readDoubleLE();
                actualReadWords += 4;
                if (contentLength > actualReadWords) {
                    m = file.readDoubleLE();
                    actualReadWords += 8;
                }
            }

            geom = geometryFactory.createPoint(new Coordinate(x, y, z));

        }
        // verify that we have read everything we need
        while (actualReadWords < contentLength) {
            @SuppressWarnings("unused")
            int junk2 = file.readShortBE();
            actualReadWords += 1;
        }

        return geom;
    }

    public void write(Geometry geometry, EndianDataOutputStream file) throws IOException {
        if (geometry.isEmpty()) {
            file.writeIntLE(0);
            return;
        }
        file.writeIntLE(getShapeType());
        Coordinate c = geometry.getCoordinates()[0];
        file.writeDoubleLE(c.x);
        file.writeDoubleLE(c.y);

        if (myShapeType == 11) {
            if (Double.isNaN(c.z)) // nan means not defined
                file.writeDoubleLE(0.0);
            else
                file.writeDoubleLE(c.z);
        }
        if ((myShapeType == 11) || (myShapeType == 21)) {
            file.writeDoubleLE(-10E40); // M
        }
    }

    /**
     * Returns the shapefile shape type value for a point
     * 
     * @return int Shapefile.POINT
     */
    public int getShapeType() {
        return myShapeType;
    }

    /**
     * Calcuates the record length of this object.
     * 
     * @return the length of the record that this point will take up in a
     *         shapefile (in WORDS)
     **/
    public int getLength(Geometry geometry) {
        if (geometry.isEmpty())
            return 2;
        else if (myShapeType == 1)
            return 10;
        else if (myShapeType == 21)
            return 14;
        else
            return 18;
    }

    /**
     * Return a empty geometry.
     */
    public Geometry getEmptyGeometry(GeometryFactory factory) {
        return factory.createPoint(new CoordinateArraySequence(0));
    }
}
