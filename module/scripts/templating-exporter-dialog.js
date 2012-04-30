/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

function DataHubTemplatingExporterDialog() {
    this._timerID = null;
    this._createDialog();
    this._updatePreview();
    this._ckan_base_uri_packages = 'http://thedatahub.org/api/rest/package/';
}

DataHubTemplatingExporterDialog.prototype._createDialog = function() {
    var self = this;
    var dialog = $(DOM.loadHTML("ckan-storage-extension", "scripts/dialogs/templating-exporter-dialog.html"));
    this._elmts = DOM.bind(dialog);
    this._elmts.controls.find("textarea").keyup(function() { self._scheduleUpdate(); });
    
    this._elmts.exportButton.click(function() { self._getPackageName(); });
    this._elmts.cancelButton.click(function() { self._dismiss(); });
    this._elmts.resetButton.click(function() {
        self._fillInTemplate(self._createDefaultTemplate());
        self._updatePreview();
    });
    
    this._getSavedTemplate(function(t) {
        self._fillInTemplate(t || self._createDefaultTemplate());
        self._updatePreview();
    });
    
    this._level = DialogSystem.showDialog(dialog);
};

DataHubTemplatingExporterDialog.prototype._getPackageName = function(){
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
			        '</table>' + 
			      '</td>' +
			    '</tr>' +
			  '</table>'
			); 
	var header = $('<div></div>').addClass("dialog-header").text("CKAN API Details").appendTo(frame);
	var body = $('<div></div>').addClass("dialog-body").appendTo(frame).append(html);
	var footer = $('<div></div>').addClass("dialog-footer").appendTo(frame);

	var elmts = DOM.bind(html);
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
	$('<button></button>').addClass('button').html('Cancel')
	.click(function(){
		DialogSystem.dismissUntil(level-1);
	}).appendTo(footer);
	$('<button></button>').addClass('button').html('&nbsp;&nbsp;Next&nbsp;&nbsp;')
		.click(function(){
			DialogSystem.dismissUntil(level-1);
			var package_id = elmts.package_id.val();
			if(!package_id){
				alert('Package ID needs to be provided');
				return;
			}
			$.get('/command/ckan-storage-extension/get-package-details',{"package_id":package_id,"ckan_base_api":self._ckan_base_uri_packages},function(data){
				if(data.packageId){
					if(data.resources.length>0){
						alert("package has " + data.resources.length + " existing resources in webstore");
					}else{
						alert("package has no existing resources in webstore");
					}
				}else{
					alert("package " + package_id + " does not exist.");
				}
				
			},"json");
		}).appendTo(footer);
};



DataHubTemplatingExporterDialog.prototype._getSavedTemplate = function(f) {
    $.getJSON(
        "/command/core/get-preference?" + $.param({ project: theProject.id, name: "exporters.templating.template" }),
        null,
        function(data) {
            if (data.value !== null) {
                f(JSON.parse(data.value));
            } else {
                f(null);
            }
        }
    );
};

DataHubTemplatingExporterDialog.prototype._createDefaultTemplate = function() {
    return {
        prefix: '{\n  "rows" : [\n',
        suffix: '\n  ]\n}',
        separator: ',\n',
        template: '    {' +
            $.map(theProject.columnModel.columns, function(column, i) {
                return '\n      "' + column.name + '" : {{jsonize(cells["' + column.name + '"].value)}}';
            }).join(',') + '\n    }'
    };
};

DataHubTemplatingExporterDialog.prototype._fillInTemplate = function(t) {
    this._elmts.prefixTextarea[0].value = t.prefix;
    this._elmts.suffixTextarea[0].value = t.suffix;
    this._elmts.separatorTextarea[0].value = t.separator;
    this._elmts.templateTextarea[0].value = t.template;
};

DataHubTemplatingExporterDialog.prototype._scheduleUpdate = function() {
    var self = this;
    
    if (this._timerID) {
        window.clearTimeout(this._timerID);
    }
    
    this._elmts.previewTextarea[0].value = "Idling...";
    this._timerID = window.setTimeout(function() {
        self._timerID = null;
        self._elmts.previewTextarea[0].value = "Updating...";
        self._updatePreview();
    }, 1000);
};

DataHubTemplatingExporterDialog.prototype._dismiss = function() {
    DialogSystem.dismissUntil(this._level - 1);
};

DataHubTemplatingExporterDialog.prototype._updatePreview = function() {
    var self = this;
    $.post(
        "/command/core/export-rows/preview.txt",
        {
            "project" : theProject.id, 
            "format" : "template",
            "engine" : JSON.stringify(ui.browsingEngine.getJSON()),
            "sorting" : JSON.stringify(ui.dataTableView.getSorting()),
            "prefix" : this._elmts.prefixTextarea[0].value,
            "suffix" : this._elmts.suffixTextarea[0].value,
            "separator" : this._elmts.separatorTextarea[0].value,
            "template" : this._elmts.templateTextarea[0].value,
            "preview" : true,
            "limit" : "20"
        },
        function (data) {
            self._elmts.previewTextarea[0].value = data;
        },
        "text"
    );
};

DataHubTemplatingExporterDialog.prototype._export = function() {
    var name = $.trim(theProject.metadata.name.replace(/\W/g, ' ')).replace(/\s+/g, '-');
    var form = document.createElement("form");
    $(form)
        .css("display", "none")
        .attr("method", "post")
        .attr("action", "/command/core/export-rows/" + name + ".txt")
        .attr("target", "refine-export");
        
    var appendField = function(name, value) {
        $('<textarea />')
            .attr("name", name)
            .attr("value", value)
            .appendTo(form);
    };

    appendField("engine", JSON.stringify(ui.browsingEngine.getJSON()));
    appendField("project", theProject.id);
    appendField("format", "template");
    appendField("sorting", JSON.stringify(ui.dataTableView.getSorting()));
    appendField("prefix", this._elmts.prefixTextarea[0].value);
    appendField("suffix", this._elmts.suffixTextarea[0].value);
    appendField("separator", this._elmts.separatorTextarea[0].value);
    appendField("template", this._elmts.templateTextarea[0].value);

    document.body.appendChild(form);

    window.open("about:blank", "refine-export");
    form.submit();

    document.body.removeChild(form);
};