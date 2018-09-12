/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.util.ui;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.util.Config;

/**
 * Wrapper to the Logon service
 * @author freyssj
 */
public class ApplicationErrorLog {

	private static String application = "Error";
	private static boolean activated = true;
	public static final String ACTION_LOGON = "Logon";

	public static void setActivated(boolean activated) {
		ApplicationErrorLog.activated = activated;
	}
	public static boolean isActivated() {
		return activated;
	}

	public static void setApplication(String application) {
		ApplicationErrorLog.application = application;
	}

	public static void logException(final Throwable exception) {
		logException(application, exception);
	}
	public static void logException(final String application, final Throwable exception) {
		try {
			if(exception==null) throw new IllegalArgumentException("exception cannot be null");
			exception.printStackTrace();
			if(!activated) return;
			if(application==null) throw new IllegalArgumentException("Application cannot be null");
			if(exception.getClass()==Exception.class) return;
			if(exception.getClass().getCanonicalName().startsWith("com.actelion")) return;


			//			int t = 0;
			//			for(Map.Entry<Thread, StackTraceElement[]> e :Thread.getAllStackTraces().entrySet()) {
			//				if(e.getKey().isInterrupted()) continue;
			//				System.out.println("ApplicationErrorLog.logException() "+(++t)+"/"+Thread.activeCount()+". "+e.getKey() + "->" + (e.getValue().length>0? e.getValue()[0]:""));
			//			}

			new Thread() {
				@Override
				public void run() {
					try {
						StringWriter s = new StringWriter(100);
						PrintWriter pw = new PrintWriter(s);
						exception.printStackTrace(pw);
						String userId = System.getProperty("user.name");
						java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
						String pcName = localMachine.getHostName();

						if("freyssj".equalsIgnoreCase(pcName)) return;
						final int maxLength = exception.getCause()!=null? 1000: 2000;

						//Error
						String error1 = s.toString();
						if(error1.length()>maxLength) {
							error1 = error1.replaceAll("com.actelion.research.", "...");
							error1 = error1.replaceAll("at org.*?\\)", "at org.").replaceAll("(\tat org.\r\n)++", "\tat org...\r\n");
							error1 = error1.replaceAll("at java.*?\\)", "at java.").replaceAll("(\tat java.\r\n)++", "\tat java..\r\n");
							int index = error1.lastIndexOf("\n", maxLength);
							error1 = error1.substring(0, index<0? maxLength: index);
						}

						//Cause?
						String error2 = "";
						if(exception.getCause()!=null) {
							s = new StringWriter(100);
							pw = new PrintWriter(s);
							exception.getCause().printStackTrace(pw);
							error2 = "\n"+s.toString();
							if(error2.length()>maxLength) {
								error2 = error2.replaceAll("com.actelion.research.", "...");
								error2 = error2.replaceAll("at org.*?\\)", "at org.").replaceAll("(\tat org.\r\n)++", "\tat org...\r\n");
								error2 = error2.replaceAll("at java.*?\\)", "at java.").replaceAll("(\tat java.\r\n)++", "\tat java..\r\n");
								int index = error2.lastIndexOf("\n", maxLength);
								error2 = error2.substring(0, index<0? maxLength: index);
							}
						}


						URL url = new URL("http://" + Config.SERVER + ":8080/dataCenter/errorLog.do?app=" + URLEncoder.encode(application, "UTF-8")
						+ "&user="+URLEncoder.encode(userId, "UTF-8")
						+ "&pcname=" + URLEncoder.encode(pcName, "UTF-8")
						+ "&stacktrace="+URLEncoder.encode(error1+error2,"UTF-8"));
						url.getContent();
					} catch(Throwable e) {

						e.printStackTrace();
					}
				}
			}.start();
		} catch (Throwable e) {
			System.err.println(e);
		}
	}

	public static void main(String[] args) throws Exception {
		//Test NullException
		System.out.println(new ApplicationErrorLog().getClass().getCanonicalName());
		try {
			//Should record
			recError(1);
			Set<Integer> s = new HashSet<>();
			s.add(null);

		} catch(Throwable e) {
			JExceptionDialog.showError(e);
		}

	}

	private static void recError(int n) {
		if(n<=0) new TreeSet<>().add(null);
		if(n%2==0) recError(n-1);
		else recError(n-2);
	}


}
