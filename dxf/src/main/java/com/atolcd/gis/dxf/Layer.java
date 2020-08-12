package com.atolcd.gis.dxf;

import java.util.ArrayList;
import java.util.List;

public class Layer {

    private String name;
    private List<Entity> entities;

    public Layer(String name) {
        this.name = name;
        this.entities = new ArrayList<Entity>();
    }

    public String getName() {
        return name;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setEntities(List<Entity> entities) {
        this.entities = entities;
    }

}
