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

package com.actelion.research.spiritapp.spirit.ui.result.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOResult.FindDuplicateMethod;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class ResultDuplicatesDlg extends JEscapeDialog {

	private JTextComboBox elb1ComboBox;
	private JTextComboBox elb2ComboBox;
	private JGenericComboBox<FindDuplicateMethod> methodComboBox = new JGenericComboBox<>(FindDuplicateMethod.values(), false);


	public ResultDuplicatesDlg() {
		super(UIUtils.getMainFrame(), "Find Duplicates Results", true);

		List<String> elbs = DAOResult.getRecentElbs(SpiritFrame.getUser());
		elbs.add(0, "");
		elb1ComboBox = new JTextComboBox(elbs);
		elb2ComboBox = new JTextComboBox(elbs);

		//Buttons
		JButton queryButton = new JIconButton(IconType.SEARCH, "Query");
		queryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				new SwingWorkerExtended("Duplicates", getContentPane()) {
					List<Result> results;
					@Override
					protected void doInBackground() throws Exception {
						String elb1 = elb1ComboBox.getText().trim();
						if(elb1.length()==0) throw new Exception("You must select an elb");

						String elb2 = elb1ComboBox.getText().trim();
						if(elb2.length()==0) elb2 = elb1;

						results = DAOResult.findDuplicates(elb1, elb2, methodComboBox.getSelection(), SpiritFrame.getUser());
					}
					@Override
					protected void done() {
						JOptionPane.showMessageDialog(ResultDuplicatesDlg.this, "The query returned " + results.size() + " results", "Results", JOptionPane.INFORMATION_MESSAGE);
						SpiritContextListener.setResults(results);
						dispose();
					}
				};
			}
		});


		//contentPanel
		setContentPane(UIUtils.createBox(
				UIUtils.createTitleBox(
						UIUtils.createVerticalBox(
								new JLabel("<html><i>This function will query all duplicates and display them in the interface.<br>You can give either:<br><li>One ELB to find duplicates among one ELB.<li>Two ELBs to find duplicates across 2 ELBS.</i>"),
								UIUtils.createTable(
										new JLabel("ELB 1: "), elb1ComboBox,
										new JLabel("ELB 2: (opt.)"), elb2ComboBox),
								methodComboBox
								)),
				null,
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), queryButton)));
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);

	}

}
