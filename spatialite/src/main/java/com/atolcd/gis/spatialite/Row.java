package com.atolcd.gis.spatialite;

import java.util.HashMap;

public class Row {

    private HashMap<String, Object> values;

    public Row() {
        this.values = new HashMap<String, Object>();
    }

    public void addValue(String fieldName, Object value) {
        values.put(fieldName, value);
    }

    public Object getValue(String fieldName) {
        return values.get(fieldName);
    }

    public void addValue(Field field, Object value) {
        values.put(field.getName(), value);
    }

    public Object getValue(Field field) {
        return values.get(field.getName());
    }

}
