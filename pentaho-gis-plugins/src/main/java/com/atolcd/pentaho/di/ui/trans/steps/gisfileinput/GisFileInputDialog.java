package com.atolcd.pentaho.di.ui.trans.steps.gisfileinput;

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
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.dialog.EnterStringDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import com.atolcd.pentaho.di.trans.steps.gisfileinput.GisFileInputMeta;
import com.atolcd.pentaho.di.trans.steps.gisfileinput.GisInputFormatDef;
import com.atolcd.pentaho.di.trans.steps.gisfileinput.GisInputFormatParameter;
import com.atolcd.pentaho.di.trans.steps.gisfileinput.GisInputFormatParameterDef;

public class GisFileInputDialog extends BaseStepDialog implements StepDialogInterface {

    private static Class<?> PKG = GisFileInputMeta.class;

    // Groupes de contrôles
    private Group wOptionnalGroup;
    private FormData fdOptionnalGroup;

    // Type de fichier
    private Label wlInputFormat;
    private CCombo wInputFormat;
    private FormData fdlInputFormat, fdInputFormat;

    // Paramètres
    private Label wlParams;
    private TableView wParams;
    private FormData fdlParams, fdParams;

    // Nom du fichier à lire
    private Label wlFileName;
    private Button wbFileName;
    private TextVar wFileName;
    private FormData fdlFileName, fdbFileName, fdFileName;

    // Colonne contenant la géométrie
    private Label wlGeometryField;
    private TextVar wGeometryField;
    private FormData fdlGeometryField, fdGeometryField;

    // Encodage
    private Label wlEncoding;
    private CCombo wEncoding;
    private FormData fdlEncoding, fdEncoding;

    // Limite d'export
    private Label wlRowLimit;
    private Text wRowLimit;
    private FormData fdlRowLimit, fdRowLimit;

    private ColumnInfo[] paramsColumnInfo;

    private GisFileInputMeta input;

