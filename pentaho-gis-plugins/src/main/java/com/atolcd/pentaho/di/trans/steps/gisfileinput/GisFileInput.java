package com.atolcd.pentaho.di.trans.steps.gisfileinput;

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

import java.util.Iterator;

import com.atolcd.pentaho.di.gis.io.AbstractFileReader;
import com.atolcd.pentaho.di.gis.io.DXFReader;
import com.atolcd.pentaho.di.gis.io.GPXReader;
import com.atolcd.pentaho.di.gis.io.GeoJSONReader;
import com.atolcd.pentaho.di.gis.io.GeoPackageReader;
import com.atolcd.pentaho.di.gis.io.MapInfoReader;
import com.atolcd.pentaho.di.gis.io.ShapefileReader;
import com.atolcd.pentaho.di.gis.io.SpatialiteReader;
import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.FeatureConverter;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

public class GisFileInput extends BaseStep {

    private GisFileInputData data;
    private GisFileInputMeta meta;
    private AbstractFileReader fileReader;

    public GisFileInput(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
        super(s, stepDataInterface, c, t, dis);
    }

    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        meta = (GisFileInputMeta) smi;
        data = (GisFileInputData) sdi;

        if (first) {

            first = false;
            data.outputRowMeta = new RowMeta();
            meta.getFields(data.outputRowMeta, getStepname(), null, null, this);

            if (meta.getInputFormat().equalsIgnoreCase("ESRI_SHP")) {
                fileReader = new ShapefileReader(environmentSubstitute(meta.getInputFileName()), environmentSubstitute(meta.getGeometryFieldName()), meta.getEncoding());
            } else if (meta.getInputFormat().equalsIgnoreCase("GEOJSON")) {
                fileReader = new GeoJSONReader(environmentSubstitute(meta.getInputFileName()), environmentSubstitute(meta.getGeometryFieldName()), meta.getEncoding());
            } else if (meta.getInputFormat().equalsIgnoreCase("MAPINFO_MIF")) {
                fileReader = new MapInfoReader(environmentSubstitute(meta.getInputFileName()), environmentSubstitute(meta.getGeometryFieldName()), meta.getEncoding());
            } else if (meta.getInputFormat().equalsIgnoreCase("SPATIALITE")) {

                String tableName = environmentSubstitute((String) meta.getInputParameterValue("DB_TABLE_NAME"));
                fileReader = new SpatialiteReader(environmentSubstitute(meta.getInputFileName()), tableName, meta.getEncoding());

            } else if (meta.getInputFormat().equalsIgnoreCase("DXF")) {
                String readXData = environmentSubstitute((String) meta.getInputParameterValue("READ_XDATA"));
                String circleAsPolygon = environmentSubstitute((String) meta.getInputParameterValue("CIRCLE_AS_POLYGON"));
                String ellipseAsPolygon = environmentSubstitute((String) meta.getInputParameterValue("ELLIPSE_AS_POLYGON"));
                String lineAsPolygon = environmentSubstitute((String) meta.getInputParameterValue("LINE_AS_POLYGON"));

                fileReader = new DXFReader(
                    environmentSubstitute(meta.getInputFileName()),
                    environmentSubstitute(meta.getGeometryFieldName()),
                    meta.getEncoding(),
                    Boolean.parseBoolean(circleAsPolygon),
                    Boolean.parseBoolean(ellipseAsPolygon),
                    Boolean.parseBoolean(lineAsPolygon),
                    Boolean.parseBoolean(readXData)
                );
            } else if (meta.getInputFormat().equalsIgnoreCase("GPX")) {
            	fileReader = new GPXReader(environmentSubstitute(meta.getInputFileName()),
            		environmentSubstitute(meta.getGeometryFieldName()),
            		meta.getEncoding()
            	);

            } else if (meta.getInputFormat().equalsIgnoreCase("GEOPACKAGE")) {

            	fileReader = new GeoPackageReader(
        			environmentSubstitute(meta.getInputFileName()),
        			environmentSubstitute((String) meta.getInputParameterValue("DB_TABLE_NAME")),
        			environmentSubstitute(meta.getGeometryFieldName()),
        			meta.getEncoding()
        		);
            }

            String forceToMultigeometry = environmentSubstitute((String) meta.getInputParameterValue("FORCE_TO_MULTIGEOMETRY"));
            if (forceToMultigeometry != null) {
                fileReader.setForceToMultiGeometry(Boolean.parseBoolean(forceToMultigeometry));
            }

            String forceTo2D = environmentSubstitute((String) meta.getInputParameterValue("FORCE_TO_2D"));
            if (forceTo2D != null) {
                fileReader.setForceTo2DGeometry(Boolean.parseBoolean(forceTo2D));
            }

            fileReader.setLimit(meta.getRowLimit());
            incrementLinesInput();
            logBasic("Initialized successfully");

        }

        Iterator<Feature> featureIt = fileReader.getFeatures().iterator();
        while (featureIt.hasNext()) {

            putRow(data.outputRowMeta, FeatureConverter.getRow(data.outputRowMeta, featureIt.next()));
            incrementLinesOutput();

        }

        setOutputDone();
        return false;

    }

    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        meta = (GisFileInputMeta) smi;
        data = (GisFileInputData) sdi;

        return super.init(smi, sdi);
    }

    public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
        meta = (GisFileInputMeta) smi;
        data = (GisFileInputData) sdi;
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

}
