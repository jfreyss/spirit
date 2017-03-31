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
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
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

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.result.TestComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class TestMoveDlg extends JSpiritEscapeDialog {


	private JGenericComboBox<TestAttribute> attComboBox = new JGenericComboBox<TestAttribute>();
	private JGenericComboBox<String> valuesComboBox = new JGenericComboBox<String>();
	//private JTextField newValueTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 10);
	private JButton moveButton = new JButton(new Action_Move());
	//private JLabel infoLabel = new JLabel();
	private List<TestAttribute> inputAtts;

	private TestComboBox tSrc;
	private TestComboBox tDest;
	private SpiritUser user;

	public TestMoveDlg(Test myTest) {
		super(UIUtils.getMainFrame(), "Admin - Tests - Move", TestMoveDlg.class.getName());
		Test test = JPAUtil.reattach(myTest);

		try{
			user = Spirit.askForAuthentication();
			if(test==null) throw new Exception("Please select a test");

			inputAtts = test.getInputAttributes();
			if(inputAtts.size()==0) throw new Exception("You must select a test with inputAttributes");
		} catch (Exception e) {
			JExceptionDialog.showError(e);
			return;
		}

		tSrc = new TestComboBox();
		tDest = new TestComboBox();

		tSrc.setSelection(test);
		tSrc.setEnabled(false);

		attComboBox = new JGenericComboBox<TestAttribute>(inputAtts, true);



		Box line1 = Box.createHorizontalBox();
		line1.add(new JLabel("Move: "));
		line1.add(tSrc);
		line1.add(Box.createHorizontalGlue());

		Box line2 = Box.createHorizontalBox();
		line2.add(new JLabel("     Where  "));
		line2.add(attComboBox);
		line2.add(new JLabel(" = "));
		line2.add(valuesComboBox);
		line2.add(Box.createHorizontalGlue());

		Box line3 = Box.createHorizontalBox();
		line3.add(new JLabel(" "));
		line3.add(Box.createHorizontalGlue());

		Box line4 = Box.createHorizontalBox();
		line4.add(new JLabel("To: "));
		line4.add(tDest);
		line4.add(Box.createHorizontalGlue());

		Box centerPane = Box.createVerticalBox();
		centerPane.setBorder(BorderFactory.createEtchedBorder());
		centerPane.add(line1);
		centerPane.add(line2);
		centerPane.add(line3);
		centerPane.add(line4);

		tDest.addTextChangeListener(e-> {
			Test t = tDest.getSelection();
			if(t==null) {
			} else if(t.getInputAttributes().size()>0) {
				JExceptionDialog.showError(TestMoveDlg.this, "Tests with input attributes are still not allowed");
			} else {

			}
		});

		attComboBox.addItemListener(e-> {
			if(e.getStateChange()!=ItemEvent.SELECTED) return;

			TestAttribute att = attComboBox.getSelection();
			Set<String> values;
			if(att==null) {
				values = new HashSet<String>();
			} else {
				values = DAOTest.getAutoCompletionFields(att);
			}
			valuesComboBox.setValues(values, true);
		});

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, centerPane);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new Action_Close()), moveButton));
		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}


	private class Action_Move extends AbstractAction {
		public Action_Move() {
			super("Move");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Test src = tSrc.getSelection();
				Test dest = tDest.getSelection();
				TestAttribute srcAtt = attComboBox.getSelection();
				String srcVal = valuesComboBox.getSelection();

				if(inputAtts.size()==0 && srcAtt==null) {
					throw new Exception("Please select an attribute");
				}

				int res = DAOResult.move(src, srcAtt, srcVal, dest, user);
				JOptionPane.showMessageDialog(UIUtils.getMainFrame(), res + " values moved", "Success", JOptionPane.INFORMATION_MESSAGE);
				dispose();
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
