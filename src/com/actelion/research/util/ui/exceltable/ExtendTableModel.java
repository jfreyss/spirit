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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

public class ExtendTableModel<ROW> extends AbstractTableModel {


	public static final Color COLOR_NONEDIT = new Color(230, 230, 230);

	//Tree Info
	private boolean canExpand = true;
	private Column<ROW, ?> treeColumn;
	private boolean treeViewActive = true;
	private boolean treeViewEnabled = false;

	//Columns
	//Columns may be set as hidden. Those "hidden" columns are not shown initially, but may be added later by configuring the display
	// - allColumns contains all possible columns
	// - displayed contains the columns, which are visibles (not set to hidden and not in the notHidden set)
	protected List<Column<ROW, ?>> allColumns = new ArrayList<>();
	protected List<Column<ROW, ?>> displayed = new ArrayList<>();
	protected Set<Column<ROW, ?>> notHidden = new HashSet<>();

	//Rows
	protected List<ROW> rows = new ArrayList<>();

	private ExcelUndoManager undoManager = null;
	private Set<Column<ROW, ?>> readOnlyColumns = new HashSet<>();
	private Set<ROW> readOnlyRows = new HashSet<ROW>();

	private int maxRowsToExplore = 200;
	private boolean isEditable = false;

	/**
	 * Node is used internally to memorize how each ROW should be formatted (if treeColumn!=null)
	 * @author freyssj
	 *
	 */
	static class Node implements Comparable<Node> {
		int index;
		Object row;
		String pattern;
		boolean expanded;
		Boolean leaf = null; //null means unknown
		Set<Node> children = new TreeSet<>();

