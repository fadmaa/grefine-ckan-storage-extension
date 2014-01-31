function CkanStorage(){
	this._ckan_base_uri_packages = 'http://datahub.io/api/rest/package/';  		
};

CkanStorage.prototype.showDialog = function(){
	var frame = DialogSystem.createDialog();
	frame.width("400px");
	    
	var header = $('<div></div>').addClass("dialog-header").text("Upload to CKAN").appendTo(frame);
	var body = $('<div></div>').addClass("dialog-body").appendTo(frame);
	var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

	this._renderBody(body);
	this._level = DialogSystem.showDialog(frame);
	this._footer(footer);
	
};

CkanStorage.prototype._renderBody = function(body){
	var self = this;
	var html = $(
				'<div bind="ckan_storage_selection">' +
				  '<span>Select files that will be uploaded to CKAN:</span>' + 
				  '<table>' +
				      '<tr><td><input value="csv" type="checkbox" checked="checked" /></td><td>Cleaned data as CSV</td></tr>' +
				      (theProject.overlayModels.rdfSchema?
				    		  (
				    				  '<tr><td><input value="Turtle" type="checkbox" checked="checked" /></td><td>RDF data (written in Turtle)</td></tr>' + 
				    				  '<tr><td><input value="add_provenance_info" type="checkbox" checked="checked" /></td><td>Provenance description of the RDF data</td></tr>'
				    		  )
				    		  :'') + 
				      '<tr><td><input value="history-json" type="checkbox" checked="checked" /></td><td>Google Refine operations history (as JSON data)</td></tr>' + 
				  '</table>' +
				'</div>'
				).appendTo(body);
	self._elmts = DOM.bind(html);
};

CkanStorage.prototype._footer = function(footer){
	var self = this;
	$('<button></button>').addClass('button').text('Upload')
		.click(function(){
			self._apikey = '';
			self._getApiDetails(function(package_id,create_new,remember_api_key){
				if(self._apikey){
					//get the files selected
					var files = self._elmts.ckan_storage_selection.find('input[type="checkbox"]:checked');
					var files_str = '';
					for(var i=0;i<files.length;i++){
						files_str += files[i].value + ','
					}
					if(!files_str){
						alert('no files are selected for upload');
						return;
					}
					//request upload command
					var dismissBusy = DialogSystem.showBusy("Uploading to CKAN...");
					$.post("/command/ckan-storage-extension/upload-to-ckan",{"remember_api_key":remember_api_key,"project":theProject.id,"apikey":self._apikey,"files":files_str,"package_id":package_id,"ckan_base_api":self._ckan_base_uri_packages,"create_new":create_new},
							function(data){
								dismissBusy();
								if(data.code!=='ok'){
									alert(data.message);
									return;
								}
								theProject.metadata.ckan_metadata = data.package_details;
								self._showSuccessMessage(data.package_details.packageUrl);
							},"json");
				}
			});
		})
		.appendTo(footer);
	
	$('<button></button>').addClass('button').text('Cancel')
		.click(function(){
			DialogSystem.dismissUntil(self._level-1);
		})
		.appendTo(footer);
};

CkanStorage.prototype._showSuccessMessage = function(url){
	var self = this;
	var frame = DialogSystem.createDialog();
	frame.width("300px");
	
	var html = $(
			'<div>' +
			   'Package was successfully registered/updated. check it and fill any missing details at: ' +
			   '<a href="' + url + '" target="_new" >' + url + '</a>' + 
			'</div>'
			);
	
	var header = $('<div></div>').addClass("dialog-header").text("Package successfully registered/updated").appendTo(frame);
	var body = $('<div></div>').addClass("dialog-body").appendTo(frame).append(html);
	var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

	var level = DialogSystem.showDialog(frame);
	var elmts = DOM.bind(html);

	$('<button></button>').addClass('button').html('&nbsp;&nbsp;Ok&nbsp;&nbsp;')
		.click(function(){
			DialogSystem.dismissAll();
		}).appendTo(footer);

};

CkanStorage.prototype._changeBaseUri = function(onDone) {
	var self = this;
	var frame = DialogSystem.createDialog();
	frame.width("350px");
	    
	var html = $(
			'<div>' +
			  'Enter the URI for interacting with packages. This should support CKAN API as described at ' +
			  '<a href="http://packages.python.org/ckan/api/version2.html">http://packages.python.org/ckan/api/version2.html</a>:' + 
			  '<br/><input type="text" value="http://datahub.org/api/rest/package/" bind="new_ckan_base_uri" size="34"/>' +
			'</div>'
		);
	var body = $('<div></div>').addClass("dialog-body").appendTo(frame).append(html);
	var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

	var level = DialogSystem.showDialog(frame);
	var elmts = DOM.bind(html);

	$('<button></button>').addClass('button').html('&nbsp;&nbsp;Ok&nbsp;&nbsp;')
	.click(function(){
		DialogSystem.dismissUntil(level-1);
		if(onDone){
			onDone(elmts.new_ckan_base_uri.val());
		}
	}).appendTo(footer);
	
	$('<button></button>').addClass('button').html('Cancel')
		.click(function(){
			DialogSystem.dismissUntil(level-1);
		}).appendTo(footer);
};

