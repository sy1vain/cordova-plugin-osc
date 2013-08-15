
var OSCListener = function(port){
	this.port = port;
}

OSCListener.prototype.startListening = function(successCallback, errorCallback){
	cordova.exec(successCallback, errorCallback, "OSC", "startListening", [this.port]);
}

OSCListener.prototype.stopListening = function(successCallback){
	cordova.exec(successCallback, function(err){
		console.log(err);
	}, "OSC", "stopListening", [this.port]);
}

OSCListener.prototype.close = function(successCallback){
	cordova.exec(successCallback, function(err){
		console.log(err);
	}, "OSC", "closeListener", [this.port]);
}

OSCListener.prototype.on = function(message, successCallback){
	cordova.exec(successCallback, function(err){
		console.log(err);
	}, "OSC", "addMessageListener", [this.port, message]);
}

module.exports = OSCListener;
