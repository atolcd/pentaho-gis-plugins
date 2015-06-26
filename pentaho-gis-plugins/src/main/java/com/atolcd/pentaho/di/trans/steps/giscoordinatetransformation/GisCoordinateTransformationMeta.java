package com.atolcd.pentaho.di.trans.steps.giscoordinatetransformation;

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

@Step(id = "GisCoordinateTransformation", image = "com/atolcd/pentaho/di/ui/trans/steps/images/GisCoordinateTransformation.png", name = "GisCoordinateTransformation.Shell.Name", description = "GisCoordinateTransformation.Shell.Description", categoryDescription = "GisCoordinateTransformation.Shell.CategoryDescription", i18nPackageName = "com.atolcd.pentaho.di.trans.steps.giscoordinatetransformation")
public class GisCoordinateTransformationMeta extends BaseStepMeta implements StepMetaInterface {

    private String geometryFieldName; // Colonne contenant la géométrie
    private String outputGeometryFieldName; // Nom de la colonne contenant la
                                            // géométrie après opération
    private String inputCRSAuthority; // Autorité du CRS d'entrée
    private String inputCRSCode; // Code du CRS d'entrée
    private String outputCRSAuthority; // Autorité du CRS de sortie
    private String outputCRSCode; // Code du CRS de sortie
    private boolean crsFromGeometry; // Utiliser le système de projection
                                     // associé à la géométrie pour la
                                     // reprojection
    private String crsOperation; // Opération à réaliser : Assignation de SRID
                                 // ou reprojection

    public String getGeometryFieldName() {
        return geometryFieldName;
    }

    public void setGeometryFieldName(String geometryFieldName) {
        this.geometryFieldName = geometryFieldName;
    }

    public String getInputCRSCode() {
        return inputCRSCode;
    }

    public void setInputCRSCode(String inputCRSCode) {
        this.inputCRSCode = inputCRSCode;
    }

    public String getOutputCRSCode() {
        return outputCRSCode;
    }

    public void setOutputCRSCode(String outputCRSCode) {
        this.outputCRSCode = outputCRSCode;
    }

    public String getInputCRSAuthority() {
        return inputCRSAuthority;
    }

    public void setInputCRSAuthority(String inputCRSAuthority) {
        this.inputCRSAuthority = inputCRSAuthority;
    }

    public String getOutputCRSAuthority() {
        return outputCRSAuthority;
    }

    public void setOutputCRSAuthority(String outputCRSAuthority) {
        this.outputCRSAuthority = outputCRSAuthority;
    }

    public boolean isCrsFromGeometry() {
        return crsFromGeometry;
    }

    public void setCrsFromGeometry(boolean crsFromGeometry) {
        this.crsFromGeometry = crsFromGeometry;
    }

    public String getCrsOperation() {
        return crsOperation;
    }

    public void setCrsOperation(String crsOperation) {
        this.crsOperation = crsOperation;
    }

    public String getOutputGeometryFieldName() {
        return outputGeometryFieldName;
    }

    public void setOutputGeometryFieldName(String outputGeometryFieldName) {
        this.outputGeometryFieldName = outputGeometryFieldName;
    }

    public String getXML() {

        StringBuffer retval = new StringBuffer();
        retval.append("    " + XMLHandler.addTagValue("crsOperation", crsOperation));
        retval.append("    " + XMLHandler.addTagValue("geometryFieldName", geometryFieldName));
        retval.append("    " + XMLHandler.addTagValue("outputGeometryFieldName", outputGeometryFieldName));
        retval.append("    " + XMLHandler.addTagValue("inputCRSAuthority", inputCRSAuthority));
        retval.append("    " + XMLHandler.addTagValue("inputCRSCode", inputCRSCode));
        retval.append("    " + XMLHandler.addTagValue("outputCRSAuthority", outputCRSAuthority));
        retval.append("    " + XMLHandler.addTagValue("outputCRSCode", outputCRSCode));
        retval.append("    " + XMLHandler.addTagValue("crsFromGeometry", crsFromGeometry));
        return retval.toString();

    }

    public void getFields(RowMetaInterface r, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) {

        ValueMetaInterface valueMeta = new ValueMetaGeometry(outputGeometryFieldName);
        valueMeta.setOrigin(origin);
        r.addValueMeta(valueMeta);

    }

    public Object clone() {

        Object retval = super.clone();
        return retval;

    }

    public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {

        try {

            crsOperation = XMLHandler.getTagValue(stepnode, "crsOperation");
            geometryFieldName = XMLHandler.getTagValue(stepnode, "geometryFieldName");
            outputGeometryFieldName = XMLHandler.getTagValue(stepnode, "outputGeometryFieldName");
            inputCRSAuthority = XMLHandler.getTagValue(stepnode, "inputCRSAuthority");
            inputCRSCode = XMLHandler.getTagValue(stepnode, "inputCRSCode");
            outputCRSAuthority = XMLHandler.getTagValue(stepnode, "outputCRSAuthority");
            outputCRSCode = XMLHandler.getTagValue(stepnode, "outputCRSCode");
            crsFromGeometry = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "crsFromGeometry"));

        } catch (Exception e) {
            throw new KettleXMLException("Unable to read step info from XML node", e);
        }

    }

    public void setDefault() {
        this.crsOperation = "ASSIGN";
        this.crsFromGeometry = false;

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
        return new GisCoordinateTransformation(stepMeta, stepDataInterface, cnr, transMeta, disp);
    }

    public StepDataInterface getStepData() {
        return new GisCoordinateTransformationData();
    }

    public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {

        try {

            crsOperation = rep.getStepAttributeString(id_step, "crsOperation");
            geometryFieldName = rep.getStepAttributeString(id_step, "geometryFieldName");
            outputGeometryFieldName = rep.getStepAttributeString(id_step, "outputGeometryFieldName");
            inputCRSAuthority = rep.getStepAttributeString(id_step, "inputCRSAuthority");
            inputCRSCode = rep.getStepAttributeString(id_step, "inputCRSCode");
            outputCRSAuthority = rep.getStepAttributeString(id_step, "outputCRSAuthority");
            outputCRSCode = rep.getStepAttributeString(id_step, "outputCRSCode");
            crsFromGeometry = rep.getStepAttributeBoolean(id_step, "crsFromGeometry");

        } catch (Exception e) {

            throw new KettleXMLException("Unable to read step info from repository", e);
        }
    }

    public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException {

        try {

            rep.saveStepAttribute(id_transformation, id_step, "crsOperation", crsOperation);
            rep.saveStepAttribute(id_transformation, id_step, "geometryFieldName", geometryFieldName);
            rep.saveStepAttribute(id_transformation, id_step, "outputGeometryFieldName", outputGeometryFieldName);
            rep.saveStepAttribute(id_transformation, id_step, "inputCRSAuthority", inputCRSAuthority);
            rep.saveStepAttribute(id_transformation, id_step, "inputCRSCode", inputCRSCode);
            rep.saveStepAttribute(id_transformation, id_step, "outputCRSAuthority", outputCRSAuthority);
            rep.saveStepAttribute(id_transformation, id_step, "outputCRSCode", outputCRSCode);
            rep.saveStepAttribute(id_transformation, id_step, "crsFromGeometry", crsFromGeometry);

        } catch (Exception e) {

            throw new KettleXMLException("Unable to write step info in repository", e);

        }
    }
}
