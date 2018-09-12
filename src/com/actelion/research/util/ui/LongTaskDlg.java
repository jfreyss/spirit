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

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Class used to do a long task in the backround, while displaying a message dialog.
 * This code is blocking
 * @author freyssj
 *
 */
public abstract class LongTaskDlg extends JDialog {


	public LongTaskDlg(String title) {


		//Open Dialog
		super(UIUtils.getMainFrame(), title, true);

		JPanel centerPane = new JPanel();
		Component spacer = Box.createRigidArea(new Dimension(140, 140));
		centerPane.add(spacer);
		//		centerPane.setBorder(BorderFactory.createRaisedBevelBorder());
		setContentPane(centerPane);
		setUndecorated(true);

		UIUtils.adaptSize(this, -1, -1);
		new SwingWorkerExtended(title, centerPane, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			@Override
			protected void doInBackground() throws Exception {
				System.out.println("LongTaskDlg.LongTaskDlg(...).new SwingWorkerExtended() {...}.doInBackground()");
				longTask();
			}
			@Override
			protected void finalize() {
				System.out.println("LongTaskDlg.LongTaskDlg(...).new SwingWorkerExtended() {...}.finalize()");
				LongTaskDlg.this.setVisible(false);
			};
		};
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);

	}
	public abstract void longTask() throws Exception;

	public static void main(String[] args) throws Exception {

		new LongTaskDlg("LONG WAIT") {
			@Override
			public void longTask() throws Exception {
				Thread.sleep(10000);
			}
		};
	}


}
