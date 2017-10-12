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

import javax.swing.JComponent;

import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;

public class EmployeeTableModel extends ExtendTableModel<Employee> {

	public static final Column<Employee, String> COLUMN_ACTIVE = new Column<Employee, String>("Status", String.class, 40) {
		@Override
		public String getValue(Employee row) {
			return row.isDisabled()?"Disabled": null;
		}
	};
	public static final Column<Employee, String> COLUMN_NAME = new Column<Employee, String>("Username", String.class, 110) {
		@Override
		public String getValue(Employee row) {
			return row.getUserName();
		}
		@Override
		public void postProcess(AbstractExtendTable<Employee> table, Employee row, int rowNo, Object value, JComponent comp) {
			comp.setFont(FastFont.BOLD);
		}
	};
	public static final Column<Employee, String> COLUMN_MANAGER = new Column<Employee, String>("Manager", String.class, 60) {
		@Override
		public String getValue(Employee row) {
			return row.getManager()==null?null: row.getManager().getUserName();
		}
	};
	public static final Column<Employee, String> COLUMN_ROLES = new Column<Employee, String>("Roles", String.class, 120) {
		@Override
		public String getValue(Employee row) {
			return MiscUtils.flatten(row.getRoles(), " ");
		}
	};
	public static final Column<Employee, String> COLUMN_GROUPS = new Column<Employee, String>("Group", String.class, 120) {
		@Override
		public String getValue(Employee row) {
			if(row.getEmployeeGroups().isEmpty()) return null;
			String s = MiscUtils.flatten(row.getEmployeeGroups(), ", ");
			return s;
		}
	};

	@Override
	public Column<Employee,?> getTreeColumn() {
		return COLUMN_NAME;
	}

	@Override
	public List<Employee> getTreeChildren(Employee row) {
		return row==null? new ArrayList<>(): new ArrayList<Employee>(row.getChildren());
	}
	@Override
	public Employee getTreeParent(Employee row) {
		return row==null? null: row.getManager();
	}

	public EmployeeTableModel() {

	}

	@Override
	public void setRows(List<Employee> rows) {
		List<Column<Employee,?>> columns = new ArrayList<>();
		columns.add(COLUMN_ROWNO);
		columns.add(COLUMN_ACTIVE);
		columns.add(COLUMN_MANAGER);
		columns.add(COLUMN_NAME);
		columns.add(COLUMN_ROLES);
		if(SpiritProperties.getInstance().isChecked(PropertyKey.TAB_RESULT)) {
			columns.add(COLUMN_GROUPS);
		}

		setColumns(columns);
		super.setRows(rows);
	}

}
