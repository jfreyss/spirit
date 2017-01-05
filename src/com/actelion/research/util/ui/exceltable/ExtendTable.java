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

import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;

import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.EasyClipboard;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.exceltable.ExtendTableModel.Node;

public class ExtendTable<ROW> extends AbstractExtendTable<ROW> {

	private SwingWorkerExtended popupShowWorker;
	
	
	public ExtendTable(List<Column<ROW, ?>> columns) {
		this(new ExtendTableModel<ROW>(columns), HeaderClickingPolicy.SORT);
	}

	public ExtendTable(final ExtendTableModel<ROW> model) {
		this(model, HeaderClickingPolicy.SORT);
	}
	
	public ExtendTable(final ExtendTableModel<ROW> model, final HeaderClickingPolicy headerClickingPolicy) {
		super(model);
		setModel(model);
		setHeaderClickingPolicy(headerClickingPolicy);
		setBorderStrategy(BorderStrategy.WHEN_DIFFERENT_VALUE);
				
		//Set Our custom CellRenderer
		defaultRenderersByColumnClass.clear();
		setDefaultRenderer(Object.class, new ExtendTableCellRenderer<ROW>(this));

		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		resetPreferredColumnWidth();
		setColumnSelectionAllowed(true);
		setCellSelectionEnabled(true);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setAutoscrolls(true);
		
		
		getTableHeader().addMouseListener(new PopupAdapter() {		
			
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (!SwingUtilities.isLeftMouseButton(e))  return;
			
				switch(getHeaderClickingPolicy()) {
				case SORT:
					if (SwingUtilities.isLeftMouseButton(e)) {
						final int col = columnAtPoint(e.getPoint());
						if(col>=0) sortBy(convertColumnIndexToModel(col));
					}
					break;
				case POPUP:
					 {
						 //Trick to:
						 // - open popup on one click (wait 200ms and then open)
						 // - trigger dbl click, without openin popup when dbl clicking faster than 200ms
						 synchronized (this) {
							 if(popupShowWorker!=null) {
								 popupShowWorker.cancel();
							 }
							 popupShowWorker = new SwingWorkerExtended(null, null, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
								@Override
								protected void done() {
									if(e.getClickCount()<=1) showPopup(e);
									popupShowWorker = null;
								}
							};
						 }
					}
						
					break;
				case SELECT:
					final int col = columnAtPoint(e.getPoint());
					if(col<0 || getRowCount()<=0) return;
					
					setRowSelectionInterval(0, getRowCount()-1);
					setColumnSelectionInterval(col, col);
					break;
				case IGNORE:
					//Nothing
				}
			}
			
			/**
			 * Show Popup.
			 * First specific column items
			 * Then table items
			 */
			@Override
			protected void showPopup(MouseEvent e) {
				JPopupMenu popupMenu = new JPopupMenu();
				
				final int col = columnAtPoint(e.getPoint());
				Column<ROW, ?> column = null;
				if(col>=0) {
					column = model.getColumn(convertColumnIndexToModel(col));
					
					popupMenu.add(new JCustomLabel(column.getName().replace("\n", ".").replaceAll("<.>|^\\.|\\.$", ""), Font.BOLD));
					popupMenu.add(new JSeparator());
					
					//Popup: Add sorting Columns
					column.populateHeaderPopup(ExtendTable.this, popupMenu);

				}

				populateHeaderPopup(popupMenu, column);
				
				//Popup: Add possible columns
				List<Column<ROW, ?>> addableColumns = new ArrayList<>();
				for (Column<ROW, ?> c : model.getAllColumns()) {
					if(c.isHideable()) {
						addableColumns.add(c);
					}
				}
				List<Column<ROW, ?>> others = model.getPossibleColumns();
				if(others!=null) {
					if(addableColumns.size()>0) addableColumns.add(null);
					addableColumns.addAll(others);
				}
				System.out.println("ExtendTable.ExtendTable(...).new PopupAdapter() {...}.showPopup() "+model.getAllColumns()+addableColumns);
				if(addableColumns.size()>0) {
					popupMenu.add(new JSeparator());
					popupMenu.add(new JCustomLabel("Extra Columns", Font.BOLD));

					for (Column<ROW, ?> column2 : addableColumns) {
						if(column2==null) {
							popupMenu.add(Box.createVerticalStrut(5));
						} else {
							popupMenu.add(new ColumnCheckbox(column2));
						}
					}							
				}
				
				//Popup: Add Hierarchy menu
				if(model.isTreeViewEnabled()) {
					popupMenu.add(new JSeparator());
					popupMenu.add(new JCustomLabel("Hierarchy", Font.BOLD));
					boolean enabled = getModel().isTreeViewActive();
					popupMenu.add(new TreeViewCheckBox(enabled));
					if(enabled) {
						JMenu expandMenu = new JMenu("Expand/Collapse");
						popupMenu.add(expandMenu);
						expandMenu.add(new TreeViewExpandAll(true, true));
						expandMenu.add(new TreeViewExpandAll(true, false));
						expandMenu.add(new TreeViewExpandAll(false, true));
						expandMenu.add(new TreeViewExpandAll(false, false));
					}
				}
				
				//HeaderPopup

				if(popupMenu.getComponentCount()==0) return;
				
	            popupMenu.show(e.getComponent(), e.getX(), e.getY());
	        }
		});
	
