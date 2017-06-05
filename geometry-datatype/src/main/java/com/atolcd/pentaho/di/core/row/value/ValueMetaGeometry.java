package com.atolcd.pentaho.di.core.row.value;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.Date;

import oracle.spatial.geometry.JGeometry;
import oracle.spatial.util.WKT;
import oracle.sql.STRUCT;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.database.MySQLDatabaseMeta;
import org.pentaho.di.core.database.OracleDatabaseMeta;
import org.pentaho.di.core.database.PostgreSQLDatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleEOFException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.ValueDataUtil;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.GeometryInterface;
import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.core.row.value.ValueMetaPlugin;
import org.postgis.PGgeometryLW;
import org.postgis.binary.BinaryParser;
import org.postgis.binary.BinaryWriter;

import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ByteOrderValues;
import com.vividsolutions.jts.io.InputStreamInStream;
import com.vividsolutions.jts.io.OutputStreamOutStream;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

@ValueMetaPlugin(id = "43663879", name = "Geometry", description = "A geometry GIS object")
public class ValueMetaGeometry extends ValueMetaBase implements GeometryInterface, Cloneable {

    // Postgis
    public static BinaryParser pgGeometryParser = new BinaryParser();
    public static BinaryWriter pgGeometryWriter = new BinaryWriter();

    // Oracle spatial/locator
    public static WKT ociWktReaderWriter = new WKT();

    public static final int TYPE_GEOMETRY = 43663879; // Value is "GEOMETRY" on
                                                      // a phone keypad

    public ValueMetaGeometry() {
        this(null);
    }

    public ValueMetaGeometry(String name) {
        super(name, TYPE_GEOMETRY);
    }

    @Override
    public ValueMetaGeometry clone() {
        ValueMetaGeometry geometryMeta = (ValueMetaGeometry) super.clone();
        return geometryMeta;

    }

    @Override
    public Object cloneValueData(Object object) throws KettleValueException {
        Geometry geometry = getGeometry(object);
        if (geometry == null)
            return null;

        try {
            Geometry cloneGeometry = new GeometryFactory().createGeometry(geometry);
            cloneGeometry.setSRID(geometry.getSRID());
            return cloneGeometry;
        } catch (Exception e) {
            throw new KettleValueException("Unable to clone Geometry", e);
        }
    }

