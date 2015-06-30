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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.w3c.dom.Node;

import com.atolcd.pentaho.di.gis.io.AbstractFileReader;
import com.atolcd.pentaho.di.gis.io.DXFReader;
import com.atolcd.pentaho.di.gis.io.GeoJSONReader;
import com.atolcd.pentaho.di.gis.io.MapInfoReader;
import com.atolcd.pentaho.di.gis.io.ShapefileReader;
import com.atolcd.pentaho.di.gis.io.SpatialiteReader;
import com.atolcd.pentaho.di.gis.io.features.FeatureConverter;
import com.atolcd.pentaho.di.ui.trans.steps.gisfileinput.GisFileInputDialog;

@Step(id = "GisFileInput", image = "com/atolcd/pentaho/di/ui/trans/steps/images/GisFileInput.png", name = "GisFileInput.Shell.Name", description = "GisFileInput.Shell.Description", categoryDescription = "GisFileInput.Shell.CategoryDescription", i18nPackageName = "com.atolcd.pentaho.di.trans.steps.gisfileinput")
public class GisFileInputMeta extends BaseStepMeta implements StepMetaInterface {

    private HashMap<String, GisInputFormatDef> inputFormatDefs;

    private String inputFormat;
    private List<GisInputFormatParameter> inputFormatParameters;
    private String inputFileName;
    private String geometryFieldName;
    private String encoding;
    private Long rowLimit;

    public GisFileInputMeta() {
        super();

        this.inputFormatDefs = new HashMap<String, GisInputFormatDef>();
        this.inputFormatParameters = new ArrayList<GisInputFormatParameter>();

        // ESRI Shapefile
        GisInputFormatDef shpDef = new GisInputFormatDef("ESRI_SHP", new String[] { "*.shp;*.SHP" }, new String[] { "*.shp" });
        shpDef.addParameterDef("FORCE_TO_2D", ValueMeta.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "TRUE");
        shpDef.addParameterDef("FORCE_TO_MULTIGEOMETRY", ValueMeta.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "FALSE");
        this.inputFormatDefs.put("ESRI_SHP", shpDef);

        // GeoJSON
        GisInputFormatDef geojsonDef = new GisInputFormatDef("GEOJSON", new String[] { "*.geojson;*.GEOJSON", "*.json;*.JSON" }, new String[] { "*.geojson", "*.json" });
        geojsonDef.addParameterDef("FORCE_TO_MULTIGEOMETRY", ValueMeta.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "FALSE");
        this.inputFormatDefs.put("GEOJSON", geojsonDef);

        // Mapinfo MIF/MID
        GisInputFormatDef mapinfoDef = new GisInputFormatDef("MAPINFO_MIF", new String[] { "*.mif;*.MIF" }, new String[] { "*.mif" });
        mapinfoDef.addParameterDef("FORCE_TO_MULTIGEOMETRY", ValueMeta.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "FALSE");
        this.inputFormatDefs.put("MAPINFO_MIF", mapinfoDef);

        // SpatialLite
        GisInputFormatDef sqlLiteDef = new GisInputFormatDef("SPATIALITE", new String[] { "*.db;*.DB", "*.sqlite;*.SQLITE" }, new String[] { "*.db", "*.sqlite" });
        sqlLiteDef.addParameterDef("DB_TABLE_NAME", ValueMeta.TYPE_STRING, true);
        this.inputFormatDefs.put("SPATIALITE", sqlLiteDef);

        // DXF
        GisInputFormatDef dxfDef = new GisInputFormatDef("DXF", new String[] { "*.dxf;*.DXF" }, new String[] { "*.dxf" });
        dxfDef.addParameterDef("FORCE_TO_MULTIGEOMETRY", ValueMeta.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "FALSE");
        dxfDef.addParameterDef("CIRCLE_AS_POLYGON", ValueMeta.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "FALSE");
        dxfDef.addParameterDef("ELLIPSE_AS_POLYGON", ValueMeta.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "FALSE");
        dxfDef.addParameterDef("LINE_AS_POLYGON", ValueMeta.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "FALSE");
        this.inputFormatDefs.put("DXF", dxfDef);
    }

