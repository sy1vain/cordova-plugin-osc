var OSC_ID= 0;

var OSC = function(){
  this.instanceID = OSC_ID++;
}

OSC.prototype.startListening = function(port, successCallback, errorCallback){
  cordova.exec(successCallback, errorCallback, "OSC", "startListening", [this.instanceID, port]);
},

OSC.prototype.stopListening = function(successCallback){
  cordova.exec(successCallback, function(err){
    console.log(err);
  }, "OSC", "stopListening", [this.instanceID]);
},

OSC.prototype.close = function(successCallback){
  cordova.exec(successCallback, function(err){
    console.log(err);
  }, "OSC", "close", [this.instanceID]);
},

OSC.prototype.on = function(message, successCallback){
  cordova.exec(successCallback, function(err){
    console.log(err);
  }, "OSC", "addListener", [this.instanceID, message]);
},

OSC.prototype.send = function(data, successCallback){
  cordova.exec(successCallback, function(err){
    console.log(err);
  }, "OSC", "send", [this.instanceID, data]);
}

module.exports = OSC;
