package com.atolcd.gis.spatialite;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.sqlite.SQLiteConfig;
import org.sqlite.spatialite.io.GeometryBlobReader;
import org.sqlite.spatialite.io.GeometryBlobWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class Database {

    // Tables utilis�es pour lire les m�tadonn�es
    private static String SQL_TABLE_MASTER = "sqlite_master";
    protected static String SQL_TABLE_CG = "geometry_columns";
    // private static String SQL_TABLE_SRS = "spatial_ref_sys";

    // Colonnes de la table de m�tadonn�es "geometry_columns"
    protected static String SQL_META_COL_GC_TABLENAME = "f_table_name";
    protected static String SQL_META_COL_GC_COLUMN = "f_geometry_column";
    protected static String SQL_META_COL_GC_TYPE = "geometry_type";
    protected static String SQL_META_COL_GC_DIM = "coord_dimension";
    protected static String SQL_META_COL_GC_SRID = "srid";
    protected static String SQL_META_COL_GC_INDEX = "spatial_index_enabled";

    private static String SQL_META_COL_NAME = "name";
    private static String SQL_META_COL_TYPE = "type";

    private static String SQL_META_TYPE_TABLE = "table";
    // private static String SQL_META_TYPE_VIEW = "view";

    private static GeometryBlobWriter geometryBlobWriter = new GeometryBlobWriter();
    private static GeometryBlobReader geometryBlobReader = new GeometryBlobReader();

    private Connection connection;
    private String fileName;
    private HashMap<String, Table> tables;
    private boolean spatial;

    public Database() {

        this.connection = null;
        this.fileName = null;
        this.tables = new HashMap<String, Table>();
        this.spatial = false;
    }

    public Table getTable(String name) {
        return tables.get(name);
    }

    public List<Table> getTables() {
        return new ArrayList<Table>(tables.values());
    }

    public boolean isSpatial() {
        return spatial;
    }

    /**
     * Cr�ation d'une base de donn�e spatialite vierge
     * 
     * @param fileName
     * @throws IOException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public void createNewSpatialiteDatabase(String fileName) throws IOException {

        String template = "/templates/spatialite_v4";
        InputStream inputstream = Database.class.getResourceAsStream(template);
        @SuppressWarnings("resource")
        OutputStream output = new FileOutputStream(fileName);
        byte[] buffer = new byte[256];
        int bytesRead = 0;
        while ((bytesRead = inputstream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }

    }

    /**
     * Ouvre la connection � la base de donn�e et initialise les m�tadonn�es de
     * base
     * 
     * @param fileName
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws Exception
     */
    public void open(String fileName) throws SQLException, ClassNotFoundException {

        this.fileName = fileName;
        Class.forName("org.sqlite.JDBC");

        SQLiteConfig config = new SQLiteConfig();
        config.setSharedCache(true);
        config.enableLoadExtension(true);
        connection = DriverManager.getConnection("jdbc:sqlite:" + this.fileName);

        // Recherche la pr�sence de la table des colonnes g�om�triques
        try {
            this.connection.createStatement().executeQuery("PRAGMA table_info('" + SQL_TABLE_CG + "')");
            this.spatial = true;
        } catch (SQLException e) {
            this.spatial = false;
        }

        String tableListSql = "";
        if (spatial) {

            // Liste toutes les tables et r�cup�ration de la structure dont
            // pr�sence de g�om�trie
            tableListSql = "SELECT " + SQL_META_COL_NAME + ", " + "CASE WHEN " + SQL_META_COL_GC_TABLENAME + " IS NULL THEN 0 ELSE 1 END AS is_spatial_table " + "FROM "
                    + SQL_TABLE_MASTER + " t "
                    // Comparaison sur les noms de table en minuscule
                    + "LEFT JOIN (SELECT DISTINCT " + SQL_META_COL_GC_TABLENAME + " FROM " + SQL_TABLE_CG + ") gt ON(lower(t." + SQL_META_COL_NAME + ") = lower(gt."
                    + SQL_META_COL_GC_TABLENAME + ")) " + "WHERE " + SQL_META_COL_TYPE + " = '" + SQL_META_TYPE_TABLE + "'";

        } else {

            // Pas de pr�sence de g�om�trie
            tableListSql = "SELECT " + SQL_META_COL_NAME + ", " + "0 AS is_spatial_table " + "FROM " + SQL_TABLE_MASTER + " " + "WHERE " + SQL_META_COL_TYPE + " = '"
                    + SQL_META_TYPE_TABLE + "'";

        }

        ResultSet tableResultSet = this.connection.createStatement().executeQuery(tableListSql);
        while (tableResultSet.next()) {

            Table table = new Table(tableResultSet.getString(SQL_META_COL_NAME));
            HashMap<String, Field> geometryFields = new HashMap<String, Field>();

            // Si table spatiale, r�cup�ration des infos des colonnes geometry
            if (tableResultSet.getInt("is_spatial_table") == 1) {

                table.setSpatial(true);

                String geometriesPropertiesSql = "SELECT " + SQL_META_COL_GC_COLUMN + ", " + SQL_META_COL_GC_TYPE + ", " + SQL_META_COL_GC_DIM + ", " + SQL_META_COL_GC_SRID + ", "
                        + SQL_META_COL_GC_INDEX + " " + "FROM " + SQL_TABLE_CG + " " + "WHERE "
                        // Comparaison sur les noms de table en minuscule
                        + "lower(" + SQL_META_COL_GC_TABLENAME + ") = lower('" + table.getName() + "')";

                ResultSet geometryfieldResultSet = this.connection.createStatement().executeQuery(geometriesPropertiesSql);
                while (geometryfieldResultSet.next()) {

                    String fieldName = geometryfieldResultSet.getString(SQL_META_COL_GC_COLUMN);
                    GeometryProperties geometryProperties = new GeometryProperties(geometryfieldResultSet.getInt(SQL_META_COL_GC_TYPE),
                            geometryfieldResultSet.getInt(SQL_META_COL_GC_DIM), geometryfieldResultSet.getInt(SQL_META_COL_GC_SRID),
                            geometryfieldResultSet.getBoolean(SQL_META_COL_GC_INDEX)

                    );

                    geometryFields.put(fieldName, new Field(fieldName, geometryProperties));

                }
            }

            // R�cup�ration des colonnes de la table
            String fieldListSql = "PRAGMA table_info('" + table.getName() + "')";
            try {

                ResultSet fieldResultSet = this.connection.createStatement().executeQuery(fieldListSql);
                while (fieldResultSet.next()) {

                    String fieldName = fieldResultSet.getString(SQL_META_COL_NAME);
                    String fieldType = fieldResultSet.getString(SQL_META_COL_TYPE);

                    // Si colonne de type geometry (comparaison sur les noms de
                    // colonnes en minuscule)
                    if (geometryFields.containsKey(fieldName.toLowerCase())) {
                        GeometryProperties geometryProperties = geometryFields.get(fieldName.toLowerCase()).getGeometryProperties();
                        table.addField(new Field(fieldName, geometryProperties));
                    } else {
                        table.addField(new Field(fieldName, fieldType));
                    }

                }

                fieldResultSet.close();
                tables.put(table.getName(), table);

            } catch (SQLException e) {
                // throw new Exception("SQL Error : " + e);
            }

        }

        tableResultSet.close();

    }

    /**
     * Ferme la connection � la base de donn�e
     * 
     * @throws SQLException
     */
    public void close() throws SQLException {

        if (!this.connection.isClosed()) {
            this.connection.close();
        }

    }

    public void writeRows(Table table, List<Row> rows, Long commitlimit, String primaryKeyFieldName) throws Exception {

        // Suppression de la table si elle existe
        if (getTable(table.getName()) != null) {
            for (String statement : table.getSqlDropTableStatements()) {
                this.connection.createStatement().execute(statement);
            }
        }

        // Cr�ation de la table + mise � jour des informations de g�om�tries
        for (String statement : table.getSqlCreateTableStatements(primaryKeyFieldName)) {
            this.connection.createStatement().execute(statement);
        }

        if (commitlimit == null || commitlimit == 0) {
            commitlimit = (long) 1;
        }

        this.connection.setAutoCommit(false);

        int featIndex = 0;

        // Insertion des donn�es
        PreparedStatement preparedStatement = this.connection.prepareStatement(table.getSqlInsertStatement());
        for (Row row : rows) {

            int i = 1;
            for (Field field : table.getFields()) {

                Object value = row.getValue(field.getName());

                if (value != null) {

                    if (field.getTypeAffinity().equals(Field.TYPE_GEOMETRY)) {

                        Geometry geometry = (Geometry) value;
                        if (checkGeometry(geometry, field.getGeometryProperties())) {
                            preparedStatement.setBytes(i, geometryBlobWriter.write(geometry));
                        }

                    } else if (field.getTypeAffinity().equals(Field.TYPE_INTEGER)) {
                        preparedStatement.setLong(i, (Long) value);

                    } else if (field.getTypeAffinity().equals(Field.TYPE_NONE)) {
                        preparedStatement.setBytes(i, (byte[]) value);

                    } else if (field.getTypeAffinity().equals(Field.TYPE_NUMERIC)) {
                        preparedStatement.setDouble(i, (Double) value);

                    } else if (field.getTypeAffinity().equals(Field.TYPE_REAL)) {
                        preparedStatement.setDouble(i, (Double) value);

                    } else if (field.getTypeAffinity().equals(Field.TYPE_TEXT)) {
                        preparedStatement.setString(i, (String) value);
                    }

                } else {
                    preparedStatement.setObject(i, null);
                }

                i++;

            }

            preparedStatement.executeUpdate();
            featIndex++;

            if (featIndex == commitlimit) {
                featIndex = 0;
                this.connection.commit();
            }

        }

        this.connection.commit();
        preparedStatement.close();

        // Cr�ation des triggers
        for (String statement : table.getSqlCreateTriggerStatements()) {
            this.connection.createStatement().execute(statement);
        }

    }

    public List<Row> getRows(Table table, Long limit) throws Exception {

        List<Row> rows = new ArrayList<Row>();

        for (Field field : table.getFields()) {
            if (field.isSpatial()) {
                if (field.getGeometryProperties().getCoordDimension() != 2) {
                    throw new Exception("Only 2D geometries are supported");
                }
            }
        }

        ResultSet valuesResultSet = this.connection.createStatement().executeQuery(table.getSqlSelectStatement(limit));
        while (valuesResultSet.next()) {

            Row row = new Row();
            for (Field field : table.getFields()) {

                Object value = valuesResultSet.getObject(field.getName());
                if (value != null) {

                    // Geometry
                    if (field.isSpatial()) {

                        byte[] bytes = (byte[]) value;
                        Geometry geometry = geometryBlobReader.read(bytes);
                        row.addValue(field.getName(), geometry);

                    } else if (field.getTypeAffinity().equals(Field.TYPE_INTEGER)) {
                        row.addValue(field.getName(), (Integer) value);

                    } else if (field.getTypeAffinity().equals(Field.TYPE_TEXT)) {
                        row.addValue(field.getName(), (String) value);

                    } else if (field.getTypeAffinity().equals(Field.TYPE_NONE)) {
                        row.addValue(field.getName(), (byte[]) value);

                    } else if (field.getTypeAffinity().equals(Field.TYPE_REAL)) {
                        row.addValue(field.getName(), (Float) value);

                    } else {
                        row.addValue(field.getName(), (Double) value);
                    }

                } else {
                    row.addValue(field.getName(), null);
                }

            }

            rows.add(row);

        }

        return rows;

    }

    private static boolean checkGeometry(Geometry geometry, GeometryProperties geometryProperties) throws Exception {

        if (geometry != null && !geometry.isEmpty()) {

            // Dimension des coordonn�es
            Coordinate firstCoordinate = geometry.getCoordinates()[0];
            if (!Double.isNaN(firstCoordinate.z)) {
                throw new Exception("Only 2D geometries are supported");
            }

            // SRID
            if (!((Integer) geometry.getSRID()).equals(geometryProperties.getSrid())) {
                throw new Exception("Geometry SRID must be " + geometryProperties.getSrid());
            }

            // Type
            if (!(geometry.getGeometryType().equalsIgnoreCase(geometryProperties.getJTSGeometryType()))) {
                throw new Exception("Geometry type must be " + geometryProperties.getJTSGeometryType());
            }

        }

        return true;

    }

}
