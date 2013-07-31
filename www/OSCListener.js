var OSCListener = function(port){
	this.port = port;
}

OSCListener.prototype.on(message, succes){
	cordova.exec(succes, function(err){
		console.log(err);
	}, "OSC", "addMessageListener", [port, message]);
}