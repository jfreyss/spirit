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

package com.actelion.research.spiritapp.ui.audit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.util.ui.exceltable.ExtendTable;

public class RevisionTable extends ExtendTable<Revision> {

	private Map<Revision, String> changeMap = new HashMap<>();

	public RevisionTable(boolean addWhatColumn) {
		super(new RevisionTableModel(addWhatColumn));
		setBorderStrategy(BorderStrategy.WHEN_DIFFERENT_VALUE);
	}

	@Override
	public RevisionTableModel getModel() {
		return (RevisionTableModel) super.getModel();
	}

	public void setChangeMap(Map<Revision, String> changeMap) {
		this.changeMap = changeMap;
	}

	public Map<Revision, String> getChangeMap() {
		return changeMap;
	}

	@Override
	public void setRows(List<Revision> data) {
		super.setRows(data);
	}

	public void setRows(List<Revision> data, Map<Revision, String> changeMap) {
		getModel().setRows(data);
		getModel().setChangeMap(changeMap);
		resetPreferredColumnWidth();
	}
}
