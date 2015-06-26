package fr.michaelm.jump.drivers.dxf;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class DxfENTITY {

    public static String geometry2Dxf(Geometry geometry, String layerName, int precision) {

        Geometry g = geometry;

        if (g.getGeometryType().equals("Point")) {
            return point2Dxf(geometry, layerName, precision);
        } else if (g.getGeometryType().equals("LineString")) {
            return lineString2Dxf(geometry, layerName, precision);
        } else if (g.getGeometryType().equals("Polygon")) {
            return polygon2Dxf(geometry, layerName, precision);
        } else if (g instanceof GeometryCollection) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < g.getNumGeometries(); i++) {
                Geometry ff = g.getGeometryN(i);
                sb.append(geometry2Dxf(ff, layerName, precision));
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    public static String point2Dxf(Geometry geometry, String layerName, int precision) {

        StringBuffer sb = new StringBuffer(DxfGroup.toString(0, "POINT"));
        sb.append(DxfGroup.toString(8, layerName));
        Coordinate coord = ((Point) geometry).getCoordinate();
        sb.append(DxfGroup.toString(10, coord.x, precision));
        sb.append(DxfGroup.toString(20, coord.y, precision));
        if (!Double.isNaN(coord.z)) {
            sb.append(DxfGroup.toString(30, coord.z, precision));
        }
        return sb.toString();
    }

    public static String lineString2Dxf(Geometry geometry, String layerName, int precision) {
        LineString geom = (LineString) geometry;
        Coordinate[] coords = geom.getCoordinates();
        boolean isLine = (coords.length == 2);
        StringBuffer sb;
        if (!isLine) {
            sb = new StringBuffer(DxfGroup.toString(0, "POLYLINE"));
        } else {
            sb = new StringBuffer(DxfGroup.toString(0, "LINE"));
        }
        sb.append(DxfGroup.toString(8, layerName));
        if (isLine) {
            sb.append(DxfGroup.toString(10, coords[0].x, precision));
            sb.append(DxfGroup.toString(20, coords[0].y, precision));
            if (!Double.isNaN(coords[0].z)) {
                sb.append(DxfGroup.toString(30, coords[0].z, precision));
            }
            sb.append(DxfGroup.toString(11, coords[1].x, precision));
            sb.append(DxfGroup.toString(21, coords[1].y, precision));
            if (!Double.isNaN(coords[1].z)) {
                sb.append(DxfGroup.toString(31, coords[1].z, precision));
            }
        } else {
            sb.append(DxfGroup.toString(66, 1));
            sb.append(DxfGroup.toString(10, "0.0"));
            sb.append(DxfGroup.toString(20, "0.0"));
            if (!Double.isNaN(coords[0].z))
                sb.append(DxfGroup.toString(30, "0.0"));
            sb.append(DxfGroup.toString(70, 8));

            for (int i = 0; i < coords.length; i++) {
                sb.append(DxfGroup.toString(0, "VERTEX"));
                sb.append(DxfGroup.toString(8, layerName));
                sb.append(DxfGroup.toString(10, coords[i].x, precision));
                sb.append(DxfGroup.toString(20, coords[i].y, precision));
                if (!Double.isNaN(coords[i].z))
                    sb.append(DxfGroup.toString(30, coords[i].z, precision));
                sb.append(DxfGroup.toString(70, 32));
            }
            sb.append(DxfGroup.toString(0, "SEQEND"));
        }
        return sb.toString();
    }

    public static String polygon2Dxf(Geometry geometry, String layerName, int precision) {
        Polygon geom = (Polygon) geometry;
        Coordinate[] coords = geom.getExteriorRing().getCoordinates();
        StringBuffer sb = new StringBuffer(DxfGroup.toString(0, "POLYLINE"));
        sb.append(DxfGroup.toString(8, layerName));
        sb.append(DxfGroup.toString(66, 1));
        sb.append(DxfGroup.toString(10, "0.0"));
        sb.append(DxfGroup.toString(20, "0.0"));
        if (!Double.isNaN(coords[0].z))
            sb.append(DxfGroup.toString(30, "0.0"));
        sb.append(DxfGroup.toString(70, 9));
        for (int i = 0; i < coords.length; i++) {
            sb.append(DxfGroup.toString(0, "VERTEX"));
            sb.append(DxfGroup.toString(8, layerName));
            sb.append(DxfGroup.toString(10, coords[i].x, precision));
            sb.append(DxfGroup.toString(20, coords[i].y, precision));
            if (!Double.isNaN(coords[i].z))
                sb.append(DxfGroup.toString(30, coords[i].z, precision));
            sb.append(DxfGroup.toString(70, 32));
        }
        sb.append(DxfGroup.toString(0, "SEQEND"));
        for (int h = 0; h < geom.getNumInteriorRing(); h++) {
            sb.append(DxfGroup.toString(0, "POLYLINE"));
            sb.append(DxfGroup.toString(8, layerName));
            sb.append(DxfGroup.toString(66, 1));
            sb.append(DxfGroup.toString(10, "0.0"));
            sb.append(DxfGroup.toString(20, "0.0"));
            if (!Double.isNaN(coords[0].z))
                sb.append(DxfGroup.toString(30, "0.0"));
            sb.append(DxfGroup.toString(70, 9));
            coords = geom.getInteriorRingN(h).getCoordinates();
            for (int i = 0; i < coords.length; i++) {
                sb.append(DxfGroup.toString(0, "VERTEX"));
                sb.append(DxfGroup.toString(8, layerName));
                sb.append(DxfGroup.toString(10, coords[i].x, precision));
                sb.append(DxfGroup.toString(20, coords[i].y, precision));
                if (!Double.isNaN(coords[i].z))
                    sb.append(DxfGroup.toString(30, coords[i].z, precision));
                sb.append(DxfGroup.toString(70, 32));
            }
            sb.append(DxfGroup.toString(0, "SEQEND"));
        }

        return sb.toString();
    }

}
