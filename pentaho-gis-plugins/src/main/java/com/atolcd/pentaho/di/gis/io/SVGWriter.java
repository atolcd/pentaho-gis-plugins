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


import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.pentaho.di.core.exception.KettleException;

import com.atolcd.gis.svg.SvgUtil;
import com.atolcd.gis.svg.SvgWriter;
import com.atolcd.gis.svg.type.AbstractElement;
import com.atolcd.gis.svg.type.AbstractStyle;
import com.atolcd.gis.svg.type.Document;
import com.atolcd.gis.svg.type.container.Defs;
import com.atolcd.gis.svg.type.container.Group;
import com.atolcd.gis.svg.type.graphic.Circle;
import com.atolcd.gis.svg.type.graphic.Image;
import com.atolcd.gis.svg.type.graphic.Text;
import com.atolcd.gis.svg.type.graphic.Use;
import com.atolcd.gis.svg.type.style.EmbeddedStyle;
import com.atolcd.gis.svg.type.style.ExternalStyle;
import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Lineal;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.Puntal;
import com.vividsolutions.jts.geom.util.AffineTransformation;

public class SVGWriter extends AbstractFileWriter{


	public static String RESSOURCE_EXTERNAL = "RESSOURCE_EXTERNAL";
	public static String RESSOURCE_EMBEDDED = "RESSOURCE_EMBEDDED";
	public static int SYMBOL_DEFAULT_SIZE = 4;
	public static String SYMBOL_DEFAULT_ID = "symbolDefault";
	public static String GROUP_PREFIX = "layer_";
	
    private String svgFileName;
    private Writer writer;
    private boolean isServletOutput;
    
    private double height;
    private double width;
    private String documentTitle;
    private String documentDescription;
    private String styleSheet;
    private String styleSheetMode;
    private int precision;
    private String symbolMode;
        
    private String featureIdField;
    private String featureTitleField;
    private String featureDescriptionField;
    private String featureSvgStyleField;
    private String featureCssClassField;
    private String featureSymbolField;
    private String featureLabelField;
    private String featureGroupField;

    public SVGWriter(String fileName, String geometryFieldName, String charsetName) throws KettleException {

        super(geometryFieldName, charsetName);
        
        this.svgFileName = checkFilename(fileName).getFile();
        this.isServletOutput = false;
        this.writer = null;

        this.height = 0;
        this.width = 0;
        this.documentTitle = null;
        this.documentDescription = null;
        this.styleSheet = null;
        this.styleSheetMode = null;
        this.precision = 0;
        this.symbolMode = null;
                
        this.featureIdField = null;
        this.featureTitleField = null;
        this.featureDescriptionField = null;
        this.featureSvgStyleField = null;
        this.featureCssClassField = null;
        this.featureSymbolField = null;
        this.featureLabelField = null;
        this.featureGroupField = null;
        this.symbolMode = null;
        
    }

    public SVGWriter(Writer writer, String geometryFieldName, String charsetName) throws KettleException {

        super(geometryFieldName, charsetName);

        this.svgFileName = null;
        this.isServletOutput = true;
        this.writer = writer;
        
        this.height = 0;
        this.width = 0;
        this.documentTitle = null;
        this.documentDescription = null;
        this.styleSheet = null;
        this.styleSheetMode = null;
        
        this.featureIdField = null;
        this.featureTitleField = null;
        this.featureDescriptionField = null;
        this.featureSvgStyleField = null;
        this.featureCssClassField = null;
        this.featureSymbolField = null;
        this.featureLabelField = null;
        this.featureGroupField = null;
        
    }

	public String getFeatureLabelField() {
		return featureLabelField;
	}

