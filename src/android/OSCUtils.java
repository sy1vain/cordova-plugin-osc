package nl.sylvain.cordova.osc;

import java.net.SocketException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;

import android.util.SparseArray;

import com.illposed.osc.*;

public class OSCUtils extends CordovaPlugin {

	private SparseArray<OSCPortIn> oscIn;

	/**
     * Constructor.
     */
    public OSCUtils() {
    	oscIn = new SparseArray<OSCPortIn>();
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
    			startListening(args.getInt(0));
    			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
    		}else if(action.equals("addMessageListener")){
    			addMessageListener(args.getInt(0), args.getString(1), callbackContext);
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


    private void startListening(int port) throws SocketException {
		OSCPortIn oscport = getPortIn(port);
		if(!oscport.isListening()){
			oscport.startListening();
		}
    }
    
    private void addMessageListener(int port, String message, CallbackContext callbackContext) throws SocketException {
    	OSCPortIn oscport = getPortIn(port);
    	oscport.addListener(message, new OSCCallback(callbackContext));
    }


    // port utils
    private OSCPortIn getPortIn(int port) throws SocketException {
    	OSCPortIn oscport = oscIn.get(port);
    	if(oscport==null){
    		oscport = new OSCPortIn(port);
    		oscIn.put(port, oscport);
    	}
    	return oscport;
    }
}

class OSCCallback implements OSCListener  {
	private CallbackContext callbackContext;
	
	OSCCallback(CallbackContext callbackContext){
		this.callbackContext = callbackContext;
	}
	
	/**
	 * Accept an incoming OSCMessage
	 * @param time     The time this message is to be executed.
	 *          <code>null</code> means execute now
	 * @param message  The message to execute.
	 */
	public void acceptMessage(java.util.Date time, OSCMessage message){
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