    @Override
    public String getString(Object object) throws KettleValueException {
        try {

            String string = null;

            switch (type) {

            case TYPE_GEOMETRY:

                if (object != null) {

                    Geometry geometry = getGeometry(object);

                    if (GeometryUtils.getCoordinateDimension(geometry) == 3) {
                        string = new WKTWriter(3).write(geometry);
                    } else {
                        string = new WKTWriter(2).write(geometry);
                    }

                    if (geometry.getSRID() > 0) {
                        string = "SRID=" + geometry.getSRID() + ";" + string;
                    }
                }
                break;

            case ValueMetaInterface.TYPE_STRING:
                switch (storageType) {
                case ValueMetaInterface.STORAGE_TYPE_NORMAL:
                    string = object == null ? null : object.toString();
                    break;
                case ValueMetaInterface.STORAGE_TYPE_BINARY_STRING:
                    string = (String) convertBinaryStringToNativeType((byte[]) object);
                    break;
                case ValueMetaInterface.STORAGE_TYPE_INDEXED:
                    string = object == null ? null : (String) index[((Integer) object).intValue()];
                    break;
                default:
                    throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
                }
                if (string != null)
                    string = trim(string);
                break;

            case ValueMetaInterface.TYPE_DATE:
                switch (storageType) {
                case ValueMetaInterface.STORAGE_TYPE_NORMAL:
                    string = convertDateToString((Date) object);
                    break;
                case ValueMetaInterface.STORAGE_TYPE_BINARY_STRING:
                    string = convertDateToString((Date) convertBinaryStringToNativeType((byte[]) object));
                    break;
                case ValueMetaInterface.STORAGE_TYPE_INDEXED:
                    string = object == null ? null : convertDateToString((Date) index[((Integer) object).intValue()]);
                    break;
                default:
                    throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
                }
                break;

            case ValueMetaInterface.TYPE_NUMBER:
                switch (storageType) {
                case ValueMetaInterface.STORAGE_TYPE_NORMAL:
                    string = convertNumberToString((Double) object);
                    break;
                case ValueMetaInterface.STORAGE_TYPE_BINARY_STRING:
                    string = convertNumberToString((Double) convertBinaryStringToNativeType((byte[]) object));
                    break;
                case ValueMetaInterface.STORAGE_TYPE_INDEXED:
                    string = object == null ? null : convertNumberToString((Double) index[((Integer) object).intValue()]);
                    break;
                default:
                    throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
                }
                break;

            case ValueMetaInterface.TYPE_INTEGER:
                switch (storageType) {
                case ValueMetaInterface.STORAGE_TYPE_NORMAL:
                    string = convertIntegerToString((Long) object);
                    break;
                case ValueMetaInterface.STORAGE_TYPE_BINARY_STRING:
                    string = convertIntegerToString((Long) convertBinaryStringToNativeType((byte[]) object));
                    break;
                case ValueMetaInterface.STORAGE_TYPE_INDEXED:
                    string = object == null ? null : convertIntegerToString((Long) index[((Integer) object).intValue()]);
                    break;
                default:
                    throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
                }
                break;

            case ValueMetaInterface.TYPE_BIGNUMBER:
                switch (storageType) {
                case ValueMetaInterface.STORAGE_TYPE_NORMAL:
                    string = convertBigNumberToString((BigDecimal) object);
                    break;
                case ValueMetaInterface.STORAGE_TYPE_BINARY_STRING:
                    string = convertBigNumberToString((BigDecimal) convertBinaryStringToNativeType((byte[]) object));
                    break;
                case ValueMetaInterface.STORAGE_TYPE_INDEXED:
                    string = object == null ? null : convertBigNumberToString((BigDecimal) index[((Integer) object).intValue()]);
                    break;
                default:
                    throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
                }
                break;

            case ValueMetaInterface.TYPE_BOOLEAN:
                switch (storageType) {
                case ValueMetaInterface.STORAGE_TYPE_NORMAL:
                    string = convertBooleanToString((Boolean) object);
                    break;
                case ValueMetaInterface.STORAGE_TYPE_BINARY_STRING:
                    string = convertBooleanToString((Boolean) convertBinaryStringToNativeType((byte[]) object));
                    break;
                case ValueMetaInterface.STORAGE_TYPE_INDEXED:
                    string = object == null ? null : convertBooleanToString((Boolean) index[((Integer) object).intValue()]);
                    break;
                default:
                    throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
                }
                break;

            case ValueMetaInterface.TYPE_BINARY:
                switch (storageType) {
                case ValueMetaInterface.STORAGE_TYPE_NORMAL:
                    string = convertBinaryStringToString((byte[]) object);
                    break;
                case ValueMetaInterface.STORAGE_TYPE_BINARY_STRING:
                    string = convertBinaryStringToString((byte[]) object);
                    break;
                case ValueMetaInterface.STORAGE_TYPE_INDEXED:
                    string = object == null ? null : convertBinaryStringToString((byte[]) index[((Integer) object).intValue()]);
                    break;
                default:
                    throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
                }
                break;

            case ValueMetaInterface.TYPE_SERIALIZABLE:
                switch (storageType) {
                case ValueMetaInterface.STORAGE_TYPE_NORMAL:
                    string = object == null ? null : object.toString();
                    break; // just go for the default toString()
                case ValueMetaInterface.STORAGE_TYPE_BINARY_STRING:
                    string = convertBinaryStringToString((byte[]) object);
                    break;
                case ValueMetaInterface.STORAGE_TYPE_INDEXED:
                    string = object == null ? null : index[((Integer) object).intValue()].toString();
                    break; // just go for the default toString()
                default:
                    throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
                }
                break;

            default:
                throw new KettleValueException(toString() + " : Unknown type " + type + " specified.");
            }

            if (isOutputPaddingEnabled() && getLength() > 0) {
                string = ValueDataUtil.rightPad(string, getLength());
            }

            return string;
        } catch (ClassCastException e) {
            throw new KettleValueException(toString() + " : There was a data type error: the data type of " + object.getClass().getName() + " object [" + object
                    + "] does not correspond to value meta [" + toStringMeta() + "]");
        }

    }

    @Override
    public Double getNumber(Object object) throws KettleValueException {
        throw new KettleValueException(toString() + " : can't be converted to a number");
    }

    @Override
    public Long getInteger(Object object) throws KettleValueException {
        throw new KettleValueException(toString() + " : can't be converted to an integer");
    }

    @Override
    public BigDecimal getBigNumber(Object object) throws KettleValueException {
        throw new KettleValueException(toString() + " : can't be converted to a big number");
    }

