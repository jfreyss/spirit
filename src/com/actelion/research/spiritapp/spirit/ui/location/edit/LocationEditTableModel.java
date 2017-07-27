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

package com.actelion.research.spiritapp.spirit.ui.location.edit;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.location.column.LocationColsColumn;
import com.actelion.research.spiritapp.spirit.ui.location.column.LocationDescriptionColumn;
import com.actelion.research.spiritapp.spirit.ui.location.column.LocationLabelingColumn;
import com.actelion.research.spiritapp.spirit.ui.location.column.LocationNameColumn;
import com.actelion.research.spiritapp.spirit.ui.location.column.LocationParentColumn;
import com.actelion.research.spiritapp.spirit.ui.location.column.LocationPrivacyColumn;
import com.actelion.research.spiritapp.spirit.ui.location.column.LocationRowsColumn;
import com.actelion.research.spiritapp.spirit.ui.location.column.LocationTypeColumn;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserAdministrationMode;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;

public class LocationEditTableModel extends ExtendTableModel<Location> {
	public LocationEditTableModel() {
		LocationNameColumn nameColumn = new LocationNameColumn();
		nameColumn.setDiplayIcon(false);
		nameColumn.setBold(true);

		List<Column<Location, ?>> columns = new ArrayList<>();
		columns.add(new LocationTypeColumn());
		columns.add(new LocationParentColumn());
		columns.add(nameColumn);
		columns.add(new LocationDescriptionColumn());
		columns.add(new LocationLabelingColumn());
		columns.add(new LocationRowsColumn());
		columns.add(new LocationColsColumn());
		if(DBAdapter.getAdapter().getUserManagedMode()!=UserAdministrationMode.UNIQUE_USER) {
			columns.add(new LocationPrivacyColumn());
		}

		setTreeColumn(nameColumn);
		setColumns(columns);
	}



	@Override
	public List<Location> getTreeChildren(Location row) {
		try {
			List<Location> res = new ArrayList<>();
			for (Location loc: row.getChildren()) {
				if(!SpiritRights.canRead(loc, SpiritFrame.getUser())) continue;
				res.add(loc);
			}
			return res;
		} catch(Exception e) {
			e.printStackTrace();
			return new ArrayList<Location>();
		}
	}

	@Override
	public Location createRecord() {
		return new Location();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		Location loc = getRows().get(rowIndex);
		if(!SpiritRights.canEdit(loc, SpiritFrame.getUser())) return false;
		return super.isCellEditable(rowIndex, columnIndex);
	}
}
