/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2016 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16,
 * CH-4123 Allschwil, Switzerland.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.spiritcore.services;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DBTools {
	
	public static List<Integer> queryAsInteger(Connection conn, String SQL) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		try {
			List<Integer> res = new ArrayList<Integer>(); 
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL);
			while(rs.next()) {
				res.add(rs.getInt(1));				
			}
			return res;			
		} finally {
			try {rs.close();stmt.close();}catch (Exception e) {}
		}
	}
	
	public static List<String> queryAsString(Connection conn, String SQL) throws SQLException {
		Statement stmt = null;
		ResultSet rs = null;
		try { 
			List<String> res = new ArrayList<String>(); 
			stmt = conn.createStatement();
			rs = stmt.executeQuery(SQL);
			while(rs.next()) {
				res.add(rs.getString(1));				
			}
			return res;
			
		} finally {
			try {rs.close();stmt.close();}catch (Exception e) {}
		}
	}	
	
	public static int update(Connection conn, String SQL) throws SQLException {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			int res = stmt.executeUpdate(SQL);
			return res;
		} finally {
			try {stmt.close();}catch (Exception e) {}
		}
		
	}
}
