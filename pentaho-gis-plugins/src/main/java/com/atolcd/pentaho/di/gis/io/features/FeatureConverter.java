package com.atolcd.pentaho.di.gis.io.features;

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
import java.util.List;

import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;

import com.atolcd.pentaho.di.core.row.value.ValueMetaGeometry;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;

public final class FeatureConverter {

    public static List<Field> getFields(RowMetaInterface rowMeta) {

        List<Field> fields = new ArrayList<Field>();

        for (String fieldName : rowMeta.getFieldNames()) {

            int fieldIndex = rowMeta.indexOfValue(fieldName);
            ValueMetaInterface valueMetaInterface = rowMeta.getValueMeta(fieldIndex);
            Integer length = null;
            Integer precision = null;
            Field field = null;

            if (valueMetaInterface.getLength() != -1) {
                length = valueMetaInterface.getLength();
            }

            if (valueMetaInterface.getPrecision() != -1) {
                precision = valueMetaInterface.getPrecision();
            }

            switch (valueMetaInterface.getType()) {

            case ValueMeta.TYPE_BOOLEAN:
                field = new Field(fieldName, FieldType.BOOLEAN, length, precision);
                break;

            case ValueMeta.TYPE_INTEGER:
                field = new Field(fieldName, FieldType.LONG, length, precision);
                break;

            case ValueMeta.TYPE_NUMBER:
                field = new Field(fieldName, FieldType.DOUBLE, length, precision);
                break;

            case ValueMeta.TYPE_BIGNUMBER:
                field = new Field(fieldName, FieldType.DOUBLE, length, precision);
                break;

            case ValueMeta.TYPE_STRING:
                field = new Field(fieldName, FieldType.STRING, length, precision);
                break;

            case ValueMeta.TYPE_DATE:
                field = new Field(fieldName, FieldType.DATE, length, precision);
                break;

            case ValueMetaGeometry.TYPE_GEOMETRY:
                field = new Field(fieldName, FieldType.GEOMETRY, length, precision);
                break;

            default:
                field = new Field(fieldName, FieldType.STRING, length, precision);
                break;
            }

            fields.add(field);

        }

        return fields;

    }

    public static Feature getFeature(RowMetaInterface rowMeta, Object[] r) throws KettleValueException {

        Feature feature = new Feature();

        for (Field field : getFields(rowMeta)) {

            int fieldIndex = rowMeta.indexOfValue(field.getName());

            if (rowMeta.isNull(r, fieldIndex)) {
                feature.addValue(field, null);
            } else {

                if (rowMeta.getValueMeta(fieldIndex).isBoolean()) {
                    feature.addValue(field, rowMeta.getBoolean(r, fieldIndex));
                } else if (rowMeta.getValueMeta(fieldIndex).isInteger()) {
                    feature.addValue(field, rowMeta.getInteger(r, fieldIndex));
                } else if (rowMeta.getValueMeta(fieldIndex).isNumber()) {
                    feature.addValue(field, rowMeta.getNumber(r, fieldIndex));
                } else if (rowMeta.getValueMeta(fieldIndex).isBigNumber()) {
                    feature.addValue(field, rowMeta.getNumber(r, fieldIndex).doubleValue());
                } else if (rowMeta.getValueMeta(fieldIndex).isString()) {
                    feature.addValue(field, rowMeta.getString(r, fieldIndex));
                } else if (rowMeta.getValueMeta(fieldIndex).isDate()) {
                    feature.addValue(field, (rowMeta.getDate(r, fieldIndex)));
                } else if (rowMeta.getValueMeta(fieldIndex).getType() == ValueMetaGeometry.TYPE_GEOMETRY) {
                    feature.addValue(field, ((ValueMetaGeometry) rowMeta.getValueMeta(fieldIndex)).getGeometry(r[fieldIndex]));
                } else {
                    feature.addValue(field, rowMeta.getString(r, fieldIndex));
                }
            }

        }

        return feature;

    }

    public static RowMetaInterface getRowMeta(List<Field> fields, String origin) {

        RowMeta rowMeta = new RowMeta();

        for (Field field : fields) {

            ValueMetaInterface valueMeta = null;

            if (field.getType().equals(FieldType.GEOMETRY)) {

                valueMeta = new ValueMetaGeometry(field.getName());

            } else if (field.getType().equals(FieldType.BOOLEAN)) {

                valueMeta = new ValueMeta(field.getName(), ValueMetaInterface.TYPE_BOOLEAN);

            } else if (field.getType().equals(FieldType.DATE)) {

                valueMeta = new ValueMeta(field.getName(), ValueMetaInterface.TYPE_DATE);

            } else if (field.getType().equals(FieldType.DOUBLE)) {

                valueMeta = new ValueMeta(field.getName(), ValueMetaInterface.TYPE_NUMBER);

            } else if (field.getType().equals(FieldType.LONG)) {

                valueMeta = new ValueMeta(field.getName(), ValueMetaInterface.TYPE_INTEGER);

            } else {

                valueMeta = new ValueMeta(field.getName(), ValueMetaInterface.TYPE_STRING);

            }

            if (field.getLength() != null) {
                valueMeta.setLength(field.getLength());
            }
            if (field.getDecimalCount() != null) {
                valueMeta.setPrecision(field.getDecimalCount());
            }

            valueMeta.setOrigin(origin);
            rowMeta.addValueMeta(valueMeta);

        }

        return rowMeta;

    }

    public static Object[] getRow(RowMetaInterface rowMeta, Feature feature) throws KettleValueException {

        Object[] row = new Object[rowMeta.size()];
        for (Field field : getFields(rowMeta)) {

            int fieldIndex = rowMeta.indexOfValue(field.getName());
            ValueMetaInterface valueMeta = rowMeta.getValueMeta(fieldIndex);
            Object value = null;

            Object featureValue = feature.getValue(field);
            if (featureValue != null) {

                if (field.getType().equals(FieldType.GEOMETRY)) {

                    value = featureValue;

                } else if (field.getType().equals(FieldType.BOOLEAN)) {

                    value = valueMeta.getBoolean(featureValue);

                } else if (field.getType().equals(FieldType.DATE)) {

                    value = valueMeta.getDate(featureValue);

                } else if (field.getType().equals(FieldType.DOUBLE)) {

                    value = valueMeta.getNumber(featureValue);

                } else if (field.getType().equals(FieldType.LONG)) {

                    value = valueMeta.getInteger(Long.parseLong(String.valueOf(featureValue)));

                } else {
                    value = valueMeta.getString(String.valueOf(featureValue));
                }

            }

            row[fieldIndex] = value;

        }

        return row;

    }

}
