package com.jacobpozaic.crs;

import static com.jacobpozaic.crs.Messages.*;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MsgListener extends Thread {
	private static boolean running = true;				// boolean manages if input should still be listened for
	private NetworkMan nm;								// A reference to the Network Manager for sending messages etc
	private ConcurrentLinkedQueue<String> received;		// The queue of received messages
	private Date closeDate;
	
	private String commandContext = "";
	
	public MsgListener(NetworkMan nm, ConcurrentLinkedQueue<String> received, Date closeDate) {
		this.nm = nm;
		this.received = received;
		this.closeDate = closeDate;
	}
	
	public void run() {
		while(running) {
			String[] cmdID = getNextReply().split(":");			// Get the command and the UID of the client who sent it
			nm.setRecepient(Integer.parseInt(cmdID[0]));		// Set the recipient of any message in this loop
			String command = cmdID[1];							// Store the command that was received
			String[] args = command.split(" ");					// Split the command into its arguments
			if(args[0].startsWith("login")) {					// If the command is to log in
				int result = DBMan.login(args[1], args[2]);		// Check login credentials against the database
				switch(result) {								// Check what the result of the login attempt means
				case(1): nm.authUser(args[1], true); break;		// If result = 1 then the login was successful and the client is a student account
				case(2): nm.authUser(args[1], false); break;	// If result = 2 then the login was successful and the client is a professor account
				default: nm.sendMsg(LOGIN_FAILED); break;		// If result = 0 then the login failed
				}
			}
			String userID = nm.getAuthUser();											// Get the userID of the client
			if(userID == null) continue;												// If the client has not yet logged in then ignore its command
			commandContext = args[0];
			switch(commandContext) {													// Check what command was received
			case("all"):  nm.sendMsg("all " + DBMan.getAllCourses()); break;			// Send a list of all the courses to the client
			case("desc"): nm.sendMsg("desc " + DBMan.getDescription(args[1])); break;	// Send a description of the selected course to the client
			case("list"): 
				if(nm.isStudent()) nm.sendMsg("list " + DBMan.listEnrolled(userID)); 	// Send a list of all the courses the student is enrolled in
				else nm.sendMsg("list " + DBMan.listTeaching(userID));					// Sent a list of all the course that the professor is teaching
				break;
			case("add"):  
				if(isRegistrationClosed()) nm.sendMsg("add " + REGISTRATION_CLOSED);  	// If the registration is closed dont allow modification of courses
				if(validStudent(userID)) {												// Verify the command is from a student
					if(args[1].equals("mandatory") || args[1].equals("optional")) {		// Check if the operand is valid
						nm.sendMsg("add " + DBMan.addCourse(userID, args[1], args[2]));	// Send the result back to the client (success/fail)
						// if(DBMan.doneRegistering(userID)) This is where the billing system would be notified that the student has completed registration
					} else nm.sendMsg(ADD_INVALID_OPERAND);								// If the operand was not valid then send the client a message saying so
				} break;
			case("remove"):
				if(isRegistrationClosed()) nm.sendMsg("add " + REGISTRATION_CLOSED);  	// If the registration is closed dont allow modification of courses
				if(nm.isStudent()) 
					nm.sendMsg("remove " + DBMan.removeCourse(userID, args[1])); 		// Removes a student from a course
				else nm.sendMsg("remove " + DBMan.removeTeacher(userID, args[1]));		// Removes a professor from a course
				break;
			case("students"): if(validProfessor(userID)) 								// Verify the command is from a professor
					nm.sendMsg("students " + DBMan.listStudents(args[1])); break;		// Send the result back to the client (success/fail)
			case("teach"): 
					if(isRegistrationClosed()) nm.sendMsg("add " + REGISTRATION_CLOSED);// If the registration is closed dont allow modification of courses
					if(validProfessor(userID)) 											// Verify the command is from a professor
					nm.sendMsg("teach " + DBMan.addTeacher(userID, args[1])); break;	// Send the result back to the client (success/fail)
			case("quit"): nm.disconnect(); break; 										// Disconnect client from the server
			default: nm.sendMsg(INVALID_COMMAND); 										// Send message to client notifying that the command was unrecognized
			}
		}
	}
	
	private boolean isRegistrationClosed() {
		Date today = new Date();
		return today.compareTo(closeDate) >= 0;
	}
	
	/**
	 * A blocking method that waits for a reply from a client
	 * 
	 * @return The next reply in the queue.
	 */
	private String getNextReply() {
		try {
			String reply = null;											// Stores reply from client
			while((reply = received.poll()) == null) Thread.sleep(10);		// Wait for a reply from a client to arrive in the queue
			return reply;													// Return the reply to the command
		} catch (InterruptedException e) { e.printStackTrace(); }
		return null;
	}
	
	/**
	 * Checks if a student is valid.
	 * 
	 * @param clientUID The UID of the connection to the client.
	 * @param userID The ID of the user.
	 * @return true if the client is a student.
	 */
	private boolean validStudent(String userID) {
		if(!nm.isStudent()) {
			nm.sendMsg(commandContext + " " + CMD_STUDENT_ONLY);
			return false;
		}
		return true;
	}
	
	/**
	 * Checks if a professor is valid.
	 * 
	 * @param clientUID
	 * @param userID
	 * @return
	 */
	private boolean validProfessor(String userID) {
		if(nm.isStudent()) {
			nm.sendMsg(commandContext + " " + CMD_PROF_ONLY);
			return false;
		}
		return true;
	}
}
