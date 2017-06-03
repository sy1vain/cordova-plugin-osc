cordova-osc-plugin
==================
This is a very basic OSC plugin for cordova

Supported platforms
===================
* Android
* iOS

Supported features
==================
* OSC Receive
* OSC Send

Example
=======
``` javascript
var port = 8000;
var osc = new OSCListener(port);

osc.startListening( successCallback, errorCallback );

osc.on("/topic", msgCallback);

function msgCallback (msg) {
    alert(msg);
}
```

Android OSC relies on https://github.com/hoijui/JavaOSC (included as a JAR file)
iOS OSC relies on https://github.com/danieldickison/CocoaOSC