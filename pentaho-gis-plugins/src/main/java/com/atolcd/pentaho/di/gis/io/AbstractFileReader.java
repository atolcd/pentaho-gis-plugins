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


import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.vfs.KettleVFS;

import com.atolcd.pentaho.di.gis.io.features.Field;

public abstract class AbstractFileReader implements FileReader {

    protected String layerName;
    protected String geometryFieldName;
    protected Charset charset;
    protected List<Field> fields;
    protected boolean forceToMultiGeometry;
    protected boolean forceTo2DGeometry;
    protected long limit;

    public AbstractFileReader(String layerName, String geometryFieldName, String charsetName) {

        this.layerName = layerName;
        this.geometryFieldName = geometryFieldName;
        this.fields = new ArrayList<Field>();
        try {
            this.charset = Charset.forName(charsetName);
        } catch (Exception e) {
            this.charset = Charset.defaultCharset();
        }
        this.forceToMultiGeometry = false;
        this.forceTo2DGeometry = false;
        this.limit = 0;

    }

    public Field getField(String name){
    	
    	for(Field field : this.fields){
    		
    		if(field.getName().equalsIgnoreCase(name)){
    			return field;
    		}
    	}
    	
    	return null;
        
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setForceToMultiGeometry(boolean forceToMultiGeometry) {
        this.forceToMultiGeometry = forceToMultiGeometry;
    }

    public void setForceTo2DGeometry(boolean forceTo2DGeometry) {
        this.forceTo2DGeometry = forceTo2DGeometry;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    // Vérification du nom de fichier
    protected URL checkFilename(String filename) throws KettleException {

        try {
            return KettleVFS.getFileObject(filename).getURL();

        } catch (IOException e) {

            throw new KettleException("", e);

        }

    }

    // Remplacement d'extension de fichier
    public static String replaceFileExtension(String fileName, String searchExtension, String replacementExtension) {
        return fileName.replaceFirst("(?i)(.*)" + searchExtension, "$1" + replacementExtension);
    }

}