    public List<GisInputFormatParameter> getInputFormatParameters() {
        return inputFormatParameters;
    }

    public void setInputFormatParameters(List<GisInputFormatParameter> inputFormatParameters) {
        this.inputFormatParameters = inputFormatParameters;
    }

    public HashMap<String, GisInputFormatDef> getInputFormatDefs() {
        return inputFormatDefs;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String getInputFileName() {
        return inputFileName;
    }

    public void setInputFileName(String inputFileName) {
        this.inputFileName = inputFileName;
    }

    public String getGeometryFieldName() {
        return geometryFieldName;
    }

    public void setGeometryFieldName(String geometryFieldName) {
        this.geometryFieldName = geometryFieldName;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public Long getRowLimit() {
        return rowLimit;
    }

    public void setRowLimit(Long rowLimit) {
        this.rowLimit = rowLimit;
    }

    public String getXML() {

        StringBuffer retval = new StringBuffer();
        retval.append("\t" + XMLHandler.addTagValue("inputFormat", inputFormat));

        // Paramètres
        retval.append("\t<params>").append(Const.CR);
        for (GisInputFormatParameter parameter : inputFormatParameters) {

            String key = parameter.getKey();
            String value = (String) parameter.getValue();

            retval.append("\t\t<param>").append(Const.CR);
            retval.append("\t\t\t").append(XMLHandler.addTagValue("key", key));
            retval.append("\t\t\t").append(XMLHandler.addTagValue("value", value));
            retval.append("\t\t</param>").append(Const.CR);

        }

        retval.append("\t</params>").append(Const.CR);

        retval.append("    " + XMLHandler.addTagValue("inputFileName", inputFileName));
        retval.append("    " + XMLHandler.addTagValue("geometryFieldName", geometryFieldName));
        retval.append("    " + XMLHandler.addTagValue("encoding", encoding));
        retval.append("    " + XMLHandler.addTagValue("rowLimit", rowLimit));

        return retval.toString();

    }

    public void getFields(RowMetaInterface r, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) {

        Charset charset;
        try {
            charset = Charset.forName(encoding);
        } catch (Exception e) {
            charset = Charset.defaultCharset();
        }

        try {

            AbstractFileReader fileReader = null;

            if (inputFormat.equalsIgnoreCase("ESRI_SHP")) {
                fileReader = new ShapefileReader(space.environmentSubstitute(inputFileName), space.environmentSubstitute(geometryFieldName), charset.displayName());
            } else if (inputFormat.equalsIgnoreCase("GEOJSON")) {
                fileReader = new GeoJSONReader(space.environmentSubstitute(inputFileName), space.environmentSubstitute(geometryFieldName), charset.displayName());
            } else if (inputFormat.equalsIgnoreCase("MAPINFO_MIF")) {
                fileReader = new MapInfoReader(space.environmentSubstitute(inputFileName), space.environmentSubstitute(geometryFieldName), charset.displayName());
            } else if (inputFormat.equalsIgnoreCase("SPATIALITE")) {
                String tableName = space.environmentSubstitute((String) getInputParameterValue("DB_TABLE_NAME"));
                fileReader = new SpatialiteReader(space.environmentSubstitute(inputFileName), tableName, charset.displayName());
            } else if (inputFormat.equalsIgnoreCase("DXF")) {
                fileReader = new DXFReader(space.environmentSubstitute(inputFileName), space.environmentSubstitute(geometryFieldName), charset.displayName(), false, false, false);
            }
            r.addRowMeta(FeatureConverter.getRowMeta(fileReader.getFields(), origin));
        } catch (KettleException e) {
            e.printStackTrace();
        }
    }

    public List<String> getParameterPredefinedValues(String formatKey, String parameterKey) {
        List<String> predefinedValues = inputFormatDefs.get(formatKey).getParameterDef(parameterKey).getPredefinedValues();
        Collections.sort(predefinedValues);
        return predefinedValues;
    }

    public int getParameterValueMetaType(String formatKey, String parameterKey) {

        return inputFormatDefs.get(formatKey).getParameterDef(parameterKey).getValueMetaType();
    }

    public Object getInputParameterValue(String parameterKey) {

        for (GisInputFormatParameter parameter : inputFormatParameters) {

            if (parameter.getKey().equalsIgnoreCase(parameterKey)) {
                return parameter.getValue();
            }

        }

        return null;
    }

    public Object clone() {

        Object retval = super.clone();
        return retval;

    }

    public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {

        try {

            inputFormat = XMLHandler.getTagValue(stepnode, "inputFormat");
            Node paramsNode = XMLHandler.getSubNode(stepnode, "params");
            for (int i = 0; i < XMLHandler.countNodes(paramsNode, "param"); i++) {

                Node paramNode = XMLHandler.getSubNodeByNr(paramsNode, "param", i);
                String key = XMLHandler.getTagValue(paramNode, "key");
                String value = XMLHandler.getTagValue(paramNode, "value");

                inputFormatParameters.add(new GisInputFormatParameter(key, value));

            }

            inputFileName = XMLHandler.getTagValue(stepnode, "inputFileName");
            geometryFieldName = XMLHandler.getTagValue(stepnode, "geometryFieldName");
            encoding = XMLHandler.getTagValue(stepnode, "encoding");
            rowLimit = Long.valueOf(XMLHandler.getTagValue(stepnode, "rowLimit"));

        } catch (Exception e) {
            throw new KettleXMLException("Unable to read step info from XML node", e);
        }

    }

    public void setDefault() {

        inputFormat = "ESRI_SHP";
        rowLimit = (long) 0;
    }

    public void check(List<CheckResultInterface> remarks, TransMeta transmeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info) {

        CheckResult cr;

        if (input.length > 0) {

            cr = new CheckResult(CheckResult.TYPE_RESULT_OK, "Step is receiving info from other steps.", stepMeta);
            remarks.add(cr);

        } else {

            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, "No input received from other steps.", stepMeta);
            remarks.add(cr);

        }

    }

    public StepDialogInterface getDialog(Shell shell, StepMetaInterface meta, TransMeta transMeta, String name) {
        return new GisFileInputDialog(shell, meta, transMeta, name);
    }

    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans disp) {
        return new GisFileInput(stepMeta, stepDataInterface, cnr, transMeta, disp);
    }

