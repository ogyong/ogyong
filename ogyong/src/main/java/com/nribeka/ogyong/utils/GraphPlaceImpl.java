package com.nribeka.ogyong.utils;

import com.facebook.model.GraphLocation;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphObjectList;
import com.facebook.model.GraphPlace;

import org.json.JSONObject;

import java.util.Map;

/**
 */
public class GraphPlaceImpl implements GraphPlace {

    private String id;

    public GraphPlaceImpl(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setName(String name) {

    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public void setCategory(String category) {

    }

    @Override
    public GraphLocation getLocation() {
        return null;
    }

    @Override
    public void setLocation(GraphLocation location) {

    }

    @Override
    public <T extends GraphObject> T cast(Class<T> graphObjectClass) {
        return null;
    }

    @Override
    public Map<String, Object> asMap() {
        return null;
    }

    @Override
    public JSONObject getInnerJSONObject() {
        return null;
    }

    @Override
    public Object getProperty(String propertyName) {
        return null;
    }

    @Override
    public <T extends GraphObject> T getPropertyAs(String propertyName, Class<T> graphObjectClass) {
        return null;
    }

    @Override
    public <T extends GraphObject> GraphObjectList<T> getPropertyAsList(String propertyName, Class<T> graphObjectClass) {
        return null;
    }

    @Override
    public void setProperty(String propertyName, Object propertyValue) {

    }

    @Override
    public void removeProperty(String propertyName) {

    }
}
