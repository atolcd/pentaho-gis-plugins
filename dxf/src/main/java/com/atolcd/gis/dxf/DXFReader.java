package com.atolcd.gis.dxf;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kabeja.dxf.DXFArc;
import org.kabeja.dxf.DXFCircle;
import org.kabeja.dxf.DXFConstants;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFEllipse;
import org.kabeja.dxf.DXFEntity;
import org.kabeja.dxf.DXFExtendedData;
import org.kabeja.dxf.DXFInsert;
import org.kabeja.dxf.DXFLWPolyline;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.dxf.DXFLine;
import org.kabeja.dxf.DXFMText;
import org.kabeja.dxf.DXFPoint;
import org.kabeja.dxf.DXFPolyline;
import org.kabeja.dxf.DXFSpline;
import org.kabeja.dxf.DXFText;
import org.kabeja.dxf.DXFVertex;
import org.kabeja.dxf.helpers.DXFSplineConverter;
import org.kabeja.dxf.helpers.Point;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.GeometricShapeFactory;

public class DXFReader {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    private String dxfFileName;
    private boolean dxfFileExist;
    private List<Layer> layers;

    private boolean circleAsPolygon;
    private boolean ellipseAsPolygon;
    private boolean polylineAsPolygon;

    public DXFReader(String fileName) throws Exception {
        this.circleAsPolygon = false;
        this.ellipseAsPolygon = false;
        this.polylineAsPolygon = false;
        
        int pointIndex = fileName.lastIndexOf('.');
	    if (pointIndex > 0) {
	    	fileName =  fileName.substring(0,pointIndex);
	    }
	    
	    this.dxfFileName = fileName.concat(".dxf");
		this.dxfFileExist = new File(this.dxfFileName).exists();
		
		if(!this.dxfFileExist){
			 throw new Exception("Missing " + this.dxfFileName + " file");
		}

        this.layers = new ArrayList<Layer>();
    }

    public List<Layer> getLayers() throws ParseException {
        Parser dxfParser = ParserBuilder.createDefaultParser();
        dxfParser.parse(this.dxfFileName, DXFParser.DEFAULT_ENCODING);
        DXFDocument dxfDoc = dxfParser.getDocument();

        Iterator<?> layerIt = dxfDoc.getDXFLayerIterator();
        while (layerIt.hasNext()) {

            DXFLayer dxfLayer = (DXFLayer) layerIt.next();
            Layer layer = new Layer(dxfLayer.getName());
            List<Entity> entities = new ArrayList<Entity>();

            entities.addAll(getMTexts(dxfLayer));
            entities.addAll(getTexts(dxfLayer));
            entities.addAll(getLines(dxfLayer));
            entities.addAll(getPolylines(dxfLayer));
            entities.addAll(getLWPolylines(dxfLayer));
            entities.addAll(getSplines(dxfLayer));
            entities.addAll(getCircles(dxfLayer));
            entities.addAll(getEllipses(dxfLayer));
            entities.addAll(getArcs(dxfLayer));
            entities.addAll(getBlocks(dxfLayer));
			entities.addAll(getPoints(dxfLayer));

            layer.setEntities(entities);

            this.layers.add(layer);
        }
        return layers;
    }

    public boolean isCircleAsPolygon() {
        return circleAsPolygon;
    }

    public void setCircleAsPolygon(boolean circleAsPolygon) {
        this.circleAsPolygon = circleAsPolygon;
    }

    public boolean isEllipseAsPolygon() {
        return ellipseAsPolygon;
    }

    public void setEllipseAsPolygon(boolean ellipseAsPolygon) {
        this.ellipseAsPolygon = ellipseAsPolygon;
    }

    public boolean isPolylineAsPolygon() {
        return polylineAsPolygon;
    }

    public void setPolylineAsPolygon(boolean polylineAsPolygon) {
        this.polylineAsPolygon = polylineAsPolygon;
    }

    private Entity addExtendedData(Entity entity, DXFEntity dxfEntity){

		for(DXFExtendedData dxfExtendedData : dxfEntity.getExtendedData()) {
			
			entity.AddExtendedData(dxfExtendedData.getName(),dxfExtendedData.getType(), dxfExtendedData.getValue());
		}
		
		return entity;
    }
    
    private List<Entity> getPoints(DXFLayer layer){
		
		List<Entity> entities = new ArrayList<Entity>();
		@SuppressWarnings("unchecked")
		List<DXFPoint> dxfPoints = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_POINT);
		
