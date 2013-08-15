
var OSCSender = function(host, port){
	this.host = host;
	this.port = port;
}

OSCSender.prototype.send = function(address, data, successCallback, errorCallback){

	//this could be more advanced with checking if no data given, just callbacks etc

	if(typeof data == 'undefined' || !data) data = [];
	if(!(data instanceof Array)) data = [data];

	//we prepend it so reverse order
	data.unshift(address);
	data.unshift(this.port);
	data.unshift(this.host);

	cordova.exec(successCallback, errorCallback, "OSC", "sendMessage", data);
}

OSCListener.prototype.close = function(successCallback){
	cordova.exec(successCallback, function(err){
		console.log(err);
	}, "OSC", "closeSender", [this.host, this.port]);
}


module.exports = OSCSender;
