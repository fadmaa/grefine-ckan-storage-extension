package com.google.refine.net.ckan.exporter;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.exporters.WriterExporter;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;


public class HistoryJsonExporter implements WriterExporter{

	@Override
	public String getContentType() {
		return "application/json";
	}

	@Override
	public void export(Project project, Properties options, Engine engine, Writer w) throws IOException {
		try{
			JSONWriter writer = new JSONWriter(w);
			writer.array();
			for (HistoryEntry entry : project.history.getLastPastEntries(-1)) {
                if (entry.operation != null) {
                    entry.operation.write(writer, options);
                }
            }
			writer.endArray();
		}catch(JSONException je){
			throw new RuntimeException(je);
		}
	}

}
