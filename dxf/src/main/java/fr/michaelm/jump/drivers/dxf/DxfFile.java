package fr.michaelm.jump.drivers.dxf;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class DxfFile {
    
	public static GeometryFactory geometryFactory = new GeometryFactory();

    public static void write(String fileName, List<String> layerNames,  Envelope envelope, List<DxfENTITY> entities, int precision, boolean writeXData) throws IOException {
              
        if(envelope == null){
        	envelope = new Envelope(0, 0, 0, 0);
        }
        
        if(layerNames== null){
        	layerNames = new ArrayList<String>();
        	layerNames.add("0");
        }
  
        FileWriter fw = new FileWriter(fileName);
       
        try {

            // ECRITURE DU HEADER
            fw.write(DxfGroup.toString(0, "SECTION"));
            fw.write(DxfGroup.toString(2, "HEADER"));
            fw.write(DxfGroup.toString(9, "$ACADVER"));
                fw.write(DxfGroup.toString(1, "AC1009"));
            fw.write(DxfGroup.toString(9, "$CECOLOR"));
                fw.write(DxfGroup.toString(62, 256));
            fw.write(DxfGroup.toString(9, "$CELTYPE"));
                fw.write(DxfGroup.toString(6, "DUPLAN"));
            fw.write(DxfGroup.toString(9, "$CLAYER"));
                fw.write(DxfGroup.toString(8, "0"));
            fw.write(DxfGroup.toString(9, "$ELEVATION"));
                fw.write(DxfGroup.toString(40, 0.0, 3));
            fw.write(DxfGroup.toString(9, "$EXTMAX"));
                fw.write(DxfGroup.toString(10, envelope.getMaxX(), 6));
                fw.write(DxfGroup.toString(20, envelope.getMaxY(), 6));
            fw.write(DxfGroup.toString(9, "$EXTMIN"));
                fw.write(DxfGroup.toString(10, envelope.getMinX(), 6));
                fw.write(DxfGroup.toString(20, envelope.getMinY(), 6));
            fw.write(DxfGroup.toString(9, "$INSBASE"));
                fw.write(DxfGroup.toString(10, 0.0, 1));
                fw.write(DxfGroup.toString(20, 0.0, 1));
                fw.write(DxfGroup.toString(30, 0.0, 1));
            fw.write(DxfGroup.toString(9, "$LIMCHECK"));
                fw.write(DxfGroup.toString(70, 1));
            fw.write(DxfGroup.toString(9, "$LIMMAX"));
                fw.write(DxfGroup.toString(10, envelope.getMaxX(), 6));
                fw.write(DxfGroup.toString(20, envelope.getMaxY(), 6));
            fw.write(DxfGroup.toString(9, "$LIMMIN"));
                fw.write(DxfGroup.toString(10, envelope.getMinX(), 6));
                fw.write(DxfGroup.toString(20, envelope.getMinY(), 6));
            fw.write(DxfGroup.toString(9, "$LUNITS"));
                fw.write(DxfGroup.toString(70, 2));
            fw.write(DxfGroup.toString(9, "$LUPREC"));
                fw.write(DxfGroup.toString(70, 2));
            fw.write(DxfGroup.toString(0, "ENDSEC"));

            // ECRITURE DES TABLES
            fw.write(DxfGroup.toString(0, "SECTION"));
            fw.write(DxfGroup.toString(2, "TABLES"));
                fw.write(DxfGroup.toString(0, "TABLE"));
                fw.write(DxfGroup.toString(2, "STYLE"));
                fw.write(DxfGroup.toString(70, 1));
                fw.write(DxfGroup.toString(0, "STYLE")); // added by L. Becker on 2006-11-08
                    DxfTABLE_STYLE_ITEM style =
                        new DxfTABLE_STYLE_ITEM("STANDARD", 0, 0f, 1f, 0f, 0, 1.0f, "xxx.txt", "yyy.txt");
                    fw.write(style.toString());
                    fw.write(DxfGroup.toString(0, "ENDTAB"));
                fw.write(DxfGroup.toString(0, "TABLE"));
                fw.write(DxfGroup.toString(2, "LTYPE"));
                fw.write(DxfGroup.toString(70, 1));
                fw.write(DxfGroup.toString(0, "LTYPE")); // added by L. Becker on 2006-11-08
                    DxfTABLE_LTYPE_ITEM ltype =
                        new DxfTABLE_LTYPE_ITEM("CONTINUE", 0, "", 65, 0f, new float[0]);
                    fw.write(ltype.toString());
                    fw.write(DxfGroup.toString(0, "ENDTAB"));
                fw.write(DxfGroup.toString(0, "TABLE"));
                fw.write(DxfGroup.toString(2, "LAYER"));
                fw.write(DxfGroup.toString(70, 2));

                for (String layerName: layerNames){
                	DxfTABLE_LAYER_ITEM layer =  new DxfTABLE_LAYER_ITEM(layerName, 0, 131, "CONTINUE");
                    fw.write(DxfGroup.toString(0, "LAYER"));
                    fw.write(layer.toString());
                }                
                fw.write(DxfGroup.toString(0, "ENDTAB"));
                fw.write(DxfGroup.toString(0, "ENDSEC"));
                
                // ECRITURE DES FEATURES
                fw.write(DxfGroup.toString(0, "SECTION"));
                fw.write(DxfGroup.toString(2, "ENTITIES"));
                for(DxfENTITY entity : entities){
                    fw.write(entity.toDXF(precision, writeXData));
            	}
                fw.write(DxfGroup.toString(0, "ENDSEC"));
                
            // FIN DE FICHIER
            fw.write(DxfGroup.toString(0, "EOF"));
            fw.flush();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (null != fw) try {fw.close();} catch(IOException ioe){};
        }
        return;
    }
    
    public static String point2Dxf(Geometry geometry, String layerName, int precision) {

        StringBuffer sb = new StringBuffer(DxfGroup.toString(0, "POINT"));
        sb.append(DxfGroup.toString(8, layerName));
        Coordinate coord = ((Point)geometry).getCoordinate();
        sb.append(DxfGroup.toString(10, coord.x, precision));
        sb.append(DxfGroup.toString(20, coord.y, precision));
        if (!Double.isNaN(coord.z)){
        	sb.append(DxfGroup.toString(30, coord.z, precision));
        }
        return sb.toString();
    }

    public static String lineString2Dxf(Geometry geometry, String layerName,int precision) {
        LineString geom = (LineString)geometry;
        Coordinate[] coords = geom.getCoordinates();
        boolean isLine = (coords.length == 2);
        StringBuffer sb;
        if (!isLine) {
        	sb = new StringBuffer(DxfGroup.toString(0, "POLYLINE"));
        }
        else {
            sb = new StringBuffer(DxfGroup.toString(0, "LINE"));       	
        }
        sb.append(DxfGroup.toString(8, layerName));
        if (isLine){
            sb.append(DxfGroup.toString(10, coords[0].x, precision));
            sb.append(DxfGroup.toString(20, coords[0].y, precision));
	        if (!Double.isNaN(coords[0].z)) {
	            sb.append(DxfGroup.toString(30, coords[0].z, precision));
	        }
            sb.append(DxfGroup.toString(11, coords[1].x, precision));
            sb.append(DxfGroup.toString(21, coords[1].y, precision));
	        if (!Double.isNaN(coords[1].z)) {
	            sb.append(DxfGroup.toString(31, coords[1].z, precision));
	        }
        }
        else {
            sb.append(DxfGroup.toString(66, 1));
            sb.append(DxfGroup.toString(10, "0.0"));
            sb.append(DxfGroup.toString(20, "0.0"));
            if (!Double.isNaN(coords[0].z)) sb.append(DxfGroup.toString(30, "0.0"));
            sb.append(DxfGroup.toString(70, 8));
            
            for (int i = 0 ; i < coords.length ; i++) {
                sb.append(DxfGroup.toString(0, "VERTEX"));
                sb.append(DxfGroup.toString(8, layerName));
                sb.append(DxfGroup.toString(10, coords[i].x, precision));
                sb.append(DxfGroup.toString(20, coords[i].y, precision));
                if (!Double.isNaN(coords[i].z)) sb.append(DxfGroup.toString(30, coords[i].z, precision));
                sb.append(DxfGroup.toString(70, 32));
            }
            sb.append(DxfGroup.toString(0, "SEQEND"));
        }
        return sb.toString();
    }

    public static String polygon2Dxf(Geometry geometry, String layerName,int precision) {
        Polygon geom = (Polygon)geometry;
        Coordinate[] coords = geom.getExteriorRing().getCoordinates();
        StringBuffer sb = new StringBuffer(DxfGroup.toString(0, "POLYLINE"));
        sb.append(DxfGroup.toString(8, layerName));
        sb.append(DxfGroup.toString(66, 1));
        sb.append(DxfGroup.toString(10, "0.0"));
        sb.append(DxfGroup.toString(20, "0.0"));
        if (!Double.isNaN(coords[0].z)) sb.append(DxfGroup.toString(30, "0.0"));
        sb.append(DxfGroup.toString(70, 9));
        for (int i = 0 ; i < coords.length ; i++) {
            sb.append(DxfGroup.toString(0, "VERTEX"));
            sb.append(DxfGroup.toString(8, layerName));
            sb.append(DxfGroup.toString(10, coords[i].x, precision));
            sb.append(DxfGroup.toString(20, coords[i].y, precision));
            if (!Double.isNaN(coords[i].z)) sb.append(DxfGroup.toString(30, coords[i].z, precision));
            sb.append(DxfGroup.toString(70, 32));
        }
        sb.append(DxfGroup.toString(0, "SEQEND"));
        for (int h = 0 ; h < geom.getNumInteriorRing() ; h++) {
            sb.append(DxfGroup.toString(0, "POLYLINE"));
            sb.append(DxfGroup.toString(8, layerName));
            sb.append(DxfGroup.toString(66, 1));
            sb.append(DxfGroup.toString(10, "0.0"));
            sb.append(DxfGroup.toString(20, "0.0"));
            if (!Double.isNaN(coords[0].z)) sb.append(DxfGroup.toString(30, "0.0"));
            sb.append(DxfGroup.toString(70, 9));
            coords = geom.getInteriorRingN(h).getCoordinates();
            for (int i = 0 ; i < coords.length ; i++) {
                sb.append(DxfGroup.toString(0, "VERTEX"));
                sb.append(DxfGroup.toString(8, layerName));
                sb.append(DxfGroup.toString(10, coords[i].x, precision));
                sb.append(DxfGroup.toString(20, coords[i].y, precision));
                if (!Double.isNaN(coords[i].z)) sb.append(DxfGroup.toString(30, coords[i].z, precision));
                sb.append(DxfGroup.toString(70, 32));
            }
            sb.append(DxfGroup.toString(0, "SEQEND"));
        }
        
        return sb.toString();
    }

}
