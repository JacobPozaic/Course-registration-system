package com.jacobpozaic.crs;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.jacobpozaic.crs.Messages.*;

public class NetworkMan extends Thread {
	private ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<String>();
	private HashMap<Integer, ClientCon> con = new HashMap<Integer, ClientCon>();
	private ServerSocket ss;
	private boolean listening = true;
	private int ID = 0;
	
	private MsgListener rl;
	
	private int recipient = 0;
	
	public NetworkMan(int port, Date date) {
		try {
			ss = new ServerSocket(port);
			rl = new MsgListener(this, received, date);
			rl.start();
		} catch (IOException e) {
			System.out.println(SERV_INIT_FAIL);
			e.printStackTrace();
			System.exit(3);
		}
	}
	
	public void run() {
		while(listening) {
			try {
				Socket socket = ss.accept();
				ClientCon connection = new ClientCon(ID++, socket, received);
				con.put(connection.UID, connection);
				connection.start();
				System.out.println(CLIENT_CON_SUCCESS + socket.getInetAddress().getHostName());
				Thread.sleep(1000);
			} catch (IOException e) { e.printStackTrace();
			} catch (InterruptedException e) {}
		}
	}
	
	/**
	 * Sends a message to a specific client.
	 * 
	 * @param clientUID The UID of the client to send the message to.
	 * @param msg The message to send to the client.
	 */
	public synchronized void sendMsg(String msg) {
		getClient(recipient).sendMessage(msg);											// Send the message to the client
	}

	/**
	 * Gets the clients userID if it has successfully authenticated on the server.
	 * 
	 * @param clientUID The connection being verified.
	 * @return
	 */
	public synchronized String getAuthUser() {
		ClientCon client = getClient(recipient);
		if(client.authenticated) return client.userID;
		sendMsg(NOT_AUTH);
		return null;
	}
	
	/**
	 * Authenticates a users connection, allowing them to perform commands.
	 * 
	 * @param clientUID	The client to be authenticated.
	 * @param userID 
	 * @param isStudent If the account is a student or professor.
	 */
	public synchronized void authUser(String userID, boolean isStudent) {
		ClientCon client = getClient(recipient);
		client.authenticated = true;
		client.userID = userID;
		client.isStudent = isStudent;
		System.out.println(client.getAddress() + DBG_LOGIN_SUCESSSFUL);
		sendMsg(LOGIN_SUCCESSFUL);
	}
	
	/**
	 * Gets a Client connection by its UID.
	 * 
	 * @param clientUID The UID of the connection
	 * @return
	 */
	private synchronized ClientCon getClient(int clientUID) {
		ClientCon client = con.get(clientUID);								// Lookup the client in the table of connections
		if(client == null) throw new ClientNotFoundException(clientUID);	// If the client was not found in the table then throw an exception
		return client;
	}

	/**
	 * Checks if a client is a student.
	 * 
	 * @param clientUID
	 * @return
	 */
	public synchronized boolean isStudent() {
		ClientCon client = getClient(recipient);
		return client.isStudent;
	}

	/**
	 * Disconnects a client from the server.
	 * 
	 * @param clientUID The UID of the client to disconnect.
	 */
	public synchronized void disconnect() {
		ClientCon client = getClient(recipient);
		client.sendMessage("quit ");
		System.out.println(CLIENT_DISCONNECT + client.getAddress());
		client.disconnect();
		con.remove(recipient);
	}
	
	public synchronized void setRecepient(int clientUID) {
		recipient = clientUID;
	}
}
