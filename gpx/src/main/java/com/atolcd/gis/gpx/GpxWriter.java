package com.atolcd.gis.gpx;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.atolcd.gis.gpx.type.Document;
import com.atolcd.gis.gpx.type.Metadata;
import com.atolcd.gis.gpx.type.Route;
import com.atolcd.gis.gpx.type.Track;
import com.atolcd.gis.gpx.type.TrackSegment;
import com.atolcd.gis.gpx.type.WayPoint;
import com.vividsolutions.jts.geom.Envelope;

public class GpxWriter extends AbstractReaderWriter{
	
	public void write(Document gpxDocument, Writer writer, String charsetName) throws IOException, DatatypeConfigurationException{
		
		org.jdom2.Document document = getGpxDocument(gpxDocument);
		Format format = Format.getPrettyFormat();
	    format.setEncoding(charsetName);
		XMLOutputter xmlOutputter = new XMLOutputter(format);
		xmlOutputter.output(document, writer);
		writer.close();
		
	}
	
	public void write(Document gpxDocument, String filename, String charsetName) throws FileNotFoundException, IOException, DatatypeConfigurationException{
		
		org.jdom2.Document document = getGpxDocument(gpxDocument);
		Format format = Format.getPrettyFormat();
	    format.setEncoding(charsetName);
		XMLOutputter xmlOutputter = new XMLOutputter(format);
		FileOutputStream fileOutputStream = new FileOutputStream(filename);
		xmlOutputter.output(document, fileOutputStream);
		fileOutputStream.close();
		
	}


	private org.jdom2.Document getGpxDocument(Document gpxDocument) throws FileNotFoundException, IOException, DatatypeConfigurationException{
		
		//Namespace
		Namespace gpxNameSpace = null;
		if(gpxDocument.getVersion().equals(Document.GPX_VERSION_1_0)){
			gpxNameSpace = GPX_NS_1_0;
		}else{
			gpxNameSpace = GPX_NS_1_1;
		}
		
		//gpx
		Element gpxElt =  new Element(GPX_TAG,gpxNameSpace);
		gpxElt.setAttribute(GPX_TAG_ATT_VERSION, gpxDocument.getVersion());
		gpxElt.setAttribute(GPX_TAG_ATT_CREATOR, gpxDocument.getCreator());
				
		//metadata
		Metadata metadata = gpxDocument.getMetadata();
		if(metadata != null){

			Element parentMetadataElt = null;
			
			Element authorElt = null;
			Element emailElt = null;
							
			if(gpxDocument.getVersion().equals(Document.GPX_VERSION_1_0)){
				parentMetadataElt = gpxElt;
				
				//Author
				if(metadata.getAuthor()!=null && metadata.getAuthor().getName()!=null){
					authorElt = new Element(GPX_TAG_AUTHOR,gpxNameSpace);
					authorElt.setText(metadata.getAuthor().getName());
				}
				
				if(metadata.getAuthor()!=null && metadata.getAuthor().getEmail()!=null){
					emailElt = new Element(GPX_TAG_EMAIL,gpxNameSpace);
					emailElt.setText(metadata.getAuthor().getEmail());
				}
				
			}else{
				
				parentMetadataElt = new Element(GPX_TAG_METADATA,gpxNameSpace);
				
				//Author
				if(metadata.getAuthor()!=null){
					
					authorElt = new Element(GPX_TAG_AUTHOR,gpxNameSpace);
					authorElt = addToElt(authorElt, GPX_TAG_NAME, metadata.getAuthor().getName(),gpxNameSpace);
					if(metadata.getAuthor().getEmail()!=null){
						
						Element authorEmailElt = new Element(GPX_TAG_EMAIL,gpxNameSpace);
						authorEmailElt.setAttribute(GPX_TAG_EMAIL_ATT_ID,metadata.getAuthor().getEmailId());
						authorEmailElt.setAttribute(GPX_TAG_EMAIL_ATT_DOMAIN,metadata.getAuthor().getEmailDomain());
						authorElt.addContent(authorEmailElt);
					}
				}
			}
			
			parentMetadataElt = addToElt(parentMetadataElt, GPX_TAG_NAME, metadata.getName(),gpxNameSpace);
			parentMetadataElt = addToElt(parentMetadataElt, GPX_TAG_DESC, metadata.getDescription(),gpxNameSpace);
			if(authorElt !=null){
				parentMetadataElt.addContent(authorElt);
			}
			
			//uniquement 1.0
			if(emailElt !=null){
				parentMetadataElt.addContent(emailElt);
			}
			
			parentMetadataElt = addToElt(parentMetadataElt, GPX_TAG_TIME, metadata.getTime(),gpxNameSpace);
			parentMetadataElt = addToElt(parentMetadataElt, GPX_TAG_KEYWORDS, metadata.getKeywords(),gpxNameSpace);
		
			Envelope bounds = metadata.getBounds();
			if(metadata.getBounds() == null){
				bounds = gpxDocument.getExtent();
			}
			
			if(bounds !=null && !bounds.isNull()){
				
				Element boundsElt = new Element(GPX_TAG_BOUNDS,gpxNameSpace);
				boundsElt.setAttribute(GPX_TAG_BOUNDS_ATT_MINLON,String.valueOf(bounds.getMinX()));
				boundsElt.setAttribute(GPX_TAG_BOUNDS_ATT_MAXLON,String.valueOf(bounds.getMaxX()));
				boundsElt.setAttribute(GPX_TAG_BOUNDS_ATT_MINLAT,String.valueOf(bounds.getMinY()));
				boundsElt.setAttribute(GPX_TAG_BOUNDS_ATT_MAXLAT,String.valueOf(bounds.getMaxY()));
				parentMetadataElt.addContent(boundsElt);
			}
			
			if(gpxDocument.getVersion().equals(Document.GPX_VERSION_1_0)){
				gpxElt = parentMetadataElt;
			}else{
				gpxElt.addContent(parentMetadataElt);
			}

		}
		
		//waypoints
		if(gpxDocument.getWayPoints() != null
				&& gpxDocument.getWayPoints().size() > 0){
			
			for(WayPoint wayPoint : gpxDocument.getWayPoints()){
				gpxElt.addContent(toPtElt(wayPoint,GPX_TAG_WPT,gpxNameSpace));
			}
		}
		
		//routes
		if(gpxDocument.getRoutes() != null
				&& gpxDocument.getRoutes().size() > 0){
			
			for(Route route : gpxDocument.getRoutes()){
				
				Element rteElt =  new Element(GPX_TAG_RTE,gpxNameSpace);
				rteElt = addToElt(rteElt, GPX_TAG_NAME, route.getName(),gpxNameSpace);
				rteElt = addToElt(rteElt, GPX_TAG_CMT, route.getComment(),gpxNameSpace);
				rteElt = addToElt(rteElt, GPX_TAG_DESC, route.getDescription(),gpxNameSpace);
				rteElt = addToElt(rteElt, GPX_TAG_SRC, route.getSource(),gpxNameSpace);
				rteElt = addToElt(rteElt, GPX_TAG_NUMBER, route.getNumber(),gpxNameSpace);
				
				if(gpxDocument.getVersion().equals(Document.GPX_VERSION_1_1)){
					rteElt = addToElt(rteElt, GPX_TAG_TYPE, route.getType(),gpxNameSpace);
				}
				
				if(route.getPoints() != null
						&& route.getPoints().size() > 0){
					
					for(WayPoint wayPoint : route.getPoints()){
						rteElt.addContent(toPtElt(wayPoint,GPX_TAG_RTEPT,gpxNameSpace));
					}
				}
				
				gpxElt.addContent(rteElt);
			}
			
		}
		
		//tracks
		if(gpxDocument.getTracks() != null
				&& gpxDocument.getTracks().size() > 0){

			for(Track track : gpxDocument.getTracks()){
				
				Element trkElt =  new Element(GPX_TAG_TRK,gpxNameSpace);
				trkElt = addToElt(trkElt, GPX_TAG_NAME, track.getName(),gpxNameSpace);
				trkElt = addToElt(trkElt, GPX_TAG_CMT, track.getComment(),gpxNameSpace);
				trkElt = addToElt(trkElt, GPX_TAG_DESC, track.getDescription(),gpxNameSpace);
				trkElt = addToElt(trkElt, GPX_TAG_SRC, track.getSource(),gpxNameSpace);
				trkElt = addToElt(trkElt, GPX_TAG_NUMBER, track.getNumber(),gpxNameSpace);
				
				if(gpxDocument.getVersion().equals(Document.GPX_VERSION_1_1)){
					trkElt = addToElt(trkElt, GPX_TAG_TYPE, track.getType(),gpxNameSpace);
				}
				
				if(track.getSegments() != null
						&& track.getSegments().size() > 0){
				
					for(TrackSegment trackSegment : track.getSegments()){
						
						Element trksegElt =  new Element(GPX_TAG_TRKSEG,gpxNameSpace);
						
						if(trackSegment.getPoints() != null
								&& trackSegment.getPoints().size() > 0){
							
							for(WayPoint wayPoint : trackSegment.getPoints()){
								trksegElt.addContent(toPtElt(wayPoint,GPX_TAG_TRKPT,gpxNameSpace));
							}
						}
						
						trkElt.addContent(trksegElt);
						
					}
						
				}
				
				gpxElt.addContent(trkElt);

			}

		}
		
		return new org.jdom2.Document(gpxElt);
	}

