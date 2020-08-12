package com.atolcd.gis.svg.type;


public abstract class AbstractGraphic extends AbstractElement{

	@SuppressWarnings("serial")
	public class SvgAbstractGraphicException extends Exception {
		
	    public SvgAbstractGraphicException(String message) {
	        super(message);
	    }
	}

}
