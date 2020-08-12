package com.atolcd.gis.gpx;

import org.jdom2.Namespace;

public abstract class AbstractReaderWriter {
	
	protected static Namespace GPX_NS_1_1 = Namespace.getNamespace("http://www.topografix.com/GPX/1/1");
	protected static Namespace GPX_NS_1_0 = Namespace.getNamespace("http://www.topografix.com/GPX/1/0");
	
	protected static String GPX_TAG						= "gpx";
	protected static String GPX_TAG_ATT_VERSION			= "version";
	protected static String GPX_TAG_ATT_CREATOR			= "creator";
	
	protected static String GPX_TAG_METADATA			= "metadata";
	protected static String GPX_TAG_NAME 				= "name";
	protected static String GPX_TAG_CMT 				= "cmt";
	protected static String GPX_TAG_DESC 				= "desc";
	protected static String GPX_TAG_SRC 				= "src";
	protected static String GPX_TAG_TIME 				= "time";
	protected static String GPX_TAG_KEYWORDS 			= "keywords";
	protected static String GPX_TAG_ELE 				= "ele";
	protected static String GPX_TAG_SYM 				= "sym";
	protected static String GPX_TAG_TYPE 				= "type";
	protected static String GPX_TAG_NUMBER 				= "number";
	
	protected static String GPX_TAG_AUTHOR				= "author";
	protected static String GPX_TAG_EMAIL				= "email";
	protected static String GPX_TAG_EMAIL_ATT_ID		= "id";
	protected static String GPX_TAG_EMAIL_ATT_DOMAIN	= "domain";
	protected static String GPX_TAG_BOUNDS 				= "bounds";
	protected static String GPX_TAG_BOUNDS_ATT_MINLON	= "minlon";
	protected static String GPX_TAG_BOUNDS_ATT_MAXLON	= "maxlon";
	protected static String GPX_TAG_BOUNDS_ATT_MINLAT	= "minlat";
	protected static String GPX_TAG_BOUNDS_ATT_MAXLAT	= "maxlat";
	
	public static String GPX_TAG_TRK					= "trk";
	protected static String GPX_TAG_TRKSEG				= "trkseg";
	public static String GPX_TAG_RTE					= "rte";
	public static String GPX_TAG_WPT					= "wpt";
	protected static String GPX_TAG_RTEPT				= "rtept";
	protected static String GPX_TAG_TRKPT				= "trkpt";
	protected static String GPX_TAG_PT_ATT_LON			= "lon";
	protected static String GPX_TAG_PT_ATT_LAT			= "lat";

}
