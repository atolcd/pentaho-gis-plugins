package org.geotools.shapefile;

import java.io.IOException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.io.EndianDataInputStream;
import com.vividsolutions.jump.io.EndianDataOutputStream;

/**
 * Interface implemented by all the ShapeType handlers
 */
public interface ShapeHandler {
    /**
     * Returns one of the ShapeType int defined by the specification.
     * <ul>
     * <li>0 Null Shape</li>
     * <li>1 Point</li>
     * <li>3 PolyLine</li>
     * <li>5 Polygon</li>
     * <li>8 MultiPoint</li>
     * <li>11 PointZ</li>
     * <li>13 PolyLineZ</li>
     * <li>15 PolygonZ</li>
     * <li>18 MultiPointZ</li>
     * <li>21 PointM</li>
     * <li>23 PolyLineM</li>
     * <li>25 PolygonM</li>
     * <li>28 MultiPointM</li>
     * <li>31 MultiPatch</li>
     * </ul>
     */
    public int getShapeType();

    public Geometry read(EndianDataInputStream file, GeometryFactory geometryFactory, int contentLength) throws IOException, InvalidShapefileException;

    public void write(Geometry geometry, EndianDataOutputStream file) throws IOException;

    public int getLength(Geometry geometry); // length in 16bit words

    /**
     * Return a empty geometry.
     */
    public Geometry getEmptyGeometry(GeometryFactory factory);
}
