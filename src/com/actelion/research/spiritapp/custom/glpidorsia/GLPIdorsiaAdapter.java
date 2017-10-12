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

package com.actelion.research.spiritapp.custom.glpidorsia;

import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.actelion.research.spiritcore.adapter.DBAdapter;

public class GLPIdorsiaAdapter extends DBAdapter {

	public GLPIdorsiaAdapter() {
	}

	@Override
	public String getDBConnectionURL() {
		if("true".equalsIgnoreCase(System.getProperty("test"))) {
			return "jdbc:mysql://ec1rds-dbmysqlt1.idorsia.com:3306/spirit";
		} else {
			return "jdbc:mysql://ec1rds-dbmysqlt1.idorsia.com:3306/spirit";
		}
	}

	@Override
	public String getDBPassword() {
		return "C6BCE3F1F7F817F80F0B6B444D4026E1";
	}

	@Override
	public String getDBUsername() {
		return "spirit_root";
	}


	@Override
	public String getHibernateDialect() {
		return  "org.hibernate.dialect.MySQLDialect";
	}

	@Override
	public String getDriverClass() {
		return "com.mysql.jdbc.Driver";
	}

	@Override
	public UserManagedMode getUserManagedMode() {
		return UserManagedMode.WRITE_NOPWD;
	}

	@Override
	public void authenticateUser(String username, char[] password) throws Exception {
		if(password.length==0) throw new Exception("No password");
		if(getContext(username, new String(password))==null) throw new Exception("Invalid credentials");

	}


	private static DirContext getContext(String user, String pw) throws Exception {
		Hashtable<String, String> env = new Hashtable<String, String>();

		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldap://chbsmv-dcema1.europe.actelion.com:389");
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, String.format("CN=%s,OU=Users,OU=Managed,DC=europe,DC=actelion,DC=com",user));
		env.put(Context.SECURITY_CREDENTIALS, pw);
		return new InitialDirContext(env);
	}

	public static void main(String[] args) throws Exception{
		System.out.println(getContext("freyssj", ""));
	}


	@Override
	public List<? extends Object> getReports() {
		return null;
	}
}