	private Element addToElt(Element parent, String childName, Object childValue, Namespace namespace) throws DatatypeConfigurationException{
		
		if(childValue != null){
			
			String stringValue = null;
			
			if(childValue instanceof GregorianCalendar){
				
				stringValue =  DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar)childValue).toString();
				
			}else{
				stringValue = String.valueOf(childValue);
			}
			
			Element childElt = new Element(childName,namespace);
			childElt.setText(stringValue);
			parent.addContent(childElt);

		}
		
		return parent;

	}
	
	private Element toPtElt(WayPoint waypoint, String ptEltName, Namespace namespace) throws DatatypeConfigurationException{
		
		Element ptElt = new Element(ptEltName,namespace);
		ptElt.setAttribute(GPX_TAG_PT_ATT_LON, String.valueOf(waypoint.getLongitude()));
		ptElt.setAttribute(GPX_TAG_PT_ATT_LAT, String.valueOf(waypoint.getLatitude()));

		if(waypoint.getElevation() != null && !waypoint.getElevation().isNaN()){
			ptElt = addToElt(ptElt,GPX_TAG_ELE,waypoint.getElevation(),namespace);
		}
		
		ptElt = addToElt(ptElt, GPX_TAG_TIME, waypoint.getTime(),namespace);
		ptElt = addToElt(ptElt, GPX_TAG_NAME, waypoint.getName(),namespace);
		ptElt = addToElt(ptElt, GPX_TAG_CMT, waypoint.getComment(),namespace);
		ptElt = addToElt(ptElt, GPX_TAG_DESC, waypoint.getDescription(),namespace);
		ptElt = addToElt(ptElt, GPX_TAG_SRC, waypoint.getSource(),namespace);
		ptElt = addToElt(ptElt, GPX_TAG_SYM, waypoint.getSymbol(),namespace);
		ptElt = addToElt(ptElt, GPX_TAG_TYPE, waypoint.getType(),namespace);
		
		return ptElt;

	}

}
