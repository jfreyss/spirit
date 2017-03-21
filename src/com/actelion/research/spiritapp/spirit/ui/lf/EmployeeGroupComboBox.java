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
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.util.ui.JGenericComboBox;

public class EmployeeGroupComboBox extends JGenericComboBox<EmployeeGroup> {
	
	private boolean onlyWithMembership;
	public EmployeeGroupComboBox(boolean onlyWithMembership) {
		super();
		this.onlyWithMembership = onlyWithMembership;
		repopulate();			
		setPreferredWidth(240);
	}	
		
	public void repopulate() {
		List<EmployeeGroup> res = DBAdapter.getAdapter().getEmployeeGroups();
		List<EmployeeGroup> depts = new ArrayList<>();
		for (EmployeeGroup g: res) {
			if(onlyWithMembership && (SpiritFrame.getUser()==null || !SpiritFrame.getUser().isSuperAdmin() || !SpiritFrame.getUser().isMember(g))) {
				continue;
			}
			depts.add(g);
		}
		
		setValues(depts, true);
		setEnabled(depts.size()>0);
	}
	
	@Override
	public Component processCellRenderer(JLabel comp, EmployeeGroup value, int index) {
		int depth = 0;
		EmployeeGroup eg = value==null? null: value.getParent();
		while(eg!=null && depth<6) {
			depth++;
			eg = eg.getParent();
		}
		comp.setBorder(BorderFactory.createEmptyBorder(0, 10*depth + 5, 0, 0));
		return comp;
	}
	
}
