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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

import com.actelion.research.util.ui.EasyClipboard;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.PopupAdapter;

public class GridInputTable extends JTable {

	private int lastEditingRow = -1;
	private int lastEditingCol = -1;


	private final class MyKeyListener extends KeyAdapter {
		private JComponent source;

		public MyKeyListener(JComponent source) {
			this.source = source;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if(e.getKeyCode()==86 && (e.getModifiersEx()&KeyEvent.CTRL_DOWN_MASK)>0) {
				String clipboard = EasyClipboard.getClipboard();
				if(clipboard!=null && clipboard.indexOf('\r')>=0 || clipboard.indexOf('\n')>=0 || clipboard.indexOf('\t')>=0) {
					editingStopped(new ChangeEvent(this));
					try {
						pasteSelection();
					} catch (Exception ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(GridInputTable.this, "Pasting returned some errors:\n"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					}
					e.consume();
				}
			} else if(e.getKeyCode()==27) {//ESCAPE
				editingStopped(new ChangeEvent(this));
				e.consume();
			} else if(e.getKeyCode()==38) {//UP
				goUp();
				e.consume();
			} else if(e.getKeyCode()==40) {//DOWN
				goDown();
				e.consume();
			} else if(e.getKeyCode()==37) {//Left
				if((!(source instanceof JTextComponent) || ((JTextComponent) source).getCaretPosition()==0)) {
					goLeft();
					e.consume();
				}
			} else if(e.getKeyCode()==39) {//Right
				if((!(source instanceof JTextComponent) || ((JTextComponent) source).getCaretPosition()==((JTextComponent) source).getText().length())) {
					goRight();
					e.consume();
				}
			}
		}
	}

	public static class GridInputTableModel extends AbstractTableModel {
		private GridUndoManager undoManager = null;
		private List<String[]> data = new ArrayList<String[]>();


		public GridUndoManager getUndoManager() {
			return undoManager;
		}
		public void setUndoManager(GridUndoManager undoManager) {
			this.undoManager = undoManager;
		}


		@Override
		public String getValueAt(int rowIndex, int columnIndex) {
			return data.get(rowIndex)[columnIndex];
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			String oldVal = data.get(rowIndex)[columnIndex];
			String newVal = (String) aValue;
			data.get(rowIndex)[columnIndex] = newVal;
			if(undoManager!=null) undoManager.addEdit(undoManager.new OneChangeEdit(rowIndex, columnIndex, oldVal, newVal));
		}
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		@Override
		public int getColumnCount() {
			return data.size()>0? data.get(0).length: 0;
		}

		@Override
		public int getRowCount() {
			return data.size();
		}
		public List<String[]> getData() {
			return data;
		}
		public void setData(List<String[]> data) {
			this.data = data;
			fireTableDataChanged();
		}


		public void addColumn() {
			setColumns(getColumnCount()+1);
		}
		public void addRow() {
			List<String[]> newTable = getData();
			newTable.add(new String[getColumnCount()]);
			setData(newTable);
		}

		public void setColumns(int cols) {
			List<String[]> newTable = new ArrayList<String[]>();

			List<String[]> data = getData();
			for (String[] row : data) {
				String[] newRow = new String[cols];
				for (int i = 0; i < newRow.length; i++) {
					newRow[i] = i<row.length? row[i]: "";
				}
				newTable.add(newRow);
			}

			setData(newTable);
		}

	}

