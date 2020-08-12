package com.atolcd.gis.svg.type.style;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import java.net.URLConnection;

import com.atolcd.gis.svg.type.AbstractStyle;

public class EmbeddedStyle extends AbstractStyle{

	private String css;
	
	public EmbeddedStyle(URL url) throws SvgStyleException {
		
		try {
			
			URLConnection connection = this.checkUrl(url).openConnection();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder stringBuilder = new StringBuilder();
			String inputLine;
			
			while ((inputLine = bufferedReader.readLine()) != null){
				stringBuilder.append(inputLine);
			}
			
			bufferedReader.close();
			this.css = stringBuilder.toString();
		
		} catch (IOException e) {
			throw new SvgStyleException(e.getMessage());
		}
	}
	
	public EmbeddedStyle(String css) {
		this.css = css;
	}

	public String getCss() {
		return css;
	}

	public void setCss(String css) {
		this.css = css;
	}

}
