package com.jacobpozaic.crs;

public final class Messages {
	public static final String MSG_ALL 		= "All courses being offered: \n";
	public static final String MSG_DESC 	= "The description of the course is: \n";
	public static final String MSG_LIST 	= "You are enrolled in the following classes: \n";
	public static final String MSG_STU 		= "The students currently enrolled in the course are: \n";
	public static final String LOGIN_FAILED	= "error invalid username or password!";
	public static final String LOGIN_PASS   = "Sucessfully logged into server!";
	public static final String INVALID_ADDR = "Invalid address!";
	public static final String INVALID_HOST = "Invalid host!";
	public static final String INVALID_PORT = "Invalid port!";
	public static final String TIMED_OUT 	= "Timed out!";
	public static final String INVALID_PASS = "Invalid ID or password!";
	public static final String INVALID_CMD	= "Invalid command, for help type ?";
	public static final String HELP 		= "all - List all availbel courses in fromat: Course-ID Course-Name\n" +
											  "desc <Course-ID> - Gives a description of the course in fromat: Course-Name Course-Description Course-Timeslot\n" +
											  "list - Lists all the courses you are currently enrolled in and if they are mandatory or optional\n" +
											  "add <mandatory/optional> <Course-ID> - Adds a course to your list of currently enrolled courses\n" +
											  "remove <Course-ID> - Removes a course from your list of currently enrolled courses\n" +
											  "students <Course-ID> - Lists all the students in a course (professors only)\n" +
											  "teach <Course-ID> - Adds a course to the list of courses you are teaching (professors only)\n" +
											  "quit - closes the program\n";
}
