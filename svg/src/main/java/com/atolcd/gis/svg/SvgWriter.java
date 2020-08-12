package com.atolcd.gis.svg;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;

import org.jdom2.CDATA;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.ProcessingInstruction;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.atolcd.gis.svg.type.AbstractContainer;
import com.atolcd.gis.svg.type.AbstractElement;
import com.atolcd.gis.svg.type.AbstractGraphic;
import com.atolcd.gis.svg.type.AbstractStyle;
import com.atolcd.gis.svg.type.Document;
import com.atolcd.gis.svg.type.container.Defs;
import com.atolcd.gis.svg.type.container.Group;
import com.atolcd.gis.svg.type.container.Link;
import com.atolcd.gis.svg.type.graphic.Circle;
import com.atolcd.gis.svg.type.graphic.Image;
import com.atolcd.gis.svg.type.graphic.Path;
import com.atolcd.gis.svg.type.graphic.Rectangle;
import com.atolcd.gis.svg.type.graphic.Text;
import com.atolcd.gis.svg.type.graphic.Use;
import com.atolcd.gis.svg.type.style.EmbeddedStyle;
import com.atolcd.gis.svg.type.style.ExternalStyle;

public class SvgWriter {
	
	private static Namespace SVG_NS = Namespace.getNamespace("http://www.w3.org/2000/svg");
	private static Namespace XLINK_NS = Namespace.getNamespace("xlink","http://www.w3.org/1999/xlink");
		
	private static String SVG_TAG_ATT_TYPE			= "type";
	
	private static String SVG_TAG					= "svg";
	private static String SVG_TAG_ATT_VERSION		= "version";
	private static String SVG_TAG_ATT_WIDTH			= "width";
	private static String SVG_TAG_ATT_HEIGHT		= "height";
	private static String SVG_TAG_ATT_VIEWBOX		= "viewBox";
	private static String SVG_TAG_ATT_ID			= "id";
	
	private static String SVG_TAG_ATT_STYLE			= "style";
	private static String SVG_TAG_ATT_CLASS			= "class";
	private static String SVG_TAG_ATT_TRANSFORM		= "transform";
	
	private static String SVG_TAG_ATT_X				= "x";
	private static String SVG_TAG_ATT_Y				= "y";
		
	private static String SVG_TAG_TITLE				= "title";
	private static String SVG_TAG_DESC				= "desc";

	private static String SVG_TAG_STYLE				= "style";
	private static String SVG_TAG_G					= "g";
	private static String SVG_TAG_A					= "a";
	private static String SVG_TAG_DEFS				= "defs";
	private static String SVG_TAG_USE				= "use";
	private static String SVG_TAG_IMAGE				= "image";
	private static String SVG_TAG_TEXT				= "text";
	
	private static String XLINK_TAG_ATT_HREF		= "href";
	private static String XLINK_TAG_ATT_SHOW		= "show";

	private static String SVG_TAG_CIRCLE			= "circle";
	private static String SVG_TAG_ATT_CX			= "cx";
	private static String SVG_TAG_ATT_CY			= "cy";
	private static String SVG_TAG_ATT_R				= "r";
	
	private static String SVG_TAG_RECT				= "rect";
	private static String SVG_TAG_ATT_RX			= "rx";
	private static String SVG_TAG_ATT_RY			= "ry";
	
	private static String SVG_TAG_PATH				= "path";
	private static String SVG_TAG_ATT_D				= "d";

	public void write(Document svgDocument, Writer writer, String charsetName) throws IOException{

		org.jdom2.Document document = getSvgDocument(svgDocument);
		Format format = Format.getPrettyFormat();
		format.setEncoding(charsetName);
		XMLOutputter xmlOutputter = new XMLOutputter(format);
		xmlOutputter.output(document, writer);
		writer.close();

	}

	public void write(Document svgDocument, String filename, String charsetName) throws FileNotFoundException, IOException{

		org.jdom2.Document document = getSvgDocument(svgDocument);
		Format format = Format.getPrettyFormat();
		format.setEncoding(charsetName);
		XMLOutputter xmlOutputter = new XMLOutputter(format);
		FileOutputStream fileOutputStream = new FileOutputStream(filename);
		xmlOutputter.output(document, fileOutputStream);
		fileOutputStream.close();

	}

