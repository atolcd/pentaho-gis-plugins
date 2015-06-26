package com.atolcd.pentaho.di.trans.steps.gisgeoprocessing;

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

import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
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

import com.atolcd.pentaho.di.core.row.value.ValueMetaGeometry;
import com.atolcd.pentaho.di.ui.trans.steps.giscoordinatetransformation.GisCoordinateTransformationDialog;

@Step(id = "GisGeoprocessing", image = "com/atolcd/pentaho/di/ui/trans/steps/images/GisGeoprocessing.png", name = "GisGeoprocessing.Shell.Name", description = "GisGeoprocessing.Shell.Description", categoryDescription = "GisGeoprocessing.Shell.CategoryDescription", i18nPackageName = "com.atolcd.pentaho.di.trans.steps.gisgeoprocessing")
public class GisGeoprocessingMeta extends BaseStepMeta implements StepMetaInterface {

    private String operator;

    // Pour opérateurs avec une seule géométrie
    private static String[] oneGeometryOperators = new String[] { "BOUNDARY", "INTERIOR_POINT", "CONVEX_HULL", "CONCAVE_HULL", "BUFFER", "EXTENDED_BUFFER", "EXPLODE", "REVERSE",
            "DENSIFY", "SIMPLIFY", "TO_2D_GEOMETRY", "TO_MULTI_GEOMETRY", "EXTRACT_COORDINATES", "MBR", "CENTROID" };
    private String firstGeometryFieldName;

    // Pour opérateurs avec deux géométries
    private static String[] twoGeometriesOperators = new String[] { "UNION", "DIFFERENCE", "INTERSECTION", "SYM_DIFFERENCE", "SNAP_TO_GEOMETRY", "SIMPLIFY_POLYGON" };
    private String secondGeometryFieldName;

    // Pour opérateurs avec possibilités de filtrage de géométries hétérogènes
    private static String[] withExtractTypeOperators = new String[] { "UNION", "DIFFERENCE", "INTERSECTION", "SYM_DIFFERENCE" };
    private static String[] extractTypes = new String[] { "ALL", "PUNTAL_ONLY", "LINEAL_ONLY", "POLYGONAL_ONLY" };
    private String extractType;

    // Filtrage de lignes
    private static String[] returnTypes = new String[] { "ALL", "NOT_NULL" };
    private String returnType;

    // Pour opérateurs avec besoin de distance
    private static String[] withDistanceOperators = new String[] { "CONCAVE_HULL", "BUFFER", "EXTENDED_BUFFER", "DENSIFY", "SIMPLIFY", ",CONCAVE_HULL", "SNAP_TO_GEOMETRY",
            "SIMPLIFY_POLYGON" };
    private boolean dynamicDistance;
    private String distanceFieldName;
    private String distanceValue;

    // Pour EXTENDED_BUFFER
    private static String[] bufferJoinStyles = new String[] { "BEVEL", "MITRE", "ROUND" };
    private static String[] bufferCapStyles = new String[] { "FLAT", "ROUND", "SQUARE" };

    private Integer bufferSegmentsCount;
    private Boolean bufferSingleSide;
    private String bufferCapStyle;
    private String bufferJoinStyle;

    // Géométrie de sortie
    private String outputFieldName;

    public String[] getBufferJoinStyles() {
        return bufferJoinStyles;
    }

    public String[] getBufferCapStyles() {
        return bufferCapStyles;
    }

    public String[] getWithExtractTypeOperators() {
        return withExtractTypeOperators;
    }

    public Integer getBufferSegmentsCount() {
        return bufferSegmentsCount;
    }

    public void setBufferSegmentsCount(Integer bufferSegmentsCount) {
        this.bufferSegmentsCount = bufferSegmentsCount;
    }

    public Boolean getBufferSingleSide() {
        return bufferSingleSide;
    }

    public void setBufferSingleSide(Boolean bufferSingleSide) {
        this.bufferSingleSide = bufferSingleSide;
    }

    public String getBufferCapStyle() {
        return bufferCapStyle;
    }

