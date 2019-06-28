var CUSTOM_ACTIONS = "/apps/custom-replication-plugin/actions.infinity.json";
var REPLICATION_SERVICE_URL = "/apps/custom-replication-plugin/actions/replicate.replicate.json";
var REPLICATION_SERVICE_STATUS = "/apps/custom-replication-plugin/actions/status.active.json";
var AGENT_GRID_PANEL = "custom-replication-panel";
var CUSTOM_REPLICATION_BUTTON_ID = "custom-replication-service";
var MISC_ADMIN_WINDOW = "cq-miscadmin-grid";
var listAgentWindow = null;
var panel_width = 1200;
replicationService = {
 getMsgBox: function(title, msg, buttons, icon, handler) {
  CQ.Ext.Msg.show({
   title: CQ.I18n.getMessage(title),
   msg: CQ.I18n.getMessage(msg),
   buttons: buttons,
   icon: icon,
   width:1000,
   animEl: 'elId',
   fn: handler
  });
 },
 ajaxCall: function(path,method,data) {
  return  $.ajax({
                  type: method || "GET",
                  url: path,
                  data:data,
                  dataType:"JSON",
                  async: false,
                  success: function (result) {
                      return result;
                  }
              });

 },
 showWindow: function() {
  replicationService.gridPanel();
  listAgentWindow.show();
 },
 gridPanel: function() {
  var gridPanelDetail = replicationService.getJsonReader();
  listAgentWindow = new CQ.Ext.Window({
   title: "<b>Active Replication Agents</b><br>",
   layout: "fit",
   hidden: true,
   maximizable: true,
   collapsible: true,
   renderTo: 'CQ',
   modal: true,
   origin: 'center',
   width: panel_width,
   height: 200,
   x: ($(window).width() - panel_width) / 2,
   y: 100,
   closeAction: 'destroy',
   items: gridPanelDetail,
   listeners: {
    beforeshow: function() {
     gridPanelDetail.getStore().load();
    }
   }
  });

 },
 getJsonReader: function() {

  var jsonReaderObj = new CQ.Ext.data.JsonReader({
   root: 'hits',
   totalProperty: 'total',
   fields: [{
     name: 'path',
     type: 'string'
    },
    {
     name: 'jcr:path',
     type: 'string'
    },
    {
     name: 'jcr:title',
     type: 'string'
    },
    {
     name: 'transportUri',
     type: 'string'
    }
   ]
  });

  var dataStore = new CQ.Ext.data.GroupingStore({
   proxy: new CQ.Ext.data.HttpProxy({
    url: CQ.HTTP.externalize("/bin/querybuilder.json"),
    method: "GET"
   }),
   baseParams: {
    "p.hits": "full",
    "path": "/etc/replication",
    "1_property": "sling:resourceType",
    "1_property.value": "cq/replication/components/agent",
    "2_property": "enabled",
    "2_property.value": "true",
    "3_property": "serializationType",
    "3_property.value": "durbo"
   },
   paramNames: {
    start: 'p.offset',
    limit: 'p.limit',
    sort: 'sort',
    dir: 'dir'
   },
   reader: jsonReaderObj,
   autoLoad: {
    params: {
     'p.start': 0,
     'p.limit': 50
    }
   }
  });
  var cmModel = new CQ.Ext.grid.ColumnModel({
   columns: [
   {
        header: CQ.I18n.getMessage("Agent ID"),
        dataIndex: 'jcr:path',
        width: 400,
        renderer : replicationService.getAgentId,

   },
   {
     header: CQ.I18n.getMessage("Agent Title"),
     dataIndex: 'jcr:title',
     width: 300
    },
    {
     header: CQ.I18n.getMessage("Host"),
     dataIndex: 'transportUri',
     width: 400
    },
    {
     header: 'Replicate ',
     xtype: 'actioncolumn',
     width: 100,
     items: [{

      icon: '/apps/custom-replication-plugin/clientlibs/icons/upload16x16.png',
      tooltip: 'Replicate',
      handler: function(grid, rowIndex, colIndex) {
       var canReplicate = replicationService.ajaxCall("/apps/custom-replication-plugin/actions");
       if (canReplicate) {
       var selctedData = replicationService.getSelections();
        if (selctedData &&  selctedData.pathSelected) {
         var pathSelected = selctedData.pathSelected;
         if (pathSelected && pathSelected.length > 0) {
          var agentSelected = selctedData.agentSelected;
          var msg;
          if (pathSelected.length == 1 && agentSelected && agentSelected.length > 0) {
            var agentId = replicationService.getAgentId(agentSelected[0].json["jcr:path"],'','');
            var agentDetatils = agentSelected[0].json["jcr:title"] +"("+agentId+")";
           msg = "Replicate (" + (pathSelected[0].json["title"] ? pathSelected[0].json["title"] : pathSelected[0].json["path"]) + ") to replication "+agentDetatils;
          } else if (pathSelected.length > 1 && agentSelected && agentSelected.length > 0) {
           msg = "Multiple path selected and target replication "+agentDetatils
          }
          if (msg) {
           replicationService.getMsgBox('Do you want to Replicate ?', msg, CQ.Ext.Msg.YESNO, CQ.Ext.MessageBox.QUESTION, replicationService.confirmDialogActions);
          } else {
           replicationService.getMsgBox("Warning", "Replication agent not selected!", CQ.Ext.Msg.OK, CQ.Ext.MessageBox.WARNING, '');
          }

         }

        }

       } else {
        replicationService.getMsgBox("Error", "Access denied !", CQ.Ext.Msg.OK, CQ.Ext.Msg.ERROR);


       }
       var rec = dataStore.getAt(rowIndex);
      }
     }]
    }



   ],
  });
  var selModel = new CQ.Ext.grid.CheckboxSelectionModel({
   singleSelect: true

  });

  var filters = {
   ftype: 'filters',
   encode: false,
   local: true,

  }



  var editorGridPanel = new CQ.Ext.grid.EditorGridPanel({
   id: AGENT_GRID_PANEL,
   store: dataStore,
   stateful: false,
   colModel: cmModel,
   clicksToEdit: 2,
   sm: selModel,
   autoScroll: true,
   frame: false,
   features: [filters],
   listeners: {
     cellclick: function (grd, rowIndex, colIndex, e)
     {

        if(colIndex==0)//action takes place only  when col=
           {
             var rec = grd.getStore().getAt(rowIndex);
             var agentId = replicationService.getAgentId(rec.get('jcr:path'));
             var response = replicationService.ajaxCall(REPLICATION_SERVICE_STATUS,"GET",{'agentId':agentId});
             replicationService.getMsgBox(agentId+ " ==> Queue Status" ,response.responseText || "Queue is idle",CQ.Ext.Msg.OK, CQ.Ext.MessageBox.INFO, '')

           }
     }
   },
   bbar: new CQ.Ext.PagingToolbar({
    pageSize: 25,
    store: dataStore,
    displayInfo: true,
    displayMsg: 'Displaying Items {0} - {1} of {2}',
    emptyMsg: "No Items to display"

   }),

  });


  return editorGridPanel;
 },
 getSelections : function(){
  var adminGrid = CQ.Ext.getCmp(MISC_ADMIN_WINDOW);
  var agentGrid = CQ.Ext.getCmp(AGENT_GRID_PANEL);
  var agentSelected = agentGrid.getSelectionModel().getSelections() || [];
  var pathSelected = adminGrid.getSelectionModel().getSelections() || [];

    var data = {
        pathSelected : pathSelected,
        agentSelected : agentSelected
    };

    return data;
 },
 confirmDialogActions: function(btn) {
  if (btn === "yes") {
  var selctedData = replicationService.getSelections();
  var pathArr = [];
        for(var i=0 ; i < selctedData.pathSelected.length ;i++){
           pathArr[i] = selctedData.pathSelected[i].json["path"];
         }

       var data ={
        pathSelected : pathArr,
        agentSelected: selctedData.agentSelected[0].json["jcr:path"],
        agentId: replicationService.getAgentId(selctedData.agentSelected[0].json["jcr:path"])
       }

       CQ.Ext.Ajax.request({
        url: REPLICATION_SERVICE_URL,
        params: data,
        success: function(response, opts) {}
       })


  } else {

  }
 },
 getAgentId : function(val, meta, record){
         if(val) {
              var pathArr = val.split("/");
              return pathArr[pathArr.length-2];
         }
         return val || "";
     }

},