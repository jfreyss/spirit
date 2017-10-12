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

package com.actelion.research.spiritapp.spirit.ui.util.lf;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JLabel;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JObjectComboBox;

public class EmployeeGroupComboBox extends JObjectComboBox<EmployeeGroup> {

	private boolean onlyWithMembership;

	public EmployeeGroupComboBox(boolean onlyWithMembership) {
		super();
		this.onlyWithMembership = onlyWithMembership;
	}

	@Override
	public Component processCellRenderer(JLabel comp, String name, int index) {
		EmployeeGroup eg = getMap().get(name);
		int depth = eg==null? 0: eg.getDepth();
		if(eg!=null) {
			boolean member = SpiritFrame.getUser()!=null && SpiritFrame.getUser().isMember(eg);
			comp.setFont(member? FastFont.BOLD: FastFont.REGULAR);
			comp.setText(MiscUtils.repeat("  ", depth) + eg.getName());
		}
		return comp;
	}

	@Override
	public Collection<EmployeeGroup> getValues() {
		List<EmployeeGroup> list = new ArrayList<>();
		for (EmployeeGroup g: DBAdapter.getInstance().getEmployeeGroups()) {
			if(onlyWithMembership && (SpiritFrame.getUser()==null || !SpiritFrame.getUser().isSuperAdmin()) && !SpiritFrame.getUser().isMember(g)) {
				continue;
			}
			list.add(g);
		}
		return list;
	}

}
