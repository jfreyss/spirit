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

package com.actelion.research.spiritapp.ui.util.component;

import java.awt.Robot;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;

import com.actelion.research.util.ui.JExceptionDialog;

public class BalanceMonitoringThread extends Thread {

	private JDialog dlg;
	private MTBalance balance;


	public BalanceMonitoringThread(JDialog dlg, MTBalance balance) {
		this.dlg = dlg;
		this.balance = balance;
	}

	@Override
	public void run() {
		while(!isInterrupted()) {
			try {Thread.sleep(200);}catch (Exception e) {e.printStackTrace(); return;}
			if(! dlg.isFocused()) continue;

			try {
				List<Double> weights = balance.getWeights();
				if(weights.size()>0) {
					Double w = weights.get(0);
					if(w==null || w<=0) continue;

					//Check that we are on a textcomponent
					if(dlg.getFocusOwner() instanceof JTextComponent) {
						((JTextComponent)dlg.getFocusOwner()).selectAll();
					} else if(dlg.getFocusOwner() instanceof JTable) {
						//OK
					} else {
						continue;
					}

					java.awt.Toolkit.getDefaultToolkit().beep();
					//Use the robot
					Robot robot = new Robot();
					robot.setAutoWaitForIdle(true);
					robot.setAutoDelay(5);
					for (char c : (""+w).toCharArray()) {
						robot.keyPress(c);
						robot.keyRelease(c);
					}
					robot.keyPress('\n');
					robot.keyRelease('\n');
					robot.waitForIdle();
				}
			} catch (Exception e) {
				if(!isInterrupted()) {
					JExceptionDialog.showError(e);
				}
				break;
			}
		}
	}
}