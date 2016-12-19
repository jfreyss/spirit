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

package com.actelion.research.spiritcore.adapter;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.property.PropertyDescriptor;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.StringEncrypter;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.spiritcore.util.SQLConverter;
import com.actelion.research.spiritcore.util.SQLConverter.SQLVendor;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.FormatterUtils.LocaleFormat;

/**
 * The adapter is responsible for the specific requirements concerning the DB: connection, user authentification, user tables, ...
 * It works as a singleton (getInstance), but its configuration can be changed by the program (setInstance)
 * @author freyssj
 *
 */
public abstract class DBAdapter {

	public static enum UserAdministrationMode {
		READ_ONLY, 
		READ_WRITE, 
		UNIQUE_USER}
	
	
	public static final String KEY = new String("adjlkdada8d0uah9d9j238jsad0a");

	public static PropertyDescriptor ADAPTER_PROPERTY = new PropertyDescriptor(
			"jnlp.adapter", "DB Adapter", 
			new Pair[] {
				new Pair<String,String>(HSQLFileAdapter.class.getName(), "HSQL Local Database for 1 user (no DB installation)"),
				new Pair<String,String>(HSQLServerAdapter.class.getName(), "HSQL Server Database (no DB installation)"),
				new Pair<String,String>(CustomAdapter.class.getName(), "Production Database (MySQL, Oracle)")});

	private static final File configFile = new File(System.getProperty("user.home"), ".spirit/spirit.config");

	
	/**Config File is db was not available at start*/
	private static DBAdapter instance;	
	private static boolean isConfigurable;
	
	private static Map<String, String> properties = new HashMap<>();
	

	/**
	 * Gets the DBAdapter based on the configuration (Singleton pattern).
	 * - The configuration is first looked upon the -Djnlp.adapter= properties
	 * - If not available, the configuration is loaded through the config file in "home/.spirit/spirit.config"
	 * - If not available, Spirit starts with a default HSQL DB
	 * 
	 */
	public static DBAdapter getAdapter() {
		if(instance==null) {
			synchronized (DBAdapter.class) {
				if(instance==null) {
					loadDBProperties();
					
					//Find the adapter and the config
					String className = System.getProperty(ADAPTER_PROPERTY.getPropertyName());					
					if(className==null || className.length()==0) {
						isConfigurable = true;
						className = properties.get(ADAPTER_PROPERTY.getPropertyName());
						if(className==null || className.length()==0) {
							className = HSQLFileAdapter.class.getName();
						}
					} else {
						isConfigurable = false;
					}
					
					
					try {
						instance = getAdapter(className);
					} catch (Exception ex) {
						throw new RuntimeException("Could not load adapter "+className, ex);
					}
					LoggerFactory.getLogger(DBAdapter.class).info("Use adapter: "+instance.getClass().getName());
				}
			}
		}
		return instance;
	}
	
	/**
	 * Sets the instance 
	 * @param myInstance
	 */
	public static void setAdapter(DBAdapter myInstance) {
		instance = myInstance;
	}
	
	public static void loadDBProperties() {
		properties = new LinkedHashMap<>();
		Properties prop = new Properties();
		if(configFile.exists()) {
			try (Reader reader = new FileReader(configFile)) {
				prop.load(reader);
			} catch(IOException ex) {
				throw new RuntimeException("Could not read "+configFile, ex);
			}
			
		}
		for (Object key : prop.keySet()) {
			properties.put((String) key, prop.getProperty((String)key));
		}
	}
	
	public final static void saveDBProperties() throws Exception {
		Properties prop = new Properties();
		for (Entry<String, String> e : properties.entrySet()) {
			prop.put(e.getKey(), e.getValue()==null?"": e.getValue());
		}
		try(FileWriter writer = new FileWriter(configFile)) {
			prop.store(writer, "Spirit Connection Properties");
		}
		LoggerFactory.getLogger(DBAdapter.class).info("Saved "+configFile);
	}
	
	public static DBAdapter getAdapter(String className) throws Exception {
		Class<?> claz = Class.forName(className);
		DBAdapter adapter = (DBAdapter) claz.newInstance();
		return adapter;
	}
	
	public PropertyDescriptor[] getSpecificProperties() {
		return new PropertyDescriptor[]{};
	}
	
	/**
	 * Set the properties, without saving
	 * @param properties
	 */
	public final static void setDBProperty(Map<String, String> p) {
		properties = p;
	}
	
	/**
	 * Retrieved the value of a property, 
	 * - first from the System.getProperty (-D option)
	 * - then, from the local config file
	 * - otherwise return the default option 
	 * @param conf
	 * @return
	 */
	public final String getDBProperty(PropertyDescriptor conf) {
		
		
		String v = System.getProperty(conf.getPropertyName());
		if(v==null || v.length()==0) {
			v = properties.get(conf.getPropertyName());
		}
		if(v==null || v.length()==0) {
			v = conf.getDefaultOptionKey();		
		}
		return v;
	}
		
	public abstract String getDBUsername();
	
	public abstract String getDBPassword();
	
	public abstract String getDBConnectionURL();
	
	public abstract String getHibernateDialect();
	
	public abstract String getDriverClass();	

	public String getHelp() {return "";} 	
		
	public void authenticateUser(String user, char[] password) throws Exception {
    	Employee emp = DAOEmployee.getEmployee(user);
    	
    	if(emp==null) throw new Exception("Invalid Username");   
    	if(emp.getPassword()!=null && emp.getPassword().length()>0 && 
    			!new StringEncrypter(KEY).encrypt(password).equals(emp.getPassword()) &&
    			!new StringEncrypter(KEY, false).encrypt(password).equals(emp.getPassword())) {
        	throw new Exception("Check your user name and password.");    
    	} else if((emp.getPassword()==null || emp.getPassword().length()==0) && password.length!=0) {
        	throw new Exception("Check your user name and password.");
    	}
    	if(emp.isDisabled()) throw new Exception("User is disabled");
    	
	}
	
