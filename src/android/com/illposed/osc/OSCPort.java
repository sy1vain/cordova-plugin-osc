/*
 * Copyright (C) 2003-2014, C. Ramakrishnan / Illposed Software.
 * All rights reserved.
 *
 * This code is licensed under the BSD 3-Clause license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package com.illposed.osc;

import com.illposed.osc.utility.OSCByteArrayToJavaConverter;
import com.illposed.osc.utility.OSCPacketDispatcher;
import com.illposed.osc.utility.OSCPatternAddressSelector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 *
 * @author Chandrasekhar Ramakrishnan
 */
public class OSCPort implements Runnable {

	/**
	 * Buffers were 1500 bytes in size, but were increased to 1536, as this is a common MTU,
	 * and then increased to 65507, as this is the maximum incoming datagram data size.
	 */
	static final int BUFFER_SIZE = 65507;

	/** state for listening */
	private boolean listening;
	private final OSCByteArrayToJavaConverter converter;
	private final OSCPacketDispatcher dispatcher;
	private Thread listeningThread;

	private final DatagramSocket socket;

	public OSCPort(final DatagramSocket socket) {
		this.socket = socket;
		try{ socket.setBroadcast(true); }catch(Exception e){}

		this.converter = new OSCByteArrayToJavaConverter();
		this.dispatcher = new OSCPacketDispatcher();
		this.listeningThread = null;
	}

	public OSCPort()  throws SocketException {
		this(new DatagramSocket());
	}

	/**
	 * Returns the socket associated with this port.
	 * @return this ports socket
	 */
	protected DatagramSocket getSocket() {
		return socket;
	}

	/**
	 * Close the socket and free-up resources.
	 * It is recommended that clients call this when they are done with the
	 * port.
	 */
	public void close() {
		socket.close();
	}


	public void send(final OSCPacket aPacket, String host, int port) throws IOException {
		final byte[] byteArray = aPacket.getByteArray();
		final DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length, InetAddress.getByName(host), port);
		getSocket().send(packet);
	}

	/**
	 * Run the loop that listens for OSC on a socket until
	 * {@link #isListening()} becomes false.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		final byte[] buffer = new byte[BUFFER_SIZE];
		final DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);
		final DatagramSocket socket = getSocket();
		while (listening) {
			try {
				try {
					socket.receive(packet);
				} catch (SocketException ex) {
					if (listening) {
						throw ex;
					} else {
						// if we closed the socket while receiving data,
						// the exception is expected/normal, so we hide it
						continue;
					}
				}
				final OSCPacket oscPacket = converter.convert(buffer,
						packet.getLength());
				dispatcher.dispatchPacket(oscPacket, packet.getAddress().getHostAddress(), packet.getPort());
			} catch (IOException ex) {
				ex.printStackTrace(); // XXX This may not be a good idea, as this could easily lead to a never ending series of exceptions thrown (due to the non-exited while loop), and because the user of the lib may want to handle this case himself
			}
		}
	}

	/**
	 * Start listening for incoming OSCPackets
	 */
	public void startListening(int port) throws SocketException {

		getSocket().bind(new InetSocketAddress("0.0.0.0", port));

		if (!isListening()) { // NOTE This is not thread-save
			listening = true;
			listeningThread = new Thread(this);
			// The JVM exits when the only threads running are all daemon threads.
			listeningThread.setDaemon(true);
			listeningThread.start();
		}
	}

	/**
	 * Stop listening for incoming OSCPackets
	 */
	public void stopListening() {

		listening = false;
		if (listeningThread != null) { // NOTE This is not thread-save
			listeningThread.interrupt();
		}
		listeningThread = null;
	}

	/**
	 * Am I listening for packets?
	 * @return true if this port is in listening mode
	 */
	public boolean isListening() {
		return listening;
	}

	/**
	 * Registers a listener that will be notified of incoming messages,
	 * if their address matches the given pattern.
	 *
	 * @param addressSelector either a fixed address like "/sc/mixer/volume",
	 *   or a selector pattern (a mix between wildcards and regex)
	 *   like "/??/mixer/*", see {@link OSCPatternAddressSelector} for details
	 * @param listener will be notified of incoming packets, if they match
	 */
	public void addListener(final String addressSelector, final OSCListener listener) {
		this.addListener(new OSCPatternAddressSelector(addressSelector), listener);
	}

	/**
	 * Registers a listener that will be notified of incoming messages,
	 * if their address matches the given selector.
	 * @param addressSelector a custom address selector
	 * @param listener will be notified of incoming packets, if they match
	 */
	public void addListener(final AddressSelector addressSelector, final OSCListener listener) {
		dispatcher.addListener(addressSelector, listener);
	}
}
