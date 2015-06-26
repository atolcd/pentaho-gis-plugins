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


public class Field {

    private String name;
    private FieldType type;
    private Integer length;
    private Integer decimalCount;

    public Field(String name, FieldType type, Integer length, Integer decimalCount) {

        this.name = name;
        this.type = type;
        this.length = length;
        this.decimalCount = decimalCount;

    }

    public enum FieldType {
        STRING, LONG, DOUBLE, BOOLEAN, DATE, BINARY, GEOMETRY
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FieldType getType() {
        return type;
    }

    public String getJavaType() {

        if (type.equals(FieldType.STRING)) {
            return "String";
        } else if (type.equals(FieldType.LONG)) {
            return "Integer";
        } else if (type.equals(FieldType.DOUBLE)) {
            return "Double";
        } else if (type.equals(FieldType.BOOLEAN)) {
            return "Boolean";
        } else if (type.equals(FieldType.DATE)) {
            return "Date";
        } else if (type.equals(FieldType.GEOMETRY)) {
            return "Geometry";
        } else {
            return null;
        }
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getDecimalCount() {
        return decimalCount;
    }

    public void setDecimalCount(Integer decimalCount) {
        this.decimalCount = decimalCount;
    }

}
