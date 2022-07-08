package com.atolcd.pentaho.di.gis.io;

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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.core.exception.KettleException;

import com.atolcd.gis.gpx.AbstractReaderWriter;
import com.atolcd.gis.gpx.type.Route;
import com.atolcd.gis.gpx.type.Track;
import com.atolcd.gis.gpx.type.WayPoint;
import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Geometry;

public class GPXReader extends AbstractFileReader {

    private String gpxFileName;
    private boolean gpxFileExist;

    public GPXReader(String fileName, String geometryFieldName, String charsetName)
            throws KettleException {
        super(null, geometryFieldName, charsetName);

        try {
            this.gpxFileExist = new File(checkFilename(fileName)).exists();

            if (!this.gpxFileExist) {
                throw new KettleException("Missing " + fileName + " file");
            } else {
                this.gpxFileName = checkFilename(fileName);
            }

            this.fields.add(new Field(geometryFieldName, FieldType.GEOMETRY, null, null));
            this.fields.add(new Field("type", FieldType.STRING, null, null));
            this.fields.add(new Field("name", FieldType.STRING, null, null));
            this.fields.add(new Field("description", FieldType.STRING, null, null));

        } catch (Exception e) {
            throw new KettleException("Error initialize reader", e);
        }
    }

    public List<Feature> getFeatures() throws KettleException {
        List<Feature> features = new ArrayList<Feature>();

        try {
            
        	com.atolcd.gis.gpx.type.Document gpxDocument = new com.atolcd.gis.gpx.GpxReader().read(gpxFileName, this.charset.displayName());

        	List<WayPoint> wayPoints = gpxDocument.getWayPoints();
        	List<Route> routes = gpxDocument.getRoutes();
        	List<Track> tracks = gpxDocument.getTracks();
        	
            int entitiesTotalNumber = wayPoints.size() + routes.size() + tracks.size();

            if (this.limit == 0 || this.limit > entitiesTotalNumber || this.limit < 0) {
                this.limit = entitiesTotalNumber;
            }

            int entityNumber = 0;

            //WPT
            for (WayPoint wayPoint : wayPoints) {
                if (entityNumber < this.limit) {

                	Feature feature = new Feature();
                	if (this.forceTo2DGeometry) {
                		 feature.addValue(this.fields.get(0),GeometryUtils.get2DGeometry(wayPoint.getGeometry()));
            		}else{
            			feature.addValue(this.fields.get(0), wayPoint.getGeometry());
            		}
                    feature.addValue(this.fields.get(1), AbstractReaderWriter.GPX_TAG_WPT);
                    feature.addValue(this.fields.get(2), wayPoint.getName());
                    feature.addValue(this.fields.get(3), wayPoint.getDescription());

                    features.add(feature);
                    entityNumber ++;
                } else {
                    break;
                }
            }
            
            //RTE
            for (Route route : routes) {
                if (entityNumber < this.limit) {
                	
                    Feature feature = new Feature();
                    if (this.forceTo2DGeometry) {
               		 	feature.addValue(this.fields.get(0),GeometryUtils.get2DGeometry(route.getGeometry()));
	           		}else{
	           			feature.addValue(this.fields.get(0), route.getGeometry());
	           		}
                    feature.addValue(this.fields.get(1), AbstractReaderWriter.GPX_TAG_RTE);
                    feature.addValue(this.fields.get(2), route.getName());
                    feature.addValue(this.fields.get(3), route.getDescription());

                    features.add(feature);
                    entityNumber++;
                } else {
                    break;
                }
            }
            
            //TRK
            for (Track track : tracks) {
                if (entityNumber < this.limit) {
                	
                    Feature feature = new Feature();
                    Geometry geometry = GeometryUtils.getNonEmptyGeometry(4326,GeometryUtils.getMultiGeometry(track.getGeometry()));
                    if (this.forceTo2DGeometry) {
               		 	feature.addValue(this.fields.get(0),GeometryUtils.get2DGeometry(geometry));
	           		}else{
	           			feature.addValue(this.fields.get(0),geometry);
	           		}

                    feature.addValue(this.fields.get(1), AbstractReaderWriter.GPX_TAG_TRK);
                    feature.addValue(this.fields.get(2), track.getName());
                    feature.addValue(this.fields.get(3), track.getDescription());

                    features.add(feature);
                    entityNumber++;
                } else {
                    break;
                }
            }

        } catch (Exception e) {
            throw new KettleException("Error reading features" + this.gpxFileName, e);
        }
        return features;
    }
}
