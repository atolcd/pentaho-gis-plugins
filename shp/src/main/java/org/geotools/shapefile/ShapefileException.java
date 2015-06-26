package org.geotools.shapefile;

/**
 * Thrown when an error relating to the shapefile occurs
 */
@SuppressWarnings("serial")
public class ShapefileException extends Exception {
    public ShapefileException() {
        super();
    }

    public ShapefileException(String s) {
        super(s);
    }
}
