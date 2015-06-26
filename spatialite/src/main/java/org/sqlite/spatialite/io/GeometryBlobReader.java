package org.sqlite.spatialite.io;

import java.io.IOException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ByteArrayInStream;
import com.vividsolutions.jts.io.ByteOrderDataInStream;
import com.vividsolutions.jts.io.ByteOrderValues;
import com.vividsolutions.jts.io.InStream;
import com.vividsolutions.jts.io.WKBConstants;

public class GeometryBlobReader {

    private GeometryFactory factory = new GeometryFactory();

    @SuppressWarnings("unused")
    public Geometry read(byte[] bytes) throws IOException {

        InStream is = new ByteArrayInStream(bytes);
        ByteOrderDataInStream dis = new ByteOrderDataInStream(is);

        byte start = dis.readByte();

        assert (start == GeometryBlobConstants.START);

        byte byteOrder = dis.readByte();

        if (byteOrder == GeometryBlobConstants.BIG_ENDIAN) {
            dis.setOrder(ByteOrderValues.BIG_ENDIAN);
        } else if (byteOrder == GeometryBlobConstants.LITTLE_ENDIAN) {
            dis.setOrder(ByteOrderValues.LITTLE_ENDIAN);
        } else {
            throw new IOException("Unexpected byte order value at pos 0x01");
        }

        int srid = dis.readInt();

        double minx = dis.readDouble();
        double miny = dis.readDouble();
        double maxx = dis.readDouble();
        double maxy = dis.readDouble();

        byte mbr_end = dis.readByte();
        assert (mbr_end == GeometryBlobConstants.MBR_END);

        TypeInfo typeInfo = new TypeInfo(dis.readInt());
        Geometry geometry = readGeometry(dis, typeInfo.geometryType, typeInfo.hasZ, typeInfo.hasM, typeInfo.compressed);

        byte last = dis.readByte();
        assert (last == GeometryBlobConstants.LAST);

        geometry.setSRID(srid);

        return geometry;

    }

    private Geometry readGeometry(ByteOrderDataInStream dis, int geometryType, boolean hasZ, boolean hasM, boolean compressed) throws IOException {

        switch (geometryType) {

        case WKBConstants.wkbPoint:

            return readPoint(dis, hasZ, hasM, compressed);

        case WKBConstants.wkbLineString:

            return readLinestring(dis, hasZ, hasM, compressed);

        case WKBConstants.wkbPolygon:

            return readPolygon(dis, hasZ, hasM, compressed);

        case WKBConstants.wkbMultiPoint:

            return readMultiPoint(dis, hasZ, hasM, compressed);

        case WKBConstants.wkbMultiLineString:

            return readMultiLineString(dis, hasZ, hasM, compressed);

        case WKBConstants.wkbMultiPolygon:

            return readMultiPolygon(dis, hasZ, hasM, compressed);

        case WKBConstants.wkbGeometryCollection:

            return readGeometryCollection(dis, hasZ, hasM, compressed);

        default:
            throw new IOException();
        }

    }

    private GeometryCollection readGeometryCollection(ByteOrderDataInStream dis, boolean hasZ, boolean hasM, boolean compressed) throws IOException {

        int numberOfComponents = dis.readInt();
        Geometry[] components = new Geometry[numberOfComponents];

        for (int i = 0; i < numberOfComponents; i++) {
            byte marker = dis.readByte();
            assert (marker == GeometryBlobConstants.GEOMETRY_ENTITY);
            TypeInfo typeInfo = new TypeInfo(dis.readInt());
            components[i] = readGeometry(dis, typeInfo.geometryType, hasZ, hasM, compressed);
        }

        return factory.createGeometryCollection(components);
    }

    private MultiPolygon readMultiPolygon(ByteOrderDataInStream dis, boolean hasZ, boolean hasM, boolean compressed) throws IOException {

        int numberOfComponents = dis.readInt();
        Polygon[] components = new Polygon[numberOfComponents];

        for (int i = 0; i < numberOfComponents; i++) {
            byte marker = dis.readByte();
            assert (marker == GeometryBlobConstants.GEOMETRY_ENTITY);
            int type = dis.readInt();
            assert (type == WKBConstants.wkbPolygon);
            components[i] = readPolygon(dis, hasZ, hasM, compressed);
        }

        return factory.createMultiPolygon(components);

    }

    private MultiLineString readMultiLineString(ByteOrderDataInStream dis, boolean hasZ, boolean hasM, boolean compressed) throws IOException {

        int numberOfComponents = dis.readInt();
        LineString[] strings = new LineString[numberOfComponents];

        for (int i = 0; i < numberOfComponents; i++) {
            byte marker = dis.readByte();
            assert (marker == GeometryBlobConstants.GEOMETRY_ENTITY);
            int type = dis.readInt();
            assert (type == WKBConstants.wkbLineString);
            strings[i] = readLinestring(dis, hasZ, hasM, compressed);
        }

        return factory.createMultiLineString(strings);

    }