	private org.jdom2.Document getSvgDocument(Document svgDocument) throws FileNotFoundException, IOException{

		//Svg
		Element svgElt =  new Element(SVG_TAG, SVG_NS);
		svgElt.addNamespaceDeclaration(XLINK_NS);
		org.jdom2.Document svgDoc = new org.jdom2.Document(svgElt);
		
		svgElt.setAttribute(SVG_TAG_ATT_VERSION, Document.SVG_VERSION_1_1);
		svgElt.setAttribute(SVG_TAG_ATT_WIDTH, svgDocument.getWidth()  + svgDocument.getUnits());
		svgElt.setAttribute(SVG_TAG_ATT_HEIGHT,svgDocument.getHeight() + svgDocument.getUnits());
		svgElt.setAttribute(SVG_TAG_ATT_VIEWBOX,"0 0 " + svgDocument.getWidth() + " " + svgDocument.getHeight());
		
		
		//Title
		if(svgDocument.getTitle() != null && !svgDocument.getTitle().isEmpty()){

			Element svgTitleElt =  new Element(SVG_TAG_TITLE,SVG_NS);
			svgTitleElt.setText(svgDocument.getTitle());
			svgElt.addContent(svgTitleElt);
		}

		//Description
		if(svgDocument.getDescription() != null && !svgDocument.getDescription().isEmpty()){

			Element svgDescriptionElt =  new Element(SVG_TAG_DESC,SVG_NS);
			svgDescriptionElt.setText(svgDocument.getDescription());
			svgElt.addContent(svgDescriptionElt);
		}
		
		
		if(svgDocument.getStyle() != null){
			
			AbstractStyle style = svgDocument.getStyle();
			
			//Style lie
			if(style instanceof ExternalStyle){
				
				ProcessingInstruction processingInstruction= new ProcessingInstruction(
						"xml-stylesheet",
						SVG_TAG_STYLE + "=\"" + SvgUtil.MIME_TYPE_CSS + "\"" + XLINK_TAG_ATT_HREF + "=\""
						+ ((ExternalStyle)style).getHref()+ "\"");
				svgDoc.addContent(0, processingInstruction);
			
			}else{
				
				Element styleElt =  new Element(SVG_TAG_STYLE, SVG_NS);
			    styleElt.setAttribute(SVG_TAG_ATT_TYPE,SvgUtil.MIME_TYPE_CSS);
			    styleElt.addContent(new CDATA(((EmbeddedStyle)style).getCss()));
				svgElt.addContent(styleElt);
				
			}

		}

		for(AbstractElement svgElement : svgDocument.getElements()){
			svgElt = addToSvgDocument(svgElt, svgElement);
		}

		return svgDoc;

	}

