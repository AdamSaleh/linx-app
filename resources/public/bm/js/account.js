// account.js

$(document).ready(function() {

    var emailId = '#email';
    var newPassId = '#new-pass';
    var confirmPassId = '#confirm';

    var accountFormId = '#account-form';
    var errorBlockId = '#bm-form-errors';

    var updateButtonId = '#update';
    var cancelButtonId = '#cancel';

    var errorFormIsVisable = false;

    // Utilities

    function trim(stringToTrim) {
        return stringToTrim.replace(/^\s+|\s+$/g, "");
    }

    // Data access

    function getEmail() {
        return trim($(emailId).val());
    }

    function getNewPassword() {
        return trim($(newPassId).val());
    }

    function getConfirmPassword() {
        return trim($(confirmPassId).val());
    }

    // Error Handling

    function showError(msg) {
        $(errorBlockId).html(msg);
        $(errorBlockId).animate({opacity: 1}, 500, function () {
            errorFormIsVisible = true;
        });
    }

    function hideError() {
        if (errorFormIsVisible)
            $(errorBlockId).animate({opacity: 0}, 500, function() {
                $(errorBlockId).html("&nbsp;");
            });
        errorFormIsVisible = false;
    }

    // Network Callbacks

    function updateAccount() {
        var data = {
            'email' : getEmail(),
            'password' : getNewPassword()
        };

        function onSuccess(data, textStatus, jqXHR) {
            console.log("success");

            $('.form').html("<h2>Success!</h2>" +
                            "<p><a href='/bm'>Return to your bookmarks.</a></p>");
        }

        function onError(jqXHR, textStatus, errorThrown) {
            showError("Unable to update account. Perhaps the email address is already used?");
        }

        $.ajax({
            type: 'POST',
            url: '/bm/api/account/',
            data: data,
            success: onSuccess,
            error: onError,
            dataType: 'json'
        });
    }

    // Event handlers

    function submitForm() {
        $(updateButtonId).focus();
        $(updateButtonId).click();
    }

    function cancelForm() {
        $(cancelButtonId).focus();
        $(cancelButtonId).click();
    }

    function onUpdateClick(event) {

        if (getNewPassword() !== getConfirmPassword()) {
            showError('New password must match confirm password.');
            return;
        }

        if (getNewPassword().length < 8) {
            showError('New password must be at least 8 characters in length.');
            return;
        }

        showError('Updating.');
        updateAccount();
    }

    function onCancelClick(event) {
        window.location.href = '/bm/';
    }

    function onKeyUp(event) {

        if (event.keyCode == 27) {
            cancelForm();
            return;
        }

        if (event.keyCode == 13) {
            submitForm();
            return;
        }

        hideError();
    }

    $(accountFormId).submit(function() { return false; });

    $(emailId).keyup(onKeyUp);
    $(newPassId).keyup(onKeyUp);
    $(confirmPassId).keyup(onKeyUp);

    $(updateButtonId).click(onUpdateClick);
    $(cancelButtonId).click(onCancelClick);

    showError("This form is not yet implemented. And it's ugly. Sorry about that.");
    $(newPassId).focus();
    $(emailId).attr('disabled', true);
});
