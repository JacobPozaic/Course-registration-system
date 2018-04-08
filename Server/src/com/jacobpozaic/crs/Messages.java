package com.jacobpozaic.crs;

public final class Messages {
	// Server
	public static final String SERV_INIT_FAIL 		= "Could not start server!";
	public static final String CLIENT_CON_FAILED	= "Connection was terminated! Address: ";
	public static final String CLIENT_MSG_FAILED	= "Could not send message to client!";
	public static final String CLIENT_CON_SUCCESS	= "Client has connected! Address: ";
	public static final String CLIENT_DISCONNECT	= "A client has disconnected! Address: ";
	public static final String DBG_LOGIN_SUCESSSFUL = " has successfully authenticated!";
	// Database
	public static final String MISSING_JDBC 		= "Cannot find database connector!";
	public static final String CON_DB_FAIL 			= "Cannot connect to database!";
	// Valid ID's
	public static final String NOT_AUTH				= "login error you are not authenticated!";
	public static final String INVALID_COURSE_ID 	= "error not a valid courseID!";
	public static final String INVALID_USER_ID 		= "error not a valid userID!";
	// Course enrollment
	public static final String NOT_ENROLLED 		= "error not enrolled in course!";
	public static final String ALREADY_ENROLLED 	= "error already enrolled in course!";
	public static final String COURSE_FULL			= "error course is full!";
	public static final String COURSE_FULL_M		= "error already have 4 mandatory courses selected!";
	public static final String COURSE_FULL_O		= "error already have 2 optional courses selected!";
	public static final String REGISTRATION_CLOSED  = "The registration due date has been reached, cannot change courses!";
	// Course teaching
	public static final String ALREADY_TAUGHT 		= "error course already has a teacher!";
	public static final String NOT_TEACHING			= "error you are not teaching that course!";
	// Commands
	public static final String LOGIN_SUCCESSFUL		= "login successful!";
	public static final String LOGIN_FAILED			= "login error invalid username or password!";
	public static final String CMD_STUDENT_ONLY		= "error only students can perfom this command!";
	public static final String CMD_PROF_ONLY		= "error only professors can perfom this command!";
	public static final String INVALID_COMMAND		= "Invalid command, for help type ?";
	public static final String ADD_INVALID_OPERAND	= "add error invalid operand <mandatory/optional>";
	public static final String COURSE_ADDED 		= "course was added sucessfully!";
	public static final String COURSE_REMOVED 		= "course was removed sucessfully!";
}
