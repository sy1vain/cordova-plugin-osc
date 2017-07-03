package nl.sylvain.cordova.osc;

import java.net.SocketException;
import java.util.Date;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPort;

import android.util.SparseArray;

public class OSCUtils extends CordovaPlugin {

	final private SparseArray<OSCPort> oscPorts;


	/**
     * Constructor.
     */
    public OSCUtils() {
		oscPorts = new SparseArray<OSCPort>();
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
				startListening(args.getInt(0), args.getInt(1), callbackContext);
			}else if(action.equals("stopListening")){
				stopListening(args.getInt(0), callbackContext);
			}else if(action.equals("close")){
				close(args.getInt(0), callbackContext);
			}else if(action.equals("addListener")){
				addListener(args.getInt(0), args.getString(1), callbackContext);
			}else if(action.equals("send")){
				send(args.getInt(0), args.getJSONObject(1), callbackContext);
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
		for(int i=0; i<oscPorts.size(); i++){
			oscPorts.valueAt(i).close();
			oscPorts.valueAt(i).stopListening();
		}
		oscPorts.clear();
    }

    //start listening
    private void startListening(final int instanceID, final int port, final CallbackContext callbackContext){
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
				OSCPort oscPort = getOSCPort(instanceID);
				if(oscPort==null){ callbackContext.error("Unable to find/create OSCPort"); return; }
				synchronized (oscPort){
					try{
						if(oscPort.isListening()) oscPort.stopListening();
						oscPort.startListening(port);
						callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
					}catch(Exception e){
						callbackContext.error(e.getMessage());
					}
				}
    		}
    	});
    }

    //stop listening on certain port
    private void stopListening(final int instanceID, final CallbackContext callbackContext){
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
				OSCPort oscPort = getOSCPort(instanceID);
				if(oscPort==null){ callbackContext.error("Unable to find/create OSCPort"); return; }
				synchronized (oscPort) {
					try {
						if(oscPort.isListening()) oscPort.stopListening();
						//callback to OK
						callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
					} catch (Exception e) {
						callbackContext.error(e.getMessage());
					}
				}
    		}
    	});
    }

    //close all
	private void close(final int instanceID, final CallbackContext callbackContext){
		cordova.getThreadPool().execute(new Runnable(){
			public void run(){
				OSCPort oscPort = getOSCPort(instanceID);
				if(oscPort==null){ callbackContext.error("Unable to find/create OSCPort"); return; }
				synchronized (oscPort) {
					oscPort.close();
				}
				synchronized (oscPorts){
					oscPorts.delete(instanceID);
				}
				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
			}
		});
	}

    //adds a message listener
    private void addListener(final int instanceID, final String address, final CallbackContext callbackContext) throws SocketException {
    	cordova.getThreadPool().execute(new Runnable(){
    		public void run(){
				try {
					OSCPort oscPort = getOSCPort(instanceID);
					if(oscPort==null){ callbackContext.error("Unable to find/create OSCPort"); return; }
					synchronized (oscPort) {
						oscPort.addListener(address, new OSCCallbackListener(callbackContext));
					}
					PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
					pluginResult.setKeepCallback(true);
					callbackContext.sendPluginResult(pluginResult);
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}
    		}
    	});
    }

    private void send(final int instanceID, final JSONObject json, final CallbackContext callbackContext){
		cordova.getThreadPool().execute(new Runnable(){
			public void run(){
				try {
					OSCPort oscPort = getOSCPort(instanceID);
					if(oscPort==null){ callbackContext.error("Unable to find/create OSCPort"); return; }
					if(!json.has("remoteAddress")){
						callbackContext.error("No remoteAddress");
						return;
					}
					if(!json.has("remotePort")){
						callbackContext.error("No remotePort");
						return;
					}
					if(!json.has("address")){
						callbackContext.error("No address");
						return;
					}

					String host = json.getString("remoteAddress");
					int port = json.getInt("remotePort");
					String address = json.getString("address");

					OSCMessage packet = new OSCMessage(address);
					if(json.has("arguments")){
						JSONArray arguments = json.getJSONArray("arguments");
						for(int i=0; i<arguments.length(); i++){
							Object argument = arguments.get(i);
							if(argument instanceof Double){
								argument = new Float(arguments.getDouble(i));
							}
							packet.addArgument(argument);
						}
					}

					synchronized (oscPort) {
						oscPort.send(packet, host, port);
					}
					callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
				} catch (Exception e) {
					callbackContext.error(e.getMessage());
				}

			}
		});
	}



	private OSCPort getOSCPort(final int instanceID){
		OSCPort oscPort;
		synchronized (oscPorts) {
			oscPort = oscPorts.get(instanceID);
			if (oscPort == null) {
				try {
					oscPort = new OSCPort();
					oscPorts.put(instanceID, oscPort);
				} catch (Exception e) {
				}
			}
		}

		return oscPort;
	}

}

class OSCCallbackListener implements OSCListener {

	private CallbackContext callbackContext;

	OSCCallbackListener(CallbackContext callbackContext){
		this.callbackContext = callbackContext;
	}

	public void acceptMessage(Date date, OSCMessage msg, String host, int port){
		try {
			//create a JSON list
			JSONArray arguments = new JSONArray();
			List<Object> objects = msg.getArguments();
			for (Object arg : objects) {
				arguments.put(arg);
			}

			JSONObject json = new JSONObject();
			json.put("address", msg.getAddress());
			json.put("arguments", arguments);
			json.put("remoteAddress", host);
			json.put("remotePort", port);

			//create the result
			PluginResult result = new PluginResult(PluginResult.Status.OK, json);

			//we keep the callback in memory so we can call it again
			result.setKeepCallback(true);

			callbackContext.sendPluginResult(result);
		}catch(Exception e){
			System.out.println("Error...");
		}
	}

	protected void finalize() throws java.lang.Throwable{
		//send no result this will, clean it at the receiver
		if(callbackContext!=null){
			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.NO_RESULT));
		}
		super.finalize();
	}

}
