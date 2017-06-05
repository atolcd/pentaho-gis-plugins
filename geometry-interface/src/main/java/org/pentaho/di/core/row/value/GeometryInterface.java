package org.pentaho.di.core.row.value;

import com.vividsolutions.jts.geom.Geometry;
import org.pentaho.di.core.exception.KettleValueException;

/**
 * Created by Sudhanshu-Tango on 1/13/2017.
 */
public interface GeometryInterface {
  Geometry getGeometry(Object object) throws KettleValueException;
}
