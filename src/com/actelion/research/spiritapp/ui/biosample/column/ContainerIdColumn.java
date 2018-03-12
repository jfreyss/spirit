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

package com.actelion.research.spiritapp.ui.biosample.column;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.biosample.editor.ContainerIdCellEditor;
import com.actelion.research.spiritapp.ui.util.component.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class ContainerIdColumn extends Column<Biosample, String> {

	public ContainerIdColumn() {
		super("Container\nContainerId", String.class, 80, 120);
	}

	@Override
	public float getSortingKey() {
		return 2.2f;
	}

	@Override
	public String getValue(Biosample row) {
		return row.getContainerId()==null?"": row.getContainerId();
	}
	@Override
	public void setValue(Biosample row, String value) {
		if(row.getBiotype()==null || row.isAbstract()) return;
		if(value==null) {
			row.setContainerId(null);
		} else {
			row.setContainerId(value);
		}
	}
	@Override
	public boolean isEditable(Biosample row) {
		if(row==null) return true;
		if(row.getBiotype()==null || row.isAbstract()) return false;
		return true;

	}

	@Override
	public void paste(Biosample row, String value) throws Exception {
		if(row.getBiotype()==null || row.isAbstract()) throw new Exception("The containertype must be set and must have a 2d barcode");
		setValue(row, value);
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		return new ContainerIdCellEditor();
	}

	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		comp.setBackground(LF.BGCOLOR_LOCATION);
	}

}