#import "CDVOSC.h"
#import <Cordova/CDV.h>


@implementation CDVOSC

- (void)pluginInitialize
{
    oscConnections = [NSMutableDictionary dictionary];
}

//sending
- (void)sendMessage:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    
    NSString *host = [command.arguments objectAtIndex:0];
    NSNumber *port = [command.arguments objectAtIndex:1];
    
    
    OSCObject *connection = [self getConnection:port];
    if(connection==nil){
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Unable to bind to port"];
    }else{
        OSCMutableMessage *message = [[OSCMutableMessage alloc] init];
        message.address = [command.arguments objectAtIndex:2];
        for(int i=0; i<[command.arguments count]; i++){
            [message addArgument:[command.arguments objectAtIndex:i]];
        }
        [connection send:message toHost:host];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    
    //send it back
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}

- (void)closeSender:(CDVInvokedUrlCommand*)command
{
    NSNumber* port = [command.arguments objectAtIndex:0];
    
    OSCObject *connection = [self getConnection:port];
    if(connection!=nil){
        //close the connection
        [connection close];
        //remove it from the list
        [oscConnections removeObjectForKey:port];
    }
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    //send it back
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

//receiving
- (void)startListening:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    
    NSNumber* port = [command.arguments objectAtIndex:0];
    
    OSCObject *connection = [self getConnection:port];
    if(connection==nil){
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Unable to create with port"];
    }else{
        [connection startListening];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    
    //send it back
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}

- (void)stopListening:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    
    NSNumber* port = [command.arguments objectAtIndex:0];
    
    OSCObject *connection = [self getConnection:port];
    if(connection==nil){
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Unable to create with port"];
    }else{
        [connection stopListening];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    
    //send it back
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)addMessageListener:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    
    NSNumber* port = [command.arguments objectAtIndex:0];
    
    OSCObject *connection = [self getConnection:port];
    if(connection==nil){
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Unable to create with port"];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:command.callbackId];
    }else{
        [connection addListener:[command.arguments objectAtIndex:1] withCallback:command.callbackId];
    }
}

- (void)closeListener:(CDVInvokedUrlCommand*)command
{
    NSNumber* port = [command.arguments objectAtIndex:0];
    
    OSCObject *connection = [self getConnection:port];
    if(connection!=nil){
        //close the connection
        [connection close];
        //remove it from the list
        [oscConnections removeObjectForKey:port];
    }
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    //send it back
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (OSCObject*)getConnection:(NSNumber*)port{
    return [self getConnection:port forceCreate:YES];
}

- (OSCObject*)getConnection:(NSNumber*)port forceCreate:(bool)create{

    OSCObject *connection = [oscConnections objectForKey:port];

    if(connection==nil && create){
        connection = [[OSCObject alloc] initWithPort:port andDelegate:self.commandDelegate];
        if(connection!=nil){
            [oscConnections setObject:connection forKey:port];
        }
    }
    
    return connection;
}

- (void)oscConnection:(OSCConnection *)connection didReceivePacket:(OSCPacket *)packet
{
    NSLog(@"packet: %@", packet.address);
}

@end





@implementation OSCObject

-(id)initWithPort:(NSNumber*)port andDelegate:(id <CDVCommandDelegate>)delegate;
{
    if ( self = [super init] )
    {
        connection = [[OSCConnection alloc] init];
        NSError *error;
        if (![connection bindToAddress:nil port:[port intValue] error:&error]){
            //throw something
            NSLog(@"Could not bind UDP connection: %@", error);
            self = nil;
        }else{
            connection.delegate = self;
            commandDelegate = delegate;
            listeners = [NSMutableDictionary dictionary];
        }
    }
    return self;
}

-(void)addListener:(NSString *)address withCallback:(NSString *)callbackId
{
    [listeners setObject:callbackId forKey:address];
    
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [result setKeepCallbackAsBool:YES];
    [commandDelegate sendPluginResult:result callbackId:callbackId];
}

-(void)send:(OSCPacket*)pkt toHost:(NSString*) host
{
    [connection sendPacket:pkt toHost:host port:[connection localPort]];
}

-(void)startListening
{
    if(!connection.continuouslyReceivePackets){
        connection.continuouslyReceivePackets = YES;
        [connection receivePacket];
    }
}

-(void)stopListening
{
    connection.continuouslyReceivePackets = NO;
}

-(void)close
{
    [self stopListening];
}

- (void)oscConnection:(OSCConnection *)connection didReceivePacket:(OSCPacket *)packet
{
    NSString *callbackId = [listeners objectForKey:packet.address];
    if(callbackId==nil) return;
        
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:packet.arguments];
    [result setKeepCallbackAsBool:YES];
    [commandDelegate sendPluginResult:result callbackId:callbackId];
}

@end