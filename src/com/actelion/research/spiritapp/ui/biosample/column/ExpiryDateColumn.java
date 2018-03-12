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

import java.awt.Color;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.biosample.editor.DateCellEditor;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class ExpiryDateColumn extends Column<Biosample, Date> {

	private Date now = JPAUtil.getCurrentDateFromDatabase();

	public ExpiryDateColumn() {
		super("Sample\nExpiryDate", Date.class, 40, 100);
	}

	@Override
	public float getSortingKey() {return 9.7f;}

	@Override
	public Date getValue(Biosample row) {
		return row.getExpiryDate();
	}

	@Override
	public void setValue(Biosample row, Date date) {
		row.setExpiryDate(date);
	}

	@Override
	public void paste(Biosample row, String value) throws Exception {
		super.paste(row, value);
	}

	@Override
	public boolean isEditable(Biosample row) {return true;}


	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {
		if(row.getExpiryDate()!=null && row.getExpiryDate().before(now)) {
			comp.setForeground(Color.RED);
		} else {
			comp.setForeground(Color.BLACK);
		}
	}


	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		return new DateCellEditor();
	}
}