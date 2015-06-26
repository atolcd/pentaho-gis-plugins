package com.atolcd.pentaho.di.trans.steps.gisfileoutput;

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

public class GisOutputFormatParameterDef {

    public static String TYPE_FIELD = "FIELD_VALUE";
    public static String TYPE_FIXED = "FIXED_VALUE";

    private String key;
    private int valueMetaType;
    private boolean required;
    private List<String> predefinedValues;
    private String defaultValue;

    public GisOutputFormatParameterDef(String key, int valueMetaType, boolean required) {
        this.key = key;
        this.valueMetaType = valueMetaType;
        this.required = required;
        this.predefinedValues = new ArrayList<String>();
        this.defaultValue = null;
    }

    public GisOutputFormatParameterDef(String key, int valueMetaType, boolean required, List<String> predefinedValues, String defaultValue) {
        this.key = key;
        this.valueMetaType = valueMetaType;
        this.required = required;
        if (predefinedValues != null) {
            this.predefinedValues = predefinedValues;
        } else {
            this.predefinedValues = new ArrayList<String>();
        }
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public int getValueMetaType() {
        return valueMetaType;
    }

    public boolean isRequired() {
        return required;
    }

    public List<String> getPredefinedValues() {
        return predefinedValues;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

}
