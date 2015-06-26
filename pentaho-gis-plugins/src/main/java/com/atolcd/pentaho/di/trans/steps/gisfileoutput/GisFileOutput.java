package com.atolcd.pentaho.di.trans.steps.gisfileoutput;

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

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import com.atolcd.pentaho.di.gis.io.AbstractFileWriter;
import com.atolcd.pentaho.di.gis.io.DXFWriter;
import com.atolcd.pentaho.di.gis.io.GeoJSONWriter;
import com.atolcd.pentaho.di.gis.io.KMLWriter;
import com.atolcd.pentaho.di.gis.io.ShapefileWriter;
import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.FeatureConverter;

public class GisFileOutput extends BaseStep implements StepInterface {

    private GisFileOutputData data;
    private GisFileOutputMeta meta;

    private List<Feature> gisFeatures = new ArrayList<Feature>();

    public GisFileOutput(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
        super(s, stepDataInterface, c, t, dis);
    }

    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        meta = (GisFileOutputMeta) smi;
        data = (GisFileOutputData) sdi;

        Object[] r = getRow();

        // Ecriture des fichiers à partir de la collection de features
        if (r == null) {

            AbstractFileWriter fileWriter = null;

            // ESRI_SHP
            if (meta.getOutputFormat().equalsIgnoreCase("ESRI_SHP")) {

                fileWriter = new ShapefileWriter(environmentSubstitute(meta.getOutputFileName()), meta.getGeometryFieldName(), meta.getEncoding());

                // Forcer en 2D
                String forceTo2D = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIXED, "FORCE_TO_2D"));
                if (forceTo2D != null) {
                    ((ShapefileWriter) fileWriter).setForceTo2DGeometry(Boolean.parseBoolean(forceTo2D));
                }

                // PRJ
                String withPRJ = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIXED, "ESRI_SHP_CREATE_PRJ"));
                if (withPRJ != null) {
                    ((ShapefileWriter) fileWriter).setCreatePrjFile(Boolean.parseBoolean(withPRJ));
                }

                // GEOJSON
            } else if (meta.getOutputFormat().equalsIgnoreCase("GEOJSON")) {

                if (meta.isDataToServlet()) {
                    Writer writer = getTrans().getServletPrintWriter();
                    fileWriter = new GeoJSONWriter(writer, meta.getGeometryFieldName(), meta.getEncoding());

                } else {
                    fileWriter = new GeoJSONWriter(environmentSubstitute(meta.getOutputFileName()), meta.getGeometryFieldName(), meta.getEncoding());
                }

                // Exporter id
                String featureIdField = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIELD, "GEOJSON_FEATURE_ID"));
                if (featureIdField != null && !featureIdField.isEmpty()) {
                    ((GeoJSONWriter) fileWriter).setFeatureIdField(featureIdField);
                }

                // KML
            } else if (meta.getOutputFormat().equalsIgnoreCase("KML")) {

                if (meta.isDataToServlet()) {
                    Writer writer = getTrans().getServletPrintWriter();
                    fileWriter = new KMLWriter(writer, meta.getGeometryFieldName(), meta.getEncoding());

                } else {
                    fileWriter = new KMLWriter(environmentSubstitute(meta.getOutputFileName()), meta.getGeometryFieldName(), meta.getEncoding());
                }

                // Forcer en 2D
                String forceTo2D = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIXED, "FORCE_TO_2D"));
                if (forceTo2D != null) {
                    ((KMLWriter) fileWriter).setForceTo2DGeometry(Boolean.parseBoolean(forceTo2D));
                }

                // Exporter les attributs
                String withAttributs = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIXED, "KML_EXPORT_ATTRIBUTS"));
                if (withAttributs != null) {
                    ((KMLWriter) fileWriter).setExportWithAttributs(Boolean.parseBoolean(withAttributs));
                }

                // Exporter nom <Document>
                String docName = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIXED, "KML_DOC_NAME"));
                if (docName != null) {
                    ((KMLWriter) fileWriter).setDocumentName(docName);
                }

                // Exporter description <Document>
                String docDescription = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIXED, "KML_DOC_DESCRIPTION"));
                if (docDescription != null) {
                    ((KMLWriter) fileWriter).setDocumentDescription(docDescription);
                }

                // Exporter nom <Placemark>
                String featureNameField = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIELD, "KML_PLACEMARK_NAME"));
                if (featureNameField != null && !featureNameField.isEmpty()) {
                    ((KMLWriter) fileWriter).setFeatureNameField(featureNameField);
                }

                // Exporter description <Placemark>
                String featureDescriptionField = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIELD, "KML_PLACEMARK_DESCRIPTION"));
                if (featureDescriptionField != null && !featureNameField.isEmpty()) {
                    ((KMLWriter) fileWriter).setFeatureDescriptionField(featureDescriptionField);
                }

                // DXF
            } else if (meta.getOutputFormat().equalsIgnoreCase("DXF")) {

                String layerName = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIXED, "DXF_LAYER_NAME"));
                fileWriter = new DXFWriter(environmentSubstitute(meta.getOutputFileName()), layerName, meta.getGeometryFieldName(), meta.getEncoding());

                String layerNameFieldName = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIELD, "DXF_FEATURE_LAYER_NAME"));
                if (layerNameFieldName != null && !layerNameFieldName.isEmpty()) {
                    ((DXFWriter) fileWriter).setLayerNameFieldName(layerNameFieldName);
                }

                String precision = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIXED, "DXF_COORD_PRECISION"));
                if (precision != null) {
                    ((DXFWriter) fileWriter).setPrecision(Integer.parseInt(precision));
                }

                // Forcer en 2D
                String forceTo2D = environmentSubstitute((String) meta.getInputParameterValue(GisOutputFormatParameterDef.TYPE_FIXED, "FORCE_TO_2D"));
                if (forceTo2D != null) {
                    ((DXFWriter) fileWriter).setForceTo2DGeometry(Boolean.parseBoolean(forceTo2D));
                }

            }

            fileWriter.setFields(FeatureConverter.getFields(getTransMeta().getPrevStepFields(getStepMeta())));

            // Création contextuelle du fichier
            if ((gisFeatures.size() == 0 && !meta.isCreateFileAtEnd()) || gisFeatures.size() > 0) {

                logBasic("Write file - Start");
                fileWriter.writeFeatures(gisFeatures);
                logBasic("Write file - End");

            }

            setOutputDone();
            return false;
        }

        if (first) {

            first = false;
            data.outputRowMeta = (RowMetaInterface) getInputRowMeta().clone();
            meta.getFields(data.outputRowMeta, getStepname(), null, null, this);
            logBasic("Initialized successfully");

        }

        // Nouvelle feature pour chaque ligne
        gisFeatures.add(FeatureConverter.getFeature(data.outputRowMeta, r));
        putRow(data.outputRowMeta, r);

        if (checkFeedback(getLinesRead())) {
            logBasic("Linenr " + getLinesRead());
        }

        return true;
    }

    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        meta = (GisFileOutputMeta) smi;
        data = (GisFileOutputData) sdi;
        return super.init(smi, sdi);
    }

    public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
        meta = (GisFileOutputMeta) smi;
        data = (GisFileOutputData) sdi;

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
