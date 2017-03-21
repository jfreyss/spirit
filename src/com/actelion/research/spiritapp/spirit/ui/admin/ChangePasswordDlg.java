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
import java.awt.event.ActionListener;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class ChangePasswordDlg extends JEscapeDialog {
	
	private Employee employee; 
	private JPasswordField oldField = new JPasswordField(10);
	private JPasswordField new1Field = new JPasswordField(10);
	private JPasswordField new2Field = new JPasswordField(10);
	
	
	public ChangePasswordDlg() {
		super(UIUtils.getMainFrame(), "Change Password", true);

		SpiritUser user = SpiritFrame.getUser();
		if(user==null) return;
		
		employee = DAOEmployee.getEmployee(user.getUsername());
		if(employee==null) {
			throw new RuntimeException("Invalid user: "+user);
		}
		
		JButton okButton = new JButton("Change Password");
		okButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					eventOk();
					dispose();
					SpiritChangeListener.fireModelChanged(SpiritChangeType.LOGIN);
				} catch (Exception e2) {
					JExceptionDialog.showError(e2);
				} 
			}
		});
		
		JPanel centerPane = UIUtils.createBox(BorderFactory.createEtchedBorder(),
				UIUtils.createTable(
						new JLabel("Old password: "), oldField,
						Box.createVerticalStrut(10), null,
						new JLabel("New password: "), new1Field,
						new JLabel("Retype password : "), new2Field)
				);
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, centerPane);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));
		setContentPane(contentPane);
		
		
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

	private void eventOk() throws Exception {
		//Check old password
		DAOSpiritUser.authenticateUser(employee.getUserName(), oldField.getPassword());

		//Validate password
		if(new1Field.getPassword().length<6) throw new Exception("Your new password must have at least 6 characters");
		if(!new String(new1Field.getPassword()).equals(new String(new2Field.getPassword()))) throw new Exception("Your passwords don't match");
		
		employee.setPassword(DBAdapter.getAdapter().encryptPassword(new1Field.getPassword()));
		DAOEmployee.persistEmployees(Collections.singleton(employee), SpiritFrame.getUser());
	}
}