    private MultiPoint readMultiPoint(ByteOrderDataInStream dis, boolean hasZ, boolean hasM, boolean compressed) throws IOException {

        int numberOfComponents = dis.readInt();
        Point[] points = new Point[numberOfComponents];

        for (int i = 0; i < numberOfComponents; i++) {
            byte marker = dis.readByte();
            assert (marker == GeometryBlobConstants.GEOMETRY_ENTITY);
            int type = dis.readInt();
            assert (type == WKBConstants.wkbPoint);
            points[i] = readPoint(dis, hasZ, hasM, compressed);
        }

        return factory.createMultiPoint(points);

    }

    private Polygon readPolygon(ByteOrderDataInStream dis, boolean hasZ, boolean hasM, boolean compressed) throws IOException {

        int numberOfRings = dis.readInt();

        LineString outerlinestring = readLinestring(dis, hasZ, hasM, compressed);
        LinearRing shell = factory.createLinearRing(outerlinestring.getCoordinates());

        LinearRing[] holes = new LinearRing[numberOfRings - 1];

        for (int i = 0; i < numberOfRings - 1; i++) {
            LineString hole = readLinestring(dis, hasZ, hasM, compressed);
            holes[i] = factory.createLinearRing(hole.getCoordinates());
        }

        return factory.createPolygon(shell, holes);

    }

    private Point readPoint(ByteOrderDataInStream dis, boolean hasZ, boolean hasM, boolean compressed) throws IOException {

        if (compressed) {
            throw new IOException(new IllegalArgumentException("Point cannot be compressed."));
        }

        return factory.createPoint(readCoordinate(dis, hasZ, hasM));

    }

    private LineString readLinestring(ByteOrderDataInStream dis, boolean hasZ, boolean hasM, boolean compressed) throws IOException {

        int numberOfPoints = dis.readInt();
        Coordinate[] coordinates = new Coordinate[numberOfPoints];

        if (compressed) {

            coordinates[0] = readCoordinate(dis, hasZ, hasM);

            for (int i = 1; i < numberOfPoints - 1; i++) {
                coordinates[i] = readDiffCoordinate(dis, hasZ, hasM, coordinates[i - 1]);
            }

            coordinates[numberOfPoints - 1] = readCoordinate(dis, hasZ, hasM);

        } else {

            for (int i = 0; i < numberOfPoints; i++) {
                coordinates[i] = readCoordinate(dis, hasZ, hasM);
            }

        }

        return factory.createLineString(coordinates);

    }

    private Coordinate readDiffCoordinate(ByteOrderDataInStream dis, boolean hasZ, boolean hasM, Coordinate p0) throws IOException {

        int diffx = dis.readInt();
        int diffy = dis.readInt();

        float dx = Float.intBitsToFloat(diffx);
        float dy = Float.intBitsToFloat(diffy);

        Coordinate c = new Coordinate(p0);
        c.x += dx;
        c.y += dy;

        if (hasZ) {

            int diffz = dis.readInt();
            float dz = Float.intBitsToFloat(diffz);
            c.z += dz;

        }

        return c;
    }

    private Coordinate readCoordinate(ByteOrderDataInStream dis, boolean hasZ, boolean hasM) throws IOException {

        double x = dis.readDouble();
        double y = dis.readDouble();
        Coordinate coordinate = new Coordinate(x, y);

        if (hasZ) {

            double z = dis.readDouble();
            coordinate.z = z;

        }

        return coordinate;

    }

    class TypeInfo {

        int geometryType;
        boolean compressed = false;
        boolean hasZ = false;
        boolean hasM = false;

        public TypeInfo(int geometryType) {

            if (geometryType > GeometryBlobConstants.COMPRESSED_OFFSET) {
                compressed = true;
                geometryType -= GeometryBlobConstants.COMPRESSED_OFFSET;
            }

            if (geometryType > GeometryBlobConstants.ZM_OFFSET) {
                hasZ = true;
                hasM = true;
                geometryType -= GeometryBlobConstants.ZM_OFFSET;
            } else if (geometryType > GeometryBlobConstants.M_OFFSET) {
                hasM = true;
                geometryType -= GeometryBlobConstants.M_OFFSET;
            } else if (geometryType > GeometryBlobConstants.Z_OFFSET) {
                hasZ = true;
                geometryType -= GeometryBlobConstants.Z_OFFSET;
            }

            this.geometryType = geometryType;

        }

    }

}
