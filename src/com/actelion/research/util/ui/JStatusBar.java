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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * Status Bar showing the copyright, the info, the memory, and the logged user
 * 
 * @author Joel Freyss
 */
public class JStatusBar extends JPanel {
	
	private JLabel copyrightLabel = new JLabel(" ");
	private JLabel infoLabel = new JLabel(" ");
	private JProgressBar memoryBar = new JProgressBar();
	private JLabel userLabel = new JLabel();
	
	private class MemoryThread extends Thread {
		@Override
		public void run() {
			while(true) {
				try {Thread.sleep(53000);} catch (Exception e) {return;}
				final int max = (int)(Runtime.getRuntime().maxMemory() / 1000);
				final int free = (int)(Runtime.getRuntime().freeMemory() / 1000);
				final int total = (int)(Runtime.getRuntime().totalMemory() / 1000);
				final int used = total-free;
				SwingUtilities.invokeLater(new Runnable() {					
					@Override
					public void run() {
						memoryBar.setMaximum(max);
						memoryBar.setValue(used);
						memoryBar.setStringPainted(true);
						memoryBar.setString((used/1000) + "M of "+(max/1000)+"M");		
						memoryBar.setToolTipText("Memory usage: "+(used/1000) + "Mo out of "+(max/1000)+"Mo available");
					}
				});
			}
		}
	}
	
	public JStatusBar() {
		super(new BorderLayout());
		Color lineColor = UIUtils.darker(getBackground(), .7);
		setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, lineColor));
		copyrightLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, lineColor), BorderFactory.createEmptyBorder(0, 4, 0, 4)));
		infoLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, lineColor), BorderFactory.createEmptyBorder(0, 4, 0, 4)));
		memoryBar.setBorder(null);
		userLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, lineColor), BorderFactory.createEmptyBorder(0, 4, 0, 4)));
		
		userLabel.setPreferredSize(new Dimension(320,22));
		userLabel.setFont(FastFont.SMALL);

		add(UIUtils.createBox(infoLabel, null, null, copyrightLabel, UIUtils.createHorizontalBox(memoryBar, userLabel)));
		MemoryThread memoryThread = new MemoryThread();
		memoryThread.setDaemon(true);
		memoryThread.setPriority(Thread.MIN_PRIORITY);
		memoryThread.start();
		memoryBar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Runtime.getRuntime().gc();
			}
		});

	}
		
	public void setCopyright(String t) {		
		copyrightLabel.setText(t);
	}
	
	public void setInfos(String t) {
		infoLabel.setText(t);
	}
	
	public void setUser(String t) {
		userLabel.setText(t);
	}

}
