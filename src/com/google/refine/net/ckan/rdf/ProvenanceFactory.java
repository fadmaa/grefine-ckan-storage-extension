package com.google.refine.net.ckan.rdf;

import java.util.Calendar;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

public class ProvenanceFactory {

	static String opmvNS = "http://purl.org/net/opmv/ns#";
	static String grefineNS = "http://vocab.deri.ie/grefine#";
	static String dctNS = "http://purl.org/dc/terms/";
	static String opmv_commonNS = "http://purl.org/net/opmv/types/common#";
	
	public Model getProvenance(String csvFileUri, String rdfFileUri, String jsonFileUri, Calendar calendar){
		Model model = ModelFactory.createDefaultModel();
		
		model.setNsPrefix("opvm", opmvNS);
		model.setNsPrefix("grefine", grefineNS);
		model.setNsPrefix("dct", dctNS);
		model.setNsPrefix("opmv-common", opmv_commonNS);
		
		Property dct_source = model.createProperty(dctNS + "source");
		Property grefine_operationDescription = model.createProperty(grefineNS + "operationDescription");
		Property grefine_wasExportedBy  = model.createProperty(grefineNS + "wasExportedBy");
		Property opmv_wasPerformedAt  = model.createProperty(opmvNS + "wasPerformedAt");
		Property opmv_used = model.createProperty(opmvNS + "used");
		Property opmv_common_usedScript = model.createProperty(opmv_commonNS + "usedScript");
		Property opmv_common_usedData = model.createProperty(opmv_commonNS + "usedData");
		Property opmv_wasGeneratedBy = model.createProperty(opmvNS + "wasGeneratedBy");	
		
		Resource opmv_Artifact = model.createResource(opmvNS + "Artifact");
		Resource grefine_OperationDescription = model.createResource(grefineNS + "OperationDescription");
		Resource grefine_ExportUsingRDFExtension = model.createResource(grefineNS + "ExportUsingRDFExtension");
		Resource opmv_Process = model.createResource(opmvNS + "Process");
		
		
		Resource rdfFileResource = model.createResource(rdfFileUri);
		Resource csvFileResource = model.createResource(csvFileUri);
		Resource jsonFileResource = model.createResource(jsonFileUri);
		Resource process = model.createResource();
		
		process.addProperty(RDF.type, opmv_Process);
		process.addProperty(RDF.type, grefine_ExportUsingRDFExtension);
		process.addProperty(grefine_operationDescription, jsonFileResource);
		process.addProperty(opmv_used, jsonFileResource);
		process.addProperty(opmv_common_usedScript, jsonFileResource);
		process.addLiteral(opmv_wasPerformedAt, model.createTypedLiteral(calendar));
		process.addProperty(opmv_common_usedData, csvFileResource);
		process.addProperty(opmv_used, csvFileResource);
		
		rdfFileResource.addProperty(RDF.type, opmv_Artifact);
		rdfFileResource.addProperty(dct_source, csvFileResource);
		rdfFileResource.addProperty(grefine_wasExportedBy, process);
		rdfFileResource.addProperty(opmv_wasGeneratedBy, process);
		
		jsonFileResource.addProperty(RDF.type, opmv_Artifact);
		jsonFileResource.addProperty(RDF.type, grefine_OperationDescription);
	
		return model;
	}
}
