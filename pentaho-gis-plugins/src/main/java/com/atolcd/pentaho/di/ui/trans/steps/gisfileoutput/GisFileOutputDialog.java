package com.atolcd.pentaho.di.ui.trans.steps.gisfileoutput;

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
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;

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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.dialog.EnterStringDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import com.atolcd.pentaho.di.core.row.value.ValueMetaGeometry;
import com.atolcd.pentaho.di.trans.steps.gisfileoutput.GisFileOutputMeta;
import com.atolcd.pentaho.di.trans.steps.gisfileoutput.GisOutputFormatDef;
import com.atolcd.pentaho.di.trans.steps.gisfileoutput.GisOutputFormatParameter;
import com.atolcd.pentaho.di.trans.steps.gisfileoutput.GisOutputFormatParameterDef;

public class GisFileOutputDialog extends BaseStepDialog implements StepDialogInterface {

    private static Class<?> PKG = GisFileOutputMeta.class;

    // Groupes de contrôles
    private Group wOptionnalGroup;
    private FormData fdOptionnalGroup;

    // Type de fichier
    private Label wlOutputFormat;
    private CCombo wOutputFormat;
    private FormData fdlOutputFormat, fdOutputFormat;

    // Paramètres de champs
    private Label wlFieldParams;
    private TableView wFieldParams;
    private FormData fdlFieldParams, fdFieldParams;

    // Paramètres fixes
    private Label wlFixedParams;
    private TableView wFixedParams;
    private FormData fdlFixedParams, fdFixedParams;

    // Nom du fichier à écrire
    private Label wlFileName;
    private Button wbFileName;
    private TextVar wFileName;
    private FormData fdlFileName, fdbFileName, fdFileName;

    // Colonne contenant la géométrie
    private Label wlGeometryField;
    private CCombo wGeometryField;
    private FormData fdlGeometryField, fdGeometryField;

    // Encodage
    private Label wlEncoding;
    private CCombo wEncoding;
    private FormData fdlEncoding, fdEncoding;

    // Ne pas créer le fichier au démarrage
    private Label wlCreateFileAtEnd;
    private Button wCreateFileAtEnd;
    private FormData fdlCreateFileAtEnd, fdCreateFileAtEnd;

    // Sortie vers servlet
    private Label wlDataToServlet;
    private Button wDataToServlet;
    private FormData fdlDataToServlet, fdDataToServlet;

    private ColumnInfo[] paramsFieldColumnInfo;
    private ColumnInfo[] paramsFixedColumnInfo;

    private GisFileOutputMeta input;

