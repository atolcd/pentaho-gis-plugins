package org.geotools.shapefile;

import java.io.IOException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jump.io.EndianDataInputStream;
import com.vividsolutions.jump.io.EndianDataOutputStream;

/**
 * Null Shape handler for files containing only null shapes.
 */
public class NullShapeHandler implements ShapeHandler {

    int myShapeType = -1;

    public NullShapeHandler(int type) throws InvalidShapefileException {
        if (type != 0) {
            throw new InvalidShapefileException("NullShapeHandler constructor: expected a type of 0");
        }
        myShapeType = type;
    }

    public NullShapeHandler() {
        myShapeType = 0;
    }

    public Geometry read(EndianDataInputStream file, GeometryFactory geometryFactory, int contentLength) throws IOException, InvalidShapefileException {

        int actualReadWords = 0; // actual number of 16 bits words read
        Geometry geom = null;

        int shapeType = file.readIntLE();
        actualReadWords += 2;

        if (shapeType == 0) {
            geom = geometryFactory.createGeometryCollection(new Geometry[0]);
        } else if (shapeType != myShapeType) {
            throw new InvalidShapefileException("nullshapehandler.read() - handler's shapetype doesnt match file's");
        } else {
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
        file.writeIntLE(0);
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
     * @return the length of a null shape (in 16 bits WORDS)
     **/
    public int getLength(Geometry geometry) {
        return 2;
    }

    /**
     * Return a empty geometry.
     */
    public Geometry getEmptyGeometry(GeometryFactory factory) {
        return factory.createPoint(new CoordinateArraySequence(0));
    }
}
