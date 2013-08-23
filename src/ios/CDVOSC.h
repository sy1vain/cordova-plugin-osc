#import <Cordova/CDV.h>
#import "CocoaOSC.h"

@interface CDVOSC : CDVPlugin
{
    NSMutableDictionary* oscIn;
    NSMutableDictionary* oscOut;
}

- (void)pluginInitialize;

//sending
- (void)sendMessage:(CDVInvokedUrlCommand*)command;
- (void)closeSender:(CDVInvokedUrlCommand*)command;

//receiving
- (void)startListening:(CDVInvokedUrlCommand*)command;
- (void)stopListening:(CDVInvokedUrlCommand*)command;
- (void)addMessageListener:(CDVInvokedUrlCommand*)command;
- (void)closeListener:(CDVInvokedUrlCommand*)command;

@end

@interface OSCSender : NSObject
{
    OSCConnection *connection;
}

-(id)init;
-(void)send:(OSCPacket*)pkt toHost:(NSString*) host atPort:(NSNumber*)port;
-(void)close;
@end

@interface OSCListener : NSObject <OSCConnectionDelegate>
{
    OSCConnection *connection;
    NSMutableDictionary *listeners;
    id <CDVCommandDelegate> commandDelegate;
}

-(id)initWithPort:(NSNumber*)port andDelegate:(id <CDVCommandDelegate>)delegate;
-(void)addListener:(NSString*)address withCallback:(NSString*)callbackId;
-(void)startListening;
-(void)stopListening;
-(void)close;
@end

