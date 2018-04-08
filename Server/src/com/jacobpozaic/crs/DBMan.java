package com.jacobpozaic.crs;

import java.sql.*;
import org.sqlite.SQLiteConfig;

import static com.jacobpozaic.crs.Messages.*;

public class DBMan {
	private static final String DB = "jdbc:sqlite:data.db";						// Directory of the SQLite database
	private static final String DRIVER = "org.sqlite.JDBC";						// The driver for using the SQLite database
	
	private static Connection con;												// An instance of a connection to the database
	
	public static void init() {
		try {
			Class.forName(DRIVER);												// Checks if the database driver exists, if not an error is thrown
			SQLiteConfig cfg = new SQLiteConfig();								// Create a configuration for the database
			cfg.enforceForeignKeys(true);										// Enable foreign keys
			con = DriverManager.getConnection(DB, cfg.toProperties());			// Connect to the database

			// If tables don't exist yet then create them
			Statement st = con.createStatement();
			st.executeUpdate("CREATE TABLE IF NOT EXISTS professors (" + 		// Creates the professors table
			        "PROFID INT PRIMARY KEY NOT NULL, " +
					"PASS VARCHAR(20) NOT NULL COLLATE NOCASE, " +
					"Name VARCHAR(20) NOT NULL COLLATE NOCASE, " +
			        "Address VARCHAR(20) NOT NULL COLLATE NOCASE);");
			st.executeUpdate("CREATE TABLE IF NOT EXISTS students (" +
					"STUDENTID INT PRIMARY KEY NOT NULL, " +
					"PASS VARCHAR(20) NOT NULL COLLATE NOCASE, " +
					"Name VARCHAR(20) NOT NULL COLLATE NOCASE, " +
					"Address VARCHAR(20) NOT NULL COLLATE NOCASE);");
			st.executeUpdate("CREATE TABLE IF NOT EXISTS courses (" + 
					"COURSEID VARCHAR(20) PRIMARY KEY NOT NULL COLLATE NOCASE, " +
			        "PROFID INT, " +
					"Name VARCHAR(20) NOT NULL COLLATE NOCASE, " +
			        "Timeslot VARCHAR(20) NOT NULL COLLATE NOCASE, " +
					"Description VARCHAR(150) NOT NULL COLLATE NOCASE, " +
					"FOREIGN KEY (PROFID) REFERENCES professors (PROFID));");
			st.executeUpdate("CREATE TABLE IF NOT EXISTS enrollment (" + 
					"STUDENTID INT NOT NULL COLLATE NOCASE, " + 
					"COURSEID VARCHAR(20) NOT NULL COLLATE NOCASE, " +
					"Type VARCHAR(20) NOT NULL COLLATE NOCASE, " +
					"FOREIGN KEY (STUDENTID) REFERENCES students (STUDENTID), " +
					"FOREIGN KEY (COURSEID) REFERENCES courses (COURSEID));");
			st.close();
		} catch (ClassNotFoundException e) {
			System.out.println(MISSING_JDBC);
			System.exit(1);
		} catch (SQLException e) {
			System.out.println(CON_DB_FAIL);
			System.exit(2);
		}
	}
	
	/**
	 * Allows the administrator to manually make sql queries from the console.
	 * 
	 * @param query The query to perform
	 */
	public static ResultSet query(String query) {
		try {
			Statement st = con.createStatement();
			return st.executeQuery(query);
		} catch (SQLException e) { }
		return null;
	}
	
	/**
	 * Authenticates a client.
	 * 
	 * @param username
	 * @param password
	 * @return 0 if login failed, 1 if student log in, 2 if professor log in
	 */
	public static byte login(String username, String password) {
		try {
			Statement st = con.createStatement();
			ResultSet r = st.executeQuery("SELECT * FROM students WHERE STUDENTID = " + username + " AND PASS = '" + password + "';");
			if(r.next()) return 1;
			r = st.executeQuery("SELECT * FROM professors WHERE PROFID = " + username + " AND PASS = '" + password + "';");
			if(r.next()) return 2;
			st.close();
		} catch (SQLException e) { e.printStackTrace(); }
		return 0;
	}
	
	/**
	 * Lists all the courses being offered.
	 * 
	 * @return
	 */
	public static String getAllCourses() {
		try {
			Statement st = con.createStatement();
			ResultSet courses = st.executeQuery("SELECT COURSEID, Name FROM courses;");
			return Util.resultSetToString(courses);
		} catch (SQLException e) { e.printStackTrace(); }
		return null;
	}