    @Override
    public Boolean getBoolean(Object object) throws KettleValueException {
        throw new KettleValueException(toString() + " : can't be converted to a boolean");
    }

    @Override
    public Date getDate(Object object) throws KettleValueException {
        throw new KettleValueException(toString() + " : can't be converted to a date");
    }

    @Override
    public Object convertData(ValueMetaInterface meta2, Object data2) throws KettleValueException {
        try {
            switch (meta2.getType()) {
            case ValueMetaInterface.TYPE_STRING:
                return convertStringToGeometry(meta2.getString(data2));
            case TYPE_GEOMETRY:
                return data2;
            default:
                throw new KettleValueException(meta2.toStringMeta() + " : can't be converted to a geometry");
            }
        } catch (KettlePluginException kpe) {
            throw new KettleValueException(kpe);
        }
    }

    @Override
    public Object getNativeDataType(Object object) throws KettleValueException {
        return getGeometry(object);
    }

    public Geometry getGeometry(Object object) throws KettleValueException {

        try {
            if (object == null || object instanceof Geometry) {
                return (Geometry) object;
            }
            switch (type) {
            case ValueMetaInterface.TYPE_NUMBER:
                throw new KettleValueException(toString() + " : I don't know how to convert a number to a geometry.");
            case ValueMetaInterface.TYPE_STRING:
                switch (storageType) {
                case ValueMetaInterface.STORAGE_TYPE_NORMAL:
                    return convertStringToGeometry((String) object);
                case ValueMetaInterface.STORAGE_TYPE_BINARY_STRING:
                    return convertStringToGeometry((String) convertBinaryStringToNativeType((byte[]) object));
                case ValueMetaInterface.STORAGE_TYPE_INDEXED:
                    return convertStringToGeometry((String) index[((Integer) object).intValue()]);
                default:
                    throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
                }
            case ValueMetaInterface.TYPE_DATE:
                throw new KettleValueException(toString() + " : I don't know how to convert a date to a geometry.");
            case ValueMetaInterface.TYPE_INTEGER:
                throw new KettleValueException(toString() + " : I don't know how to convert an integer to a geometry.");
            case ValueMetaInterface.TYPE_BIGNUMBER:
                throw new KettleValueException(toString() + " : I don't know how to convert a big number to a geometry.");
            case ValueMetaInterface.TYPE_BOOLEAN:
                throw new KettleValueException(toString() + " : I don't know how to convert a boolean to a geometry.");
            case ValueMetaInterface.TYPE_BINARY:
                throw new KettleValueException(toString() + " : I don't know how to convert binary values to numbers.");
            case ValueMetaInterface.TYPE_SERIALIZABLE:
                throw new KettleValueException(toString() + " : I don't know how to convert serializable values to numbers.");
            default:
                throw new KettleValueException(toString() + " : Unknown type " + type + " specified.");
            }
        } catch (Exception e) {
            throw new KettleValueException("Unexpected conversion error while converting value [" + toString() + "] to a Geometry", e);
        }

    }

    protected Geometry convertStringToGeometry(String object) throws KettlePluginException {

        try {
            return GeometryUtils.getGeometryFromEWKT(object);
        } catch (Exception e) {
            throw new KettlePluginException(e);
        }

    }

    @Override
    public Object readData(DataInputStream inputStream) throws KettleFileException, KettleEOFException, SocketTimeoutException {
        try {
            // Is the value NULL?
            if (inputStream.readBoolean()) {
                return null; // done
            }

            switch (storageType) {
            case ValueMetaInterface.STORAGE_TYPE_NORMAL:
                try {

                    int size = inputStream.readInt();
                    byte[] buffer = new byte[size];
                    inputStream.readFully(buffer);

                    return new WKBReader().read(buffer);

                } catch (ParseException e) {
                    e.printStackTrace();
                }

            case ValueMetaInterface.STORAGE_TYPE_BINARY_STRING:
                return readBinaryString(inputStream);

            case ValueMetaInterface.STORAGE_TYPE_INDEXED:
                return readSmallInteger(inputStream);

            default:
                throw new KettleFileException(toString() + " : Unknown storage type " + getStorageType());
            }
        } catch (EOFException e) {
            throw new KettleEOFException(e);
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            throw new KettleFileException(toString() + " : Unable to read value geometry from input stream", e);
        }
    }

