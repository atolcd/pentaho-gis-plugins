package com.atolcd.gis.svg.type.style;

import com.atolcd.gis.svg.type.AbstractStyle;

public class ExternalStyle  extends AbstractStyle{

	private String href;
	
	public ExternalStyle(String href){
		this.href = href;
	}

	public String getHref() {
		return href;
	}


}
