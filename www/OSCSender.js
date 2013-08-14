
var OSCSender = function(host, port){
	this.host = host;
	this.port = port;
}

OSCSender.prototype.send = function(address, data, successCallback, errorCallback){

	//this could be more advanced with checking if no data given, just callbacks etc

	if(typeof data == 'undefined' || !data) data = [];
	if(!(data instanceof Array)) data = [data];

	data.unshift(address);
	data.unshift(this.port);
	data.unshift(this.host);

	cordova.exec(successCallback, errorCallback, "OSC", "sendMessage", data);
}


module.exports = OSCSender;
