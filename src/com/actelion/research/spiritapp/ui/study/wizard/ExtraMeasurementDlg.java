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

package com.actelion.research.spiritapp.ui.study.wizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.ui.result.TestComboBox;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class ExtraMeasurementDlg extends JEscapeDialog {

	private final TestComboBox testChoice = new TestComboBox();
	private final JPanel centerPanel = new JPanel(new BorderLayout());
	private final JButton okButton = new JButton("OK");
	private final List<JCustomTextField> tas = new ArrayList<JCustomTextField>();
	private Measurement extraMeasurement;

	public ExtraMeasurementDlg() {
		super(UIUtils.getMainFrame(), "Extra Measurement", true);

		testChoice.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				initUI();
			}
		});
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					eventOk();
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});



		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.NORTH, UIUtils.createHorizontalBox(new JLabel("Test: "), testChoice, Box.createHorizontalGlue()));
		contentPane.add(BorderLayout.CENTER, centerPanel);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));

		setContentPane(contentPane);
		initUI();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);


	}

	private void initUI() {
		Test t = testChoice.getSelection();
		int n = t==null?0: t.getAttributes(OutputType.INPUT).size();
		List<Component> comps = new ArrayList<>();
		while(tas.size()<n) {
			tas.add(new JCustomTextField());
		}
		while(tas.size()>n) {
			tas.remove(n);
		}

		for (int i = 0; i < Math.max(2, n); i++) {
			TestAttribute ta = t==null || i>=n? null: t.getAttributes(OutputType.INPUT).get(i);
			if(ta==null) {
				comps.add(new JLabel());
				comps.add(new JLabel());
			} else {
				comps.add(new JLabel(ta.getName()+": "));
				comps.add(tas.get(i));
			}
		}


		centerPanel.removeAll();
		centerPanel.add(BorderLayout.CENTER, UIUtils.createTable(comps.toArray(new Component[0])));
		pack();

	}
	private void eventOk() throws Exception {
		Test t = testChoice.getSelection();
		if(t==null) throw new Exception("You must select a choice");
		String[] parameters = new String[tas.size()];
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = tas.get(i).getText();
		}

		extraMeasurement = new Measurement(t, parameters);
		dispose();
	}

	public Measurement getExtraMeasurement() {
		return extraMeasurement;
	}

}
