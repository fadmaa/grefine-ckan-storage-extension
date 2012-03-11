package com.google.refine.net.ckan.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Resource {

	public final String format;
	public final String description;
	public final String url;

	public Resource(String format, String description,String url) {
		this.format = format;
		this.description = description;
		this.url = url;
	}
	
	public JSONObject asJsonObject() throws JSONException{
		JSONObject obj = new JSONObject();
		obj.put("url", url);
		obj.put("description", description);
		obj.put("format", format);
		
		return obj;
	}
}
