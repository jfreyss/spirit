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

package com.actelion.research.spiritapp.spirit.ui.util.closabletab;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class JClosableTabbedPane extends JTabbedPane {

	/**
	 * 
	 * @author freyssj
	 */
	public interface IClosableTab {
		/** can we close this tab */
		public boolean isClosable();
		/** Called when the user closes the tab, returns true if ok*/
		public boolean onClose();
	}
	
	/**
	 * 
	 * @author freyssj
	 *
	 */
	public static class DefaultTab extends JPanel implements IClosableTab {		
		@Override
		public boolean isClosable() {
			return true;
		}
		@Override
		public boolean onClose() {
			return true;
		}
	}
	
	
	public JClosableTabbedPane() {		
		setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
	}
	
	
	@Override
	public void insertTab(String title, Icon icon, Component component, String tip, int index) {
		super.insertTab(title, icon, component, tip, index);
		if(component instanceof IClosableTab) {
			setTabComponentAt(index, new ClosableTabComponent(this));
		}
	}
	
	/**
	 * To be overridden when a tab is closed
	 */
	protected void closeTab(int index) {
		Component comp = getComponentAt(index);
		if(comp instanceof IClosableTab) {
			if(((IClosableTab) comp).onClose()) {
				setSelectedIndex(index>0? index-1: index+1<getTabCount()? index+1:-1);
				removeTabAt(index);
			}
		}
	}
	

	/**
	 * Example of use
	 * @param args
	 */
	public static void main(String[] args) {
		JFrame test = new JFrame();
		JClosableTabbedPane pane = new JClosableTabbedPane();
		pane.addTab("tab1", new DefaultTab());
		pane.addTab("tab2", new DefaultTab());
		pane.addTab("not closable", new JPanel());
		test.setContentPane(pane);
		test.pack();
		test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		test.setVisible(true);
	}
}
