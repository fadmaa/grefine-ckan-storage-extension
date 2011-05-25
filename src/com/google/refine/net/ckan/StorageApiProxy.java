package com.google.refine.net.ckan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StorageApiProxy {

	public String uploadFile(String fileContent, String fileLabel, String apikey) {
		HttpResponse formFields = null;
		try{
			String filekey = null;
			HttpClient client = new DefaultHttpClient();
		
			//	get the form fields required from ckan storage
			String formUrl = CKAN_STORAGE_BASE_URI + "/auth/form/file/" + fileLabel;
			HttpGet getFormFields = new HttpGet(formUrl);
			getFormFields.setHeader("Authorization", apikey);
			formFields = client.execute(getFormFields);
			HttpEntity entity = formFields.getEntity();
			if(entity!=null){
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				entity.writeTo(os);
			
				//now parse JSON
				JSONObject obj = new JSONObject(os.toString());
			
				//post the file now
				String uploadFileUrl = obj.getString("action");
				HttpPost postFile = new HttpPost(uploadFileUrl);
				postFile.setHeader("Authorization", apikey);
				MultipartEntity mpEntity = new MultipartEntity();
			
				JSONArray fields = obj.getJSONArray("fields");
				for(int i=0;i<fields.length();i++){
					JSONObject fieldObj = fields.getJSONObject(i);
					String fieldName = fieldObj.getString("name");
					String fieldValue = fieldObj.getString("value");
					if(fieldName.equals("key")){
						filekey = fieldValue;
					}
					mpEntity.addPart(fieldName, new StringBody(fieldValue));
				}
			
				//	assure that we got the file key
				if(filekey==null){
					throw new RuntimeException("failed to get the file key from CKAN storage form API. the response from " + formUrl + " was: " + os.toString());
				}
			
				//the file should be the last part
				mpEntity.addPart("file", new StringBody(fileContent));

				postFile.setEntity(mpEntity);
			
				HttpResponse fileUploadResponse = client.execute(postFile);
			
				//check if the response status code was in the 200 range
				if(fileUploadResponse.getStatusLine().getStatusCode()<200 || fileUploadResponse.getStatusLine().getStatusCode()>=300){
					throw new RuntimeException("failed to add the file to CKAN storage. response status line from " + uploadFileUrl + " was: " + fileUploadResponse.getStatusLine());
				}
			
				return CKAN_STORAGE_FILES_BASE_URI + filekey;
			}
			throw new RuntimeException("failed to get form details from CKAN storage. response line was: " + formFields.getStatusLine());
		}catch(JSONException je){
			throw new RuntimeException("failed to upload file to CKAN Storage. A wrong API key maybe? ",je);
		}catch(IOException ioe){
			throw new RuntimeException("failed to upload file to CKAN Storage ",ioe);
		}
	}
	
	private static final String CKAN_STORAGE_BASE_URI = "http://test.ckan.net/api/storage";
	private static final String CKAN_STORAGE_FILES_BASE_URI = "http://test.ckan.net/storage/f/";
	
}
