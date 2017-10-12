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

import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserManagedMode;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class EmployeePanel extends JPanel {

	private JCheckBox activeCheckbox = new JCheckBox("Hide disabled users", true);
	private JCustomTextField filterField = new JCustomTextField(CustomFieldType.ALPHANUMERIC);
	private EmployeeTable employeeTable = new EmployeeTable();

	public EmployeePanel() {

		final JButton deleteUserButton = new JIconButton(IconType.DELETE, "Delete User");
		final JButton editUserButton = new JIconButton(IconType.EDIT, "Edit User");
		final JButton createUserButton = new JIconButton(IconType.NEW, "Create User");

		boolean editable = DBAdapter.getInstance().getUserManagedMode()==UserManagedMode.WRITE_NOPWD || DBAdapter.getInstance().getUserManagedMode()==UserManagedMode.WRITE_PWD;
		deleteUserButton.setVisible(editable);
		editUserButton.setVisible(editable);
		createUserButton.setVisible(editable);

		deleteUserButton.addActionListener(ev-> {
			List<Employee> sel = employeeTable.getSelection();
			if(sel.size()==1) {
				int res = JOptionPane.showConfirmDialog(EmployeePanel.this, "Are you sure you want to delete " + sel.get(0)+"?", "Delete user", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(res!=JOptionPane.YES_OPTION) return;

				try {
					DAOEmployee.removeEmployee(sel.get(0), SpiritFrame.getUser());
					SpiritFrame.clearAll();
					refresh();
				} catch (Exception e) {
					JExceptionDialog.showError(e);
				}
			}
		});
		editUserButton.addActionListener(ev-> {
			List<Employee> sel = employeeTable.getSelection();
			if(sel.size()==1) {
				new EmployeeEditDlg(sel.get(0));
				SpiritFrame.clearAll();
				refresh();
				employeeTable.setSelection(sel);
			}
		});
		createUserButton.addActionListener(ev-> {
			createUser("");
		});

		employeeTable.getModel().setTreeViewActive(false);
		//		if(editable) {
		//			employeeTable.addMouseListener(new MouseAdapter() {
		//				@Override
		//				public void mouseClicked(MouseEvent e) {
		//					if(e.getClickCount()>=2) {
		//						editUserButton.getActionListeners()[0].actionPerformed(null);
		//					}
		//				}
		//			});
		//		}

		activeCheckbox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				refresh();
			}
		});


		filterField.setOpaque(false);
		filterField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				refresh();
			}
		});

		setLayout(new GridLayout());
		add(UIUtils.createBox(
				UIUtils.createTitleBox("Users", new JScrollPane(employeeTable)),
				UIUtils.createTitleBox("Filter", UIUtils.createHorizontalBox(new JLabel("User: "), filterField, activeCheckbox, Box.createHorizontalGlue())),
				UIUtils.createHorizontalBox(deleteUserButton, editUserButton, createUserButton, Box.createHorizontalGlue())));


		refresh();
	}

	public void refresh() {
		List<Employee> res = new ArrayList<>();
		for(Employee emp: DAOEmployee.getEmployees()) {
			if(activeCheckbox.isSelected() && emp.isDisabled()) continue;
			if(filterField.getText().length()>0) {
				if(emp.getUserName().toLowerCase().contains(filterField.getText().toLowerCase())) {
					//Ok
				} else {
					boolean ok = false;
					for (EmployeeGroup g : emp.getEmployeeGroups()) {
						if(g.getName().toLowerCase().contains(filterField.getText().toLowerCase())) { ok=true; break;}
					}
					if(!ok) continue;
				}
			}

			res.add(emp);
		}
		employeeTable.setRows(res);
	}

	private void createUser(String prefix) {
		Employee emp = new Employee();
		if(prefix!=null) emp.setUserName(prefix);
		new EmployeeEditDlg(emp);
		SpiritFrame.clearAll();

		refresh();
		employeeTable.setSelection(Collections.singletonList(emp));
	}
}