	public GridInputTable() {
		super(new GridInputTableModel());
		getModel().setUndoManager(new GridUndoManager(GridInputTable.this));
		String[][] table = new String[10][10];
		table[0][0] = "Corner";
		table[2][2] = "Test";
		table[7][4] = "Test2";
		setTable(table);
		setTableHeader(null);
		setColumnSelectionAllowed(true);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setRowHeight(20);
		putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		putClientProperty("JTable.autoStartsEdit", Boolean.TRUE);
		putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		setSurrendersFocusOnKeystroke(true);
		setFillsViewportHeight(true);
		setShowGrid(true);

		setDefaultEditor(Object.class, new AlphaNumericalCellEditor());

		setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			JLabelNoRepaint lbl = new JLabelNoRepaint();
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				JComponent c = (JComponent)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				lbl.setVerticalAlignment(SwingConstants.CENTER);
				lbl.setText((String)value);
				lbl.setForeground(c.getForeground());
				lbl.setBackground(c.getBackground());
				lbl.setBorder(c.getBorder());
				lbl.setFont(row==0? FastFont.BOLD: FastFont.REGULAR);
				return lbl;
			}
		});

		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke('Z', InputEvent.CTRL_DOWN_MASK), "undo");
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke('Y', InputEvent.CTRL_DOWN_MASK), "redo");
		getActionMap().put("undo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getModel().getUndoManager().undo();
			}
		});
		getActionMap().put("redo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getModel().getUndoManager().redo();
			}
		});


		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('C', InputEvent.CTRL_DOWN_MASK), "copy");
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('V', InputEvent.CTRL_DOWN_MASK), "paste");
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('X', InputEvent.CTRL_DOWN_MASK), "cut");

		getActionMap().put("copy", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copySelection();
			}
		});
		getActionMap().put("delete", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteSelection();
			}
		});

		getActionMap().put("paste", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					pasteSelection();
					resetPreferredColumnWidth();
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(GridInputTable.this, "Pasting returned some errors:\n"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		getActionMap().put("cut", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copySelection();
				deleteSelection();
			}
		});

		getModel().addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				fitTable();
			}
		});


		//MouseClick should select the cell but not start editing
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int row = rowAtPoint(e.getPoint());
				int col = columnAtPoint(e.getPoint());
				if(col>=0) {
					col = convertColumnIndexToModel(col);
					row = convertRowIndexToModel(row);
					//The user must double-click to edit a cell (Excel-like) except if it is a Boolean (checkbox)

					if((row!=lastEditingRow || col!=lastEditingCol)) {
						editingStopped(null);
						requestFocusInWindow();
					}
				}
				lastEditingRow = row;
				lastEditingCol = col;
			}
		});

		//MousePopup to insert rows
		addMouseListener(new PopupAdapter(this) {
			@Override
			protected void showPopup(MouseEvent e) {
				JPopupMenu popupMenu = new JPopupMenu();
				popupMenu.add(new Copy_Action());
				popupMenu.add(new Paste_Action());
				popupMenu.add(new JSeparator());
				popupMenu.add(new UndoAction());
				popupMenu.add(new RedoAction());


				popupMenu.show(GridInputTable.this, e.getX(), e.getY());
			}
		});

		addKeyListener(new MyKeyListener(this));
	}

	protected void resetPreferredColumnWidth() {

		GridInputTableModel model = getModel();

		for (int col = 0; col < model.getColumnCount(); col++) {
			int colMaxWidth = 30;

			//Check Data width
			for (int row = 0; row < model.getRowCount(); row++) {
				Component c = getCellRenderer(row, col).getTableCellRendererComponent(this, getValueAt(row, col), true, true, row, col);

				int w = c.getPreferredSize().width+4;
				colMaxWidth = Math.max(colMaxWidth, w);
			}

			//Update Column Width
			TableColumn column = getColumnModel().getColumn(col);
			column.setPreferredWidth(colMaxWidth);
		}



	}

	@Override
	public GridInputTableModel getModel() {
		return (GridInputTableModel) super.getModel();
	}


	public void setTable(String[][] table) {
		List<String[]> rows = new ArrayList<String[]>();
		//Calculate the number of cols
		int cols = 0;
		for(String[] row: table) {
			cols = Math.max(cols, row.length);
		}

		//Update the table
		for(String[] row: table) {
			String[] stringArray = new String[cols];
			for (int i = 0; i < stringArray.length; i++) {
				stringArray[i] = i<row.length && row[i]!=null? row[i]: "";
			}
			rows.add(stringArray);
		}

		getModel().setData(rows);
		fitTable();
		getModel().fireTableStructureChanged();

		resetPreferredColumnWidth();


	}

	/**
	 * Add an extra row/column if needed
	 */
	public void fitTable() {
		int cols = getModel().getColumnCount();
		//AddRow?
		boolean addRow = false;
		List<String[]> data = getModel().getData();
		for(String s: data.get(data.size()-1)) {
			if(s!=null && s.length()>0) {
				addRow = true; break;
			}
		}
		if(addRow) {
			getModel().addRow();
		}

		//AddColumn?
		boolean addColumn = false;
		for(String[] row: data) {
			if(cols<=0 || (row[cols-1]!=null && row[cols-1].length()>0)) {
				addColumn = true; break;
			}
		}
		if(addColumn) {
			getModel().addColumn();
		}

		if(addRow || addColumn) {
			getModel().fireTableStructureChanged();
			resetPreferredColumnWidth();
		}

	}



	public String[][] getTable() {
		String[][] table = getModel().getData().toArray(new String[][] {});

		//Clean data by emptying last columns/rows
		int maxCols = 0;
		int maxRows = 0;
		for (int i = 0; i < table.length; i++) {
			for (int j = 0; j < table[i].length; j++) {
				if(table[i][j]!=null && table[i][j].length()>0) {
					maxRows = Math.max(maxRows, i);
					maxCols = Math.max(maxCols, j);
				}
			}
		}

		String[][] res = new String[maxRows+1][maxCols+1];
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res[i].length; j++) {
				res[i][j] = table[i][j]==null?"": table[i][j].trim();
			}
		}
		return res;
	}

	public static void main(String[] args) {

		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			UIManager.put("nimbusSelectionBackground", new Color(173,207,231));
		} catch (Exception e) {
			e.printStackTrace();
		}


		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.CENTER, new JScrollPane(new GridInputTable()));
		panel.add(BorderLayout.NORTH, new JLabel("GridTable"));
		panel.setPreferredSize(new Dimension(400, 300));

		JOptionPane.showOptionDialog(null, panel, "Test", JOptionPane.YES_NO_OPTION, 0, null, null, null);
	}

	private void copySelection() {
		StringBuilder sb = new StringBuilder();
		int[] rows = getSelectedRows();
		int[] cols = getSelectedColumns();
		for (int r = 0; r<rows.length; r++) {
			if(r>0) sb.append("\n");
			for (int c = 0; c<cols.length; c++) {
				if(c>0) sb.append("\t");
				Object o = getValueAt(rows[r], cols[c]);
				sb.append(o==null? "": o);
			}
		}

		EasyClipboard.setClipboard(sb.toString());
	}


	private GridUndoManager getUndoManager() {
		return getModel().getUndoManager();
	}
	private void deleteSelection() {
		int[] rows = getSelectedRows();
		int[] cols = getSelectedColumns();
		editingCanceled(new ChangeEvent(GridInputTable.this));
		getUndoManager().setTransaction(true);
		for (int r = 0; r<rows.length; r++) {
			for (int c = 0; c<cols.length; c++) {
				int modelRow = rows[r];
				int modelCol = convertColumnIndexToModel(cols[c]);
				if(!getModel().isCellEditable(modelRow, modelCol)) continue;
				try {
					getModel().setValueAt(null, modelRow, modelCol);
				} catch (Exception ex) {
					System.err.println(ex);
				}
			}
		}
		getUndoManager().setTransaction(false);
		getModel().fireTableDataChanged();
		select(rows, cols);
	}

	/**
	 * Paste data
	 */
	protected void pasteSelection() throws Exception {
		int[] selRows = getSelectedRows();
		int[] selCols = getSelectedColumns();

		if(selRows.length==0 || selCols.length==0) return;

		String paste = EasyClipboard.getClipboard();
		if(paste==null) return;
		String[] lines = paste.split("(\r\n|\r|\n|\n\r)");
		if(lines.length==0) return;
		String[] tokensFor1Line = null;
		tokensFor1Line = lines[0].split("\t");

		if(cellEditor!=null) cellEditor.stopCellEditing();
		int selRow = getSelectedRow();
		int selCol = getSelectedColumn();

		//Apply the Paste, line by line
		StringBuilder errors = new StringBuilder();
		getUndoManager().setTransaction(true);
		if(lines.length>1 || selRows.length==1 || tokensFor1Line.length!=selCols.length) {
			//Paste one block
			int row = selRow;
			for (String line : lines) {
				int col = selCol;
				String[] tokens = line.split("\t");
				for (String token : tokens) {
					if(convertRowIndexToModel(row)>=getModel().getRowCount()) {
						getModel().addRow();
					}
					if(col>=getModel().getColumnCount()) {
						getModel().addColumn();
					}
					try {
						getModel().setValueAt(token.trim(), row, col);
					} catch (ArrayIndexOutOfBoundsException ex) {
						errors.append("Table too small (" + token.trim() +")\n");
					} catch (Exception ex) {
						errors.append("Err:"+ex.getMessage() + " (" + token.trim() +")\n");
					}
					col++;
				}
				row++;
			}
		} else {
			//The user selected several lines -> copy the data n times
			for(int row: selRows) {
				for(int i=0; i<selCols.length; i++) {
					try {
						getModel().setValueAt(tokensFor1Line[i].trim(), row, selCols[i]);
					} catch (Exception ex) {
						errors.append("Paste:"+ex.getMessage() + " (" + tokensFor1Line[i].trim().trim() + ")\n");
					}
				}
			}

		}
		getUndoManager().setTransaction(false);
		if(errors.length()>0) {
			getUndoManager().undo();
			throw new Exception(errors.toString());
		}
		getModel().fireTableStructureChanged();
		resetPreferredColumnWidth();
		select(selRows, selCols);
	}

	private void select(int[] rows, int[] cols) {
		clearSelection();
		for (int c : cols) {
			addColumnSelectionInterval(c, c);
		}
		for (int r : rows) {
			addRowSelectionInterval(r, r);
		}
	}


	public class Paste_Action extends AbstractAction {
		public Paste_Action() {
			putValue(AbstractAction.NAME, "Paste");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				pasteSelection();
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(GridInputTable.this, "Pasting returned some errors:\n"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public class Copy_Action extends AbstractAction {
		public Copy_Action() {
			putValue(AbstractAction.NAME, "Copy");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			copySelection();
		}
	}

	protected class UndoAction extends AbstractAction {
		public UndoAction() {
			putValue(Action.NAME, "Undo");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('Z', InputEvent.CTRL_DOWN_MASK));
			setEnabled(getModel().getUndoManager().canUndo());

		}
		@Override
		public void actionPerformed(ActionEvent e) {
			getModel().getUndoManager().undo();
		}
	}
	protected class RedoAction extends AbstractAction {
		public RedoAction() {
			putValue(Action.NAME, "Redo");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('Y', InputEvent.CTRL_DOWN_MASK));
			setEnabled(getModel().getUndoManager().canRedo());

		}
		@Override
		public void actionPerformed(ActionEvent e) {
			getModel().getUndoManager().redo();
		}
	}

	public void goRight() {
		editingStopped(new ChangeEvent(this));
		int row = getSelectedRow();
		int col = getSelectedColumn();
		if(row<0 || col<0) return;

		if(col>=getColumnCount()-1) {
			getModel().addColumn();
			getModel().fireTableStructureChanged();
			resetPreferredColumnWidth();
		}
		setRowSelectionInterval(row, row);
		setColumnSelectionInterval(col+1, col+1);
	}

	public void goLeft() {
		editingStopped(new ChangeEvent(this));
		int row = getSelectedRow();
		int col = getSelectedColumn();
		if(row<0 || col<0) return;

		if(col>0) {
			setColumnSelectionInterval(col-1, col-1);
		}
	}

	public void goUp() {
		editingStopped(new ChangeEvent(this));
		int row = getSelectedRow();
		int col = getSelectedColumn();
		if(row<0 || col<0) return;
		if(row>0) {
			setColumnSelectionInterval(col, col);
			setRowSelectionInterval(row-1, row-1);
		}
	}

	public void goDown() {
		editingStopped(new ChangeEvent(this));
		int row = getSelectedRow();
		int col = getSelectedColumn();
		if(row<0 || col<0) return;
		if(row>=getRowCount()-1) {
			getModel().addRow();
			getModel().fireTableStructureChanged();
			resetPreferredColumnWidth();
		}
		if(row<getRowCount()-1) {
			setColumnSelectionInterval(col, col);
			setRowSelectionInterval(row+1, row+1);
		}
	}


}
