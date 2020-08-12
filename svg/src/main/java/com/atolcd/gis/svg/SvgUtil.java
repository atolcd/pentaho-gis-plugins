package com.atolcd.gis.svg;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

import javax.imageio.ImageIO;

import com.atolcd.gis.svg.type.graphic.Image;
import com.atolcd.gis.svg.type.graphic.Path;
import com.atolcd.gis.svg.type.graphic.Text;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.geom.util.AffineTransformationBuilder;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;


public class SvgUtil {

	private static ShapeWriter shapeWriter = new ShapeWriter();
	
	protected static String EXT_SVG = ".svg";
	protected static String EXT_PNG = ".png";
	protected static String EXT_JPEG = ".jpeg";
	protected static String EXT_CSS = ".css";
	
	protected static String MIME_TYPE_SVG = "image/svg+xml";
	protected static String MIME_TYPE_PNG = "image/png";
	protected static String MIME_TYPE_JPEG = "image/jpeg";
	protected static String MIME_TYPE_CSS = "text/css";
	
	public static AffineTransformation getGeometryToSvgTransformation (Envelope geometryExtent, Envelope svgExtent){
	
		double ratioGeo = geometryExtent.getWidth() / geometryExtent.getHeight();
		double ratioSvg = svgExtent.getWidth() / svgExtent.getHeight();
		
		double minSvgX;
		double minSvgY;
		double maxSvgX;
		double maxSvgY;
		
		if(ratioSvg < ratioGeo){
			minSvgX = svgExtent.getMinX();
			maxSvgX = svgExtent.getMaxX();
			minSvgY = svgExtent.getMaxY() / 2 - (svgExtent.getMaxX() / ratioGeo / 2);
			maxSvgY = svgExtent.getMaxY() / 2 + (svgExtent.getMaxX() / ratioGeo / 2);
		}else{
			minSvgY = svgExtent.getMinY();
			maxSvgY = svgExtent.getMaxY();
			minSvgX = svgExtent.getMaxX() / 2 - (svgExtent.getMaxY() * ratioGeo / 2);
			maxSvgX = svgExtent.getMaxX() / 2 + (svgExtent.getMaxY() * ratioGeo / 2);
		}
			
		AffineTransformationBuilder AffineTransformationBuilder = new AffineTransformationBuilder(
				new Coordinate(geometryExtent.getMinX(),geometryExtent.getMinY()),
				new Coordinate(geometryExtent.getMinX(),geometryExtent.getMaxY()),
				new Coordinate(geometryExtent.getMaxX(),geometryExtent.getMaxY()),
				new Coordinate(minSvgX,maxSvgY),
				new Coordinate(minSvgX,minSvgY),
				new Coordinate(maxSvgX,minSvgY)
				);
		
		return AffineTransformationBuilder.getTransformation();
	
	}
	
	public static URL toURL(String filenameOrUrl) throws Exception{


		try {

			return new URL(filenameOrUrl);
			
			
		} catch (MalformedURLException e) {
			
			try {
				
				File file = new File(filenameOrUrl);
				if(file.exists()){
					return file.toURI().toURL();
				}else{
					throw new Exception (filenameOrUrl + " URL o filename is not valid");
				}
				
				
			} catch (Exception e1) {
				throw new Exception (filenameOrUrl + " URL o filename is not valid");
			}
	
		}

	}
	
