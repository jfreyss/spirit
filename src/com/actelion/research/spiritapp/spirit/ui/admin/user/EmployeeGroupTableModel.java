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

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;

public class EmployeeGroupTableModel extends ExtendTableModel<EmployeeGroup> {

	public static final Column<EmployeeGroup, String> COLUMN_NAME = new Column<EmployeeGroup, String>("GroupName", String.class, 100) {		
		@Override
		public String getValue(EmployeeGroup row) {
			return row.getName();
		}
	};
	
	@Override
	public Column<EmployeeGroup,?> getTreeColumn() {
		return COLUMN_NAME;
	}
	
	@Override
	public List<EmployeeGroup> getTreeChildren(EmployeeGroup row) {
		return new ArrayList<EmployeeGroup>(row.getChildren());
	}

	
	public EmployeeGroupTableModel() {
		List<Column<EmployeeGroup,?>> columns = new ArrayList<>();
		columns.add(COLUMN_ROWNO);
		columns.add(COLUMN_NAME);		
		setColumns(columns);
	}
	
}
