
/* Hides button with provided id withing the pe:documentViewer component with provided title */
function pdfViewerHideButton(buttonId, iframeTitle) {
	$('iframe[title="' + iframeTitle + '"]').on('load',
		function() {
			var head = $(this).contents().find('head');
			var css = '<style type="text/css">#' + buttonId + '{display:none !important};</style>';
			$(head).append(css);
		});
}

function applyMoveEvent(galleriaWidgetVar) {
    // Find the content div of Galleria
    var contentDiv = PF(galleriaWidgetVar).getJQ().find('.ui-galleria-content');
	
    // Apply absolute position and center the image with transform
	contentDiv.css({
	        'position': 'absolute',
	        'left': '50%', // Center horizontally
	        'top': '50%',  // Center vertically
	        'transform': 'translate(-50%, -50%)', // Offset to make the center the reference point
	        'cursor': 'move',
	        'user-select': 'none',
	    });

    // Handle mousedown to start dragging
    contentDiv.mousedown(function(event) {
        event.preventDefault(); // Prevent text selection or other default behaviors
        var element = $(this);

        // Get the bounding rectangle of the content div for accurate positioning
        var rect = element[0].getBoundingClientRect();
        // Calculate initial mouse position relative to the image's top-left corner
        // getBoundingClientRect accounts for transforms and viewport positioning
        var offsetX = event.clientX - rect.left - (rect.width / 2);
        var offsetY = event.clientY - rect.top - (rect.height / 2);

        // Handle mousemove to update image position
        $(document).mousemove(function(e) {
            // Calculate new position relative to the viewport
            element.css({
                'left': (e.clientX - offsetX) + 'px',
                'top': (e.clientY - offsetY) + 'px'
            });
        });

        // Stop dragging on mouseup
        $(document).mouseup(function() {
            $(document).off('mousemove');
        });
    });
}