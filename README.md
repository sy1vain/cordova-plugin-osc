cordova-plugin-osc
==================

This is a very basic OSC plugin for Cordova, enabling sending and receiving OSC messages over a network.

**WARNING: VERSION 1.x.x is a big rewrite and has a (slightly) new interface, please see below.**

Supported platforms
-------------------

* android
* ios
* osx

Supported features
------------------

* Sending and receiving OSC message over a network

OSC API
-------

### Methods

An OSC instance can both send and/or receive messages. Each instance can only listen one one local port, but can send to any remote port.

Each instance of an OSC object implements the following methods:

| Method  | Description | Arguments
| :---:   | ---         | ---
| startListening  | Start listening for OSC messages on given port | `port`: Port to listen on <br> `[successCallback]`: callback on success: `function()` <br> `[errorCallback]`: callback on failure: `function(err)`
| stopListening   | Stop listening for OSC messages | `[successCallback]`: callback on success: `function()` <br> `[errorCallback]`: callback on failure: `function(err)` <br> _some messages may still arrive after stopping due to internal threading_
| send            | Send an OSC message |  `message`: OSC message in the following format: `{remoteAddress: 'IPorHOST', remotePort: PORT, address: '/path/of/message'[, arguments: [LIST, OF, ARGUMENTS]]}` <br> `[successCallback]`: callback on success: `function()` <br> `[errorCallback]`: callback on failure: `function(err)` <br>
| addListener     | Adds a listener for a certain OSC event | `address`: OSC address like `/path/of/message` <br> `successCallback`: callback on received message: `function(message)` _message format equals that of the `send` method_ <br> `[errorCallback]`: callback on failure: `function(err)` <br> _It is possible to use wildcards in addresses (currently Android only)_
| on              | Alias of addListener | see addListener
| close           | Closes OSC object, removes all listeners and prepare it for garbage collection | `[successCallback]`: callback on success: `function()` <br> `[errorCallback]`: callback on failure: `function(err)`


Example
-------
``` javascript
var port = 8000;

var osc = new OSC();

osc.startListening(port,
  function(){
    console.log('great success!')
  },
  function(err){
    console.log('epic fail', err)
  }
);

osc.on("/test", function(message){
  console.log('received a message');
  console.log(JSON.stringify(message));
});

osc.send({
    remoteAddress: '127.0.0.1',
    remotePort: 8001,
    address: '/test',
    arguments: [1, 2, 'three!']
});

```

Credits
-------

Android OSC relies on (a heavily modified version of) [JavaOSC](https://github.com/hoijui/JavaOSC)

iOS OSC relies on (a heavily modified version of) [CocoaOSC](https://github.com/danieldickison/CocoaOSC)