	public static Path toSvgPath(Geometry geometry, int decimalCount){

		Shape shape = shapeWriter.toShape(geometry);
		String pathData = "";

		double tab[] = new double[6];
		PathIterator pathIterator = shape.getPathIterator(null);

		while(!pathIterator.isDone()){

			int currSegmentType= pathIterator.currentSegment(tab);
			tab = round(tab,decimalCount);
			
			switch(currSegmentType) {

				case PathIterator.SEG_MOVETO: {
					pathData += "M " + (tab[0]) + " " + (tab[1]) + " ";
					break;
				}
	
				case PathIterator.SEG_LINETO: {
					pathData += "L " + (tab[0]) + " " + (tab[1]) + " ";
					break;
				}
	
				case PathIterator.SEG_CLOSE: {
					pathData += "Z ";
					break;
				}
	
				case PathIterator.SEG_QUADTO: {
					pathData += "Q " + (tab[0]) + " " + (tab[1]);
					pathData += " "  + (tab[2]) + " " + (tab[3]);
					pathData += " ";
					break;
				}
	
				case PathIterator.SEG_CUBICTO: {
					pathData += "C " + (tab[0]) + " " + (tab[1]);
					pathData += " "  + (tab[2]) + " " + (tab[3]);
					pathData += " "  + (tab[4]) + " " + (tab[5]);
					pathData += " ";
					break;
				}
	
				default:{
					break;
				}
				
			}
			
			pathIterator.next();
		}

		return new Path(pathData.trim());
	}
	
	
	public static Text toSvgText(Geometry geometry, String textContent ) throws Exception{

		return new Text(geometry.getCentroid().getX(), geometry.getCentroid().getY(), textContent);
	
	}
	
	public static Image toSvgImage(URL url, boolean encode) throws Exception{
		

		String mimeType = null;
		int width = 0;
		int height = 0;

		if(url.toExternalForm().toLowerCase().endsWith(EXT_SVG)){
			
			mimeType = MIME_TYPE_SVG;
			
			SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
	        File file = new File("D:/puce.svg");
	        InputStream is = new FileInputStream(file);
	        org.w3c.dom.Document document = factory.createDocument(file.toURI().toURL().toString(), is);
	        UserAgent agent = new UserAgentAdapter();
	        DocumentLoader loader= new DocumentLoader(agent);
	        BridgeContext context = new BridgeContext(agent, loader);
	        context.setDynamic(true);
	        GVTBuilder builder= new GVTBuilder();
	        GraphicsNode root= builder.build(context, document);
	        
			width = (int) root.getPrimitiveBounds().getWidth();
			height = (int) root.getPrimitiveBounds().getHeight();
			
		
		}else if(url.toExternalForm().toLowerCase().endsWith(EXT_PNG)
				|| url.toExternalForm().toLowerCase().endsWith(EXT_JPEG)){		
			
			BufferedImage bufferedImage = null;
			if(url.getProtocol().toString().equalsIgnoreCase("file")){
				bufferedImage = ImageIO.read(new File(url.getFile()));
			}else{
				bufferedImage = ImageIO.read(url);
			}

			if(url.toExternalForm().toLowerCase().endsWith(EXT_PNG)){
				mimeType = MIME_TYPE_PNG;
			}else{
				mimeType = MIME_TYPE_JPEG;
			}
			
			width = bufferedImage.getWidth();
			height = bufferedImage.getHeight();
				
		
		}else{
			throw new Exception("Only svg, png and jpeg images are allowed");
		}
				
		//Lien ou encodage 64 ?
		String href = null;
		if(encode){
			href = "data:".concat(mimeType).concat(";base64,").concat(toBase64(url));

		}else{
			href = url.toExternalForm();
		}

		return new Image(0, 0, width, height, href);

	}

	
	private static String toBase64(URL url) throws Exception {
		
		InputStream inputStream;
		if(url.getProtocol().toString().equalsIgnoreCase("file")){
			inputStream = new FileInputStream(url.getFile());
		}else{
			inputStream = url.openStream();
		}

		byte[] byteArray = new byte[inputStream.available()];
		inputStream.read(byteArray);
		inputStream.close();
		return Base64.getEncoder().encodeToString(byteArray);
      
    }

	
	private static double[] round(double[] values, int decimalCount){
		
		double[] newValues = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			newValues[i] = BigDecimal.valueOf(values[i]).setScale(decimalCount, RoundingMode.HALF_UP).doubleValue();
		}
		return newValues;
	}
	
}
