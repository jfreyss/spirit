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

import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.Pair;

/**
 * CustomAdapter is an the adapter where the user can configure the connection through 
 * @author freyssj
 *
 */
class CustomAdapter extends DBAdapter {

	private DBProperty DBVENDOR_PROPERTY = new DBProperty("jnlp.custom.vendor", "Connection Vendor", new Pair[] {
			new Pair<String, String>("mysql", "MySQL 5.6+ InnoDB"),
			new Pair<String, String>("oracle", "Oracle 10+")});

	private DBProperty DBURL_PROPERTY = new DBProperty("jnlp.custom.dburl", "Connection URL", "jdbc:mysql://localhost:3306/spirit");

	private DBProperty DBNAME_PROPERTY = new DBProperty("jnlp.custom.username", "Connection UserId", "spirit");

	private DBProperty DBPASSWORD_PROPERTY = new DBProperty("jnlp.custom.password", "Connection Password", "");
	
	protected CustomAdapter() {
    }
    
	@Override
	public DBProperty[] getSpecificProperties() {
		return new DBProperty[] {DBVENDOR_PROPERTY, DBURL_PROPERTY, DBNAME_PROPERTY, DBPASSWORD_PROPERTY};
	}
	
    @Override
    public String getDBConnectionURL() {    	
   		String url = getDBProperty(DBURL_PROPERTY);
    	if(url==null) throw new IllegalArgumentException(DBURL_PROPERTY + " is null");
   		return url;
    }

    @Override 
    public String getDBUsername() {
    	String username = getDBProperty(DBNAME_PROPERTY);
    	if(username==null || username.length()==0) throw new IllegalArgumentException(DBNAME_PROPERTY + " is null");
    	return username;
    }

    @Override 
    public String getDBPassword() {
    	String password = getDBProperty(DBPASSWORD_PROPERTY);
    	if(password==null || password.length()==0) throw new IllegalArgumentException(DBPASSWORD_PROPERTY + " is empty");
    	return password;
    }
    
    @Override
    public String getHibernateDialect() {
    	String s = getDBProperty(DBVENDOR_PROPERTY);
    	if("mysql".equals(s)) {
    		return "org.hibernate.dialect.MySQLDialect";
    	} else if("oracle".equals(s)) {
    		return "org.hibernate.dialect.Oracle10gDialect";
    	} else {
    		throw new IllegalArgumentException(DBVENDOR_PROPERTY + " is null or not valid ("+s+")");
    	}
    }
    
    @Override
    public String getDriverClass() {
    	String s = getDBProperty(DBVENDOR_PROPERTY);
    	if("mysql".equals(s)) {
    		return "com.mysql.jdbc.Driver";
    	} else if("oracle".equals(s)) {
    		return "oracle.jdbc.driver.OracleDriver";
    	} else {
    		throw new IllegalArgumentException(DBVENDOR_PROPERTY + " is null or not valid ("+s+")");
    	}
    }   
    
    @Override
    public SpiritUser loadUser(String username) throws Exception {
    	SpiritUser user = super.loadUser(username);
    	
    	//keep compatibility with previous versions (before having roles)
    	if(username.startsWith("admin_")) user.setRole(SpiritUser.ROLE_ADMIN, true);
    	else if(username.startsWith("readall_")) user.setRole(SpiritUser.ROLE_READALL, true);
    	//end
    	
    	return user;
    }
    
    @Override
    public UserAdministrationMode getUserManagedMode() {
    	return UserAdministrationMode.READ_WRITE;
    }	
	
	@Override
	public String getHelp() {
		return "The CustomAdapter is the most versatile way to connect to a server database. The steps to install it are:"
				+ "<ul>"
				+ "<li>Install an Oracle or a MySql database (download at http://dev.mysql.com/)"
				+ "<li>Create a new schema: <i>create schema spirit;</i>"
				+ "<li>Create a new user: <i>create user spirit identified by 'PASSWORD';</i>"
				+ "<li>Grants all rights: <i>grant create,insert,select,update,delete on spirit.* to 'spirit'@'%';</i>"
				+ "<li>Test the connection and create the schema using those buttons...</i>"
				+ "</ul>"				
				;
	}
	
}
