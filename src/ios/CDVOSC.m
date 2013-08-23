#import "CDVOSC.h"
#import <Cordova/CDV.h>


@implementation CDVOSC

- (void)pluginInitialize
{
    oscIn = [NSMutableDictionary dictionary];
    oscOut = [NSMutableDictionary dictionary];
}

//sending
- (void)sendMessage:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    
    NSString *host = [command.arguments objectAtIndex:0];
    NSNumber *port = [command.arguments objectAtIndex:1];
    
    
    OSCSender *osc = [self getOscOut:port];
    
    if(osc==nil){
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Unable to create out port"];
    }else{
        OSCMutableMessage *message = [[OSCMutableMessage alloc] init];
        message.address = [command.arguments objectAtIndex:2];
        for(int i=3; i<[command.arguments count]; i++){
            [message addArgument:[command.arguments objectAtIndex:i]];
        }
        [osc send:message toHost:host atPort:port];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    
    //send it back
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}

- (void)closeSender:(CDVInvokedUrlCommand*)command
{
    NSNumber* port = [command.arguments objectAtIndex:0];
    
    OSCSender *osc = [self getOscOut:port forceCreate:NO];
    if(osc!=nil){
        //close the connection
        [osc close];
        //remove it from the list
        [oscOut removeObjectForKey:port];
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
    
    OSCListener *osc = [self getOscIn:port];
    if(osc==nil){
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Unable to create with port"];
    }else{
        [osc startListening];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    
    //send it back
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}

- (void)stopListening:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    
    NSNumber* port = [command.arguments objectAtIndex:0];
    
    OSCListener *osc = [self getOscIn:port];
    if(osc==nil){
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Unable to create with port"];
    }else{
        [osc stopListening];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    
    //send it back
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)addMessageListener:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    
    NSNumber* port = [command.arguments objectAtIndex:0];
    
    OSCListener *osc = [self getOscIn:port];
    if(osc==nil){
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Unable to create with port"];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:command.callbackId];
    }else{
        [osc addListener:[command.arguments objectAtIndex:1] withCallback:command.callbackId];
    }
}

- (void)closeListener:(CDVInvokedUrlCommand*)command
{
    NSNumber* port = [command.arguments objectAtIndex:0];
    
    OSCListener *osc = [self getOscIn:port forceCreate:NO];
    if(osc!=nil){
        //close the connection
        [osc close];
        //remove it from the list
        [oscIn removeObjectForKey:port];
    }
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    //send it back
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (OSCListener*)getOscIn:(NSNumber*)port{
    return [self getOscIn:port forceCreate:YES];
}

- (OSCListener*)getOscIn:(NSNumber*)port forceCreate:(bool)create{

    OSCListener *osc = [oscIn objectForKey:port];

    if(osc==nil && create){
        osc = [[OSCListener alloc] initWithPort:port andDelegate:self.commandDelegate];
        if(osc!=nil){
            [oscIn setObject:osc forKey:port];
        }
    }
    
    return osc;
}

- (OSCSender*)getOscOut:(NSNumber*)port{
    return [self getOscOut:port forceCreate:YES];
}

- (OSCSender*)getOscOut:(NSNumber*)port forceCreate:(bool)create{
    
    OSCSender *osc = [oscOut objectForKey:port];
    
    if(osc==nil && create){
        osc = [[OSCSender alloc] init];
        if(osc!=nil){
            [oscOut setObject:osc forKey:port];
        }
    }
    
    return osc;
}

@end


@implementation OSCSender

-(id)init
{
    if(self = [super init])
    {
        connection = [[OSCConnection alloc] init];
        NSError *error;
        if (![connection bindToPort:0 error:&error])
        {
            NSLog(@"Could not bind UDP connection: %@", error);
            self = nil;
        }
    }
    return self;
}

-(void)send:(OSCPacket *)pkt toHost:(NSString *)host atPort:(NSNumber*)port
{
    [connection sendPacket:pkt toHost:host port:[port intValue]];
}

-(void)close
{
    [connection disconnect];
}

@end



@implementation OSCListener

-(id)initWithPort:(NSNumber*)port andDelegate:(id <CDVCommandDelegate>)delegate;
{
    if ( self = [super init] )
    {
        connection = [[OSCConnection alloc] init];
        NSError *error;
        if (![connection bindToPort:[port intValue] error:&error]){
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
    [connection disconnect];
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