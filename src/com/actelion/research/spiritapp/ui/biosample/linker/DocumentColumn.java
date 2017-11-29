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

package com.actelion.research.spiritapp.ui.biosample.linker;

import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.ui.biosample.editor.DocumentCellEditor;
import com.actelion.research.spiritapp.ui.biosample.editor.DocumentZipCellEditor;
import com.actelion.research.spiritapp.ui.util.component.DocumentLabel;
import com.actelion.research.spiritapp.ui.util.component.DocumentTextField;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;

public class DocumentColumn extends AbstractLinkerColumn<Document> {
	
	private static DocumentLabel documentLabel = new DocumentLabel();
	
	protected DocumentColumn(final BiosampleLinker linker) {
		super(linker, Document.class, 70);
	}
	@Override
	public Document getValue(Biosample row) {
		row = linker.getLinked(row);
		if(row==null  || row.getMetadataDocument(getType())==null) return null;
		return row.getMetadataDocument(getType());
	}
	@Override
	public void setValue(Biosample row, Document doc) {
		row.setMetadataDocument(getType(), doc);
	}
	
	@Override
	public void paste(Biosample row, String value) throws Exception {
		if(value==null) setValue(row, null); 
		else throw new Exception("You cannot paste documents");
	}
	
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		documentLabel.setSelectedDocument((Document)value);
		return documentLabel;
	}
	
	@Override
	public boolean isEditable(Biosample row) {
		return true;
	}
	
	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {
		if(getType().getDataType()==DataType.FILES) {			
			return new DocumentZipCellEditor();
		} else {
			return new DocumentCellEditor();
		}
	}
	
	@Override
	public boolean mouseDoubleClicked(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value) {
		if(getType().getDataType()==DataType.FILES) {			
			return false;
		} else {
			DocumentTextField.open((Document) value);
			return true;
		}
	}
}