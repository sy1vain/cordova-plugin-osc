package nl.sylvain.cordova.osc;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.illposed.osc.*;

public class OSCUtils extends CordovaPlugin {

	HashMap<Integer, OSCPortIn> oscIn;

	/**
     * Constructor.
     */
    public OSCUtils() {
    	System.out.println("OSCUtils constructed");
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
    		}else{
    			return false;
    		}
    	}catch(Exception e){
    		callbackContext.error(e.getMessage());
    	}
    	//always return true, we only get here through an exception
    	return true;
    }


    private void startListening(port) throws SocketException{
		OSCPortIn oscport = getPortIn(port);
		if(!oscport.isListening()){
			oscport.startListening();
		}
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