	public void setFeatureLabelField(String featureLabelField) {
		this.featureLabelField = featureLabelField;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public Writer getWriter() {
		return writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public String getFeatureIdField() {
		return featureIdField;
	}

	public void setFeatureIdField(String featureIdField) {
		this.featureIdField = featureIdField;
	}

	public String getFeatureTitleField() {
		return featureTitleField;
	}

	public void setFeatureTitleField(String featureTitleField) {
		this.featureTitleField = featureTitleField;
	}

	public String getFeatureDescriptionField() {
		return featureDescriptionField;
	}

	public void setFeatureDescriptionField(String featureDescriptionField) {
		this.featureDescriptionField = featureDescriptionField;
	}

	public String getDocumentTitle() {
		return documentTitle;
	}

	public void setDocumentTitle(String documentTitle) {
		this.documentTitle = documentTitle;
	}

	public String getDocumentDescription() {
		return documentDescription;
	}

	public void setDocumentDescription(String documentDescription) {
		this.documentDescription = documentDescription;
	}

	public String getStyleSheet() {
		return styleSheet;
	}

	public void setStyleSheet(String styleSheet) {
		this.styleSheet = styleSheet;
	}

	public String getStyleSheetMode() {
		return styleSheetMode;
	}

	public void setStyleSheetMode(String styleSheetMode) {
		this.styleSheetMode = styleSheetMode;
	}

	public String getFeatureSvgStyleField() {
		return featureSvgStyleField;
	}

	public void setFeatureSvgStyleField(String featureSvgStyleField) {
		this.featureSvgStyleField = featureSvgStyleField;
	}

	public String getFeatureCssClassField() {
		return featureCssClassField;
	}

	public void setFeatureCssClassField(String featureCssClassField) {
		this.featureCssClassField = featureCssClassField;
	}

	public String getFeatureSymbolField() {
		return featureSymbolField;
	}

	public void setFeatureSymbolField(String featureSymbolField) {
		this.featureSymbolField = featureSymbolField;
	}

	public String getFeatureGroupField() {
		return featureGroupField;
	}

	public void setFeatureGroupField(String featureGroupField) {
		this.featureGroupField = featureGroupField;
	}

	public String getSymbolMode() {
		return symbolMode;
	}

	public void setSymbolMode(String symbolMode) {
		this.symbolMode = symbolMode;
	}

	private String getMD5(String chaine) throws UnsupportedEncodingException, NoSuchAlgorithmException{
		
		byte[] chaineBytes = chaine.getBytes("UTF-8");
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		byte[] hash = messageDigest.digest(chaineBytes);

		StringBuilder hexString = new StringBuilder();
		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xFF & hash[i]);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
		
	}

	public void writeFeatures(List<Feature> features) throws KettleException {

			
		//Nouveau SVG 
		Document svgDocument = new Document();
		svgDocument.setHeight(this.height);
		svgDocument.setWidth(this.width);
		svgDocument.setUnits(Document.SVG_UNIT_PX);
		
		//Si présence d'un titre et d'une description
		if (this.documentTitle != null && !this.documentTitle.isEmpty()) {
			svgDocument.setTitle(this.documentTitle);
        }
		
		if (this.documentDescription != null && !this.documentDescription.isEmpty()) {
			svgDocument.setDescription(this.documentDescription);
        }
		
		//Si présence d'une feuille de style
		if(this.styleSheet != null){
			
			AbstractStyle style = null;
		
			try{
				
				//Charge le contenu CSS ou établi un lien
				if(this.styleSheetMode != null && this.styleSheetMode.equalsIgnoreCase(SVGWriter.RESSOURCE_EMBEDDED)){
					style = new EmbeddedStyle(SvgUtil.toURL(this.styleSheet));
				
				}else{
					style = new ExternalStyle(this.styleSheet);
				}
			
				svgDocument.setStyle(style);
				
			}catch(Exception e){
				throw new KettleException(e.getMessage());
        	}
		
		}

        //Première passe :
        // - Récupération des champs de paramètres dynamiques
        // - Récupération de l'extent
        // - Création de groupe de features
        // - Référencement des images comme defs svg

        Field geometryField = null;
        Field idField = null;
        Field titleField = null;
        Field descriptionField = null;
        Field svgStyleField = null;
        Field cssClassField = null;
        Field symbolField = null;
        Field labelField = null;
        Field groupField = null;
        
        Envelope geoExtent = new Envelope();
        geoExtent.setToNull();
        
        HashMap<String,Image> symbols =  new HashMap<String,Image>();
        boolean hasPuntal = false;
        
        TreeMap<Long,List<Feature>> groups= new TreeMap<Long,List<Feature>>();
        groups.put(Long.MAX_VALUE, new ArrayList<Feature>());
        
        List<Text> labels =  new ArrayList<Text>();

        boolean first = true;
        for(Feature feature : features){
        	
        	if (first) {
    			
                geometryField = feature.getField(this.geometryFieldName);

                //id de feature
                if (this.featureIdField != null) {
                	idField = feature.getField(this.featureIdField);
                }
                
                if (this.featureTitleField != null) {
                    titleField = feature.getField(this.featureTitleField);
                }

                if (this.featureDescriptionField != null) {
                    descriptionField = feature.getField(this.featureDescriptionField);
                }
                
                if (this.featureSvgStyleField != null) {
                	svgStyleField = feature.getField(this.featureSvgStyleField);
                }
                
                if (this.featureCssClassField != null) {
                	cssClassField = feature.getField(this.featureCssClassField);
                }
                
                if (this.featureSymbolField != null) {
                	symbolField = feature.getField(this.featureSymbolField);
                }
                
                if (this.featureLabelField != null) {
                	labelField = feature.getField(this.featureLabelField);
                }
                
                if (this.featureGroupField != null) {
                	groupField = feature.getField(this.featureGroupField);
                }

                first = false;
                
            }
        	
        	Geometry geometry = (Geometry) feature.getValue(geometryField);
        	if(geometry instanceof Puntal && !hasPuntal){
        		hasPuntal = true;
        	}
        	
        	//Extent
    		if(geoExtent.isNull()){
    			geoExtent = geometry.getEnvelopeInternal();
    		}else{
    			geoExtent.expandToInclude(geometry.getEnvelopeInternal());
    		}
    		
    		//Si présence d'un champs de symbole
    		if(symbolField != null
    				&& feature.getValue(symbolField) != null
    				&& geometry instanceof Puntal){
    			
    			try{
    				
	    			URL imageURL = new URL((String)feature.getValue(symbolField));
	    			String symbolId = getMD5(imageURL.toExternalForm());

	    			if(!symbols.containsKey(symbolId)){
	    				
	    				boolean encodeImage = false;
	    				
	    				//Conversion en base 64
	    				if(this.symbolMode != null && this.symbolMode.equalsIgnoreCase(SVGWriter.RESSOURCE_EMBEDDED)){
	    					encodeImage = true;
	    				}
	    				
	    				Image image = SvgUtil.toSvgImage(imageURL, encodeImage);
	    				image.setId(symbolId);
	    				symbols.put(symbolId, image);
    				}

    			
	    		}catch(Exception e){
					throw new KettleException(e.getMessage() + feature.getValue(symbolField));
	        	}
    			
    		}
    		
    		//Groupe
    		if(groupField != null && feature.getValue(groupField) != null){
    			
    			long key = (Long)feature.getValue(groupField);
    			
    			//Création du group s'il n'existe pas
    			if(!groups.containsKey(key)){
    				groups.put(key, new ArrayList<Feature>());
    			}
    			
    			//Ajout au groupe
    			groups.get(key).add(feature);

    		//Absence de groupe
    		}else{
    			groups.get(Long.MAX_VALUE).add(feature);
    		}

    		
        }
        
        if(hasPuntal){
        	Defs defs = new Defs();
        	
        	//Symbole par défaut
        	Circle circle = new Circle(0,0,SYMBOL_DEFAULT_SIZE/2);
        	circle.setId(SYMBOL_DEFAULT_ID);
        	defs.getElements().add(circle);
        	
        	//Si symboles dynamiques
	        if(!symbols.isEmpty()){
	        	defs.getElements().addAll(symbols.values());
	        }
	        
	        svgDocument.getElements().add(defs);
	        
        }

        Envelope svgExtent = new Envelope(0,this.width,0,this.height);
        AffineTransformation svgTransformation = SvgUtil.getGeometryToSvgTransformation(geoExtent, svgExtent);

        //Bouclage sur les groupe de feature
        for (long key : groups.keySet()) {

        	Group groupElement = new Group();
    		groupElement.setId(GROUP_PREFIX + String.valueOf(key));
    
    		for(Feature feature : groups.get(key)){
            
	            //Récupération de la géométrie
	            Geometry geometry = (Geometry) feature.getValue(geometryField);
	            geometry = svgTransformation.transform(geometry);
	      	
	            //Alimention du SVG
	            AbstractElement element = null;
	            
	            if(geometry instanceof Puntal){
	         
	            	String symbolId = SYMBOL_DEFAULT_ID;
	            	if(symbolField != null && feature.getValue(symbolField) != null){
	            		
	            		try{
	            			
	            			URL imageURL = new URL((String)feature.getValue(symbolField));
	            			String md5 = getMD5(imageURL.toExternalForm());

			    			if(symbols.containsKey(md5)){
			    				symbolId = md5;
		    				}
	            			

			    		}catch(Exception e){
							throw new KettleException(e.getMessage() + feature.getValue(symbolField));
			        	}

	            	}
	            	
	            	double dX = 0;
	            	double dY = 0;
	            	
	         
	            	if(!symbolId.equalsIgnoreCase(SYMBOL_DEFAULT_ID)){
	            		Image image = symbols.get(symbolId);
	            		dX = image.getWidth()/2;
	            		dY = image.getHeight()/2;
	            	}
	            	
	            	Puntal puntal = (Puntal) GeometryUtils.getLessPrecisionGeometry(geometry, this.precision);;
	            	
	            	//Simple point
	            	if(geometry instanceof Point){
	            		
	            		try{
	            			
	            			element = new Use(((Point)puntal).getX() - dX, ((Point)puntal).getY() - dY, symbolId);
	            		
	            		}catch(Exception e){
							throw new KettleException(e.getMessage() + feature.getValue(symbolField));
			        	}

	            	
	            	//Multipoint
	            	}else{
	            		
	            		try{
	            					            
		            		element = new Group();
		            		for(int i = 0; i < geometry.getNumGeometries(); i++){
		            			
		            			Point point = (Point) geometry.getGeometryN(i);
		            			((Group)element).getElements().add(new Use(point.getX() - dX, point.getY() - dY, symbolId));
		            		}
		            		
	            		}catch(Exception e){
							throw new KettleException(e.getMessage() + feature.getValue(symbolField));
			        	}
	            		
	            	}
	            
	            }else if(geometry instanceof Lineal || geometry instanceof Polygonal){
	            	
	            	element = SvgUtil.toSvgPath(geometry,this.precision);
	 	            	
	            }else{
	            	
	            	throw new KettleException("Geometry " + GeometryUtils.getGeometryType(geometry) + " is not allowed");
	            	
	            }
		            
	            // Id de feature
	            if (idField != null) {
	                String id = (String) feature.getValue(idField);
	                if (id != null && !id.isEmpty()) {
	                	element.setId(id);
	                }

	            }
	            
	            // Titre de feature
	            if (titleField != null) {
	                String title = (String) feature.getValue(titleField);
	                if (title != null  && !title.isEmpty()) {
	                	element.setTitle(title);
	                }
	            }

	            // Description de feature
	            if (descriptionField != null) {
	                String description = (String) feature.getValue(descriptionField);
	                if (description != null  && !description.isEmpty()) {
	                	element.setDescription(description);
	                }
	            }
	            
	            // Style SVG de feature
	            if (svgStyleField != null) {
	                String style = (String) feature.getValue(svgStyleField);
	                if (style != null  && !style.isEmpty()) {
	                	element.setSvgStyle(style);
	                }
	            }
	            
	            // Css class de feature
	            if (cssClassField != null) {
	                String cssClass = (String) feature.getValue(cssClassField);
	                if (cssClass != null  && !cssClass.isEmpty()) {
	                	element.setCssClass(cssClass);
	                }
	            }

	            groupElement.getElements().add(element);
	            
	            //On ajoute le label à la liste globale : etiquettes doivent être dessinées en dernier
	            if (labelField != null) {

                	try{
                		
                		String textValue = (String) feature.getValue(labelField);
                		
                		if(textValue != null && !textValue.isEmpty()){
                			
	                		Text labelElement =  SvgUtil.toSvgText(geometry,textValue);
		                	
	                		if (featureCssClassField != null) {
		    	                String cssClass = (String) feature.getValue(cssClassField);
		    	                if (cssClass != null  && !cssClass.isEmpty()) {
		    	                	
		    	                	//class = nom classe associée à la feature suffixée de "_label"
		    	                	labelElement.setCssClass(cssClass + "_label");
		    	                }
		    	            }
		                	
		                	labels.add(labelElement);
                		}

                	}catch(Exception e){
    					throw new KettleException(e.getMessage());
    	        	}
	               
	            }

	        }
    		
    		//Si groupe max -> pas un groupe réel
    		//ajout de la feature directement dans la racine svg 
    		if(key == Long.MAX_VALUE){
    			
    			svgDocument.getElements().addAll(groupElement.getElements());
    		
    		//Sinon, ajoute le groupe au svg
        	}else{
        		svgDocument.getElements().add(groupElement);
        	}
        	
    		
        }
        
        //Ajout des labels
        if(!labels.isEmpty()){
        	Group groupElement = new Group();
    		groupElement.setId(GROUP_PREFIX + String.valueOf("Labels"));
    		groupElement.getElements().addAll(labels);
        	svgDocument.getElements().add(groupElement);
        }
        
        try{
        	
            //Ecriture du fichier ou du flux
        	SvgWriter svgWriter = new SvgWriter();
            if (isServletOutput) {

                if (features.size() > 0) {
                	svgWriter.write(svgDocument, writer, this.charset.name());
                }

            } else {

            	svgWriter.write(svgDocument, this.svgFileName, this.charset.name());

            }
         
		}catch(Exception e){
			throw new KettleException(e.getMessage());
		}
  
	}
	
}
