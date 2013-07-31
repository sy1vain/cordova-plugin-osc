var OSCListener = function(port){
	this.port = port;
}

OSCListener.prototype.startListening = function(successCallback, errorCallback){
	cordova.exec(successCallback, errorCallback, "OSC", "startListening", [this.port]);
}

OSCListener.prototype.on = function(message, successCallback){
	cordova.exec(successCallback, function(err){
		console.log(err);
	}, "OSC", "addMessageListener", [this.port, message]);
}

module.exports = OSCListener;