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

package com.actelion.research.spiritapp.ui.result;

import java.util.List;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.ui.util.component.SpiritExtendTable;
import com.actelion.research.spiritcore.business.result.Result;

public class ResultTable extends SpiritExtendTable<Result> implements ListSelectionListener {
	
	public ResultTable() {
		this(false);
	}
	public ResultTable(boolean compact) {
		super(new ResultTableModel(compact));
		init();
	}
	
		
	/**
	 * Initialization
	 */
	private void init() {
		setColumnSelectionAllowed(true);
		setRowSelectionAllowed(true);
		setCellSelectionEnabled(true);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}
	
	@Override
	public ResultTableModel getModel() {
		return (ResultTableModel) super.getModel();
	}

	@Override
	public void setRows(final List<Result> data) {
		
		//Set Rows
		getModel().setRows(data);		
		
		//Init Model
		getModel().initColumns();
		
		//Remove empty columns
		getModel().removeEmptyColumns();
		
		//Reset Columns width
		resetPreferredColumnWidth();
		
	}
	
}
