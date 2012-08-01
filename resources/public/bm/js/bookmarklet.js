// bookmarklet.js

(function() {

    console.log("start");

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

    console.log("b");

    function kraken() {
        console.log("kraken");
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
        html += "<a id='a1-cancel'>dismiss</a>";
        html += "</div>";
        html += "</div>";

        $('body').append(html);

        console.log("showing.");

        $('#a1-save').click(function() {
            $('#a1').fadeOut(500, cleanUp);
        });

        $('#a1-cancel').click(function() {
            $('#a1').fadeOut(500, cleanUp);
        });

        console.log("fade in:");
        console.log($('#a1'));
        $('#a1').delay(100).fadeIn(200, function() { console.log("fade in complete") });
    }

    console.log("c");

    function showForm() {
        $('#a1').show();
    }

    console.log("d");

    function loadStyles() {
        console.log("load-styles");
        var href = $('#a1-code').attr('hb') + 'css/bookmarklet.css';
        console.log("css at:" + href);
        var l = "<link id='a1-style' rel='stylesheet' type='text/css' href='" + href + "'/>";
        $('head').append(l);
    }

    function cleanUp() {
        console.log("cleanup");
        if ( $ === window.jQuery) {
            $('#a1').remove();
            $('#a1-style').remove();
            $('#a1-code').remove();
        }
    }

    function loadJquery() {
        try {
            console.log("testing for jquery presence");
            if ( $ === window.jQuery) {
                console.log("found it, rendering.");
                kraken();
                return;
            }
            console.log("Didn't find it.");
        }

        catch (exception) {
        }
        console.log("loading jquery");
        var script = document.createElement( 'script' );
        script.src = 'http://code.jquery.com/jquery-1.7.2.min.js';
        script.onload=kraken;
        document.body.appendChild(script);
    }

    console.log("starting jquery");
    loadJquery();

}());
