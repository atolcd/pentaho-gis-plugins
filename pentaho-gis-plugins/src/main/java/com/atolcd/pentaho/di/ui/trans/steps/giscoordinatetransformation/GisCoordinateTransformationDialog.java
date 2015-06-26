package com.atolcd.pentaho.di.ui.trans.steps.giscoordinatetransformation;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.cts.CRSFactory;
import org.cts.crs.CRSException;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.crs.GeodeticCRS;
import org.cts.op.CoordinateOperation;
import org.cts.op.CoordinateOperationFactory;
import org.cts.registry.EPSGRegistry;
import org.cts.registry.ESRIRegistry;
import org.cts.registry.IGNFRegistry;
import org.cts.registry.Registry;
import org.cts.registry.RegistryException;
import org.cts.registry.RegistryManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusListener;
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
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import com.atolcd.pentaho.di.trans.steps.giscoordinatetransformation.GisCoordinateTransformationMeta;

public class GisCoordinateTransformationDialog extends BaseStepDialog implements StepDialogInterface {

    private static Class<?> PKG = GisCoordinateTransformationMeta.class;

    // Groupes de contrôles
    private Group wInputCRSGroup;
    private FormData fdInputCRSGroup;

    private Group wOutputCRSGroup;
    private FormData fdOutputCRSGroup;

    // Type d'opération à réaliser
    private Label wlCrsOperation;
    private CCombo wCrsOperation;
    private FormData fdlCrsOperation, fdCrsOperation;

    // Colonne contenant la géométrie
    private Label wlGeometryField;
    private CCombo wGeometryField;
    private FormData fdlGeometryField, fdGeometryField;

    // Colonne contenant la géométrie de sortie
    private Label wlOutputGeometryField;
    private Text wOutputGeometryField;
    private FormData fdlOutputGeometryField, fdOutputGeometryField;

    // Dectection automatique
    private Label wlCrsFromGeometry;
    private Button wCrsFromGeometry;
    private FormData fdlCrsFromGeometry, fdCrsFromGeometry;

    // CRS initial (autorité)
    private Label wlInputCRSAuthority;
    private CCombo wInputCRSAuthority;
    private FormData fdlInputCRSAuthority, fdInputCRSAuthority;

    // CRS initial (code)
    private Label wlInputCRSCode;
    private Button wbInputCRSCode;
    private TextVar wInputCRSCode;
    private FormData fdlInputCRSCode, fdbInputCRSCode, fdInputCRSCode;

    // CRS initial (autorité)
    private Label wlOutputCRSAuthority;
    private CCombo wOutputCRSAuthority;
    private FormData fdlOutputCRSAuthority, fdOutputCRSAuthority;

    // CRS sortie (code)
    private Label wlOutputCRSCode;
    private Button wbOutputCRSCode;
    private TextVar wOutputCRSCode;
    private FormData fdlOutputCRSCode, fdbOutputCRSCode, fdOutputCRSCode;

    private Button wVerify;

    private boolean gotPreviousFields = false;

    private GisCoordinateTransformationMeta input;
    private CRSFactory cRSFactory;
    private RegistryManager registryManager;
    private HashMap<String, String> crsOperationList = new HashMap<String, String>();

