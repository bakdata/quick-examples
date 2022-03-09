$(document).ready(function() {

    var QUICK_HOST = ""
    var KEY = ""
    var ingest_ip = "https://" + QUICK_HOST + "/ingest/tiny-url" // set your ingest ip
    var count_ingest_ip = "https://" + QUICK_HOST + "/ingest/track-fetch"
    var gateway_ip = "https://" + QUICK_HOST + "/gateway/tinyurl-gateway/graphql" // set your gateway ip

    NOT_VALID_WORD_MESSAGE = "The token should be only letters and numbers and no special characters and white space! Valid characters are A-Z a-z 0-9"
    NOT_VALID_URL_MESSAGE = "The URL you entered is invalid. Please check the URL and try again."
    ALREADY_EXISTS_MESSAGE = "The following token is already taken. Choose another token please."
    DOES_NOT_EXIST_MESSAGE = "The token is free. There is no URL set for this word yet."
    ERROR_MESSAGE = "Something went wrong!"

    $("#addMessage").hide();
    $("#fetchMessage").hide();

    function validWord(word) {
        var wordregex = /^[0-9a-zA-Z]+$/g;
        return wordregex.test(word); //validates regex on word and return true if passed.
    }

    function validURL(url) {
        var urlregex = /^(https?|ftp):\/\/([a-zA-Z0-9.-]+(:[a-zA-Z0-9.&%$-]+)*@)*((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9][0-9]?)(\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}|([a-zA-Z0-9-]+\.)*[a-zA-Z0-9-]+\.(com|edu|gov|int|mil|net|org|biz|arpa|info|name|pro|aero|coop|museum|[a-zA-Z]{2}))(:[0-9]+)*(\/($|[a-zA-Z0-9.,?'\\+&%$#=~_-]+))*$/;
        return urlregex.test(url); //validates regex on url and return true if passed.
    }

    function changeToRed(red) { // if error
        $("#" + red).css({
            "background": "#E26868",
            "border-color": "#B63E5A"
        });
    }

    function changeToGreen(green) { // if success
        $("#" + green).css({
            "background": "#20A286",
            "border-color": "#19B99A"
        });
    }

    function customAlertAdd(message) {
        $("#addMessage").show();
        $("#addMessage").html(message);
    }

    function customAlertFetch(message) {
        $("#fetchMessage").show();
        $("#fetchMessage").html(message);
    }

    function ingestWord(word, url) {
        let data = JSON.stringify({ "key": word, "value": url });
        $.ajax({
            type: "POST",
            url: count_ingest_ip,
            contentType: "application/json",
            headers: {
                "accept": "application/json",
                "X-API-Key": KEY,
                'Access-Control-Allow-Origin': '*',
                'Access-Control-Allow-Credentials': true
            },
            data: data,
            crossDomain: true
        });
    }

    function fetchURL(data) {
        let url = data["data"]["fetchURL"]["url"];
        let count = data["data"]["fetchURL"]["count"];
        if (url === null) {
            changeToRed(message);
            customAlertFetch(DOES_NOT_EXIST_MESSAGE) //if the key is empty
        } else {
            if (count === null) {
                let count = 1;
            }
            changeToGreen(message);
            ingestWord(word, url);
            count = count + 1
            customAlertFetch("Your URL is: " + url + ".\n The URL was fetched " + count + " times.");
        }
    }

    $("#addButton").click(function() {
        // extract current input and create JSON string which will be send to the ingest service
        let message = "addMessage"
        let key = $('#userWord').val();
        let value = $('#userUrl').val();
        let data = JSON.stringify({ "key": key, "value": value });
        //check if the word and url are valid
        if (!validWord(key)) {
            changeToRed(message);
            customAlertAdd(NOT_VALID_WORD_MESSAGE)
        } else if (!validURL(value)) {
            changeToRed(message);
            customAlertAdd(NOT_VALID_URL_MESSAGE)
        } else {
            $.ajax({
                type: "POST",
                url: ingest_ip,
                contentType: "application/json",
                headers: {
                    "accept": "application/json",
                    "X-API-Key": KEY,
                    'Access-Control-Allow-Origin': '*',
                    'Access-Control-Allow-Credentials': true
                },
                crossDomain: true,
                data: data,
                success: function() {
                    changeToGreen(message);
                    customAlertAdd("Successfully added: " + data)
                },
                error: function(xhr) {
                    changeToRed(message);
                    if (xhr.status === 400) {
                        customAlertAdd(ALREADY_EXISTS_MESSAGE)
                    } else {
                        customAlertAdd(ERROR_MESSAGE) //if the error is > 400
                    }
                }
            });
        }
    });

    $("#fetchButton").click(function() {
        let message = "fetchMessage";
        let word = $('#userFetch').val();
        var query = `{ fetchURL (token: "${word}") { url count }}`;

        if (!validWord(word)) {
            changeToRed(message);
            customAlertFetch(NOT_VALID_WORD_MESSAGE)
        } else {
            $.ajax({
                type: "POST",
                url: gateway_ip,
                contentType: "application/json",
                data: JSON.stringify({
                    query
                }),
                crossDomain: true,
                headers: {
                    "accept": "application/json",
                    "X-API-Key": KEY,
                    'Access-Control-Allow-Origin': '*',
                    'Access-Control-Allow-Credentials': true
                },
                success: fetchURL,
                error: function(xhr) {
                    changeToRed(message);
                    customAlertFetch(ERROR_MESSAGE);
                }
            });
        }
    });
});
