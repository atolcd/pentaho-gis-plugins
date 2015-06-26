package org.geotools.shapefile;

/**
 * Thrown when an attempt is made to load a shapefile which contains a shape
 * type that is not supported by the loader
 */
@SuppressWarnings("serial")
public class ShapeTypeNotSupportedException extends ShapefileException {
    public ShapeTypeNotSupportedException(String s) {
        super(s);
    }
}