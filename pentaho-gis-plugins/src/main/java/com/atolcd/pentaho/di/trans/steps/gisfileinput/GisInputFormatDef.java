package com.atolcd.pentaho.di.trans.steps.gisfileinput;

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
import java.util.LinkedHashMap;
import java.util.List;

public class GisInputFormatDef {

    private String key;
    private String[] extensions;
    private LinkedHashMap<String, GisInputFormatParameterDef> parameterDefs;

    public GisInputFormatDef(String key, String[] extensions) {
        this.key = key;
        this.extensions = extensions;
        this.parameterDefs = new LinkedHashMap<String, GisInputFormatParameterDef>();
    }

    public String getKey() {
        return key;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public void addParameterDef(String key, int valueMetaType, boolean required) {
        parameterDefs.put(key, new GisInputFormatParameterDef(key, valueMetaType, required));
    }

    public void addParameterDef(String key, int valueMetaType, boolean required, List<String> predefinedValues, String defaultValue) {
        parameterDefs.put(key, new GisInputFormatParameterDef(key, valueMetaType, required, predefinedValues, defaultValue));
    }

    public GisInputFormatParameterDef getParameterDef(String key) {
        return parameterDefs.get(key);
    }

    public List<GisInputFormatParameterDef> getParameterDefs() {
        return new ArrayList<GisInputFormatParameterDef>(parameterDefs.values());
    }

}
