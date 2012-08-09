// login.js

$(document).ready(function() {

    var emailId = '#bm-user';
    var passwordId = '#bm-pass';
    var confirmId = '#join-confirm';

    var loginButtonId = '#bm-login';
    var joinButtonId = '#bm-join';

    var errorBlockId = '#bm-login-errors';

    var successUrl = "/bm/";

    var loginMode = true;

    function gotoHome() {
        window.location.href = successUrl;
    }

    function focusForm() {
        $(emailId).focus();
    }

    function showError() {
        $(errorBlockId).fadeIn(500);
    }

    function hideError() {
        $(errorBlockId).fadeOut(500);
    }

    function getEmail() {
        return $(emailId).val();
    }

    function getPassword() {
        return $(passwordId).val();
    }

    function authentic(onSuccess, onFailure) {
        var data = {
            "email" : getEmail(),
            "password" : getPassword()
        };

        $.ajax({
            type: 'POST',
            url: '/bm/api/auth/',
            data: data,
            success: onSuccess,
            error: onFailure,
            dataType: 'json'
        });
    }

    function join() {

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
            $('#join-confirm').focus();
        }

        authentic(onSuccess, onFailure);
    }

    function login() {

        function onSuccess(data, textStatus, jqXHR) {
            gotoHome();
        }

        function onError(jqXHR, textStatus, errorThrown) {
            showError();
            focusForm();
        }

        authentic(onSuccess, onError);
    }

    function submitLoginForm() {
        $(loginButtonId).focus();
        $(loginButtonId).click();
    }

    function joinLoginForm() {
        $(joinButtonId).focus();
        $(joinButtonId).click();
    }

    function onKeyEvent(event) {
        hideError();
        if (event.keyCode !== 13)
            return false;

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
