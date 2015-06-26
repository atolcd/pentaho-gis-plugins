package org.geotools.dbffile;

/**
 * Thrown when an error relating to the shapefile occures
 */
@SuppressWarnings("serial")
public class DbfFileException extends Exception {
    public DbfFileException(String s) {
        super(s);
    }
}
