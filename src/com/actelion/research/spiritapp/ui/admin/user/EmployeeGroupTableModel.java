/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.ui.admin.user;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.spiritapp.ui.util.component.SpiritExtendTableModel;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.util.ui.exceltable.Column;

public class EmployeeGroupTableModel extends SpiritExtendTableModel<EmployeeGroup> {

	public static final Column<EmployeeGroup, String> COLUMN_NAME = new Column<EmployeeGroup, String>("GroupName", String.class, 100) {
		@Override
		public String getValue(EmployeeGroup row) {
			return row.getName();
		}
	};
	public static final Column<EmployeeGroup, String> COLUMN_ACTIVE = new Column<EmployeeGroup, String>("Status", String.class, 40) {
		@Override
		public String getValue(EmployeeGroup row) {
			return row.isDisabled()?"Disabled": null;
		}
	};

	@Override
	public Column<EmployeeGroup,?> getTreeColumn() {
		return COLUMN_NAME;
	}

	@Override
	public List<EmployeeGroup> getTreeChildren(EmployeeGroup row) {
		return row==null? new ArrayList<EmployeeGroup>(): new ArrayList<EmployeeGroup>(row.getChildren());
	}

	@Override
	public EmployeeGroup getTreeParent(EmployeeGroup row) {
		return row==null? null: row.getParent();
	}


	public EmployeeGroupTableModel() {
		List<Column<EmployeeGroup,?>> columns = new ArrayList<>();
		columns.add(COLUMN_ROWNO);
		columns.add(COLUMN_ACTIVE);
		columns.add(COLUMN_NAME);
		setColumns(columns);
	}
	@Override
	public void setRows(List<EmployeeGroup> rows) {
		super.setRows(rows);
		System.out.println("EmployeeGroupTableModel.setRows() "+rows);

		for (EmployeeGroup r : rows) {
			System.out.println("EmployeeGroupTableModel.setRows() "+r+" --> "+r.getChildren()+">"+rows.containsAll(r.getChildren()));
		}
	}

}
