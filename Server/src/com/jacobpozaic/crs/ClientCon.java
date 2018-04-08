package com.jacobpozaic.crs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.jacobpozaic.crs.Messages.*;

public class ClientCon extends Thread {
	public final int UID;								// The unique identifier for this connection to a Client, used to identify between each session/user
	
	private Socket socket;								// The socket that the connection exists on
	private BufferedReader inClient;					// Data coming in from the client
	private PrintWriter outServer;						// Data going out to the server
	private ConcurrentLinkedQueue<String> received;		// A reference to the queue of all messages received by all clients
	
	private volatile boolean running = true;			// Used to stop the thread when the connection is terminated
	public boolean authenticated = false;				// A boolean for if the Client is successfully authenticated on the server
	public String userID;								// The student or teacher ID
	public boolean isStudent;							// A boolean for if this session is a student
	
	public ClientCon(int UID, Socket socket, ConcurrentLinkedQueue<String> received) {
		this.UID = UID;
		this.socket = socket;
		this.received = received;
	}
	
	public void run() {
		try {
			inClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			outServer = new PrintWriter(socket.getOutputStream(), true);
			String in = "";
			while((in = inClient.readLine()) != null) {						// Reads any messages received from the client, terminates when the connection is ended or the server disconnects the client
				received.add(UID + ":" + in);								// Add the message that was read to the queue
			    if(!running) break;											// TODO: Will still wait for one more input...
			}
		} catch (IOException e) { System.out.println(CLIENT_CON_FAILED + getAddress()); }
	}
	
	/**
	 * Gets the host address of this connection (used for naming in output)
	 * @return
	 */
	public synchronized String getAddress() {
		return socket.getInetAddress().getHostName();
	}
	
	/**
	 * Sends a message to this client.
	 * 
	 * @param msg The message to send.
	 */
	public synchronized void sendMessage(String msg) {
		msg = msg.replace("\n", "|").replace("\r", "");						// Replaces newlines with ':' because client can only read one line at a time
		outServer.println(msg);
	}
	
	/**
	 * Disconnects the client from the server.
	 */
	public synchronized void disconnect() {
		running = false;
	}
}
