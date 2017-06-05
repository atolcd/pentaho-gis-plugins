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


import org.cts.IllegalCoordinateException;
import org.cts.op.CoordinateOperation;
import org.pentaho.di.core.exception.KettleException;

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

public final class CoordinateTransformer {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * Conversion d'une géométrie JTS d'un système de coordonnées vers un autre
     * 
     * @param geometry
     * @param transformation
     * @return
     * @throws KettleException
     */
    public static Geometry transform(Geometry geometry, CoordinateOperation transformation) throws KettleException {

        if (!GeometryUtils.isNullOrEmptyGeometry(geometry)) {

            // Point
            if (geometry instanceof Point) {

                return tranformPoint((Point) geometry, transformation);

                // MultiPoint
            } else if (geometry instanceof MultiPoint) {

                return tranformMultiPoint((MultiPoint) geometry, transformation);

                // Linestring
            } else if (geometry instanceof LineString) {

                return tranformLineString((LineString) geometry, transformation);

                // MultiLineString
            } else if (geometry instanceof MultiLineString) {

                return tranformMultiLineString((MultiLineString) geometry, transformation);

            } else if (geometry instanceof Polygon) {

                return tranformPolygon((Polygon) geometry, transformation);

            } else if (geometry instanceof MultiPolygon) {

                return tranformMultiPolygon((MultiPolygon) geometry, transformation);

            } else if (geometry instanceof GeometryCollection) {

                return tranformGeometryCollection((GeometryCollection) geometry, transformation);

            } else {
                throw new KettleException("Transformation error : " + geometry.getClass().getCanonicalName() + " is not supported.");
            }

        } else {
            return null;
        }

    }

    /**
     * Transformation d'un point
     * 
     * @param point
     * @param transformation
     * @return
     * @throws KettleException
     */
    private static Point tranformPoint(Point point, CoordinateOperation transformation) throws KettleException {
        return geometryFactory.createPoint(transformCoordinate(point.getCoordinate(), transformation));
    }

    /**
     * Transformation d'une ligne
     * 
     * @param lineString
     * @param transformation
     * @return
     * @throws KettleException
     */
    private static LineString tranformLineString(LineString lineString, CoordinateOperation transformation) throws KettleException {
        return geometryFactory.createLineString(tranformCoordinates(lineString.getCoordinates(), transformation));
    }

    /**
     * Transformation d'un polygone
     * 
     * @param polygon
     * @param transformation
     * @return
     * @throws KettleException
     */
    private static Polygon tranformPolygon(Polygon polygon, CoordinateOperation transformation) throws KettleException {

        LinearRing exteriorRing = geometryFactory.createLinearRing(tranformLineString(polygon.getExteriorRing(), transformation).getCoordinates());
        LinearRing interiorRings[] = new LinearRing[polygon.getNumInteriorRing()];
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            interiorRings[i] = geometryFactory.createLinearRing(tranformLineString(polygon.getInteriorRingN(i), transformation).getCoordinates());
        }

        return geometryFactory.createPolygon(exteriorRing, interiorRings);
    }

    /**
     * Transformation d'un (multi) point
     * 
     * @param multiPoint
     * @param transformation
     * @return
     * @throws KettleException
     */
    private static MultiPoint tranformMultiPoint(MultiPoint multiPoint, CoordinateOperation transformation) throws KettleException {

        return geometryFactory.createMultiPoint(tranformCoordinates(multiPoint.getCoordinates(), transformation));

    }

    /**
     * Transformation d'une (multi) ligne
     * 
     * @param multiLineString
     * @param transformation
     * @return
     * @throws KettleException
     */
    private static MultiLineString tranformMultiLineString(MultiLineString multiLineString, CoordinateOperation transformation) throws KettleException {

        LineString lineStrings[] = new LineString[multiLineString.getNumGeometries()];
        for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
            lineStrings[i] = tranformLineString((LineString) multiLineString.getGeometryN(i), transformation);
        }

        return geometryFactory.createMultiLineString(lineStrings);
    }

    /**
     * Transformation d'un (multi) polygone
     * 
     * @param multiPolygon
     * @param transformation
     * @return
     * @throws KettleException
     */
    private static MultiPolygon tranformMultiPolygon(MultiPolygon multiPolygon, CoordinateOperation transformation) throws KettleException {

        Polygon polygons[] = new Polygon[multiPolygon.getNumGeometries()];
        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
            polygons[i] = tranformPolygon((Polygon) multiPolygon.getGeometryN(i), transformation);
        }

        return geometryFactory.createMultiPolygon(polygons);
    }

    /**
     * Transformation d'une collection de géométries
     * 
     * @param geometryCollection
     * @param transformation
     * @return
     * @throws KettleException
     */
    private static GeometryCollection tranformGeometryCollection(GeometryCollection geometryCollection, CoordinateOperation transformation) throws KettleException {

        Geometry geometries[] = new Geometry[geometryCollection.getNumGeometries()];
        for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {

            Geometry geometry = geometryCollection.getGeometryN(i);

            // Point
            if (geometry instanceof Point) {

                geometries[i] = tranformPoint((Point) geometry, transformation);

                // MultiPoint
            } else if (geometry instanceof MultiPoint) {

                geometries[i] = tranformMultiPoint((MultiPoint) geometry, transformation);

                // Linestring
            } else if (geometry instanceof LineString) {

                geometries[i] = tranformLineString((LineString) geometry, transformation);

                // MultiLineString
            } else if (geometry instanceof MultiLineString) {

                geometries[i] = tranformMultiLineString((MultiLineString) geometry, transformation);

            } else if (geometry instanceof Polygon) {

                geometries[i] = tranformPolygon((Polygon) geometry, transformation);

            } else if (geometry instanceof MultiPolygon) {

                geometries[i] = tranformMultiPolygon((MultiPolygon) geometry, transformation);

            }
        }

        return geometryFactory.createGeometryCollection(geometries);
    }

    /**
     * Transformation d'un tableau de coordonnées
     * 
     * @param coordinates
     * @param transformation
     * @return
     * @throws KettleException
     */
    private static Coordinate[] tranformCoordinates(Coordinate coordinates[], CoordinateOperation transformation) throws KettleException {

        Coordinate outCoordinates[] = new Coordinate[coordinates.length];
        for (int i = 0; i < coordinates.length; i++) {
            outCoordinates[i] = transformCoordinate(coordinates[i], transformation);
        }

        return outCoordinates;

    }

    /**
     * Transformation d'une coordonnée
     * 
     * @param coordinate
     * @param transformation
     * @return
     * @throws KettleException
     */
    private static Coordinate transformCoordinate(Coordinate coordinate, CoordinateOperation transformation) throws KettleException {

        try {

            double[] pIn;
            double[] pOut;

            pIn = new double[2];
            pIn[0] = coordinate.x;
            pIn[1] = coordinate.y;

            pOut = transformation.transform(pIn);

            if (!Double.isNaN(coordinate.z)) {
                return new Coordinate(pOut[0], pOut[1], coordinate.z);
            } else {
                return new Coordinate(pOut[0], pOut[1]);
            }

        } catch (IllegalCoordinateException e) {
            new KettleException(e);
        }

        return null;

    }

}
