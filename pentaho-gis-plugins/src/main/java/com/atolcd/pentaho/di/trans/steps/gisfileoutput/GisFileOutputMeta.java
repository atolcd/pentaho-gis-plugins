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
import org.pentaho.di.core.row.value.ValueMetaBase;
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

import com.atolcd.pentaho.di.ui.trans.steps.gisfileoutput.GisFileOutputDialog;

@Step(id = "GisFileOutput", image = "com/atolcd/pentaho/di/ui/trans/steps/images/GisFileOutput.png", name = "GisFileOutput.Shell.Name", description = "GisFileOutput.Shell.Description", categoryDescription = "GisFileOutput.Shell.CategoryDescription", i18nPackageName = "com.atolcd.pentaho.di.trans.steps.gisfileoutput")
public class GisFileOutputMeta extends BaseStepMeta implements StepMetaInterface {

    private HashMap<String, GisOutputFormatDef> outputFormatDefs;
    private String outputFormat;
    private List<GisOutputFormatParameter> outputFormatFieldParameters;
    private List<GisOutputFormatParameter> outputFormatFixedParameters;
    private String outputFileName;
    private String geometryFieldName;
    private String encoding;
    private boolean createFileAtEnd;
    private boolean dataToServlet;

    public GisFileOutputMeta() {
        super();

        this.outputFormatDefs = new HashMap<String, GisOutputFormatDef>();
        this.outputFormatFieldParameters = new ArrayList<GisOutputFormatParameter>();
        this.outputFormatFixedParameters = new ArrayList<GisOutputFormatParameter>();

        // ESRI Shapefile
        GisOutputFormatDef shpDef = new GisOutputFormatDef("ESRI_SHP", new String[] { "*.shp;*.SHP" }, new String[] { "*.shp" });
        shpDef.addParameterFixedDef("FORCE_TO_2D", ValueMetaBase.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "TRUE");
        shpDef.addParameterFixedDef("ESRI_SHP_CREATE_PRJ", ValueMetaBase.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "TRUE");
        this.outputFormatDefs.put("ESRI_SHP", shpDef);

        // GeoJSON
        GisOutputFormatDef geojsonDef = new GisOutputFormatDef("GEOJSON", new String[] { "*.geojson;*.GEOJSON", "*.json;*.JSON" }, new String[] { "*.geojson", "*.json" });
        geojsonDef.addParameterFieldDef("GEOJSON_FEATURE_ID", ValueMetaBase.TYPE_STRING, false);
        this.outputFormatDefs.put("GEOJSON", geojsonDef);

        // Keyhole Markup LanguageKML
        GisOutputFormatDef kmlDef = new GisOutputFormatDef("KML", new String[] { "*.kml;*.KML" }, new String[] { "*.kml" });
        kmlDef.addParameterFixedDef("FORCE_TO_2D", ValueMetaBase.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "TRUE");
        kmlDef.addParameterFixedDef("KML_DOC_NAME", ValueMetaBase.TYPE_STRING, false);
        kmlDef.addParameterFixedDef("KML_DOC_DESCRIPTION", ValueMetaBase.TYPE_STRING, false);
        kmlDef.addParameterFixedDef("KML_EXPORT_ATTRIBUTS", ValueMetaBase.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "FALSE");
        kmlDef.addParameterFieldDef("KML_PLACEMARK_NAME", ValueMetaBase.TYPE_STRING, false);
        kmlDef.addParameterFieldDef("KML_PLACEMARK_DESCRIPTION", ValueMetaBase.TYPE_STRING, false);
        this.outputFormatDefs.put("KML", kmlDef);

        // DXF
        GisOutputFormatDef dxfDef = new GisOutputFormatDef("DXF", new String[] { "*.dxf;*.DXF" }, new String[] { "*.dxf" });
        dxfDef.addParameterFixedDef("FORCE_TO_2D", ValueMetaBase.TYPE_BOOLEAN, true, Arrays.asList(new String[] { "TRUE", "FALSE" }), "TRUE");
        dxfDef.addParameterFixedDef("DXF_LAYER_NAME", ValueMetaBase.TYPE_STRING, true, null, "0");
        dxfDef.addParameterFixedDef("DXF_COORD_PRECISION", ValueMetaBase.TYPE_INTEGER, true, null, "5");
        dxfDef.addParameterFieldDef("DXF_FEATURE_LAYER_NAME", ValueMetaBase.TYPE_STRING, false);
        this.outputFormatDefs.put("DXF", dxfDef);

        // Gps exchange Format GPX
        GisOutputFormatDef gpxDef = new GisOutputFormatDef("GPX", new String[] { "*.gpx;*.GPX" }, new String[] { "*.gpx" });
        gpxDef.addParameterFixedDef("GPX_VERSION", ValueMetaBase.TYPE_STRING, true, Arrays.asList(new String[] { "1.0", "1.1" }), "1.1");
        gpxDef.addParameterFixedDef("GPX_META_NAME", ValueMetaBase.TYPE_STRING, false);
        gpxDef.addParameterFixedDef("GPX_META_DESCRIPTION", ValueMetaBase.TYPE_STRING, false);
        gpxDef.addParameterFixedDef("GPX_META_AUTHOR_NAME", ValueMetaBase.TYPE_STRING, false);
        gpxDef.addParameterFixedDef("GPX_META_AUTHOR_EMAIL", ValueMetaBase.TYPE_STRING, false);
        gpxDef.addParameterFixedDef("GPX_META_KEYWORDS", ValueMetaBase.TYPE_STRING, false);
        gpxDef.addParameterFixedDef("GPX_META_DATETIME", ValueMetaBase.TYPE_DATE, false);
        gpxDef.addParameterFieldDef("GPX_FEATURE_NAME", ValueMetaBase.TYPE_STRING, false);
        gpxDef.addParameterFieldDef("GPX_FEATURE_DESCRIPTION", ValueMetaBase.TYPE_STRING, false);
        this.outputFormatDefs.put("GPX", gpxDef);
         
        // GeoPackage
        GisOutputFormatDef gpkgDef = new GisOutputFormatDef("GEOPACKAGE", new String[] { "*.gpkg;*.GPKG"}, new String[] {"*.gpkg"});
        gpkgDef.addParameterFixedDef("REPLACE_FILE", ValueMetaBase.TYPE_BOOLEAN, true, Arrays.asList(new String[] {"TRUE", "FALSE"}), "TRUE");
        gpkgDef.addParameterFixedDef("DB_TABLE_NAME", ValueMetaBase.TYPE_STRING, true);
        gpkgDef.addParameterFixedDef("DB_TABLE_COMMIT_LIMIT", ValueMetaBase.TYPE_INTEGER, true,null,"1000");
        gpkgDef.addParameterFixedDef("REPLACE_TABLE", ValueMetaBase.TYPE_BOOLEAN, true, Arrays.asList(new String[] {"TRUE", "FALSE"}), "FALSE");
        gpkgDef.addParameterFixedDef("GPKG_CONTENTS_IDENTIFIER", ValueMetaBase.TYPE_STRING, false);
        gpkgDef.addParameterFixedDef("GPKG_CONTENTS_DESCRIPTION", ValueMetaBase.TYPE_STRING, false);
        gpkgDef.addParameterFixedDef("GPKG_GEOMETRY_SRID", ValueMetaBase.TYPE_INTEGER, false);
        gpkgDef.addParameterFixedDef("GPKG_GEOMETRY_GEOMETRYTYPE", ValueMetaBase.TYPE_STRING, false, Arrays.asList(new String[] {"POINT", "LINESTRING", "POLYGON","MULTIPOINT", "MULTILINESTRING", "MULTIPOLYGON","GEOMETRY"}), "GEOMETRY");
        gpkgDef.addParameterFixedDef("FORCE_TO_2D", ValueMetaBase.TYPE_BOOLEAN, true, Arrays.asList(new String[] {"TRUE", "FALSE"}), "TRUE");
        gpkgDef.addParameterFieldDef("DB_TABLE_PK_FIELD", ValueMetaBase.TYPE_INTEGER, true);
        this.outputFormatDefs.put("GEOPACKAGE", gpkgDef);
        
        // Scalable Vector Graphics SVG
        GisOutputFormatDef svgDef = new GisOutputFormatDef("SVG", new String[] { "*.svg;*.SVG" }, new String[] { "*.svg" });
        svgDef.addParameterFixedDef("SVG_DOC_WIDTH", ValueMetaBase.TYPE_INTEGER, true, null, "1000");
        svgDef.addParameterFixedDef("SVG_DOC_HEIGHT", ValueMetaBase.TYPE_INTEGER, true,null, "1000");
        svgDef.addParameterFixedDef("SVG_DOC_COORD_PRECISION", ValueMetaBase.TYPE_INTEGER, true, null, "5");
        svgDef.addParameterFixedDef("SVG_DOC_TITLE", ValueMetaBase.TYPE_STRING, false);
        svgDef.addParameterFixedDef("SVG_DOC_DESCRIPTION", ValueMetaBase.TYPE_STRING, false);
        svgDef.addParameterFixedDef("SVG_DOC_STYLESHEET", ValueMetaBase.TYPE_STRING, false);
        svgDef.addParameterFixedDef("SVG_DOC_STYLESHEET_MODE", ValueMetaBase.TYPE_STRING, false, Arrays.asList(new String[] {"RESSOURCE_EXTERNAL", "RESSOURCE_EMBEDDED"}), "RESSOURCE_EXTERNAL");
        svgDef.addParameterFixedDef("SVG_DOC_SYMBOL_MODE", ValueMetaBase.TYPE_STRING, false, Arrays.asList(new String[] {"RESSOURCE_EXTERNAL", "RESSOURCE_EMBEDDED"}), "RESSOURCE_EXTERNAL");
        svgDef.addParameterFieldDef("SVG_FEATURE_ID", ValueMetaBase.TYPE_STRING, false);
        svgDef.addParameterFieldDef("SVG_FEATURE_TITLE", ValueMetaBase.TYPE_STRING, false);
        svgDef.addParameterFieldDef("SVG_FEATURE_DESCRIPTION", ValueMetaBase.TYPE_STRING, false);
        svgDef.addParameterFieldDef("SVG_FEATURE_STYLE", ValueMetaBase.TYPE_STRING, false);
        svgDef.addParameterFieldDef("SVG_FEATURE_CLASS", ValueMetaBase.TYPE_STRING, false);
        svgDef.addParameterFieldDef("SVG_FEATURE_SYMBOL", ValueMetaBase.TYPE_STRING, false);
        svgDef.addParameterFieldDef("SVG_FEATURE_LABEL", ValueMetaBase.TYPE_STRING, false);
        svgDef.addParameterFieldDef("SVG_FEATURE_GROUP", ValueMetaBase.TYPE_INTEGER, false);
        this.outputFormatDefs.put("SVG", svgDef);
    }