	/**
	 * Gets the description of a course.
	 * 
	 * @param courseID The ID of the course
	 * @return
	 */
	public static String getDescription(String courseID) {
		if(!validCourseID(courseID)) return INVALID_COURSE_ID;
		try {
			Statement st = con.createStatement();
			ResultSet r = st.executeQuery("SELECT Name, Description, Timeslot FROM courses WHERE COURSEID = \'" + courseID + "\';");
			return Util.resultSetToString(r);
		} catch (SQLException e) { e.printStackTrace(); }
		return null;
	}

	/**
	 * Get the list of courses the student is enrolled in.
	 * 
	 * @param userID The student number of the student to list courses for.
	 * @return
	 */
	public static String listEnrolled(String userID) {
		try {
			Statement st = con.createStatement();
			ResultSet r = st.executeQuery("SELECT courses.COURSEID, Name, Type FROM courses INNER JOIN enrollment ON courses.COURSEID = enrollment.COURSEID WHERE STUDENTID = " + userID + ";");
			return Util.resultSetToString(r);
		} catch (SQLException e) { e.printStackTrace(); }
		return null;
	}
	
	/**
	 * Get the list of courses the student is enrolled in.
	 * 
	 * @param userID The student number of the student to list courses for.
	 * @return
	 */
	public static boolean doneRegistering(String userID) {
		try {
			Statement st = con.createStatement();
			ResultSet r = st.executeQuery("SELECT COUNT(courses.COURSEID) FROM courses INNER JOIN enrollment ON courses.COURSEID = enrollment.COURSEID WHERE STUDENTID = " + userID + ";");
			r.next();
			return r.getInt(1) == 6;
		} catch (SQLException e) { e.printStackTrace(); }
		return false;
	}
	
	/**
	 * Get a list of all the courses the professor is teaching.
	 * 
	 * @param userID The professor number of the student to list courses for.
	 * @return
	 */
	public static String listTeaching(String userID) {
		try {
			Statement st = con.createStatement();
			ResultSet r = st.executeQuery("SELECT COURSEID, Name FROM courses WHERE PROFID = " + userID + ";");
			return Util.resultSetToString(r);
		} catch (SQLException e) { e.printStackTrace(); }
		return null;
	}

	/**
	 * Adds a course to a students list of courses.
	 * 
	 * @param userID The student ID
	 * @param type The type of course (mandatory/optional)
	 * @param courseID The id of the course
	 * @return
	 */
	public static String addCourse(String userID, String type, String courseID) {
		if(!validCourseID(courseID)) return INVALID_COURSE_ID;
		if(isEnrolled(userID, courseID)) return ALREADY_ENROLLED;
		try {
			Statement st = con.createStatement();
			ResultSet re = st.executeQuery("SELECT COUNT(enrollment.STUDENTID) FROM courses INNER JOIN enrollment ON courses.COURSEID = enrollment.COURSEID WHERE courses.COURSEID = \'" + courseID + "\';");
			re.next();
			if(re.getInt(1) >= 10) return COURSE_FULL;
			if(type.equals("mandatory")) {
				ResultSet r = st.executeQuery("SELECT COUNT(COURSEID) FROM enrollment WHERE Type = \'mandatory\' AND STUDENTID = " + userID + ";");
				r.next();
				if(r.getInt(1) >= 4) return COURSE_FULL_M;
			} else {
				ResultSet r = st.executeQuery("SELECT COUNT(COURSEID) FROM enrollment WHERE Type = \'optional\' AND STUDENTID = " + userID + ";");
				r.next();
				if(r.getInt(1) >= 2) return COURSE_FULL_O;
			}
			st.executeUpdate("INSERT INTO enrollment VALUES(" + userID + ", \'" + courseID + "\', \'" + type + "\');");
			st.close();
		} catch (SQLException e) { e.printStackTrace(); }
		return COURSE_ADDED;
	}

	/**
	 * Removes a course from the list of courses a student is enrolled in.
	 * 
	 * @param userID The student number of the student dropping the course
	 * @param courseID The ID of the course being dropped
	 * @return
	 */
	public static String removeCourse(String userID, String courseID) {
		if(!validCourseID(courseID)) return INVALID_COURSE_ID;
		if(!isEnrolled(userID, courseID)) return NOT_ENROLLED;
		try {
			Statement st = con.createStatement();
			st.executeUpdate("DELETE FROM enrollment WHERE STUDENTID = " + userID + " AND COURSEID = \'" + courseID + "\';");
			st.close();
		} catch (SQLException e) { e.printStackTrace(); }
		return COURSE_REMOVED;
	}
	
