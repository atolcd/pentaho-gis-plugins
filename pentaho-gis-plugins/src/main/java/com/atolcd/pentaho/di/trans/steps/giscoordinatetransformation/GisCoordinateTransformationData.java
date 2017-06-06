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

import org.cts.CRSFactory;
import org.cts.op.CoordinateOperation;
import org.cts.registry.RegistryManager;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.GeometryInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

public class GisCoordinateTransformationData extends BaseStepData implements StepDataInterface {

    public RowMetaInterface outputRowMeta;
    public GeometryInterface geomeryInterface;

    Integer geometryFieldIndex;
    Integer outputFieldIndex;
    String crsOperationType = null;

    CRSFactory cRSFactory;
    RegistryManager registryManager;

    CoordinateOperation transformation = null;

    public GisCoordinateTransformationData() {
        super();
    }
}
