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

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JTabbedPane;

import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.UIUtils;

public class UserAdminDlg extends JEscapeDialog {
	private EmployeePanel employeePanel = new EmployeePanel();
	private EmployeeGroupPanel employeeGroupPanel = new EmployeeGroupPanel();
	
	public UserAdminDlg() {
		super(UIUtils.getMainFrame(), "User/Group Administration");
		
		JTabbedPane tabbedPane = new JCustomTabbedPane();
		tabbedPane.add("Users", employeePanel);
		tabbedPane.add("Groups", employeeGroupPanel);

		setContentPane(UIUtils.createBox(tabbedPane, null, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new CloseAction()))));

//		//Open employee creation if there are no employees
//		if(employeeTable.getRows().size()==0) {
//			createUser("admin_spirit");
//		}
		
		UIUtils.adaptSize(this, 800, 620);
		setVisible(true);
	}

}