    public HashMap<String, GisOutputFormatDef> getOutputFormatDefs() {
        return outputFormatDefs;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public List<GisOutputFormatParameter> getOutputFormatFieldParameters() {
        return outputFormatFieldParameters;
    }

    public void setOutputFormatFieldParameters(List<GisOutputFormatParameter> outputFormatFieldParameters) {
        this.outputFormatFieldParameters = outputFormatFieldParameters;
    }

    public List<GisOutputFormatParameter> getOutputFormatFixedParameters() {
        return outputFormatFixedParameters;
    }

    public void setOutputFormatFixedParameters(List<GisOutputFormatParameter> outputFormatFixedParameters) {
        this.outputFormatFixedParameters = outputFormatFixedParameters;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
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

    public boolean isCreateFileAtEnd() {
        return createFileAtEnd;
    }

    public void setCreateFileAtEnd(boolean createFileAtEnd) {
        this.createFileAtEnd = createFileAtEnd;
    }

    public boolean isDataToServlet() {
        return dataToServlet;
    }

    public void setDataToServlet(boolean dataToServlet) {
        this.dataToServlet = dataToServlet;
    }

    public String getXML() {

        StringBuffer retval = new StringBuffer();
        retval.append("\t" + XMLHandler.addTagValue("outputFormat", outputFormat));

        // Paramètres de champs
        retval.append("\t<fieldParams>").append(Const.CR);
        for (GisOutputFormatParameter parameter : outputFormatFieldParameters) {

            String key = parameter.getKey();
            String value = (String) parameter.getValue();

            retval.append("\t\t<param>").append(Const.CR);
            retval.append("\t\t\t").append(XMLHandler.addTagValue("key", key));
            retval.append("\t\t\t").append(XMLHandler.addTagValue("value", value));
            retval.append("\t\t</param>").append(Const.CR);

        }

        retval.append("\t</fieldParams>").append(Const.CR);

        // Paramètres fixes
        retval.append("\t<fixedParams>").append(Const.CR);
        for (GisOutputFormatParameter parameter : outputFormatFixedParameters) {

            String key = parameter.getKey();
            String value = (String) parameter.getValue();

            retval.append("\t\t<param>").append(Const.CR);
            retval.append("\t\t\t").append(XMLHandler.addTagValue("key", key));
            retval.append("\t\t\t").append(XMLHandler.addTagValue("value", value));
            retval.append("\t\t</param>").append(Const.CR);

        }

        retval.append("\t</fixedParams>").append(Const.CR);

        retval.append("    " + XMLHandler.addTagValue("outputFileName", outputFileName));
        retval.append("    " + XMLHandler.addTagValue("geometryFieldName", geometryFieldName));
        retval.append("    " + XMLHandler.addTagValue("encoding", encoding));
        retval.append("    " + XMLHandler.addTagValue("createFileAtEnd", createFileAtEnd));
        retval.append("    " + XMLHandler.addTagValue("dataToServlet", dataToServlet));

        return retval.toString();

    }

    public void getFields(RowMetaInterface r, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) {

    }

    public List<String> getParameterPredefinedValues(String formatKey, String parameterType, String parameterKey) {

        List<String> predefinedValues = null;

        if (parameterType.equalsIgnoreCase(GisOutputFormatParameterDef.TYPE_FIELD)) {

            predefinedValues = outputFormatDefs.get(formatKey).getParameterFieldDef(parameterKey).getPredefinedValues();

        } else if (parameterType.equalsIgnoreCase(GisOutputFormatParameterDef.TYPE_FIXED)) {

            predefinedValues = outputFormatDefs.get(formatKey).getParameterFixedDef(parameterKey).getPredefinedValues();

        }

        Collections.sort(predefinedValues);
        return predefinedValues;

    }

    public boolean isParameterValueRequired(String formatKey, String parameterType, String parameterKey) {

        boolean required = false;

        if (parameterType.equalsIgnoreCase(GisOutputFormatParameterDef.TYPE_FIELD)) {

            required = outputFormatDefs.get(formatKey).getParameterFieldDef(parameterKey).isRequired();

        } else if (parameterType.equalsIgnoreCase(GisOutputFormatParameterDef.TYPE_FIXED)) {

            required = outputFormatDefs.get(formatKey).getParameterFixedDef(parameterKey).isRequired();

        }

        return required;

    }

    public int getParameterValueMetaType(String formatKey, String parameterType, String parameterKey) {

        int valueMetaType = ValueMetaBase.TYPE_NONE;

        if (parameterType.equalsIgnoreCase(GisOutputFormatParameterDef.TYPE_FIELD)) {

            valueMetaType = outputFormatDefs.get(formatKey).getParameterFieldDef(parameterKey).getValueMetaType();

        } else if (parameterType.equalsIgnoreCase(GisOutputFormatParameterDef.TYPE_FIXED)) {

            valueMetaType = outputFormatDefs.get(formatKey).getParameterFixedDef(parameterKey).getValueMetaType();

        }

        return valueMetaType;

    }

    public Object getInputParameterValue(String parameterType, String parameterKey) {

        if (parameterType.equalsIgnoreCase(GisOutputFormatParameterDef.TYPE_FIELD)) {

            for (GisOutputFormatParameter parameter : outputFormatFieldParameters) {

                if (parameter.getKey().equalsIgnoreCase(parameterKey)) {
                    return parameter.getValue();
                }

            }

        } else if (parameterType.equalsIgnoreCase(GisOutputFormatParameterDef.TYPE_FIXED)) {

            for (GisOutputFormatParameter parameter : outputFormatFixedParameters) {

                if (parameter.getKey().equalsIgnoreCase(parameterKey)) {
                    return parameter.getValue();
                }

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

            outputFormat = XMLHandler.getTagValue(stepnode, "outputFormat");
            Node fieldParamsNode = XMLHandler.getSubNode(stepnode, "fieldParams");
            for (int i = 0; i < XMLHandler.countNodes(fieldParamsNode, "param"); i++) {

                Node paramNode = XMLHandler.getSubNodeByNr(fieldParamsNode, "param", i);
                String key = XMLHandler.getTagValue(paramNode, "key");
                String value = XMLHandler.getTagValue(paramNode, "value");

                outputFormatFieldParameters.add(new GisOutputFormatParameter(key, value));

            }

            Node fixedParamsNode = XMLHandler.getSubNode(stepnode, "fixedParams");
            for (int i = 0; i < XMLHandler.countNodes(fixedParamsNode, "param"); i++) {

                Node paramNode = XMLHandler.getSubNodeByNr(fixedParamsNode, "param", i);
                String key = XMLHandler.getTagValue(paramNode, "key");
                String value = XMLHandler.getTagValue(paramNode, "value");

                outputFormatFixedParameters.add(new GisOutputFormatParameter(key, value));

            }

            outputFileName = XMLHandler.getTagValue(stepnode, "outputFileName");
            geometryFieldName = XMLHandler.getTagValue(stepnode, "geometryFieldName");
            encoding = XMLHandler.getTagValue(stepnode, "encoding");
            createFileAtEnd = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "createFileAtEnd"));
            dataToServlet = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "dataToServlet"));

        } catch (Exception e) {
            throw new KettleXMLException("Unable to read step info from XML node", e);
        }
    }

    public void setDefault() {

        outputFormat = "ESRI_SHP";
        dataToServlet = false;
        createFileAtEnd = false;
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
        return new GisFileOutputDialog(shell, meta, transMeta, name);
    }

    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans disp) {
        return new GisFileOutput(stepMeta, stepDataInterface, cnr, transMeta, disp);
    }

    public StepDataInterface getStepData() {
        return new GisFileOutputData();
    }

    public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {

        try {

            outputFormat = rep.getStepAttributeString(id_step, "outputFormat");

            for (int i = 0; i < rep.countNrStepAttributes(id_step, "param_field_key"); i++) {

                String key = rep.getStepAttributeString(id_step, i, "param_field_key");
                String value = rep.getStepAttributeString(id_step, i, "param_field_value");

                outputFormatFieldParameters.add(new GisOutputFormatParameter(key, value));
            }

            for (int i = 0; i < rep.countNrStepAttributes(id_step, "param_fixed_key"); i++) {

                String key = rep.getStepAttributeString(id_step, i, "param_fixed_key");
                String value = rep.getStepAttributeString(id_step, i, "param_fixed_value");

                outputFormatFixedParameters.add(new GisOutputFormatParameter(key, value));
            }

            outputFileName = rep.getStepAttributeString(id_step, "outputFileName");
            geometryFieldName = rep.getStepAttributeString(id_step, "geometryFieldName");
            encoding = rep.getStepAttributeString(id_step, "encoding");
            createFileAtEnd = rep.getStepAttributeBoolean(id_step, "createFileAtEnd");
            dataToServlet = rep.getStepAttributeBoolean(id_step, "dataToServlet");

        } catch (Exception e) {

            throw new KettleXMLException("Unable to read step info from repository", e);
        }

    }

    public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException {

        try {

            rep.saveStepAttribute(id_transformation, id_step, "outputFormat", outputFormat);

            int i = 0;
            for (GisOutputFormatParameter parameter : outputFormatFieldParameters) {

                String key = parameter.getKey();
                String value = (String) parameter.getValue();

                rep.saveStepAttribute(id_transformation, id_step, i, "param_field_key", key);
                rep.saveStepAttribute(id_transformation, id_step, i, "param_field_value", value);

                i++;
            }

            int j = 0;
            for (GisOutputFormatParameter parameter : outputFormatFixedParameters) {

                String key = parameter.getKey();
                String value = (String) parameter.getValue();

                rep.saveStepAttribute(id_transformation, id_step, j, "param_fixed_key", key);
                rep.saveStepAttribute(id_transformation, id_step, j, "param_fixed_value", value);

                j++;
            }

            rep.saveStepAttribute(id_transformation, id_step, "outputFileName", outputFileName);
            rep.saveStepAttribute(id_transformation, id_step, "geometryFieldName", geometryFieldName);
            rep.saveStepAttribute(id_transformation, id_step, "encoding", encoding);
            rep.saveStepAttribute(id_transformation, id_step, "createFileAtEnd", createFileAtEnd);
            rep.saveStepAttribute(id_transformation, id_step, "dataToServlet", dataToServlet);

        } catch (Exception e) {

            throw new KettleXMLException("Unable to write step info in repository", e);

        }
    }
}
