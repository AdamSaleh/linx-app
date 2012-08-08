// bookmarklet.js

(function() {

    function postBookmark() {
        var root = $('#a1-code').attr('hb');
        var url = root + '/bm/bookmark/';
        var login = root + '/bm/login/';

        var data = {
            'name' : $('#a1-desc').val(),
            'addr' : $('#a1-addr').val(),
            'tags' : $('#a1-tags').val(),
            'cuid' : $('#a1-code').attr('uid')
        };

        function onSuccess(data, textStatus, jqXHR) {
            cleanUp();
        }

        function onError(jqXHR, textStatus, errorThrown) {
            var html = "";
            html += "<div class='a1-error'>";
            html += "Unable to submit link. Maybe try ";
            html += "<a href='" + login + "'>re-logging</a> in?";
            html += "</div>";

            console.log('----error----');
            console.log(jqXHR);
            console.log(textStatus);
            console.log(errorThrown);
            console.log(jqXHR.status);

            $('#a1').html(html);
            setTimeout(cleanUp, 5000);
//            cleanUp();
        }

        console.log(url);
        console.log(data);

        $.ajax({
            type: 'POST',
            url: url,
            data: data,
            success: onSuccess,
            error: onError,
            dataType: 'json'
        });
    }

    function capture() {
        if ($('#a1').length > 0) {
            $('#a1').remove();
        }

        function labelWidget(id, prompt, value) {
            var html = "";
            html += "<label for='" + id + "'>";
            html += "  <span class='prompt'>" + prompt + "</span>";
            html += "  <span class='widget'>";
            html += "    <input id='" + id + "' value='" + value + "'/>";
            html += "  </span>";
            html += "</label>";
            return html;
        }

        loadStyles();

        var d = document;

        var html = "";
        html += "<div id='a1'>";
        html += "<h1>Add a new link?</h1>";
        html += "<div class='a1-widgets'>";
        html += labelWidget('a1-desc', 'desc:', d.title);
        html += labelWidget('a1-addr', 'addr:', window.location.href);
        html += labelWidget('a1-tags', 'tags:', '');
        html += "</div>";
        html += "<div class='a1-controls'>";
        html += "<a id='a1-save'>save</a> ";
        html += "<a id='a1-dismiss'>dismiss</a>";
        html += "</div>";
        html += "</div>";

        $('body').append(html);

        $('#a1-save').click(function() {
            postBookmark();
        });

        $('#a1-dismiss').click(function() {
            cleanUp();
        });

        function keyHandler(event) {
            if (event.keyCode == 13)
                postBookmark();
            if (event.keyCode == 27)
                cleanUp();
        }

        $('#a1-desc').keyup(keyHandler);
        $('#a1-addr').keyup(keyHandler);
        $('#a1-tags').keyup(keyHandler);

        $('#a1').delay(100).fadeIn(200, function() {
            $('#a1-tags').focus();
        });
    }

    function showForm() {
        $('#a1').show();
    }

    function loadStyles() {
        var href = $('#a1-code').attr('hb') + '/bm/css/bookmarklet.css';

        var l = "<link id='a1-style' rel='stylesheet' ";
            l += "type='text/css' href='" + href + "'/>";
        $('head').append(l);
    }

    function cleanUp() {
        if ($ !== window.jQuery)
            return;

        $('#a1').fadeOut(500, function() {
            $('#a1').remove();
            $('#a1-style').remove();
            $('#a1-code').remove();
        });
    }

    function loadJquery() {
        try {
            if ( $ === window.jQuery) {
                capture();
                return;
            }
        }

        catch (exception) {
            // Ooo! Evil!
        }

        var script = document.createElement( 'script' );
        script.src = 'http://code.jquery.com/jquery-1.7.2.min.js';
        script.onload=capture;
        document.body.appendChild(script);
    }

    loadJquery();

}());
