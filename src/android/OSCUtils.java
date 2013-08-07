package nl.sylvain.cordova.osc;

import java.net.SocketException;

import netP5.Logger;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import oscP5.OscEventListener;
import oscP5.OscMessage;
import oscP5.OscP5;
import oscP5.OscStatus;

import android.util.SparseArray;

public class OSCUtils extends CordovaPlugin {

	private SparseArray<OscP5> oscIn = new SparseArray<OscP5>();

	/**
     * Constructor.
     */
    public OSCUtils() {
    	//turn off logging
    	Logger.set(Logger.DEBUG, Logger.OFF);
    	Logger.set(Logger.INFO, Logger.OFF);
    	Logger.set(Logger.PROCESS, Logger.OFF);
    	Logger.set(Logger.WARNING, Logger.OFF);
    	Logger.set(Logger.ERROR, Logger.ON);
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
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
    			synchronized (oscIn) {
    		        for(int i=0; i<oscIn.size(); i++){
    		        	OscP5 oscport = oscIn.valueAt(i);
    		            oscport.stop();
    		        }
    		        oscIn.clear();
    	    	}	
    		}
    	});
    }

    private void startListening(final int port, final CallbackContext callbackContext){
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
    			getPortIn(port);
    			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
    		}
    	});
    }
    
    private void stopListening(final int port, final CallbackContext callbackContext){
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
    			OscP5 oscport = getPortIn(port, false);
    			if(oscport!=null){
    				oscport.stop();
    		
    				//also remove all listeners if threaded
    				synchronized (oscIn) {
    					oscIn.delete(port);
    				}
    			}
    			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
    		}
    	});
    }
    
    private void addMessageListener(final int port, final String message, final CallbackContext callbackContext) throws SocketException {
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
    			OscP5 oscport = getPortIn(port);
    	    	oscport.addListener(new OscListener(message, callbackContext));
    		}
    	});
    }
    
    private void close(final int port) {
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
    			OscP5 oscport = getPortIn(port, false);
    			if(oscport!=null){
    				oscport.stop();
    		
    				//also remove all listeners if threaded
    				synchronized (oscIn) {
    					oscIn.delete(port);
    				}
    			}
    		}
    	});
    }

    
    //get a port (always create)
    private OscP5 getPortIn(int port){
    	return getPortIn(port, true);
    }
    
    //get a port and create if needed
    private OscP5 getPortIn(int port, boolean create) {
    	synchronized (oscIn) {
    		OscP5 oscport = oscIn.get(port);
        	if(oscport==null && create){
        		/* we use "new Object()" instead of null
        		 * since the underlying object will
        		 * throw an error otherwise (tsk tsk)
        		 */
        		oscport = new OscP5(new Object(), port);
        		oscIn.put(port, oscport);
        	}
        	return oscport;
		}
    }
    
}

class OscListener implements OscEventListener, Runnable {
	
	private String addr;
	private CallbackContext callbackContext;
	
	OscListener(String addr, CallbackContext callbackContext){
		this.addr = addr;
		this.callbackContext = callbackContext;
	}
	
	public void oscEvent(OscMessage msg){
		if(!msg.addrPattern().equals(this.addr)) return;
		
		//create a json list
        JSONArray list = new JSONArray();
        
        Object[] objects = msg.arguments();
        for(int i=0; i<objects.length; i++){
            list.put(objects[i]);
        }
        
        PluginResult result = new PluginResult(PluginResult.Status.OK, list);
        
        //we keep the callback in memory so we can call it again
        result.setKeepCallback(true);
        
        this.callbackContext.sendPluginResult(result);
	}
	
	public void oscStatus(OscStatus status){
		System.out.println("status " + status.toString());
	}
	
	public void run(){
		
	}
}
