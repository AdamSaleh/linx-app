// bookmarklet.js

(function() {

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

    function postBookmark() {
        var url = $('#a1-code').attr('hb') + '/bm/bookmark/';

        var data = {
            "name" : $('#a1-desc').val(),
            "addr" : $('#a1-addr').val(),
            "tags" : $('#a1-tags').val(),
            "cuid" : $('#a1-code').attr('uid')
        };

        function onSuccess(data, textStatus, jqXHR) {
            cleanUp();
        }

        function onError(jqXHR, textStatus, errorThrown) {
            console.log("----error----");
            console.log(jqXHR);
            console.log(textStatus);
            console.log(errorThrown);
            console.log(jqXHR.status);
            cleanUp();
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

        $('#a1').delay(100).fadeIn(200, function() {
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
        }

        var script = document.createElement( 'script' );
        script.src = 'http://code.jquery.com/jquery-1.7.2.min.js';
        script.onload=capture;
        document.body.appendChild(script);
    }

    loadJquery();

}());