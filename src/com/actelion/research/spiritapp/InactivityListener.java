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

package com.actelion.research.spiritapp;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.Timer;

public class InactivityListener implements ActionListener, AWTEventListener {
	public final static long KEY_EVENTS = AWTEvent.KEY_EVENT_MASK;
	public final static long MOUSE_EVENTS = AWTEvent.MOUSE_MOTION_EVENT_MASK + AWTEvent.MOUSE_EVENT_MASK;
	public final static long USER_EVENTS = KEY_EVENTS + MOUSE_EVENTS;

	private Window window;
	private Action action;
	private int interval;
	private long eventMask;
	private Timer timer = new Timer(0, this);

	public InactivityListener(Window window) {
		this(window, null, 1);
	}

	/*
	 *  Use a default inactivity interval of 1 minute and listen for
	 *  USER_EVENTS
	 */
	public InactivityListener(Window window, Action action) {
		this(window, action, 1);
	}

	/*
	 *	Specify the inactivity interval and listen for USER_EVENTS
	 */
	public InactivityListener(Window window, Action action, int interval) {
		this(window, action, interval, USER_EVENTS);
	}

	/*
	 *  Specify the inactivity interval and the events to listen for
	 */
	public InactivityListener(Window window, Action action, int minutes, long eventMask) {
		this.window = window;
		setAction( action );
		setInterval( minutes );
		setEventMask( eventMask );
	}

	/*
	 *  The Action to be invoked after the specified inactivity period
	 */
	public void setAction(Action action) {
		this.action = action;
	}

	/*
	 *  The interval before the Action is invoked specified in minutes
	 */
	public void setInterval(int minutes) {
		setIntervalInMillis(minutes * 60000);
	}

	/*
	 *  The interval before the Action is invoked specified in milliseconds
	 */
	public void setIntervalInMillis(int interval) {
		this.interval = interval;
		timer.setInitialDelay(interval);
	}

	/*
	 *	A mask specifying the events to be passed to the AWTEventListener
	 */
	public void setEventMask(long eventMask) {
		this.eventMask = eventMask;
	}

	/*
	 *  Start listening for events.
	 */
	public void start() {
		timer.setInitialDelay(interval);
		timer.setRepeats(false);
		timer.start();
		Toolkit.getDefaultToolkit().addAWTEventListener(this, eventMask);
	}

	/*
	 *  Stop listening for events
	 */
	public void stop() {
		Toolkit.getDefaultToolkit().removeAWTEventListener(this);
		timer.stop();
	}

	//  Implement ActionListener for the Timer

	@Override
	public void actionPerformed(ActionEvent e) {
		if(action!=null) {
			ActionEvent ae = new ActionEvent(window, ActionEvent.ACTION_PERFORMED, "");
			action.actionPerformed(ae);
		}
	}

	//  Implement AWTEventListener

	@Override
	public void eventDispatched(AWTEvent e) {
		if (timer.isRunning())
			timer.restart();
	}
}
