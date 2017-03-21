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

package com.actelion.research.spiritapp.spirit.ui.admin.user;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.lf.EmployeeGroupComboBox;
import com.actelion.research.spiritapp.spirit.ui.lf.UserIdComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class EmployeeEditDlg extends JSpiritEscapeDialog {
	private JCustomTextField usernameField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 10);
	private JCustomTextField passwordField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 10);
	private UserIdComboBox managerField = new UserIdComboBox();
	private JButton generateButton = new JButton("Reset");

	private JCheckBox disabledCheckBox  = new JCheckBox("Disabled");
	private EmployeeGroupComboBox group1Box = new EmployeeGroupComboBox(false);
	private EmployeeGroupComboBox group2Box = new EmployeeGroupComboBox(false);
	private EmployeeGroupComboBox group3Box = new EmployeeGroupComboBox(false);
	private EmployeeGroupComboBox group4Box = new EmployeeGroupComboBox(false);
	
	private JGenericComboBox<String> role1Box;
	private JGenericComboBox<String> role2Box;
	private JGenericComboBox<String> role3Box;
	private JGenericComboBox<String> role4Box;

	private final Employee emp;
	
	public EmployeeEditDlg(final Employee myEmp) {
		super(UIUtils.getMainFrame(), "Add/Edit User", EmployeeEditDlg.class.getName());
		emp = JPAUtil.reattach(myEmp);
		
		
		role1Box = new JGenericComboBox<>(SpiritProperties.getInstance().getUserRoles(), true);
		role2Box = new JGenericComboBox<>(SpiritProperties.getInstance().getUserRoles(), true);
		role3Box = new JGenericComboBox<>(SpiritProperties.getInstance().getUserRoles(), true);
		role4Box = new JGenericComboBox<>(SpiritProperties.getInstance().getUserRoles(), true);
		
		generateButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
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
			}
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
		
		Iterator<String> iter2 = emp.getRoles().iterator();
		if(iter2.hasNext()) role1Box.setSelection(iter2.next());
		if(iter2.hasNext()) role2Box.setSelection(iter2.next());
		if(iter2.hasNext()) role3Box.setSelection(iter2.next());
		if(iter2.hasNext()) role4Box.setSelection(iter2.next());
		
		
		JPanel centerPane = UIUtils.createVerticalBox(
				UIUtils.createTitleBox("User", UIUtils.createTable(
						new JLabel("Username: "), UIUtils.createHorizontalBox(usernameField, new JInfoLabel("unique")),
						new JLabel("Password: "), UIUtils.createHorizontalBox(passwordField, generateButton),
						new JLabel("Manager: "), managerField,
						null, disabledCheckBox)),
				UIUtils.createTitleBox("Groups", UIUtils.createBox(
						UIUtils.createTable(
							new JLabel("Group: "), group1Box),
						new JLabel("<html>All samples created by this user are automatically assigned to his group.<br>"
								+ "All the members of a group can read/edit those samples."))),
					UIUtils.createTitleBox("Roles", UIUtils.createBox(							 
							UIUtils.createTable(
								Box.createHorizontalStrut(20), role1Box,
								null, role2Box,
								null, role3Box,
								null, role4Box),
							new JLabel("<html>Roles are specific rights to be given to a user.")))
				);

		
		JButton saveButton = new JIconButton(IconType.SAVE, emp.getId()<=0? "Add Employee": "Update");
		saveButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					emp.setUserName(usernameField.getText());
					if(managerField.getText().length()>0) {
						Employee manager = DAOEmployee.getEmployee(managerField.getText());
						if(manager==null) throw new Exception("The manager "+managerField.getText()+" does not exist");
						emp.setManager(manager);
					} else {
						emp.setManager(null);
					}
					
					if(passwordField.getText().length()>0) {
						emp.setPassword(DBAdapter.getAdapter().encryptPassword(passwordField.getText().toCharArray()));
					} else if(emp.getPassword()==null || emp.getPassword().length()==0) {
						int res =JOptionPane.showConfirmDialog(EmployeeEditDlg.this, "You didn't enter any password. Are you sure?", "No Password", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if(res!=JOptionPane.YES_OPTION) return;
					}
					
					emp.getEmployeeGroups().clear();
					if(group1Box.getSelection()!=null) emp.getEmployeeGroups().add(group1Box.getSelection());
					if(group2Box.getSelection()!=null) emp.getEmployeeGroups().add(group2Box.getSelection());
					if(group3Box.getSelection()!=null) emp.getEmployeeGroups().add(group3Box.getSelection());
					if(group4Box.getSelection()!=null) emp.getEmployeeGroups().add(group4Box.getSelection());
					
					Set<String> roles = new HashSet<>();
					if(role1Box.getSelection()!=null) roles.add(role1Box.getSelection());
					if(role2Box.getSelection()!=null) roles.add(role2Box.getSelection());
					if(role3Box.getSelection()!=null) roles.add(role3Box.getSelection());
					if(role4Box.getSelection()!=null) roles.add(role4Box.getSelection());
					
					emp.setRoles(roles);
					
					emp.setDisabled(disabledCheckBox.isSelected());
					
					DAOEmployee.persistEmployees(Collections.singleton(emp), SpiritFrame.getUser());
					dispose();
				} catch (Exception e) {
					JExceptionDialog.showError(e);
				}
			}
		});
		
		
				
		setContentPane(UIUtils.createBox(centerPane, null, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), saveButton)));
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

}