    public GisCoordinateTransformationDialog(Shell parent, Object in, TransMeta tr, String sname) {

        super(parent, (BaseStepMeta) in, tr, sname);
        input = (GisCoordinateTransformationMeta) in;

        this.cRSFactory = new CRSFactory();
        this.registryManager = this.cRSFactory.getRegistryManager();
        this.registryManager.addRegistry(new IGNFRegistry());
        this.registryManager.addRegistry(new EPSGRegistry());
        this.registryManager.addRegistry(new ESRIRegistry());
        // this.registryManager.addRegistry(new Nad27Registry());
        // this.registryManager.addRegistry(new Nad83Registry());

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
        shell.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.Shell.Title"));

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

        // Type d'opération
        wlCrsOperation = new Label(shell, SWT.RIGHT);
        wlCrsOperation.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.CrsOperation.Label"));
        props.setLook(wlCrsOperation);
        fdlCrsOperation = new FormData();
        fdlCrsOperation.left = new FormAttachment(0, 0);
        fdlCrsOperation.top = new FormAttachment(wStepname, margin);
        fdlCrsOperation.right = new FormAttachment(middle, -margin);
        wlCrsOperation.setLayoutData(fdlCrsOperation);

        wCrsOperation = new CCombo(shell, SWT.BORDER | SWT.READ_ONLY);
        wCrsOperation.setToolTipText(BaseMessages.getString(PKG, "GisCoordinateTransformation.CrsOperation.ToolTip"));
        wCrsOperation.setEditable(false);
        props.setLook(wCrsOperation);
        wCrsOperation.addModifyListener(lsMod);
        fdCrsOperation = new FormData();
        fdCrsOperation.left = new FormAttachment(middle, 0);
        fdCrsOperation.right = new FormAttachment(100, 0);
        fdCrsOperation.top = new FormAttachment(wStepname, margin);
        wCrsOperation.setLayoutData(fdCrsOperation);
        wCrsOperation.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                setCrsOperationFlags();
            }
        }

        );

        // Colonne géométrie
        wlGeometryField = new Label(shell, SWT.RIGHT);
        wlGeometryField.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.GeometryFieldName.Label"));
        props.setLook(wlGeometryField);
        fdlGeometryField = new FormData();
        fdlGeometryField.left = new FormAttachment(0, 0);
        fdlGeometryField.top = new FormAttachment(wCrsOperation, margin);
        fdlGeometryField.right = new FormAttachment(middle, -margin);
        wlGeometryField.setLayoutData(fdlGeometryField);

        wGeometryField = new CCombo(shell, SWT.BORDER | SWT.READ_ONLY);
        wGeometryField.setToolTipText(BaseMessages.getString(PKG, "GisCoordinateTransformation.GeometryFieldName.ToolTip"));
        wGeometryField.setEditable(true);
        props.setLook(wGeometryField);
        wGeometryField.addModifyListener(lsMod);
        fdGeometryField = new FormData();
        fdGeometryField.left = new FormAttachment(middle, 0);
        fdGeometryField.right = new FormAttachment(100, 0);
        fdGeometryField.top = new FormAttachment(wCrsOperation, margin);
        wGeometryField.setLayoutData(fdGeometryField);
        wGeometryField.addFocusListener(new FocusListener() {

            public void focusLost(org.eclipse.swt.events.FocusEvent e) {
            }

            public void focusGained(org.eclipse.swt.events.FocusEvent e) {
                loadFields();
            }
        });

        // Colonne géométrie en sortie
        wlOutputGeometryField = new Label(shell, SWT.RIGHT);
        wlOutputGeometryField.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.OutputGeometryFieldName.Label"));
        props.setLook(wlOutputGeometryField);
        fdlOutputGeometryField = new FormData();
        fdlOutputGeometryField.left = new FormAttachment(0, 0);
        fdlOutputGeometryField.top = new FormAttachment(wGeometryField, margin);
        fdlOutputGeometryField.right = new FormAttachment(middle, -margin);
        wlOutputGeometryField.setLayoutData(fdlOutputGeometryField);

        wOutputGeometryField = new Text(shell, SWT.BORDER | SWT.READ_ONLY);
        wOutputGeometryField.setToolTipText(BaseMessages.getString(PKG, "GisCoordinateTransformation.OutputGeometryFieldName.ToolTip"));
        wOutputGeometryField.setEditable(true);
        props.setLook(wOutputGeometryField);
        wOutputGeometryField.addModifyListener(lsMod);
        fdOutputGeometryField = new FormData();
        fdOutputGeometryField.left = new FormAttachment(middle, 0);
        fdOutputGeometryField.right = new FormAttachment(100, 0);
        fdOutputGeometryField.top = new FormAttachment(wGeometryField, margin);
        wOutputGeometryField.setLayoutData(fdOutputGeometryField);

        // ///////////////////////////////////////////////
        // Début du groupe : CRS entrée
        wInputCRSGroup = new Group(shell, SWT.SHADOW_NONE);
        props.setLook(wInputCRSGroup);
        wInputCRSGroup.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.InputCRSGroup.Label"));

        FormLayout inputCRSGroupLayout = new FormLayout();
        inputCRSGroupLayout.marginWidth = 5;
        inputCRSGroupLayout.marginHeight = 5;
        wInputCRSGroup.setLayout(inputCRSGroupLayout);

        // Détection du SRID de la géométrie
        wlCrsFromGeometry = new Label(wInputCRSGroup, SWT.RIGHT);
        wlCrsFromGeometry.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.CrsFromGeometry.Label"));
        props.setLook(wlCrsFromGeometry);
        fdlCrsFromGeometry = new FormData();
        fdlCrsFromGeometry.left = new FormAttachment(0, 0);
        fdlCrsFromGeometry.top = new FormAttachment(0, margin);
        fdlCrsFromGeometry.right = new FormAttachment(middle, -margin);
        wlCrsFromGeometry.setLayoutData(fdlCrsFromGeometry);

        wCrsFromGeometry = new Button(wInputCRSGroup, SWT.CHECK);
        wCrsFromGeometry.setToolTipText(BaseMessages.getString(PKG, "GisCoordinateTransformation.CrsFromGeometry.ToolTip"));
        props.setLook(wCrsFromGeometry);
        fdCrsFromGeometry = new FormData();
        fdCrsFromGeometry.left = new FormAttachment(middle, 0);
        fdCrsFromGeometry.top = new FormAttachment(0, margin);
        fdCrsFromGeometry.right = new FormAttachment(100, 0);
        wCrsFromGeometry.setLayoutData(fdCrsFromGeometry);
        wCrsFromGeometry.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent arg0) {
                setCrsReprojectionModeFlags();
            }
        });

        // CRS entrée (autorité)
        wlInputCRSAuthority = new Label(wInputCRSGroup, SWT.RIGHT);
        wlInputCRSAuthority.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.InputCRSAuthority.Label"));
        props.setLook(wlInputCRSAuthority);
        fdlInputCRSAuthority = new FormData();
        fdlInputCRSAuthority.left = new FormAttachment(0, 0);
        fdlInputCRSAuthority.top = new FormAttachment(wCrsFromGeometry, margin);
        fdlInputCRSAuthority.right = new FormAttachment(middle, -margin);
        wlInputCRSAuthority.setLayoutData(fdlInputCRSAuthority);

        wInputCRSAuthority = new CCombo(wInputCRSGroup, SWT.BORDER | SWT.READ_ONLY);
        wInputCRSAuthority.setToolTipText(BaseMessages.getString(PKG, "GisCoordinateTransformation.InputCRSAuthority.ToolTip"));
        wInputCRSAuthority.setEditable(false);
        props.setLook(wInputCRSAuthority);
        wInputCRSAuthority.addModifyListener(lsMod);
        fdInputCRSAuthority = new FormData();
        fdInputCRSAuthority.left = new FormAttachment(middle, 0);
        fdInputCRSAuthority.right = new FormAttachment(100, 0);
        fdInputCRSAuthority.top = new FormAttachment(wCrsFromGeometry, margin);
        wInputCRSAuthority.setLayoutData(fdInputCRSAuthority);
        wInputCRSAuthority.addFocusListener(new FocusListener() {

            public void focusLost(org.eclipse.swt.events.FocusEvent e) {

            }

            public void focusGained(org.eclipse.swt.events.FocusEvent e) {
                wInputCRSCode.setText("");
            }
        });

        // CRS entrée (code)
        wlInputCRSCode = new Label(wInputCRSGroup, SWT.RIGHT);
        wlInputCRSCode.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.InputCRSCode.Label"));
        props.setLook(wlInputCRSCode);
        fdlInputCRSCode = new FormData();
        fdlInputCRSCode.left = new FormAttachment(0, 0);
        fdlInputCRSCode.top = new FormAttachment(wInputCRSAuthority, margin);
        fdlInputCRSCode.right = new FormAttachment(middle, -margin);
        wlInputCRSCode.setLayoutData(fdlInputCRSCode);

        wbInputCRSCode = new Button(wInputCRSGroup, SWT.PUSH | SWT.CENTER);
        props.setLook(wbInputCRSCode);
        wbInputCRSCode.setText("...");
        wbInputCRSCode.setToolTipText(BaseMessages.getString(PKG, "GisCoordinateTransformation.InputCRSCode.ToolTip"));
        fdbInputCRSCode = new FormData();
        fdbInputCRSCode.right = new FormAttachment(100, 0);
        fdbInputCRSCode.top = new FormAttachment(wInputCRSAuthority, margin);
        wbInputCRSCode.setLayoutData(fdbInputCRSCode);

        wInputCRSCode = new TextVar(transMeta, wInputCRSGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wInputCRSCode.setToolTipText(BaseMessages.getString(PKG, "GisCoordinateTransformation.InputCRSCode.ToolTip"));
        props.setLook(wInputCRSCode);
        wInputCRSCode.addModifyListener(lsMod);
        fdInputCRSCode = new FormData();
        fdInputCRSCode.left = new FormAttachment(middle, 0);
        fdInputCRSCode.right = new FormAttachment(wbInputCRSCode, -margin);
        fdInputCRSCode.top = new FormAttachment(wInputCRSAuthority, margin);
        wInputCRSCode.setLayoutData(fdInputCRSCode);

        fdInputCRSGroup = new FormData();
        fdInputCRSGroup.left = new FormAttachment(0, margin);
        fdInputCRSGroup.right = new FormAttachment(100, -margin);
        fdInputCRSGroup.top = new FormAttachment(wOutputGeometryField, margin);
        wInputCRSGroup.setLayoutData(fdInputCRSGroup);
        // Fin du groupe : Options de création de table
        // ///////////////////////////////////////////////

        // ///////////////////////////////////////////////
        // Début du groupe : CRS sortie
        wOutputCRSGroup = new Group(shell, SWT.SHADOW_NONE);
        props.setLook(wOutputCRSGroup);
        wOutputCRSGroup.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.OutputCRSGroup.Label"));

        FormLayout outputCRSGroupLayout = new FormLayout();
        outputCRSGroupLayout.marginWidth = 5;
        outputCRSGroupLayout.marginHeight = 5;
        wOutputCRSGroup.setLayout(outputCRSGroupLayout);

        // CRS sortie (autorité)
        wlOutputCRSAuthority = new Label(wOutputCRSGroup, SWT.RIGHT);
        wlOutputCRSAuthority.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.OutputCRSAuthority.ToolTip"));
        props.setLook(wlOutputCRSAuthority);
        fdlOutputCRSAuthority = new FormData();
        fdlOutputCRSAuthority.left = new FormAttachment(0, 0);
        fdlOutputCRSAuthority.top = new FormAttachment(0, margin);
        fdlOutputCRSAuthority.right = new FormAttachment(middle, -margin);
        wlOutputCRSAuthority.setLayoutData(fdlOutputCRSAuthority);

        wOutputCRSAuthority = new CCombo(wOutputCRSGroup, SWT.BORDER | SWT.READ_ONLY);
        wOutputCRSAuthority.setToolTipText(BaseMessages.getString(PKG, "GisCoordinateTransformation.OutputCRSAuthority.ToolTip"));
        wOutputCRSAuthority.setEditable(false);
        props.setLook(wOutputCRSAuthority);
        wOutputCRSAuthority.addModifyListener(lsMod);
        fdOutputCRSAuthority = new FormData();
        fdOutputCRSAuthority.left = new FormAttachment(middle, 0);
        fdOutputCRSAuthority.right = new FormAttachment(100, 0);
        fdOutputCRSAuthority.top = new FormAttachment(0, margin);
        wOutputCRSAuthority.setLayoutData(fdOutputCRSAuthority);
        wOutputCRSAuthority.addFocusListener(new FocusListener() {

            public void focusLost(org.eclipse.swt.events.FocusEvent e) {

            }

            public void focusGained(org.eclipse.swt.events.FocusEvent e) {
                wOutputCRSCode.setText("");
            }
        });

        // CRS sortie (code)
        wlOutputCRSCode = new Label(wOutputCRSGroup, SWT.RIGHT);
        wlOutputCRSCode.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.OutputCRSCode.Label"));
        props.setLook(wlOutputCRSCode);
        fdlOutputCRSCode = new FormData();
        fdlOutputCRSCode.left = new FormAttachment(0, 0);
        fdlOutputCRSCode.top = new FormAttachment(wOutputCRSAuthority, margin);
        fdlOutputCRSCode.right = new FormAttachment(middle, -margin);
        wlOutputCRSCode.setLayoutData(fdlOutputCRSCode);

        wbOutputCRSCode = new Button(wOutputCRSGroup, SWT.PUSH | SWT.CENTER);
        props.setLook(wbOutputCRSCode);
        wbOutputCRSCode.setText("...");
        wbOutputCRSCode.setToolTipText(BaseMessages.getString(PKG, "GisCoordinateTransformation.OutputCRSCode.Label"));
        fdbOutputCRSCode = new FormData();
        fdbOutputCRSCode.right = new FormAttachment(100, 0);
        fdbOutputCRSCode.top = new FormAttachment(wOutputCRSAuthority, margin);
        wbOutputCRSCode.setLayoutData(fdbOutputCRSCode);

        wOutputCRSCode = new TextVar(transMeta, wOutputCRSGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wOutputCRSCode.setToolTipText(BaseMessages.getString(PKG, "GisCoordinateTransformation.OutputCRSCode.ToolTip"));
        props.setLook(wOutputCRSCode);
        wOutputCRSCode.addModifyListener(lsMod);
        fdOutputCRSCode = new FormData();
        fdOutputCRSCode.left = new FormAttachment(middle, 0);
        fdOutputCRSCode.right = new FormAttachment(wbOutputCRSCode, -margin);
        fdOutputCRSCode.top = new FormAttachment(wOutputCRSAuthority, margin);
        wOutputCRSCode.setLayoutData(fdOutputCRSCode);

        fdOutputCRSGroup = new FormData();
        fdOutputCRSGroup.left = new FormAttachment(0, margin);
        fdOutputCRSGroup.right = new FormAttachment(100, -margin);
        fdOutputCRSGroup.top = new FormAttachment(wInputCRSGroup, margin);
        wOutputCRSGroup.setLayoutData(fdOutputCRSGroup);
        // Fin du groupe : Options de création de table
        // ///////////////////////////////////////////////

        // Boutons Ok et Annuler
        wOK = new Button(shell, SWT.PUSH);
        wOK.setText(BaseMessages.getString(PKG, "System.Button.OK"));
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText(BaseMessages.getString(PKG, "System.Button.Cancel"));
        wVerify = new Button(shell, SWT.PUSH);
        wVerify.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.Button.Check.Label"));
        wVerify.setToolTipText("");

        setButtonPositions(new Button[] { wOK, wCancel, wVerify }, margin, wOutputCRSGroup);
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

        // Afficahe de la liste des codes disponibles pour le systeme en entrée
        wbInputCRSCode.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                if (wInputCRSAuthority.getText() != null) {

                    String descriptions[] = loadRegistryInfo(wInputCRSAuthority.getText());

                    EnterSelectionDialog inputCodeDialog = new EnterSelectionDialog(shell, descriptions, BaseMessages.getString(PKG, "GisCoordinateTransformation.InputCRSGroup."
                            + getCrsOperationKey(wCrsOperation.getText()) + ".Label"), BaseMessages.getString(PKG, "GisCoordinateTransformation.InputCRSGroup."
                            + getCrsOperationKey(wCrsOperation.getText()) + ".Label"));

                    inputCodeDialog.setMulti(false);
                    inputCodeDialog.setFixed(true);
                    inputCodeDialog.setViewOnly();

                    if (wInputCRSCode.getText() != null) {

                        Registry registry = registryManager.getRegistry(wInputCRSAuthority.getText());
                        try {
                            Map<String, String> map = registry.getParameters(wInputCRSCode.getText());

                            if (map != null) {

                                Integer index = Arrays.binarySearch(descriptions, wInputCRSCode.getText().toUpperCase() + " - " + map.get("title"));
                                if (index != null) {
                                    int[] selected = new int[1];
                                    selected[0] = index;
                                    inputCodeDialog.setSelectedNrs(selected);
                                }
                            }

                        } catch (RegistryException e1) {
                        }
                    }

                    String selection = inputCodeDialog.open();

                    if (selection != null) {
                        wInputCRSCode.setText(selection.split(" - ")[0].trim());
                    }
                }

            }
        });

        // Afficahe de la liste des codes disponibles pour le systeme en sortie
        wbOutputCRSCode.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (wOutputCRSAuthority.getText() != null) {

                    String descriptions[] = loadRegistryInfo(wOutputCRSAuthority.getText());
                    EnterSelectionDialog inputCodeDialog = new EnterSelectionDialog(shell, descriptions, BaseMessages.getString(PKG,
                            "GisCoordinateTransformation.OutputCRSGroup.Label"), BaseMessages.getString(PKG, "GisCoordinateTransformation.OutputCRSGroup.Label"));

                    /*
                     * inputCodeDialog.setMulti(false);
                     * inputCodeDialog.setFixed(true);
                     * inputCodeDialog.setViewOnly();
                     */

                    if (wOutputCRSCode.getText() != null) {

                        Registry registry = registryManager.getRegistry(wOutputCRSAuthority.getText());
                        try {
                            Map<String, String> map = registry.getParameters(wOutputCRSCode.getText());

                            if (map != null) {

                                Integer index = Arrays.binarySearch(descriptions, wOutputCRSCode.getText().toUpperCase() + " - " + map.get("title"));
                                if (index != null) {
                                    int[] selected = new int[1];
                                    selected[0] = index;
                                    inputCodeDialog.setSelectedNrs(selected);
                                }
                            }

                        } catch (RegistryException e1) {
                        }
                    }

                    String selection = inputCodeDialog.open();

                    if (selection != null) {
                        wOutputCRSCode.setText(selection.split(" - ")[0].trim());
                    }
                }

            }
        });

        // Vérification des codes entrée et sorties
        wVerify.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {

                String dialogTitle = wCrsOperation.getText();
                String dialogMessage = null;
                int dialogIcon = SWT.ICON_INFORMATION;

                CoordinateReferenceSystem inputCRS = null;
                String inputSystemName = (wInputCRSAuthority.getText() + ":" + wInputCRSCode.getText()).toUpperCase();

                // Controle Assignement de SRID ou Système en entrée
                try {

                    inputCRS = cRSFactory.getCRS(inputSystemName);
                    if (inputCRS == null) {

                        dialogIcon = SWT.ICON_ERROR;
                        dialogMessage = String.format(BaseMessages.getString(PKG, "GisCoordinateTransformation.MessageBox.CRSCode.ERROR"), inputSystemName);
                        showMessage(dialogIcon, dialogTitle, dialogMessage);

                    } else {

                        if (getCrsOperationKey(wCrsOperation.getText()).equalsIgnoreCase("ASSIGN")) {
                            dialogIcon = SWT.ICON_INFORMATION;
                            dialogMessage = String.format(BaseMessages.getString(PKG, "GisCoordinateTransformation.MessageBox.CRSCode.OK"), inputSystemName);
                            showMessage(dialogIcon, dialogTitle, dialogMessage);
                        }

                    }

                } catch (CRSException e1) {

                    dialogIcon = SWT.ICON_ERROR;
                    dialogMessage = String.format(BaseMessages.getString(PKG, "GisCoordinateTransformation.MessageBox.CRSCode.ERROR"), inputSystemName);
                    showMessage(dialogIcon, dialogTitle, dialogMessage);
                }

                // Si Reprojection
                if (getCrsOperationKey(wCrsOperation.getText()).equalsIgnoreCase("REPROJECT")) {

                    CoordinateReferenceSystem outputCRS = null;
                    String outputSystemName = (wOutputCRSAuthority.getText() + ":" + wOutputCRSCode.getText()).toUpperCase();

                    // Système de sortie
                    try {

                        outputCRS = cRSFactory.getCRS(outputSystemName);
                        if (outputCRS == null) {

                            dialogIcon = SWT.ICON_ERROR;
                            dialogMessage = String.format(BaseMessages.getString(PKG, "GisCoordinateTransformation.MessageBox.CRSCode.ERROR"), outputSystemName);
                            showMessage(dialogIcon, dialogTitle, dialogMessage);
                        }

                    } catch (CRSException e1) {

                        dialogIcon = SWT.ICON_ERROR;
                        dialogMessage = String.format(BaseMessages.getString(PKG, "GisCoordinateTransformation.MessageBox.CRSCode.ERROR"), outputSystemName);
                        showMessage(dialogIcon, dialogTitle, dialogMessage);
                    }

                    // Existence de la transformation
                    if (inputCRS != null && outputCRS != null) {

                        List<CoordinateOperation> transformations = CoordinateOperationFactory.createCoordinateOperations((GeodeticCRS) inputCRS, (GeodeticCRS) outputCRS);

                        if (!transformations.isEmpty() && transformations.get(0) != null) {

                            dialogIcon = SWT.ICON_INFORMATION;
                            dialogMessage = String.format(BaseMessages.getString(PKG, "GisCoordinateTransformation.MessageBox.CRSTransformation.OK"), inputSystemName,
                                    outputSystemName);

                        } else {

                            dialogIcon = SWT.ICON_INFORMATION;
                            dialogMessage = String.format(BaseMessages.getString(PKG, "GisCoordinateTransformation.MessageBox.CRSTransformation.ERROR"), inputSystemName,
                                    outputSystemName);

                        }

                        showMessage(dialogIcon, dialogTitle, dialogMessage);

                    }

                }

            }
        });

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

        loadAuthorities();
        loadCrsOperation();
        loadFields();
        loadData();
        setCrsOperationFlags();
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

        wCrsOperation.setText(getCrsOperationValue(input.getCrsOperation()));

        if (input.getGeometryFieldName() != null) {
            wGeometryField.setText(input.getGeometryFieldName());
        }

        if (input.getOutputGeometryFieldName() != null) {
            wOutputGeometryField.setText(input.getOutputGeometryFieldName());
        } else {
            wOutputGeometryField.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.OutputGeometryFieldName.Default"));
        }

        wCrsFromGeometry.setSelection(input.isCrsFromGeometry());

        if (input.getInputCRSAuthority() != null) {
            wInputCRSAuthority.setText(input.getInputCRSAuthority());
        }

        if (input.getInputCRSCode() != null) {
            wInputCRSCode.setText(input.getInputCRSCode());
        }

        if (input.getOutputCRSAuthority() != null) {
            wOutputCRSAuthority.setText(input.getOutputCRSAuthority());
        }

        if (input.getOutputCRSCode() != null) {
            wOutputCRSCode.setText(input.getOutputCRSCode());
        }

        wStepname.selectAll();
    }

    // Création de la liste des colonnes
    private void loadAuthorities() {

        wInputCRSAuthority.setItems(registryManager.getRegistryNames());
        wOutputCRSAuthority.setItems(wInputCRSAuthority.getItems());

    }

    // Initialise la liste des types d'operation
    private void loadCrsOperation() {

        crsOperationList.put("ASSIGN", BaseMessages.getString(PKG, "GisCoordinateTransformation.CrsOperation.ASSIGN"));
        crsOperationList.put("REPROJECT", BaseMessages.getString(PKG, "GisCoordinateTransformation.CrsOperation.REPROJECT"));
        wCrsOperation.add(getCrsOperationValue("ASSIGN"));
        wCrsOperation.add(getCrsOperationValue("REPROJECT"));

    }

    private String getCrsOperationValue(String key) {
        return crsOperationList.get(key);
    }

    private String getCrsOperationKey(String value) {

        for (Entry<String, String> entry : crsOperationList.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(value)) {
                return entry.getKey();
            }
        }
        return null;

    }

    public void setCrsOperationFlags() {

        if (getCrsOperationKey(wCrsOperation.getText()).equalsIgnoreCase("ASSIGN")) {

            wlCrsFromGeometry.setEnabled(false);
            wCrsFromGeometry.setEnabled(false);
            wCrsFromGeometry.setSelection(false);

            wlInputCRSAuthority.setEnabled(false);
            wInputCRSAuthority.setEnabled(false);
            wInputCRSAuthority.setText("epsg");

            wlInputCRSCode.setEnabled(true);
            wInputCRSCode.setEnabled(true);
            wbInputCRSCode.setEnabled(true);

            wOutputCRSGroup.setEnabled(false);

            wlOutputCRSAuthority.setEnabled(false);
            wOutputCRSAuthority.setEnabled(false);
            wOutputCRSAuthority.setText("");

            wlOutputCRSCode.setEnabled(false);
            wOutputCRSCode.setEnabled(false);
            wbOutputCRSCode.setEnabled(false);
            wOutputCRSCode.setText("");

        } else {

            wlCrsFromGeometry.setEnabled(true);
            wCrsFromGeometry.setEnabled(true);

            wlInputCRSAuthority.setEnabled(true);
            wInputCRSAuthority.setEnabled(true);

            wlInputCRSCode.setEnabled(true);
            wInputCRSCode.setEnabled(true);
            wbInputCRSCode.setEnabled(true);

            wOutputCRSGroup.setEnabled(true);

            wlOutputCRSAuthority.setEnabled(true);
            wOutputCRSAuthority.setEnabled(true);
            if (wOutputCRSAuthority.getText() == "") {
                wOutputCRSAuthority.setText("epsg");
            }

            wlOutputCRSCode.setEnabled(true);
            wOutputCRSCode.setEnabled(true);
            wbOutputCRSCode.setEnabled(true);

        }

        wInputCRSGroup.setText(BaseMessages.getString(PKG, "GisCoordinateTransformation.InputCRSGroup." + getCrsOperationKey(wCrsOperation.getText()) + ".Label"));
        wVerify.setToolTipText(BaseMessages.getString(PKG, "GisCoordinateTransformation.Button.Check." + getCrsOperationKey(wCrsOperation.getText()) + ".ToolTip"));
        setCrsReprojectionModeFlags();
    }

    public void setCrsReprojectionModeFlags() {

        if (getCrsOperationKey(wCrsOperation.getText()).equalsIgnoreCase("REPROJECT")) {

            if (wCrsFromGeometry.getSelection()) {

                wlInputCRSAuthority.setEnabled(false);
                wInputCRSAuthority.setEnabled(false);
                wInputCRSAuthority.setText("");

                wlInputCRSCode.setEnabled(false);
                wInputCRSCode.setEnabled(false);
                wbInputCRSCode.setEnabled(false);
                wInputCRSCode.setText("");

                wVerify.setEnabled(false);

            } else {

                wlInputCRSAuthority.setEnabled(true);
                wInputCRSAuthority.setEnabled(true);

                wlInputCRSCode.setEnabled(true);
                wInputCRSCode.setEnabled(true);
                wbInputCRSCode.setEnabled(true);

                wVerify.setEnabled(true);
            }

        } else {
            wVerify.setEnabled(true);
        }

    }

    // Création de la liste des colonnes
    private void loadFields() {

        if (!gotPreviousFields) {

            try {

                String geometryField = wGeometryField.getText();
                wGeometryField.removeAll();

                // Récupération des colonnes de l'étape précédente
                // et alimentation des combos
                RowMetaInterface r = transMeta.getPrevStepFields(stepname);
                if (r != null) {

                    // Filtrage par type de colonne texte
                    TreeSet<String> textFieldsTree = new TreeSet<String>();
                    String[] fieldNames = r.getFieldNames();
                    String[] fieldNamesAndTypes = r.getFieldNamesAndTypes(0);

                    for (int i = 0; i < fieldNames.length; i++) {
                        if (fieldNamesAndTypes[i].toLowerCase().contains("geometry")) {
                            if (fieldNames[i] != null && !fieldNames[i].isEmpty()) {
                                textFieldsTree.add(fieldNames[i]);
                            }
                        }
                    }

                    String textFields[] = textFieldsTree.toArray(new String[] {});

                    wGeometryField.setItems(textFields);

                }

                if (geometryField != null) {
                    wGeometryField.setText(geometryField);
                }

            } catch (KettleException ke) {
                new ErrorDialog(
                        shell,
                        BaseMessages.getString(PKG, "ChangeFileEncodingDialog.FailedToGetFields.DialogTitle"), BaseMessages.getString(PKG, "ChangeFileEncodingDialog.FailedToGetFields.DialogMessage"), ke); //$NON-NLS-1$ //$NON-NLS-2$
            }

            gotPreviousFields = true;
        }

    }

    private void cancel() {
        stepname = null;
        input.setChanged(backupChanged);
        dispose();
    }

    private void ok() {

        stepname = wStepname.getText();
        input.setCrsOperation(getCrsOperationKey(wCrsOperation.getText()));
        input.setGeometryFieldName(wGeometryField.getText());
        input.setOutputGeometryFieldName(wOutputGeometryField.getText());
        input.setCrsFromGeometry(wCrsFromGeometry.getSelection());
        input.setInputCRSAuthority(wInputCRSAuthority.getText());
        input.setInputCRSCode(wInputCRSCode.getText());
        input.setOutputCRSAuthority(wOutputCRSAuthority.getText());
        input.setOutputCRSCode(wOutputCRSCode.getText());

        dispose();
    }

    private void showMessage(int icon, String title, String message) {

        MessageBox messageBox = new MessageBox(shell, icon | SWT.CLOSE);
        messageBox.setText(title);
        messageBox.setMessage(message);
        messageBox.open();

    }

    private String[] loadRegistryInfo(String registryName) {

        String registryFile = "/" + input.getClass().getPackage().getName().replace(".", "/") + "/registry/" + registryName;
        InputStream inputstream = GisCoordinateTransformationDialog.class.getResourceAsStream(registryFile);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(inputstream, writer);
            String descriptions[] = writer.toString().split("[|]");
            Arrays.sort(descriptions);
            return descriptions;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String[0];

    }

}
