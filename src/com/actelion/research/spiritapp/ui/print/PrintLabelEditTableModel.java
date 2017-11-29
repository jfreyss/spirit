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

package com.actelion.research.spiritapp.ui.print;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.spiritapp.print.PrintLabel;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;

public class PrintLabelEditTableModel extends ExtendTableModel<PrintLabel> {

	public static final Column<PrintLabel,String> COLUMN_BARCODE = new Column<PrintLabel, String>("Barcode", String.class) {
		@Override
		public String getValue(PrintLabel row) {
			return row.getBarcodeId();
		}
		@Override
		public void setValue(PrintLabel row, String value) {
			row.setBarcodeId(value);
		}
		@Override
		public boolean isEditable(PrintLabel row) {
			return true;
		}
	};

	public static final Column<PrintLabel,String> COLUMN_LABEL = new Column<PrintLabel, String>("Label", String.class) {
		@Override
		public String getValue(PrintLabel row) {
			return row.getLabel();
		}
		@Override
		public boolean isMultiline() {
			return true;
		}
		@Override
		public void setValue(PrintLabel row, String value) {
			row.setLabel(value);
		}
		@Override
		public boolean isEditable(PrintLabel row) {
			return true;
		}
	};

	public PrintLabelEditTableModel() {
		List<Column<PrintLabel,?>> columns = new ArrayList<Column<PrintLabel,?>>();
		columns.add(COLUMN_ROWNO);
		columns.add(COLUMN_BARCODE);
		columns.add(COLUMN_LABEL);
		setColumns(columns);
	}

	@Override
	public PrintLabel createRecord() {
		return new PrintLabel();
	}

}