		if(dxfPoints != null){
			
			for(DXFPoint dxfPoint : dxfPoints){

				Point dxfInsertPoint = dxfPoint.getPoint();
				
				Entity entity = new Entity(
						dxfPoint.getID(),
						geometryFactory.createPoint(
								new Coordinate(
									dxfInsertPoint.getX(),
									dxfInsertPoint.getY(),
									dxfInsertPoint.getZ()
								)
							),
						Entity.TYPE_POINT,
						null
				);
				
				entities.add(addExtendedData(entity,dxfPoint));
			
			}
		}
		
		return entities;
		
    }
    
    private List<Entity> getBlocks(DXFLayer layer){
		
		List<Entity> entities = new ArrayList<Entity>();
		@SuppressWarnings("unchecked")
		List<DXFInsert> dxfInserts = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_INSERT);
		
		if(dxfInserts != null){
			
			for(DXFInsert dxfInsert : dxfInserts){

				Point dxfInsertPoint = dxfInsert.getPoint();
				
				Entity entity = new Entity(
						dxfInsert.getID(),
						geometryFactory.createPoint(
								new Coordinate(
									dxfInsertPoint.getX(),
									dxfInsertPoint.getY(),
									dxfInsertPoint.getZ()
								)
							),
						Entity.TYPE_BLOCK,
						null
				);
				
				entities.add(addExtendedData(entity,dxfInsert));
			
			}
		}
		
		return entities;
		
	}

    private List<Entity> getMTexts(DXFLayer layer){
				
		List<Entity> entities = new ArrayList<Entity>();
		@SuppressWarnings("unchecked")
		List<DXFMText> dxfMTexts = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_MTEXT);
		
		if(dxfMTexts != null){
			
			for(DXFMText dxfMText : dxfMTexts){

				Point dxfInsertPoint = dxfMText.getInsertPoint();

				Entity entity = new Entity(
						dxfMText.getID(),
						geometryFactory.createPoint(
								new Coordinate(
										dxfInsertPoint.getX(),
										dxfInsertPoint.getY(),
										dxfInsertPoint.getZ()
									)
							),
						Entity.TYPE_MTEXT,
						dxfMText.getText()
				);
				
				entities.add(addExtendedData(entity,dxfMText));
			
			}
		}
		
		return entities;
		
	}

    private List<Entity> getTexts(DXFLayer layer){

		List<Entity> entities = new ArrayList<Entity>();
		@SuppressWarnings("unchecked")
		List<DXFText> dxfTexts = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_TEXT);
		
		if(dxfTexts != null){
			
			for(DXFText dxfText : dxfTexts){
				
				Point dxfInsertPoint = dxfText.getInsertPoint();
				
				Entity entity = new Entity(
						dxfText.getID(),
						geometryFactory.createPoint(
								new Coordinate(
										dxfInsertPoint.getX(),
										dxfInsertPoint.getY(),
										dxfInsertPoint.getZ()
									)
							),
						Entity.TYPE_TEXT,
						dxfText.getText()
				);
				
				entities.add(addExtendedData(entity,dxfText));
			
			}
		}
		
		return entities;
		
	}

    private List<Entity> getLines(DXFLayer layer){
		
		List<Entity> entities = new ArrayList<Entity>();
		@SuppressWarnings("unchecked")
		List<DXFLine> dxfLines = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_LINE);
		
		if(dxfLines != null){
			
			for(DXFLine dxfLine : dxfLines){
				
		
				Point start = dxfLine.getStartPoint();
				Point end = dxfLine.getEndPoint();
				
				Entity entity = new Entity(
						dxfLine.getID(),
						geometryFactory.createLineString(
							new Coordinate[]{
								new Coordinate(start.getX(),start.getY(),start.getZ()),
								new Coordinate(end.getX(),end.getY(),end.getZ())
							}
						),
						Entity.TYPE_LINE,
						null
				);
				
				entities.add(addExtendedData(entity,dxfLine));
			}
		}
		
		return entities;
		
	}

    private List<Entity> getPolylines(DXFLayer layer){

		List<Entity> entities = new ArrayList<Entity>();
		@SuppressWarnings("unchecked")
		List<DXFPolyline> dxfpolylines = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_POLYLINE);
		
		if(dxfpolylines != null){

			for(DXFPolyline dxfPolyline : dxfpolylines){

				CoordinateList coordinates = new CoordinateList();
				@SuppressWarnings("unchecked")
				Iterator<DXFVertex> vertexIt = dxfPolyline.getVertexIterator();
				
				while(vertexIt.hasNext()){
					
					DXFVertex vertex = vertexIt.next();
					
					if(!vertex.is2DSplineApproximationVertex()
							&& !vertex.is2DSplineControlVertex()
							&& !vertex.isCurveFitVertex()){
						
						coordinates.add(new Coordinate(vertex.getX(),vertex.getY(),vertex.getZ()),false);

					}
					
				}

				if(dxfPolyline.isClosed()){
					coordinates.add(coordinates.get(0),false);
				}
				
				Geometry geometry = geometryFactory.createLineString(coordinates.toCoordinateArray());
				if(((LineString)geometry).isClosed() && geometry.getCoordinates().length >= 4 && this.polylineAsPolygon){
					geometry = geometryFactory.createPolygon(geometry.getCoordinates());
				}

				Entity entity = new Entity(
						dxfPolyline.getID(),
						geometry,
						Entity.TYPE_POLYLINE,
						null
				);
				
				entities.add(addExtendedData(entity,dxfPolyline));

			}
		}
		
		return entities;
		
	}

    private List<Entity> getLWPolylines(DXFLayer layer){
		
		List<Entity> entities = new ArrayList<Entity>();
		@SuppressWarnings("unchecked")
		List<DXFLWPolyline> dxfLwPolylines = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_LWPOLYLINE);
		
		if(dxfLwPolylines != null){

			for(DXFPolyline dxfLwPolyline : dxfLwPolylines){
		
				CoordinateList coordinates = new CoordinateList();
				@SuppressWarnings("unchecked")
				Iterator<DXFVertex> vertexIt = dxfLwPolyline.getVertexIterator();
				
				while(vertexIt.hasNext()){
					
					DXFVertex vertex = vertexIt.next();

					if(!vertex.is2DSplineApproximationVertex()
							&& !vertex.is2DSplineControlVertex()
							&& !vertex.isCurveFitVertex()){
						
						coordinates.add(new Coordinate(vertex.getX(),vertex.getY(), vertex.getZ()),false);

					}
					
				}

				if(dxfLwPolyline.isClosed()){
					coordinates.add(coordinates.get(0),false);
				}
				
				Geometry geometry = geometryFactory.createLineString(coordinates.toCoordinateArray());
				if(((LineString)geometry).isClosed() && geometry.getCoordinates().length >= 4 && this.polylineAsPolygon){
					geometry = geometryFactory.createPolygon(geometry.getCoordinates());
				}
				
				Entity entity = new Entity(
						dxfLwPolyline.getID(),
						geometry,
						Entity.TYPE_LWPOLYLINE,
						null
				);
				
				entities.add(addExtendedData(entity,dxfLwPolyline));

			}
		}
		
		return entities;

	}
	
	private List<Entity> getSplines(DXFLayer layer){
		
		List<Entity> entities = new ArrayList<Entity>();
		@SuppressWarnings("unchecked")
		List<DXFSpline> dxfSplines = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_SPLINE);
				
		if(dxfSplines != null){

			for(DXFSpline dxfSpline : dxfSplines){

				DXFPolyline dxfPolyline = DXFSplineConverter.toDXFPolyline(dxfSpline);

				CoordinateList coordinates = new CoordinateList();
				@SuppressWarnings("unchecked")
				Iterator<DXFVertex> vertexIt = dxfPolyline.getVertexIterator();
				
				while(vertexIt.hasNext()){
					
					DXFVertex vertex = vertexIt.next();
					
					if(!vertex.is2DSplineApproximationVertex()
							&& !vertex.is2DSplineControlVertex()
							&& !vertex.isCurveFitVertex()){
						
						coordinates.add(new Coordinate(vertex.getX(),vertex.getY(),vertex.getZ()),false);

					}
					
				}

				if(dxfPolyline.isClosed()){
					coordinates.add(coordinates.get(0),false);
				}
				
				Geometry geometry = geometryFactory.createLineString(coordinates.toCoordinateArray());
				if(((LineString)geometry).isClosed() && geometry.getCoordinates().length >= 4 && this.polylineAsPolygon){
					geometry = geometryFactory.createPolygon(geometry.getCoordinates());
				}

				Entity entity = new Entity(
						dxfSpline.getID(),
						geometry,
						Entity.TYPE_LWPOLYLINE,
						null
				);
				
				entities.add(addExtendedData(entity,dxfSpline));

			}
		}
		
		return entities;

	}
	
	private List<Entity> getCircles(DXFLayer layer){
		
		List<Entity> entities = new ArrayList<Entity>();
		@SuppressWarnings("unchecked")
		List<DXFCircle> dxfCircles = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_CIRCLE);
		
		if(dxfCircles != null){
			
			for(DXFCircle dxfCircle : dxfCircles){

				
				Point dxfCenter = dxfCircle.getCenterPoint();

				GeometricShapeFactory geometricShapeFactory = new GeometricShapeFactory();
				geometricShapeFactory.setCentre(new Coordinate(dxfCenter.getX(),dxfCenter.getY()));
				geometricShapeFactory.setWidth(dxfCircle.getRadius()*2);
				geometricShapeFactory.setHeight(dxfCircle.getRadius()*2);
			
				Geometry geometry = geometricShapeFactory.createCircle();
				geometry = geometryFactory.createPolygon(setZ(geometry.getCoordinates(),dxfCenter.getZ()));
				
				if(!this.circleAsPolygon){
					geometry = geometryFactory.createLineString(((Polygon)geometry).getExteriorRing().getCoordinates());
				}
				
				Entity entity = new Entity(
						dxfCircle.getID(),
						geometry,
						Entity.TYPE_CIRCLE,
						null
				);
				
				entities.add(addExtendedData(entity,dxfCircle));
			
			}
		}
		
		return entities;
		
	}
	
	private List<Entity> getEllipses(DXFLayer layer){
		
		List<Entity> entities = new ArrayList<Entity>();
		@SuppressWarnings("unchecked")
		List<DXFEllipse> dxfEllipses = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_ELLIPSE);
		
		if(dxfEllipses != null){
			
			for(DXFEllipse dxfEllipse : dxfEllipses){

				Point dxfCenter = dxfEllipse.getCenterPoint();
				double width = dxfEllipse.getHalfMajorAxisLength() * 2;
				double height = width * dxfEllipse.getRatio();
				GeometricShapeFactory geometricShapeFactory = new GeometricShapeFactory();
				geometricShapeFactory.setCentre(new Coordinate(dxfCenter.getX(),dxfCenter.getY()));
				geometricShapeFactory.setWidth(width);
				geometricShapeFactory.setHeight(height);
				geometricShapeFactory.setRotation(dxfEllipse.getRotationAngle());

				Geometry geometry = geometricShapeFactory.createEllipse();
				geometry = geometryFactory.createPolygon(setZ(geometry.getCoordinates(),dxfCenter.getZ()));
				
				if(!this.ellipseAsPolygon){
					geometry = geometryFactory.createLineString(((Polygon)geometry).getExteriorRing().getCoordinates());
				}
				
				Entity entity = new Entity(
						dxfEllipse.getID(),
						geometry,
						Entity.TYPE_ELLIPSE,
						null
				);
				
				entities.add(addExtendedData(entity,dxfEllipse));
			
			}
		}
		
		return entities;
		
	}
	
	private List<Entity> getArcs(DXFLayer layer){
		
		List<Entity> entities = new ArrayList<Entity>();
		@SuppressWarnings("unchecked")
		List<DXFArc> dxfArcs = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_ARC);
		
		if(dxfArcs != null){
			
			for(DXFArc dxfArc : dxfArcs){

				Point dxfCenter = dxfArc.getCenterPoint();
				double dxfStarAngle = dxfArc.getStartAngle();
				double dxfTotalAngle = dxfArc.getTotalAngle();

				Coordinate center = new Coordinate(dxfCenter.getX(),dxfCenter.getY());
				
				double startAngle = Angle.toRadians(dxfStarAngle);
				double totalAngle = Angle.toRadians(dxfTotalAngle);

				GeometricShapeFactory geometricShapeFactory = new GeometricShapeFactory();
				geometricShapeFactory.setCentre(center);
				geometricShapeFactory.setWidth(dxfArc.getRadius()*2);
				geometricShapeFactory.setHeight(dxfArc.getRadius()*2);
				Geometry geometry = geometricShapeFactory.createArc(startAngle, totalAngle);
				geometry = geometryFactory.createLineString(setZ(geometry.getCoordinates(),dxfCenter.getZ()));
				
				Entity entity = new Entity(
						dxfArc.getID(),
						geometry,
						Entity.TYPE_ARC,
						null
				);
				
				entities.add(addExtendedData(entity,dxfArc));
			
			}
		}
		
		return entities;
		
	}
	
	private Coordinate[] setZ(Coordinate[] coordinates, double zValue){
		
		for(Coordinate coordinate : coordinates){
			coordinate.z = zValue;
		}
		
		return coordinates;
	}
}