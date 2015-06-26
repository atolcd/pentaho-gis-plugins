package org.sqlite.spatialite.io;

/**
 * SpatiaLite internally stores geometry values using ordinary SQLite's BLOB
 * columns in a format that is very closely related to WKB format, but not
 * exactly identical.
 * 
 * The main rationale to adopt this modified WKB format was initially based on
 * absence of any Spatial Index on earlier versions, and is still now preserved
 * so to avoid any unpleasant cross-version compatibility issue.
 * 
 * Any SpatiaLite's BLOB-Geometry includes an explicitly defined MBR; this helps
 * a lot in order to grant a quick access to entities selected on a spatial
 * basis, even when no Spatial Index is available.
 * 
 * The second good reason to use a purposely encoded format is needing to be
 * sure that a generic BLOB value really corresponds to a valid SpatiaLite
 * GEOMETRY; remember that one of SQLite specific features is to offer a very
 * weak column-type enforcement.
 * 
 * So SpatiaLite simply relies on ordinary SQLite general support to check that
 * one column value contains a generic BLOB, and then implements on its own any
 * further check if this may be considered as a valid GEOMETRY value.
 * 
 * To do this, SpatiaLite inserts some special markers at predictable and
 * strategic positions.
 * 
 * BLOB layout :
 * 
 * 0x00 : START MARKER (0x00) 0x01 : BYTE ORDER (0x00 or 0x01) 0x02 : SRID, 4
 * bytes (32-bits integer) 0x06 : MBR minx, 8 bytes (64-bits double value) 0x14
 * : MBR miny, 8 bytes (64-bits double value) 0x22 : MBR maxx, 8 bytes (64-bits
 * double value) 0x30 : MBR maxy, 8 bytes (64-bits double value) 0x38 : END OF
 * MBR MARKER (0x7c) 0x39 : GEOMETRY TYPE, 4 bytes (32-bits integer) 0x43 :
 * geometry data (WKB like) ... : END OF RECORD MARKER (0xfe)
 *
 */
public interface GeometryBlobConstants {

    byte START = 0x00;
    byte LAST = (byte) 0xFE;
    byte BIG_ENDIAN = 0x00;
    byte LITTLE_ENDIAN = 0x01;
    byte MBR_END = 0x7C;
    int Z_OFFSET = 1000;
    int M_OFFSET = 2000;
    int ZM_OFFSET = 3000;
    int COMPRESSED_OFFSET = 1000000;
    byte GEOMETRY_ENTITY = 0x69;

}
