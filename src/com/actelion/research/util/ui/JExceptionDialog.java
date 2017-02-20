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

package com.actelion.research.util.ui;

import java.awt.Component;
import java.awt.Frame;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Utility class to display an error and its causes, in a JOptionPane
 * @author freyssj
 *
 */
public class JExceptionDialog {

	private JExceptionDialog() {}
	
	public static void showError(Throwable e) {
		Frame parent = Frame.getFrames().length > 0 ? Frame.getFrames()[Frame.getFrames().length - 1] : null;
		if(parent instanceof SplashScreen2) parent = null;
		showError(parent, e);
	}
	
	public static void showError(String s) {
		Frame parent = Frame.getFrames().length > 0 ? Frame.getFrames()[Frame.getFrames().length - 1] : null;
		if(parent instanceof SplashScreen2) parent = null;
		showError(parent, s);
	}
	
	public static void showError(Component parent, Throwable e) {
		if(e==null) throw new IllegalArgumentException("The error cannot be null");
		System.err.println("Unexpected Error");
		ApplicationErrorLog.logException(e);
		StringBuilder sb = new StringBuilder();
		while(e!=null) {
			sb.append(e.getMessage() + "\n");
			e = e.getCause();
		}
		showError(parent, sb.toString());
	}
	
	public static void showError(Component parent, List<String> messages) {
		StringBuilder sb = new StringBuilder();
		sb.append("Some problems were found:\n");
		int i = 0;
		for (String s : messages) {
			sb.append(" - " + s + "\n");
			if(i++>10) {
				sb.append(messages.size()-10+" more...");
				break;
			}
		}
		showError(parent, sb.toString());
	}
	
	public static void showError(final Component parent, final String message) {
		show(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
	}
	public static void showInfo(Component parent, String message) {
		show(parent, message, "Info", JOptionPane.INFORMATION_MESSAGE);
	}
	public static void showWarning(Component parent, String message) {
		show(parent, message, "Warning", JOptionPane.WARNING_MESSAGE);
	}
	
	public static void show(final Component parent, final String message, final String title, final int messageType) {
		if(!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JOptionPane.showMessageDialog(parent, message, title, messageType);
				}
			});
		} else {
			JOptionPane.showMessageDialog(parent, message, title, messageType);
		}
	}

}
