package org.geotools.shapefile;

/**
 * Thrown when an attempt is made to load a shapefile which contains an error
 * such as an invalid shape
 */
@SuppressWarnings("serial")
public class InvalidShapefileException extends ShapefileException {
    public InvalidShapefileException(String s) {
        super(s);
    }
}