    public GisFileInputDialog(Shell parent, Object in, TransMeta tr, String sname) {

        super(parent, (BaseStepMeta) in, tr, sname);
        input = (GisFileInputMeta) in;
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
        shell.setText(BaseMessages.getString(PKG, "GisFileInput.Shell.Title"));

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
        fdStepname.right = new FormAttachment(100, 0);
        fdStepname.top = new FormAttachment(0, margin);
        wStepname.setLayoutData(fdStepname);

        // Type de fichier
        wlInputFormat = new Label(shell, SWT.RIGHT);
        wlInputFormat.setText(BaseMessages.getString(PKG, "GisFileInput.FileFormat.Label"));
        props.setLook(wlInputFormat);
        fdlInputFormat = new FormData();
        fdlInputFormat.left = new FormAttachment(0, 0);
        fdlInputFormat.right = new FormAttachment(middle, -margin);
        fdlInputFormat.top = new FormAttachment(wStepname, margin);
        wlInputFormat.setLayoutData(fdlInputFormat);

        wInputFormat = new CCombo(shell, SWT.BORDER | SWT.READ_ONLY);
        wInputFormat.setToolTipText(BaseMessages.getString(PKG, "GisFileInput.FileFormat.ToolTip"));
        wInputFormat.setEditable(false);
        props.setLook(wInputFormat);
        wInputFormat.addModifyListener(lsMod);
        fdInputFormat = new FormData();
        fdInputFormat.left = new FormAttachment(middle, 0);
        fdInputFormat.right = new FormAttachment(100, 0);
        fdInputFormat.top = new FormAttachment(wStepname, margin);
        wInputFormat.setLayoutData(fdInputFormat);
        wInputFormat.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent arg0) {
                wFileName.setText("");
                clearTables();

                // Pas de nom de colonne géométrie pour spatialite : plusieurs
                // géométries autorisées
                if (wInputFormat.getText() != null && !wInputFormat.getText().isEmpty() && getFormatKey(wInputFormat.getText()).equalsIgnoreCase("SPATIALITE")) {
                    wGeometryField.setEnabled(false);
                    wGeometryField.setText("");
                    wlGeometryField.setEnabled(false);

                    wEncoding.setEnabled(false);
                    wlEncoding.setEnabled(false);
                    wEncoding.setText("UTF-8");

                } else {
                    wGeometryField.setEnabled(true);
                    wGeometryField.setText(BaseMessages.getString(PKG, "GisFileInput.GeometryFieldName.Default"));
                    wlGeometryField.setEnabled(true);

                    wEncoding.setEnabled(true);
                    wlEncoding.setEnabled(true);

                }
            }
        });

        // Fichier à lire
        wlFileName = new Label(shell, SWT.RIGHT);
        wlFileName.setText(BaseMessages.getString(PKG, "GisFileInput.FileName.Label"));
        props.setLook(wlFileName);
        fdlFileName = new FormData();
        fdlFileName.left = new FormAttachment(0, 0);
        fdlFileName.right = new FormAttachment(middle, -margin);
        fdlFileName.top = new FormAttachment(wInputFormat, margin);
        wlFileName.setLayoutData(fdlFileName);

        wbFileName = new Button(shell, SWT.PUSH | SWT.CENTER);
        props.setLook(wbFileName);
        wbFileName.setText(BaseMessages.getString(PKG, "System.Button.Browse"));
        fdbFileName = new FormData();
        fdbFileName.right = new FormAttachment(100, 0);
        fdbFileName.top = new FormAttachment(wInputFormat, margin);
        wbFileName.setLayoutData(fdbFileName);
        wbFileName.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {

                if (wInputFormat.getText() != null && !wInputFormat.getText().isEmpty()) {

                    String[] extensions = input.getInputFormatDefs().get(getFormatKey(wInputFormat.getText())).getExtensions();
                    String[] extensionsNames = input.getInputFormatDefs().get(getFormatKey(wInputFormat.getText())).getExtensionsNames();

                    FileDialog dialog = new FileDialog(shell, SWT.OPEN);
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
        wFileName.setToolTipText(BaseMessages.getString(PKG, "GisFileInput.FileName.ToolTip"));
        props.setLook(wFileName);
        wFileName.addModifyListener(lsMod);
        fdFileName = new FormData();
        fdFileName.left = new FormAttachment(middle, 0);
        fdFileName.right = new FormAttachment(wbFileName, -margin);
        fdFileName.top = new FormAttachment(wInputFormat, margin);
        wFileName.setLayoutData(fdFileName);

        // Colonne géométrie
        wlGeometryField = new Label(shell, SWT.RIGHT);
        wlGeometryField.setText(BaseMessages.getString(PKG, "GisFileInput.GeometryFieldName.Label"));
        props.setLook(wlGeometryField);
        fdlGeometryField = new FormData();
        fdlGeometryField.left = new FormAttachment(0, 0);
        fdlGeometryField.top = new FormAttachment(wbFileName, margin);
        fdlGeometryField.right = new FormAttachment(middle, -margin);
        wlGeometryField.setLayoutData(fdlGeometryField);

        wGeometryField = new TextVar(transMeta, shell, SWT.BORDER | SWT.READ_ONLY);
        wGeometryField.setToolTipText(BaseMessages.getString(PKG, "GisFileInput.GeometryFieldName.ToolTip"));
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
        wOptionnalGroup.setText(BaseMessages.getString(PKG, "GisFileInput.Optionnal.Label"));

        FormLayout optionnalGroupLayout = new FormLayout();
        optionnalGroupLayout.marginWidth = 5;
        optionnalGroupLayout.marginHeight = 5;
        wOptionnalGroup.setLayout(optionnalGroupLayout);

        // Encodage
        wlEncoding = new Label(wOptionnalGroup, SWT.RIGHT);
        wlEncoding.setText(BaseMessages.getString(PKG, "GisFileInput.Encoding.Label"));
        props.setLook(wlEncoding);
        fdlEncoding = new FormData();
        fdlEncoding.left = new FormAttachment(0, 0);
        fdlEncoding.top = new FormAttachment(0, margin);
        fdlEncoding.right = new FormAttachment(middle, -margin);
        wlEncoding.setLayoutData(fdlEncoding);

        wEncoding = new CCombo(wOptionnalGroup, SWT.BORDER | SWT.READ_ONLY);
        wEncoding.setToolTipText(BaseMessages.getString(PKG, "GisFileInput.Encoding.ToolTip"));
        wEncoding.setEditable(true);
        props.setLook(wEncoding);
        wEncoding.addModifyListener(lsMod);
        fdEncoding = new FormData();
        fdEncoding.left = new FormAttachment(middle, 0);
        fdEncoding.right = new FormAttachment(100, 0);
        fdEncoding.top = new FormAttachment(0, margin);
        wEncoding.setLayoutData(fdEncoding);

        // Limite
        wlRowLimit = new Label(wOptionnalGroup, SWT.RIGHT);
        wlRowLimit.setText(BaseMessages.getString(PKG, "GisFileInput.RowLimit.Label"));
        props.setLook(wlRowLimit);
        fdlRowLimit = new FormData();
        fdlRowLimit.left = new FormAttachment(0, 0);
        fdlRowLimit.top = new FormAttachment(wEncoding, margin);
        fdlRowLimit.right = new FormAttachment(middle, -margin);
        wlRowLimit.setLayoutData(fdlRowLimit);

        wRowLimit = new Text(wOptionnalGroup, SWT.BORDER | SWT.READ_ONLY);
        wRowLimit.setToolTipText(BaseMessages.getString(PKG, "GisFileInput.RowLimit.ToolTip"));
        wRowLimit.setEditable(true);
        props.setLook(wRowLimit);
        wRowLimit.addModifyListener(lsMod);
        fdRowLimit = new FormData();
        fdRowLimit.left = new FormAttachment(middle, 0);
        fdRowLimit.right = new FormAttachment(100, 0);
        fdRowLimit.top = new FormAttachment(wEncoding, margin);
        wRowLimit.setLayoutData(fdRowLimit);

        fdOptionnalGroup = new FormData();
        fdOptionnalGroup.left = new FormAttachment(0, margin);
        fdOptionnalGroup.right = new FormAttachment(100, -margin);
        fdOptionnalGroup.top = new FormAttachment(wGeometryField, margin);
        wOptionnalGroup.setLayoutData(fdOptionnalGroup);

        // Fin du groupe : Options
        // ///////////////////////////////////////////////

        // Boutons Ok et Annuler
        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
        setButtonPositions(new Button[] { wOK, wCancel }, margin, null);

        // Paramètres
        wlParams = new Label(shell, SWT.NONE);
        wlParams.setText(BaseMessages.getString(PKG, "GisFileInput.Params.Label"));
        props.setLook(wlParams);
        fdlParams = new FormData();
        fdlParams.left = new FormAttachment(0, 0);
        fdlParams.top = new FormAttachment(wOptionnalGroup, margin);
        wlParams.setLayoutData(fdlParams);

        paramsColumnInfo = getParamsColumnInfo();
        wParams = new TableView(transMeta, shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, paramsColumnInfo, 0, lsMod, props);
        wParams.optWidth(true);
        wParams.setReadonly(true);
        fdParams = new FormData();
        fdParams.left = new FormAttachment(0, 0);
        fdParams.top = new FormAttachment(wlParams, margin);
        fdParams.right = new FormAttachment(100, 0);
        fdParams.bottom = new FormAttachment(wOK, -margin);
        wParams.setLayoutData(fdParams);

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
        loadData();
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
        for (Entry<String, GisInputFormatDef> formatsDefs : input.getInputFormatDefs().entrySet()) {
            formatsLabels.add(getFormatLabel(formatsDefs.getKey()));
        }
        Collections.sort(formatsLabels);
        wInputFormat.setItems(formatsLabels.toArray(new String[formatsLabels.size()]));

        if (input.getInputFormat() != null && !input.getInputFormat().isEmpty()) {

            String formatKey = input.getInputFormat();
            wInputFormat.setText(getFormatLabel(formatKey));

            // Tableau de valeur des paramètres
            Table paramsTable = wParams.table;
            paramsTable.removeAll();

            if (!input.getInputFormatParameters().isEmpty()) {

                int i = 0;
                for (GisInputFormatParameter parameter : input.getInputFormatParameters()) {

                    TableItem tableItem = new TableItem(paramsTable, SWT.NONE);
                    tableItem.setText(0, String.valueOf(i));
                    tableItem.setText(1, getParamLabel(parameter.getKey()));
                    GisInputFormatParameterDef parameterDef = input.getInputFormatDefs().get(formatKey).getParameterDef(parameter.getKey());
                    tableItem.setText(2, BaseMessages.getString(PKG, "GisFileInput.Params.Required." + String.valueOf(parameterDef.isRequired()).toUpperCase() + ".Label"));
                    if (parameter.getValue() != null) {
                        tableItem.setText(3, getParamValueLabel(parameter.getValue().toString()));
                    }
                    i++;

                }

                wParams.setRowNums();
                if (wParams.nrNonEmpty() > 0) {
                    wlParams.setEnabled(true);
                    wParams.setEnabled(true);
                } else {
                    wlParams.setEnabled(false);
                    wParams.setEnabled(false);
                }

            } else {

                clearTables();
            }

        }

        if (input.getInputFileName() != null) {
            wFileName.setText(input.getInputFileName());
        }

        if (input.getGeometryFieldName() != null) {
            wGeometryField.setText(input.getGeometryFieldName());
        } else {
            wGeometryField.setText(BaseMessages.getString(PKG, "GisFileInput.GeometryFieldName.Default"));
        }

        if (input.getEncoding() != null) {
            wEncoding.setText(input.getEncoding());
        }

        wRowLimit.setText(input.getRowLimit().toString());

        wStepname.selectAll();
    }

    private void cancel() {
        stepname = null;
        input.setChanged(backupChanged);
        dispose();
    }

    private void ok() {

        stepname = wStepname.getText();

        String formatKey = getFormatKey(wInputFormat.getText());
        input.setInputFormat(formatKey);

        List<GisInputFormatParameter> inputFormatParameters = new ArrayList<GisInputFormatParameter>();
        for (int i = 0; i < wParams.nrNonEmpty(); i++) {

            TableItem tableItem = wParams.getNonEmpty(i);

            String paramKey = getParamKey(formatKey, tableItem.getText(1));

            if (tableItem.getText(3) != null && !tableItem.getText(3).isEmpty()) {
                String value = getParamValueKey(formatKey, paramKey, tableItem.getText(3));
                inputFormatParameters.add(new GisInputFormatParameter(paramKey, value));
            } else {
                inputFormatParameters.add(new GisInputFormatParameter(paramKey, ""));
            }
        }

        input.setInputFormatParameters(inputFormatParameters);
        input.setInputFileName(wFileName.getText());
        input.setGeometryFieldName(wGeometryField.getText());
        input.setEncoding(wEncoding.getText());
        input.setRowLimit(Long.valueOf(wRowLimit.getText()));
        dispose();
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

    // Code du format depuis son libellé
    private String getFormatKey(String label) {

        for (Entry<String, GisInputFormatDef> formatDef : input.getInputFormatDefs().entrySet()) {

            if (label.equalsIgnoreCase(BaseMessages.getString(PKG, "GisFileInput.Format." + formatDef.getKey() + ".Label"))) {
                return formatDef.getKey();
            }
        }

        return null;

    }

    // Libellé du format depuis son code
    private String getFormatLabel(String key) {

        return BaseMessages.getString(PKG, "GisFileInput.Format." + key + ".Label");

    }

    // Code du paramètre depuis son libellé
    private String getParamKey(String formatKey, String label) {

        for (GisInputFormatParameterDef parameterDef : input.getInputFormatDefs().get(formatKey).getParameterDefs()) {

            if (label.equalsIgnoreCase(BaseMessages.getString(PKG, "GisFileInput.Params." + parameterDef.getKey() + ".Label"))) {
                return parameterDef.getKey();
            }
        }

        return null;

    }

    // Libellé du paramètre depuis son code
    private String getParamLabel(String key) {

        return BaseMessages.getString(PKG, "GisFileInput.Params." + key + ".Label");

    }

    // Code de la valeur prédéfinie son libellé
    private String getParamValueKey(String formatKey, String paramKey, String label) {

        for (String predefinedValueKey : input.getParameterPredefinedValues(formatKey, paramKey)) {

            if (label.equalsIgnoreCase(BaseMessages.getString(PKG, "GisFileInput.Params.Predefined." + predefinedValueKey + ".Label"))) {
                return predefinedValueKey;
            }
        }

        return label;

    }

    // Libellé de la valeur prédéfinie depuis son code
    private String getParamValueLabel(String key) {
        String label = BaseMessages.getString(PKG, "GisFileInput.Params.Predefined." + key + ".Label");
        if (label.startsWith("!")) {
            return key;
        } else {
            return BaseMessages.getString(PKG, "GisFileInput.Params.Predefined." + key + ".Label");
        }
    }

    // Réinitialise la table des paramètres
    private void clearTables() {

        Table paramsTable = wParams.table;
        paramsTable.removeAll();

        if (wInputFormat.getText() != null && !wInputFormat.getText().isEmpty()) {

            String inputFormatKey = getFormatKey(wInputFormat.getText());

            int i = 0;
            for (GisInputFormatParameterDef parameterDef : input.getInputFormatDefs().get(inputFormatKey).getParameterDefs()) {

                TableItem tableItem = new TableItem(paramsTable, SWT.NONE);
                tableItem.setText(0, String.valueOf(i));
                tableItem.setText(1, getParamLabel(parameterDef.getKey()));
                tableItem.setText(2, BaseMessages.getString(PKG, "GisFileInput.Params.Required." + String.valueOf(parameterDef.isRequired()).toUpperCase() + ".Label"));
                String defaultValue = parameterDef.getDefaultValue();
                if (defaultValue != null && !defaultValue.isEmpty()) {

                    String value = getParamValueLabel(parameterDef.getDefaultValue());
                    tableItem.setText(3, value);
                }
                i++;

            }

            wParams.setRowNums();
            if (wParams.nrNonEmpty() > 0) {
                wlParams.setEnabled(true);
                wParams.setEnabled(true);
            } else {
                wlParams.setEnabled(false);
                wParams.setEnabled(false);
            }

        }

        wStepname.selectAll();
    }

    private ColumnInfo[] getParamsColumnInfo() {

        // Nom
        ColumnInfo nameColumnInfo = new ColumnInfo(BaseMessages.getString(PKG, "GisFileInput.Params.Columns.PARAM_KEY.Label"), ColumnInfo.COLUMN_TYPE_TEXT);
        nameColumnInfo.setReadOnly(true);

        // Requis
        ColumnInfo requiredColumnInfo = new ColumnInfo(BaseMessages.getString(PKG, "GisFileInput.Params.Columns.PARAM_REQUIRED.Label"), ColumnInfo.COLUMN_TYPE_TEXT);
        requiredColumnInfo.setReadOnly(true);

        // Valeur fixe ou prédéfinie
        ColumnInfo fixedValueColumnInfo = new ColumnInfo(BaseMessages.getString(PKG, "GisFileInput.Params.Columns.PARAM_VALUE.Label"), ColumnInfo.COLUMN_TYPE_TEXT);
        fixedValueColumnInfo.setReadOnly(false);
        fixedValueColumnInfo.setSelectionAdapter(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {

                Integer tableIndex = wParams.getSelectionIndex();
                TableItem tableItem = wParams.table.getItem(tableIndex);

                // Récupération du type de donnée attendu pour le paramètre
                // sélectionné
                // et des valeurs prédéfinies
                String formatKey = getFormatKey(wInputFormat.getText());
                String paramKey = getParamKey(formatKey, tableItem.getText(1));
                int paramValueMetaType = input.getParameterValueMetaType(formatKey, paramKey);

                // List<String>values =
                // input.getParameterPredefinedValues(formatKey,paramKey);
                List<String> values = new ArrayList<String>();
                for (String value : input.getParameterPredefinedValues(formatKey, paramKey)) {
                    values.add(getParamValueLabel(value));
                }

                // Si valeurs prédéfinies, ajout des paramètres de la
                // transformation
                if (values.size() > 0) {
                    for (String parameter : transMeta.listParameters()) {
                        values.add("${" + parameter + "}");
                    }
                }

                // Paramétrage de la boîte de dialogue (commun)
                String dialogTitle = BaseMessages.getString(PKG, "GisFileInput.Params.Dialog.PARAM_VALUE.Title");
                String dialogParamComment = BaseMessages.getString(PKG, "GisFileInput.Params." + paramKey + ".Description") + " (" + ValueMeta.getTypeDesc(paramValueMetaType)
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

}
