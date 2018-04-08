package com.jacobpozaic.crs;

/**
 * The client side of an application for managing course selection for students and professors
 * 
 * @bug Security issue with plain text sending of data over network
 * @bug waiting for command reply doesn't time out
 * 
 * @author Jacob Pozaic
 * @date 3/25/2017
 * @dateModified 3/30/2017
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.jacobpozaic.crs.Messages.*;

public class Client {
	private static final int TIMEOUT = 1000;										// The timeout for trying to connect to the server
	private static final int TIMOUT_CMD = 5000;										// The timeout for waiting for a reply for a command
	
	private static Socket con = new Socket();										// The socket that connects to the server
	private static PrintWriter outServ = null;										// The output stream to the server
	private static ServerReplyThread serverListener = null;							// A Thread that listens for replies from the server
	private static ConcurrentLinkedQueue<String> replies = new ConcurrentLinkedQueue<String>();
	private static boolean connected = false;										// A boolean for if the Client is successfully connected to the server
	private static boolean authenticated = false;									// A boolean for if the Client is successfully authenticated on the server
		
	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);										// Reads input from the user
		while(true) {
			// Connect to server
			if(!connected) {														// If the client has not connected to the server then do that first, if it fails to connect it will ask again
				System.out.println("Enter hostname:port");							// Prompt user for address of server
				parseAddressAndConnect(input.nextLine());							// Parse the input and try to connect to the host if it was valid
				continue;
			}
			
			// Authenticate user on server
			if(!authenticated) {													// Check if the client has authenticated (required to distinguish students from each other server side)
				System.out.println("Enter your login number: ");					// Prompt user for student number (or teacher number)
				String user = input.nextLine().trim();								// Store user name
				System.out.println("Enter your password: ");						// Prompt user for password
				String pass = input.nextLine().trim();								// Store password
				
				String login = "login " + user + " " + pass;						// Build message to server for login (data is send via plain text, security issue)
				outServ.println(login);												// Write message to server
				String reply = getReply("login");									// Get the reply for the login attempt
				if(reply.equals(LOGIN_FAILED)) {									// If the login failed
					System.out.println(INVALID_PASS);								// Notify the user that the login was unsuccessful
					continue;														// Continue back to the main program loop, effectively asking for login credentials again.
				}
				System.out.println(LOGIN_PASS);										// Notify user they have successfully logged in
				authenticated = true;												// Client authenticated successfully
			}
			
			// Process input from user
			String in = input.nextLine().toLowerCase().trim();
			String[] sp = in.split(" ");
			String reply = null;
			outServ.println(in);
			switch(sp[0]) {
			case("all"):      reply = MSG_ALL + getReply("all"); break;				// Lists all courses being offered
			case("desc"):     reply = MSG_DESC + getReply("desc"); break;			// Gives a description of the course
			case("list"):     reply = MSG_LIST + getReply("list").trim(); break;	// Lists all courses currently enrolled
			case("add"):      reply = getReply("add"); break;						// Adds a course to the students current list of courses
			case("remove"):   reply = getReply("remove"); break;					// Removes the course from the list of courses the student is enrolled in
			case("students"): 														// Lists all the students in a given course
				reply = getReply("students"); 
				if(!reply.startsWith("error")) reply = MSG_STU + reply;				// If there was an error then don't print the prefix
				break;		
			case("teach"):	  reply = getReply("teach"); break;						// Adds a professor to teach a course
			case("quit"):															// Disconnects from the server	  
				getReply("quit"); 													// Wait for server to accept disconnect
				serverListener.shutdown();											// Stop listening for input from the server
				try { con.close();													// Close the socket
				} catch (IOException e) { e.printStackTrace(); }
				System.exit(0); 													// Exit program
				break;
			case("?"):		  reply = HELP; break;									// Prints help information (does not go to server)
			default:          reply = INVALID_CMD; break;							// Catches invalid commands
			}
			System.out.println(reply);												// Writes the servers response to the console
		}
	}

	/**
	 * A blocking method that waits for a reply in the queue from the server for the corresponding command
	 * 
	 * @param command The command to wait for.
	 * @return The reply from the server for that command.  null if the thread was interrupted.
	 */
	private static String getReply(String command) {
		try {
			String reply = null;													// Stores reply from server
			long startTime = System.currentTimeMillis();							// Get the current time to determine the wait timeout
			while(true) {															// Continue cycling through the replies until the desired response if found, or timeout is reached
				while((reply = replies.poll()) == null) {							// Poll the queue for the next reply, if the queue is empty then wait 10ms
					Thread.sleep(10);												// Wait for a reply from the server to appear in the queue
					startTime = System.currentTimeMillis();							// Reset the current time if queue is empty, because there is nothing to time out waiting for
				}
				if(!reply.startsWith(command)) {									// If the reply was not for the command then put it back on the queue for later
					replies.offer(reply);											// Put the reply back on the queue
					Thread.sleep(10);												// Wait to prevent reading the same invalid queue items repeatedly at maximum speed (will eat CPU if this isn't here)
					if(System.currentTimeMillis() - startTime > TIMOUT_CMD) break;	// If timeout is reached then break
					continue;														// Go back to waiting for another item on the queue
				}
				break;																// If a reply was found for the command then leave the checking loop
			}
			return reply.substring(command.length() + 1, reply.length());			// Return the reply to the command
		} catch (InterruptedException e) { e.printStackTrace(); }
		return null;
	}

	/**
	 * Parses a string into a host and port and then tries to connect to the server.
	 * 
	 * @param in The string to parse.
	 */
	private static void parseAddressAndConnect(String in) {
		InetAddress host = null;												// Stores the host of the server to connect to
		int port = 0;															// Stores the port of the server to connect to
		try {
			in.trim();															// Remove any leading or trailing whitespace
			in = in.replace(" ", "");											// Remove any spaces from host and IP
			if(!in.contains(":")) throw new MalformedURLException();			// If port is missing/malformed throw exception
			String[] address = in.split(":");									// Split the address into host and port
			host = InetAddress.getByName(address[0]);							// Very that the host is valid, if not throw UnknownHostException
			port = Integer.parseInt(address[1]);								// Try to parse the port, if invalid throw NumberFormatException
			con.connect(new InetSocketAddress(host, port), TIMEOUT);			// Connect to the server, throws exception if timed out
			outServ = new PrintWriter(con.getOutputStream(), true);				// Sets the output stream to the server
			serverListener = new ServerReplyThread(								// Creates a new thread that manages received messages from the server and queues them
					new BufferedReader(
							new InputStreamReader(
									con.getInputStream())), replies);
			serverListener.start();												// Start the listener thread
			connected = true;													// Set connected flag to true
		} catch (MalformedURLException e) { System.out.println(INVALID_ADDR);	// The host address was invalid
		} catch (UnknownHostException e) { System.out.println(INVALID_HOST);	// The host address was invalid
		} catch (NumberFormatException e) { System.out.println(INVALID_PORT);	// The port was invalid
		} catch (IllegalArgumentException e) { System.out.println(INVALID_PORT);// The port was out of range
		} catch (SocketTimeoutException e) { System.out.println(TIMED_OUT);		// connection timed out
		} catch (IOException e) { e.printStackTrace(); }
	}
}