		addMouseListener( new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					Point p = e.getPoint();
					int rowNumber = rowAtPoint( p );
					int colNumber = columnAtPoint( p );					
					if(rowNumber>=0 && colNumber>=0) {
						if(!isRowSelected(rowNumber)) getSelectionModel().setSelectionInterval(rowNumber, rowNumber);
						if(!isColumnSelected(colNumber)) setColumnSelectionInterval(colNumber, colNumber);
					}					
				}
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				int col = getSelectedColumn();
				int row = getSelectedRow();
				if(col<0 || row<0 || e.getClickCount()<2) return;
				
				
				Column<ROW, ?> column = getModel().getColumn(convertColumnIndexToModel(col));
				boolean consume = column.mouseDoubleClicked(ExtendTable.this, getModel().getRow(row), row, getModel().getValueAt(row, convertColumnIndexToModel(col)));
				if(consume) {
					e.consume();
					return;
				}
				
				if(getModel().isCanExpand()) {
					eventTreeExpand();
				}
			}
		});
		
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy");
		getActionMap().put("copy", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copySelection();
			}
		});		
		
		//Tree Expand / Collapse on Enter / doubleClick
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
		getActionMap().put("enter", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(getModel().isCanExpand()) eventTreeExpand();
			}
		});		
		
	}
	
	public class TreeViewExpandAll extends AbstractAction {
		private final boolean expandAll;
		private final boolean selectedOnly;
		public TreeViewExpandAll(final boolean expandAll, final boolean selectedOnly) {			
			super((expandAll?"Expand": "Collapse") + " " + (selectedOnly?"Selection": "All"));
			this.expandAll = expandAll;
			this.selectedOnly = selectedOnly;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new SwingWorkerExtended("Hierarchy", ExtendTable.this, false) {
				@Override
				protected void doInBackground() throws Exception {
					List<ROW> sel = new ArrayList<ROW>(selectedOnly? getSelection(): getRows());
					for (ROW row : sel) {
						expandRow(row, expandAll, 6, false);
					}
				}
				@Override
				protected void done() {					
					getModel().fireTableDataChanged();
					resetPreferredColumnWidth();
				}
			};
		
		}
	}
	
	
	protected synchronized void eventTreeExpand() {
		if(!getModel().isTreeViewActive() || getModel().getTreeColumn()==null) return;
		
		
		final int[] sRows = getSelectedRows();
		if(sRows.length!=1) return;
		
		final int[] sCols = getSelectedColumns();
		if(sCols.length!=1 || getModel().getColumn(convertColumnIndexToModel(sCols[0]))!=getModel().getTreeColumn()) return;
		
		final ROW obj = getModel().getRow(sRows[0]);
		final Node info = getModel().getNode(obj);
		if(info==null || info.leaf==Boolean.TRUE) return;
		
		new SwingWorkerExtended("Expand", ExtendTable.this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			@Override
			protected void done() {				
				expandRow(obj, !info.expanded, 1, true);
				int selCol = getModel().getColumns().indexOf(getModel().getTreeColumn());
				if(selCol>=0) {
					selCol = convertColumnIndexToView(selCol);
					setColumnSelectionInterval(selCol, selCol);
				}
			}
		};
		
	}	
	
	protected void expandRow(final ROW obj, final boolean expand, final int maxDepth, final boolean fireEvents) {
		final int[] sRows = getSelectedRows();

		
		int[] minMax = null;
		if(maxDepth<=0) {
			return;
		} else if(!expand) {
			List<ROW> toRemove = new ArrayList<>(); 
			getChildrenRec(obj, toRemove);

			Map<ROW, Integer> obj2index = new HashMap<>();
			for (int i = 0; i < getModel().getRows().size(); i++) {
				obj2index.put(getModel().getRow(i), i);			
			}
			List<Integer> indexes = new ArrayList<>();
			for (ROW row : toRemove) {
				Integer index = obj2index.get(row);
				if(index!=null) indexes.add(index);
				Node node = getModel().getNode(obj);
				if(node!=null) {
					node.expanded=false;
				}
			}
			if(indexes.size()==0) return;
			
			Collections.sort(indexes);
			
			
			for (int i = indexes.size()-1; i >= 0; i--) {
				getModel().getRows().remove((int)indexes.get(i));
			}
			
			minMax = new int[] {indexes.get(0), indexes.get(indexes.size()-1) - (indexes.size()-1) };
			
		} else if(expand) {
			
			Node node = getModel().getNode(obj);
			if(node!=null) {
				node.expanded=true;
			}
			int minIndex = getModel().getRows().indexOf(obj);

			List<ROW> children = new ArrayList<>(getModel().getTreeChildren(obj));
			if(children.size()>0) {
				Collections.sort(children, CompareUtils.OBJECT_COMPARATOR);
				for (ROW elt : children) {
					//Add or move the children to the end				
					getModel().getRows().remove(elt);
					getModel().getRows().add(elt);
					expandRow(elt, expand, maxDepth-1, false);
				}
				minMax = new int[]{minIndex, minIndex+children.size()};
			}
		}

		
		if(fireEvents) {
			if(!expand) {
				if(minMax!=null) {
					if(minMax[0]<=minMax[1] && minMax[0]>=0 && minMax[1]<getModel().getRowCount()) {
						getModel().fireTableRowsDeleted(minMax[0], minMax[1]);
					} else {
						getModel().fireTableDataChanged();
					}
				}
			} else {
				getModel().fireTableDataChanged();
			}
			resetPreferredColumnWidth();
			int index = getModel().getColumns().indexOf(getModel().getTreeColumn());
			
			if(index>=0) {
				index = convertColumnIndexToView(index);
				setColumnSelectionInterval(index, index);
			}
			if(sRows[0]<getRowCount()) setRowSelectionInterval(sRows[0], sRows[0]);		
		}

	}
	
	/**
	 * Add the children of obj into toPopulate (does not add obj)
	 * @param obj
	 * @param toPopulate
	 * @return
	 */
	private void getChildrenRec(ROW obj, List<ROW> toPopulate) {
		Collection<ROW> children = getModel().getTreeChildren(obj);
		for (ROW c : children) {
			toPopulate.add(c);
			getChildrenRec(c, toPopulate);
		}		
	}
	
	protected void copySelection() {
		StringBuilder sb = new StringBuilder();
		int[] rows = getSelectedRows();
		int[] cols = getSelectedColumns();
		
		if(cols.length>1) {
			//Add headers
			for (int c = 0; c<cols.length; c++) {
				if(c>0) sb.append("\t");
				int modelCol = convertColumnIndexToModel(cols[c]);
				Column<ROW, ?> column = getModel().getColumn(modelCol);
				String s = column.getName();
				if(s!=null) {
					sb.append(s.replaceAll("[\\s+]", " ").replaceAll("<.>", ""));
				}				
			}
			sb.append("\n");
		}
		
		for (int r = 0; r<rows.length; r++) {
			if(r>0) sb.append("\n");
			for (int c = 0; c<cols.length; c++) {
				if(c>0) sb.append("\t");
				int modelCol = convertColumnIndexToModel(cols[c]);
				Column<ROW, ?> column = getModel().getColumn(modelCol);
				String s = column.getCopyValue(getRows().get(rows[r]), rows[r]);
				if(s!=null) {
					sb.append(s);
				}
			}					
		}
		EasyClipboard.setClipboard(sb.toString());		
	}
	
	

	
	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {
			@Override
			public String getToolTipText(MouseEvent event) {
                java.awt.Point p = event.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                if(index<0) return null;
                int realIndex = columnModel.getColumn(index).getModelIndex();
                if(realIndex<0 || realIndex>=getModel().getColumnCount()) return null;
                Column<ROW, ?> col = getModel().getColumn(realIndex);
                return col.getToolTipText()==null? "<html>" + col.getName().replace("\n", "<br>") : "<html>" + col.getToolTipText().replace("\n", "<br>");
			}
		};
	}

	@Override
	public Object getValueAt(int row, int column) {
		
		int modelCol = convertColumnIndexToModel(column);
		if(modelCol>=(2<<16)) {
			System.err.println("Maximum "+(2<<16) + "columns");
			return super.getValueAt(row, column);
		}
		int key = (row*(2<<16)) + modelCol; //allows up to 1024 column, should be enough
		
		Object res;
		if(valuesCacheMap.containsKey(key)) {
			res = valuesCacheMap.get(key);
		} else {
			//Not in cache
			
			if(modelCol<0 || modelCol>=getModel().getColumnCount()) {
				res = null;
			} else {
				res = super.getValueAt(row, column);
			}
			valuesCacheMap.put(key, res);
		}
		return res;
	}

}
