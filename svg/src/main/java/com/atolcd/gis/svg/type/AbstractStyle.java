package com.atolcd.gis.svg.type;

import java.net.URL;

public class AbstractStyle {

	protected URL checkUrl(URL url) throws SvgStyleException{
		
		if(url == null){
			throw new SvgStyleException("Url should not be null");
		}

		return url;
	}
	
	@SuppressWarnings("serial")
	public class SvgStyleException extends Exception {
		
	    public SvgStyleException(String message) {
	        super(message);
	    }
	    
	}

}
