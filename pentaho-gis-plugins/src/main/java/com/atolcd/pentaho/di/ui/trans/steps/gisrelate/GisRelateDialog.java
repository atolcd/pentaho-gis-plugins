package com.atolcd.pentaho.di.ui.trans.steps.gisrelate;

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
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import com.atolcd.pentaho.di.core.row.value.ValueMetaGeometry;
import com.atolcd.pentaho.di.trans.steps.gisrelate.GisRelateMeta;

public class GisRelateDialog extends BaseStepDialog implements StepDialogInterface {

    private static Class<?> PKG = GisRelateMeta.class;

    private Group wInputGroup;
    private FormData fdInputGroup;

    private Group wOutputGroup;
    private FormData fdOutputGroup;

    private Label wlOperator;
    private CCombo wOperator;
    private FormData fdlOperator, fdOperator;

    private Text wOperatorDescription;
    private FormData fdOperatorDescription;

    private Label wlReturnType;
    private CCombo wReturnType;
    private FormData fdlReturnType, fdReturnType;

    private Label wlFirstGeometryField;
    private CCombo wFirstGeometryField;
    private FormData fdlFirstGeometryField, fdFirstGeometryField;

    private Label wlDistanceField;
    private CCombo wDistanceField;
    private FormData fdlDistanceField, fdDistanceField;

    private TextVar wDistanceValue;
    private FormData fdDistanceValue;

    private Label wlDynamicDistance;
    private Button wDynamicDistance;
    private FormData fdlDynamicDistance, fdDynamicDistance;

    private Label wlSecondGeometryField;
    private CCombo wSecondGeometryField;
    private FormData fdlSecondGeometryField, fdSecondGeometryField;

    private Label wlOutputField;
    private Text wOutputField;
    private FormData fdlOutputField, fdOutputField;

    private GisRelateMeta input;

    public GisRelateDialog(Shell parent, Object in, TransMeta tr, String sname) {

        super(parent, (BaseStepMeta) in, tr, sname);
        input = (GisRelateMeta) in;

    }

