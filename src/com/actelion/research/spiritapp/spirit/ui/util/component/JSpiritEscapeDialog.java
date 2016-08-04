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

import java.awt.AWTEvent;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JDialog;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JEscapeDialog;

/**
 * JEscapeDialog for Spirit that allow transaction isolation.
 * @author freyssj
 *
 */
public class JSpiritEscapeDialog extends JEscapeDialog {

	private String pushContext;
	private boolean mustAskForExit = false;
	
	public JSpiritEscapeDialog(Frame owner, String title, final String pushContext) {
		super(owner, title, true);
		this.pushContext = pushContext;		
		if(pushContext!=null) JPAUtil.pushEditableContext(Spirit.getUser());
		init();
	}

	public JSpiritEscapeDialog(JDialog owner, String title, final String pushContext) {
		super(owner, title, true);
		this.pushContext = pushContext;	
		if(pushContext!=null) JPAUtil.pushEditableContext(Spirit.getUser());
		
		init();
		
	}
	
	private void init() {
		final AWTEventListener myListener = new AWTEventListener() {			
			@Override
			public void eventDispatched(AWTEvent event) {	
				if(!(event instanceof KeyEvent)) return;
				KeyEvent evt = (KeyEvent) event;
				if(evt.getKeyCode()==0 || evt.getKeyCode()==27 || evt.getKeyCode()==39 || evt.getKeyCode()==37 || evt.getKeyCode()==40 || evt.getKeyCode()==10) return;
				setMustAskForExit(true);
			}
		};
   		
   		
   		addWindowFocusListener(new WindowFocusListener() {			
			@Override
			public void windowLostFocus(WindowEvent e) {
				Toolkit.getDefaultToolkit().removeAWTEventListener(myListener);				
			}
			
			@Override
			public void windowGainedFocus(WindowEvent e) {
				Toolkit.getDefaultToolkit().addAWTEventListener(myListener, AWTEvent.KEY_EVENT_MASK);
			}
		});
	}

	public void setMustAskForExit(boolean mustAskForExit) {
		this.mustAskForExit = mustAskForExit;
	}
	
	@Override
	protected boolean mustAskForExit() {
		return mustAskForExit;
	}
	
	@Override
	public final boolean eventWindowClosed() {
		//Check that we can close the window
		boolean res = super.eventWindowClosed();
		//If yes, rollback the given connection's thread (security measure, to prevent unclosed transactions)
		if(res && pushContext!=null) {
			JPAUtil.popEditableContext();
		}
		return res;
	}
	
	 
}
