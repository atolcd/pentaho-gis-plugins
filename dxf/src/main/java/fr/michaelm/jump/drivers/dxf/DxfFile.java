package fr.michaelm.jump.drivers.dxf;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class DxfFile {

    public static GeometryFactory geometryFactory = new GeometryFactory();

    public DxfFile() {
    }

    public void write(List<Geometry> geometries, String[] layerNames, FileWriter fw, int precision) {

        Envelope envelope = geometryFactory.buildGeometry(geometries).getEnvelopeInternal();
        // Date date = new Date(System.currentTimeMillis());
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
            fw.write(DxfGroup.toString(0, "STYLE")); // added by L. Becker on
                                                     // 2006-11-08
            DxfTABLE_STYLE_ITEM style = new DxfTABLE_STYLE_ITEM("STANDARD", 0, 0f, 1f, 0f, 0, 1.0f, "xxx.txt", "yyy.txt");
            fw.write(style.toString());
            fw.write(DxfGroup.toString(0, "ENDTAB"));
            fw.write(DxfGroup.toString(0, "TABLE"));
            fw.write(DxfGroup.toString(2, "LTYPE"));
            fw.write(DxfGroup.toString(70, 1));
            fw.write(DxfGroup.toString(0, "LTYPE")); // added by L. Becker on
                                                     // 2006-11-08
            DxfTABLE_LTYPE_ITEM ltype = new DxfTABLE_LTYPE_ITEM("CONTINUE", 0, "", 65, 0f, new float[0]);
            fw.write(ltype.toString());
            fw.write(DxfGroup.toString(0, "ENDTAB"));
            fw.write(DxfGroup.toString(0, "TABLE"));
            fw.write(DxfGroup.toString(2, "LAYER"));
            fw.write(DxfGroup.toString(70, 2));
            for (int i = 0; i < layerNames.length; i++) {
                DxfTABLE_LAYER_ITEM layer = new DxfTABLE_LAYER_ITEM(layerNames[i], 0, 131, "CONTINUE");
                fw.write(DxfGroup.toString(0, "LAYER"));
                fw.write(layer.toString());
            }
            fw.write(DxfGroup.toString(0, "ENDTAB"));
            fw.write(DxfGroup.toString(0, "ENDSEC"));

            // ECRITURE DES FEATURES
            fw.write(DxfGroup.toString(0, "SECTION"));
            fw.write(DxfGroup.toString(2, "ENTITIES"));
            for (Geometry geometry : geometries) {
                fw.write(DxfENTITY.geometry2Dxf(geometry, geometry.getUserData().toString(), precision));
            }

            fw.write(DxfGroup.toString(0, "ENDSEC"));

            // FIN DE FICHIER
            fw.write(DxfGroup.toString(0, "EOF"));
            fw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            if (null != fw)
                try {
                    fw.close();
                } catch (IOException ioe) {
                }
            ;
        }
        return;
    }

}
