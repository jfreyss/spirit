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

package com.actelion.research.spiritapp.spirit.ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.TextChangeListener;
import com.actelion.research.util.ui.UIUtils;

public class TestRenameAttDlg extends JSpiritEscapeDialog {


	private JGenericComboBox<TestAttribute> attComboBox = new JGenericComboBox<>();
	private JTextComboBox valuesComboBox = new JTextComboBox();
	private JTextField newValueTextField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 10);
	private JButton renameButton = new JButton(new Action_Rename());
	private JLabel infoLabel = new JLabel();

	public TestRenameAttDlg(Test myTest) {
		super(UIUtils.getMainFrame(), "Admin - Tests - Rename Attribute", TestRenameAttDlg.class.getName());

		Test test = JPAUtil.reattach(myTest);
		if(test==null) throw new IllegalArgumentException("Please select a test");

		String text = "<html><table>";
		List<TestAttribute> atts = new ArrayList<>();
		for (TestAttribute att : test.getAttributes()) {
			if(att.getDataType()==DataType.AUTO || att.getDataType()==DataType.LIST) {
				atts.add(att);
			}
			text += "<tr><td>" + att.getIndex() + "</td><td><b>" + att.getName() + "</td><td>"+ att.getDataType().getName() + "</td></tr>";
		}
		text += "</table></html>";
		JLabel testLabel = new JLabel(text);

		attComboBox = new JGenericComboBox<TestAttribute>(atts, true);

		Box line1 = Box.createHorizontalBox();
		line1.add(new JLabel("Attribute: "));
		line1.add(attComboBox);
		line1.add(valuesComboBox);
		line1.add(infoLabel);
		infoLabel.setForeground(Color.GRAY);
		line1.add(Box.createHorizontalGlue());

		Box line2 = Box.createHorizontalBox();
		line2.add(new JLabel(" "));
		line2.add(testLabel);
		testLabel.setForeground(Color.LIGHT_GRAY);
		line2.add(Box.createHorizontalGlue());

		Box line3 = Box.createHorizontalBox();
		line3.add(new JLabel("Rename To: "));
		line3.add(newValueTextField);
		line3.add(renameButton);
		line3.add(Box.createHorizontalGlue());

		Box centerPane = Box.createVerticalBox();
		centerPane.setBorder(BorderFactory.createEtchedBorder());
		centerPane.add(line1);
		centerPane.add(line2);
		centerPane.add(Box.createVerticalGlue());
		centerPane.add(line3);

		attComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()!=ItemEvent.SELECTED) return;
				repopulateValues();

			}
		});
		valuesComboBox.addTextChangeListener(new TextChangeListener() {
			@Override
			public void textChanged(javax.swing.JComponent src) {
				String value = valuesComboBox.getText();
				newValueTextField.setText(value);
				renameButton.setEnabled(value!=null);
			}
		});

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, centerPane);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new Action_Close())));
		setContentPane(contentPane);
		setSize(600, 195);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

	private void repopulateValues() {
		TestAttribute att = attComboBox.getSelection();
		Set<String> values;
		if(att==null) {
			values = new HashSet<>();
			infoLabel.setText("");
		} else {
			values = DAOTest.getAutoCompletionFields(att);
			infoLabel.setText("id:"+att.getId());
		}
		valuesComboBox.setChoices(values);
	}

	private class Action_Rename extends AbstractAction {
		public Action_Rename() {
			super("Rename");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				SpiritUser user = Spirit.askForAuthentication();
				TestAttribute att = attComboBox.getSelection();
				String value = valuesComboBox.getText();
				String newValue = newValueTextField.getText();
				if(att==null || value==null) throw new Exception("Please select an attribute and a value");
				if(newValue.length()==0) throw new Exception("Please enter a new value");
				int res = DAOResult.rename(att, value, newValue, user);
				JOptionPane.showMessageDialog(UIUtils.getMainFrame(), res + " values renamed", "Success", JOptionPane.INFORMATION_MESSAGE);
				repopulateValues();
				valuesComboBox.setText(newValue);

			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}

	private class Action_Close extends AbstractAction {
		public Action_Close() {
			super("Close");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	}
}
