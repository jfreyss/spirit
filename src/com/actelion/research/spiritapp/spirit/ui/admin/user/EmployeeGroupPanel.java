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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class EmployeeGroupPanel extends JPanel {

	private EmployeeGroupTable employeeGroupTable = new EmployeeGroupTable();

	public EmployeeGroupPanel() {

		final JButton deleteButton = new JIconButton(IconType.DELETE, "Delete");
		final JButton editButton = new JIconButton(IconType.EDIT, "Edit");
		final JButton createButton = new JIconButton(IconType.NEW, "Create Group");

		deleteButton.addActionListener(ev-> {
			List<EmployeeGroup> sel = employeeGroupTable.getSelection();
			if(sel.size()==1) {
				int res = JOptionPane.showConfirmDialog(EmployeeGroupPanel.this, "Are you sure you want to delete " + sel.get(0)+"?", "Delete Group", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(res!=JOptionPane.YES_OPTION) return;

				try {
					DAOEmployee.removeEmployeeGroup(sel.get(0), SpiritFrame.getUser());
					refresh();
				} catch (Exception e) {
					JExceptionDialog.showError(e);
				}
			}
		});
		editButton.addActionListener(e-> {
			List<EmployeeGroup> sel = employeeGroupTable.getSelection();
			if(sel.size()==1) {
				new EmployeeGroupEditDlg(sel.get(0));
				refresh();
				employeeGroupTable.setSelection(sel);
			}
		});
		createButton.addActionListener(e-> {
			EmployeeGroup group = new EmployeeGroup();
			new EmployeeGroupEditDlg(group);
			refresh();
			employeeGroupTable.setSelection(Collections.singletonList(group));
		});

		employeeGroupTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount()==2) {
					editButton.getActionListeners()[0].actionPerformed(null);
				}
			}
		});

		setLayout(new GridLayout());
		add(UIUtils.createBox(
				UIUtils.createTitleBox("Groups", new JScrollPane(employeeGroupTable)),
				null,
				UIUtils.createHorizontalBox(deleteButton, editButton, createButton, Box.createHorizontalGlue())));



		refresh();


	}

	public void refresh() {
		SpiritFrame.clearAll();
		List<EmployeeGroup> emps = DBAdapter.getAdapter().getEmployeeGroups();
		employeeGroupTable.setRows(emps);
	}
}
