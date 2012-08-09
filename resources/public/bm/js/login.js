// login.js

$(document).ready(function() {

    var emailId = '#bm-user';
    var passwordId = '#bm-pass';
    var confirmId = '#join-confirm';

    var loginButtonId = '#bm-login';
    var joinButtonId = '#bm-join';

    var errorBlockId = '#bm-login-errors';

    var successUrl = '/bm/';
    var joinUrl = '/bm/api/join/';

    var loginMode = true;

    // Utilities

    function trim(stringToTrim) {
        return stringToTrim.replace(/^\s+|\s+$/g, "");
    }

    function gotoHome() {
        window.location.href = successUrl;
    }

    function focusForm() {
        $(emailId).focus();
    }

    function showError(msg) {
        var defaultMsg = "We don't recognize your account. Try again?";
        $('#bm-error-pane').html(msg == null ? defaultMsg : msg);
        $(errorBlockId).fadeIn(500);
    }

    function hideError() {
        $(errorBlockId).fadeOut(500);
    }

    function getEmail() {
        return trim($(emailId).val());
    }

    function getPassword() {
        return trim($(passwordId).val());
    }

    function getConfirmPassword() {
        return trim($(confirmId).val());
    }

    function callAuth(onSuccess, onFailure) {
        var post = {
            type: 'POST',
            url: '/bm/api/auth/',
            data: { 'email' : getEmail(), 'password' : getPassword() },
            success: onSuccess,
            error: onFailure,
            dataType: 'json'
        };

        $.ajax(post);
    }

    function callJoin(onSuccess, onFailure) {
        var post = {
            type: 'POST',
            url: joinUrl,
            data: { 'email' : getEmail(), 'password' : getPassword() },
            success: onSuccess,
            error: onFailure,
            dataType: 'json'
        };

        $.ajax(post);
    }

    function join() {

        loginMode = false;

        $('#confirmer').fadeIn(500);
        $('#join-confirm').focus();

        function onSuccess(data, textStatus, jqXHR) {
            // If success, then I'm going to assume the user hit the wrong
            // button, so I'll just log him in.
            console.log("join-success");
            gotoHome();
        }

        function onFailure(jqXHR, textStatus, errorThrown) {
            console.log("failure");

            // Check validity of passwords. If no go, show the error
            // form. If we're good, submit the join form again.

            if (getPassword().length < 8) {
                $(passwordId).focus();
                showError("Password needs to be > 8 characters.");
                return;
            }

            if (getPassword() != getConfirmPassword()) {
                $(confirmId).focus();
                showError("Password confirmation must match.");
                return;
            }

            function joinSuccess() {
                gotoHome();
            }

            function joinFail(jqXHR, textStatus, errorThrown) {
                showError("Unable to join at this time. " + textStatus);
            }

            callJoin(joinSuccess, joinFail);
        }

        callAuth(onSuccess, onFailure);
    }

    function login() {

        loginMode = true;

        function onSuccess(data, textStatus, jqXHR) {
            gotoHome();
        }

        function onError(jqXHR, textStatus, errorThrown) {
            showError();
            focusForm();
        }

        callAuth(onSuccess, onError);
    }

    function onKeyEvent(event) {

        hideError();

        if (event.keyCode !== 13)
            return false;

        // If we're in log-in mode, pressing the return
        // key will trigger the log in attempt.

        // If we're in join mode, pressing the return
        // key will trigger the join attempt.

        var mode = loginMode ? loginButtonId : joinButtonId;

        $(mode).focus();
        $(mode).click();
    }

    $(emailId).focus();

    $(loginButtonId).click(function() { login(); });

    $(joinButtonId).click(function() { join(); });

    $(emailId).keyup(onKeyEvent);
    $(passwordId).keyup(onKeyEvent);
    $(confirmId).keyup(onKeyEvent);

    // Disable submit
    $('#login-form form').submit(function() { return false; });
});
