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

package com.actelion.research.spiritapp.ui.result.column;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.biosample.editor.DocumentCellEditor;
import com.actelion.research.spiritapp.ui.biosample.editor.DocumentZipCellEditor;
import com.actelion.research.spiritapp.ui.util.component.DocumentLabel;
import com.actelion.research.spiritapp.ui.util.component.DocumentTextField;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class DocumentColumn extends Column<Result, Document> {
	
	private TestAttribute att;

	private DocumentLabel documentLabel = new DocumentLabel();

	
	public DocumentColumn(final TestAttribute att) {
		super(att.getName(), Document.class, 70);
		this.att = att;
	}
	
	@Override
	public Document getValue(Result row) {
		if(row==null  || row.getResultValue(att)==null) return null;
		return row.getResultValue(att).getLinkedDocument();
	}
	@Override
	public void setValue(Result row, Document value) {
		row.getResultValue(att).setLinkedDocument(value);
	}
	
	@Override
	public void paste(Result row, String value) throws Exception {
		if(value==null) setValue(row, null); 
		else throw new Exception("You cannot paste documents");
	}
	
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
		documentLabel.setSelectedDocument((Document)value);
		return documentLabel;
	}
	
	@Override
	public boolean isEditable(Result row) {
		return true;
	}
	
	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Result> table) {
		if(att.getDataType()==DataType.FILES) {
			return new DocumentZipCellEditor();			
		} else {
			return new DocumentCellEditor();
		}
	}
	
	@Override
	public boolean mouseDoubleClicked(AbstractExtendTable<Result> table, Result row, int rowNo, Object value) {
		if(att.getDataType()==DataType.FILES) {
			return false;			
		} else {
			DocumentTextField.open((Document) value);
			return true;
		}
	}
		
}