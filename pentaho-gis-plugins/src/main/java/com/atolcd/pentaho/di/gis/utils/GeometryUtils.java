package com.atolcd.pentaho.di.gis.utils;

/*
 * #%L
 * Pentaho Data Integrator GIS Plugin
 * %%
 * Copyright (C) 2015 Atol CD
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.GeometryExtracter;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

public final class GeometryUtils {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * Returns a geometry from WKT/EWKT string
     * 
     * @param wkt
     * @return
     * @throws Exception
     */
    public static Geometry getGeometryFromEWKT(String wkt) throws Exception {

        Geometry outputGeometry = null;

        try {

            String wktParts[] = wkt.toUpperCase().split(";");

            if (wktParts[0].replace("SRID=", "").matches("[0-9]+")) {

                outputGeometry = new WKTReader().read(wktParts[1]);
                outputGeometry.setSRID(Integer.valueOf(wktParts[0].replace("SRID=", "")));

            } else {

                outputGeometry = new WKTReader().read(wktParts[0]);
            }

        } catch (ParseException e) {
            throw new Exception("The value \"" + wkt + "\" is not a WKT/EWKT valid string");
        }

        return getNonEmptyGeometry(outputGeometry.getSRID(), outputGeometry);
    }

    /**
     * Returns the EWKT string geometry with SRID and Z coordinates (if
     * possible)
     * 
     * @param geometry
     * @param forceTo2d
     * @return
     */
    public static String getEWKTFromGeometry(Geometry geometry, boolean forceTo2d) {

        String outputEWKT = null;

        if (!isNullOrEmptyGeometry(geometry)) {

            if (getCoordinateDimension(geometry) == 3 && !forceTo2d) {
                outputEWKT = new WKTWriter(3).write(geometry);
            } else {
                outputEWKT = geometry.toString();
            }

            if (geometry.getSRID() > 0) {
                outputEWKT = "SRID=" + geometry.getSRID() + ";" + outputEWKT;
            }

        }

        return outputEWKT;

    }

    /**
     * Returns the geometry type
     * 
     * @param geometry
     * @return
     */
    public static String getGeometryType(Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {
            return geometry.getGeometryType().toUpperCase();
        }

        return null;

    }

    /**
     * Returns null if the JTS geometry is empty otherwise geometry
     * 
     * @param geometry
     * @return
     */
    public static Geometry getNonEmptyGeometry(Integer srid, Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {

            if (srid != null) {
                geometry.setSRID(srid);
                return geometry;
            } else {
                return geometry;
            }

        } else {
            return null;
        }
    }

    /**
     * Returns the geometry area
     * 
     * @param geometry
     * @return
     */
    public static Double getArea(Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {
            return geometry.getArea();
        }

        return null;

    }

    /**
     * Returns the geometry length or perimeter
     * 
     * @param geometry
     * @return
     */
    public static Double getLength(Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {
            return geometry.getLength();
        }

        return null;

    }

    /**
     * Returns the x min
     * 
     * @param geometry
     * @return
     */
    public static Double getMinX(Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {
            return geometry.getEnvelopeInternal().getMinX();
        }

        return null;

    }

    /**
     * Returns the x max
     * 
     * @param geometry
     * @return
     */
    public static Double getMaxX(Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {
            return geometry.getEnvelopeInternal().getMaxX();
        }

        return null;

    }

    /**
     * Returns the y min
     * 
     * @param geometry
     * @return
     */
    public static Double getMinY(Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {
            return geometry.getEnvelopeInternal().getMinY();
        }

        return null;

    }

    /**
     * Returns the y max
     * 
     * @param geometry
     * @return
     */
    public static Double getMaxY(Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {
            return geometry.getEnvelopeInternal().getMaxY();
        }

        return null;

    }

    /**
     * Returns the geometry sub-geometries number or 1 if simple geometry
     * 
     * @param geometry
     * @return
     */
    public static Integer getGeometriesCount(Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {
            return geometry.getNumGeometries();
        }

        return null;

    }

    /**
     * Returns the geometry coordinates number
     * 
     * @param geometry
     * @return
     */
    public static Integer getCoordinatesCount(Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {
            return geometry.getCoordinates().length;
        }

        return null;

    }

    /**
     * Returns true if geometry object is null or JTS geometry is empty
     * 
     * @param geometry
     * @return
     */
    public static boolean isNullOrEmptyGeometry(Geometry geometry) {

        if (geometry != null && !geometry.isEmpty()) {
            return false;
        } else {
            return true;
        }

    }

    /**
     * Returns 3 if z coordinate otherwise 2
     * 
     * @param geometry
     * @return
     */
    public static Integer getCoordinateDimension(Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {
            Coordinate firstCoordinate = geometry.getCoordinates()[0];
            if (!Double.isNaN(firstCoordinate.z)) {
                return 3;
            } else {
                return 2;
            }
        }

        return null;

    }

    /**
     * Returns geometry SRID
     * 
     * @param geometry
     * @return
     */
    public static Integer getSrid(Geometry geometry) {

        if (!isNullOrEmptyGeometry(geometry)) {
            return geometry.getSRID();
        }
        return null;

    }

    /**
     * Returns a 2d coordinate
     * 
     * @param coordinate
     * @return
     */
    private static Coordinate get2DCoordinate(Coordinate coordinate) {

        if (coordinate != null) {
            return new Coordinate(coordinate.x, coordinate.y);
        }

        return null;

    }

    /**
     * Returns a 2d coordinates array
     * 
     * @param coordinates
     * @return
     */
    private static Coordinate[] get2DCoordinates(Coordinate coordinates[]) {

        Coordinate outCoordinates[] = new Coordinate[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            outCoordinates[i] = get2DCoordinate(coordinates[i]);
        }

        return outCoordinates;

    }

    /**
     * Returns 2D geometry from input geometry
     * 
     * @param geometry
     * @return
     * @throws Exception
     */
    public static Geometry get2DGeometry(Geometry geometry) {

        Geometry outputGeometry = null;

        if (!isNullOrEmptyGeometry(geometry)) {

            // Point
            if (geometry instanceof Point) {

                outputGeometry = geometryFactory.createPoint(get2DCoordinate(((Point) geometry).getCoordinate()));

                // MultiPoint
            } else if (geometry instanceof MultiPoint) {

                outputGeometry = geometryFactory.createMultiPoint(get2DCoordinates(geometry.getCoordinates()));

                // Linestring
            } else if (geometry instanceof LineString) {

                outputGeometry = geometryFactory.createLineString(get2DCoordinates(geometry.getCoordinates()));

                // MultiLineString
            } else if (geometry instanceof MultiLineString) {

                LineString lineStrings[] = new LineString[geometry.getNumGeometries()];
                for (int i = 0; i < geometry.getNumGeometries(); i++) {
                    lineStrings[i] = (LineString) get2DGeometry((LineString) geometry.getGeometryN(i));
                }

                outputGeometry = geometryFactory.createMultiLineString(lineStrings);

                // Polygon
            } else if (geometry instanceof Polygon) {

                Polygon polygon = (Polygon) geometry;
                LinearRing exteriorRing = geometryFactory.createLinearRing(get2DGeometry(polygon.getExteriorRing()).getCoordinates());
                LinearRing interiorRings[] = new LinearRing[polygon.getNumInteriorRing()];
                for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                    interiorRings[i] = geometryFactory.createLinearRing(get2DGeometry(polygon.getInteriorRingN(i)).getCoordinates());
                }

                outputGeometry = geometryFactory.createPolygon(exteriorRing, interiorRings);

            } else if (geometry instanceof MultiPolygon) {

                MultiPolygon multiPolygon = (MultiPolygon) geometry;
                Polygon polygons[] = new Polygon[multiPolygon.getNumGeometries()];
                for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                    polygons[i] = (Polygon) get2DGeometry((Polygon) multiPolygon.getGeometryN(i));
                }

                outputGeometry = geometryFactory.createMultiPolygon(polygons);

            } else if (geometry instanceof GeometryCollection) {

                GeometryCollection geometryCollection = (GeometryCollection) geometry;
                Geometry geometries[] = new Geometry[geometryCollection.getNumGeometries()];
                for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {

                    geometries[i] = get2DGeometry(geometry);

                }

                outputGeometry = geometryFactory.createGeometryCollection(geometries);

            }
        }

        return getNonEmptyGeometry(geometry.getSRID(), outputGeometry);

    }

    @SuppressWarnings("unchecked")
    public static Geometry getGeometryFromType(Geometry geometry, Class<?> geometryClass) {

        List<Geometry> typedGeometries = GeometryExtracter.extract(geometry, geometryClass);
        List<Geometry> outputGeometries = new ArrayList<Geometry>();

        for (Geometry typedGeometry : typedGeometries) {

            if (typedGeometry instanceof MultiLineString || typedGeometry instanceof MultiPoint || typedGeometry instanceof MultiPolygon) {

                for (int i = 0; i < typedGeometry.getNumGeometries(); i++) {
                    outputGeometries.add(typedGeometry.getGeometryN(i));
                }

            } else {
                outputGeometries.add(typedGeometry);
            }

        }

        Geometry outputGeometry = geometryFactory.buildGeometry(outputGeometries);
        return getNonEmptyGeometry(geometry.getSRID(), outputGeometry);

    }

    /**
     * Returns multi-geometry from input geometry
     * 
     * @param geometry
     * @return
     */
    public static Geometry getMultiGeometry(Geometry geometry) {

        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry outputGeometry = null;

        if (!isNullOrEmptyGeometry(geometry)) {

            if (geometry instanceof Point) {

                List<Point> pointList = new ArrayList<Point>();
                pointList.add((Point) geometry);
                outputGeometry = geometryFactory.createMultiPoint(GeometryFactory.toPointArray(pointList));

            } else if (geometry instanceof LineString) {

                List<LineString> lineStringList = new ArrayList<LineString>();
                lineStringList.add((LineString) geometry);
                outputGeometry = geometryFactory.createMultiLineString(GeometryFactory.toLineStringArray(lineStringList));

            } else if (geometry instanceof Polygon) {

                List<Polygon> polygonList = new ArrayList<Polygon>();
                polygonList.add((Polygon) geometry);
                outputGeometry = geometryFactory.createMultiPolygon(GeometryFactory.toPolygonArray(polygonList));

            } else {
                outputGeometry = geometry;
            }

        }

        return getNonEmptyGeometry(geometry.getSRID(), outputGeometry);

    }

    /**
     * Returns metadatas from a GeometryCollection geometry
     * 
     * @param geometryCollection
     * @return
     */
    public static HashMap<String, Object> getGeometryCollectionInfos(GeometryCollection geometryCollection) {

        HashMap<String, Object> geometryCollectionInfos = new HashMap<String, Object>();
        TreeSet<String> primaryTypes = new TreeSet<String>();
        TreeSet<String> types = new TreeSet<String>();
        TreeSet<Integer> dimensions = new TreeSet<Integer>();
        TreeSet<Integer> srids = new TreeSet<Integer>();

        for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {

            Geometry geometry = geometryCollection.getGeometryN(i);

            // Geometry types
            String type = getGeometryType(geometry);
            types.add(type);

            // Primary types
            primaryTypes.add(type.replace("MULTI", ""));

            // Dimensions
            if (getCoordinateDimension(geometry) == 3) {
                dimensions.add(3);
            } else {
                dimensions.add(2);
            }

            // SRIDs
            srids.add(geometry.getSRID());

        }

        geometryCollectionInfos.put("PRIMARY_TYPES", new ArrayList<String>(primaryTypes));
        geometryCollectionInfos.put("TYPES", new ArrayList<String>(types));
        geometryCollectionInfos.put("DIMENSIONS", new ArrayList<Integer>(dimensions));
        geometryCollectionInfos.put("SRIDS", new ArrayList<Integer>(srids));

        return geometryCollectionInfos;
    }

    public static Geometry getMergedGeometry(Geometry geometry) {

        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry outputGeometry = null;

        if (!isNullOrEmptyGeometry(geometry)) {

            if (geometry instanceof LineString || geometry instanceof MultiLineString) {
                LineMerger lineMerger = new LineMerger();
                lineMerger.add(geometry);
                LineString[] lineStrings = GeometryFactory.toLineStringArray(lineMerger.getMergedLineStrings());
                outputGeometry = geometryFactory.createMultiLineString(lineStrings);

            } else {
                outputGeometry = geometry;
            }

        }

        return getNonEmptyGeometry(geometry.getSRID(), outputGeometry);

    }

    /*
     * public static Geometry asCurve(Geometry geometry){
     * 
     * GeometryFactory geometryFactory = new GeometryFactory();
     * 
     * if(!isNullOrEmptyGeometry(geometry)){ if(geometry.getDimension() >= 1){
     * 
     * LineMerger lineMerger = new LineMerger(); lineMerger.add(geometry);
     * LineString[] lineStrings =
     * GeometryFactory.toLineStringArray(lineMerger.getMergedLineStrings());
     * Geometry outputGeometry =
     * geometryFactory.createMultiLineString(lineStrings);
     * outputGeometry.setSRID(geometry.getSRID());
     * 
     * if(!isNullOrEmptyGeometry(outputGeometry) &&
     * outputGeometry.getNumGeometries()==1){ outputGeometry =
     * outputGeometry.getGeometryN(0);
     * outputGeometry.setSRID(geometry.getSRID()); }
     * 
     * return getNonEmptyGeometry(outputGeometry);
     * 
     * }else{ return null; } }else{ return null; } }
     * 
     * public static Geometry asSurface(Geometry geometry){
     * 
     * GeometryFactory geometryFactory = new GeometryFactory();
     * 
     * if(!isNullOrEmptyGeometry(geometry)){
     * 
     * if(geometry.getDimension() == 2){
     * 
     * return getNonEmptyGeometry(geometry);
     * 
     * }else if(geometry.getDimension() == 1){
     * 
     * LineMerger lineMerger = new LineMerger(); lineMerger.add(geometry);
     * Polygonizer polygonizer = new Polygonizer();
     * polygonizer.add(lineMerger.getMergedLineStrings()); Polygon[] polygons =
     * GeometryFactory.toPolygonArray(polygonizer.getPolygons()); Geometry
     * outputGeometry = geometryFactory.createMultiPolygon(polygons);
     * outputGeometry.setSRID(geometry.getSRID());
     * 
     * if(!isNullOrEmptyGeometry(outputGeometry) &&
     * outputGeometry.getNumGeometries()==1){ outputGeometry =
     * outputGeometry.getGeometryN(0);
     * outputGeometry.setSRID(outputGeometry.getSRID()); }
     * 
     * return getNonEmptyGeometry(outputGeometry);
     * 
     * }else{ return null; } }else{ return null; } }
     */

}
