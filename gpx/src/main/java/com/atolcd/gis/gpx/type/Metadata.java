package com.atolcd.gis.gpx.type;

import java.util.GregorianCalendar;

import com.atolcd.gis.gpx.IElement;
import com.vividsolutions.jts.geom.Envelope;


public class Metadata extends AbstractElement implements IElement{
	
	private Author author;
	private GregorianCalendar time;
	private String keywords;
	private Envelope bounds;
	
	public Author getAuthor() {
		return author;
	}
	
	public void setAuthor(Author author) {
		this.author = author;
	}
	
	public GregorianCalendar getTime() {
		return time;
	}
	
	public void setTime(GregorianCalendar time) {
		this.time = time;
	}
	
	public String getKeywords() {
		return keywords;
	}
	
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	
	public Envelope getBounds() {
		return bounds;
	}
	
	public void setBounds(Envelope bounds) {
		this.bounds = bounds;
	}

}
