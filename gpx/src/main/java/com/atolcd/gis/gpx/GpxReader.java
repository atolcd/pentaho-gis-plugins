package com.atolcd.gis.gpx;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import com.atolcd.gis.gpx.type.Author;
import com.atolcd.gis.gpx.type.Author.AuthorException;
import com.atolcd.gis.gpx.type.Document;
import com.atolcd.gis.gpx.type.Route;
import com.atolcd.gis.gpx.type.Route.RouteException;
import com.atolcd.gis.gpx.type.Track;
import com.atolcd.gis.gpx.type.Track.TrackException;
import com.atolcd.gis.gpx.type.TrackSegment;
import com.atolcd.gis.gpx.type.WayPoint;
import com.atolcd.gis.gpx.type.Document.GpxException;
import com.atolcd.gis.gpx.type.Metadata;
import com.atolcd.gis.gpx.type.WayPoint.WayPointException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class GpxReader extends AbstractReaderWriter{
	
	public Document read(String filename, String charsetName) throws JDOMException, IOException, GpxException, WayPointException, RouteException, TrackException, AuthorException{
		
		Document gpxDocument = new Document();

		SAXBuilder sxb = new SAXBuilder();
		org.jdom2.Document document = sxb.build(new InputStreamReader(new FileInputStream(filename), charsetName));
		
		Element gpxElt = document.getRootElement();
		Namespace gpxNameSpace = null;
		
		//version & creator
		gpxDocument.setVersion(gpxElt.getAttributeValue(GPX_TAG_ATT_VERSION));
		gpxDocument.setCreator(gpxElt.getAttributeValue(GPX_TAG_ATT_CREATOR));
		
		//metadata
		Element parentMetadataElt = null;
		if(gpxDocument.getVersion().equals(Document.GPX_VERSION_1_0)){
			gpxNameSpace = GPX_NS_1_0;
		}else{
			gpxNameSpace = GPX_NS_1_1;
			parentMetadataElt = gpxElt.getChild(GPX_TAG_METADATA,gpxNameSpace);
		}

		if(parentMetadataElt != null){

			Metadata metadata = new Metadata();
			metadata.setName((String) getChildValue(parentMetadataElt, GPX_TAG_NAME,gpxNameSpace));
			metadata.setDescription((String) getChildValue(parentMetadataElt, GPX_TAG_DESC,gpxNameSpace));
			metadata.setTime((GregorianCalendar) getChildValue(parentMetadataElt, GPX_TAG_TIME,gpxNameSpace));
			
			Element authorElt = parentMetadataElt.getChild(GPX_TAG_AUTHOR,gpxNameSpace);
			String authorName = null;
			String authorEmail = null;
			if(authorElt != null){
	
				if(gpxDocument.getVersion().equals(Document.GPX_VERSION_1_0)){
					
					authorName =  (String) getChildValue(parentMetadataElt,GPX_TAG_AUTHOR,gpxNameSpace);
				
				}else{
					
					authorName = (String) getChildValue(authorElt, GPX_TAG_NAME,gpxNameSpace);
					Element authorEmailElt = authorElt.getChild(GPX_TAG_EMAIL,gpxNameSpace);
					if(authorEmailElt != null){
						authorEmail = authorEmailElt.getAttributeValue(GPX_TAG_EMAIL_ATT_ID)
						+ "@" + 
						authorEmailElt.getAttributeValue(GPX_TAG_EMAIL_ATT_DOMAIN);
					}

				}
			}
			
			if(gpxDocument.getVersion().equals(Document.GPX_VERSION_1_0)){
				authorEmail =  (String) getChildValue(parentMetadataElt,GPX_TAG_EMAIL,gpxNameSpace);
			}
			
			if(authorName != null || authorEmail != null){
				metadata.setAuthor(new Author(authorName, authorEmail));
			}
						
			metadata.setKeywords((String) getChildValue(parentMetadataElt, GPX_TAG_KEYWORDS,gpxNameSpace));
			Element boundsElt = parentMetadataElt.getChild(GPX_TAG_BOUNDS,gpxNameSpace);
			if(boundsElt != null){
				
				Envelope bounds = new Envelope();
				bounds.init(
						Double.parseDouble(boundsElt.getAttributeValue(GPX_TAG_BOUNDS_ATT_MINLON)),
						Double.parseDouble(boundsElt.getAttributeValue(GPX_TAG_BOUNDS_ATT_MAXLON)),
						Double.parseDouble(boundsElt.getAttributeValue(GPX_TAG_BOUNDS_ATT_MINLAT)),
						Double.parseDouble(boundsElt.getAttributeValue(GPX_TAG_BOUNDS_ATT_MAXLAT))
				);
				
				metadata.setBounds(bounds);
				
			}
			
			gpxDocument.setMetadata(metadata);
		}
		
		//waypoints
		List<Element> wptElts = gpxElt.getChildren(GPX_TAG_WPT,gpxNameSpace);
		for(Element wptElt : wptElts){
			gpxDocument.getWayPoints().add(toWayPoint(wptElt, gpxNameSpace));
		}
		
		//routes
		List<Element> rteElts = gpxElt.getChildren(GPX_TAG_RTE,gpxNameSpace);
		for(Element rteElt : rteElts){
			
			Route route = new Route();
			route.setComment((String) getChildValue(rteElt,GPX_TAG_CMT,gpxNameSpace));
			route.setDescription((String) getChildValue(rteElt,GPX_TAG_DESC,gpxNameSpace));
			route.setName((String) getChildValue(rteElt,GPX_TAG_NAME,gpxNameSpace));
			route.setNumber((Integer) getChildValue(rteElt,GPX_TAG_NUMBER,gpxNameSpace));
			route.setSource((String) getChildValue(rteElt,GPX_TAG_SRC,gpxNameSpace));
			route.setType((String) getChildValue(rteElt,GPX_TAG_TYPE,gpxNameSpace));
			
			List<Element> rteptElts = rteElt.getChildren(GPX_TAG_RTEPT,gpxNameSpace);
			for(Element rteptElt : rteptElts){
				route.getPoints().add(toWayPoint(rteptElt, gpxNameSpace));
			}
			
			gpxDocument.getRoutes().add(route);
		}
		
		//tracks
		List<Element> trkElts = gpxElt.getChildren(GPX_TAG_TRK,gpxNameSpace);
		for(Element trkElt : trkElts){
			
			Track track = new Track();
			track.setComment((String) getChildValue(trkElt,GPX_TAG_CMT,gpxNameSpace));
			track.setDescription((String) getChildValue(trkElt,GPX_TAG_DESC,gpxNameSpace));
			track.setName((String) getChildValue(trkElt,GPX_TAG_NAME,gpxNameSpace));
			track.setNumber((Integer) getChildValue(trkElt,GPX_TAG_NUMBER,gpxNameSpace));
			track.setSource((String) getChildValue(trkElt,GPX_TAG_SRC,gpxNameSpace));
			track.setType((String) getChildValue(trkElt,GPX_TAG_TYPE,gpxNameSpace));
			
			List<Element> trksegElts = trkElt.getChildren(GPX_TAG_TRKSEG,gpxNameSpace);
			for(Element trksegElt : trksegElts){
				
				TrackSegment trackSegment = new TrackSegment();
				List<Element> trkptElts = trksegElt.getChildren(GPX_TAG_TRKPT,gpxNameSpace);
				for(Element trkptElt : trkptElts){
					trackSegment.getPoints().add(toWayPoint(trkptElt, gpxNameSpace));
				}
				
				track.getSegments().add(trackSegment);
			}
			
			gpxDocument.getTracks().add(track);
		}

		
		return gpxDocument;
	}
	
	private WayPoint toWayPoint(Element ptElt, Namespace namespace) throws WayPointException{
		
		Coordinate coordinate = new Coordinate(
				Double.parseDouble(ptElt.getAttributeValue(GPX_TAG_PT_ATT_LON).replace(",", ".")),
				Double.parseDouble(ptElt.getAttributeValue(GPX_TAG_PT_ATT_LAT).replace(",", "."))
		);
		
		Object elevation = getChildValue(ptElt, GPX_TAG_ELE,namespace);
		if(elevation != null){
			coordinate.z = Double.parseDouble(elevation.toString());
		}

		WayPoint waypoint = new WayPoint(coordinate);
		waypoint.setTime((GregorianCalendar) getChildValue(ptElt, GPX_TAG_TIME,namespace));
		waypoint.setName((String) getChildValue(ptElt, GPX_TAG_NAME,namespace));
		waypoint.setComment((String) getChildValue(ptElt, GPX_TAG_CMT,namespace));
		waypoint.setDescription((String) getChildValue(ptElt, GPX_TAG_DESC,namespace));
		waypoint.setSource((String) getChildValue(ptElt, GPX_TAG_SRC,namespace));
		waypoint.setSymbol((String) getChildValue(ptElt, GPX_TAG_SYM,namespace));
		waypoint.setType((String) getChildValue(ptElt, GPX_TAG_TYPE,namespace));
		
		return waypoint;
	}
	
	private Object getChildValue(Element parent, String childName,Namespace namespace){
		
		Element childElt = parent.getChild(childName,namespace);
		if(childElt != null && childElt.getText() != null){
			
			String chiltText = childElt.getText();
			
			try{
				
				XMLGregorianCalendar xmlCalendar= DatatypeFactory.newInstance().newXMLGregorianCalendar(chiltText);
				return xmlCalendar.toGregorianCalendar();
			
			}catch(Exception e){}
			
			try{
				
				return Integer.parseInt(chiltText);
			
			}catch(Exception e){}
			
			try{
				
				return Double.parseDouble(chiltText.replace(",", "."));
			
			}catch(Exception e){}
						
			return childElt.getText();
		}
		
		return null;
		
	}

}

