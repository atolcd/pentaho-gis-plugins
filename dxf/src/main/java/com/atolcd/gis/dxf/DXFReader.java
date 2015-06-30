package com.atolcd.gis.dxf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kabeja.dxf.DXFArc;
import org.kabeja.dxf.DXFCircle;
import org.kabeja.dxf.DXFConstants;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFEllipse;
import org.kabeja.dxf.DXFLWPolyline;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.dxf.DXFLine;
import org.kabeja.dxf.DXFMText;
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
    private List<Layer> layers;

    private boolean circleAsPolygon;
    private boolean ellipseAsPolygon;
    private boolean polylineAsPolygon;

    public DXFReader(String fileName) throws Exception {
        this.circleAsPolygon = false;
        this.ellipseAsPolygon = false;
        this.polylineAsPolygon = false;
        this.dxfFileName = fileName;
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

    private List<Entity> getMTexts(DXFLayer layer) {
        List<Entity> entities = new ArrayList<Entity>();
        @SuppressWarnings("unchecked")
        List<DXFMText> dxfMTexts = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_MTEXT);

        if (dxfMTexts != null) {
            for (DXFMText dxfMText : dxfMTexts) {
                Entity entity = new Entity();

                Point dxfInsertPoint = dxfMText.getInsertPoint();

                entity.setType(Entity.TYPE_MTEXT);
                entity.setGeometry(geometryFactory.createPoint(new Coordinate(dxfInsertPoint.getX(), dxfInsertPoint.getY())));
                entity.setText(dxfMText.getText());

                entities.add(entity);
            }
        }
        return entities;
    }

    private List<Entity> getTexts(DXFLayer layer) {
        List<Entity> entities = new ArrayList<Entity>();
        @SuppressWarnings("unchecked")
        List<DXFText> dxfMTexts = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_TEXT);

        if (dxfMTexts != null) {
            for (DXFText dxfText : dxfMTexts) {
                Entity entity = new Entity();

                Point dxfInsertPoint = dxfText.getInsertPoint();

                entity.setType(Entity.TYPE_TEXT);
                entity.setGeometry(geometryFactory.createPoint(new Coordinate(dxfInsertPoint.getX(), dxfInsertPoint.getY())));
                entity.setText(dxfText.getText());

                entities.add(entity);
            }
        }
        return entities;
    }

    private List<Entity> getLines(DXFLayer layer) {
        List<Entity> entities = new ArrayList<Entity>();
        @SuppressWarnings("unchecked")
        List<DXFLine> dxfLines = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_LINE);

        if (dxfLines != null) {
            for (DXFLine dxfLine : dxfLines) {
                Entity entity = new Entity();

                Point start = dxfLine.getStartPoint();
                Point end = dxfLine.getEndPoint();

                entity.setType(Entity.TYPE_LINE);
                entity.setGeometry(geometryFactory.createLineString(new Coordinate[] { new Coordinate(start.getX(), start.getY()), new Coordinate(end.getX(), end.getY()) }));

                entities.add(entity);
            }
        }
        return entities;
    }

    private List<Entity> getPolylines(DXFLayer layer) {
        List<Entity> entities = new ArrayList<Entity>();
        @SuppressWarnings("unchecked")
        List<DXFPolyline> dxfpolylines = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_POLYLINE);

        if (dxfpolylines != null) {
            for (DXFPolyline dxfPolyline : dxfpolylines) {
                Entity entity = new Entity();

                CoordinateList coordinates = new CoordinateList();
                @SuppressWarnings("unchecked")
                Iterator<DXFVertex> vertexIt = dxfPolyline.getVertexIterator();

                while (vertexIt.hasNext()) {
                    DXFVertex vertex = vertexIt.next();
                    if (!vertex.is2DSplineApproximationVertex() && !vertex.is2DSplineControlVertex() && !vertex.isCurveFitVertex()) {
                        coordinates.add(new Coordinate(vertex.getX(), vertex.getY()), false);
                    }
                }
                if (dxfPolyline.isClosed()) {
                    coordinates.add(coordinates.get(0), false);
                }
                Geometry geometry = geometryFactory.createLineString(coordinates.toCoordinateArray());
                if (((LineString) geometry).isClosed() && this.polylineAsPolygon) {
                    geometry = geometryFactory.createPolygon(geometry.getCoordinates());
                }
                entity.setType(Entity.TYPE_POLYLINE);
                entity.setGeometry(geometry);
                entities.add(entity);
            }
        }
        return entities;
    }

    private List<Entity> getLWPolylines(DXFLayer layer) {
        List<Entity> entities = new ArrayList<Entity>();
        @SuppressWarnings("unchecked")
        List<DXFLWPolyline> dxfLwPolylines = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_LWPOLYLINE);

        if (dxfLwPolylines != null) {
            for (DXFPolyline dxfLwPolyline : dxfLwPolylines) {
                Entity entity = new Entity();

                CoordinateList coordinates = new CoordinateList();
                @SuppressWarnings("unchecked")
                Iterator<DXFVertex> vertexIt = dxfLwPolyline.getVertexIterator();

                while (vertexIt.hasNext()) {
                    DXFVertex vertex = vertexIt.next();
                    if (!vertex.is2DSplineApproximationVertex() && !vertex.is2DSplineControlVertex() && !vertex.isCurveFitVertex()) {
                        coordinates.add(new Coordinate(vertex.getX(), vertex.getY()), false);
                    }
                }
                if (dxfLwPolyline.isClosed()) {
                    coordinates.add(coordinates.get(0), false);
                }
                Geometry geometry = geometryFactory.createLineString(coordinates.toCoordinateArray());
                if (((LineString) geometry).isClosed() && this.polylineAsPolygon) {
                    geometry = geometryFactory.createPolygon(geometry.getCoordinates());
                }
                entity.setType(Entity.TYPE_LWPOLYLINE);
                entity.setGeometry(geometry);
                entities.add(entity);
            }
        }
        return entities;
    }

    private List<Entity> getSplines(DXFLayer layer) {
        List<Entity> entities = new ArrayList<Entity>();
        @SuppressWarnings("unchecked")
        List<DXFSpline> dxfSplines = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_SPLINE);

        if (dxfSplines != null) {
            for (DXFSpline dxfSpline : dxfSplines) {
                DXFPolyline dxfPolyline = DXFSplineConverter.toDXFPolyline(dxfSpline);

                Entity entity = new Entity();

                CoordinateList coordinates = new CoordinateList();
                @SuppressWarnings("unchecked")
                Iterator<DXFVertex> vertexIt = dxfPolyline.getVertexIterator();

                while (vertexIt.hasNext()) {
                    DXFVertex vertex = vertexIt.next();
                    if (!vertex.is2DSplineApproximationVertex() && !vertex.is2DSplineControlVertex() && !vertex.isCurveFitVertex()) {
                        coordinates.add(new Coordinate(vertex.getX(), vertex.getY()), false);
                    }
                }
                if (dxfPolyline.isClosed()) {
                    coordinates.add(coordinates.get(0), false);
                }
                Geometry geometry = geometryFactory.createLineString(coordinates.toCoordinateArray());
                if (((LineString) geometry).isClosed() && this.polylineAsPolygon) {
                    geometry = geometryFactory.createPolygon(geometry.getCoordinates());
                }
                entity.setType(Entity.TYPE_LWPOLYLINE);
                entity.setGeometry(geometry);
                entities.add(entity);
            }
        }
        return entities;
    }

    private List<Entity> getCircles(DXFLayer layer) {
        List<Entity> entities = new ArrayList<Entity>();
        @SuppressWarnings("unchecked")
        List<DXFCircle> dxfCircles = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_CIRCLE);

        if (dxfCircles != null) {
            for (DXFCircle dxfCircle : dxfCircles) {
                Entity entity = new Entity();

                Point dxfCenter = dxfCircle.getCenterPoint();

                GeometricShapeFactory geometricShapeFactory = new GeometricShapeFactory();
                geometricShapeFactory.setCentre(new Coordinate(dxfCenter.getX(), dxfCenter.getY()));
                geometricShapeFactory.setWidth(dxfCircle.getRadius() * 2);
                geometricShapeFactory.setHeight(dxfCircle.getRadius() * 2);

                Geometry geometry = geometricShapeFactory.createCircle();
                if (!this.circleAsPolygon) {
                    geometry = geometryFactory.createLineString(((Polygon) geometry).getExteriorRing().getCoordinates());
                }

                entity.setType(Entity.TYPE_CIRCLE);
                entity.setGeometry(geometry);
                entities.add(entity);
            }
        }
        return entities;

    }

    private List<Entity> getEllipses(DXFLayer layer) {
        List<Entity> entities = new ArrayList<Entity>();
        @SuppressWarnings("unchecked")
        List<DXFEllipse> dxfEllipses = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_ELLIPSE);

        if (dxfEllipses != null) {
            for (DXFEllipse dxfEllipse : dxfEllipses) {
                Entity entity = new Entity();

                Point dxfCenter = dxfEllipse.getCenterPoint();
                // Bounds dxfBounds = dxfEllipse.getBounds();
                // Envelope dxfEnv = new Envelope(dxfBounds.getMinimumX(),
                // dxfBounds.getMaximumX(), dxfBounds.getMinimumY(),
                // dxfBounds.getMaximumY());

                double width = dxfEllipse.getHalfMajorAxisLength() * 2;
                double height = width * dxfEllipse.getRatio();
                GeometricShapeFactory geometricShapeFactory = new GeometricShapeFactory();
                geometricShapeFactory.setCentre(new Coordinate(dxfCenter.getX(), dxfCenter.getY()));
                geometricShapeFactory.setWidth(width);
                geometricShapeFactory.setHeight(height);
                geometricShapeFactory.setRotation(dxfEllipse.getRotationAngle());

                Geometry geometry = geometricShapeFactory.createEllipse();
                if (!this.ellipseAsPolygon) {
                    geometry = geometryFactory.createLineString(((Polygon) geometry).getExteriorRing().getCoordinates());
                }

                entity.setType(Entity.TYPE_ELLIPSE);
                entity.setGeometry(geometry);

                entities.add(entity);
            }
        }
        return entities;
    }

    private List<Entity> getArcs(DXFLayer layer) {
        List<Entity> entities = new ArrayList<Entity>();
        @SuppressWarnings("unchecked")
        List<DXFArc> dxfArcs = layer.getDXFEntities(DXFConstants.ENTITY_TYPE_ARC);

        if (dxfArcs != null) {
            for (DXFArc dxfArc : dxfArcs) {
                Entity entity = new Entity();

                Point dxfCenter = dxfArc.getCenterPoint();
                double dxfStarAngle = dxfArc.getStartAngle();
                double dxfTotalAngle = dxfArc.getTotalAngle();

                Coordinate center = new Coordinate(dxfCenter.getX(), dxfCenter.getY());

                double startAngle = Angle.toRadians(dxfStarAngle);
                double totalAngle = Angle.toRadians(dxfTotalAngle);

                GeometricShapeFactory geometricShapeFactory = new GeometricShapeFactory();
                geometricShapeFactory.setCentre(center);
                geometricShapeFactory.setWidth(dxfArc.getRadius() * 2);
                geometricShapeFactory.setHeight(dxfArc.getRadius() * 2);
                Geometry geometry = geometricShapeFactory.createArc(startAngle, totalAngle);

                entity.setType(Entity.TYPE_ARC);
                entity.setGeometry(geometry);

                entities.add(entity);
            }
        }
        return entities;
    }
}