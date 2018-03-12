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

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;

import com.actelion.research.util.ui.EasyClipboard;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.exceltable.ExtendTableModel.Node;

/**
 * This class represents a table, with all the features of AbstractExtendTable (copy, extra columns). This table cannot be edited
 * However the table is sortable, and each row can be expended (by double-clicking) if the model implements getChildren or getParent
 *
 * @author Joel Freyss
 *
 * @param <ROW> to represent the class of the encapsulated rows
 */
public class ExtendTable<ROW> extends AbstractExtendTable<ROW> {

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