	/**
	 * Removes a professor from teaching a course.
	 * 
	 * @param userID
	 * @param courseID
	 * @return
	 */
	public static String removeTeacher(String userID, String courseID) {
		if(!validCourseID(courseID)) return INVALID_COURSE_ID;
		if(!isTeaching(userID, courseID)) return NOT_TEACHING;
		try {
			Statement st = con.createStatement();
			st.executeUpdate("UPDATE courses SET PROFID = NULL WHERE COURSEID = \'" + courseID + "\';");
			st.close();
		} catch (SQLException e) { e.printStackTrace(); }
		return COURSE_REMOVED;
	}

	/**
	 * Lists all the students enrolled in a course.
	 * 
	 * @param courseID The id of the course to find the students for.
	 * @return
	 */
	public static String listStudents(String courseID) {
		if(!validCourseID(courseID)) return INVALID_COURSE_ID;
		try {
			Statement st = con.createStatement();
			ResultSet r = st.executeQuery("SELECT Name FROM students INNER JOIN enrollment ON students.STUDENTID = enrollment.STUDENTID WHERE COURSEID = \'" + courseID + "\';");
			return Util.resultSetToString(r);
		} catch (SQLException e) { e.printStackTrace(); }
		return null;
	}

	/**
	 * Adds a professor to a course as a teacher.
	 * 
	 * @param userID The id for the teacher.
	 * @param courseID The course to set the teacher of.
	 * @return
	 */
	public static String addTeacher(String userID, String courseID) {
		if(!validCourseID(courseID)) return INVALID_COURSE_ID;
		if(isAllreadyTaught(courseID)) return ALREADY_TAUGHT;
		try {
			Statement st = con.createStatement();
			st.executeUpdate("UPDATE courses SET PROFID = " + userID + " WHERE COURSEID = \'" + courseID + "\';");
			st.close();
		} catch (SQLException e) { e.printStackTrace(); }
		return COURSE_ADDED;
	}
	
	/**
	 * Checks if a given courseID is a course currently being offered.
	 * 
	 * @param courseID The courseID to check.
	 * @return
	 */
	private static boolean validCourseID(String courseID) {
		try {
			Statement st = con.createStatement();
			ResultSet r = st.executeQuery("SELECT * FROM courses WHERE COURSEID = \'" + courseID + "\';");
			return r.next();
		} catch (SQLException e) { e.printStackTrace(); }
		return true;
	}
	
	/**
	 * Checks if a student is enrolled in a class.
	 * 
	 * Must occur after userId and courseID validity checks.
	 * 
	 * @param userID The student number
	 * @param courseID The course to check
	 * @return
	 */
	private static boolean isEnrolled(String userID, String courseID) {
		try {
			Statement st = con.createStatement();
			ResultSet r = st.executeQuery("SELECT * FROM enrollment WHERE COURSEID = \'" + courseID + "\' AND STUDENTID = " + userID + ";");
			return r.next();
		} catch (SQLException e) { e.printStackTrace(); }
		return false;
	}
	
	/**
	 * Checks if a teacher is teaching a course.
	 * 
	 * @param userID
	 * @param courseID
	 * @return
	 */
	private static boolean isTeaching(String userID, String courseID) {
		try {
			Statement st = con.createStatement();
			ResultSet r = st.executeQuery("SELECT * FROM courses WHERE COURSEID = \'" + courseID + "\' AND PROFID = " + userID + ";");
			return r.next();
		} catch (SQLException e) { e.printStackTrace(); }
		return false;
	}
	
	/**
	 * Checks if a class already has a teacher.
	 * 
	 * Must occur after courseID validity check.
	 * 
	 * @param courseID The course to check
	 * @return
	 */
	private static boolean isAllreadyTaught(String courseID) {
		try {
			Statement st = con.createStatement();
			ResultSet r = st.executeQuery("SELECT PROFID FROM courses WHERE COURSEID = \'" + courseID + "\';");
			r.next();
			return !(r.getObject(1) == null);
		} catch (SQLException e) { }
		return false;
	}

	public static void removeCoursesWithLiessThanThreeStudents() {
		try { Statement st = con.createStatement();
			st.executeQuery("DELETE FROM courses WHERE SELECT COURSEID FROM enrollment WHERE COUNT(COURBY(COURSEID)) < 3;");
		} catch (SQLException e) { e.printStackTrace(); }
	}
}
