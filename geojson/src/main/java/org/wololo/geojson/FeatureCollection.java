package org.wololo.geojson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class FeatureCollection extends GeoJSON {

    private final Feature[] features;
    @JsonSerialize()
    private final Crs crs;

    @JsonCreator
    public FeatureCollection(@JsonProperty("features") Feature[] features, @JsonProperty("crs") Crs crs) {
        super();
        this.features = features;
        this.crs = crs;
    }

    public Feature[] getFeatures() {
        return features;
    }

    public Crs getCrs() {
        return crs;
    }

}
