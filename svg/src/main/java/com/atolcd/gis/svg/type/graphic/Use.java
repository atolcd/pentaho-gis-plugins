package com.atolcd.gis.svg.type.graphic;

import java.net.URL;

import com.atolcd.gis.svg.type.AbstractGraphic;

public class Use extends AbstractGraphic{

	private double x;
	private double y;
	private URL href;
	private String reference;
	
	public Use(double x, double y, String reference) throws SvgUseException {
		this.x = x;
		this.y = y;
		this.reference =  this.checkReference(reference);
		this.href = null;
	}
	
	public Use(double x, double y, URL href) throws SvgUseException {
		this.x = x;
		this.y = y;
		this.reference =  null;
		this.href = this.checkHref(href);
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public URL getHref() {
		return href;
	}

	public String getReference() {
		return reference;
	}

	public void setHref(URL href) throws SvgUseException {
		this.reference = null;
		this.href = this.checkHref(href);
	}

	public void setReference(String reference) throws SvgUseException {
		this.reference = this.checkReference(reference);
		this.href = null;
	}

	private URL checkHref(URL href) throws SvgUseException{
		
		if(href == null){
			throw new SvgUseException("Href should not be null");
		}

		return href;
	}
	
	private String checkReference(String reference) throws SvgUseException{
		
		if(reference == null){
			throw new SvgUseException("Reference to internal resource should not be null");
		}else{
			if(!reference.startsWith("#")){
				reference = "#".concat(reference);
			}
			
		}

		return reference;
	}
	

	@SuppressWarnings("serial")
	public class SvgUseException extends Exception {
		
	    public SvgUseException(String message) {
	        super(message);
	    }
	    
	}

}
