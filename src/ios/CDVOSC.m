#import "CDVOSC.h"
#import <Cordova/CDV.h>


@implementation CDVOSC

- (void)pluginInitialize
{
    connections = [NSMutableDictionary dictionary];
    listeners = [NSMutableDictionary dictionary];
}

- (void)startListening:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSError *error;
    
    OSCConnection* connection = [self getConnection:[command.arguments objectAtIndex:0]];
    NSNumber* port = [command.arguments objectAtIndex:1];
    
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
    OSCConnection* connection = [self getConnection:[command.arguments objectAtIndex:0]];
    connection.continuouslyReceivePackets = NO;
}

- (void)addListener:(CDVInvokedUrlCommand*)command
{
    NSNumber* key = [command.arguments objectAtIndex:0];
    NSString* callbackId = command.callbackId;
    NSString* address = [command.arguments objectAtIndex:1];
    
    NSMutableDictionary *lstnrs = [listeners objectForKey:key];
    if(lstnrs==nil){
        lstnrs = [NSMutableDictionary dictionary];
        [listeners setObject:lstnrs forKey:key];
    }
    
    [lstnrs setObject:callbackId forKey:address];
    //
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:callbackId];
}

- (void)close:(CDVInvokedUrlCommand*)command
{
    [self stopListening:command];
    NSNumber* key = [command.arguments objectAtIndex:0];
    OSCConnection* connection = [self getConnection:key];
    [connection disconnect];
    [connections removeObjectForKey:key];
    [listeners removeObjectForKey:key];
}

- (void)send:(CDVInvokedUrlCommand*)command
{
    OSCConnection* connection = [self getConnection:[command.arguments objectAtIndex:0]];
    
    NSDictionary* data = [command.arguments objectAtIndex:1];
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
    for (NSNumber *key in connections) {
        if(connections[key]==connection){
            NSDictionary* lstnrs = listeners[key];
            if(lstnrs==nil) continue;
            NSString *callbackId = lstnrs[packet.address];
            if(callbackId==nil) continue;

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
    }
}

- (OSCConnection*)getConnection:(NSNumber*)instanceID
{
    OSCConnection* connection = [connections objectForKey:instanceID];
    
    if(connection==nil){
        connection = [[OSCConnection alloc] init];
        connection.delegate = self;
        [connections setObject:connection forKey:instanceID];
    }
    return connection;
}

@end
