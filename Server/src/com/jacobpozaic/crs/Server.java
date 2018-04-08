package com.jacobpozaic.crs;

import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Server {
	private static int port = 8081;
	private static Date registrationCloseDate;
	private static NetworkMan nm;
	
	public static void main(String[] cmd) {
		try { registrationCloseDate = new SimpleDateFormat("dd/MM/yyyy").parse("01/01/2020");
		} catch (ParseException e) { e.printStackTrace(); }
		
		Scanner input = new Scanner(System.in);
		DBMan.init();
		nm = new NetworkMan(port, registrationCloseDate);
		nm.start();
		
		Thread dateListeningThread = new Thread(){
			public void run() {
				while(true) {
					Date today = new Date();
					if(today.compareTo(registrationCloseDate) >= 0)
						DBMan.removeCoursesWithLiessThanThreeStudents();
					try { Thread.sleep(5000);
					} catch (InterruptedException e) { e.printStackTrace(); } // only check every 5 seconds
				}
			}
		};
		dateListeningThread.start();
		
		while(input.hasNextLine()) {
			// TODO: administrative commands
			ResultSet r = DBMan.query(input.nextLine().toLowerCase());
			System.out.println(Util.resultSetToString(r));
		}
		input.close();
	}
}
