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

package com.actelion.research.spiritapp.ui.result.column;


import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.biosample.editor.QualityCellEditor;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class QualityColumn extends Column<Result, Quality> {
	public QualityColumn() {
		super("Quality", Quality.class);
	}

	@Override
	public float getSortingKey() {
		return 9f;
	}

	@Override
	public Quality getValue(Result row) {
		return row.getQuality();
	}

	@Override
	public void setValue(Result row, Quality value) {
		row.setQuality(value);
	}

	@Override
	public void paste(Result row, String value) throws Exception {
		for(Quality q: Quality.values()) {
			if(q.getName().equalsIgnoreCase(value)) {
				setValue(row, q);
				return;
			}
		}
		throw new Exception(value+" is not a valid quality");
	}

	@Override
	public boolean isEditable(Result row) {return true;}

	private JLabelNoRepaint lbl = new JLabelNoRepaint();

	@Override
	public JComponent getCellComponent(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
		lbl.setText(value==null?null: ((Quality)value).getName());
		return lbl;
	}
	@Override
	public void postProcess(AbstractExtendTable<Result> table, Result row, int rowNo, Object value, JComponent comp) {
		comp.setBackground(value==null?null:((Quality)value).getBackground());
	}

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Result> table) {
		return new QualityCellEditor();
	}


	@Override
	public boolean isHideable() {
		return true;
	}

}