CkanStorage.prototype._getApiDetails = function(onDone){
	var self = this;
	var frame = DialogSystem.createDialog();
	frame.width("520px");
	    
	var html = $(
			  '<div class="ckan-storage-note">' +
			    'Packages will be updated/created at <em bind="ckan_base_uri"></em> ' +
			    '<a href="#" bind="change_ckan_base_uri">change</a>' + 
			  '</div>' +
			  '<table>' +
			    '<tr>' +
			      '<td>CKAN package ID:</td>' + 
			      '<td>' +
			        '<table class="ckan-package-details-table">' +
			          '<tr><td><input type="text" bind="package_id" /></td></tr>' +
			          '<tr><td><input checked="checked" type="checkbox" bind="create_if_non_exist" /> Create the package if it doesn\'t exist</td></tr>' + 
			        '</table>' + 
			      '</td>' +
			    '</tr>' +
			    '<tr>' +
			      '<td>CKAN API key:</td>' + 
			      '<td>' +
			        '<table class="ckan-package-details-table">' +
			          '<tr><td><input type="password" bind="api_key" /></td></tr>' +
			          '<tr><td><input type="checkbox" bind="remember_api_key" />Remember API key</td></tr>' +
			        '</table>' + 
			      '</td>' +
			    '</tr>' +
			  '</table>'
			); 
	var header = $('<div></div>').addClass("dialog-header").text("CKAN API Details").appendTo(frame);
	var body = $('<div></div>').addClass("dialog-body").appendTo(frame).append(html);
	var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

	var elmts = DOM.bind(html);
	//try to get the API Key if saved
	$.ajax({
        async: false,
        url: "/command/core/get-preference?" + $.param({ 
            name: "CKAN.api_key" 
        }),
        success: function(data) {
            if (data.value && data.value != "null") {
            	elmts.api_key.val(data.value);
        		elmts.remember_api_key.attr('checked','checked');
            } 
        },
        dataType: "json"
    });
	var level = DialogSystem.showDialog(frame);
	
	elmts.ckan_base_uri.text(self._ckan_base_uri_packages);
	
	elmts.change_ckan_base_uri.click(function(e){
		e.preventDefault();
		self._changeBaseUri(function(new_uri){
			self._ckan_base_uri_packages = new_uri; 
			elmts.ckan_base_uri.text(self._ckan_base_uri_packages);
		});
	});
	
	if(theProject.metadata.ckan_metadata){
		elmts.package_id.val(theProject.metadata.ckan_metadata.packageId);
	}else if(theProject.dcat_metadata){
		var uri = theProject.dcat_metadata.uri;
		if(uri.match(/^http:\/\/ckan\.net\/package\//g)){
			var insert_index = uri.indexOf('ckan.net/package') + 17;
			var package_id = uri.substring(insert_index);
			elmts.package_id.val(package_id);
			
		}
	}
	
	$('<button></button>').addClass('button').html('&nbsp;&nbsp;Ok&nbsp;&nbsp;')
		.click(function(){
			self._apikey = elmts.api_key.val();
			DialogSystem.dismissUntil(level-1);
			var package_id = elmts.package_id.val();
			if(!package_id){
				alert('Package ID needs to be provided');
				return;
			}
			if(!self._apikey){
				alert('CKAN API key needs to be provided');
				return;
			}
			if(onDone){
				onDone(package_id,elmts.create_if_non_exist.attr('checked')=='checked',elmts.remember_api_key.attr('checked')=='checked');
			}
		}).appendTo(footer);
	$('<button></button>').addClass('button').html('Cancel')
	.click(function(){
		DialogSystem.dismissUntil(level-1);
	}).appendTo(footer);
};


$(function(){
	ExtensionBar.MenuItems.push(
			{
				"id":"ckan",
				"label": "CKAN",
				"submenu" : [
				             {
				            	 "id":"ckan-storage-upload",
				            	 label:"Upload to the Data Hub Storage...",
				            	 click:function(){
				            		 var ckan_storage = new CkanStorage();
				            		 ckan_storage.showDialog();
				            	 }
				             }
				           ] 
			});
});