		public Node(int index, Object row) {
			this.index = index;
			this.row = row;
		}
		@Override
		public int compareTo(Node o) {
			return index - o.index;
		}
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof ExtendTableModel.Node)) return false;
			return this.compareTo((ExtendTableModel.Node)obj)==0;
		}
		@Override
		public String toString() {
			return "["+index+"-"+row+"]";
		}
	}
	private Map<ROW, Node> row2node = new HashMap<>();


	public Column<ROW, Integer> COLUMN_ROWNO = new Column<ROW, Integer>("#",  Integer.class, 18) {
		private JLabelNoRepaint lbl = new JLabelNoRepaint();
		@Override
		public Integer getValue(ROW row, int rowNo) {
			return rowNo+1;
		}
		@Override
		public Integer getValue(ROW row) {return null;}
		@Override
		public boolean isEditable(ROW row) {return false;}
		@Override
		public boolean shouldMerge(ROW r1, ROW r2) {return false;}
		@Override
		public JComponent getCellComponent(AbstractExtendTable<ROW> table, ROW row, int rowNo, Object value) {
			lbl.setHorizontalAlignment(SwingConstants.RIGHT);
			lbl.setText(""+(rowNo+1));
			return lbl;
		}

		@Override
		public void postProcess(AbstractExtendTable<ROW> table, ROW row, int rowNo, Object value, JComponent comp) {
			comp.setBackground(COLOR_NONEDIT);
		}
		@Override
		public float getSortingKey() {return -1f;}
	};

	public ExtendTableModel() {
	}
	public ExtendTableModel(Column<ROW, ?>[] allColumns) {
		this();
		setColumns(Arrays.asList(allColumns));
	}
	public ExtendTableModel(List<Column<ROW, ?>> allColumns) {
		this();
		setColumns(allColumns);
	}

	private void initDisplayed() {
		displayed.clear();
		for (Column<ROW, ?> column : allColumns) {
			if(isEditable() || !column.isHideable() || notHidden.contains(column)) {
				displayed.add(column);
			}
		}
	}

	public Column<ROW, ?> getColumn(int index) {
		return index<0 || index>displayed.size()? null: displayed.get(index);
	}


	@Override
	public int getColumnCount() {
		return displayed.size();
	}

	@Override
	public int getRowCount() {
		return rows.size();
	}

	public void setMaxRowsToExplore(int maxRowsToExplore) {
		this.maxRowsToExplore = maxRowsToExplore;
	}

	public int getMaxRowsToExplore() {
		return maxRowsToExplore;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if(columnIndex>=displayed.size()) return null;
		if(rowIndex>=rows.size()) return null;
		return displayed.get(columnIndex).getValue(rows.get(rowIndex), rowIndex);
	}

	@Override
	public String getColumnName(int column) {
		return displayed.get(column).getName();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if(columnIndex<displayed.size()) {
			return displayed.get(columnIndex).getColumnClass();
		}
		return null;
	}

	public List<ROW> getRows() {
		return rows;
	}

	public ROW getRow(int row) {
		return row<0 || row>=rows.size()? null: rows.get(row);
	}

	public void setColumns(List<Column<ROW, ?>> allColumns) {
		this.allColumns = new ArrayList<>(allColumns);
		if(isEditable) {
			notHidden.addAll(allColumns);
		}

		initDisplayed();
		fireTableStructureChanged();
	}

	public void setRows(List<ROW> rows) {
		if(rows==null) {
			this.rows = new ArrayList<>();
		} else {
			this.rows = rows;
		}
		fireTableDataChanged();
	}

	public void clear() {
		if(rows.size()>0) {
			rows.clear();
			fireTableDataChanged();
		}
	}

	public void add(ROW row) {
		addAll(Collections.singletonList(row));
	}

	public void addAll(Collection<ROW> newRows) {
		if(newRows!=null && newRows.size()>0) {
			for (ROW row : newRows) {
				rows.add(row);
			}
			fireTableDataChanged();
		}
	}

	public void addColumn(Column<ROW, ?> column) {
		List<Column<ROW, ?>> l = new ArrayList<Column<ROW,?>>();
		l.add(column);
		addColumns(l, false);
	}

	public void addColumns(Collection<Column<ROW, ?>> columns, boolean sort) {
		boolean modified = false;
		for (Column<ROW, ?> column : columns) {
			if(!this.allColumns.contains(column)) {
				this.allColumns.add(column);
				modified = true;
			}
		}
		if(modified) {
			if(sort) sortColumns();
			initDisplayed();
			fireTableStructureChanged();
		}
	}

	public void sortColumns() {
		sortColumns(this.allColumns);
	}

	public void sortColumns(List<Column<ROW, ?>> columns) {
		Collections.sort(columns);
	}

	public void removeColumns(Collection<Column<ROW, ?>> columns) {
		boolean modified = false;
		for (Column<ROW, ?> column : columns) {
			allColumns.remove(column);
			modified=true;
		}
		if(modified) {
			initDisplayed();
			fireTableStructureChanged();
		}
	}

	public void removeColumn(Column<ROW, ?> column) {
		List<Column<ROW, ?>> l = new ArrayList<>();
		l.add(column);
		removeColumns(l);
	}

	/**
	 * Remove Empty columns (ie. where all values are null or empty)
	 * - fire structurechange event
	 * - does not reset width
	 */
	public void removeEmptyColumns() {
		setColumns(removeEmptyColumns(getAllColumns()));
	}

	/**
	 * Remove Empty(value=null) columns from the given list except those in exceptions
	 * don't fire event
	 * only maxRowsToExplore are checked
	 */
	public List<Column<ROW, ?>> removeEmptyColumns(List<Column<ROW, ?>> columns) {
		List<Integer> rows = AbstractExtendTable.selectIndixes(getRowCount(), getMaxRowsToExplore());
		List<Column<ROW, ?>> res = new ArrayList<>();
		for (int i = 0; i < columns.size(); i++) {
			Column<ROW, ?> col = columns.get(i);
			if(col.isHideable()) {
				res.add(col);
			} else {
				exploreRows: for(int row: rows) {
					Object obj = col.getValue(getRow(row), row);
					if(obj!=null && obj.toString().length()>0) {
						res.add(col);
						break exploreRows;
					}
				}
			}
		}
		return res;
	}

	/**
	 * Returns currently displayed columns
	 * @return
	 */
	public List<Column<ROW, ?>> getColumns() {
		return Collections.unmodifiableList(displayed);
	}

	public List<Column<ROW, ?>> getAllColumns() {
		return allColumns;
	}


	/**
	 * To be extended
	 * @return null if right click is not supported (default)
	 */
	public List<Column<ROW, ?>> getPossibleColumns() {
		return null;
	}

	/**
	 * To be overriden if a column has to behave like a tree.
	 * In that case, the children always need to be immediately after their parent
	 * @return
	 */
	public Column<ROW, ?> getTreeColumn() {
		return treeColumn;
	}

	public void setTreeColumn(Column<ROW, ?> treeColumn) {
		this.treeColumn = treeColumn;
	}

	/**
	 * To be overridden if getTreeChildren does not return null.
	 * Returns the list of children for the given node. The list does not need to be sorted,
	 * it is sorted afterwards based on the current sort or the object sort
	 * @param row
	 * @return
	 */
	public Collection<ROW> getTreeChildren(ROW row) {
		throw new IllegalArgumentException("getTreeChildren must be implemented if treeColumn!=null");
	}

	/**
	 * Add the children of obj into toPopulate (does not add obj)
	 * @param obj
	 * @param toPopulate
	 * @return
	 */
	protected void populateTreeChildrenRec(ROW obj, List<ROW> toPopulate) {
		Collection<ROW> children = getTreeChildren(obj);
		for (ROW c : children) {
			toPopulate.add(c);
			populateTreeChildrenRec(c, toPopulate);
		}
	}

	public ROW getTreeParent(ROW row) {
		throw new IllegalArgumentException("getTreeParent should be implemented if treeColumn!=null");
	}


	protected void reorderTree() {
		//		if(push>0) return;
		if(isTreeViewActive()) {
			//			push++;

			//			try {
			//Create tree
			Set<Node> roots = computeTreeRoots();

			//Recreate rows and treeinfo from the tree hierarchy
			int index = 0;
			for(Node root: roots) {
				index = createRows(index, "", root);
			}

			//			} finally {
			//				push--;
			//			}
		}
	}


	private Set<Node> computeTreeRoots() {
		//		Thread.dumpStack();
		TreeSet<Node> res = new TreeSet<>();
		row2node.clear();
		for (int i = 0; i < rows.size(); i++) {
			Node node = new Node(i, rows.get(i));
			row2node.put(rows.get(i), node);
			res.add(node);
		}

		//For each object, recreate the visible Node hierarchy.
		//There are 2 ways of doing so. Either we use the parent, or the children.
		//Creating the hierarchy from the parent is always the fastest option
		boolean useParent;
		try {
			getTreeParent(null);
			useParent = true;
		} catch(Exception e) {
			useParent = false;
		}

		if(useParent) {
			for (ROW child : rows) {
				Node childNode = row2node.get(child);
				assert childNode!=null;

				ROW parent = getTreeParent(child);
				Node parentNode = row2node.get(parent);
				if(parent!=null && parentNode!=null) {
					res.remove(childNode);
					parentNode.children.add(childNode);
					parentNode.leaf = false;
					parentNode.expanded = true;
				}
			}

		} else {
			for (ROW parent : rows) {
				Node parentNode = row2node.get(parent);
				assert parentNode!=null;

				int nChildren = 0;
				int nPresent = 0;
				for (ROW child : getTreeChildren(parent)) {
					nChildren++;

					Node childNode = row2node.get(child);
					if(childNode!=null) {
						res.remove(childNode);
						parentNode.children.add(childNode);
						nPresent++;
					}
				}

				parentNode.leaf = nChildren==0;
				parentNode.expanded = nPresent>0;// && nPresent==nChildren;
			}
		}

		return res;
	}



	@SuppressWarnings("unchecked")
	private int createRows(int index, String pattern, Node root) {
		if(index<rows.size()) {
			rows.set(index, (ROW) root.row);
		} else {
			rows.add((ROW) root.row);
		}
		index++;
		root.pattern = pattern;
		for (Iterator<Node> iterator = root.children.iterator(); iterator.hasNext();) {
			Node n = iterator.next();

			String newPattern =  pattern.replace('3', '1').replace('2', '0') + (iterator.hasNext()?'3': '2');
			index = createRows(index,  newPattern, n);
		}

		return index;
	}

	protected boolean isTreeViewActive() {
		return treeViewEnabled && treeViewActive;
	}


	public void setTreeViewActive(boolean activeTreeView) {
		if(this.treeViewActive == activeTreeView) return;
		this.treeViewActive = activeTreeView;
		fireTableDataChanged();
	}

	public void setCanExpand(boolean canExpand) {
		this.canExpand = canExpand;
	}
	public boolean isCanExpand() {
		return canExpand;
	}

	/**
	 * Do we reserve space for the hierarchy
	 * @return
	 */
	public boolean isTreeViewEnabled() {
		return treeViewEnabled;
	}

	/**
	 * Should be set automatically on tableChange.
	 * When disabled, there is no need to compute children or reserve space for the hierarchy
	 * @param treeViewEnabled
	 */
	protected void setTreeViewEnabled(boolean treeViewEnabled) {
		this.treeViewEnabled = treeViewEnabled;
	}

	public void showHideable(Column<ROW, ?> column, boolean display) {
		System.out.println("ExtendTableModel.showHideable() "+column+" "+display);
		//Extract the column of the model (if the argument is not exactly equal)
		for (Column<ROW, ?> column2 : allColumns) {
			if(column2.equals(column)) {
				System.err.println("FOUND "+column);
				column = column2;
				break;
			}
		}

		//Show or hide the column
		assert column.isHideable();
		if(display && !notHidden.contains(column)) {
			notHidden.add(column);
			initDisplayed();
			fireTableStructureChanged();
		} else if(!display && notHidden.contains(column)) {
			notHidden.remove(column);
			initDisplayed();
			fireTableStructureChanged();
		}
	}

	public void showAllHideable(boolean val) {
		boolean modified = false;
		if(val) {
			for (Column<ROW, ?> column : getAllColumns()) {
				if(column.isHideable() && !notHidden.contains(column)) {
					notHidden.add(column);
					modified = true;
				}
			}
		} else {
			for (Column<ROW, ?> column : getAllColumns()) {
				if(column.isHideable() && notHidden.contains(column)) {
					notHidden.remove(column);
					modified = true;
				}
			}
		}
		if(modified) {
			initDisplayed();
			fireTableStructureChanged();
		}
	}

	Node getNode(ROW row) {
		return row2node.get(row);
	}

	public void setEditable(boolean isEditable) {
		this.isEditable = isEditable;
		initDisplayed();
		fireTableStructureChanged();
	}

	public boolean isEditable() {
		return isEditable;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		try {
			if(!isEditable) return false;
			ROW row = getRow(rowIndex);
			Column<ROW, ?> colObject = getColumn(columnIndex);
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
		if((oldValue==null || oldValue.toString()==null || oldValue.toString().length()==0) && (newValue==null || newValue.toString()==null || newValue.toString().length()==0) ) return;
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
		if(!isEditable()) return;
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
