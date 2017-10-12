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

import org.hsqldb.Server;

import com.actelion.research.spiritcore.business.property.PropertyDescriptor;
import com.actelion.research.spiritcore.services.migration.MigrationScript;

/**
 * The HSQLFileAdapter is responsible for file-based local DB, through HSQL.
 * It is not necessary to start a server for this, but this will only work locally.
 * 
 * @author freyssj
 *
 */
 public class HSQLFileAdapter extends DBAdapter {
	
    protected static Server server = null;

	public static PropertyDescriptor DBPATH_PROPERTY = new PropertyDescriptor("jnlp.hsqlFileAdapter.path", "DB Path", new File(System.getProperty("user.home"), ".spirit/hsqldb").getPath());
	
	public HSQLFileAdapter() {
	}
	
	
	
	@Override
	public void preInit() throws Exception {
		super.preInit();
		
		if(server!=null && !(this instanceof HSQLServerAdapter)) {
			server.shutdown(); 
			server = null;
		}
		
		String dbVersion = MigrationScript.getDBVersion();			
		if(dbVersion==null) {
			SchemaCreator.recreateTables(this);
		}		
	}
		
	@Override
	public UserManagedMode getUserManagedMode() {
		return UserManagedMode.UNIQUE_USER;
	}
	
	@Override
	public String getDBUsername() {
		return "SA";
	}

	@Override
	public String getDBPassword() {
		return "";
	}

	@Override
	public String getDBConnectionURL() {
		return "jdbc:hsqldb:file:" + getDBProperty(DBPATH_PROPERTY);
	}

	@Override
	public String getHibernateDialect() {
		return "org.hibernate.dialect.HSQLDialect";
	}

	@Override
	public String getDriverClass() {
		return "org.hsqldb.jdbcDriver";
	}
		
	@Override
	public PropertyDescriptor[] getSpecificProperties() {		
		return new PropertyDescriptor[] {DBPATH_PROPERTY};
	}
	
	@Override
	public String getHelp() {
		return "Simple Database used for testing and/or working with files. No installation needed, no user rights, and backup can be done by moving the selected directory.";
	}
	
}
