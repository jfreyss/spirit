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

import com.actelion.research.spiritcore.business.property.PropertyDescriptor;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.Pair;

/**
 * CustomAdapter is an the adapter where the user can configure the connection through
 * @author freyssj
 *
 */
class CustomAdapter extends DBAdapter {

	private PropertyDescriptor DBVENDOR_PROPERTY = new PropertyDescriptor("jnlp.custom.vendor", "Connection Vendor", new Pair[] {
			new Pair<String, String>("mysql", "MySQL 5.6+ InnoDB"),
			new Pair<String, String>("oracle", "Oracle 10+")});

	private PropertyDescriptor DBURL_PROPERTY = new PropertyDescriptor("jnlp.custom.dburl", "Connection URL", "jdbc:mysql://localhost:3306/spirit");

	private PropertyDescriptor DBNAME_PROPERTY = new PropertyDescriptor("jnlp.custom.username", "Connection UserId", "spirit");

	private PropertyDescriptor DBPASSWORD_PROPERTY = new PropertyDescriptor("jnlp.custom.password", "Connection Password", "");

	protected CustomAdapter() {
	}

	@Override
	public PropertyDescriptor[] getSpecificProperties() {
		return new PropertyDescriptor[] {DBVENDOR_PROPERTY, DBURL_PROPERTY, DBNAME_PROPERTY, DBPASSWORD_PROPERTY};
	}

	@Override
	public String getDBConnectionURL() {
		String url = getDBProperty(DBURL_PROPERTY);
		if(url==null) throw new RuntimeException(DBURL_PROPERTY + " is null");
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
			throw new RuntimeException(DBVENDOR_PROPERTY + " is null or not valid ("+s+")");
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
			throw new RuntimeException(DBVENDOR_PROPERTY + " is null or not valid ("+s+")");
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
	public UserManagedMode getUserManagedMode() {
		return UserManagedMode.WRITE_PWD;
	}

	@Override
	public String getHelp() {
		return "This Adapter is the most versatile way to connect to a server database. The steps to install are:"
				+ "<ul>"
				+ "<li>Install a database:"
				+ "<ul><li>Oracle <li>or MySql database (download at http://dev.mysql.com/)</ul>"
				+ "<li>Connect as root and run the following script: <br><br><i>create schema spirit;<br> create user spirit identified by 'PASSWORD';<br>grant all on spirit.* to 'spirit';<br><br></i>"
				+ "<li>Test the connection and save to initialize the tables (this may take some time)</i>"
				+ "</ul>"
				;
	}

}