    @Override
    public void writeData(DataOutputStream outputStream, Object object) throws KettleFileException {
        try {
            // Is the value NULL?
            outputStream.writeBoolean(object == null);

            if (object != null) {
                switch (storageType) {
                case ValueMetaInterface.STORAGE_TYPE_NORMAL:
                    byte[] binary = new WKBWriter().write((Geometry) object);
                    outputStream.writeInt(binary.length);
                    outputStream.write(binary);

                    break;

                case ValueMetaInterface.STORAGE_TYPE_BINARY_STRING:
                    writeBinaryString(outputStream, (byte[]) object);
                    break;

                case ValueMetaInterface.STORAGE_TYPE_INDEXED:
                    writeInteger(outputStream, (Integer) object); // just an
                                                                  // index
                    break;

                default:
                    throw new KettleFileException(toString() + " : Unknown storage type " + getStorageType());
                }
            }
        } catch (ClassCastException e) {
            throw new RuntimeException(toString() + " : There was a data type error: the data type of " + object.getClass().getName() + " object [" + object
                    + "] does not correspond to value meta [" + toStringMeta() + "]");
        } catch (IOException e) {
            throw new KettleFileException(toString() + " : Unable to write value geometry to output stream", e);
        }
    }

    /**
     * Return ValueMetaGeometry if sql column type is spatial column
     * 
     * @param databaseMeta
     * @param name
     * @param rm
     * @param index
     * @param ignoreLength
     * @param lazyConversion
     * @return
     * @throws KettleDatabaseException
     */
    @Override
    public ValueMetaInterface getValueFromSQLType(DatabaseMeta databaseMeta, String name, ResultSetMetaData rm, int index, boolean ignoreLength, boolean lazyConversion)
            throws KettleDatabaseException {

        try {

            int type = rm.getColumnType(index);
            String columnTypeName = rm.getColumnTypeName(index);
            boolean isDatabaseGeometryColumn;

            // Postgis
            if (databaseMeta.getDatabaseInterface() instanceof PostgreSQLDatabaseMeta && type == java.sql.Types.OTHER && columnTypeName.equalsIgnoreCase("GEOMETRY")) {

                isDatabaseGeometryColumn = true;

                // Oracle Spatial/Locator
            } else if (databaseMeta.getDatabaseInterface() instanceof OracleDatabaseMeta && type == java.sql.Types.STRUCT && columnTypeName.equalsIgnoreCase("MDSYS.SDO_GEOMETRY")) {

                isDatabaseGeometryColumn = true;

                // MySQL
            } else if (databaseMeta.getDatabaseInterface() instanceof MySQLDatabaseMeta && type == java.sql.Types.BINARY && columnTypeName.equalsIgnoreCase("GEOMETRY")) {

                isDatabaseGeometryColumn = true;

            } else {

                isDatabaseGeometryColumn = false;

            }

            // Return ValueMetaGeometry
            if (isDatabaseGeometryColumn) {

                ValueMetaGeometry valueMeta = new ValueMetaGeometry(name);
                getOriginalColumnMetadata(valueMeta, rm, index, ignoreLength);
                return valueMeta;

            } else {
                return null;
            }

        } catch (Exception e) {
            throw new KettleDatabaseException("Error evaluating value metadata", e);
        }
    }

    /**
     * Get Jts Geometry from resultset
     * 
     * @param databaseInterface
     * @param resultSet
     * @param index
     * @return
     * @throws KettleDatabaseException
     */

