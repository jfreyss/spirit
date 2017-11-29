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

package com.actelion.research.spiritapp.ui.admin.user;

import java.awt.BorderLayout;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritapp.ui.util.lf.EmployeeGroupComboBox;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class EmployeeGroupEditDlg extends JSpiritEscapeDialog {

	private JCustomTextField nameField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 25);
	private EmployeeGroupComboBox parentComboBox = new EmployeeGroupComboBox(true);
	private EmployeeGroup group;

	public EmployeeGroupEditDlg(EmployeeGroup myGroup) {
		super(UIUtils.getMainFrame(), "Add/Edit Group", EmployeeGroupEditDlg.class.getName());

		group = JPAUtil.reattach(myGroup);
		nameField.setText(group.getName());
		parentComboBox.setSelection(group.getParent());

		JPanel centerPane = UIUtils.createTable(
				new JLabel("GroupName: "), nameField,
				new JLabel("Parent: "), parentComboBox);
		centerPane.setBorder(BorderFactory.createEtchedBorder());

		JButton saveButton = new JIconButton(IconType.SAVE, group.getId()<=0? "Add Group": "Update");
		saveButton.addActionListener(ev-> {
			try {
				group.setName(nameField.getText());
				group.setParent(parentComboBox.getSelection());
				DAOEmployee.persistEmployeeGroups(Collections.singleton(group), SpiritFrame.getUser());
				dispose();
			} catch (Exception e) {
				JExceptionDialog.showError(e);
			}
		});


		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, centerPane);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), saveButton));

		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

}
