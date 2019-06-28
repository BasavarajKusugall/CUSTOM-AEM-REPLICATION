(function() {
    var INTERVAL = setInterval(function() {
        var grid = CQ.Ext.getCmp("cq-miscadmin-grid");

        if (grid) {
            clearInterval(INTERVAL);

            var toolBar = grid.getTopToolbar();
            var createInPath = "/content/we-retail";

            toolBar.insertButton(10, new CQ.Ext.Toolbar.Button({
                text: 'Custom Replication',
                cls: "custom-replication-service",
                iconCls: "cq-siteadmin-create-icon",
                handler: function() {
                    replicationService.showWindow();
                }
            }));

            grid.doLayout();
        }
    }, 250);
})();
var listAgentWindow = null;
var panel_width = 1200;
replicationService = {
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
                    name: 'agentName',
                    type: 'string'
                },
                {
                    name: 'host',
                    type: 'string'
                },
                {
                    name: 'queue',
                    type: 'string'
                }
            ]
        });

        var dataStore = new CQ.Ext.data.GroupingStore({
            proxy: new CQ.Ext.data.HttpProxy({
                url: '/mm/mis/subs.getSapInOutData.json',
                method: "GET"
            }),
            reader: jsonReaderObj,
            pageSize: 50

        });
        var cmModel = new CQ.Ext.grid.ColumnModel({
            columns: [{
                    header: CQ.I18n.getMessage("Agent Name"),
                    dataIndex: 'agentName',
                    width: 500
                },
                {
                    header: CQ.I18n.getMessage("Host"),
                    dataIndex: 'host',
                    width: 400
                },
                {
                    header: CQ.I18n.getMessage("Queue Status"),
                    dataIndex: 'queue',
                    width: 100
                },
                {
                    header: 'Replicate ',
                    xtype: 'actioncolumn',
                    width: 100,
                    items: [{

                        icon: '/apps/mmcommons/components/content/mis/subsMisListing/sap_lg.gif',
                        tooltip: 'Replicate',
                        handler: function(grid, rowIndex, colIndex) {
                            var rec = dataStore.getAt(rowIndex);
                            CQ.Ext.MessageBox.alert("info", rec.get('orderNumber') + "&nbsp;&nbsp;Replicated successfully");
                        }
                    }]
                }



            ],
        });
        var selModel = new CQ.Ext.grid.CheckboxSelectionModel({
            singleSelect: false

        });

        var filters = {
            ftype: 'filters',
            encode: false,
            local: true,

        }



        var editorGridPanel = new CQ.Ext.grid.EditorGridPanel({
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
    }

}