    public String open() {

        Shell parent = getParent();
        Display display = parent.getDisplay();

        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
        props.setLook(shell);
        setShellImage(shell, input);

        ModifyListener lsMod = new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                input.setChanged();
            }
        };
        backupChanged = input.hasChanged();

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;

        shell.setLayout(formLayout);
        shell.setText(BaseMessages.getString(PKG, "GisRelate.Shell.Title"));

        int middle = props.getMiddlePct();
        int margin = Const.MARGIN;

        // Nom du step
        wlStepname = new Label(shell, SWT.RIGHT);
        wlStepname.setText(BaseMessages.getString(PKG, "System.Label.StepName"));
        props.setLook(wlStepname);
        fdlStepname = new FormData();
        fdlStepname.left = new FormAttachment(0, 0);
        fdlStepname.right = new FormAttachment(middle, -margin);
        fdlStepname.top = new FormAttachment(0, margin);
        wlStepname.setLayoutData(fdlStepname);
        wStepname = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wStepname.setText(stepname);
        props.setLook(wStepname);
        wStepname.addModifyListener(lsMod);
        fdStepname = new FormData();
        fdStepname.left = new FormAttachment(middle, 0);
        fdStepname.top = new FormAttachment(0, margin);
        fdStepname.right = new FormAttachment(100, 0);
        wStepname.setLayoutData(fdStepname);

        // Operation
        wlOperator = new Label(shell, SWT.RIGHT);
        wlOperator.setText(BaseMessages.getString(PKG, "GisRelate.Operator.Label"));
        props.setLook(wlOperator);
        fdlOperator = new FormData();
        fdlOperator.left = new FormAttachment(0, 0);
        fdlOperator.top = new FormAttachment(wStepname, margin);
        fdlOperator.right = new FormAttachment(middle, -margin);
        wlOperator.setLayoutData(fdlOperator);

        wOperator = new CCombo(shell, SWT.BORDER | SWT.READ_ONLY);
        wOperator.setToolTipText(BaseMessages.getString(PKG, "GisRelate.Operator.ToolTip"));
        wOperator.setEditable(false);
        props.setLook(wOperator);
        wOperator.addModifyListener(lsMod);
        fdOperator = new FormData();
        fdOperator.left = new FormAttachment(middle, 0);
        fdOperator.right = new FormAttachment(100, 0);
        fdOperator.top = new FormAttachment(wStepname, margin);
        wOperator.setLayoutData(fdOperator);
        wOperator.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent arg0) {
                setOperatorFlags();
            }
        });

        wOperatorDescription = new Text(shell, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        wOperatorDescription.setText("");
        wOperatorDescription.setEditable(false);
        props.setLook(wOperatorDescription);
        wOperatorDescription.addModifyListener(lsMod);
        fdOperatorDescription = new FormData();
        fdOperatorDescription.left = new FormAttachment(middle, 0);
        fdOperatorDescription.right = new FormAttachment(100, 0);
        fdOperatorDescription.top = new FormAttachment(wOperator, margin);
        wOperatorDescription.setLayoutData(fdOperatorDescription);

        // ///////////////////////////////////////////////
        // Début du groupe : Input

        wInputGroup = new Group(shell, SWT.SHADOW_NONE);
        props.setLook(wInputGroup);
        wInputGroup.setText(BaseMessages.getString(PKG, "GisRelate.InputGroup.Label"));

        FormLayout InputGroupLayout = new FormLayout();
        InputGroupLayout.marginWidth = 5;
        InputGroupLayout.marginHeight = 5;
        wInputGroup.setLayout(InputGroupLayout);

        // Première géométrie
        wlFirstGeometryField = new Label(wInputGroup, SWT.RIGHT);
        wlFirstGeometryField.setText(BaseMessages.getString(PKG, "GisRelate.FirstGeometryFieldName.Label"));
        props.setLook(wlFirstGeometryField);
        fdlFirstGeometryField = new FormData();
        fdlFirstGeometryField.left = new FormAttachment(0, 0);
        fdlFirstGeometryField.top = new FormAttachment(0, margin);
        fdlFirstGeometryField.right = new FormAttachment(middle, -margin);
        wlFirstGeometryField.setLayoutData(fdlFirstGeometryField);

        wFirstGeometryField = new CCombo(wInputGroup, SWT.BORDER | SWT.READ_ONLY);
        wFirstGeometryField.setToolTipText(BaseMessages.getString(PKG, "GisRelate.FirstGeometryFieldName.ToolTip"));
        wFirstGeometryField.setEditable(false);
        props.setLook(wFirstGeometryField);
        wFirstGeometryField.addModifyListener(lsMod);
        fdFirstGeometryField = new FormData();
        fdFirstGeometryField.left = new FormAttachment(middle, 0);
        fdFirstGeometryField.right = new FormAttachment(100, 0);
        fdFirstGeometryField.top = new FormAttachment(wStepname, margin);
        wFirstGeometryField.setLayoutData(fdFirstGeometryField);

        // Seconde géométrie
        wlSecondGeometryField = new Label(wInputGroup, SWT.RIGHT);
        wlSecondGeometryField.setText(BaseMessages.getString(PKG, "GisRelate.SecondGeometryFieldName.Label"));
        props.setLook(wlSecondGeometryField);
        fdlSecondGeometryField = new FormData();
        fdlSecondGeometryField.left = new FormAttachment(0, 0);
        fdlSecondGeometryField.top = new FormAttachment(wFirstGeometryField, margin);
        fdlSecondGeometryField.right = new FormAttachment(middle, -margin);
        wlSecondGeometryField.setLayoutData(fdlSecondGeometryField);

        wSecondGeometryField = new CCombo(wInputGroup, SWT.BORDER | SWT.READ_ONLY);
        wSecondGeometryField.setToolTipText(BaseMessages.getString(PKG, "GisRelate.SecondGeometryFieldName.ToolTip"));
        wSecondGeometryField.setEditable(false);
        props.setLook(wSecondGeometryField);
        wSecondGeometryField.addModifyListener(lsMod);
        fdSecondGeometryField = new FormData();
        fdSecondGeometryField.left = new FormAttachment(middle, 0);
        fdSecondGeometryField.right = new FormAttachment(100, 0);
        fdSecondGeometryField.top = new FormAttachment(wFirstGeometryField, margin);
        wSecondGeometryField.setLayoutData(fdSecondGeometryField);

        // Distance dynamique ?
        wlDynamicDistance = new Label(wInputGroup, SWT.RIGHT);
        wlDynamicDistance.setText(BaseMessages.getString(PKG, "GisRelate.DistanceDynamic.Label"));
        props.setLook(wlDynamicDistance);
        fdlDynamicDistance = new FormData();
        fdlDynamicDistance.left = new FormAttachment(0, 0);
        fdlDynamicDistance.top = new FormAttachment(wSecondGeometryField, margin);
        fdlDynamicDistance.right = new FormAttachment(middle, -margin);
        wlDynamicDistance.setLayoutData(fdlDynamicDistance);

        wDynamicDistance = new Button(wInputGroup, SWT.CHECK);
        wDynamicDistance.setToolTipText(BaseMessages.getString(PKG, "GisRelate.DistanceDynamic.ToolTip"));
        props.setLook(wDynamicDistance);
        fdDynamicDistance = new FormData();
        fdDynamicDistance.left = new FormAttachment(middle, 0);
        fdDynamicDistance.top = new FormAttachment(wSecondGeometryField, margin);
        fdDynamicDistance.right = new FormAttachment(100, 0);
        wDynamicDistance.setLayoutData(fdDynamicDistance);
        wDynamicDistance.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent arg0) {
                setOperatorFlags();
            }
        });

        // Distance
        wlDistanceField = new Label(wInputGroup, SWT.RIGHT);
        wlDistanceField.setText(BaseMessages.getString(PKG, "GisRelate.Distance.Label"));
        props.setLook(wlDistanceField);
        fdlDistanceField = new FormData();
        fdlDistanceField.left = new FormAttachment(0, 0);
        fdlDistanceField.top = new FormAttachment(wDynamicDistance, margin);
        fdlDistanceField.right = new FormAttachment(middle, -margin);
        wlDistanceField.setLayoutData(fdlDistanceField);

        wDistanceValue = new TextVar(transMeta, wInputGroup, SWT.BORDER | SWT.READ_ONLY);
        wDistanceValue.setToolTipText(BaseMessages.getString(PKG, "GisRelate.DistanceValue.ToolTip"));
        wDistanceValue.setEditable(true);
        props.setLook(wDistanceValue);
        wDistanceValue.addModifyListener(lsMod);
        fdDistanceValue = new FormData();
        fdDistanceValue.left = new FormAttachment(middle, 0);
        fdDistanceValue.top = new FormAttachment(wDynamicDistance, margin);
        fdDistanceValue.width = 150;
        wDistanceValue.setLayoutData(fdDistanceValue);

        wDistanceField = new CCombo(wInputGroup, SWT.BORDER | SWT.READ_ONLY);
        wDistanceField.setToolTipText(BaseMessages.getString(PKG, "GisRelate.DistanceFieldName.ToolTip"));
        wDistanceField.setEditable(false);
        props.setLook(wDistanceField);
        wDistanceField.addModifyListener(lsMod);
        fdDistanceField = new FormData();
        // fdDistanceField.left = new FormAttachment(middle, 0);
        fdDistanceField.left = new FormAttachment(wDistanceValue, margin);
        fdDistanceField.right = new FormAttachment(100, 0);
        fdDistanceField.top = new FormAttachment(wDynamicDistance, margin);
        wDistanceField.setLayoutData(fdDistanceField);

        fdInputGroup = new FormData();
        fdInputGroup.left = new FormAttachment(0, margin);
        fdInputGroup.right = new FormAttachment(100, -margin);
        fdInputGroup.top = new FormAttachment(wOperatorDescription, margin);
        wInputGroup.setLayoutData(fdInputGroup);

        // ///////////////////////////////////////////////
        // Début du groupe : Output

        wOutputGroup = new Group(shell, SWT.SHADOW_NONE);
        props.setLook(wOutputGroup);
        wOutputGroup.setText(BaseMessages.getString(PKG, "GisRelate.OutputGroup.Label"));

        FormLayout OutputGroupLayout = new FormLayout();
        OutputGroupLayout.marginWidth = 5;
        OutputGroupLayout.marginHeight = 5;
        wOutputGroup.setLayout(OutputGroupLayout);

        // Type de résultat
        wlReturnType = new Label(wOutputGroup, SWT.RIGHT);
        wlReturnType.setText(BaseMessages.getString(PKG, "GisRelate.ReturnType.Label"));
        props.setLook(wlReturnType);
        fdlReturnType = new FormData();
        fdlReturnType.left = new FormAttachment(0, 0);
        fdlReturnType.top = new FormAttachment(0, margin);
        fdlReturnType.right = new FormAttachment(middle, -margin);
        wlReturnType.setLayoutData(fdlReturnType);

        wReturnType = new CCombo(wOutputGroup, SWT.BORDER | SWT.READ_ONLY);
        wReturnType.setToolTipText(BaseMessages.getString(PKG, "GisRelate.ReturnType.ToolTip"));
        wReturnType.setEditable(false);
        props.setLook(wReturnType);
        wReturnType.addModifyListener(lsMod);
        fdReturnType = new FormData();
        fdReturnType.left = new FormAttachment(middle, 0);
        fdReturnType.right = new FormAttachment(100, 0);
        fdReturnType.top = new FormAttachment(wSecondGeometryField, margin);
        wReturnType.setLayoutData(fdReturnType);
        wReturnType.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent arg0) {
                setReturnTypeFlags();
            }
        });

        // Résultat du test
        wlOutputField = new Label(wOutputGroup, SWT.RIGHT);
        wlOutputField.setText(BaseMessages.getString(PKG, "GisRelate.OutputFieldName.Label"));
        props.setLook(wlOutputField);
        fdlOutputField = new FormData();
        fdlOutputField.left = new FormAttachment(0, 0);
        fdlOutputField.top = new FormAttachment(wReturnType, margin);
        fdlOutputField.right = new FormAttachment(middle, -margin);
        wlOutputField.setLayoutData(fdlOutputField);

        wOutputField = new Text(wOutputGroup, SWT.BORDER | SWT.READ_ONLY);
        wOutputField.setToolTipText(BaseMessages.getString(PKG, "GisRelate.OutputFieldName.ToolTip"));
        wOutputField.setEditable(true);
        props.setLook(wOutputField);
        wOutputField.addModifyListener(lsMod);
        fdOutputField = new FormData();
        fdOutputField.left = new FormAttachment(middle, 0);
        fdOutputField.right = new FormAttachment(100, 0);
        fdOutputField.top = new FormAttachment(wReturnType, margin);
        wOutputField.setLayoutData(fdOutputField);

        fdOutputGroup = new FormData();
        fdOutputGroup.left = new FormAttachment(0, margin);
        fdOutputGroup.right = new FormAttachment(100, -margin);
        fdOutputGroup.top = new FormAttachment(wInputGroup, margin);
        wOutputGroup.setLayoutData(fdOutputGroup);

        // Boutons Ok et Annuler
        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));

        setButtonPositions(new Button[] { wOK, wCancel }, margin, wOutputGroup);
        lsCancel = new Listener() {
            public void handleEvent(Event e) {
                cancel();
            }
        };
        lsOK = new Listener() {
            public void handleEvent(Event e) {
                ok();
            }
        };

        wCancel.addListener(SWT.Selection, lsCancel);
        wOK.addListener(SWT.Selection, lsOK);
        lsDef = new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent e) {
                ok();
            }
        };
        wStepname.addSelectionListener(lsDef);

        shell.addShellListener(new ShellAdapter() {
            public void shellClosed(ShellEvent e) {
                cancel();
            }
        });

        wFirstGeometryField.setItems(getFieldsFromType(ValueMeta.getTypeDesc(ValueMetaGeometry.TYPE_GEOMETRY)));
        wSecondGeometryField.setItems(getFieldsFromType(ValueMeta.getTypeDesc(ValueMetaGeometry.TYPE_GEOMETRY)));
        wDistanceField.setItems(getFieldsFromType(ValueMeta.getTypeDesc(ValueMeta.TYPE_NUMBER)));
        loadData();
        // setReturnTypeFlags();
        // setOperatorFlags
        input.setChanged(changed);
        setSize();

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }

        return stepname;
    }

    // Charge les données dans le formulaire
    public void loadData() {

        // Liste des opérateurs
        List<String> operators = new ArrayList<String>();
        for (String operator : input.getBoolResultOperators()) {
            operators.add(getOperatorLabel(operator));
        }
        for (String operator : input.getNumericResultOperators()) {
            operators.add(getOperatorLabel(operator));
        }
        Collections.sort(operators);
        wOperator.setItems(operators.toArray(new String[operators.size()]));

        // Liste des types de résultats
        List<String> returnTypes = new ArrayList<String>();
        for (String returnType : input.getReturnTypes()) {
            returnTypes.add(getReturnTypeLabel(returnType));
        }
        Collections.sort(returnTypes);
        wReturnType.setItems(returnTypes.toArray(new String[returnTypes.size()]));

        if (input.getOperator() != null) {
            wOperator.setText(getOperatorLabel(input.getOperator()));
        }

        if (input.getReturnType() != null) {
            wReturnType.setText(getReturnTypeLabel(input.getReturnType()));
        }

        if (input.getFirstGeometryFieldName() != null) {
            wFirstGeometryField.setText(input.getFirstGeometryFieldName());
        }

        wDynamicDistance.setSelection(input.isDynamicDistance());
        wDistanceField.setEnabled(wDynamicDistance.getSelection());
        wDistanceValue.setEnabled(!wDynamicDistance.getSelection());

        if (input.getDistanceFieldName() != null) {
            wDistanceField.setText(input.getDistanceFieldName());
        }

        if (input.getDistanceValue() != null) {
            wDistanceValue.setText(input.getDistanceValue());
        }

        if (input.getSecondGeometryFieldName() != null) {
            wSecondGeometryField.setText(input.getSecondGeometryFieldName());
        }

        if (input.getOutputFieldName() != null) {
            wOutputField.setText(input.getOutputFieldName());
        }

        wStepname.selectAll();
    }

    private void cancel() {
        stepname = null;
        input.setChanged(backupChanged);
        dispose();
    }

    private void ok() {

        stepname = wStepname.getText();
        input.setOperator(getOperatorKey(wOperator.getText()));
        input.setReturnType(getReturnTypeKey(wReturnType.getText()));
        input.setFirstGeometryFieldName(wFirstGeometryField.getText());
        input.setSecondGeometryFieldName(wSecondGeometryField.getText());
        input.setDynamicDistance(wDynamicDistance.getSelection());
        input.setDistanceFieldName(wDistanceField.getText());
        input.setDistanceValue(wDistanceValue.getText());
        input.setOutputFieldName(wOutputField.getText());

        dispose();
    }

    // Si besoin de distance
    private void setOperatorFlags() {

        String operatorKey = getOperatorKey(wOperator.getText());

        if (operatorKey != null) {

            wOperatorDescription.setText(BaseMessages.getString(PKG, "GisRelate.Operator." + operatorKey + ".Description"));

            // Besoin de distance
            if (ArrayUtils.contains(input.getWithDistanceOperators(), operatorKey)) {

                wlDynamicDistance.setEnabled(true);
                wDynamicDistance.setEnabled(true);
                wlDistanceField.setEnabled(true);

                if (wDynamicDistance.getSelection()) {
                    wDistanceField.setEnabled(true);
                    wDistanceValue.setEnabled(false);
                    wDistanceValue.setText("");
                } else {
                    wDistanceField.setEnabled(false);
                    wDistanceField.setText("");
                    wDistanceValue.setEnabled(true);
                }

            } else {

                wlDynamicDistance.setEnabled(false);
                wDynamicDistance.setEnabled(false);
                wDynamicDistance.setSelection(false);

                wlDistanceField.setEnabled(false);
                wDistanceField.setEnabled(false);
                wDistanceField.setText("");

                wDistanceValue.setEnabled(false);
                wDistanceValue.setText("");
            }

            if (ArrayUtils.contains(input.getBoolResultOperators(), operatorKey)) {

                wlReturnType.setEnabled(true);
                wReturnType.setEnabled(true);

            } else {

                wlReturnType.setEnabled(false);
                wReturnType.setEnabled(false);
                wReturnType.setText(getReturnTypeLabel("ALL"));

            }
        }

    }

    // Si filtrage de lignes
    private void setReturnTypeFlags() {

        String operatorKey = getOperatorKey(wOperator.getText());

        if (operatorKey != null) {

            if (getReturnTypeKey(wReturnType.getText()).equalsIgnoreCase("ALL")) {

                wlOutputField.setEnabled(true);
                wOutputField.setEnabled(true);

            } else {

                wlOutputField.setEnabled(false);
                wOutputField.setEnabled(false);
                wOutputField.setText("");
            }
        }

    }

    // Libellé de l'opérateur en fonction du code
    private String getOperatorLabel(String key) {

        return BaseMessages.getString(PKG, "GisRelate.Operator." + key + ".Label");

    }

    // Code de l'opérateur en fonction du libellé
    private String getOperatorKey(String label) {

        for (String key : input.getBoolResultOperators()) {

            if (label.equalsIgnoreCase(BaseMessages.getString(PKG, "GisRelate.Operator." + key + ".Label"))) {
                return key;
            }
        }

        for (String key : input.getNumericResultOperators()) {

            if (label.equalsIgnoreCase(BaseMessages.getString(PKG, "GisRelate.Operator." + key + ".Label"))) {
                return key;
            }
        }

        return null;

    }

    // Libellé de du type de resultat en fonction du code
    private String getReturnTypeLabel(String key) {

        return BaseMessages.getString(PKG, "GisRelate.ReturnType." + key + ".Label");

    }

    // Code du type de resultat en fonction du libellé
    private String getReturnTypeKey(String label) {

        for (String key : input.getReturnTypes()) {

            if (label.equalsIgnoreCase(BaseMessages.getString(PKG, "GisRelate.ReturnType." + key + ".Label"))) {
                return key;
            }
        }

        return null;

    }

    // Liste les colonnes d'un certain type
    private String[] getFieldsFromType(String type) {

        String fieldNamesFromType[] = null;

        try {

            // Récupération des colonnes de l'étape précédente
            RowMetaInterface r = transMeta.getPrevStepFields(stepname);
            if (r != null) {

                // Filtrage par type de colonne texte
                TreeSet<String> fieldsTree = new TreeSet<String>();
                String[] fieldNames = r.getFieldNames();
                String[] fieldNamesAndTypes = r.getFieldNamesAndTypes(0);

                for (int i = 0; i < fieldNames.length; i++) {
                    if (fieldNamesAndTypes[i].toLowerCase().contains(type.toLowerCase())) {
                        if (fieldNames[i] != null && !fieldNames[i].isEmpty()) {
                            fieldsTree.add(fieldNames[i]);
                        }
                    }
                }

                fieldNamesFromType = fieldsTree.toArray(new String[] {});

            }

        } catch (KettleException ke) {
            new ErrorDialog(shell, BaseMessages.getString(PKG, "ChangeFileEncodingDialog.FailedToGetFields.DialogTitle"), BaseMessages.getString(PKG,
                    "ChangeFileEncodingDialog.FailedToGetFields.DialogMessage"), ke);
        }

        return fieldNamesFromType;

    }

}
