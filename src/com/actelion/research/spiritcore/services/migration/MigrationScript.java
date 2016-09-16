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

package com.actelion.research.spiritcore.services.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.HSQLFileAdapter;
import com.actelion.research.spiritcore.adapter.PropertyKey;
import com.actelion.research.spiritcore.util.SQLConverter.SQLVendor;

/**
 * The Migration Script class is responsible for updating the DB from one version to the next. Therefore each script must implement:
 * - getToVersion()
 * - getScript() 
 * 
 * This superclass MigrationScript implements some static methods to know the current version and what scripts should be called.
 * 
 *  
 * @author freyssj
 */
public abstract class MigrationScript {

	private final String toVersion;
	
	public static class FatalException extends RuntimeException {
		public FatalException(String message) {
			super(message);
		}
	}
 	
	public static class MismatchDBException extends Exception {
		public MismatchDBException(String dbVersion) {
			super("The current version of the Spirit program ("+getExpectedDBVersion()+") does not match the DB version ("+(dbVersion==null?"NA":"")+")");
		}
	}
	
	public static interface ILogger {
		public void info(String sql, String msg);
		public void error(String sql, Exception e);
	}
	
	private static List<MigrationScript> getScripts() {
		List<MigrationScript> scripts = new ArrayList<>();		
		scripts.add(new MigrationScript1_9());
		scripts.add(new MigrationScript2_0());
		return scripts;
	}	
	
	public static String getExpectedDBVersion() {
		List<MigrationScript> scripts = getScripts();
		return scripts.get(scripts.size()-1).getToVersion();
	}
	
	/**
	 * Throws a MismatchDBException if the DB verson is not the one expected by the program
	 * @throws MismatchDBException
	 */
	public static void assertLatestVersion() throws MismatchDBException {
		String dbVersion;
		try {
			dbVersion = MigrationScript.getDBVersion();
		} catch(Exception e) {
			dbVersion = null;
		}
			
		if(dbVersion==null || !dbVersion.equals(MigrationScript.getExpectedDBVersion())) {
			throw new MismatchDBException(dbVersion);
		}
	}

	
	public static String getSql(SQLVendor vendor) throws Exception {
		StringBuilder sb = new StringBuilder();
		String version = MigrationScript.getDBVersion();
		for (MigrationScript script : getScripts()) {
			if(version==null || version.compareTo(script.getToVersion())<0) {
				sb.append("\r\n");
				sb.append("/* Migration to " + script.getToVersion() + " */\r\n");
				sb.append(script.getMigrationSql(vendor));
			}
		}
		return sb.toString();
	}
	
	/**
	 * Updates the DB, bx exececting the migration script.
	 * However the DB version is not set.
	 * @param vendor
	 * @param logger
	 * @throws Exception
	 */
	public static void updateDB(SQLVendor vendor, ILogger logger) throws Exception {
		Connection conn = DBAdapter.getAdapter().getConnection();
		try {
			String sql = getSql(vendor);
			executeScript(conn, sql, false, logger);
			conn.commit();
		} catch(Exception e) {
			conn.rollback();
			throw e;
		} finally {
			conn.close();
		}
	}
	
	
	/**
	 * Constructs a new migration script from the given version to the next version.
	 * The version is given by JPAUtil.getDBVersion()
	 * @param fromVersion
	 * @param toVersion
	 */
	protected MigrationScript(String toVersion) {
		assert toVersion!=null;
		this.toVersion = toVersion;
	}
	
	public String getToVersion() {
		return toVersion;
	}
	
	public abstract String getMigrationSql(SQLVendor vendor) throws Exception;
	
	/**
	 * Return the DB Version as stated in the table spirit.spirit_property.
	 * If the version is not set, this routine will update it
	 * 
	 * 
	 * @return
	 */
	public static String getDBVersion() throws Exception {
		Connection conn = null;
		try {
			conn = DBAdapter.getAdapter().getConnection();
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select max(value) from spirit.spirit_property where id = '" + PropertyKey.DB_VERSION.getKey() + "'");
			rs.next();
			String version = rs.getString(1);
			rs.close();
			stmt.close();
			
			LoggerFactory.getLogger(MigrationScript.class).info("DBVersion: " + version);			
			return version;
		} catch(Exception ex) {
			//Throw the exception in case of lock (HSQL file mode)
			if(ex.getMessage().startsWith("Database lock acquisition failure") && DBAdapter.getAdapter().getClass()==HSQLFileAdapter.class) {
				throw new FatalException("Please close the other Spirit instance before opening a new one.");
			}
			
			LoggerFactory.getLogger(MigrationScript.class).warn(ex.getMessage());
			return null;
		} finally {
			try{conn.close();}catch(Exception ex){}
		}
	}

	/**
	 * Util function to execute a script (in batch)
	 * @param conn
	 * @param scripts
	 * @param failOnError
	 * @param logger
	 * @throws Exception
	 */
	public static void executeScript(Connection conn, String scripts, boolean failOnError, ILogger logger) throws Exception {
		Statement stmt = null;
		
		for (String script : split(scripts)) {
			if(script.trim().length()==0) continue;
			try {
				stmt = conn.createStatement();
				LoggerFactory.getLogger(MigrationScript.class).info("script: "+script+"");
				int n = stmt.executeUpdate(script);
				LoggerFactory.getLogger(MigrationScript.class).info("script: "+script+": OK ("+ n + " rows updated)");
				if(logger!=null) logger.info(script, " rows updated");
				stmt.close();
			} catch(Exception ex) {
				LoggerFactory.getLogger(MigrationScript.class).error("Could not execute: "+script, ex);
				if(failOnError) throw ex;
				if(logger!=null) logger.error(script, ex);
			}
		}
	}
	
	private static List<String> split(String string){
		List<String> l = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(string, ";\'\\\r\n", true);
		StringBuilder sb = new StringBuilder();
		boolean inQuotes = false;
		boolean escape = false;
		while(st.hasMoreTokens()) {
			String s = st.nextToken();
			if(s.equals("\r") || s.equals("\n")) {
				//ignore
			} else if(s.equals(";")) { 
				if(inQuotes) {
					sb.append(s);
				} else {
					l.add(sb.toString());
					sb.setLength(0);
				}
			} else if(s.equals("\'")) {
				sb.append(s);
				if(!escape) {
					inQuotes = !inQuotes;
				}
			} else {
				sb.append(s);
			}
			escape = s.equals("\\");
		}
		if(inQuotes) {
			System.err.println("error in "+string);
		}
		l.add(sb.toString());
		
		return l;
	}
	
}
