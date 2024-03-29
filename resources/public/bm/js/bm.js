// bm.js

$(document).ready(function() {

    // Store for the most recent search results.

    var LINX = {};
    var lastTerms = "!";

    //-------------------------------------------------------------------------
    // Display status messages
    //-------------------------------------------------------------------------

    //-------------------------------------------------------------------------
    // Refresh the search results periodically in case new bookmarks come in
    // while we're watching. (Would be more fun for the server to push these
    // up via web sockets.)
    //-------------------------------------------------------------------------

    function startRefresher() {

        var refreshInterval = 30000;
        var timeoutInterval = 2000;

        function hideActivity() {
            $('.activity').fadeOut(1000, function() { visible = false; });
        }

        function showActivity() {
            $('.activity').fadeIn(1000, function() {
                setTimeout(hideActivity, timeoutInterval);
            });
        }

        function refresh() {
            showActivity();
            forcePostSearchTerms();
        }

        setInterval(refresh, refreshInterval);
    }

    //-------------------------------------------------------------------------
    // Bookmark editing
    //-------------------------------------------------------------------------

    function updateBookmark(bookmarkId) {

        function onSuccess(data, textStatus, jqXHR) {
            hideEditForm();
            forcePostSearchTerms();
        }

        function onFailure(jqXHR, textStatus, errorThrown) {
            console.log("Unable to update bookmark. Sorry.");
            hideEditForm();
        }

        var data = {
            url : $('#edit-url').val(),
            desc : $('#edit-desc').val(),
            tags : $('#edit-tags').val()
        };

        $.ajax({
            type: 'POST',
            url: '/bm/api/bookmark/' + bookmarkId + '/',
            success: onSuccess,
            error: onFailure,
            data: data,
            dataType: 'json'
        });
    }

    function showEditForm(bookmarkId) {
        var b = LINX[bookmarkId];
        $('#edit-id').val(bookmarkId);
        $('#edit-url').val(b.url);
        $('#edit-desc').val(b.desc);
        $('#edit-tags').val(b.tags);
        $('#edit-form').fadeIn(500, function() {
            $('#edit-desc').focus();
        });
    }

    function hideEditForm() {
        $('#edit-form').fadeOut(500, function() {
            $('#edit-id').val('');
            $('#edit-url').val('');
            $('#edit-desc').val('');
            $('#edit-tags').val('');
        });
    }

    function submitEditForm() {
        updateBookmark($('#edit-id').val());
    }

    //-------------------------------------------------------------------------
    // Adding new bookmarks
    //-------------------------------------------------------------------------

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

    //-------------------------------------------------------------------------
    // Delete bookmark
    //-------------------------------------------------------------------------

    function deleteBookmark(bookmarkId) {

        function onSuccess(data, textStatus, jqXHR) {
            lastTerms = "!";
            postSearchTerms();
        }

        function onFailure(jqXHR, textStatus, errorThrown) {
            console.log("Unable to delete bookmark. Sorry.");
        }


        $.ajax({
            type: 'DELETE',
            url: '/bm/api/bookmark/' + bookmarkId + '/',
            success: onSuccess,
            error: onFailure
        });
    }

    //-------------------------------------------------------------------------
    // Search Results
    //-------------------------------------------------------------------------

    function renderTags(tags) {
        try {
            return tags.join(", ");
        }

        catch (yikes) {
            return tags;
        }
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
        LINX = {};

        for (var i = 0; i < bookmarks.length; i++) {
            var b = bookmarks[i];
            html += "<tr>";
            html += "<td><a title='" + b.url + "' href='" + b.url + "'>" +
                b.desc + "</a></td>";
            html += "<td><a title='" + b.url + "' href='" + b.url + "'>" +
                renderUrl(b.url) + "</a></td>";
            html += "<td>" + renderTags(b.tags) + "</td>";
            html += "<td class='bm-id' title='" + b.id + "'>";
            html += " <span class='mark-id'>" + b.id.substr(b.id.length -4, 4) + "</span>";
            html += " <span class='controls'>";
            html += "  <span class='control edit-control' id='" + b.id + "'>&#x2710;</span>";
            html += "  <span class='control delete-control' id='" + b.id + "'>X</span>";
            html += " </span>";
            html += "</td>";
            html += "</tr>";
            LINX[b.id] = b;
        }

        $('#bm-table tr:gt(0)').remove();
        $('#bm-table-header').after(html);

        $('.delete-control').click(function(event) {
            var msg = "Do you want to delete '" + LINX[this.id].desc + "'?";
            if (confirm(msg)) {
                deleteBookmark(this.id);
            }
        });

        $('.edit-control').click(function(event) {
            showEditForm(this.id);
        });
    }

    function forcePostSearchTerms() {
        postSearchTerms("force");
    }

    function postSearchTerms(force) {
        var terms = $('#search-terms').val();
        if (typeof force == "undefined") {
            if (terms == lastTerms)
                return;
            lastTerms = terms;
        };

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

            hideBookmarkForm();
            clearBookmarkForm();
            forcePostSearchTerms();
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

    //-------------------------------------------------------------------------

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

    $('#edit-form form').submit(function() { return false; });

    $('#edit-cancel').click(function() {
        hideEditForm();
    });

    $('#edit-update').click(function() {
        submitEditForm();
    });

    postSearchTerms();
    startRefresher();
});
