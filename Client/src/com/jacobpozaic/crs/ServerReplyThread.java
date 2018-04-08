package com.jacobpozaic.crs;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerReplyThread extends Thread {
	private BufferedReader inFromServer = null;
	private ConcurrentLinkedQueue<String> replies;
	private boolean running = true;
	
	public ServerReplyThread(BufferedReader inFromServer, ConcurrentLinkedQueue<String> replies) {
		this.inFromServer = inFromServer;
		this.replies = replies;
	}
	
	public void run() {
		String inC = "";
		try {
			while ((inC = inFromServer.readLine()) != null) {
				inC = inC.replace("|", "\n");
			    replies.offer(inC);
			    if(!running) break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void shutdown() {
		running = false;
	}
}
