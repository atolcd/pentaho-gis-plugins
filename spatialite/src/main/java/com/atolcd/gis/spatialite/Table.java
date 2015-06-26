package com.atolcd.gis.spatialite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class Table {

    private String name;
    private LinkedHashMap<String, Field> fields;
    private boolean spatial;

    public Table(String name) {
        this.name = name;
        this.fields = new LinkedHashMap<String, Field>();
        this.spatial = false;
    }

    public String getName() {
        return name;
    }

    public void addField(Field field) {
        fields.put(field.getName(), field);
    }

    public Field getField(String fieldName) {
        return fields.get(fieldName);
    }

    public List<Field> getFields() {
        return new ArrayList<Field>(fields.values());
    }

    public boolean isSpatial() {
        return spatial;
    }

    public void setSpatial(boolean spatial) {
        this.spatial = spatial;
    }

    public List<String> getFieldNames() {
        return new ArrayList<String>(fields.keySet());
    }

    protected String getSqlInsertStatement() {

        String statement = "INSERT INTO " + name + " (" + StringUtils.join(getFieldNames(), ", ") + ") " + "VALUES(";

        for (int i = 0; i < fields.size(); i++) {
            statement = statement + "?, ";
        }
        statement = StringUtils.removeEnd(statement, ", ") + ");";
        return statement;
    }

    protected List<String> getSqlDropTableStatements() {

        List<String> statements = new ArrayList<String>();

        statements.add("DROP TABLE " + name + ";");
        if (isSpatial()) {
            statements.add("DELETE FROM " + Database.SQL_META_COL_GC_TABLENAME + " WHERE " + Database.SQL_META_COL_GC_TABLENAME + " = '" + name.toLowerCase() + "';");
        }

        return statements;

    }

    protected String getSqlSelectStatement(Long limit) {

        String statement = "SELECT " + StringUtils.join(getFieldNames(), ", ") + " " + "FROM " + name;

        if (limit > 0) {
            statement = statement + " LIMIT " + limit + ";";
        } else {
            statement = statement + ";";
        }

        return statement;

    }

    protected List<String> getSqlCreateTableStatements(String pkFieldName) {

        List<String> statements = new ArrayList<String>();
        List<String> geometryColumnStatements = new ArrayList<String>();
        String createtableSql = "CREATE TABLE " + name + " (";

        for (Field field : this.getFields()) {

            if (field.getName().equalsIgnoreCase(pkFieldName)) {
                createtableSql = createtableSql + field.getName() + " " + field.getTypeAffinity() + " PRIMARY KEY, ";
            } else {
                createtableSql = createtableSql + field.getName() + " " + field.getTypeAffinity() + ", ";
            }

            if (field.isSpatial()) {

                GeometryProperties geometryProperties = field.getGeometryProperties();

                // Mise � jour de la table de m�tadonn�es des g�om�tries
                geometryColumnStatements.add("INSERT INTO " + Database.SQL_TABLE_CG + " (" + Database.SQL_META_COL_GC_TABLENAME + ", " + Database.SQL_META_COL_GC_COLUMN + ", "
                        + Database.SQL_META_COL_GC_TYPE + ", " + Database.SQL_META_COL_GC_DIM + ", " + Database.SQL_META_COL_GC_SRID + ", " + Database.SQL_META_COL_GC_INDEX
                        + ") VALUES ("
                        // Nom de table forc�ment en minuscule
                        + "'" + name.toLowerCase() + "', "
                        // Nom de colonne forc�ment en minuscule
                        + "'" + field.getName().toLowerCase() + "', " + geometryProperties.getGeometryType() + ", " + geometryProperties.getCoordDimension() + ", "
                        + geometryProperties.getSrid() + ", " + 0 + ");");

            }

        }

        statements.add(StringUtils.removeEnd(createtableSql, ", ") + ");");
        statements.addAll(geometryColumnStatements);
        return statements;

    }

    protected List<String> getSqlCreateTriggerStatements() {

        List<String> statements = new ArrayList<String>();

        for (Field field : this.getFields()) {

            if (field.isSpatial()) {

                // Before Insert
                statements.add("CREATE TRIGGER \"ggi_" + name + "_" + field.getName() + "\" BEFORE INSERT ON \"" + name + "\" " + "FOR EACH ROW BEGIN "
                        + "SELECT RAISE(ROLLBACK, '" + name + "." + field.getName() + " violates Geometry constraint [geom-type or SRID not allowed]') "
                        + "WHERE (SELECT geometry_type FROM geometry_columns " + "WHERE Lower(f_table_name) = Lower('" + name + "') AND Lower(f_geometry_column) = Lower('"
                        + field.getName() + "') " + "AND GeometryConstraints(NEW.\"" + field.getName() + "\", geometry_type, srid) = 1) IS NULL; " + "END;");

                // Before Update
                statements.add("CREATE TRIGGER \"ggu_" + name + "_" + field.getName() + "\" BEFORE UPDATE ON \"" + name + "\" " + "FOR EACH ROW BEGIN "
                        + "SELECT RAISE(ROLLBACK, '" + name + "." + field.getName() + " violates Geometry constraint [geom-type or SRID not allowed]') "
                        + "WHERE (SELECT geometry_type FROM geometry_columns " + "WHERE Lower(f_table_name) = Lower('" + name + "') AND Lower(f_geometry_column) = Lower('"
                        + field.getName() + "') " + "AND GeometryConstraints(NEW.\"" + field.getName() + "\", geometry_type, srid) = 1) IS NULL; " + "END;");

                // After Insert
                statements.add("CREATE TRIGGER \"gii_" + name + "_" + field.getName() + "\" AFTER INSERT ON \"" + name + "\" " + "FOR EACH ROW BEGIN "
                        + "UPDATE geometry_columns_time SET last_insert = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') " + "WHERE Lower(f_table_name) = Lower('" + name
                        + "') AND Lower(f_geometry_column) = Lower('" + field.getName() + "');" + "END;");

                // After update
                statements.add("CREATE TRIGGER \"giu_" + name + "_" + field.getName() + "\" AFTER UPDATE ON \"" + name + "\" " + "FOR EACH ROW BEGIN "
                        + "UPDATE geometry_columns_time SET last_insert = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') " + "WHERE Lower(f_table_name) = Lower('" + name
                        + "') AND Lower(f_geometry_column) = Lower('" + field.getName() + "');" + "END;");

            }
        }

        return statements;
    }

}
