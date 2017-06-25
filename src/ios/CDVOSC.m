#import "CDVOSC.h"
#import <Cordova/CDV.h>


@implementation CDVOSC

- (void)pluginInitialize
{
    connection = [[OSCConnection alloc] init];
    connection.delegate = self;
    listeners = [NSMutableDictionary dictionary];
}

- (void)startListening:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSError *error;
    NSNumber* port = [command.arguments objectAtIndex:0];
    
    if ([connection bindToAddress:@"0.0.0.0" port:[port intValue] error:&error]){
        if(!connection.continuouslyReceivePackets){
            connection.continuouslyReceivePackets = YES;
            [connection receivePacket];
        }
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }else{
        NSLog(@"Could not bind UDP connection: %@", error);
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Unable start Listening"];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)stopListening:(CDVInvokedUrlCommand*)command
{
    connection.continuouslyReceivePackets = NO;
}

- (void)addListener:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    NSString* address = [command.arguments objectAtIndex:0];
    [listeners setObject:callbackId forKey:address];
    //
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}

- (void)close:(CDVInvokedUrlCommand*)command
{
    [self stopListening:nil];
    [connection disconnect];
}

- (void)send:(CDVInvokedUrlCommand*)command
{
    NSDictionary* data = [command.arguments objectAtIndex:0];
    NSString* host = [data objectForKey:@"remoteAddress"];
    NSNumber* port = [data objectForKey:@"remotePort"];
    NSString* address = [data objectForKey:@"address"];
    NSArray* arguments = [data objectForKey:@"arguments"];
    
    OSCMutableMessage *message = [[OSCMutableMessage alloc] init];
    message.address = address;
    for (id argument in arguments) {
        [message addArgument:argument];
    }
    [connection sendPacket:message toHost:host port:[port unsignedIntegerValue]];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)oscConnection:(OSCConnection *)connection didReceivePacket:(OSCPacket *)packet fromHost:(NSString *)host port:(UInt16)port
{
    NSString *callbackId = [listeners objectForKey:packet.address];
    if(callbackId==nil) return;

    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                            messageAsDictionary: [NSDictionary dictionaryWithObjectsAndKeys:
                                                                  packet.address, @"address",
                                                                  packet.arguments, @"arguments",
                                                                  [NSNumber numberWithUnsignedInt:port], @"remotePort",
                                                                  host, @"remoteAddress",
                                                                  nil
                                                                  ]
                               ];

    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}
@end
