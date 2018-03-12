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

package com.actelion.research.util.ui.exceltable;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

@SuppressWarnings("rawtypes")
public class ExcelUndoManager extends UndoManager {


	protected class OneChangeEdit extends AbstractUndoableEdit {
		private int columnIndex;
		private int rowIndex;
		private Object oldValue;
		private Object newValue;

		public OneChangeEdit(int rowIndex, int columnIndex, Object oldValue, Object newValue) {
			this.columnIndex = columnIndex;
			this.rowIndex = rowIndex;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		@Override
		public void undo() {
			super.undo();
			table.setValueAt(oldValue, rowIndex, columnIndex);
			table.getModel().fireTableCellUpdated(rowIndex, columnIndex);
			table.setRowSelectionInterval(rowIndex, rowIndex);
			table.setColumnSelectionInterval(columnIndex, columnIndex);
		}

		@Override
		public void redo() {
			super.redo();
			table.setValueAt(newValue, rowIndex, columnIndex);
			table.getModel().fireTableCellUpdated(rowIndex, columnIndex);
			table.setRowSelectionInterval(rowIndex, rowIndex);
			table.setColumnSelectionInterval(columnIndex, columnIndex);
		}
		@Override
		public String toString() {
			return "Edit:"+rowIndex+"x"+columnIndex+" - " + oldValue +" -> "+ newValue;
		}
	}

	protected class TransactionEdit extends AbstractUndoableEdit {
		List<OneChangeEdit> changes = new ArrayList<OneChangeEdit>();
		boolean closed;
		public TransactionEdit() {

		}

		@Override
		public void undo() {
			super.undo();
			for (int i = changes.size()-1; i>=0; i--) {
				changes.get(i).undo();
			}
		}

		@Override
		public void redo() {
			super.redo();
			for (int i = 0; i<changes.size(); i++) {
				changes.get(i).redo();
			}
		}
		@Override
		public String toString() {
			return "TransactionEdit closed="+closed;
		}
	}

	protected class RowEdit<ROW> extends AbstractUndoableEdit {
		private int row;
		private ROW oldRow;
		private ROW newRow;

		public RowEdit(int row, ROW oldRow, ROW newRow) {
			if(row<0) throw new IllegalArgumentException("Invalid row "+row);
			this.row = row;
			this.oldRow = oldRow;
			this.newRow = newRow;
		}

		@Override
		public void undo() {
			super.undo();
			ExtendTableModel<ROW> model = table.getModel();
			model.getRows().set(row, oldRow);
			table.getModel().reorderTree();
			table.setRowSelectionInterval(row, row);
			table.setColumnSelectionInterval(0, table.getColumnCount()-1);
		}

		@Override
		public void redo() {
			super.redo();
			ExtendTableModel<ROW> model = table.getModel();
			model.getRows().set(row, newRow);
			table.getModel().reorderTree();
			table.setRowSelectionInterval(row, row);
			table.setColumnSelectionInterval(0, table.getColumnCount()-1);
		}
		@Override
		public String toString() {
			return "RowEdit "+row+" "+oldRow+">"+newRow;
		}
	}


	private ExcelTable table;
	private int push = 0;
	private boolean transaction = false;
	private boolean hasChanges = false;


	public ExcelUndoManager(ExcelTable table) {
		this.table = table;
	}


	@Override
	public void undo() {
		table.editingStopped(new ChangeEvent(table));
		if(canUndo()) {
			if(push>0) return;
			push++;
			try {
				super.undo();
			} finally {
				push--;
			}
		}
	}
	@Override
	public void redo() {
		table.editingStopped(new ChangeEvent(table));
		if(canRedo()) {
			if(push>0) return;
			push++;
			try {
				super.redo();
			} finally {
				push--;
			}
		}
	}

	public boolean isPushed() {
		return push>0;
	}

	@Override
	public synchronized boolean addEdit(UndoableEdit anEdit) {
		if(push>0) return false;

		if(anEdit instanceof OneChangeEdit) {
			OneChangeEdit oneEdit = (OneChangeEdit) anEdit;
			if((oneEdit.oldValue==null && oneEdit.oldValue==oneEdit.newValue) ||
					(oneEdit.oldValue!=null && oneEdit.newValue!=null &&
					oneEdit.oldValue.toString().equals(oneEdit.newValue.toString()))) return false;
		}

		hasChanges = true;
		if(anEdit instanceof OneChangeEdit) {
			OneChangeEdit oneEdit = (OneChangeEdit) anEdit;


			if(transaction) {
				if((lastEdit() instanceof TransactionEdit) && !((TransactionEdit) lastEdit()).closed) {
					TransactionEdit last = (TransactionEdit) lastEdit();
					last.changes.add(oneEdit);
					replaceEdit(last);
				} else {
					TransactionEdit last = new TransactionEdit();
					last.changes.add(oneEdit);
					last.closed = false;
					super.addEdit(last);
				}
				return true;
			} else {
				OneChangeEdit last = (lastEdit() instanceof OneChangeEdit)? (OneChangeEdit) lastEdit(): null;
				if(last!=null && last.columnIndex==oneEdit.columnIndex && last.rowIndex==oneEdit.rowIndex) {
					oneEdit.oldValue = last.oldValue;
					if(((oneEdit.oldValue==null || oneEdit.oldValue.equals("")) && (oneEdit.newValue==null || oneEdit.newValue.equals(""))) || (oneEdit.oldValue!=null && oneEdit.oldValue.equals(oneEdit.newValue))) return false;
					replaceEdit(oneEdit);
				} else {
					if(((oneEdit.oldValue==null || oneEdit.oldValue.equals("")) && (oneEdit.newValue==null || oneEdit.newValue.equals(""))) || (oneEdit.oldValue!=null && oneEdit.oldValue.equals(oneEdit.newValue))) return false;
					super.addEdit(oneEdit);
				}
				return true;
			}
		} else {
			super.addEdit(anEdit);
			return true;
		}
	}


	public void setTransaction(boolean transaction) {
		//		System.out.println("ExcelUndoManager.setTransaction() = "+transaction);
		this.transaction = transaction;
		if(!transaction && lastEdit() instanceof TransactionEdit) {
			TransactionEdit last = (TransactionEdit) lastEdit();
			last.closed = true;
			//			System.out.println("ExcelUndoManager.setTransaction() Closed " );
		}
	}

	/**
	 * BUG IN SUPERCLASS (in latest java version). This function is to fix it
	 */
	@Override
	public boolean replaceEdit(UndoableEdit anEdit) {
		if(edits.size()>0) edits.remove(edits.size()-1);
		return super.addEdit(anEdit);
	}

	/**
	 * Returns true if some changes have been made
	 * @return
	 */
	public boolean hasChanges() {
		return hasChanges;
	}

	public<T> void addOfflineRowChange(int row, T oldRow, T newRow) {
		addEdit(new RowEdit<T>(row, oldRow, newRow));
	}

}
