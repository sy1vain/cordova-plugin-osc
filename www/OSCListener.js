var OSCListener = function(port){
	this.port = port;
}

OSCListener.prototype.on = function(message, succes){
	cordova.exec(succes, function(err){
		console.log(err);
	}, "OSC", "addMessageListener", [this.port, message]);
}

module.exports = OSCListener;