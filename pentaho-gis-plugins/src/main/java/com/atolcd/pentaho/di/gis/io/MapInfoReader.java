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


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.gdms.driver.mifmid.MifMidReader;
import org.pentaho.di.core.exception.KettleException;

import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Geometry;

public class MapInfoReader extends AbstractFileReader {

    private String mifFileName;
    private boolean mifFileExist;
    private boolean midFileExist;

    public MapInfoReader(String fileName, String geometryFieldName, String charsetName) throws KettleException {

        super(null, geometryFieldName, charsetName);

        try {

            this.mifFileExist = new File(checkFilename(fileName).getFile()).exists();
            this.midFileExist = new File(checkFilename(replaceFileExtension(fileName, ".mif", ".mid")).getFile()).exists();

            if (!this.mifFileExist) {
                throw new KettleException("Missing " + fileName + " file");
            } else {
                this.mifFileName = checkFilename(fileName).getFile();
            }

            if (!this.midFileExist) {
                throw new KettleException("Missing " + replaceFileExtension(fileName, ".mif", ".mid") + " file");
            }

            this.fields.add(new Field(geometryFieldName, FieldType.GEOMETRY, null, null));

            MifMidReader mifMidReader = new MifMidReader(new File(this.mifFileName), this.charset);
            mifMidReader.populateMMFileFeatureSchema();
            mifMidReader.readMMFileProperties();

            for (Entry<String, String> entry : mifMidReader.getColumns().entrySet()) {

                Field field = null;
                String fieldName = entry.getKey();
                String miType = entry.getValue();
                if (miType.equalsIgnoreCase("STRING")) {
                    field = new Field(fieldName, FieldType.STRING, null, null);
                } else if (miType.equalsIgnoreCase("INTEGER")) {
                    field = new Field(fieldName, FieldType.LONG, null, null);
                } else if (miType.equalsIgnoreCase("DATE")) {
                    field = new Field(fieldName, FieldType.DATE, null, null);
                } else if (miType.equalsIgnoreCase("DOUBLE")) {
                    field = new Field(fieldName, FieldType.DOUBLE, null, null);
                } else if (miType.equalsIgnoreCase("BOOLEAN")) {
                    field = new Field(fieldName, FieldType.BOOLEAN, null, null);
                } else {
                    field = new Field(fieldName, FieldType.STRING, null, null);
                }

                this.fields.add(field);

            }

        } catch (Exception e) {
            throw new KettleException("Error initialize reader", e);
        }
    }

    public List<Feature> getFeatures() throws KettleException {

        List<Feature> features = new ArrayList<Feature>();

        try {

            MifMidReader mifMidReader = new MifMidReader(new File(this.mifFileName), this.charset);

            mifMidReader.populateMMFileFeatureSchema();
            mifMidReader.readMMFileProperties();
            mifMidReader.createIndexes();

            if (this.limit == 0 || this.limit > mifMidReader.getFeatureNumber() || this.limit < 0) {
                this.limit = mifMidReader.getFeatureNumber();
            }

            for (int i = 0; i < this.limit; i++) {

                Feature feature = new Feature();
                Geometry geometry = mifMidReader.getGeometry(i);
                Object[] values = mifMidReader.getValues(i);

                if (this.forceTo2DGeometry) {
                    geometry = GeometryUtils.get2DGeometry(geometry);
                }

                if (this.forceToMultiGeometry) {
                    geometry = GeometryUtils.getMultiGeometry(geometry);
                }

                feature.addValue(this.fields.get(0), geometry);

                for (int j = 1; j < this.fields.size(); j++) {
                    feature.addValue(this.fields.get(j), values[j - 1]);
                }

                features.add(feature);

            }

            mifMidReader.close();

        } catch (IOException e) {
            throw new KettleException("Error reading features" + this.mifFileName, e);
        } catch (Exception e) {
            throw new KettleException("Error reading features" + this.mifFileName, e);
        }

        return features;

    }

}
