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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

@SuppressWarnings("serial" )
public class JStatusBar extends JPanel {
	
	//private JLabel text = new JLabel("                                                          ");
	private JLabel copyright = new JLabel(" ");
	private JLabel infos = new JLabel(" ");
	private JProgressBar memory = new JProgressBar();
	private JProgressBar progress = new JProgressBar();
	
	private class MemoryThread extends Thread {
		@Override
		public void run() {
			while(true) {
				try {Thread.sleep(2000);} catch (Exception e) {}
				int max = (int)(Runtime.getRuntime().maxMemory() / 1000);
				int free = (int)(Runtime.getRuntime().freeMemory() / 1000);
				int total = (int)(Runtime.getRuntime().totalMemory() / 1000);
				int used = total-free;
				memory.setMaximum(max);
				memory.setValue(used);
				memory.setStringPainted(true);
				memory.setString((used/1000) + "M of "+(max/1000)+"M");
				
			}
		}
	}
	
	public JStatusBar() {
		super(new BorderLayout(5,5));
		copyright.setBorder(BorderFactory.createLoweredBevelBorder());
		infos.setBorder(BorderFactory.createLoweredBevelBorder());
		copyright.setPreferredSize(new Dimension(400,10));
		progress.setPreferredSize(new Dimension(300,22));
		progress.setFont(progress.getFont().deriveFont(10f));
		progress.setBorder(BorderFactory.createLoweredBevelBorder());
		progress.setStringPainted(true);
		progress.setString("");

		//JPanel panel2 = new JPanel(new BorderLayout(5,5));
		//panel2.add(BorderLayout.CENTER, text);
		//panel2.add(BorderLayout.EAST, progress);

		JPanel left = new JPanel(new BorderLayout());
		left.add(BorderLayout.WEST, copyright);
		left.add(BorderLayout.CENTER, infos);	
		left.add(BorderLayout.EAST, memory);	
		
		add(BorderLayout.CENTER, left);
		add(BorderLayout.EAST, progress);
		MemoryThread memoryThread = new MemoryThread();
		memoryThread.setDaemon(true);
		memoryThread.setPriority(Thread.MAX_PRIORITY);
		memoryThread.start();
		memory.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Runtime.getRuntime().gc();
			}
		});

	}
		
	public void setCopyright(String t) {		
		copyright.setText(t);
	}
	
	public void setInfos(String t) {
		infos.setText(t);
	}
	
	public void setProgress(String t) {
		progress.setString(t);
	}
	
	public void startWorkInProgress(String t) {
		progress.setString(t);
		progress.setIndeterminate(true);
	}

	public void stopWorkInProgress(String t) {
		progress.setString(t);
		progress.setIndeterminate(false);
	}

	public void setUser(String t) {
		progress.setString(t);
		progress.setStringPainted(true);
	}

}
