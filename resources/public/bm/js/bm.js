// bm.js

// Should build a memory map in db.clj to get things working initially.

$(document).ready(function() {

    function renderBookmarks(bookmarks) {
        var html = "";
        for (var i = 0; i < bookmarks.length; i++) {
            var b = bookmarks[i];
            html += "<tr>";
            html += " <td title='" + b.id + "'>" + b.id.substr(b.id.length -4, 4) + "</td>";
            html += " <td><a href='" + b.url + "'>" + b.desc + "</a></td>";
            html += " <td><a href='" + b.url + "'>" + b.url + "</a></td>";
            html += " <td>" + b.tags + "</td>";
            html += "</tr>";
        }

        $('#bm-table tr:gt(0)').remove();
        $('#bm-table-header').after(html);
    }

    var lastTerms = "!";

    function postTerms() {
        var terms = $('#search-terms').val();
        if (terms == lastTerms)
            return;
        lastTerms = terms;
        $.post('/bm/api/search/', { "terms" : terms }, function(data) {
            // console.log(data);
            renderBookmarks(data);
        });
    }

    $('#search-terms').focus();

    $('#bm-new').click(function() {
        $('#bm-form').show();
        $('#bm-search').hide();
    });

    $('#bm-form-itself').submit(function() { return false; });

    $('#bm-cancel').click(function() {
        $('#bm-form').hide();
        $('#bm-search').show();
    });

    $('#search-terms').keyup(function() {
        postTerms();
    });

    postTerms();
});
