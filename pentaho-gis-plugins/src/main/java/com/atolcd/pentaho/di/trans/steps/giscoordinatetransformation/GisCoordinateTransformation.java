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

import org.cts.CRSFactory;
import org.cts.crs.CRSException;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.crs.GeodeticCRS;
import org.cts.op.CoordinateOperation;
import org.cts.op.CoordinateOperationFactory;
import org.cts.registry.EPSGRegistry;
import org.cts.registry.ESRIRegistry;
import org.cts.registry.IGNFRegistry;
import org.cts.registry.RegistryManager;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import com.atolcd.pentaho.di.core.row.value.ValueMetaGeometry;
import com.atolcd.pentaho.di.gis.utils.CoordinateTransformer;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Geometry;

public class GisCoordinateTransformation extends BaseStep implements StepInterface {

    private GisCoordinateTransformationData data;
    private GisCoordinateTransformationMeta meta;

    Integer geometryFieldIndex;
    Integer outputFieldIndex;
    String crsOperationType = null;
    protected ValueMetaInterface geometryValueMeta;

    CRSFactory cRSFactory;
    RegistryManager registryManager;

    CoordinateOperation transformation = null;

    public GisCoordinateTransformation(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
        super(s, stepDataInterface, c, t, dis);

        cRSFactory = new CRSFactory();
        registryManager = cRSFactory.getRegistryManager();
        registryManager.addRegistry(new IGNFRegistry());
        registryManager.addRegistry(new EPSGRegistry());
        registryManager.addRegistry(new ESRIRegistry());
    }

    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        meta = (GisCoordinateTransformationMeta) smi;
        data = (GisCoordinateTransformationData) sdi;

        Object[] r = getRow();

        if (r == null) {

            setOutputDone();
            return false;

        }

        if (first) {

            first = false;
            data.outputRowMeta = (RowMetaInterface) getInputRowMeta().clone();
            meta.getFields(data.outputRowMeta, getStepname(), null, null, this);

            // Récupération de l'index de la colonne contenant la geométrie
            RowMetaInterface inputRowMeta = getInputRowMeta();
            geometryFieldIndex = getInputRowMeta().indexOfValue(meta.getGeometryFieldName()); // Récupération
                                                                                              // de
                                                                                              // l'index
                                                                                              // de
                                                                                              // la
                                                                                              // colonne
                                                                                              // contenant
                                                                                              // la
                                                                                              // geométrie
            geometryValueMeta = inputRowMeta.getValueMeta(geometryFieldIndex);

            // Récupération de l'index de la colonne contenant le résultat
            outputFieldIndex = data.outputRowMeta.indexOfValue(meta.getOutputGeometryFieldName());

            crsOperationType = meta.getCrsOperation();

            if (crsOperationType.equalsIgnoreCase("REPROJECT")) {

                if (!meta.isCrsFromGeometry()) {
                    transformation = getTransformation(meta.getInputCRSAuthority() + ":" + environmentSubstitute(meta.getInputCRSCode()), meta.getOutputCRSAuthority() + ":"
                            + environmentSubstitute(meta.getOutputCRSCode()));
                }

            }

            logBasic("Initialized successfully");

        }

        Object[] outputRow = RowDataUtil.resizeArray(r, r.length + 1);
        Geometry inGeometry = ((ValueMetaGeometry) geometryValueMeta).getGeometry(r[geometryFieldIndex]);

        if (crsOperationType.equalsIgnoreCase("ASSIGN")) {

            Geometry outGeometry = (Geometry) inGeometry.clone();

            if (!GeometryUtils.isNullOrEmptyGeometry(inGeometry)) {
                outGeometry.setSRID(Integer.valueOf(environmentSubstitute(meta.getInputCRSCode())));
            }

            outputRow[outputFieldIndex] = outGeometry;

        } else {

            if (meta.isCrsFromGeometry()) {

                if (!GeometryUtils.isNullOrEmptyGeometry(inGeometry)) {

                    if (inGeometry.getSRID() > 0) {

                        transformation = getTransformation("EPSG:" + inGeometry.getSRID(), meta.getOutputCRSAuthority() + ":" + environmentSubstitute(meta.getOutputCRSCode()));

                    } else {
                        throw new KettleException("Transformation error : Unknown SRID for geometry " + inGeometry.toString());
                    }

                }

            }

            Geometry outGeometry = null;
            if (transformation != null) {
                outGeometry = CoordinateTransformer.transform(inGeometry, transformation);
            }

            // Assignation SRID si EPSG
            if (meta.getOutputCRSAuthority().equalsIgnoreCase("EPSG") && !GeometryUtils.isNullOrEmptyGeometry(outGeometry)) {
                outGeometry.setSRID(Integer.valueOf(environmentSubstitute(meta.getOutputCRSCode())));
            }

            outputRow[outputFieldIndex] = outGeometry;

        }

        putRow(data.outputRowMeta, outputRow);

        if (checkFeedback(getLinesRead())) {
            logBasic("Linenr " + getLinesRead());
        }

        return true;
    }

    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        meta = (GisCoordinateTransformationMeta) smi;
        data = (GisCoordinateTransformationData) sdi;
        return super.init(smi, sdi);
    }

    public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
        meta = (GisCoordinateTransformationMeta) smi;
        data = (GisCoordinateTransformationData) sdi;

        super.dispose(smi, sdi);
    }

    private CoordinateOperation getTransformation(String inputCRSCode, String outputCRSCode) {

        CoordinateOperation transformation = null;

        // Création de la transformation à partir des CRS entrées et sorties
        try {

            CoordinateReferenceSystem inputCRS = cRSFactory.getCRS(inputCRSCode);
            CoordinateReferenceSystem outputCRS = cRSFactory.getCRS(outputCRSCode);
            List<CoordinateOperation> transformations = CoordinateOperationFactory.createCoordinateOperations((GeodeticCRS) inputCRS, (GeodeticCRS) outputCRS);

            if (!transformations.isEmpty()) {
                transformation = transformations.get(0);
            } else {
                new KettleException("No transformation available");
            }

        } catch (CRSException e) {

            new KettleException(e);
        }
        return transformation;

    }

    public void run() {
        logBasic("Starting to run...");
        try {
            while (processRow(meta, data) && !isStopped())
                ;
        } catch (Exception e) {
            logError("Unexpected error : " + e.toString());
            logError(Const.getStackTracker(e));
            setErrors(1);
            stopAll();
        } finally {
            dispose(meta, data);
            logBasic("Finished, processing " + getLinesRead() + " rows");
            markStop();
        }
    }

}
