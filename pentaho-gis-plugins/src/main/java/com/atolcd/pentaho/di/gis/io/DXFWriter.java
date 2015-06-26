package com.atolcd.pentaho.di.gis.io;

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


import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.pentaho.di.core.exception.KettleException;

import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Geometry;

import fr.michaelm.jump.drivers.dxf.DxfFile;

public class DXFWriter extends AbstractFileWriter {

    private DxfFile dxfFile;
    private String dxfFileName;
    private String layerName;
    private String layerNameFieldName;
    private boolean forceTo2DGeometry;
    private int precision;

    public DXFWriter(String fileName, String layerName, String geometryFieldName, String charsetName) throws KettleException {

        super(geometryFieldName, charsetName);
        this.dxfFileName = checkFilename(fileName).getFile();
        if (layerName == null || layerName.isEmpty()) {
            this.layerName = "0";
        } else {
            this.layerName = checkLayerName(layerName);
        }
        this.layerNameFieldName = null;
        this.dxfFile = new DxfFile();
        this.forceTo2DGeometry = false;
        this.precision = 0;

    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public boolean isForceTo2DGeometry() {
        return forceTo2DGeometry;
    }

    public void setForceTo2DGeometry(boolean forceTo2DGeometry) {
        this.forceTo2DGeometry = forceTo2DGeometry;
    }

    public void setLayerNameFieldName(String layerNameFieldName) {
        this.layerNameFieldName = layerNameFieldName;
    }

    public void writeFeatures(List<Feature> features) throws KettleException {

        try {

            Set<String> layerNames = new HashSet<String>();
            List<Geometry> geometries = new ArrayList<Geometry>();

            for (Feature feature : features) {

                Geometry geometry = (Geometry) feature.getValue(feature.getField(this.geometryFieldName));

                if (!GeometryUtils.isNullOrEmptyGeometry(geometry)) {

                    if (this.forceTo2DGeometry) {
                        geometry = GeometryUtils.get2DGeometry(geometry);
                    }

                    if (this.layerNameFieldName != null) {

                        String featureLayerName = (String) feature.getValue(feature.getField(this.layerNameFieldName));
                        if (featureLayerName != null && !featureLayerName.isEmpty()) {

                            featureLayerName = checkLayerName(featureLayerName);
                            layerNames.add(featureLayerName);
                            geometry.setUserData(featureLayerName);

                        } else {
                            geometry.setUserData(this.layerName);
                            layerNames.add(this.layerName);
                        }

                    } else {

                        geometry.setUserData(this.layerName);
                        layerNames.add(this.layerName);

                    }

                    geometries.add(geometry);

                }
            }

            this.dxfFile.write(geometries, layerNames.toArray(new String[layerNames.size()]), new FileWriter(this.dxfFileName), this.precision);

        } catch (IOException e) {
            throw new KettleException("Error writing features to " + this.dxfFileName, e);
        }
    }

    private String checkLayerName(String layerName) {

        String normalizedString = Normalizer.normalize(layerName, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalizedString).replaceAll("").replaceAll("[^a-zA-Z]+", "_").toUpperCase();

    }

}
