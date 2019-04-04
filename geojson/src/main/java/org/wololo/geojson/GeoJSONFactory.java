package org.wololo.geojson;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JavaType;

public class GeoJSONFactory {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static GeoJSON create(File geoJson) {
        try {
            JsonNode node = mapper.readTree(geoJson);
            String type = node.get("type").asText();
            if (type.equals("FeatureCollection")) {
                return readFeatureCollection(node);
            } else if (type.equals("Feature")) {
                return readFeature(node);
            } else {
                return readGeometry(node, type);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static FeatureCollection readFeatureCollection(JsonNode node) throws JsonParseException, JsonMappingException, IOException, ClassNotFoundException {

        Feature[] features = new Feature[node.get("features").size()];

        JsonNode crsNode = node.get("crs");
        for (int i = 0; i < node.get("features").size(); i++) {
            features[i] = readFeature(node.get("features").get(i));
        }
        return new FeatureCollection(features, readCrs(crsNode));
    }

    private static Crs readCrs(JsonNode node) throws JsonParseException, JsonMappingException, IOException, ClassNotFoundException {

        if (node != null) {

            JavaType javaType = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
            Map<String, Object> properties = mapper.readValue(node.get("properties").traverse(), javaType);
            String type = node.get("type").asText();
            return new Crs(type, properties);

        } else {
            return null;
        }
    }

    private static Feature readFeature(JsonNode node) throws JsonParseException, JsonMappingException, IOException, ClassNotFoundException {
        JsonNode geometryNode = node.get("geometry");
        JavaType javaType = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> properties = mapper.readValue(node.get("properties").traverse(), javaType);
        String type = geometryNode.get("type").asText();
        Geometry geometry = readGeometry(geometryNode, type);
        return new Feature(geometry, properties);
    }

    private static Geometry readGeometry(JsonNode node, String type) throws JsonParseException, JsonMappingException, IOException, ClassNotFoundException {
        Geometry geometry = (Geometry) mapper.readValue(node.traverse(), Class.forName("org.wololo.geojson." + type));
        return geometry;
    }
}
