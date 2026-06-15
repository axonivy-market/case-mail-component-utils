// makes the validation message of "advanced" p:fileUpload to show in a growl
(function($) {
	var originalShowFunction = $.fn.show;
	$.fn.show = function() {
		this.trigger("show");
		return originalShowFunction.apply(this, arguments);
	};
})(jQuery);

$(document).on("show", ".ui-fileupload-content>.ui-messages", function() {
	$(this).children().hide().find("li").each(function(index, message) {
		var $message = $(message);
		if (!$message.hasClass("processed")) {
			PF("validationGrowlWidget").renderMessage({
				severity: 'error',
				summary: $(".ui-messages-error-summary", $message).text(),
				detail: $(".ui-messages-error-detail", $message).text()
			});
			$message.addClass("processed");
		}
	});
});