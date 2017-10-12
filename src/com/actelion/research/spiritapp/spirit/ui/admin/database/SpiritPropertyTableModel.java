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

package com.actelion.research.spiritapp.spirit.ui.admin.database;

import com.actelion.research.spiritcore.business.property.SpiritProperty;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;
import com.actelion.research.util.ui.exceltable.StringColumn;

public class SpiritPropertyTableModel extends ExtendTableModel<SpiritProperty> {

	public StringColumn<SpiritProperty> keyColumn = new StringColumn<SpiritProperty>("Key") {
		@Override
		public String getValue(SpiritProperty row) {
			return row.getKey();
		}
	};
	public StringColumn<SpiritProperty> valueColumn = new StringColumn<SpiritProperty>("Value") {
		@Override
		public String getValue(SpiritProperty row) {
			return row.getValue();
		}
	};

	@SuppressWarnings("unchecked")
	public SpiritPropertyTableModel() {
		setColumns(MiscUtils.listOf(keyColumn, valueColumn));
	}

}
