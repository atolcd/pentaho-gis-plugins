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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.atolcd.pentaho.di.ui.trans.steps.gisfileinput.GisFileInputDialog;

@Step(id = "GisGeometryInfo", image = "com/atolcd/pentaho/di/ui/trans/steps/images/GisGeometryInfo.png", name = "GisGeometryInfo.Shell.Name", description = "GisGeometryInfo.Shell.Description", categoryDescription = "GisGeometryInfo.Shell.CategoryDescription", i18nPackageName = "com.atolcd.pentaho.di.trans.steps.gisgeometryinfo")
public class GisGeometryInfoMeta extends BaseStepMeta implements StepMetaInterface {

    private HashMap<String, Integer> infosTypes;
    private String geometryFieldName;
    private LinkedHashMap<String, String> outputFields;

    public GisGeometryInfoMeta() {

        super();
        this.infosTypes = new HashMap<String, Integer>();
        this.outputFields = new LinkedHashMap<String, String>();

        this.infosTypes.put("NULL_OR_EMPTY", ValueMeta.TYPE_BOOLEAN);
        this.infosTypes.put("AREA", ValueMeta.TYPE_NUMBER);
        this.infosTypes.put("LENGTH", ValueMeta.TYPE_NUMBER);
        this.infosTypes.put("DIMENSION", ValueMeta.TYPE_INTEGER);
        this.infosTypes.put("SRID", ValueMeta.TYPE_INTEGER);
        this.infosTypes.put("GEOMETRY_TYPE", ValueMeta.TYPE_STRING);
        this.infosTypes.put("GEOMETRY_COUNT", ValueMeta.TYPE_INTEGER);
        this.infosTypes.put("GEOMETRY_VERTEX_COUNT", ValueMeta.TYPE_INTEGER);
        this.infosTypes.put("X_MIN", ValueMeta.TYPE_NUMBER);
        this.infosTypes.put("Y_MIN", ValueMeta.TYPE_NUMBER);
        this.infosTypes.put("Z_MIN", ValueMeta.TYPE_NUMBER);
        this.infosTypes.put("X_MAX", ValueMeta.TYPE_NUMBER);
        this.infosTypes.put("Y_MAX", ValueMeta.TYPE_NUMBER);
        this.infosTypes.put("Z_MAX", ValueMeta.TYPE_NUMBER);

    }

    public HashMap<String, Integer> getInfosTypes() {
        return infosTypes;
    }

    public void setInfosTypes(HashMap<String, Integer> infosTypes) {
        this.infosTypes = infosTypes;
    }

    public String getGeometryFieldName() {
        return geometryFieldName;
    }

    public void setGeometryFieldName(String geometryFieldName) {
        this.geometryFieldName = geometryFieldName;
    }

    public LinkedHashMap<String, String> getOutputFields() {
        return outputFields;
    }

    public void setOutputFields(LinkedHashMap<String, String> outputFields) {
        this.outputFields = outputFields;
    }

    public String getXML() {

        StringBuffer retval = new StringBuffer();

        retval.append("    " + XMLHandler.addTagValue("geometryFieldName", geometryFieldName));

        retval.append("\t<outputs>").append(Const.CR);
        for (Entry<String, String> output : outputFields.entrySet()) {

            String key = output.getKey();
            String value = output.getValue();

            retval.append("\t\t<output>").append(Const.CR);
            retval.append("\t\t\t").append(XMLHandler.addTagValue("infoKey", key));
            retval.append("\t\t\t").append(XMLHandler.addTagValue("infoFieldname", value));
            retval.append("\t\t</output>").append(Const.CR);

        }

        retval.append("\t</outputs>").append(Const.CR);

        return retval.toString();

    }

    public void getFields(RowMetaInterface r, String origin, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space) {

        for (Entry<String, String> output : outputFields.entrySet()) {

            String fieldName = output.getValue();
            int valueMetaType = infosTypes.get(output.getKey());

            ValueMetaInterface valueMeta = null;

            if (valueMetaType == ValueMetaGeometry.TYPE_GEOMETRY) {
                valueMeta = new ValueMetaGeometry(fieldName);
            } else {
                valueMeta = new ValueMeta(fieldName, valueMetaType);
            }

            valueMeta.setOrigin(origin);
            r.addValueMeta(valueMeta);
        }

    }

    public Object clone() {

        Object retval = super.clone();
        return retval;

    }

    public void loadXML(Node stepnode, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleXMLException {

        try {

            geometryFieldName = XMLHandler.getTagValue(stepnode, "geometryFieldName");
            Node outputsNode = XMLHandler.getSubNode(stepnode, "outputs");
            for (int i = 0; i < XMLHandler.countNodes(outputsNode, "output"); i++) {

                Node outputNode = XMLHandler.getSubNodeByNr(outputsNode, "output", i);
                String key = XMLHandler.getTagValue(outputNode, "infoKey");
                String value = XMLHandler.getTagValue(outputNode, "infoFieldname");

                outputFields.put(key, value);

            }

        } catch (Exception e) {
            throw new KettleXMLException("Unable to read step info from XML node", e);
        }

    }

    public void setDefault() {

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
        return new GisGeometryInfo(stepMeta, stepDataInterface, cnr, transMeta, disp);
    }

    public StepDataInterface getStepData() {
        return new GisGeometryInfoData();
    }

    public void readRep(Repository rep, ObjectId id_step, List<DatabaseMeta> databases, Map<String, Counter> counters) throws KettleException {

        try {

            geometryFieldName = rep.getStepAttributeString(id_step, "geometryFieldName");

            for (int i = 0; i < rep.countNrStepAttributes(id_step, "info_key"); i++) {

                String key = rep.getStepAttributeString(id_step, i, "info_key");
                String value = rep.getStepAttributeString(id_step, i, "info_fieldname");

                outputFields.put(key, value);
            }

        } catch (Exception e) {

            throw new KettleXMLException("Unable to read step info from repository", e);
        }
    }

    public void saveRep(Repository rep, ObjectId id_transformation, ObjectId id_step) throws KettleException {

        try {

            rep.saveStepAttribute(id_transformation, id_step, "geometryFieldName", geometryFieldName);

            int i = 0;
            for (Entry<String, String> output : outputFields.entrySet()) {

                String key = output.getKey();
                String value = output.getValue();

                rep.saveStepAttribute(id_transformation, id_step, i, "info_key", key);
                rep.saveStepAttribute(id_transformation, id_step, i, "info_fieldname", value);

                i++;
            }

        } catch (Exception e) {

            throw new KettleXMLException("Unable to write step info in repository", e);

        }
    }
}
