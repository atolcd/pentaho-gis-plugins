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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Counter;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
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

import com.atolcd.pentaho.di.ui.trans.steps.giscoordinatetransformation.GisCoordinateTransformationDialog;

@Step(id = "GisRelate", image = "com/atolcd/pentaho/di/ui/trans/steps/images/GisRelate.png", name = "GisRelate.Shell.Name", description = "GisRelate.Shell.Description", categoryDescription = "GisRelate.Shell.CategoryDescription", i18nPackageName = "com.atolcd.pentaho.di.trans.steps.gisrelate")
public class GisRelateMeta extends BaseStepMeta implements StepMetaInterface {

    private String operator;

    // Opérateurs avec résultat de type boolean
    private static String[] boolResultOperators = new String[] { "CONTAINS", "COVERED_BY", "COVERS", "CROSSES", "DISJOINT", "EQUALS", "EQUALS_EXACT", "INTERSECTS", "WITHIN",
            "OVERLAPS", "TOUCHES", "IS_WITHIN_DISTANCE", "IS_NOT_WITHIN_DISTANCE" };

    // Opérateurs avec résultat de type numérique
    private static String[] numericResultOperators = new String[] { "DISTANCE_MIN", "DISTANCE_MAX" };

    private String firstGeometryFieldName;
    private String secondGeometryFieldName;

    // Filtrage de lignes
    private static String[] returnTypes = new String[] { "ALL", "FALSE", "TRUE" };
    private String returnType;

    // Pour opérateurs avec besoin de distance
    private static String[] withDistanceOperators = new String[] { "IS_WITHIN_DISTANCE", "IS_NOT_WITHIN_DISTANCE" };
    private boolean dynamicDistance;
    private String distanceFieldName;
    private String distanceValue;

    // Colonne de sortie
    private String outputFieldName;

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getFirstGeometryFieldName() {
        return firstGeometryFieldName;
    }

    public String[] getBoolResultOperators() {
        return boolResultOperators;
    }

    public String[] getNumericResultOperators() {
        return numericResultOperators;
    }

    public String[] getWithDistanceOperators() {
        return withDistanceOperators;
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

    public boolean isDynamicDistance() {
        return dynamicDistance;
    }

    public void setDynamicDistance(boolean dynamicDistance) {
        this.dynamicDistance = dynamicDistance;
    }

    public String getDistanceFieldName() {
        return distanceFieldName;
    }

    public void setDistanceFieldName(String distanceFieldName) {
        this.distanceFieldName = distanceFieldName;
    }

    public String getDistanceValue() {
        return distanceValue;
    }

    public void setDistanceValue(String distanceValue) {
        this.distanceValue = distanceValue;
    }

    public String getXML() {

        StringBuffer retval = new StringBuffer();
        retval.append("    " + XMLHandler.addTagValue("operator", operator));
        retval.append("    " + XMLHandler.addTagValue("returnType", returnType));
        retval.append("    " + XMLHandler.addTagValue("firstGeometryFieldName", firstGeometryFieldName));
        retval.append("    " + XMLHandler.addTagValue("secondGeometryFieldName", secondGeometryFieldName));
        retval.append("    " + XMLHandler.addTagValue("dynamicDistance", dynamicDistance));
        retval.append("    " + XMLHandler.addTagValue("distanceFieldName", distanceFieldName));
        retval.append("    " + XMLHandler.addTagValue("distanceValue", distanceValue));
        retval.append("    " + XMLHandler.addTagValue("outputFieldName", outputFieldName));
        return retval.toString();

    }

    public void getFields(RowMetaInterface r, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) {

        if (ArrayUtils.contains(numericResultOperators, operator)) {
            ValueMetaInterface valueMeta = new ValueMeta(outputFieldName, ValueMeta.TYPE_NUMBER);
            valueMeta.setOrigin(origin);
            r.addValueMeta(valueMeta);
        }

        if (ArrayUtils.contains(boolResultOperators, operator)) {

            if (returnType.equalsIgnoreCase("ALL")) {
                ValueMetaInterface valueMeta = new ValueMeta(outputFieldName, ValueMeta.TYPE_BOOLEAN);
                valueMeta.setOrigin(origin);
                r.addValueMeta(valueMeta);
            }
        }

    }

    public Object clone() {

        Object retval = super.clone();
        return retval;

    }

    public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {

        try {

            operator = XMLHandler.getTagValue(stepnode, "operator");
            returnType = XMLHandler.getTagValue(stepnode, "returnType");
            firstGeometryFieldName = XMLHandler.getTagValue(stepnode, "firstGeometryFieldName");
            secondGeometryFieldName = XMLHandler.getTagValue(stepnode, "secondGeometryFieldName");
            dynamicDistance = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "dynamicDistance"));
            distanceFieldName = XMLHandler.getTagValue(stepnode, "distanceFieldName");
            distanceValue = XMLHandler.getTagValue(stepnode, "distanceValue");
            outputFieldName = XMLHandler.getTagValue(stepnode, "outputFieldName");

        } catch (Exception e) {
            throw new KettleXMLException("Unable to read step info from XML node", e);
        }

    }

    public void setDefault() {
        operator = "CONTAINS";
        returnType = "ALL";
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
        return new GisRelate(stepMeta, stepDataInterface, cnr, transMeta, disp);
    }

    public StepDataInterface getStepData() {
        return new GisRelateData();
    }

    public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {

        try {

            operator = rep.getStepAttributeString(id_step, "operator");
            returnType = rep.getStepAttributeString(id_step, "returnType");
            firstGeometryFieldName = rep.getStepAttributeString(id_step, "firstGeometryFieldName");
            secondGeometryFieldName = rep.getStepAttributeString(id_step, "secondGeometryFieldName");
            dynamicDistance = rep.getStepAttributeBoolean(id_step, "dynamicDistance");
            distanceValue = rep.getStepAttributeString(id_step, "distanceValue");
            distanceFieldName = rep.getStepAttributeString(id_step, "distanceFieldName");
            outputFieldName = rep.getStepAttributeString(id_step, "outputFieldName");

        } catch (Exception e) {

            throw new KettleXMLException("Unable to read step info from repository", e);
        }
    }

    public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException {

        try {

            rep.saveStepAttribute(id_transformation, id_step, "operator", operator);
            rep.saveStepAttribute(id_transformation, id_step, "returnType", returnType);
            rep.saveStepAttribute(id_transformation, id_step, "firstGeometryFieldName", firstGeometryFieldName);
            rep.saveStepAttribute(id_transformation, id_step, "secondGeometryFieldName", secondGeometryFieldName);
            rep.saveStepAttribute(id_transformation, id_step, "dynamicDistance", dynamicDistance);
            rep.saveStepAttribute(id_transformation, id_step, "distanceFieldName", distanceFieldName);
            rep.saveStepAttribute(id_transformation, id_step, "distanceValue", distanceValue);
            rep.saveStepAttribute(id_transformation, id_step, "outputFieldName", outputFieldName);

        } catch (Exception e) {

            throw new KettleXMLException("Unable to write step info in repository", e);

        }
    }
}
