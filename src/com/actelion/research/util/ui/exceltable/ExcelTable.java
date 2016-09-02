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
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import com.actelion.research.util.ui.EasyClipboard;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.SwingWorkerExtended;

/**
 * The ExcelTable replaces the JTable to mimic tables like Excel:
 * - double click to start editing
 * - typing text will start editing by replacing the existing text
 * - copy paste
 * - pluggable ExcelTableModel
 * 
 * Check the main() function for an example
 * @author freyssj
 *
 * @param <ROW>
 */
public class ExcelTable<ROW> extends AbstractExtendTable<ROW> {

	public static final boolean DEBUG = false; 

	private boolean canAddRow = true;
	private boolean canSort = true;
	private ExcelUndoManager undoManager = new ExcelUndoManager(this);
	private boolean goNextOnEnter = false;


	
	private int lastEditingRow = -1;
	private int lastEditingCol = -1;
	private SwingWorkerExtended popupShowWorker = null;

	/**
	 * 
	 * @param model
	 */
	public ExcelTable(final ExcelTableModel<ROW> model) {
		super();
		setModel(model);

		putClientProperty("JTable.autoStartsEdit", Boolean.TRUE);
		putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		setFillsViewportHeight(true);

		if(model.getRowCount()==0) {
			ROW t = getModel().createRecord();
			if(t!=null && canAddRow) model.add(t);
		}
				
		/**
		 * Popup when right-click
		 */
		getTableHeader().addMouseListener(new PopupAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				switch(getHeaderClickingPolicy()) {
				case SORT:
					if (SwingUtilities.isLeftMouseButton(e)) {
						final int col = columnAtPoint(e.getPoint());
						if(col>=0) sortBy(convertColumnIndexToModel(col));
					}
					break;
				case POPUP:
					if(popupShowWorker!=null) {
						 popupShowWorker.cancel();
					 }
					 popupShowWorker = new SwingWorkerExtended() {
						 @Override
						protected void doInBackground() throws Exception {
							 try {Thread.sleep(500);}catch(Exception ex) {return;}								 								
						}
						@Override
						protected void done() {
							if(e.getClickCount()<=1) showPopup(e);
							popupShowWorker = null;
						}
					};
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
			
			@Override
			protected void showPopup(MouseEvent e) {
				
				final JPopupMenu popupMenu = new JPopupMenu();
				
				final int col = columnAtPoint(e.getPoint());
				Column<ROW, ?> column = null;
				if(col>=0) {
					column = model.getColumn(convertColumnIndexToModel(col));
					
					popupMenu.add(new JCustomLabel("Column: " + column.getShortName(), Font.BOLD));
					popupMenu.add(new JSeparator());
					
					if(canSort) {						
						column.populateHeaderPopup(ExcelTable.this, popupMenu);
					}
				}
				

				//Popup: Add TreeView activation
				if(model.isTreeViewEnabled()) {
					popupMenu.add(new JSeparator());
					popupMenu.add(new JCustomLabel("Hierarchy", Font.BOLD));
					popupMenu.add(new TreeViewCheckBox(getModel().isTreeViewActive()));
				}
				
				//Custom actions
				populateHeaderPopup(popupMenu, column);
				
				
				//Add possible columns
				List<Column<ROW, ?>> addableColumns = new ArrayList<Column<ROW,?>>();
				for (Column<ROW, ?> c : model.getAllColumns()) {
					if(c.isHideable()) {
						addableColumns.add(c);
					}
				}
				List<Column<ROW, ?>> others = model.getPossibleColumns();
				if(addableColumns.size()>0 && others!=null) addableColumns.add(null);
				if(others!=null) addableColumns.addAll(others);
				
				if(addableColumns.size()>0) {
					popupMenu.add(new JSeparator());
					popupMenu.add(new JCustomLabel("Add Columns", Font.BOLD));

					for (Column<ROW, ?> column2 : addableColumns) {
						if(column2==null) {
							popupMenu.add(Box.createVerticalStrut(5));
						} else {
							popupMenu.add(new ColumnCheckbox(column2));
						}
					}							
				}
				
				if(popupMenu.getComponents().length>0) {
					popupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
	        }
			
		});
		
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);		
		setCellSelectionEnabled(true);
		setAutoscrolls(true);
		putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		setSurrendersFocusOnKeystroke(true);

		setDefaultRenderer(Object.class, new ExtendTableCellRenderer<ROW>(this));
		
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
		getActionMap().put("down", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goDown();
			}
		});
		
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "next");
		getActionMap().put("next", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goNext();
			}
		});
		
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
		getActionMap().put("up", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int row = getSelectedRow();
				int col = getSelectedColumn();
				if(row>0) {
					editingStopped(new ChangeEvent(this));
					setRowSelectionInterval(row-1, row-1);
					setColumnSelectionInterval(col, col);
				}
			}
		});
		
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "right");
		getActionMap().put("right", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goRight();
			}
		});		
		
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
		getActionMap().put("left", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goLeft();
			}
		});
		
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke('Z', InputEvent.CTRL_DOWN_MASK), "undo");
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke('Y', InputEvent.CTRL_DOWN_MASK), "redo");
		getActionMap().put("undo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				undoManager.undo();
			}
		});
		getActionMap().put("redo", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				undoManager.redo();
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
					resetPreferredColumnWidth();
					JExceptionDialog.showError(ExcelTable.this, "Pasting returned some errors:\n"+ex.getMessage());
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
		
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				final int row = getSelectedRow();
				final int col = getSelectedColumn();
				final int modelRow = convertRowIndexToModel(row);
				final int modelCol = convertColumnIndexToModel(col);
				if(modelCol<0) return;

				//Hack to fix Java bug, where editable combobox don't get the keyevent dispatched to their editor 
				if(e.getKeyChar()>=32 && e.getKeyChar()<127 && e.getKeyChar()!=127 && (e.getModifiersEx()&KeyEvent.CTRL_DOWN_MASK)==0) {
					if(!isEditing()) {
						if(!getModel().isCellEditable(row, col)) return;
						editCellAt(modelRow, modelCol);
					}
				}
				
			}
			@Override
			public void keyReleased(KeyEvent e) {
				lastEditingRow = convertRowIndexToModel(getSelectedRow());
				lastEditingCol = convertColumnIndexToModel(getSelectedColumn());
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
				if(canAddRow) {
					popupMenu.add(new JSeparator());
					popupMenu.add(new InsertRow_Action(true));
					popupMenu.add(new InsertRow_Action(false));
					popupMenu.add(new DeleteRow_Action());
				}
				
//				populateHeaderPopup(popupMenu, false);

				popupMenu.add(new JSeparator());
				popupMenu.add(new UndoAction());
				popupMenu.add(new RedoAction());
				
				
				popupMenu.show(ExcelTable.this, e.getX(), e.getY());
			}			
		});
					