    public void setBufferCapStyle(String bufferCapStyle) {
        this.bufferCapStyle = bufferCapStyle;
    }

    public String getBufferJoinStyle() {
        return bufferJoinStyle;
    }

    public void setBufferJoinStyle(String bufferJoinStyle) {
        this.bufferJoinStyle = bufferJoinStyle;
    }

    public String[] getOneGeometryOperators() {
        return oneGeometryOperators;
    }

    public String[] getTwoGeometriesOperators() {
        return twoGeometriesOperators;
    }

    public String[] getWithDistanceOperators() {
        return withDistanceOperators;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getFirstGeometryFieldName() {
        return firstGeometryFieldName;
    }

    public void setFirstGeometryFieldName(String firstGeometryFieldName) {
        this.firstGeometryFieldName = firstGeometryFieldName;
    }

    public String getSecondGeometryFieldName() {
        return secondGeometryFieldName;
    }

    public void setSecondGeometryFieldName(String secondGeometryFieldName) {
        this.secondGeometryFieldName = secondGeometryFieldName;
    }

    public String getOutputFieldName() {
        return outputFieldName;
    }

    public void setOutputFieldName(String outputFieldName) {
        this.outputFieldName = outputFieldName;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String[] getReturnTypes() {
        return returnTypes;
    }

    public String getDistanceFieldName() {
        return distanceFieldName;
    }

    public void setDistanceFieldName(String distanceFieldName) {
        this.distanceFieldName = distanceFieldName;
    }

    public boolean isDynamicDistance() {
        return dynamicDistance;
    }

    public void setDynamicDistance(boolean dynamicDistance) {
        this.dynamicDistance = dynamicDistance;
    }

    public String getDistanceValue() {
        return distanceValue;
    }

    public void setDistanceValue(String distanceValue) {
        this.distanceValue = distanceValue;
    }

    public String[] getExtractTypes() {
        return extractTypes;
    }

    public String getExtractType() {
        return extractType;
    }

    public void setExtractType(String extractType) {
        this.extractType = extractType;
    }

    public String getXML() {

        StringBuffer retval = new StringBuffer();
        retval.append("    " + XMLHandler.addTagValue("operator", operator));
        retval.append("    " + XMLHandler.addTagValue("returnType", returnType));
        retval.append("    " + XMLHandler.addTagValue("extractType", extractType));
        retval.append("    " + XMLHandler.addTagValue("firstGeometryFieldName", firstGeometryFieldName));
        retval.append("    " + XMLHandler.addTagValue("secondGeometryFieldName", secondGeometryFieldName));
        retval.append("    " + XMLHandler.addTagValue("dynamicDistance", dynamicDistance));
        retval.append("    " + XMLHandler.addTagValue("distanceFieldName", distanceFieldName));
        retval.append("    " + XMLHandler.addTagValue("distanceValue", distanceValue));
        retval.append("    " + XMLHandler.addTagValue("outputFieldName", outputFieldName));

        retval.append("    " + XMLHandler.addTagValue("bufferSegmentsCount", bufferSegmentsCount));
        retval.append("    " + XMLHandler.addTagValue("bufferSingleSide", bufferSingleSide));
        retval.append("    " + XMLHandler.addTagValue("bufferCapStyle", bufferCapStyle));
        retval.append("    " + XMLHandler.addTagValue("bufferJoinStyle", bufferJoinStyle));

        return retval.toString();

    }

    public void getFields(RowMetaInterface r, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) {

        ValueMetaInterface valueMeta = new ValueMetaGeometry(outputFieldName);
        valueMeta.setOrigin(origin);
        r.addValueMeta(valueMeta);

    }

    public Object clone() {

        Object retval = super.clone();
        return retval;

    }

    public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {

        try {

            operator = XMLHandler.getTagValue(stepnode, "operator");
            returnType = XMLHandler.getTagValue(stepnode, "returnType");
            extractType = XMLHandler.getTagValue(stepnode, "extractType");
            firstGeometryFieldName = XMLHandler.getTagValue(stepnode, "firstGeometryFieldName");
            secondGeometryFieldName = XMLHandler.getTagValue(stepnode, "secondGeometryFieldName");
            dynamicDistance = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "dynamicDistance"));
            distanceFieldName = XMLHandler.getTagValue(stepnode, "distanceFieldName");
            distanceValue = XMLHandler.getTagValue(stepnode, "distanceValue");
            outputFieldName = XMLHandler.getTagValue(stepnode, "outputFieldName");

            bufferSegmentsCount = Integer.parseInt(XMLHandler.getTagValue(stepnode, "bufferSegmentsCount"));
            bufferSingleSide = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "bufferSingleSide"));
            bufferCapStyle = XMLHandler.getTagValue(stepnode, "bufferCapStyle");
            bufferJoinStyle = XMLHandler.getTagValue(stepnode, "bufferJoinStyle");

        } catch (Exception e) {
            throw new KettleXMLException("Unable to read step info from XML node", e);
        }

    }

    public void setDefault() {
        operator = "CENTROID";
        returnType = "ALL";
        extractType = "ALL";
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
        return new GisCoordinateTransformationDialog(shell, meta, transMeta, name);
    }

    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans disp) {
        return new GisGeoprocessing(stepMeta, stepDataInterface, cnr, transMeta, disp);
    }

    public StepDataInterface getStepData() {
        return new GisGeoprocessingData();
    }

    public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {

        try {

            operator = rep.getStepAttributeString(id_step, "operator");
            returnType = rep.getStepAttributeString(id_step, "returnType");
            extractType = rep.getStepAttributeString(id_step, "extractType");
            firstGeometryFieldName = rep.getStepAttributeString(id_step, "firstGeometryFieldName");
            secondGeometryFieldName = rep.getStepAttributeString(id_step, "secondGeometryFieldName");
            dynamicDistance = rep.getStepAttributeBoolean(id_step, "dynamicDistance");
            distanceValue = rep.getStepAttributeString(id_step, "distanceValue");
            distanceFieldName = rep.getStepAttributeString(id_step, "distanceFieldName");
            outputFieldName = rep.getStepAttributeString(id_step, "outputFieldName");

            bufferSegmentsCount = (int) rep.getStepAttributeInteger(id_step, "bufferSegmentsCount");
            bufferSingleSide = rep.getStepAttributeBoolean(id_step, "bufferSingleSide");
            bufferCapStyle = rep.getStepAttributeString(id_step, "bufferCapStyle");
            bufferJoinStyle = rep.getStepAttributeString(id_step, "bufferJoinStyle");

        } catch (Exception e) {

            throw new KettleXMLException("Unable to read step info from repository", e);
        }
    }

    public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException {

        try {

            rep.saveStepAttribute(id_transformation, id_step, "operator", operator);
            rep.saveStepAttribute(id_transformation, id_step, "returnType", returnType);
            rep.saveStepAttribute(id_transformation, id_step, "extractType", extractType);
            rep.saveStepAttribute(id_transformation, id_step, "firstGeometryFieldName", firstGeometryFieldName);
            rep.saveStepAttribute(id_transformation, id_step, "secondGeometryFieldName", secondGeometryFieldName);
            rep.saveStepAttribute(id_transformation, id_step, "dynamicDistance", dynamicDistance);
            rep.saveStepAttribute(id_transformation, id_step, "distanceFieldName", distanceFieldName);
            rep.saveStepAttribute(id_transformation, id_step, "distanceValue", distanceValue);
            rep.saveStepAttribute(id_transformation, id_step, "outputFieldName", outputFieldName);

            rep.saveStepAttribute(id_transformation, id_step, "bufferSegmentsCount", bufferSegmentsCount);
            rep.saveStepAttribute(id_transformation, id_step, "bufferSingleSide", bufferSingleSide);
            rep.saveStepAttribute(id_transformation, id_step, "bufferCapStyle", bufferCapStyle);
            rep.saveStepAttribute(id_transformation, id_step, "bufferJoinStyle", bufferJoinStyle);

        } catch (Exception e) {

            throw new KettleXMLException("Unable to write step info in repository", e);

        }
    }
}