    public StepDataInterface getStepData() {
        return new GisFileInputData();
    }

    public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {

        try {

            inputFormat = rep.getStepAttributeString(id_step, "inputFormat");

            for (int i = 0; i < rep.countNrStepAttributes(id_step, "param_key"); i++) {

                String key = rep.getStepAttributeString(id_step, i, "param_key");
                String value = rep.getStepAttributeString(id_step, i, "param_value");

                inputFormatParameters.add(new GisInputFormatParameter(key, value));
            }

            inputFileName = rep.getStepAttributeString(id_step, "inputFileName");
            geometryFieldName = rep.getStepAttributeString(id_step, "geometryFieldName");
            encoding = rep.getStepAttributeString(id_step, "encoding");
            rowLimit = rep.getStepAttributeInteger(id_step, "rowLimit");

        } catch (Exception e) {

            throw new KettleXMLException("Unable to read step info from repository", e);
        }
    }

    public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException {

        try {

            rep.saveStepAttribute(id_transformation, id_step, "inputFormat", inputFormat);

            int i = 0;
            for (GisInputFormatParameter parameter : inputFormatParameters) {

                String key = parameter.getKey();
                String value = (String) parameter.getValue();

                rep.saveStepAttribute(id_transformation, id_step, i, "param_key", key);
                rep.saveStepAttribute(id_transformation, id_step, i, "param_value", value);

                i++;
            }

            rep.saveStepAttribute(id_transformation, id_step, "inputFileName", inputFileName);
            rep.saveStepAttribute(id_transformation, id_step, "geometryFieldName", geometryFieldName);
            rep.saveStepAttribute(id_transformation, id_step, "encoding", encoding);
            rep.saveStepAttribute(id_transformation, id_step, "rowLimit", rowLimit);

        } catch (Exception e) {

            throw new KettleXMLException("Unable to write step info in repository", e);

        }
    }
}
