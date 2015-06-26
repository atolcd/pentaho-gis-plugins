package com.atolcd.pentaho.di.trans.steps.gisrelate;

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

import org.apache.commons.lang.ArrayUtils;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import com.atolcd.pentaho.di.core.row.value.ValueMetaGeometry;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class GisRelate extends BaseStep implements StepInterface {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    private GisRelateData data;
    private GisRelateMeta meta;

    private String operator;

    private Integer firstGeometryFieldIndex;
    private Integer secondGeometryFieldIndex;
    private Integer distanceFieldIndex;
    private Double distanceValue;

    private String returnType;
    private Integer outputFieldIndex;

    private boolean withDistance;
    private Class<?> resultType;

    private Object getRelateResult(Object[] row) throws KettleException {

        Geometry firstGeometry = new ValueMetaGeometry().getGeometry(row[firstGeometryFieldIndex]);
        Geometry secondGeometry = new ValueMetaGeometry().getGeometry(row[secondGeometryFieldIndex]);

        if (!GeometryUtils.isNullOrEmptyGeometry(firstGeometry) && !GeometryUtils.isNullOrEmptyGeometry(secondGeometry)) {

            Double distance = null;

            if (withDistance) {

                if (distanceFieldIndex != null) {

                    distance = getInputRowMeta().getNumber(row, distanceFieldIndex);

                    if (distance == null) {
                        throw new KettleException("Distance can not be null");
                    }

                } else {
                    distance = distanceValue;
                }

            }

            return process(operator, firstGeometry, secondGeometry, distance);

        }

        return null;

    }

    public GisRelate(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
        super(s, stepDataInterface, c, t, dis);
    }

    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        meta = (GisRelateMeta) smi;
        data = (GisRelateData) sdi;

        Object result;

        Object[] r = getRow();

        if (r == null) {

            setOutputDone();
            return false;

        }

        if (first) {

            first = false;
            data.outputRowMeta = (RowMetaInterface) getInputRowMeta().clone();
            meta.getFields(data.outputRowMeta, getStepname(), null, null, this);

            operator = meta.getOperator();
            returnType = meta.getReturnType();

            // Récupération des indexes des colonnes contenant les géomrtries
            // d'entrée
            firstGeometryFieldIndex = getInputRowMeta().indexOfValue(meta.getFirstGeometryFieldName());
            secondGeometryFieldIndex = getInputRowMeta().indexOfValue(meta.getSecondGeometryFieldName());

            // Besoin de distance
            if (ArrayUtils.contains(meta.getWithDistanceOperators(), operator)) {

                withDistance = true;
                if (meta.isDynamicDistance()) {
                    distanceFieldIndex = getInputRowMeta().indexOfValue(meta.getDistanceFieldName());
                } else {

                    try {
                        distanceValue = Double.parseDouble(environmentSubstitute(meta.getDistanceValue()));
                    } catch (Exception e) {
                        throw new KettleException("Distance is not valid");
                    }
                }

            } else {
                withDistance = false;
            }

            // En fonction du type de résultat
            if (ArrayUtils.contains(meta.getBoolResultOperators(), operator)) {
                resultType = Boolean.class;
            } else if (ArrayUtils.contains(meta.getNumericResultOperators(), operator)) {
                resultType = Double.class;
            }

            // Récupération de l'index de la colonne contenant le résultat
            outputFieldIndex = data.outputRowMeta.indexOfValue(meta.getOutputFieldName());

            logBasic("Initialized successfully");

        }

        Object[] outputRow = null;
        result = getRelateResult(r);

        if (resultType.equals(Boolean.class)) {

            if (returnType.equalsIgnoreCase("ALL")) {

                outputRow = RowDataUtil.resizeArray(r, r.length + 1);
                outputRow[outputFieldIndex] = (Boolean) result;
                putRow(data.outputRowMeta, outputRow);

            } else {

                if (String.valueOf((Boolean) result).equalsIgnoreCase(returnType)) {
                    putRow(data.outputRowMeta, r);
                }

            }

        } else if (resultType.equals(Double.class)) {

            outputRow = RowDataUtil.resizeArray(r, r.length + 1);
            outputRow[outputFieldIndex] = (Double) result;
            putRow(data.outputRowMeta, outputRow);

        }

        incrementLinesInput();

        if (checkFeedback(getLinesRead())) {
            logBasic("Linenr " + getLinesRead());
        }

        return true;
    }

    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        meta = (GisRelateMeta) smi;
        data = (GisRelateData) sdi;
        return super.init(smi, sdi);
    }

    public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
        meta = (GisRelateMeta) smi;
        data = (GisRelateData) sdi;

        super.dispose(smi, sdi);
    }

    public void run() {
        logBasic("Starting to run...");
        try {
            while (processRow(meta, data) && !isStopped())
                ;
        } catch (Exception e) {
            logError("Unexpected error : " + e.toString());
            logError(Const.getStackTracker(e));
            setErrors(1);
            stopAll();
        } finally {
            dispose(meta, data);
            logBasic("Finished, processing " + getLinesRead() + " rows");
            markStop();
        }
    }

    private Object process(String operator, Geometry inGeometryA, Geometry inGeometryB, Double distance) throws KettleException {

        Object result = false;

        if (GeometryUtils.getSrid(inGeometryA).compareTo(GeometryUtils.getSrid(inGeometryB)) == 0) {

            if (operator.equalsIgnoreCase("CONTAINS")) {
                result = inGeometryA.contains(inGeometryB);

            } else if (operator.equalsIgnoreCase("COVERED_BY")) {
                result = inGeometryA.coveredBy(inGeometryB);

            } else if (operator.equalsIgnoreCase("COVERS")) {
                result = inGeometryA.covers(inGeometryB);

            } else if (operator.equalsIgnoreCase("CROSSES")) {
                result = inGeometryA.crosses(inGeometryB);

            } else if (operator.equalsIgnoreCase("DISJOINT")) {
                result = inGeometryA.disjoint(inGeometryB);

            } else if (operator.equalsIgnoreCase("EQUALS")) {
                result = inGeometryA.equals(inGeometryB);

            } else if (operator.equalsIgnoreCase("EQUALS_EXACT")) {
                result = inGeometryA.equalsExact(inGeometryB);

            } else if (operator.equalsIgnoreCase("INTERSECTS")) {
                result = inGeometryA.intersects(inGeometryB);

            } else if (operator.equalsIgnoreCase("WITHIN")) {
                result = inGeometryA.within(inGeometryB);

            } else if (operator.equalsIgnoreCase("IS_WITHIN_DISTANCE")) {

                distance = Math.abs(distance);
                result = inGeometryA.isWithinDistance(inGeometryB, distance);

            } else if (operator.equalsIgnoreCase("IS_NOT_WITHIN_DISTANCE")) {

                distance = Math.abs(distance);
                result = !inGeometryA.isWithinDistance(inGeometryB, distance);

            } else if (operator.equalsIgnoreCase("OVERLAPS")) {
                result = inGeometryA.overlaps(inGeometryB);

            } else if (operator.equalsIgnoreCase("TOUCHES")) {
                result = inGeometryA.touches(inGeometryB);

            } else if (operator.equalsIgnoreCase("DISTANCE_MIN")) {
                result = inGeometryA.distance(inGeometryB);

            } else if (operator.equalsIgnoreCase("DISTANCE_MAX")) {

                Double maxDistance = inGeometryA.distance(inGeometryB);
                Geometry geometry = geometryFactory.createGeometryCollection(new Geometry[] { inGeometryA, inGeometryB });
                Coordinate[] coords = geometry.convexHull().getCoordinates();

                for (Coordinate aCoordinate : coords) {

                    for (Coordinate bCoordinate : coords) {

                        double currenDistance = geometryFactory.createPoint(aCoordinate).distance(geometryFactory.createPoint(bCoordinate));
                        if (currenDistance > maxDistance) {
                            maxDistance = currenDistance;
                        }

                    }

                }

                result = maxDistance;

            } else {
                throw new IllegalArgumentException("Function \"" + operator + "\" is not allowed");
            }

        } else {
            throw new KettleException("Unauthorized mixed srids : " + GeometryUtils.getSrid(inGeometryA) + " with " + GeometryUtils.getSrid(inGeometryB));
        }

        return result;

    }

}
