
var path =[],
function excute(){
    for (var index = 0; index <path.length; index++){
         var url = "https://www.merckgroup.com"+path[index];
         var successArr = [];
         var failure = [];
          $.ajax({
                            type: "GET",
                            url: url,
                            dataType:"JSON",
                            async: false,
                            success: function (result) {
                                console.log(result)
                            }
                        });
    }

}



var ScriptRunner = {
    SCAFFOLD_URL: "/mnt/overlay/commerce/gui/content/products/properties.html?item=",
    LOG_TEMPLATE: null,
    ROW_OFFSET: 4,
    init: function() {
        if (document.getElementById("processListing")) {
            ScriptRunner.processTable = document.getElementById("processListing");
            window.top.ScriptRunner = ScriptRunner;
            ScriptRunner.LOG_TEMPLATE = $('.logs').clone();
            window.top.document.getElementById("startButton").on('click', ScriptRunner.processDefinitionSelected);
            window.top.document.getElementById("reset").on('click', ScriptRunner.reset);
        }
    },
    processDefinitionSelected: function(event) {
        if (event) {
            var excelFile = window.top.document.getElementById("excelFile");
            var baseFolder = window.top.document.getElementById("baseFolder");
            if (excelFile && excelFile.value && baseFolder && baseFolder.value) {
                var formData = {
                    excelFile: excelFile.value,
                    baseFolder: baseFolder.value,

                }
                ScriptRunner.submitForm();
                window.top.document.getElementById("startProcessWizard").next();
            }
        }
    },
    submitForm: function() {
        $('.logs').hide();
        $('.logs').parent().remove('.logs');
        $('#wait').show();
        var data = new FormData($('#product-importer-form ', window.top.document)[0]);
        var url = Granite.HTTP.getPath() + "/jcr:content.upload.json";

        jQuery.ajax({
            url: url,
            method: "POST",
            dataType: "json",
            success: ScriptRunner.logging,
            error: ScriptRunner.error,
            data: data,
            cache: false,
            contentType: false,
            processData: false
        });

    },
    logging: function(response) {
        if (response && !response.ErrorDesc && (response.success || response.failure)) {
            $('.logs').parent().append(ScriptRunner.LOG_TEMPLATE);
            $('.logs').show();
            var totalProducts = 0;
            var totalSuccess = 0;
            var totalFail = 0;

            if (response.success && response.success.length > 0) {
                var successWindow = $('#success-logs-messages');
                totalSuccess = response.success.length;
                $('#sucess-logs-accordion').show();
                successWindow.empty();
                var sucessList = response.success;
                var ul = "<ul>"
                for (var index = 0; index < sucessList.length; index++) {
                    ul += "<li><span>" + sucessList[index][0] + "<a href='" + ScriptRunner.SCAFFOLD_URL + "" + sucessList[index][0] + "' target='_blank'>(View)</a></span></li>"
                }
                ul += "</ul>"
                $(successWindow).append(ul);


            }else{
                $('#sucess-logs-accordion').hide();
            }
            if (response.failure && response.failure.length > 0) {
                totalFail = parseInt(response.failure.length ? response.failure.length : 0)
                var failureWindow = $('#failure-logs-messages');
                $('#failure-logs-accordion').show();
                failureWindow.empty();
                var failureList = response.failure;
                var ul = "<ul>";
                for (var index = 0; index < failureList.length; index++) {
                    var row = parseInt(failureList[index].row ? failureList[index].row : 0) + parseInt(ScriptRunner.ROW_OFFSET);
                    ul += "<li><span> Row <strong>" + row + " </strong> is not imported because of <strong>" + failureList[index].errMsg + "</strong> </span></li>";
                }

                ul += "</ul>"
                $(failureWindow).append(ul);

            }else{
                $('#failure-logs-accordion').hide();
            }
            totalProducts = parseInt(totalSuccess) + parseInt(totalFail);
            if (totalProducts > 0) {
                var totalMessage = '<h3> <a href="#success-logs-messages">' + totalSuccess + '</a> out of ' + totalProducts + ' products imported successfully and <a href="#failure-logs-messages">' + totalFail + '</a> failed</h3>'
                $('#total-message').html(totalMessage);
                $('#total-message').show();
            }
        } else {
            $('.logs').hide();
            var ui = $(window).adaptTo('foundation-ui');
            ui.alert('Notification', response.ErrorDesc || "Error while processing the file.", 'error');
        }
        $('#wait').hide();
    },
    error: function(e) {
        $('#wait').hide();
        console.log("Error condition detected -- check logs!");
        console.log(e);
        $('.logs').hide();
        var ui = $(window).adaptTo('foundation-ui');
        ui.alert('Notification', "Error while processing the file.", 'error');
    },
    reset: function() {
        var excelFile = window.top.document.getElementById("excelFile").value = "";
        var baseFolder = window.top.document.getElementById("baseFolder").value = "";
    }
};

jQuery('#processListing').ready(function() {
    ScriptRunner.init();
});