    @Override
    public Object getValueFromResultSet(DatabaseInterface databaseInterface, ResultSet resultSet, int index) throws KettleDatabaseException {

        try {

            Geometry geometry = null;
            Integer srid = null;

            // Postgis
            if (databaseInterface instanceof PostgreSQLDatabaseMeta) {

                String wkt = resultSet.getString(index + 1);
                if (wkt != null) {
                    org.postgis.Geometry pgGeometry = pgGeometryParser.parse(resultSet.getString(index + 1));
                    String type = pgGeometry.getTypeString().trim();
                    String coords = pgGeometry.getValue().trim();
                    srid = pgGeometry.getSrid();
                    geometry = new WKTReader().read(type + coords);
                }

                // Oracle Spatial/Locator
            } else if (databaseInterface instanceof OracleDatabaseMeta) {

                if (resultSet.getObject(index + 1) != null) {

                    STRUCT st = (oracle.sql.STRUCT) resultSet.getObject(index + 1);
                    JGeometry ociGeometry = JGeometry.load(st);
                    srid = ociGeometry.getSRID();

                    // TODO : gerer la 3D sans passer par du WKT
                    /*
                     * Type Code 2D Code 3D POINT 2001 3001 LINE 2002 3002
                     * POLYGON 2003 3003 COLLECTION 2004 3004 MULTIPOINT 2005
                     * 3005 MULTILINE 2006 3006 MULTIPOLYGON 2007 3007
                     */

                    if (ociGeometry.getDimensions() > 2) {
                        throw new KettleDatabaseException(toStringMeta() + " : Unable to get Geometry " + ociGeometry.getDimensions() + "D  from resultset at index " + index
                                + " for " + databaseInterface.getDriverClass().toString());
                    }

                    String wkt = new String(ociWktReaderWriter.fromJGeometry(ociGeometry));
                    geometry = new WKTReader().read(wkt);
                }

                // MySQL
            } else if (databaseInterface instanceof MySQLDatabaseMeta) {

                int byteOrder = ByteOrderValues.LITTLE_ENDIAN;

                byte[] bytes = resultSet.getBytes(index + 1);
                if (bytes != null) {

                    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

                    // SRID
                    byte[] sridBytes = new byte[4];
                    inputStream.read(sridBytes);
                    srid = ByteOrderValues.getInt(sridBytes, byteOrder);

                    // Geometry
                    geometry = new WKBReader().read(new InputStreamInStream(inputStream));
                }

            }

            if (srid != null) {
                geometry.setSRID(srid);
            }

            return geometry;

        } catch (Exception e) {
            throw new KettleDatabaseException(toStringMeta() + " : Unable to get Geometry from resultset at index " + index + " for "
                    + databaseInterface.getDriverClass().toString(), e);
        }
    }

    /**
     * Get Sql type name for geometry data type
     * 
     * @param databaseInterface
     * @param tk
     * @param pk
     * @param use_autoinc
     * @param add_fieldname
     * @param add_cr
     * @return
     */
    @Override
    public String getDatabaseColumnTypeDefinition(DatabaseInterface databaseInterface, String tk, String pk, boolean use_autoinc, boolean add_fieldname, boolean add_cr) {

        String retval = null;

        // Postgis or Oracle Spatial/Locator or Mysql
        if (databaseInterface instanceof PostgreSQLDatabaseMeta || databaseInterface instanceof OracleDatabaseMeta || databaseInterface instanceof MySQLDatabaseMeta) {

            if (add_fieldname) {
                retval = getName() + " ";
            } else {
                retval = "";
            }

            // Postgis
            if (databaseInterface instanceof PostgreSQLDatabaseMeta) {
                retval += "GEOMETRY";

                // Oracle Spatial/Locator
            } else if (databaseInterface instanceof OracleDatabaseMeta) {
                retval += "SDO_GEOMETRY";

                // Mysql
            } else if (databaseInterface instanceof MySQLDatabaseMeta) {
                retval += "GEOMETRY";
            }

            if (add_cr) {
                retval += Const.CR;
            }
        }

        return retval;
    }

