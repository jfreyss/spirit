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

package com.actelion.research.spiritapp.spirit.ui.util.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExecutor;

public abstract class AutosaveDecorator {

	private final JDialog caller;
	private int lapseSeconds = 60;
	private int increment = 0;
	private int total = 0;
	private JCheckBox autosaveCheckBox = new JCheckBox("Autosave");
	
	private class AutosaveThread extends Thread {
		@Override
		public void run() {
			while(!isInterrupted()) {
				try {Thread.sleep(lapseSeconds*1000);}catch(Exception e) {return;}
				if(!caller.isVisible()) return;
				
				autosaveCheckBox.setText("Saving");

				for(int i=0; i<10; i++) {
					try {Thread.sleep(100);} catch(Exception e) {}
					autosaveCheckBox.setForeground(i%2==0?Color.GRAY: Color.LIGHT_GRAY);
				}
				autosaveCheckBox.setOpaque(true);
				
				if(!caller.isShowing()) return;
				
				//Autosave
				SwingWorkerExecutor.execute(new Thread() {
					@Override
					public void run() {
						try {
							autosave();
							autosaveCheckBox.setText("Autosaved (" + FormatterUtils.formatTime(new Date())+")");
							autosaveCheckBox.setToolTipText("Saved " + FormatterUtils.formatDateTime(new Date())+" (every "+lapseSeconds+"s)");
							autosaveCheckBox.setForeground(Color.BLACK);
							autosaveCheckBox.setBackground(Color.GREEN);
						} catch(Exception e) {

							autosaveCheckBox.setText("Autosave Error");
							autosaveCheckBox.setToolTipText("Error: " + e.getMessage());
							autosaveCheckBox.setOpaque(true);
							autosaveCheckBox.setForeground(Color.WHITE);
							autosaveCheckBox.setBackground(Color.RED);
							JExceptionDialog.showError(e);
						}
					}
				});
			}
		}
	}
	private AutosaveThread autosaveThread;
	
	public AutosaveDecorator(JDialog caller) {
		this(caller, "autosave");
	}
	
	/**
	 * 
	 * @param caller
	 * @param property (null to disable it upon start)
	 */
	public AutosaveDecorator(JDialog caller, final String property) {
		if(caller==null) throw new IllegalArgumentException("The caller cannot be null");
		this.caller = caller;
		boolean started = property!=null && Spirit.getConfig().getProperty(property, true);

		if(started) {
			startAutosave();
		} else {
			stopAutosave();			
		}
		
		autosaveCheckBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(property!=null) Spirit.getConfig().setProperty(property, autosaveCheckBox.isSelected());
				if(autosaveCheckBox.isSelected()) {
					startAutosave();
				} else {
					stopAutosave();
				}
			}
		});
	}
	
	public abstract void autosave() throws Exception;
	
	public synchronized void startAutosave() {
		autosaveCheckBox.setSelected(true);
		autosaveCheckBox.setOpaque(true);
		autosaveCheckBox.setBackground(Color.GREEN);
		if(autosaveThread==null) {		
			
			//To avoid too many saving, we increase the lapse every 10 occurences 
			increment++;
			if(increment%10==0) lapseSeconds = lapseSeconds*2;
			total += lapseSeconds;
			if(total>3600*24) {
				//After 1 day, we stop the autosave
				stopAutosave();
				return;
			}
			
			autosaveThread = new AutosaveThread();
			autosaveThread.setDaemon(true);			
			autosaveThread.start();		
		}
	}
	
	public synchronized void stopAutosave() {
		if(autosaveThread!=null) {
			autosaveThread.interrupt();
			autosaveThread=null;
		}
		autosaveCheckBox.setSelected(false);
		autosaveCheckBox.setOpaque(false);
	}

	

	/**
	 * @return the balanceCheckBox
	 */
	public JPanel getAutosaveCheckBox() {
		JPanel panel = new JPanel(new GridLayout(1,1));
		panel.setMinimumSize(new Dimension(135, 24));	
		panel.setPreferredSize(new Dimension(135, 24));	
		panel.setMaximumSize(new Dimension(135, 24));		

		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1,1,1,1), BorderFactory.createLineBorder(Color.GRAY)));
		panel.add(autosaveCheckBox);

		return panel;
	}
}
