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

package com.actelion.research.util;

import java.net.URL;
import java.net.URLEncoder;

/**
 * Wrapper to the Logon service
 * @author freyssj
 */
public class UsageLog {

	public static final String ACTION_LOGON = "Logon";
	
	
	public void logUsage(final String application, final String userId) {
		logUsage(application, userId, ACTION_LOGON, null, null);
	}
	
	/**
	 * Parameters should be formatted like: [key=value][,key=value]*
	 * @param application
	 * @param userId (if null, the current user.name)
	 * @param pcName (if null, the current InetAddress.getLocalhose)
	 * @param action (if null, Logon) 
	 * @param parameters (can be null)
	 */
	public static void logUsage(final String application, final String userId, final String pcName, final String action, final String parameters) {
		if(application==null) throw new IllegalArgumentException("Application cannot be null");
		new Thread() {			
			@Override
			public void run() {
				try {
					

					String userId2 = userId == null? System.getProperty("user.name"): userId;
					String pcName2;
					if(pcName==null) {
						java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
						pcName2 = localMachine.getHostName();
					} else {
						pcName2 = pcName;
					}
					if("freyssj".equals(pcName2)) return;

					String action2 = action==null? ACTION_LOGON: action;
					String userId = System.getProperty("user.name");
					String os = System.getProperty("os.name");
					String home = System.getProperty("java.home");
					String parameters2 = "home="+home+";user="+userId+";os="+os+";version="+System.getProperty("java.version")+";";
					if(parameters!=null) parameters2 += parameters;

					new URL("http://ares:8080/dataCenter/log.do?project=" + URLEncoder.encode(application, "UTF-8") 
							+ "&username="+URLEncoder.encode(userId2, "UTF-8")
							+ "&pcname=" + URLEncoder.encode(pcName2, "UTF-8")
							+ "&action=" + URLEncoder.encode(action2, "UTF-8") 
							+ (parameters2!=null? "&parameters="+URLEncoder.encode(parameters2, "UTF-8"): "")
							).getContent();
						
				} catch(Throwable e) {
					e.printStackTrace();
				}				
			}
		}.start();
	}
		

	public static void main(String[] args) {
		System.out.println("UsageLog.main() os="+System.getProperty("os.name")+" vendor="+System.getProperty("java.home")+" java="+System.getProperty("java.version"));
	}
}