//		init();
	}
	
	@Override
	public void createDefaultColumnsFromModel() {
		super.createDefaultColumnsFromModel();
		
		if(getColumnCount()!=getModel().getColumnCount()) {
			System.err.println("getColumnCount()!=getModel().getColumnCount() in "+this+" getAutoCreateColumnsFromModel="+getAutoCreateColumnsFromModel());
		}
		
		//Define default cell editors
		for (int i = 0; i < getColumnCount(); i++) {
			TableColumn col = getColumnModel().getColumn(i);
			Column<ROW, ?> column = getModel().getColumn(i);
			TableCellEditor editor = column.getCellEditor(this);
			if(editor!=null) {
				col.setCellEditor(editor);
			} else if(dataModel.getColumnClass(i)==Boolean.class) {					
				col.setCellEditor(new BooleanCellEditor());					
			} else if(dataModel.getColumnClass(i)==Double.class) {
				col.setCellEditor(new DoubleCellEditor());					
			} else if(dataModel.getColumnClass(i)==Integer.class) {
				col.setCellEditor(new IntegerCellEditor());					
			} else {
				col.setCellEditor(new AlphaNumericalCellEditor());
			}			
		}		
				
		//Postprocess
		initCellEditors();
		
		for (int i = 0; i < getColumnCount(); i++) {
			TableColumn col = getColumnModel().getColumn(i);
			assert col.getCellEditor()!=null;
			try {
				JComponent comp = (JComponent) col.getCellEditor().getTableCellEditorComponent(ExcelTable.this, null, false, 0, i);
				addCellEditorListeners(comp);
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		}	
	}

	@Override
	public void setModel(TableModel dataModel) {
		if(dataModel instanceof ExcelTableModel) {
			super.setModel(dataModel);
			getModel().setUndoManager(undoManager);
		}				
	}
	
	
	@Override
	public ExcelTableModel<ROW> getModel() {
		return (ExcelTableModel<ROW>) super.getModel();
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
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('V', InputEvent.CTRL_DOWN_MASK));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				pasteSelection();
			} catch (Exception ex) {
				JExceptionDialog.showError(ExcelTable.this, "Pasting returned some errors:\n"+ex.getMessage());
			}
		}
	}
	
	public class Copy_Action extends AbstractAction {
		public Copy_Action() {			
			putValue(AbstractAction.NAME, "Copy");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('Z', InputEvent.CTRL_DOWN_MASK));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			copySelection();
		}
	}
	
	/**
	 * 
	 * @author freyssj
	 *
	 */
	public class InsertRow_Action extends AbstractAction {
		private boolean before;
		public InsertRow_Action(boolean before) {			
			this.before = before;
			putValue(AbstractAction.NAME, "Insert Rows " + (before?"Before": "After"));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			int selRow = getSelectedRow();
			ROW t = getModel().createRecord();
			if(t==null) return;

			if(selRow<0) {
				if(getRowCount()==0) {
					getModel().getRows().add(selRow, getModel().createRecord());
					getModel().fireTableDataChanged();
				}
				return;
			}
			String res = JOptionPane.showInputDialog(ExcelTable.this, "Enter the number of rows you want to insert " +  (before?"before": "after") + " row " + selRow + "?");
			int n;
			try {
				n = Integer.parseInt(res);
			} catch (Exception ex) {
				return;
			}
			if(!before) selRow = selRow+1;
			if(selRow>=getModel().getRows().size()) {
				for (int i = 0; i < n; i++) {
					getModel().getRows().add(getModel().createRecord());
				}
			} else {
				for (int i = 0; i < n; i++) {
					getModel().getRows().add(selRow, getModel().createRecord());
				}
			}
			getModel().fireTableDataChanged();
		}
	}
	
	public class DeleteRow_Action extends AbstractAction {
		public DeleteRow_Action() {			
			putValue(AbstractAction.NAME, "Delete Rows");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			int[] selRows = getSelectedRows();
			ROW t = getModel().createRecord();
			if(t==null) return;
			Arrays.sort(selRows);
			
			for (int i = 0; i<selRows.length; i++) {
				if(!canRemove(getModel().getRow(selRows[i]))) {
					JExceptionDialog.showError(ExcelTable.this, "You cannot delete the row "+(selRows[i]+1));
					return;
				}
			}
			for (int i = selRows.length-1; i>=0; i--) {
				getModel().getRows().remove(selRows[i]);
			}
			if(getModel().getRows().size()==0) {
				getModel().getRows().add(t);
			}
			getModel().fireTableDataChanged();
		}
	}

	public void setSelection(ROW row, String headerName) {
		clearSelection();
		
		//Select the header
		if(headerName!=null) {
			for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
				TableColumn c = getColumnModel().getColumn(i);
				if(c.getHeaderValue().toString().equalsIgnoreCase(headerName)) {
					setColumnSelectionInterval(i, i);
					break;
				}				
			}
		} else {
			setColumnSelectionInterval(0, getColumnCount()-1);
		}
		
		//Select the row
		for(int i=0; i<getRowCount(); i++) {
			if(getModel().getRows().get(convertRowIndexToModel(i))==row) {
				setRowSelectionInterval(i, i);
				break;
			}
		}
		scrollTo(row);
	}

	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {
			@Override
			public String getToolTipText(MouseEvent event) {
                java.awt.Point p = event.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                if(index<0) return null;
                int realIndex = convertColumnIndexToModel(index);
                Column<ROW, ?> col = getModel().getColumn(realIndex);
                return col.getToolTipText()==null? "<html>" + col.getName().replace("\n", "<br>") : "<html>" + col.getToolTipText().replace("\n", "<br>");
			}
		};
	}
	

	

	public ExcelUndoManager getUndoManager() {
		return undoManager;
	}
	
	protected class UndoAction extends AbstractAction {
		public UndoAction() {
			putValue(Action.NAME, "Undo");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('Z', InputEvent.CTRL_DOWN_MASK));
			setEnabled(undoManager.canUndo());
			
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			undoManager.undo();			
		}
	}
	protected class RedoAction extends AbstractAction {
		public RedoAction() {
			putValue(Action.NAME, "Redo");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke('Y', InputEvent.CTRL_DOWN_MASK));
			setEnabled(undoManager.canRedo());
			
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			undoManager.redo();			
		}
	}
	
	/**
	 * @param canAddRow the canAddRow to set
	 */
	public void setCanAddRow(boolean canAddRow) {
		this.canAddRow = canAddRow;
	}


	/**
	 * @return the canAddRow
	 */
	public boolean isCanAddRow() {
		return canAddRow;
	}


	
	public boolean isGoNextOnEnter() {
		return goNextOnEnter;
	}


	public void setGoNextOnEnter(boolean goNextOnEnter) {
		this.goNextOnEnter = goNextOnEnter;
	}


	/**
	 * @param canSort the canSort to set
	 */
	public void setCanSort(boolean canSort) {
		this.canSort = canSort;
	}


	/**
	 * @return the canSort
	 */
	public boolean isCanSort() {
		return canSort;
	}
	
	/**
	 * To be overridden, reset the celleditors for each column.
	 * This method is called on startup or when a column is added
	 */
	public void initCellEditors() {}
	

	
	///////////////////////////
	// EXAMPLE


	/**
	 * Example of Use
	 * @param args
	 */
	public static void main(String[] args) {
//		
//		
//		String s = "line1\rline2\nline3\r\nline4\n\n\nwas just empty\r\r\rthere too";
//		for (String string : s.split("\\r?\\n")) {
//			System.out.println(string);
//		}
//		if(true) return;
		
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");			
			UIManager.put("nimbusSelectionBackground", new Color(173,207,231));
		} catch (Exception e) {
			e.printStackTrace();
		}

		JFrame test = new JFrame();
		
		List<Column<String[], ?>> columns = new ArrayList<Column<String[], ?>>();
		columns.add(new Column<String[], String>("Col 1", String.class) {
			@Override
			public String getValue(String[] row) {
				return row[0];
			}
			@Override
			public void setValue(String[] row, String value) {
				row[0] = (String) value;
			}
		});
		columns.add(new Column<String[], String>("Col 2", String.class) {
			@Override
			public String getValue(String[] row) {
				return row[1];
			}
			@Override
			public void setValue(String[] row, String value) {
				row[1] = (String) value;
			}
		});
		columns.add(new Column<String[], String>("Non Edit", String.class) {
			@Override
			public String getValue(String[] row) {
				return "Non editable";
			}
			@Override
			public boolean isEditable(String[] row) {return false;}
		});
		columns.add(new Column<String[], Boolean>("Boolean", Boolean.class) {
			@Override
			public Boolean getValue(String[] row) {
				return "1".equals(row[2]);
			}
			@Override
			public void setValue(String[] row, Boolean value) {
				row[2] = value==Boolean.TRUE? "1": "0";
			}
		});
		
		columns.add(new Column<String[], Double>("Double", Double.class) {
			@Override
			public Double getValue(String[] row) {
				try {
					return row[3]==null || row[3].length()==0?null: Double.parseDouble(row[3]);
				} catch (Exception e) {
					return null;
				}
			}
			@Override
			public void setValue(String[] row, Double value) {
				row[3] = value==null?"": value.toString();
			}
			
		});
		columns.add(new Column<String[], String>("Combo", String.class, 100) {
			@Override
			public String getValue(String[] row) {
				return row[4];
			}
			@Override
			public void setValue(String[] row, String value) {
				row[4] = (String) value;
			}			
			
			@Override
			public TableCellEditor getCellEditor(AbstractExtendTable<String[]> table) {
				return new ComboboxCellEditor<String>(new JGenericComboBox<String>(new String[]{"M","F"}, true));
			}
		});
		
		ExcelTableModel<String[]> model = new ExcelTableModel<String[]>(columns){
			@Override
			public String[] createRecord() {
				return new String[5];
			}			
		};
		model.add(new String[] {"1", null, null, null, null});
		model.add(new String[] {"2", "aaaaaaaaaa", null, null, null});
		model.add(new String[] {"3", "bbbbbb", null, "1.55", "M"});
		model.add(new String[] {"4", "ccccccc", null, null, null});
		model.add(new String[] {"5", "abcdabcd", null, null, null});
		model.add(new String[] {"6", "abcdabcdefg", null, null, null});
		
		
		ExcelTable<String[]> table = new ExcelTable<String[]>(model);
		test.setContentPane(new JScrollPane(table));
		test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		test.setSize(650,600);
		test.setVisible(true);
	}


	/**
	 * Can be overriden
	 * @param row
	 * @return
	 */
	public boolean canRemove(ROW row) {
		return true;
	}
	

	@Override
	public boolean getScrollableTracksViewportHeight() {
        Component parent = getParent();

        if (parent instanceof JViewport) return parent.getHeight() > getPreferredSize().height;

        return false;
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
	
	private void deleteSelection() {
		int[] rows = getSelectedRows();
		int[] cols = getSelectedColumns();
		editingCanceled(new ChangeEvent(ExcelTable.this));
		undoManager.setTransaction(true);
		for (int r = 0; r<rows.length; r++) {
			for (int c = 0; c<cols.length; c++) {
				int modelRow = rows[r];
				int modelCol = convertColumnIndexToModel(cols[c]);
				if(!getModel().isCellEditable(modelRow, modelCol)) continue;
				try {
					getModel().paste(null, modelRow, modelCol);
				} catch (Exception ex) {
					System.err.println(ex);
				}
			}					
		}
		undoManager.setTransaction(false);
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
		undoManager.setTransaction(true);
		if(lines.length>1 || selRows.length==1 || tokensFor1Line.length!=selCols.length) {
			//Paste one block
			int row = selRow;
			loop: for (String line : lines) {
				int col = selCol;
				String[] tokens = line.split("\t");
				for (String token : tokens) {
					if(row>=getRowCount()) {
						if(canAddRow) {
							getModel().getRows().add(getModel().createRecord());
						} else {
							continue;
						}
					} 	
					if(col>=getColumnCount()) {
						continue;
					}
					try {						
						getModel().paste(token.trim(), row, convertColumnIndexToModel(col));
					} catch (Exception ex) {
						ex.printStackTrace();
						errors.append(ex.getMessage() + " (" + token.trim() +")\n");
						if(errors.length()>300) break loop;
					}
					col++;
				}
				row++;
			}
		} else {
			//The user selected several lines -> copy the data n times
			loop: for(int row: selRows) {
				for(int i=0; i<selCols.length; i++) {					
					try {
						getModel().paste(tokensFor1Line[i].trim(), row, convertColumnIndexToModel(selCols[i]));					
					} catch (Exception ex) {
						ex.printStackTrace();
						errors.append(ex.getMessage() + " (" + tokensFor1Line[i].trim().trim() + ")\n");
						if(errors.length()>300) break loop; 
					}
				}
			}
			
		}
		undoManager.setTransaction(false);
		if(errors.length()>0) {			
			undoManager.undo();
			throw new Exception(errors.toString());
		}
		getModel().fireTableDataChanged();
		select(selRows, selCols);
	}
		
	private final class MyKeyListener extends KeyAdapter {
		private JComponent source;
		
		public MyKeyListener(JComponent source) {
			this.source = source;
		}
		
		@Override
		public void keyPressed(KeyEvent e) {
			if(e.getKeyCode()==86 && (e.getModifiersEx()&KeyEvent.CTRL_DOWN_MASK)>0) {
				String clipboard = EasyClipboard.getClipboard();
				if(clipboard==null) {
					JExceptionDialog.showError(ExcelTable.this, "The Clipboard is empty");
					return; 
				}
				if(clipboard.indexOf('\n')>=0 || clipboard.indexOf('\t')>=0) {
					editingStopped(new ChangeEvent(this));
					try {
						pasteSelection();
					} catch (Exception ex) {
						ex.printStackTrace();
						JExceptionDialog.showError(ExcelTable.this, "Pasting returned some errors:\n"+ex.getMessage());
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
	
	private void addCellEditorListeners(final JComponent source) {
		if(source==null) return;
	
		//make sure we didn't add it already
		for(KeyListener kl: source.getKeyListeners()) {
			if(kl instanceof ExcelTable.MyKeyListener) return;
		}
		
		//Add the listener
		source.addKeyListener(new MyKeyListener(source));
		if(source instanceof JComboBox) {
			source.setBorder(null);
		} else {
			source.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		}
		

		source.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "next");
		source.getActionMap().put("next", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goNext();
			}
		});

	}

	@Override
	public void editingStopped(ChangeEvent e) {
		int col = getSelectedColumn();
		int row = getSelectedRow();
		
		super.editingStopped(e);
		if(row>=0 && col>=0) {
			if(getSelectedColumn()!=col || getSelectedRow()!=row) {
				setRowSelectionInterval(row, row);
				setColumnSelectionInterval(col, col);
			}
		} else if(lastEditingRow>=0 && lastEditingCol>=0 && lastEditingRow<getRowCount() && lastEditingCol<getColumnCount()) {
			setRowSelectionInterval(lastEditingRow, lastEditingRow);
			setColumnSelectionInterval(lastEditingCol, lastEditingCol);
		}
		
		
	}
	
	/**
	 * BooleanCellEditor
	 * @author freyssj
	 *
	 */
	public class BooleanCellEditor extends AbstractCellEditor implements TableCellEditor {
		private JCheckBox checkbox = new JCheckBox();
		
		public BooleanCellEditor() {
			checkbox.setOpaque(true);
			checkbox.setBackground(Color.red);
			checkbox.addItemListener(new ItemListener() {				
				@Override
				public void itemStateChanged(ItemEvent e) {
					editingStopped(new ChangeEvent(BooleanCellEditor.this));
					repaint();
				}
			});			
			checkbox.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		}
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			checkbox.setOpaque(false);
			checkbox.setSelected(value!=null && (Boolean)value);
			checkbox.setHorizontalAlignment(SwingConstants.CENTER);
			return checkbox;
		}

		@Override
		public Object getCellEditorValue() {
			return checkbox.isSelected();
		}				
		
	}	

	protected void addRow() {
		ROW r = getModel().createRecord();
		if(r!=null && canAddRow) {
			getModel().getRows().add(r);
			getModel().fireTableDataChanged();
		}
	}
	public void goRight() {
		editingStopped(new ChangeEvent(this));
		int row = getSelectedRow();
		int col = getSelectedColumn();
		if(row<0 || col<0) return;

		if(col<getColumnCount()-1) {
			setColumnSelectionInterval(col+1, col+1);
		} else {
			if(row>=getRowCount()-1) {
				addRow();
			}
			setColumnSelectionInterval(0, 0);					
			if(row+1<getRowCount()) setRowSelectionInterval(row+1, row+1);					
		}		
	}

	public void goLeft() {
		editingStopped(new ChangeEvent(this));
		int row = getSelectedRow();
		int col = getSelectedColumn();
		if(row<0 || col<0) return;

		if(col>0) {
			setColumnSelectionInterval(col-1, col-1);
		} else if(row>0) {
			setColumnSelectionInterval(getColumnCount()-1, getColumnCount()-1);					
			setRowSelectionInterval(row-1, row-1);					
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
			addRow();
		}
		if(row<getRowCount()-1) {
			setColumnSelectionInterval(col, col);
			setRowSelectionInterval(row+1, row+1);
		}
	}
	public void goNext() {
		if(goNextOnEnter) {
			goNextEditable();
		} else if(getSelectedRow()<getRowCount()-1) {
			goDown();
		}
	}
	
	
	public void goNextEditable() {
		editingStopped(null);
		int row = getSelectedRow();
		int col = getSelectedColumn();
		if(row<0 || col<0) return;
		for (int i = col+1; i < getColumnCount(); i++) {
			if(getModel().isCellEditable(row, convertColumnIndexToModel(i))) {
				setColumnSelectionInterval(i, i);
				setRowSelectionInterval(row, row);
				return;
			}
		}
		if(row>=getRowCount()-1) {
			addRow();
		}
		if(row<getRowCount()-1) {
			for (int i = 0; i < getColumnCount(); i++) {
				if(getModel().isCellEditable(row, convertColumnIndexToModel(i))) {
					setColumnSelectionInterval(i, i);
					setRowSelectionInterval(row+1, row+1);
					return;
				}
			}
		}
	}


}
