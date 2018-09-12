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

package com.actelion.research.spiritapp.ui.admin.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JTabbedPane;

import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class ConfigDlg extends JEscapeDialog {

	private JTabbedPane tabbedPane = new JCustomTabbedPane();

	private ConfigWeighingTab weighingTab = new ConfigWeighingTab();

	public ConfigDlg() {
		super(UIUtils.getMainFrame(), "Config", true);

		tabbedPane.add("Weighing Balance", weighingTab);


		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				eventOk();
			}
		});

		setContentPane(UIUtils.createBox(tabbedPane, null, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton)));
		UIUtils.adaptSize(this, -1, -1);
		setVisible(true);
	}

	private void eventOk() {
		try {
			weighingTab.eventOk();
			dispose();
		} catch (Exception e) {
			JExceptionDialog.showError(e);
		}
	}
}