    /**
     * Encode geometry to for spatial column
     * 
     * @param databaseMeta
     * @param preparedStatement
     * @param index
     * @param data
     * @throws KettleDatabaseException
     */
    @Override
    public void setPreparedStatementValue(DatabaseMeta databaseMeta, PreparedStatement preparedStatement, int index, Object data) throws KettleDatabaseException {

        try {

            // Postgis
            if (databaseMeta.getDatabaseInterface() instanceof PostgreSQLDatabaseMeta) {

                Geometry geometry = getGeometry(data);

                if (geometry != null) {

                    String wkt = null;

                    if (GeometryUtils.getCoordinateDimension(geometry) == 3) {
                        wkt = new WKTWriter(3).write(geometry);
                    } else {
                        wkt = new WKTWriter(2).write(geometry);
                    }

                    if (geometry.getSRID() > 0) {
                        wkt = "SRID=" + geometry.getSRID() + ";" + wkt;
                    }

                    PGgeometryLW pgGeom = new PGgeometryLW(wkt);
                    preparedStatement.setObject(index, pgGeom, Types.OTHER);

                } else {

                    preparedStatement.setObject(index, null, Types.OTHER);

                }

                // Oracle Spatial/Locator
            } else if (databaseMeta.getDatabaseInterface() instanceof OracleDatabaseMeta) {

                Geometry geometry = getGeometry(data);

                if (geometry != null) {

                    String wkt = null;

                    // TODO : gerer la 3D sans passer par du WKT
                    /*
                     * Type Code 2D Code 3D POINT 2001 3001 LINE 2002 3002
                     * POLYGON 2003 3003 COLLECTION 2004 3004 MULTIPOINT 2005
                     * 3005 MULTILINE 2006 3006 MULTIPOLYGON 2007 3007
                     */

                    if (GeometryUtils.getCoordinateDimension(geometry) == 3) {
                        throw new KettleDatabaseException(toStringMeta() + " : Unable to set Geometry 3D on prepared statement on index " + index);
                    } else {
                        wkt = new WKTWriter(2).write(geometry);
                    }

                    JGeometry ociGeometry = ociWktReaderWriter.toJGeometry(wkt.getBytes());
                    if (geometry.getSRID() > 0) {
                        ociGeometry.setSRID(geometry.getSRID());
                    }
                    preparedStatement.setObject(index, JGeometry.store(ociGeometry, preparedStatement.getConnection()), Types.STRUCT);

                } else {

                    preparedStatement.setObject(index, null, Types.STRUCT);

                }

                // Mysql
            } else if (databaseMeta.getDatabaseInterface() instanceof MySQLDatabaseMeta) {

                Geometry geometry = getGeometry(data);

                if (geometry != null) {

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] sridBytes = new byte[4];
                    ByteOrderValues.putInt(geometry.getSRID(), sridBytes, ByteOrderValues.LITTLE_ENDIAN);
                    outputStream.write(sridBytes);

                    WKBWriter writer = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN);
                    writer.write(geometry, new OutputStreamOutStream(outputStream));

                    preparedStatement.setBytes(index, outputStream.toByteArray());

                } else {

                    preparedStatement.setObject(index, null, Types.BINARY);

                }

            }

        } catch (Exception e) {
            throw new KettleDatabaseException(toStringMeta() + " : Unable to set Geometry on prepared statement on index " + index, e);
        }

    }

    @Override
    public int compare(Object data1, Object data2) throws KettleValueException {
        boolean n1 = isNull(data1);
        boolean n2 = isNull(data2);

        // null is always smaller!
        if (n1 && !n2) {
            return -1;
        }
        if (!n1 && n2) {
            return 1;
        }
        if (n1 && n2) {
            return 0;
        }

        int cmp = 0;
        switch (getType()) {
        case ValueMetaInterface.TYPE_STRING:
            String one = getString(data1);
            String two = getString(data2);

            if (caseInsensitive) {
                cmp = one.compareToIgnoreCase(two);
            } else {
                cmp = one.compareTo(two);
            }
            break;

        case ValueMetaInterface.TYPE_INTEGER:
            cmp = getInteger(data1).compareTo(getInteger(data2));
            break;

        case ValueMetaInterface.TYPE_NUMBER:
            cmp = Double.compare(getNumber(data1).doubleValue(), getNumber(data2).doubleValue());
            break;

        case ValueMetaInterface.TYPE_DATE:
            cmp = Long.valueOf(getDate(data1).getTime()).compareTo(Long.valueOf(getDate(data2).getTime()));
            break;

        case ValueMetaInterface.TYPE_BIGNUMBER:
            cmp = getBigNumber(data1).compareTo(getBigNumber(data2));
            break;

        case ValueMetaInterface.TYPE_BOOLEAN:
            if (getBoolean(data1).booleanValue() == getBoolean(data2).booleanValue()) {
                cmp = 0; // true == true, false == false
            } else if (getBoolean(data1).booleanValue() && !getBoolean(data2).booleanValue()) {
                cmp = 1; // true > false
            } else {
                cmp = -1; // false < true
            }
            break;

        case ValueMetaInterface.TYPE_BINARY:
            byte[] b1 = (byte[]) data1;
            byte[] b2 = (byte[]) data2;

            int length = b1.length < b2.length ? b1.length : b2.length;

            for (int i = 0; i < length; i++) {
                cmp = b1[i] - b2[i];
                if (cmp != 0) {
                    cmp = cmp < 0 ? -1 : 1;
                    break;
                }
            }

            cmp = b1.length - b2.length;

            break;

        case TYPE_GEOMETRY:
            String geom1 = getString(data1);
            String geom2 = getString(data2);

            if (caseInsensitive) {
                cmp = geom1.compareToIgnoreCase(geom2);
            } else {
                cmp = geom1.compareTo(geom2);
            }
            break;

        default:
            throw new KettleValueException(toString() + " : Comparing values can not be done with data type : " + getType());
        }

        if (isSortedDescending()) {
            return -cmp;
        } else {
            return cmp;
        }
    }

}
