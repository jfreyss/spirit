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
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.property.PropertyDescriptor;
import com.actelion.research.spiritcore.util.Pair;

/**
 * The LocalAdapter is responsible for file-based local DB, through HSQL.
 * It is not necessary to start a server for this, but this will only work locally.
 * 
 * @author freyssj
 *
 */
public class HSQLServerAdapter extends HSQLFileAdapter {
	
	public static PropertyDescriptor DBMODE_PROPERTY = new PropertyDescriptor("jnlp.hsqlServerAdapter.mode", "Mode", new Pair[] {new Pair<String, String>("server", "Server"), new Pair<String, String>("client", "Client")});
	public static PropertyDescriptor DBPATH_PROPERTY = new PropertyDescriptor("jnlp.hsqlServerAdapter.path", "Path [if Server mode]", new File(System.getProperty("user.home"), ".spirit/db").getPath());
	public static PropertyDescriptor DBSERVER_PROPERTY = new PropertyDescriptor("jnlp.hsqlServerAdapter.server", "Server", "localhost");
	
	public HSQLServerAdapter() {
		super();
	}
	
	@Override
	public PropertyDescriptor[] getSpecificProperties() {
		return new PropertyDescriptor[]{DBMODE_PROPERTY, DBPATH_PROPERTY, DBSERVER_PROPERTY};
	}
	
	
	
	@Override
	public UserManagedMode getUserManagedMode() {
		return UserManagedMode.WRITE_PWD;
	}
	
	
	@Override
	public void preInit() throws Exception {		
		super.preInit();
		
		if(!"client".equals(getDBProperty(DBMODE_PROPERTY))) {
	        
	        if(server!=null) {
				LoggerFactory.getLogger(getClass()).info("Stop HSQL Server");
	        	server.shutdown();
	        }
	        
			LoggerFactory.getLogger(getClass()).info("Start HSQL Server");
	        server = new Server();
	        server.setDatabaseName(0, "spirit");
	        server.setDatabasePath(0, "file:" + getDBProperty(DBPATH_PROPERTY));
	        server.setDaemon(true);
	        server.start();
		}        
	}

	@Override
	public String getDBConnectionURL() {
		return "jdbc:hsqldb:hsql://" + getDBProperty(DBSERVER_PROPERTY) + "/spirit";
	}

	@Override
	public String getHelp() {
		return "Simple database with user rights. No DB installation is required. "
				+ "<ul>"
				+ "<li>One instance must be configured as server and the program must be kept open."
				+ "<li>The other instances must be configured as client and can connect to the server by specifying the IP/serverName.";
	}
}
