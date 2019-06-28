(function() {
 var TIMEOUT = 500;
 var INTERVAL = setInterval(function() {
  var grid = CQ.Ext.getCmp(MISC_ADMIN_WINDOW);
  if (grid) {
   clearInterval(INTERVAL);
   var actionResponse = replicationService.ajaxCall(CUSTOM_ACTIONS);
   if (actionResponse && actionResponse.responseJSON) {
    var btnLabel = actionResponse.responseJSON["jcr:title"];
    var toolBar = grid.getTopToolbar();

    toolBar.insertButton(10, new CQ.Ext.Toolbar.Button({
     text: btnLabel,
     id: CUSTOM_REPLICATION_BUTTON_ID,
     cls: CUSTOM_REPLICATION_BUTTON_ID,
     icon:"/apps/custom-replication-plugin/clientlibs/icons/network.png",
     handler: function() {
      var pathSelections = grid.getSelectionModel().getSelections();
      if (pathSelections && pathSelections.length > 0) {
       replicationService.showWindow();
      } else {
       replicationService.getMsgBox("Warning", "Item not selected!", CQ.Ext.Msg.OK, CQ.Ext.MessageBox.WARNING, '');
      }
     }
    }));

    grid.doLayout();
   } else {
    console.debug("Don't have permission!");
   }

  }
 }, TIMEOUT);

})();