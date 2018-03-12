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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;




public class JEscapeDialog extends JDialog {
		
	private boolean closed = false;

	public JEscapeDialog(Dialog owner, String title) {
		super(owner, title, true);		
		init();
	}
	public JEscapeDialog(Frame owner, String title) {
		super(owner, title, true);
		init();
	}
	public JEscapeDialog(Dialog owner, String title, boolean modal) {
		super(owner, title, modal);		
		init();
	}
	public JEscapeDialog(Frame owner, String title, boolean modal) {
		super(owner, title, modal);
		init();
	}
			

	
	private void init() {
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				eventCancel();
			}
			@Override
			public void windowClosed(WindowEvent e) {
				eventWindowClosed();
			}
		});
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}
	
	@Override
	protected JRootPane createRootPane() {
	    ActionListener actionListener = new ActionListener() {
		@Override
	      public void actionPerformed(ActionEvent actionEvent) {
	    	  eventCancel();
	      }
	    };
	    JRootPane rootPane = new JRootPane();
	    KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	    rootPane.registerKeyboardAction(actionListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
	    return rootPane;
	  }

	
	/**
	 * 
	 * @return
	 */
	protected boolean canBeClosed() {
		return true;
	}
	protected boolean mustAskForExit() {
		return false;
	}
	
	/**
	 * Can be overriden
	 */
	protected boolean eventCancel() {
		if(!canBeClosed()) return false;
		if(mustAskForExit()) {
			int res = JOptionPane.showOptionDialog(this, "Are you sure you want to close this window without saving?", "Quit?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
					new String[] {"Close without saving", "Cancel"}, "Cancel");
			if(res!=0) return false;
		}
		boolean res = eventWindowClosed(); 
		if(res) {
			dispose();
		}
		return res;
	}

	/**
	 * Can be overriden
	 */
	protected boolean eventWindowClosed() {
		//The closed variable is used to avoid the function being closed twice (onCancel and onClose for example)
		if(closed) return false;
		if(!canBeClosed()) return false;
		
		//We can close the dialog
		
		closed = true;
		return true;
	}
	 
	/**
	 * Util class for implementing classes
	 * @author freyssj
	 *
	 */
	public class CloseAction extends AbstractAction {
		public CloseAction() {
			super("Close");
		}
		public CloseAction(String title) {
			super(title);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
		
	}
	
	
}


