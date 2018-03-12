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

package com.actelion.research.spiritapp.ui.admin.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.component.EmployeeGroupComboBox;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritapp.ui.util.component.UserIdComboBox;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserManagedMode;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JComboCheckBox;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class EmployeeEditDlg extends JSpiritEscapeDialog {
	private JCustomTextField usernameField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 10);
	private JCustomTextField passwordField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 10);
	private UserIdComboBox managerField = new UserIdComboBox();
	private JButton generateButton = new JButton("Reset");

	private JCheckBox disabledCheckBox  = new JCheckBox("Disabled");
	private EmployeeGroupComboBox group1Box = new EmployeeGroupComboBox(false);
	private EmployeeGroupComboBox group2Box = new EmployeeGroupComboBox(false);
	private EmployeeGroupComboBox group3Box = new EmployeeGroupComboBox(false);
	private EmployeeGroupComboBox group4Box = new EmployeeGroupComboBox(false);

	private JComboCheckBox rolesBox;

	private final Employee emp;

	public EmployeeEditDlg(final Employee myEmp) {
		super(UIUtils.getMainFrame(), "Add/Edit User", EmployeeEditDlg.class.getName());
		emp = JPAUtil.reattach(myEmp);


		rolesBox = new JComboCheckBox(SpiritProperties.getInstance().getUserRoles());
		rolesBox.setColumns(25);
		rolesBox.setAllowTyping(false);

		generateButton.addActionListener(e-> {
			char[] symbols = "aeoiubcdfghjklmnprstvxyz".toCharArray();
			char[] voyels = "aeoiu".toCharArray();
			char[] numbers = "0123456789".toCharArray();

			boolean hasNumber = false;
			String pwd = "";
			for (int i = 0; i < 8; i++) {
				if(i>0 && "0123456789".indexOf(pwd.charAt(i-1))>=0 && Math.random()>.5) {
					pwd += numbers[(int)(Math.random()*numbers.length)];
				} else if(i>1 && Math.random()>.2 && !hasNumber) {
					pwd += numbers[(int)(Math.random()*numbers.length)];
					hasNumber = true;
				} else if(i>0 && "aeoiu".indexOf(pwd.charAt(i-1))<0) {
					pwd += voyels[(int)(Math.random()*voyels.length)];
				} else {
					pwd += symbols[(int)(Math.random()*symbols.length)];
				}
			}
			passwordField.setText(pwd);
		});

		passwordField.setTextWhenEmpty(emp.getPassword()!=null && emp.getPassword().length()>0? "********":"");
		usernameField.setText(emp.getUserName());
		managerField.setText(emp.getManager()==null?"": emp.getManager().getUserName());
		Iterator<EmployeeGroup> iter = emp.getEmployeeGroups().iterator();
		if(iter.hasNext()) group1Box.setSelection(iter.next());
		if(iter.hasNext()) group2Box.setSelection(iter.next());
		if(iter.hasNext()) group3Box.setSelection(iter.next());
		if(iter.hasNext()) group4Box.setSelection(iter.next());
		disabledCheckBox.setSelected(emp.isDisabled());


		rolesBox.setCheckedItems(new ArrayList<>(emp.getRoles()).toArray(new String[0]));


		UserManagedMode mode = DBAdapter.getInstance().getUserManagedMode();
		JPanel groupPanel = null;
		if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
			groupPanel = UIUtils.createTitleBox("Groups", UIUtils.createBox(
					UIUtils.createTable(
							new JLabel("Group: "), group1Box),
					new JLabel("<html>All samples created by this user are automatically assigned to his group.<br>"
							+ "All the members of a group can read/edit those samples.")));
		}

		boolean useGroups = SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS);

		JPanel centerPane = UIUtils.createVerticalBox(
				UIUtils.createTitleBox("User", UIUtils.createTable(
						new JLabel("Username: "), UIUtils.createHorizontalBox(usernameField, new JInfoLabel("unique")),
						(mode==UserManagedMode.WRITE_PWD? new JLabel("Password: "): null), (mode==UserManagedMode.WRITE_PWD? UIUtils.createHorizontalBox(passwordField, generateButton): null),
						useGroups? new JLabel("Manager: "): null, useGroups? managerField: null,
								null, disabledCheckBox)),
				groupPanel,
				UIUtils.createTitleBox("Roles", UIUtils.createTable(
						Box.createHorizontalStrut(20), rolesBox)));


		JButton saveButton = new JIconButton(IconType.SAVE, emp.getId()<=0? "Add Employee": "Update");
		saveButton.addActionListener(ev-> {
			try {
				emp.setUserName(usernameField.getText());
				if(managerField.getText().length()>0) {
					Employee manager = DAOEmployee.getEmployee(managerField.getText());
					if(manager==null) throw new Exception("The manager "+managerField.getText()+" does not exist");
					emp.setManager(manager);
				} else {
					emp.setManager(null);
				}

				if(mode==UserManagedMode.WRITE_PWD) {
					if(passwordField.getText().length()>0) {
						emp.setPassword(DBAdapter.getInstance().encryptPassword(passwordField.getText().toCharArray()));
					} else if(emp.getPassword()==null || emp.getPassword().length()==0) {
						int res =JOptionPane.showConfirmDialog(EmployeeEditDlg.this, "You didn't enter any password. Are you sure?", "No Password", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if(res!=JOptionPane.YES_OPTION) return;
					}
				}

				emp.getEmployeeGroups().clear();
				if(group1Box.getSelection()!=null) emp.getEmployeeGroups().add(group1Box.getSelection());
				if(group2Box.getSelection()!=null) emp.getEmployeeGroups().add(group2Box.getSelection());
				if(group3Box.getSelection()!=null) emp.getEmployeeGroups().add(group3Box.getSelection());
				if(group4Box.getSelection()!=null) emp.getEmployeeGroups().add(group4Box.getSelection());


				emp.setRoles(new TreeSet<>(Arrays.asList(rolesBox.getCheckedItems())));

				emp.setDisabled(disabledCheckBox.isSelected());

				DAOEmployee.persistEmployees(Collections.singleton(emp), SpiritFrame.getUser());
				dispose();
			} catch (Exception e) {
				JExceptionDialog.showError(e);
			}
		});



		setContentPane(UIUtils.createBox(centerPane, null, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), saveButton)));
		UIUtils.adaptSize(this, -1, -1);
		setVisible(true);
	}

}