	private Element addToSvgDocument(Element parent, AbstractElement svgElement){
		
		Element element = null;
		
		if(svgElement instanceof AbstractContainer){
			
			//Simple groupe
			if (svgElement instanceof Group){
				
				element =  new Element(SVG_TAG_G, SVG_NS);

			//Lien
			}else if (svgElement instanceof Link){
				element =  new Element(SVG_TAG_A, SVG_NS);
				element.setAttribute(XLINK_TAG_ATT_HREF,((Link) svgElement).getHref().toExternalForm(), XLINK_NS);
				element.setAttribute(XLINK_TAG_ATT_SHOW, ((Link) svgElement).getTarget(), XLINK_NS);
		
			//Defs
			}else if (svgElement instanceof Defs){
				element =  new Element(SVG_TAG_DEFS, SVG_NS);
			}

			//Contenu du groupe
			for(AbstractElement svgSubElement : ((AbstractContainer) svgElement).getElements()){
				element = addToSvgDocument(element, svgSubElement);
			}

		}else if (svgElement instanceof AbstractGraphic){
			
			//Circle
			if (svgElement instanceof Circle){

				element =  new Element(SVG_TAG_CIRCLE, SVG_NS);
				element.setAttribute(SVG_TAG_ATT_CX, String.valueOf(((Circle) svgElement).getX()));
				element.setAttribute(SVG_TAG_ATT_CY, String.valueOf(((Circle) svgElement).getY()));
				element.setAttribute(SVG_TAG_ATT_R,  String.valueOf(((Circle) svgElement).getRadius()));
			
			//Rectangle
			}else if (svgElement instanceof Rectangle){

				element =  new Element(SVG_TAG_RECT, SVG_NS);
				element.setAttribute(SVG_TAG_ATT_X, String.valueOf(((Rectangle) svgElement).getX()));
				element.setAttribute(SVG_TAG_ATT_Y, String.valueOf(((Rectangle) svgElement).getY()));
				element.setAttribute(SVG_TAG_ATT_WIDTH, String.valueOf(((Rectangle) svgElement).getWidth()));
				element.setAttribute(SVG_TAG_ATT_HEIGHT, String.valueOf(((Rectangle) svgElement).getHeight()));

				double rx = ((Rectangle) svgElement).getXRadius();
				double ry = ((Rectangle) svgElement).getYRadius();

				if(!Double.isNaN(rx) && rx > 0){
					element.setAttribute(SVG_TAG_ATT_RX,  String.valueOf(rx));
				}

				if(!Double.isNaN(ry) && ry > 0){
					element.setAttribute(SVG_TAG_ATT_RY,  String.valueOf(ry));
				}

			//Path
			}else if (svgElement instanceof Path){

				element =  new Element(SVG_TAG_PATH, SVG_NS);
				element.setAttribute(SVG_TAG_ATT_D, String.valueOf(((Path) svgElement).getData()));
				
			//Text
			}else if (svgElement instanceof Text){
	
				element =  new Element(SVG_TAG_TEXT, SVG_NS);
				element.setAttribute(SVG_TAG_ATT_X, String.valueOf(((Text) svgElement).getX()));
				element.setAttribute(SVG_TAG_ATT_Y, String.valueOf(((Text) svgElement).getY()));
				element.setText(((Text) svgElement).getText());
			
			//Image
			}else if (svgElement instanceof Image){
	
				element =  new Element(SVG_TAG_IMAGE, SVG_NS);
				if(!parent.getName().equalsIgnoreCase(SVG_TAG_DEFS)){
					element.setAttribute(SVG_TAG_ATT_X, String.valueOf(((Image) svgElement).getX()));
					element.setAttribute(SVG_TAG_ATT_Y, String.valueOf(((Image) svgElement).getY()));
				}
				element.setAttribute(SVG_TAG_ATT_WIDTH, String.valueOf(((Image) svgElement).getWidth()));
				element.setAttribute(SVG_TAG_ATT_HEIGHT, String.valueOf(((Image) svgElement).getHeight()));
				element.setAttribute(XLINK_TAG_ATT_HREF,((Image) svgElement).getHref(), XLINK_NS);


			//Use
			}else if (svgElement instanceof Use){

				element =  new Element(SVG_TAG_USE, SVG_NS);
				element.setAttribute(SVG_TAG_ATT_X, String.valueOf(((Use) svgElement).getX()));
				element.setAttribute(SVG_TAG_ATT_Y, String.valueOf(((Use) svgElement).getY()));

				if(((Use) svgElement).getHref() != null){
					element.setAttribute(XLINK_TAG_ATT_HREF,((Use) svgElement).getHref().toExternalForm(), XLINK_NS);
				}else{
					element.setAttribute(XLINK_TAG_ATT_HREF,((Use) svgElement).getReference(), XLINK_NS);
				}
			}
		
		}
		
		//Id
		if(svgElement.getId() != null && !svgElement.getId().isEmpty()){
			element.setAttribute(SVG_TAG_ATT_ID, svgElement.getId());
		}

		//Title
		if(svgElement.getTitle() != null && !svgElement.getTitle().isEmpty()){

			Element svgTitleElt =  new Element(SVG_TAG_TITLE,SVG_NS);
			svgTitleElt.setText(svgElement.getTitle());
			element.addContent(svgTitleElt);
		}

		//Description
		if(svgElement.getDescription() != null && !svgElement.getDescription().isEmpty()){

			Element svgDescriptionElt =  new Element(SVG_TAG_DESC,SVG_NS);
			svgDescriptionElt.setText(svgElement.getDescription());
			element.addContent(svgDescriptionElt);
		}
		
		//Style
		if(svgElement.getSvgStyle() != null && !svgElement.getSvgStyle().isEmpty()){

			element.setAttribute(SVG_TAG_ATT_STYLE,svgElement.getSvgStyle());

		}
		
		//Class
		if(svgElement.getCssClass() != null && !svgElement.getCssClass().isEmpty()){

			element.setAttribute(SVG_TAG_ATT_CLASS,svgElement.getCssClass());
		}
		
		//Transform
		if(svgElement.getTransform() != null && !svgElement.getTransform().isEmpty()){

			element.setAttribute(SVG_TAG_ATT_TRANSFORM,svgElement.getTransform());
		}
		
		parent.addContent(element);
		return parent;

	}

}
