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

package com.actelion.research.spiritapp.ui.util.lf;

import java.awt.Component;
import java.util.Collection;

import javax.swing.JLabel;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.JObjectComboBox;

public class UserIdComboBox extends JObjectComboBox<Employee> {

	public UserIdComboBox() {
		super();
		setColumns(8);
	}

	@Override
	public Component processCellRenderer(JLabel comp, String value, int index) {
		Employee emp = getMap().get(value);
		if(emp==null) {
			comp.setText(value==null?"": ""+value);
		} else {
			comp.setText("<html><b>" + emp.getUserName() + "</b>" +
					(emp.getEmployeeGroups().isEmpty()?"": "<span style='color:gray;font-size:9px'>  " + MiscUtils.flatten(emp.getEmployeeGroups(), ",") + "</span>"));
		}

		return comp;
	}

	@Override
	public Collection<Employee> getValues() {
		return DBAdapter.getInstance().getEmployees();
	}



	@Override
	public String getToolTipText() {
		Employee emp = getMap().get(getText());
		if(emp==null) return null;
		return emp.getUserName() + (emp.getEmployeeGroups().isEmpty()? "": ": " + MiscUtils.flatten(emp.getEmployeeGroups(), ","));
	}

	@Override
	public String convertObjectToString(Employee obj) {
		return obj==null? "": obj.getUserName();
	}
}
