package nl.sylvain.cordova.osc;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;

import netP5.Logger;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

import android.util.SparseArray;

public class OSCUtils extends CordovaPlugin {

	private SparseArray<OSCPortIn> oscIn = new SparseArray<OSCPortIn>();
	//osc out uses a hashmap since the keys are strings
	private HashMap<String, OSCPortOut> oscOut = new HashMap<String, OSCPortOut>();
	

	/**
     * Constructor.
     */
    public OSCUtils() {
    }

    /**
     * Executes the request and returns whether the action was valid.
     *
     * @param action 		The action to execute.
     * @param args 		JSONArray of arguments for the plugin.
     * @param callbackContext	The callback context used when calling back into JavaScript.
     * @return 			True if the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	try{
    		if(action.equals("startListening")){
    			startListening(args.getInt(0), callbackContext);
            }else if(action.equals("stopListening")){
            	stopListening(args.getInt(0), callbackContext);
            }else if(action.equals("close")){
                close(args.getInt(0), callbackContext);
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
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
    			synchronized(oscIn){
    				for(int i=0; i<oscIn.size(); i++){
    					OSCPortIn oscport = oscIn.valueAt(i);
    					if(oscport.isListening()){
    						oscport.stopListening();
    					}
    					oscport.close();
    				}
    				oscIn.clear();
    			}
    		}
    	});
    }

    //start listening
    private void startListening(final int port, final CallbackContext callbackContext){
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
    			try{
    				OSCPortIn oscport = getPortIn(port);
    				if(!oscport.isListening()){
    					oscport.startListening();
    				}
    				//callback to OK
    				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
    			}catch(Exception e){
    				callbackContext.error(e.getMessage());
    			}
    		}
    	});
    }
    
    //stop listening on certain port
    private void stopListening(final int port, final CallbackContext callbackContext){
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
    			try{
    				//get the port without creating
    				OSCPortIn oscport = getPortIn(port, false);
    				if(oscport!=null && oscport.isListening()){
    					oscport.stopListening();
    				}
    				//callback to OK
    				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
    			}catch(Exception e){
    				callbackContext.error(e.getMessage());
    			}
    		}
    	});
    }
    
    //adds a message listener
    private void addMessageListener(final int port, final String address, final CallbackContext callbackContext) throws SocketException {
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
    			try{
    				//get the port
    				OSCPortIn oscport = getPortIn(port);
    				oscport.addListener(address, new OSCCallbackListener(callbackContext));
    			}catch(Exception e){
    				callbackContext.error(e.getMessage());
    			}
    		}
    	});
    }
    
    //close the port
    private void close(final int port, final CallbackContext callbackContext) {
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
    			try{
    				OSCPortIn oscport = getPortIn(port, false);
    				if(oscport!=null){
    					if(oscport.isListening()){
    						oscport.stopListening();
    					}
    					oscport.close();
    					synchronized (oscIn) {
    						oscIn.delete(port);
    					}
    				}
    				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
    			}catch(Exception e){
    				callbackContext.error(e.getMessage());
    			}
    		}
    	});
    }

    
    //get a port (always create)
    private OSCPortIn getPortIn(int port) throws SocketException {
    	return getPortIn(port, true);
    }
    
    //get a port and create if needed
    private OSCPortIn getPortIn(int port, boolean create) throws SocketException {
    	OSCPortIn oscport = null;
    	synchronized (oscIn) {
    		oscport = oscIn.get(port);
    	}
        if(oscport==null && create){
        	oscport = new OSCPortIn(port);
        	synchronized (oscIn) {
        		oscIn.put(port, oscport);
        	}
        }
       	return oscport;
    }
    
    //get a port (always create)
    private OSCPortOut getPortOut(String host, int port) throws SocketException, UnknownHostException {
    	return getPortOut(host, port, true);
    }
    
    //get a port and create if needed
    private OSCPortOut getPortOut(String host, int port, boolean create) throws SocketException, UnknownHostException {
    	String hashkey = createOutKey(host, port);
    	OSCPortOut oscport = null;
    	synchronized (oscOut) {
    		oscport = oscOut.get(hashkey);
    	}
        if(oscport==null && create){
        	oscport = new OSCPortOut(InetAddress.getByName(host), port);
        	synchronized (oscOut) {
        		oscOut.put(hashkey, oscport);
        	}
        }
       	return oscport;
    }
    
    private String createOutKey(String host, int port){
    	return host + ":" + port;
    }
    
}

class OSCCallbackListener implements OSCListener {
	
	private CallbackContext callbackContext;
	
	OSCCallbackListener(CallbackContext callbackContext){
		this.callbackContext = callbackContext;
	}
	
	public void acceptMessage(Date date, OSCMessage msg){
		
		//create a JSON list
		JSONArray list = new JSONArray();
		
		Object[] objects = msg.getArguments();
		for(int i=0; i<objects.length; i++){
			list.put(objects[i]);
		}
		
		//create the result
		PluginResult result = new PluginResult(PluginResult.Status.OK, list);
		
		//we keep the callback in memory so we can call it again
		result.setKeepCallback(true);
		
		callbackContext.sendPluginResult(result);
	}
	
	protected void finalize() throws java.lang.Throwable{
		//send no result this will, clean it at the receiver
		if(callbackContext!=null){
			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.NO_RESULT));
		}
		super.finalize();
	}
	
}
