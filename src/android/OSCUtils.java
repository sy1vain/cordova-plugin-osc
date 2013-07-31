package nl.sylvain.cordova.osc;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OSCUtils extends CordovaPlugin {
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
    	System.out.println("exec " + action);
    	return false;
    }
}