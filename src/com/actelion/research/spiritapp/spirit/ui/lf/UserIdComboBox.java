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

package com.actelion.research.spiritapp.spirit.ui.lf;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.JTextComboBox;

public class UserIdComboBox extends JTextComboBox {
	
	private static List<String> userIds = null;
	private static List<Employee> employees = null;

	public UserIdComboBox() {
		super(false);
		setColumns(8);
		setListCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(employees==null || index-1<0 || index-1>=employees.size()) {
					setText(value==null?"": ""+value);
				} else {
					Employee emp = employees.get(index-1);
					setText("<html><b>" + emp.getUserName() + "</b>" +
							(emp.getEmployeeGroups().isEmpty()?"": "<span style='color:gray;font-size:9px'>  " + MiscUtils.flatten(emp.getEmployeeGroups(), ",") + "</span>"));
				}
				
				return this;
			}
		});
		employees = DBAdapter.getAdapter().getEmployees();
	}
	
	@Override
	public Collection<String> getChoices() {
		userIds = new ArrayList<>();
		
		for(Employee emp: employees) {
			userIds.add(emp.getUserName());
		}
		return userIds;
	}
	
	

	@Override
	public String getToolTipText() {
		if(employees==null) getChoices();		
		if(employees==null) return null;
		
		for (Employee e : employees) {
			if(e.getUserName().equals(getText())) {
				return e.getUserName() + (e.getEmployeeGroups().isEmpty()? "": ": " + MiscUtils.flatten(e.getEmployeeGroups(), ","));
			}
		}
		return null;
	}
	
}
