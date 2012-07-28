// login.js

$(document).ready(function() {

    var successUrl = "/bm/";

    function gotoHome() {
        window.location.href = successUrl;
    }

    function focusForm() {
        $('#bm-user').focus();
    }

    function showError() {
        $('#bm-login-errors').fadeIn(500);
    }

    function hideError() {
        $('#bm-login-errors').fadeOut(500);
    }

    function getEmail() {
        return $('#bm-user').val();
    }

    function getPassword() {
        return $('#bm-pass').val();
    }

    function authenticate() {

        var data = {
            "email" : getEmail(),
            "password" : getPassword()
        };

        function onSuccess(data, textStatus, jqXHR) {
            gotoHome();
        }

        function onError(jqXHR, textStatus, errorThrown) {
            showError();
            focusForm();
        }

        $.ajax({
            type: 'POST',
            url: '/bm/api/auth/',
            data: data,
            success: onSuccess,
            error: onError,
            dataType: 'json'
        });
    }

    function onKeyEvent(event) {
        hideError();
        if (event.keyCode == 13) {
            $('#bm-login').focus();
            $('#bm-login').click();
        };
    }

    $('#bm-user').focus();

    $('#bm-login').click(function() {
        authenticate();
    });

    $('#bm-user').keyup(onKeyEvent);

    $('#bm-pass').keyup(onKeyEvent);

    // Turn off form submit, we'll handle it here.
    $('#bm-form-itself').submit(function() {
        return false;
    });

    console.log("done loading.");
});
