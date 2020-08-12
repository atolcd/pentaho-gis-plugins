package com.atolcd.gis.svg.type.container;

import java.net.URL;

import com.atolcd.gis.svg.type.AbstractContainer;

public class Link extends AbstractContainer{
	
	public static String SVG_XLINK_TARGET_NEW = "new";
	public static String SVG_XLINK_TARGET_REPLACE = "replace";
	public static String SVG_XLINK_TARGET_EMBED = "embed";
	public static String SVG_XLINK_TARGET_OTHER = "other";
	public static String SVG_XLINK_TARGET_NONE = "none";
	
	private URL href;
	private String target;
	
	public Link(URL href) throws SvgAbstractContainerException{
		this(href,Link.SVG_XLINK_TARGET_NEW);
	}
		
	public Link(URL href, String target) throws SvgAbstractContainerException{
		this.href = href;
		this.target = checkTarget(target);
	}

	public URL getHref() {
		return href;
	}

	public void setUrl(URL href) {
		this.href = href;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) throws SvgAbstractContainerException {
		this.target = checkTarget(target);
	}
		
	private String checkTarget(String target) throws SvgAbstractContainerException{
		
		if(target == null || (
				!target.equalsIgnoreCase(SVG_XLINK_TARGET_NEW)
				&& !target.equalsIgnoreCase(SVG_XLINK_TARGET_REPLACE)
				&& !target.equalsIgnoreCase(SVG_XLINK_TARGET_EMBED)
				&& !target.equalsIgnoreCase(SVG_XLINK_TARGET_OTHER)
				&& !target.equalsIgnoreCase(SVG_XLINK_TARGET_NONE)
			)
		){
			throw new SvgAbstractContainerException("The value \"" + target + "\" is not allowed for the link target attribute");
		}

		return target;
	}

}
