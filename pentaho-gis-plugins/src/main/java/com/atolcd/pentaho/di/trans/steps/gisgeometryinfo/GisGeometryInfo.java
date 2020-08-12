package com.atolcd.pentaho.di.trans.steps.gisgeometryinfo;

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

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.atolcd.pentaho.di.core.row.value.ValueMetaGeometry;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Geometry;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

public class GisGeometryInfo extends BaseStep {

    private GisGeometryInfoData data;
    private GisGeometryInfoMeta meta;

    private Integer geometryFieldIndex;
    private LinkedHashMap<String, Integer> outputMap = new LinkedHashMap<String, Integer>();

    public GisGeometryInfo(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
        super(s, stepDataInterface, c, t, dis);
    }

    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        meta = (GisGeometryInfoMeta) smi;
        data = (GisGeometryInfoData) sdi;

        Object[] r = getRow();

        if (r == null) {

            setOutputDone();
            return false;

        }

        if (first) {

            first = false;
            data.outputRowMeta = (RowMetaInterface) getInputRowMeta().clone();
            meta.getFields(data.outputRowMeta, getStepname(), null, null, this);

            // Récupération de l'index de la colonne contenant la géométrie
            // d'entrée
            geometryFieldIndex = getInputRowMeta().indexOfValue(meta.getGeometryFieldName());

            // Récupération des infos demandées et des index des colonnes
            // resultats
            for (Entry<String, String> output : meta.getOutputFields().entrySet()) {
                outputMap.put(output.getKey(), data.outputRowMeta.indexOfValue(output.getValue()));
            }

            logBasic("Initialized successfully");

        }

        putRow(data.outputRowMeta, getOutputInfoRow(r));

        incrementLinesInput();
        if (checkFeedback(getLinesRead())) {
            logBasic("Linenr " + getLinesRead());
        }

        return true;

    }

    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        meta = (GisGeometryInfoMeta) smi;
        data = (GisGeometryInfoData) sdi;

        return super.init(smi, sdi);
    }

    public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
        meta = (GisGeometryInfoMeta) smi;
        data = (GisGeometryInfoData) sdi;
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

    private Object[] getOutputInfoRow(Object[] row) throws KettleException {

        Object[] newRow = RowDataUtil.resizeArray(row, row.length + outputMap.size());

        Geometry geometry = new ValueMetaGeometry().getGeometry(row[geometryFieldIndex]);
        Object value = null;

        for (Entry<String, Integer> output : outputMap.entrySet()) {

            String infoKey = output.getKey();
            Integer fieldIndex = output.getValue();

            if (infoKey.equalsIgnoreCase("NULL_OR_EMPTY")) {
                value = GeometryUtils.isNullOrEmptyGeometry(geometry);

            } else if (infoKey.equalsIgnoreCase("AREA")) {
                value = GeometryUtils.getArea(geometry);

            } else if (infoKey.equalsIgnoreCase("LENGTH")) {
                value = GeometryUtils.getLength(geometry);

            } else if (infoKey.equalsIgnoreCase("DIMENSION")) {
                value = getIntegerAsLong(GeometryUtils.getCoordinateDimension(geometry));

            } else if (infoKey.equalsIgnoreCase("SRID")) {
                value = getIntegerAsLong(GeometryUtils.getSrid(geometry));

            } else if (infoKey.equalsIgnoreCase("GEOMETRY_TYPE")) {
                value = GeometryUtils.getGeometryType(geometry);

            } else if (infoKey.equalsIgnoreCase("GEOMETRY_COUNT")) {
                value = getIntegerAsLong(GeometryUtils.getGeometriesCount(geometry));

            } else if (infoKey.equalsIgnoreCase("GEOMETRY_VERTEX_COUNT")) {
                value = getIntegerAsLong(GeometryUtils.getCoordinatesCount(geometry));

            } else if (infoKey.equalsIgnoreCase("X_MIN")) {
                value = GeometryUtils.getMinX(geometry);

            } else if (infoKey.equalsIgnoreCase("X_MAX")) {
                value = GeometryUtils.getMaxX(geometry);

            } else if (infoKey.equalsIgnoreCase("Y_MIN")) {
                value = GeometryUtils.getMinY(geometry);

            } else if (infoKey.equalsIgnoreCase("Y_MAX")) {
                value = GeometryUtils.getMaxY(geometry);

            } else if (infoKey.equalsIgnoreCase("Z_MIN")) {
                value = GeometryUtils.getMinZ(geometry);

            } else if (infoKey.equalsIgnoreCase("Z_MAX")) {
                value = GeometryUtils.getMaxZ(geometry);

            }

            newRow[fieldIndex] = value;
        }

        return newRow;

    }

    private static Long getIntegerAsLong(Integer value) {

        if (value != null) {
            return new Long(value);
        }
        return null;
    }

}
