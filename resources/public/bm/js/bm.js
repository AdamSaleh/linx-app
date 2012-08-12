// bm.js

$(document).ready(function() {

    function renderTags(tags) {
        return tags.join(", ");
    }

    function renderUrl(url) {
        if (! url)
            return "&lt;null&gt;";

        if (url.length > 30)
            return url.substr(0, 30) + "...";
        else
            return url;
    }

    function renderBookmarks(bookmarks) {
        var html = "";
        for (var i = 0; i < bookmarks.length; i++) {
            var b = bookmarks[i];
            html += "<tr>";
            html += " <td title='" + b.id + "'>" + b.id.substr(b.id.length -4, 4) + "</td>";
            html += " <td><a title='" + b.url + "' href='" + b.url + "'>" + b.desc + "</a></td>";
            html += " <td><a title='" + b.url + "' href='" + b.url + "'>" + renderUrl(b.url) + "</a></td>";
            html += " <td>" + renderTags(b.tags) + "</td>";
            html += "</tr>";
        }

        $('#bm-table tr:gt(0)').remove();
        $('#bm-table-header').after(html);
    }

    function showBookmarkFormError(errors) {
        $('#bm-form-errors').html("<p>" + errors + "</p>");
    }

    function clearBookmarkFormError() {
        showBookmarkFormError("");
    }

    function showBookmarkForm() {
        $('#bm-search').hide();
        $('#bm-form').show();
        $('#bm-url').focus();
        clearBookmarkFormError();
    }

    function hideBookmarkForm() {
        $('#bm-form').hide();
        $('#bm-search').show();
        $('#search-terms').focus();
    }

    function clearBookmarkForm() {
        $('#bm-title').val('');
        $('#bm-url').val('');
        $('#bm-tags').val('');
        clearBookmarkFormError();
    }

    var lastTerms = "!";

    function postSearchTerms() {
        var terms = $('#search-terms').val();
        if (terms == lastTerms)
            return;
        lastTerms = terms;
        $.post('/bm/api/search/', { "terms" : terms }, function(data) {
            renderBookmarks(data);
        });
    }

    function postBookmark() {
        var data = {
            "name" : $('#bm-title').val(),
            "addr" : $('#bm-url').val(),
            "tags" : $('#bm-tags').val()
        };

        function onSuccess(data, textStatus, jqXHR) {
            // Delay invoking a refresh of the search terms
            // to give time for the server to save the data.
            lastTerms = "!";
            setTimeout(postSearchTerms, 1000);
            hideBookmarkForm();
            clearBookmarkForm();
        }

        function onError(jqXHR, textStatus, errorThrown) {
            console.log(jqXHR);
            showBookmarkFormError(textStatus + ":" + errorThrown);
        }

        $.ajax({
            type: 'POST',
            url: '/bm/api/bookmark/',
            data: data,
            success: onSuccess,
            error: onError,
            dataType: 'json'
        });
    }

    $('#search-terms').focus();

    $('#bm-new').click(function() {
        showBookmarkForm();
    });

    $('#bm-form-itself').submit(function() { return false; });

    $('#bm-add').click(function() {
        postBookmark();
    });

    $('#bm-cancel').click(function() {
        hideBookmarkForm();
    });

    $('#search-terms').keyup(function() {
        postSearchTerms();
    });

    postSearchTerms();
});
