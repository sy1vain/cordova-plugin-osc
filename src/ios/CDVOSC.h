#import <Cordova/CDV.h>
#import "CocoaOSC.h"

@interface CDVOSC : CDVPlugin <OSCConnectionDelegate>
{
    OSCConnection *connection;
    NSMutableDictionary *listeners;
    id <CDVCommandDelegate> commandDelegate;
}

- (void)pluginInitialize;

- (void)startListening:(CDVInvokedUrlCommand*)command;
- (void)stopListening:(CDVInvokedUrlCommand*)command;
- (void)addListener:(CDVInvokedUrlCommand*)command;
- (void)close:(CDVInvokedUrlCommand*)command;
- (void)send:(CDVInvokedUrlCommand*)command;

@end
