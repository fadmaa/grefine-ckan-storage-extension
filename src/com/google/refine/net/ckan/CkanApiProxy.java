package com.google.refine.net.ckan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.Exporter;
import com.google.refine.exporters.StreamExporter;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.model.Project;
import com.google.refine.net.ckan.model.Resource;
import com.google.refine.net.ckan.rdf.ProvenanceFactory;
import com.hp.hpl.jena.rdf.model.Model;

public class CkanApiProxy {

	public String registerPackageResources(String packageUrl, Set<Resource> resources, String apiKey) throws ClientProtocolException, IOException, JSONException{
		//get the package 
		HttpClient client = new DefaultHttpClient();
		//head request does not work! 
		HttpGet get = new HttpGet(packageUrl);
		HttpResponse response = client.execute(get);
		if(response.getStatusLine().getStatusCode() != 200){
			throw new RuntimeException("package " + packageUrl + " not found");
		}
		//parse resources 
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		response.getEntity().writeTo(os);
		JSONObject packageObj = new JSONObject(os.toString());
		JSONArray resourcesArr = packageObj.getJSONArray("resources");
		String ckan_url = packageObj.getString("ckan_url");
		//add the new resources
		for(Resource resource:resources){
			resourcesArr.put(resource.asJsonObject());
		}
		//save
		JSONObject obj = new JSONObject();
		obj.put("resources", resourcesArr);
		
		HttpPost post = new HttpPost(packageUrl);
		post.setHeader("Authorization", apiKey);
		StringEntity entity = new StringEntity(obj.toString(),"UTF-8");
		post.setHeader("Content-type","application/x-www-form-urlencoded");
		post.setEntity(entity);
		
		response =  client.execute(post);
		
		if(response.getStatusLine().getStatusCode()!=200){
			throw new RuntimeException("something went wrong whil registering the new resource of package " + packageUrl );
		}
		
		return ckan_url;
	}
	
	/**
	 * @param packageUrl
	 * @return checks whether a package with the URL given exists
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public boolean exists(String packageUrl) throws ClientProtocolException, IOException{
		HttpClient client = new DefaultHttpClient();
		//head request does not work! 
		HttpGet get = new HttpGet(packageUrl);
		HttpResponse response = client.execute(get);
		return response.getStatusLine().getStatusCode() == 200;
	}
	
	public JSONObject getPackage(String packageUrl) throws ClientProtocolException, IOException, JSONException{
		HttpClient client = new DefaultHttpClient();
		//head request does not work! 
		HttpGet get = new HttpGet(packageUrl);
		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != 200){
			return null;
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		response.getEntity().writeTo(os);
		return new JSONObject(os.toString());
	}
	
	public void registerNewPackage(String packageId, String ckanBaseUri, String apikey) throws ClientProtocolException, IOException, JSONException{
		HttpClient client = new DefaultHttpClient();
		//head request does not work! 
		HttpPost post = new HttpPost(ckanBaseUri);
		post.setHeader("Authorization", apikey);
		post.setHeader("Content-type","application/x-www-form-urlencoded");
		StringEntity entity = new StringEntity(getNewPackageInJson(packageId).toString(),"UTF-8");
		post.setEntity(entity);
		
		HttpResponse response = client.execute(post);
		int responseCode = response.getStatusLine().getStatusCode();
		if(responseCode < 200 || responseCode >=300){
			throw new RuntimeException("failed to register a new package with response code " + responseCode + " at " + ckanBaseUri );
		}
	}
	
	//return the URL of the package
	public String addGroupOfResources(String ckanBaseUri, String packageId, Set<Exporter> exporters, Project project, 
			Engine engine, ProvenanceFactory provFactory, String apikey, boolean createNewIfNonExisitng, boolean provenanceRequired) throws IOException, JSONException {
		Map<String,String> resourceFormatsUrlsMap = new HashMap<String, String>();
		String packageUrl = ckanBaseUri + "/" + packageId;
		Set<Resource> resources = new HashSet<Resource>();
		if(! exists(packageUrl)){
			if(createNewIfNonExisitng){
				//create a new package
				registerNewPackage(packageId, ckanBaseUri,apikey);
			}else{
				//fail
				throw new RuntimeException("Package with id " + packageId + " does not exist on " + ckanBaseUri);
			}
		}
		StorageApiProxy storage = new StorageApiProxy();
		
		String seed = "-" + getRandomString(project.id);
		for(Exporter exporter:exporters){
			String lbl = exporter.getContentType().replaceAll("\\/", "-") + seed;
			String url;
			if(exporter instanceof WriterExporter){
				StringWriter out = new StringWriter();
				((WriterExporter) exporter).export(project, new Properties(), engine, out);
				url = storage.uploadFile(out.toString(), lbl, apikey);
			}else if(exporter instanceof StreamExporter){
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				((StreamExporter) exporter).export(project, new Properties(), engine, out);
				url = storage.uploadFile(out.toString(), lbl, apikey);
			}else{
				throw new RuntimeException("Unknown exporter type");
			}
			//collect the added resource
			resourceFormatsUrlsMap.put(exporter.getContentType(),url);
			resources.add(getCkanResource(exporter.getContentType(), url, packageId));
		}
		//if provenance is required
		if(provenanceRequired){
			//make sure that all the required files are provided
			if(resourceFormatsUrlsMap.containsKey("application/x-unknown") && resourceFormatsUrlsMap.containsKey("text/turtle") && resourceFormatsUrlsMap.containsKey("application/json")){
				String provFileLabel = "provenance-" + seed; 
				resources.add(getProvenance(resourceFormatsUrlsMap,storage, provFactory , provFileLabel, apikey));
			}
		}
		//register resources
		String ckan_url = this.registerPackageResources(packageUrl, resources, apikey);
		
		return ckan_url;
	}
	
	private Resource getProvenance(Map<String, String> resourceFormatsUrlsMap, StorageApiProxy storage, ProvenanceFactory provFactory, String provFileLabel, String apikey) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		//build the RDF
		Model model = provFactory.getProvenance(resourceFormatsUrlsMap.get("application/x-unknown"), resourceFormatsUrlsMap.get("text/turtle"), 
				resourceFormatsUrlsMap.get("application/json"),calendar);
		//upload the RDF data to CKAN Storage
		StringWriter sw = new StringWriter();
		model.write(sw, null, "TURTLE");
		sw.flush();
		String url = storage.uploadFile(sw.toString(), provFileLabel, apikey);
		//build and return a representing resource
		String description = "RDF provenance description (from Google Refine)";
		return new Resource("text/turtle", description, url);
	}

	private String getRandomString(long id) {
		return String.valueOf(id) + "-" + System.currentTimeMillis();
	}

	private Resource getCkanResource(String format, String url, String packageId){
		//Google Refine CSV exporter returns "application/x-unknown" as format I'll replace this with text/csv as it is more intuitive
		if(format.equals("application/x-unknown")){
			format = "text/csv";
		}
		String description = translate(format) + " (from Google Refine)";
		return new Resource(format, description, url);
	}
	
	private JSONObject getNewPackageInJson(String packageId) throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("name", packageId);
		obj.put("notes", "This package was created using Google Refine extension.");
		
		return obj;
	}
	
	//provide a more friendly label for formate e.g. text/csv ==> CSV table
	private String translate(String format){
		return formatLabels.get(format);
	}
	
	private static final Map<String, String> formatLabels = new HashMap<String, String>();
	static{
		formatLabels.put("text/csv", "CSV table");
		formatLabels.put("application/json", "Operation history");
		formatLabels.put("text/turtle", "RDF data");
	}
}
