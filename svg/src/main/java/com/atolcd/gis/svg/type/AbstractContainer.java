package com.atolcd.gis.svg.type;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractContainer extends AbstractElement{

	private List<AbstractElement> elements;
	
	public AbstractContainer() {
		this.elements = new ArrayList<AbstractElement>();
	}

	public List<AbstractElement> getElements() {
		return elements;
	}
	
	@SuppressWarnings("serial")
	public class SvgAbstractContainerException extends Exception {
		
	    public SvgAbstractContainerException(String message) {
	        super(message);
	    }
	    
	}

}
