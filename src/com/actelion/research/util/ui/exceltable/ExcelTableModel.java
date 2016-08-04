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

package com.actelion.research.util.ui.exceltable;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ExcelTableModel<ROW> extends ExtendTableModel<ROW> {
	
	private ExcelUndoManager undoManager = null;
	private Set<Column<ROW, ?>> readOnlyColumns = new HashSet<Column<ROW, ?>>();
	private Set<ROW> readOnlyRows = new HashSet<ROW>();
	
	
	public ExcelTableModel() {
		this(new ArrayList<Column<ROW, ?>>());
	}
	
	public ExcelTableModel(List<Column<ROW, ?>> columns) {
		super(columns);
	}
	@Override
	public void clear() {
		rows.clear();
		setColumns(new ArrayList<Column<ROW, ?>>());
	}
		
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		try {
			ROW row = getRow(rowIndex);
			Column<ROW, ?> colObject = (Column<ROW, ?>) getColumn(columnIndex);
			if(getReadOnlyColumns().contains(colObject)) return false;
			if(getReadOnlyRows().contains(row)) return false;
		
			return getColumn(columnIndex).isEditable(rows.get(rowIndex));
		} catch (Exception e) {
			return false;			
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
		Column<ROW, Object> colObject = (Column<ROW, Object>)getColumn(columnIndex);
		if(rowIndex>=rows.size() || rowIndex<0 || !colObject.isEditable(rows.get(rowIndex))) return;
		
		Object oldValue = getValueAt(rowIndex, columnIndex);
		if((oldValue==null || oldValue.toString().length()==0) && (newValue==null || newValue.toString().length()==0) ) return;
		if(oldValue!=null && oldValue.equals(newValue)) return;

		try {			
			colObject.setValue(rows.get(rowIndex), newValue);
			if(undoManager!=null) {
				undoManager.addEdit(undoManager.new OneChangeEdit(rowIndex, columnIndex, oldValue, newValue));
			}
		} catch (Exception e) {
			System.err.println("ExcelTableModel: Invalid value: "+e+" "+(newValue!=null?"("+newValue+") in "+newValue.getClass():"null")+" expected "+colObject.getColumnClass());
			e.printStackTrace();
		}
		fireTableRowsUpdated(rowIndex, rowIndex);
	}	
	
	public void setUndoManager(ExcelUndoManager undoManager) {
		this.undoManager = undoManager;
	}

	/**
	 * Called when pasting Data
	 * @param valueString
	 * @param rowIndex
	 * @param modelColumnIndex
	 * @throws Exception
	 */
	public void paste(String valueString, int rowIndex, int modelColumnIndex) throws Exception {
		Column<ROW, ?> colObject = getColumn(modelColumnIndex);
		ROW row = rows.get(rowIndex);

		if(colObject==null || !colObject.isEditable(row)) return;
		Object oldValue = getValueAt( rowIndex, modelColumnIndex );			
		colObject.paste(row, valueString);
		if(undoManager!=null) {
			undoManager.addEdit(undoManager.new OneChangeEdit(rowIndex, modelColumnIndex, oldValue, getValueAt( rowIndex, modelColumnIndex )));
		}
	}	
	
	public Set<Column<ROW, ?>> getReadOnlyColumns() {
		return readOnlyColumns;
	}
	
	public Set<ROW> getReadOnlyRows() {
		return readOnlyRows;
	}
	
	public void setReadOnlyRow(ROW row, boolean v) {
		if(v) readOnlyRows.add(row);
		else readOnlyRows.remove(row);
	}

	
	/**
	 * Should be overridden to allow insertion of new rows.
	 * If not overridden, adding/deleting of rows is not possible
	 */
	public ROW createRecord() {
		return null;
	}
	

}
