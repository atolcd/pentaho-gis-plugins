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
import java.util.LinkedHashMap;
import java.util.List;

public class GisOutputFormatDef {

    private String key;
    private String[] extensions;
    private String[] extensionsNames;
    private LinkedHashMap<String, GisOutputFormatParameterDef> parameterFieldDefs;
    private LinkedHashMap<String, GisOutputFormatParameterDef> parameterFixedDefs;

    public GisOutputFormatDef(String key, String[] extensions, String[] extensionsNames) {
        this.key = key;
        this.extensions = extensions;
        this.extensionsNames = extensionsNames;
        this.parameterFieldDefs = new LinkedHashMap<String, GisOutputFormatParameterDef>();
        this.parameterFixedDefs = new LinkedHashMap<String, GisOutputFormatParameterDef>();
    }

    public String getKey() {
        return key;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public String[] getExtensionsNames() {
        return extensionsNames;
    }

    public void addParameterFieldDef(String key, int valueMetaType, boolean required) {
        parameterFieldDefs.put(key, new GisOutputFormatParameterDef(key, valueMetaType, required));
    }

    public void addParameterFieldDef(String key, int valueMetaType, boolean required, List<String> predefinedValues, String defaultValue) {
        parameterFieldDefs.put(key, new GisOutputFormatParameterDef(key, valueMetaType, required, predefinedValues, defaultValue));
    }

    public void addParameterFixedDef(String key, int valueMetaType, boolean required) {
        parameterFixedDefs.put(key, new GisOutputFormatParameterDef(key, valueMetaType, required));
    }

    public void addParameterFixedDef(String key, int valueMetaType, boolean required, List<String> predefinedValues, String defaultValue) {
        parameterFixedDefs.put(key, new GisOutputFormatParameterDef(key, valueMetaType, required, predefinedValues, defaultValue));
    }

    public GisOutputFormatParameterDef getParameterFieldDef(String key) {
        return parameterFieldDefs.get(key);
    }

    public List<GisOutputFormatParameterDef> getParameterFieldDefs() {
        return new ArrayList<GisOutputFormatParameterDef>(parameterFieldDefs.values());
    }

    public GisOutputFormatParameterDef getParameterFixedDef(String key) {
        return parameterFixedDefs.get(key);
    }

    public List<GisOutputFormatParameterDef> getParameterFixedDefs() {
        return new ArrayList<GisOutputFormatParameterDef>(parameterFixedDefs.values());
    }

}
