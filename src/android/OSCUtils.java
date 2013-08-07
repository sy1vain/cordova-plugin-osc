package nl.sylvain.cordova.osc;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;

import android.util.SparseArray;

import com.illposed.osc.*;

public class OSCUtils extends CordovaPlugin {

    private SparseArray<OSCPortInTC> oscIn;

    /**
     * Constructor.
     */
    public OSCUtils() {
        oscIn = new SparseArray<OSCPortInTC>();
    }

    /**
     * Executes the request and returns whether the action was valid.
     *
     * @param action        The action to execute.
     * @param args      JSONArray of arguments for the plugin.
     * @param callbackContext   The callback context used when calling back into JavaScript.
     * @return          True if the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        try{
            if(action.equals("startListening")){
                startListening(args.getInt(0));
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            }else if(action.equals("stopListening")){
                stopListening(args.getInt(0));
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            }else if(action.equals("close")){
                close(args.getInt(0));
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
            }else if(action.equals("addMessageListener")){
                addMessageListener(args.getInt(0), args.getString(1), callbackContext);
                PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }else{
                return false;
            }
        }catch(SocketException e){
            callbackContext.error(e.getMessage());
        }catch(Exception e){
            callbackContext.error(e.getMessage());
        }
        //always return true, we only get here through an exception
        return true;
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking      Flag indicating if multitasking is turned on for app
     */
    public void onPause(boolean multitasking) {
        //TODO
    }
    
    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking      Flag indicating if multitasking is turned on for app
     */
    public void onResume(boolean multitasking) {
        //TODO
    }
    
    /**
     * Called when the WebView does a top-level navigation or refreshes.
     *
     * Plugins should stop any long-running processes and clean up internal state.
     *
     * Does nothing by default.
     */
    public void onReset() {
        for(int i=0; i<oscIn.size(); i++){
            OSCPortInTC inport = oscIn.valueAt(i);
            inport.stopListening();
            inport.close();
        }
        oscIn.clear();
    }

    private void startListening(int port) throws SocketException {
        OSCPortInTC oscport = getPortIn(port);
        if(!oscport.isListening()){
            oscport.startListening();
        }
    }

    private void stopListening(int port) throws SocketException {
        OSCPortInTC oscport = getPortIn(port);
        if(oscport.isListening()){
            oscport.stopListening();
        }
    }

    private void close(int port) throws SocketException {
        stopListening(port);
        OSCPortInTC oscport = getPortIn(port);
        oscport.close();
        //remove from lists
        oscIn.delete(port);
    }
    
    private void addMessageListener(int port, String message, CallbackContext callbackContext) throws SocketException {
        OSCPortInTC oscport = getPortIn(port);
        OSCCallback callback = new OSCCallback(callbackContext);
        oscport.addListener(message, callback);
        cordova.getThreadPool().execute(callback);
    }


    // port utils
    private OSCPortInTC getPortIn(int port) throws SocketException {
        OSCPortInTC oscport = oscIn.get(port);
        if(oscport==null){
            oscport = new OSCPortInTC(port);
            oscIn.put(port, oscport);
        }
        return oscport;
    }
}

class OSCIn extends OSCPortIn {
    
    OSCIn(int port) throws SocketException{
        super(port);

    }
    
}


//variant which add threaded callbacks
class OSCPortInTC extends OSCPortIn {
    private final LinkedList<OSCCallback> callbacks = new LinkedList<OSCCallback>(); 
    
    OSCPortInTC(int port) throws SocketException {
        super(port);
        
        DatagramSocket socket = getSocket();
        
        socket.setBroadcast(true);
    }
    
    public void close(){
        super.close();
        
        while(callbacks.size()>0){
            OSCCallback callback = callbacks.pop();
            callback.end();
        }
    }
    
    public void addListener(String message, OSCCallback callback){
        super.addListener(message, callback);
        addCallback(callback);
    }
    
    public void addCallback(OSCCallback callback){
        callbacks.add(callback);
    }
    
    public void finalize() throws Throwable{
        this.close();
        super.finalize();
    }
}

class OSCCallback extends Thread implements OSCListener  {
    private CallbackContext callbackContext;
    
    private LinkedList<OSCMessage> queue;
    private boolean running;
    
    OSCCallback(CallbackContext callbackContext){
        this.callbackContext = callbackContext;
        queue = new LinkedList<OSCMessage>();
        running = false;
    }
    
    public void end(){
        synchronized (this) {
            running = false;
        }
    }
    
    /**
     * this is the thead that runs
     */
    public void run(){
        synchronized (this) {
            if(running) return;
            running = true;
        }
        while(true){
            synchronized (this) {
                if(!running) return;
            }
            OSCMessage message = null;
            synchronized (queue) {
                while(queue.size()>0){
                    message = queue.pop();
                }
            }
            if(message==null){
                continue;
            }
            
            
            //create a json list
            JSONArray list = new JSONArray();
            
            Object[] objects = message.getArguments();
            for(int i=0; i<objects.length; i++){
                list.put(objects[i]);
            }
            
            PluginResult result = new PluginResult(PluginResult.Status.OK, list);
            
            //we keep the callback in memory so we can call it again
            result.setKeepCallback(true);
            
            this.callbackContext.sendPluginResult(result);
        }
    }
    
    /**
     * Accept an incoming OSCMessage
     * @param time     The time this message is to be executed.
     *          <code>null</code> means execute now
     * @param message  The message to execute.
     */
    public void acceptMessage(java.util.Date time, OSCMessage message){
        /**
         * We change this to run a thread to notify the client
         * possibly also implement a queue
         */
        synchronized (queue) {
            queue.add(message);
        }
    }
}