var exec = require('cordova/exec');

exports.initToken = function (success, error,sdkKey,sdkSecret,sdkEndpoint,appId,token,enableNetwork) {
    exec(success, error, 'BlueGPS', 'init', [sdkKey,sdkSecret,sdkEndpoint,appId,token,enableNetwork]);
};

exports.initUser = function (success, error,sdkKey,sdkSecret,sdkEndpoint,appId,username,password,enableNetwork) {
    exec(success, error, 'BlueGPS', 'init', [sdkKey,sdkSecret,sdkEndpoint,appId,username,password,enableNetwork]);
};
exports.init = function (success, error,sdkKey,sdkSecret,sdkEndpoint,appId,enableNetwork) {
    exec(success, error, 'BlueGPS', 'init', [sdkKey,sdkSecret,sdkEndpoint,appId,enableNetwork]);
};

exports.login = function (success, error,token) {
    exec(success, error, 'BlueGPS', 'login', [token]);
};

exports.loginToken = function (success, error,username,password) {
    exec(success, error, 'BlueGPS', 'login', [username,password]);
};

exports.openMap = function (success, error,tagID,style,showMap) {
    exec(success, error, 'BlueGPS', 'openMap', [tagID,style,showMap]);
};

exports.startAdv = function (success, error) {
    exec(success, error, 'BlueGPS', 'startAdv', []);
};

exports.stopAdv = function (success, error) {
    exec(success, error, 'BlueGPS', 'stopAdv', []);
};