    public GisFileOutputDialog(Shell parent, Object in, TransMeta tr, String sname) {

        super(parent, (BaseStepMeta) in, tr, sname);
        input = (GisFileOutputMeta) in;
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
        shell.setText(BaseMessages.getString(PKG, "GisFileOutput.Shell.Title"));

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

        // Type de fichier
        wlOutputFormat = new Label(shell, SWT.RIGHT);
        wlOutputFormat.setText(BaseMessages.getString(PKG, "GisFileOutput.FileFormat.Label"));
        props.setLook(wlOutputFormat);
        fdlOutputFormat = new FormData();
        fdlOutputFormat.left = new FormAttachment(0, 0);
        fdlOutputFormat.right = new FormAttachment(middle, -margin);
        fdlOutputFormat.top = new FormAttachment(wStepname, margin);
        wlOutputFormat.setLayoutData(fdlOutputFormat);

        wOutputFormat = new CCombo(shell, SWT.BORDER | SWT.READ_ONLY);
        wOutputFormat.setToolTipText(BaseMessages.getString(PKG, "GisFileOutput.FileFormat.ToolTip"));
        wOutputFormat.setEditable(false);
        props.setLook(wOutputFormat);
        wOutputFormat.addModifyListener(lsMod);
        fdOutputFormat = new FormData();
        fdOutputFormat.left = new FormAttachment(middle, 0);
        fdOutputFormat.right = new FormAttachment(100, 0);
        fdOutputFormat.top = new FormAttachment(wStepname, margin);
        wOutputFormat.setLayoutData(fdOutputFormat);
        wOutputFormat.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent arg0) {

                wFileName.setText("");

                if (wOutputFormat.getText() != null && !wOutputFormat.getText().isEmpty()) {

                    // Si ESRI_SHP ou SPATIALITE ou SQLITE ou DXF pas de servlet
                    if (getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("ESRI_SHP") || getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("SPATIALITE")
                            || getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("SQLITE") || getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("DXF")) {

                        wlDataToServlet.setEnabled(false);
                        wDataToServlet.setEnabled(false);
                        wDataToServlet.setSelection(false);

                    } else {

                        wlDataToServlet.setEnabled(true);
                        wDataToServlet.setEnabled(true);

                    }

                    // Si KML ou Spatalite ou sqlite UTF8 forcé
                    if (getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("KML") || getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("SPATIALITE")
                            || getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("SQLITE") || getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("DXF")) {

                        wEncoding.setEnabled(false);
                        wlEncoding.setEnabled(false);
                        
                        if (getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("KML") || getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("SPATIALITE")
                                || getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("SQLITE")){
                        
                        	wEncoding.setText("UTF-8");
                        
                        }else{
                        	//DXF
                        	wEncoding.setText("windows-1252");
                        }

                    } else {

                        wEncoding.setEnabled(true);
                        wlEncoding.setEnabled(true);

                    }

                    // Si sqlite : pas besoin de géométrie
                    if (getFormatKey(wOutputFormat.getText()).equalsIgnoreCase("SQLITE")) {

                        wlGeometryField.setEnabled(false);
                        wGeometryField.setEnabled(false);
                        wGeometryField.setText("");

                    } else {
                        wlGeometryField.setEnabled(true);
                        wGeometryField.setEnabled(true);
                    }

                }

                clearTables();
            }
        });

        // Sortie vers servlet
        wlDataToServlet = new Label(shell, SWT.RIGHT);
        wlDataToServlet.setText(BaseMessages.getString(PKG, "GisFileOutput.DataToServlet.Label"));
        props.setLook(wlDataToServlet);
        fdlDataToServlet = new FormData();
        fdlDataToServlet.left = new FormAttachment(0, 0);
        fdlDataToServlet.top = new FormAttachment(wOutputFormat, margin);
        fdlDataToServlet.right = new FormAttachment(middle, -margin);
        wlDataToServlet.setLayoutData(fdlDataToServlet);

        wDataToServlet = new Button(shell, SWT.CHECK);
        wDataToServlet.setToolTipText(BaseMessages.getString(PKG, "GisFileOutput.DataToServlet.ToolTip"));
        props.setLook(wDataToServlet);
        fdDataToServlet = new FormData();
        fdDataToServlet.left = new FormAttachment(middle, 0);
        fdDataToServlet.top = new FormAttachment(wOutputFormat, margin);
        fdDataToServlet.right = new FormAttachment(100, 0);
        wDataToServlet.setLayoutData(fdDataToServlet);
        wDataToServlet.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent arg0) {
                setDataToServletFlags();
            }
        });

        // Ne pas créer de fichier au démmarage
        wlCreateFileAtEnd = new Label(shell, SWT.RIGHT);
        wlCreateFileAtEnd.setText(BaseMessages.getString(PKG, "GisFileOutput.CreateFileAtEnd.Label"));
        props.setLook(wlCreateFileAtEnd);
        fdlCreateFileAtEnd = new FormData();
        fdlCreateFileAtEnd.left = new FormAttachment(0, 0);
        fdlCreateFileAtEnd.top = new FormAttachment(wDataToServlet, margin);
        fdlCreateFileAtEnd.right = new FormAttachment(middle, -margin);
        wlCreateFileAtEnd.setLayoutData(fdlCreateFileAtEnd);

        wCreateFileAtEnd = new Button(shell, SWT.CHECK);
        wCreateFileAtEnd.setToolTipText(BaseMessages.getString(PKG, "GisFileOutput.CreateFileAtEnd.ToolTip"));
        props.setLook(wCreateFileAtEnd);
        fdCreateFileAtEnd = new FormData();
        fdCreateFileAtEnd.left = new FormAttachment(middle, 0);
        fdCreateFileAtEnd.top = new FormAttachment(wDataToServlet, margin);
        fdCreateFileAtEnd.right = new FormAttachment(100, 0);
        wCreateFileAtEnd.setLayoutData(fdCreateFileAtEnd);

        // Fichier à écrire
        wlFileName = new Label(shell, SWT.RIGHT);
        wlFileName.setText(BaseMessages.getString(PKG, "GisFileOutput.FileName.Label"));
        props.setLook(wlFileName);
        fdlFileName = new FormData();
        fdlFileName.left = new FormAttachment(0, 0);
        fdlFileName.right = new FormAttachment(middle, -margin);
        fdlFileName.top = new FormAttachment(wCreateFileAtEnd, margin);
        wlFileName.setLayoutData(fdlFileName);

        wbFileName = new Button(shell, SWT.PUSH | SWT.CENTER);
        props.setLook(wbFileName);
        wbFileName.setText(BaseMessages.getString(PKG, "System.Button.Browse"));
        fdbFileName = new FormData();
        fdbFileName.right = new FormAttachment(100, 0);
        fdbFileName.top = new FormAttachment(wCreateFileAtEnd, margin);
        wbFileName.setLayoutData(fdbFileName);
        wbFileName.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {

                if (wOutputFormat.getText() != null && !wOutputFormat.getText().isEmpty()) {

                    String[] extensions = input.getOutputFormatDefs().get(getFormatKey(wOutputFormat.getText())).getExtensions();
                    String[] extensionsNames = input.getOutputFormatDefs().get(getFormatKey(wOutputFormat.getText())).getExtensionsNames();

                    FileDialog dialog = new FileDialog(shell, SWT.SAVE);
                    dialog.setFilterExtensions(extensions);
                    dialog.setFilterNames(extensionsNames);
                    if (wFileName.getText() != null) {
                        dialog.setFileName(transMeta.environmentSubstitute(wFileName.getText()));
                    }

                    if (dialog.open() != null) {
                        String str = dialog.getFilterPath() + Const.FILE_SEPARATOR + dialog.getFileName();
                        wFileName.setText(str);
                    }
                }
            }
        });

        wFileName = new TextVar(transMeta, shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wFileName.setToolTipText(BaseMessages.getString(PKG, "GisFileOutput.FileName.ToolTip"));
        props.setLook(wFileName);
        wFileName.addModifyListener(lsMod);
        fdFileName = new FormData();
        fdFileName.left = new FormAttachment(middle, 0);
        fdFileName.right = new FormAttachment(wbFileName, -margin);
        fdFileName.top = new FormAttachment(wCreateFileAtEnd, margin);
        wFileName.setLayoutData(fdFileName);

        // Colonne géométrie
        wlGeometryField = new Label(shell, SWT.RIGHT);
        wlGeometryField.setText(BaseMessages.getString(PKG, "GisFileOutput.GeometryFieldName.Label"));
        props.setLook(wlGeometryField);
        fdlGeometryField = new FormData();
        fdlGeometryField.left = new FormAttachment(0, 0);
        fdlGeometryField.top = new FormAttachment(wbFileName, margin);
        fdlGeometryField.right = new FormAttachment(middle, -margin);
        wlGeometryField.setLayoutData(fdlGeometryField);

        wGeometryField = new CCombo(shell, SWT.BORDER | SWT.READ_ONLY);
        wGeometryField.setToolTipText(BaseMessages.getString(PKG, "GisFileOutput.GeometryFieldName.ToolTip"));
        wGeometryField.setEditable(true);
        props.setLook(wGeometryField);
        wGeometryField.addModifyListener(lsMod);
        fdGeometryField = new FormData();
        fdGeometryField.left = new FormAttachment(middle, 0);
        fdGeometryField.right = new FormAttachment(100, 0);
        fdGeometryField.top = new FormAttachment(wbFileName, margin);
        wGeometryField.setLayoutData(fdGeometryField);

        // ///////////////////////////////////////////////
        // Début du groupe : Options

        wOptionnalGroup = new Group(shell, SWT.SHADOW_NONE);
        props.setLook(wOptionnalGroup);
        wOptionnalGroup.setText(BaseMessages.getString(PKG, "GisFileOutput.Optionnal.Label"));

        FormLayout optionnalGroupLayout = new FormLayout();
        optionnalGroupLayout.marginWidth = 5;
        optionnalGroupLayout.marginHeight = 5;
        wOptionnalGroup.setLayout(optionnalGroupLayout);

        // Encodage
        wlEncoding = new Label(wOptionnalGroup, SWT.RIGHT);
        wlEncoding.setText(BaseMessages.getString(PKG, "GisFileOutput.Encoding.Label"));
        props.setLook(wlEncoding);
        fdlEncoding = new FormData();
        fdlEncoding.left = new FormAttachment(0, 0);
        fdlEncoding.top = new FormAttachment(0, margin);
        fdlEncoding.right = new FormAttachment(middle, -margin);
        wlEncoding.setLayoutData(fdlEncoding);

        wEncoding = new CCombo(wOptionnalGroup, SWT.BORDER | SWT.READ_ONLY);
        wEncoding.setToolTipText(BaseMessages.getString(PKG, "GisFileOutput.Encoding.ToolTip"));
        wEncoding.setEditable(true);
        props.setLook(wEncoding);
        wEncoding.addModifyListener(lsMod);
        fdEncoding = new FormData();
        fdEncoding.left = new FormAttachment(middle, 0);
        fdEncoding.right = new FormAttachment(100, 0);
        fdEncoding.top = new FormAttachment(0, margin);
        wEncoding.setLayoutData(fdEncoding);

        fdOptionnalGroup = new FormData();
        fdOptionnalGroup.left = new FormAttachment(0, margin);
        fdOptionnalGroup.right = new FormAttachment(100, -margin);
        fdOptionnalGroup.top = new FormAttachment(wGeometryField, margin);
        wOptionnalGroup.setLayoutData(fdOptionnalGroup);

        // Fin du groupe : Options
        // ///////////////////////////////////////////////

        // Paramètres fixes
        wlFixedParams = new Label(shell, SWT.NONE);
        wlFixedParams.setText(BaseMessages.getString(PKG, "GisFileOutput.Params.Fixed.Label"));
        props.setLook(wlFixedParams);
        fdlFixedParams = new FormData();
        fdlFixedParams.left = new FormAttachment(0, 0);
        fdlFixedParams.top = new FormAttachment(wOptionnalGroup, margin);
        wlFixedParams.setLayoutData(fdlFixedParams);

        paramsFixedColumnInfo = getParamsFixedColumnInfo();
        wFixedParams = new TableView(transMeta, shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, paramsFixedColumnInfo, 0, lsMod, props);
        wFixedParams.optWidth(true);
        wFixedParams.setReadonly(true);
        fdFixedParams = new FormData();
        fdFixedParams.left = new FormAttachment(0, 0);
        fdFixedParams.top = new FormAttachment(wlFixedParams, margin);
        fdFixedParams.right = new FormAttachment(100, 0);
        fdFixedParams.height = 150;
        wFixedParams.setLayoutData(fdFixedParams);

        // Boutons Ok et Annuler
        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
        setButtonPositions(new Button[] { wOK, wCancel }, margin, null);

        // Paramètres de champs
        wlFieldParams = new Label(shell, SWT.NONE);
        wlFieldParams.setText(BaseMessages.getString(PKG, "GisFileOutput.Params.Field.Label"));
        props.setLook(wlFieldParams);
        fdlFieldParams = new FormData();
        fdlFieldParams.left = new FormAttachment(0, 0);
        fdlFieldParams.top = new FormAttachment(wFixedParams, margin);
        wlFieldParams.setLayoutData(fdlFieldParams);

        paramsFieldColumnInfo = getParamsFieldColumnInfo();
        wFieldParams = new TableView(transMeta, shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, paramsFieldColumnInfo, 0, lsMod, props);
        wFieldParams.optWidth(true);
        wFieldParams.setReadonly(true);
        fdFieldParams = new FormData();
        fdFieldParams.left = new FormAttachment(0, 0);
        fdFieldParams.top = new FormAttachment(wlFieldParams, margin);
        fdFieldParams.right = new FormAttachment(100, 0);
        fdFieldParams.bottom = new FormAttachment(wOK, -margin);
        wFieldParams.setLayoutData(fdFieldParams);

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

        loadEncodings();
        wGeometryField.setItems(getFieldsFromType(ValueMeta.getTypeDesc(ValueMetaGeometry.TYPE_GEOMETRY), false));
        loadData();
        setDataToServletFlags();
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

        // Liste des formats
        List<String> formatsLabels = new ArrayList<String>();
        for (Entry<String, GisOutputFormatDef> formatsDefs : input.getOutputFormatDefs().entrySet()) {
            formatsLabels.add(getFormatLabel(formatsDefs.getKey()));
        }
        Collections.sort(formatsLabels);
        wOutputFormat.setItems(formatsLabels.toArray(new String[formatsLabels.size()]));

        if (input.getOutputFormat() != null && !input.getOutputFormat().isEmpty()) {

            String formatKey = input.getOutputFormat();
            wOutputFormat.setText(getFormatLabel(formatKey));

            // Tableau de valeur des paramètres figés
            Table fixedParamsTable = wFixedParams.table;
            fixedParamsTable.removeAll();

            if (!input.getOutputFormatFixedParameters().isEmpty()) {

                int i = 0;
                for (GisOutputFormatParameter parameter : input.getOutputFormatFixedParameters()) {

                    TableItem tableItem = new TableItem(fixedParamsTable, SWT.NONE);
                    tableItem.setText(0, String.valueOf(i));
                    tableItem.setText(1, getParamLabel(parameter.getKey()));
                    GisOutputFormatParameterDef parameterDef = input.getOutputFormatDefs().get(formatKey).getParameterFixedDef(parameter.getKey());
                    tableItem.setText(2, BaseMessages.getString(PKG, "GisFileOutput.Params.Required." + String.valueOf(parameterDef.isRequired()).toUpperCase() + ".Label"));
                    if (parameter.getValue() != null) {
                        tableItem.setText(3, getParamFixedValueLabel(parameter.getValue().toString()));
                        // tableItem.setText(3,
                        // getParamLabel(parameter.getValue().toString()));
                    }
                    i++;

                }

                wFixedParams.setRowNums();
                if (wFixedParams.nrNonEmpty() > 0) {
                    wlFixedParams.setEnabled(true);
                    wFixedParams.setEnabled(true);
                } else {
                    wlFixedParams.setEnabled(false);
                    wFixedParams.setEnabled(false);
                }

            } else {

                clearFixedParamTable();
            }

            // Paramètres dynamiques
            Table fieldParamsTable = wFieldParams.table;
            fieldParamsTable.removeAll();

            if (!input.getOutputFormatFieldParameters().isEmpty()) {
                int j = 0;
                for (GisOutputFormatParameter parameter : input.getOutputFormatFieldParameters()) {

                    TableItem tableItem = new TableItem(fieldParamsTable, SWT.NONE);
                    tableItem.setText(0, String.valueOf(j));
                    tableItem.setText(1, getParamLabel(parameter.getKey()));
                    GisOutputFormatParameterDef parameterDef = input.getOutputFormatDefs().get(formatKey).getParameterFieldDef(parameter.getKey());
                    tableItem.setText(2, BaseMessages.getString(PKG, "GisFileOutput.Params.Required." + String.valueOf(parameterDef.isRequired()).toUpperCase() + ".Label"));
                    if (parameter.getValue() != null) {
                        tableItem.setText(3, parameter.getValue().toString());
                    }
                    j++;

                }

                wFieldParams.setRowNums();
                if (wFieldParams.nrNonEmpty() > 0) {
                    wlFieldParams.setEnabled(true);
                    wFieldParams.setEnabled(true);
                } else {
                    wlFieldParams.setEnabled(false);
                    wFieldParams.setEnabled(false);
                }

            } else {

                clearFieldParamTable();
            }

        } else {
            clearTables();
        }

        if (input.getOutputFileName() != null) {
            wFileName.setText(input.getOutputFileName());
        }

        if (input.getGeometryFieldName() != null) {
            wGeometryField.setText(input.getGeometryFieldName());
        }

        if (input.getEncoding() != null) {
            wEncoding.setText(input.getEncoding());
        }

        wCreateFileAtEnd.setSelection(input.isCreateFileAtEnd());
        wDataToServlet.setSelection(input.isDataToServlet());

        wStepname.selectAll();
    }

    private void cancel() {
        stepname = null;
        input.setChanged(backupChanged);
        dispose();
    }

    private void ok() {

        stepname = wStepname.getText();

        String formatKey = getFormatKey(wOutputFormat.getText());
        input.setOutputFormat(formatKey);

        List<GisOutputFormatParameter> outputFormatFixedParameters = new ArrayList<GisOutputFormatParameter>();
        for (int i = 0; i < wFixedParams.nrNonEmpty(); i++) {

            TableItem tableItem = wFixedParams.getNonEmpty(i);

            String paramKey = getParamKey(formatKey, GisOutputFormatParameterDef.TYPE_FIXED, tableItem.getText(1));

            if (tableItem.getText(3) != null && !tableItem.getText(3).isEmpty()) {
                String value = getParamFixedValueKey(formatKey, paramKey, tableItem.getText(3));
                outputFormatFixedParameters.add(new GisOutputFormatParameter(paramKey, value));
            } else {
                outputFormatFixedParameters.add(new GisOutputFormatParameter(paramKey, ""));
            }

        }
        input.setOutputFormatFixedParameters(outputFormatFixedParameters);

        List<GisOutputFormatParameter> outputFormatFieldParameters = new ArrayList<GisOutputFormatParameter>();
        for (int i = 0; i < wFieldParams.nrNonEmpty(); i++) {

            TableItem tableItem = wFieldParams.getNonEmpty(i);

            String paramKey = getParamKey(formatKey, GisOutputFormatParameterDef.TYPE_FIELD, tableItem.getText(1));

            if (tableItem.getText(3) != null && !tableItem.getText(3).isEmpty()) {
                outputFormatFieldParameters.add(new GisOutputFormatParameter(paramKey, tableItem.getText(3)));
            } else {
                outputFormatFieldParameters.add(new GisOutputFormatParameter(paramKey, ""));
            }
        }
        input.setOutputFormatFieldParameters(outputFormatFieldParameters);

        input.setOutputFileName(wFileName.getText());
        input.setGeometryFieldName(wGeometryField.getText());
        input.setEncoding(wEncoding.getText());
        input.setCreateFileAtEnd(wCreateFileAtEnd.getSelection());
        input.setDataToServlet(wDataToServlet.getSelection());

        dispose();

    }

    // Liste les colonnes d'un certain type
    private String[] getFieldsFromType(String type, boolean includeBlank) {

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

                if (includeBlank) {
                    fieldsTree.add("");
                }

                fieldNamesFromType = fieldsTree.toArray(new String[] {});

            }

        } catch (KettleException ke) {
            new ErrorDialog(shell, BaseMessages.getString(PKG, "ChangeFileEncodingDialog.FailedToGetFields.DialogTitle"), BaseMessages.getString(PKG,
                    "ChangeFileEncodingDialog.FailedToGetFields.DialogMessage"), ke);
        }

        return fieldNamesFromType;

    }

    // Liste des encodages
    private void loadEncodings() {

        wEncoding.removeAll();
        List<Charset> values = new ArrayList<Charset>(Charset.availableCharsets().values());
        for (int i = 0; i < values.size(); i++) {
            Charset charSet = (Charset) values.get(i);
            wEncoding.add(charSet.displayName());
        }

        // Celui par défaut en environnement
        String defEncoding = Const.getEnvironmentVariable("file.encoding", "UTF-8");
        int idx = Const.indexOfString(defEncoding, wEncoding.getItems());
        if (idx >= 0)
            wEncoding.select(idx);

    }

    private void setDataToServletFlags() {

        if (wDataToServlet.getSelection()) {

            wCreateFileAtEnd.setSelection(false);
            wCreateFileAtEnd.setEnabled(false);
            wFileName.setText("");
            wFileName.setEnabled(false);
            wbFileName.setEnabled(false);

        } else {

            wCreateFileAtEnd.setEnabled(true);
            wFileName.setEnabled(true);
            wbFileName.setEnabled(true);

        }

    }

    // Code du format depuis son libellé
    private String getFormatKey(String label) {

        for (Entry<String, GisOutputFormatDef> formatDef : input.getOutputFormatDefs().entrySet()) {

            if (label.equalsIgnoreCase(BaseMessages.getString(PKG, "GisFileOutput.Format." + formatDef.getKey() + ".Label"))) {
                return formatDef.getKey();
            }
        }

        return null;

    }

    // Libellé du format depuis son code
    private String getFormatLabel(String key) {

        return BaseMessages.getString(PKG, "GisFileOutput.Format." + key + ".Label");

    }

    // Code du paramètre depuis son libellé
    private String getParamKey(String formatKey, String parameterType, String label) {

        if (parameterType.equalsIgnoreCase(GisOutputFormatParameterDef.TYPE_FIELD)) {

            for (GisOutputFormatParameterDef parameterDef : input.getOutputFormatDefs().get(formatKey).getParameterFieldDefs()) {

                if (label.equalsIgnoreCase(BaseMessages.getString(PKG, "GisFileOutput.Params." + parameterDef.getKey() + ".Label"))) {
                    return parameterDef.getKey();
                }
            }

        } else if (parameterType.equalsIgnoreCase(GisOutputFormatParameterDef.TYPE_FIXED)) {

            for (GisOutputFormatParameterDef parameterDef : input.getOutputFormatDefs().get(formatKey).getParameterFixedDefs()) {

                if (label.equalsIgnoreCase(BaseMessages.getString(PKG, "GisFileOutput.Params." + parameterDef.getKey() + ".Label"))) {
                    return parameterDef.getKey();
                }
            }

        }

        return null;

    }

    // Libellé du paramètre depuis son code
    private String getParamLabel(String key) {

        return BaseMessages.getString(PKG, "GisFileOutput.Params." + key + ".Label");

    }

    // Code de la valeur prédéfinie son libellé
    private String getParamFixedValueKey(String formatKey, String paramKey, String label) {

        for (String predefinedValueKey : input.getParameterPredefinedValues(formatKey, GisOutputFormatParameterDef.TYPE_FIXED, paramKey)) {

            if (label.equalsIgnoreCase(BaseMessages.getString(PKG, "GisFileOutput.Params.Predefined." + predefinedValueKey + ".Label"))) {
                return predefinedValueKey;
            }
        }

        return label;

    }

    // Libellé de la valeur prédéfinie depuis son code
    private String getParamFixedValueLabel(String key) {
        String label = BaseMessages.getString(PKG, "GisFileOutput.Params.Predefined." + key + ".Label");
        if (label.startsWith("!")) {
            return key;
        } else {
            return BaseMessages.getString(PKG, "GisFileOutput.Params.Predefined." + key + ".Label");
        }
    }

    private void clearFixedParamTable() {

        if (wOutputFormat.getText() != null && !wOutputFormat.getText().isEmpty()) {

            String inputFormatKey = getFormatKey(wOutputFormat.getText());

            // Paramètres fixes
            Table fixedParamsTable = wFixedParams.table;
            fixedParamsTable.removeAll();

            int i = 0;
            for (GisOutputFormatParameterDef parameterDef : input.getOutputFormatDefs().get(inputFormatKey).getParameterFixedDefs()) {

                TableItem tableItem = new TableItem(fixedParamsTable, SWT.NONE);
                tableItem.setText(0, String.valueOf(i));
                tableItem.setText(1, getParamLabel(parameterDef.getKey()));
                tableItem.setText(2, BaseMessages.getString(PKG, "GisFileOutput.Params.Required." + String.valueOf(parameterDef.isRequired()).toUpperCase() + ".Label"));
                String defaultValue = parameterDef.getDefaultValue();
                if (defaultValue != null && !defaultValue.isEmpty()) {

                    String value = getParamFixedValueLabel(parameterDef.getDefaultValue());
                    tableItem.setText(3, value);
                }
                i++;

            }

            wFixedParams.setRowNums();
            if (wFixedParams.nrNonEmpty() > 0) {
                wlFixedParams.setEnabled(true);
                wFixedParams.setEnabled(true);
            } else {
                wlFixedParams.setEnabled(false);
                wFixedParams.setEnabled(false);
            }

        }

        wStepname.selectAll();
    }

    private void clearFieldParamTable() {

        if (wOutputFormat.getText() != null && !wOutputFormat.getText().isEmpty()) {

            String inputFormatKey = getFormatKey(wOutputFormat.getText());

            // Paramètres dynamiques
            Table fieldParamsTable = wFieldParams.table;
            fieldParamsTable.removeAll();

            int j = 0;
            for (GisOutputFormatParameterDef parameterDef : input.getOutputFormatDefs().get(inputFormatKey).getParameterFieldDefs()) {

                TableItem tableItem = new TableItem(fieldParamsTable, SWT.NONE);
                tableItem.setText(0, String.valueOf(j));
                tableItem.setText(1, getParamLabel(parameterDef.getKey()));
                tableItem.setText(2, BaseMessages.getString(PKG, "GisFileOutput.Params.Required." + String.valueOf(parameterDef.isRequired()).toUpperCase() + ".Label"));
                tableItem.setText(3, "");

                j++;

            }

            wFieldParams.setRowNums();
            if (wFieldParams.nrNonEmpty() > 0) {
                wlFieldParams.setEnabled(true);
                wFieldParams.setEnabled(true);
            } else {
                wlFieldParams.setEnabled(false);
                wFieldParams.setEnabled(false);
            }

        }

        wStepname.selectAll();
    }

    // Réinitialise les tables des paramètres
    private void clearTables() {

        clearFixedParamTable();
        clearFieldParamTable();
        wStepname.selectAll();
    }

    private ColumnInfo[] getParamsFixedColumnInfo() {

        // Nom
        ColumnInfo nameColumnInfo = new ColumnInfo(BaseMessages.getString(PKG, "GisFileOutput.Params.Columns.PARAM_KEY.Label"), ColumnInfo.COLUMN_TYPE_TEXT);
        nameColumnInfo.setReadOnly(true);

        // Requis
        ColumnInfo requiredColumnInfo = new ColumnInfo(BaseMessages.getString(PKG, "GisFileOutput.Params.Columns.PARAM_REQUIRED.Label"), ColumnInfo.COLUMN_TYPE_TEXT);
        requiredColumnInfo.setReadOnly(true);

        // Valeur fixe ou prédéfinie
        ColumnInfo fixedValueColumnInfo = new ColumnInfo(BaseMessages.getString(PKG, "GisFileOutput.Params.Columns.PARAM_VALUE.Label"), ColumnInfo.COLUMN_TYPE_TEXT);
        fixedValueColumnInfo.setReadOnly(false);
        fixedValueColumnInfo.setSelectionAdapter(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {

                Integer tableIndex = wFixedParams.getSelectionIndex();
                TableItem tableItem = wFixedParams.table.getItem(tableIndex);

                // Récupération du type de donnée attendu pour le paramètre
                // sélectionné
                // et des valeurs prédénies
                String formatKey = getFormatKey(wOutputFormat.getText());
                String paramKey = getParamKey(formatKey, GisOutputFormatParameterDef.TYPE_FIXED, tableItem.getText(1));
                int paramValueMetaType = input.getParameterValueMetaType(formatKey, GisOutputFormatParameterDef.TYPE_FIXED, paramKey);

                // List<String>values =
                // input.getParameterPredefinedValues(formatKey,paramKey);
                List<String> values = new ArrayList<String>();
                for (String value : input.getParameterPredefinedValues(formatKey, GisOutputFormatParameterDef.TYPE_FIXED, paramKey)) {
                    values.add(getParamFixedValueLabel(value));
                }

                // Si valeurs prédéfinies, ajout des paramètres de la
                // transformation
                if (values.size() > 0) {

                    for (String parameter : transMeta.listParameters()) {
                        values.add("${" + parameter + "}");
                    }

                }

                // Paramétrage de la boîte de dialogue (commun)
                String dialogTitle = BaseMessages.getString(PKG, "GisFileOutput.Params.Dialog.PARAM_VALUE.Title");
                String dialogParamComment = BaseMessages.getString(PKG, "GisFileOutput.Params." + paramKey + ".Description") + " (" + ValueMeta.getTypeDesc(paramValueMetaType)
                        + ")";

                if (values.isEmpty()) {

                    // Saisie directe de la valeur du paramètre
                    EnterStringDialog dialog = new EnterStringDialog(shell, tableItem.getText(3), dialogTitle, dialogParamComment, true, transMeta);

                    // Ouverture de la boîte de dialogue
                    String selectedValue = dialog.open();
                    if (selectedValue != null) {
                        tableItem.setText(3, selectedValue);
                    }

                } else {

                    // Choix dans une liste
                    EnterSelectionDialog dialog = new EnterSelectionDialog(shell, values.toArray(new String[values.size()]), dialogTitle, dialogParamComment);
                    dialog.setMulti(false);
                    dialog.setViewOnly();
                    dialog.setAvoidQuickSearch();

                    // Ouverture de la boîte de dialogue
                    int index = dialog.getSelectionNr(tableItem.getText(3));
                    if (index >= 0) {
                        dialog.setSelectedNrs(new int[] { index });
                    }

                    String selectedValue = dialog.open();
                    if (selectedValue != null) {
                        tableItem.setText(3, selectedValue);
                    }

                }

            }

        });

        return new ColumnInfo[] { nameColumnInfo, requiredColumnInfo, fixedValueColumnInfo };
    }

    private ColumnInfo[] getParamsFieldColumnInfo() {

        // Nom
        ColumnInfo nameColumnInfo = new ColumnInfo(BaseMessages.getString(PKG, "GisFileOutput.Params.Columns.PARAM_KEY.Label"), ColumnInfo.COLUMN_TYPE_TEXT);
        nameColumnInfo.setReadOnly(true);

        // Requis
        ColumnInfo requiredColumnInfo = new ColumnInfo(BaseMessages.getString(PKG, "GisFileOutput.Params.Columns.PARAM_REQUIRED.Label"), ColumnInfo.COLUMN_TYPE_TEXT);
        requiredColumnInfo.setReadOnly(true);

        // Valeur de colonne
        ColumnInfo fieldNameValueColumnInfo = new ColumnInfo(BaseMessages.getString(PKG, "GisFileOutput.Params.Columns.PARAM_FIELD.Label"), ColumnInfo.COLUMN_TYPE_TEXT);
        fieldNameValueColumnInfo.setReadOnly(false);
        fieldNameValueColumnInfo.setSelectionAdapter(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {

                Integer tableIndex = wFieldParams.getSelectionIndex();
                TableItem tableItem = wFieldParams.table.getItem(tableIndex);

                // Récupération du type de donnée attendu pour le paramètre
                // sélectionné
                // et des valeurs prédéfinies
                String formatKey = getFormatKey(wOutputFormat.getText());
                String paramKey = getParamKey(formatKey, GisOutputFormatParameterDef.TYPE_FIELD, tableItem.getText(1));
                int paramValueMetaType = input.getParameterValueMetaType(formatKey, GisOutputFormatParameterDef.TYPE_FIELD, paramKey);
                List<String> values = input.getParameterPredefinedValues(formatKey, GisOutputFormatParameterDef.TYPE_FIELD, paramKey);

                // Si valeurs prédéfinies, ajout des paramètres de la
                // transformation
                if (values.size() > 0) {

                    for (String parameter : transMeta.listParameters()) {
                        values.add("${" + parameter + "}");
                    }

                }

                // Paramètrage de la boîte de dialogue
                String dialogTitle = BaseMessages.getString(PKG, "GisFileOutput.Params.Dialog.PARAM_FIELD.Title");
                String dialogParamComment = BaseMessages.getString(PKG, "GisFileOutput.Params." + paramKey + ".Description") + " (" + ValueMeta.getTypeDesc(paramValueMetaType)
                        + ")";
                if (!values.isEmpty()) {
                    dialogParamComment = dialogParamComment + "\n\n" + BaseMessages.getString(PKG, "GisFileOutput.Params.Dialog.PARAM_ALLOWED_VALUES.Description") + " :\n"
                            + values.toString();
                }

                // Valeur vide dans la liste si champ non obligatoire
                String[] fields = getFieldsFromType(ValueMeta.getTypeDesc(paramValueMetaType),
                        !input.isParameterValueRequired(formatKey, GisOutputFormatParameterDef.TYPE_FIELD, paramKey));

                // Choix dans une liste
                EnterSelectionDialog dialog = new EnterSelectionDialog(shell, fields, dialogTitle, dialogParamComment);
                dialog.setMulti(false);
                dialog.setFixed(true);
                dialog.setViewOnly();
                dialog.setAvoidQuickSearch();

                // Ouverture de la boîte de dialogue
                int index = dialog.getSelectionNr(tableItem.getText(3));
                if (index >= 0) {
                    dialog.setSelectedNrs(new int[] { index });
                }

                String selectedValue = dialog.open();
                if (selectedValue != null) {
                    tableItem.setText(3, selectedValue);
                }

            }

        });

        return new ColumnInfo[] { nameColumnInfo, requiredColumnInfo, fieldNameValueColumnInfo };

    }

}
