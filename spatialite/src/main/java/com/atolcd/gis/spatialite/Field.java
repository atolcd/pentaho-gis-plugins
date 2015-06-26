package com.atolcd.gis.spatialite;

public class Field {

    public static String TYPE_GEOMETRY = "GEOMETRY";
    public static String TYPE_INTEGER = "INTEGER";
    public static String TYPE_TEXT = "TEXT";
    public static String TYPE_REAL = "REAL";
    public static String TYPE_NUMERIC = "NUMERIC";
    public static String TYPE_NONE = "NONE";

    private String name;
    private String type;
    private GeometryProperties geometryProperties;
    private boolean spatial;

    public Field(String name, GeometryProperties geometryProperties) {
        this.name = name;
        this.type = TYPE_GEOMETRY;
        this.geometryProperties = geometryProperties;
        this.spatial = true;
    }

    public Field(String name, String type) {
        this.name = name;
        this.type = type;
        this.geometryProperties = null;
        this.spatial = false;
    }

    public boolean isSpatial() {
        return spatial;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public GeometryProperties getGeometryProperties() {
        return geometryProperties;
    }

    public String getTypeAffinity() {

        if (type.equalsIgnoreCase(TYPE_GEOMETRY)) {
            return type;

        } else if (type.toUpperCase().contains("INT") && !type.toUpperCase().contains("POINT")) {
            return Field.TYPE_INTEGER;

        } else if (type.toUpperCase().contains("CHAR") || type.toUpperCase().contains("CLOB") || type.toUpperCase().contains("TEXT")) {
            return Field.TYPE_TEXT;

        } else if (type.toUpperCase().contains("BLOB") || type.isEmpty()) {
            return Field.TYPE_NONE;

        } else if (type.toUpperCase().contains("REAL") || type.toUpperCase().contains("FLOA") || type.toUpperCase().contains("DOUB")) {
            return Field.TYPE_REAL;

        } else {
            return Field.TYPE_NUMERIC;
        }

    }

}
