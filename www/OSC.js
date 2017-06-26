var OSC_ID= 0;

var OSC = function(){
  this.instanceID = OSC_ID++;
}

OSC.prototype.startListening = function(port, successCallback, errorCallback){
  cordova.exec(successCallback, errorCallback, "OSC", "startListening", [this.instanceID, port]);
},

OSC.prototype.stopListening = function(successCallback, errorCallback){
  cordova.exec(successCallback, errorCallback, "OSC", "stopListening", [this.instanceID]);
},

OSC.prototype.close = function(successCallback, errorCallback){
  cordova.exec(successCallback, errorCallback, "OSC", "close", [this.instanceID]);
},

OSC.prototype.on = OSC.prototype.addListener = function(address, successCallback, errorCallback){
  cordova.exec(successCallback, errorCallback, "OSC", "addListener", [this.instanceID, address]);
},

OSC.prototype.send = function(message, successCallback, errorCallback){
  cordova.exec(successCallback, errorCallback, "OSC", "send", [this.instanceID, message]);
}

module.exports = OSC;
