module.exports = {
  startListening: function(port, successCallback, errorCallback){
    cordova.exec(successCallback, errorCallback, "OSC", "startListening", [port]);
  },

  stopListening: function(successCallback){
    cordova.exec(successCallback, function(err){
  		console.log(err);
  	}, "OSC", "stopListening");
  },

  close: function(successCallback){
    cordova.exec(successCallback, function(err){
  		console.log(err);
  	}, "OSC", "close");
  },

  on: function(message, successCallback){
    cordova.exec(successCallback, function(err){
  		console.log(err);
  	}, "OSC", "addListener", [message]);
  },

  send: function(data, successCallback){
    cordova.exec(successCallback, function(err){
  		console.log(err);
  	}, "OSC", "send", [data]);
  }
};
