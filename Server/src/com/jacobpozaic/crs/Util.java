package com.jacobpozaic.crs;

import java.sql.ResultSet;

public class Util {
	public static String resultSetToString(ResultSet r) {
		String out = "";
		try {
			while(r.next()) {
				for(int i = 1; i <= r.getMetaData().getColumnCount(); i++)
					out += r.getString(i) + " ";
				out += "\n";
			}
		} catch (Exception e) { System.out.println(e.getMessage());	}
		return out;
	}
}