	public String encryptPassword(char[] password) {
		return new StringEncrypter(KEY).encrypt(password);
	}
	
	public SpiritUser loadUser(String username) throws Exception {
    	Employee emp = DAOEmployee.getEmployee(username);
    	if(emp==null) return null;
    	SpiritUser user = new SpiritUser(emp);    	
    	return user;
	}
	
	public boolean isInActelionDomain() {		
		return false;
	}
	
	public abstract UserAdministrationMode getUserManagedMode();
	
	public Map<Location, URL> getAutomaticStores(){
		return null;
	}
	
	public SQLConverter.SQLVendor getVendor() {
		if(getDriverClass().contains("Oracle")) {
			return SQLVendor.ORACLE;
		} else if(getDriverClass().contains("hsqldb")) {
			return SQLVendor.HSQL;
		} else if(getDriverClass().contains("mysql")) {
			return SQLVendor.MYSQL;	
		} else {
			throw new RuntimeException("Unknown driver: "+getDriverClass());
		}
	}
	
	public String getTestQuery() {
		if(getDriverClass().contains("Oracle")) {
			return "select sysdate from dual";
		} else if(getDriverClass().contains("hsqldb")) {
			return "select current_timestamp from (VALUES(0))";
		} else if(getDriverClass().contains("mysql")) {
			return "select current_timestamp()";			
		} else {
			throw new RuntimeException("Unknown driver for getCurrentDateQuery: "+getDriverClass());
		}
	}
	
	public String getCurrentDateQuery() {
		if(getDriverClass().contains("Oracle")) {
			return "select sysdate from dual";
		} else if(getDriverClass().contains("hsqldb")) {
			return "select current_timestamp from (VALUES(0))";
		} else if(getDriverClass().contains("mysql")) {
			return "select current_timestamp()";			
		} else {
			throw new RuntimeException("Unknown driver for getCurrentDateQuery: "+getDriverClass());
		}
	}
	
	public List<EmployeeGroup> getEmployeeGroups() {
		return DAOEmployee.getEmployeeGroups();
	}
	
	
	public List<Employee> getEmployees() {
		return DAOEmployee.getEmployees();
	}
	
	
	public void checkValid(Collection<String> usernames) throws Exception {
		Set<String> allUsernames = new HashSet<String>();
		for (Employee emp : getEmployees()) {
			allUsernames.add(emp.getUserName());
		}
		for(String u: usernames) {
			if(!allUsernames.contains(u)) throw new Exception("The user "+u+" is not valid");
		}
	}
	
	/**
	 * Called before initializing the JPA Factory.
	 * This function can be called to starts a DB server (HSQL per example)
	 */
	public void preInit() throws Exception {		
	}
	

	/**
	 * Called after initializing JPA, can be used to populate tables or set properties
	 * @throws Exception
	 */
	public void postInit() throws Exception {
		LocaleFormat localeFormat = LocaleFormat.SWISS;
		String format = SpiritProperties.getInstance().getValue(PropertyKey.DATE_MODE);
		if(format.length()>0) {
			localeFormat = LocaleFormat.get(format);
			if(localeFormat==null) throw new Exception("Invalid LocaleFormat in spirit_property: "+format);
			FormatterUtils.setLocaleFormat(localeFormat);
		}
	}

	
	/**
	 * Validates the schema
	 * @throws Exception
	 */
	public void validate() throws Exception {
		JPAUtil.initFactory(this, "validate");
	}
	
	
	public static boolean isConfigurable() {
		return isConfigurable;
	}
	
	protected void executeScripts(String scripts, boolean failOnError) throws Exception {
		Connection conn = null;		
		try {
			conn = getConnection();
			//Convert the script to the appropriate DB
			scripts = SQLConverter.convertScript(scripts, getVendor());
			
			//Execute the script in batch mode
			Statement stmt =  conn.createStatement();
			for (String script : scripts.split(";")) {
				script = script.replaceAll("[\r\n]", "").trim();
				if(script.trim().length()==0) continue;
				LoggerFactory.getLogger(DBAdapter.class).debug("Execute script on "+getDBConnectionURL()+": "+script);
				stmt.addBatch(script);					
			}
			stmt.executeBatch();
			stmt.close();
			conn.commit();
		} catch(Exception e) {
			LoggerFactory.getLogger(DBAdapter.class).warn("Could not execute script: "+e);
			try {conn.rollback();} catch(Exception e2) {}
			if(failOnError) throw e;
		} finally {
			if(conn!=null) conn.close();
		}			
	}
		
	public void testConnection() throws Exception {
		Connection conn = null; 
		try {
			conn = getConnection();
		} finally {
			if(conn!=null) conn.close();
		}
	}
	
	/**
	 * Gets a native connection to the database. Be sure to close it afterwards
	 * @return
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		return getConnection(getDBUsername(), new String(new StringEncrypter("program from joel").decrypt(getDBPassword())));
	}
	
	public Connection getConnection(String username, String password) throws SQLException {
		LoggerFactory.getLogger(getClass()).debug("Connect to " + username + "@" + getDBConnectionURL());
		Connection conn = DriverManager.getConnection(getDBConnectionURL(), username, password);
		conn.setAutoCommit(false);
		return conn;
	}


	public Set<Location> getAutomatedStoreLocation() {
		if(getAutomaticStores()==null) return new HashSet<Location>();
		return getAutomaticStores().keySet();
		
	}
	
	public boolean isInAutomatedStore(Location loc) {
		return loc!=null && getAutomatedStoreLocation().contains(loc);
	}

	
	
}
