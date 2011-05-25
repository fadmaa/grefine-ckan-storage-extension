package com.google.refine.net.ckan.storage.commands;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.exporters.Exporter;
import com.google.refine.exporters.ExporterRegistry;
import com.google.refine.model.Project;
import com.google.refine.net.ckan.CkanApiProxy;
import com.google.refine.net.ckan.exporter.HistoryJsonExporter;

public class UploadToCKANCommand extends Command{

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Project project = getProject(request);
		String apikey = request.getParameter("apikey");
		String ckanApiBase = request.getParameter("ckan_base_api");
		final String packageId = request.getParameter("package_id");
		boolean createNewIfNonExisitng = request.getParameter("create_new")!=null && request.getParameter("create_new").toLowerCase().equals("true");
		//get (comma-separated list of ) formats required to be uploaded
		String files = request.getParameter("files");
		
		try{
			if(ckanApiBase==null || ckanApiBase.isEmpty()){
				throw new RuntimeException("Some required parameters are missing: CKAN API base URL");
			}
			if(packageId==null || packageId.isEmpty()){
				throw new RuntimeException("Some required parameters are missing: package ID");
			}
			//remove the last "/" from CKAN API base if it exists
			ckanApiBase = ckanApiBase.trim();
			if(ckanApiBase.endsWith("/")){
				ckanApiBase = ckanApiBase.substring(0, ckanApiBase.length()-1);
			}
			
			CkanApiProxy ckanApiClient = new CkanApiProxy();
					
			Engine engine = getEngine(request, project);
			StringTokenizer tokenizer = new StringTokenizer(files, ",");
			Set<Exporter> exporters = new HashSet<Exporter>();
			while(tokenizer.hasMoreTokens()){
				String format = tokenizer.nextToken();
				Exporter exporter = ExporterRegistry.getExporter(format);
				
				if(exporter==null){
					//either JSON representation of the history or something went wrong
					if(format.equals("history-json")){
						exporter = new HistoryJsonExporter();
					}else{
						//fail
						respondException(response, new RuntimeException("Unknown exporter format"));
						return;
					}
				}
				exporters.add(exporter);
			}
			
			final String packageUrl = ckanApiClient.addGroupOfResources(ckanApiBase , packageId, exporters, project, engine, apikey, createNewIfNonExisitng);
			respondJSON(response, new Jsonizable() {
				
				@Override
				public void write(JSONWriter writer, Properties options)
						throws JSONException {
					writer.object();
					writer.key("code"); writer.value("ok");
					writer.key("package_details");
					writer.object();
					writer.key("packageUrl"); writer.value(packageUrl);
					writer.key("packageId"); writer.value(packageId);
					writer.endObject();
					writer.endObject();
				}
			});
		}catch(Exception e){
			respondException(response, e);
		}
	}

	
}
