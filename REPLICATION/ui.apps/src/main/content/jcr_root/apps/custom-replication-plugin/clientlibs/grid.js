var CUSTOM_ACTIONS = "/apps/custom-replication-plugin/actions";
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
   animEl: 'elId',
   fn: handler
  });
 },
 ajaxCall: function(path,response) {
  var url = path + '.infinity.json'

  return  $.ajax({
                  type: "GET",
                  url: url,
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
   columns: [{
     header: CQ.I18n.getMessage("Agent Name"),
     dataIndex: 'jcr:title',
     width: 500
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
       var canReplicate = replicationService.ajaxCall("/apps/custom-replication-plugin/actions") || true;
       if (canReplicate) {
        var adminGrid = CQ.Ext.getCmp(MISC_ADMIN_WINDOW);
        if (adminGrid) {
         var pathSelected = adminGrid.getSelectionModel().getSelections();
         if (pathSelected && pathSelected.length > 0) {
          var agentGrid = CQ.Ext.getCmp(AGENT_GRID_PANEL);
          var agentSelected = agentGrid.getSelectionModel().getSelections();
          var msg;
          if (pathSelected.length == 1 && agentSelected && agentSelected.length > 0) {
           msg = "Item selected (" + pathSelected[0].id + ") and target replication agent(" + agentSelected[0].json["jcr:title"] + ")"
          } else if (pathSelected.length > 1 && agentSelected && agentSelected.length > 0) {
           msg = "Multiple path selected and target replication agent(" + agentSelected[0].json["jcr:title"] + ")"
          }
          if (msg) {
           replicationService.getMsgBox('Do you want to Replicate ?', msg, CQ.Ext.Msg.YESNOCANCEL, CQ.Ext.MessageBox.QUESTION, replicationService.confirmDialogActions);
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
 confirmDialogActions: function(btn) {
  if (btn === "yes") {
   CQ.Ext.Ajax.request({
    url: 'custom/replication/service.json',
    params: {
     pathSelected: "",
     agentSelected:""
    },
    success: function(response, opts) {}
   })


  } else {

  }
 },

},