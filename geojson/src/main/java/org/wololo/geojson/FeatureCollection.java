package org.wololo.geojson;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class FeatureCollection extends GeoJSON {

    private final Feature[] features;
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
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
