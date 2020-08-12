package com.atolcd.gis.svg.type.graphic;

import com.atolcd.gis.svg.type.AbstractGraphic;

public class Path  extends AbstractGraphic{

	private String data;

	public Path(String data) {
		this.data = data;
	}

	public String getData() {
		return data;
	}

}
