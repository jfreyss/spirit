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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

public abstract class AbstractExtendTable<ROW> extends JTable implements IExportable {

	public static enum BorderStrategy {
		NO_BORDER,
		ALL_BORDER,
		WHEN_DIFFERENT_VALUE
	}
	
	public static enum HeaderClickingPolicy {
		IGNORE,
		SORT,
		SELECT,
		POPUP
	}
		
	private boolean DEBUG = System.getProperty("debug", "false").equals("true"); 
	private boolean highlightSimilarCells = false;
	private boolean highlightRows = false;
	private BorderStrategy borderStrategy = BorderStrategy.ALL_BORDER;
	private Integer currentSortColumn = null; 
	private int sortKey = 0; 
	private boolean useSmartWidth = true;
	private boolean useSmartHeight = true;
	private boolean useAlphaForMergedCells = false;
	private boolean tableChanged = true;

	private HeaderClickingPolicy headerClickingPolicy = HeaderClickingPolicy.POPUP;
	private final FastHeaderRenderer<ROW> renderer = new FastHeaderRenderer<>();
	
	private class MyTableModelListener implements TableModelListener {			
		@Override
		public void tableChanged(TableModelEvent e) {
			currentSortColumn = null;
			valuesCacheMap.clear();
			tableChanged = true;
			
			//Check if the treeview is valid
			boolean enable = getModel().getTreeColumn()!=null;
			getModel().setTreeViewEnabled(enable);
			
			for (Column<ROW,?> col : getModel().getAllColumns()) {
				col.setTable(AbstractExtendTable.this);
			}
		}
	}

	public AbstractExtendTable() {
		this(new ExtendTableModel<ROW>());
	}
	
	/**
	 * Constructor
	 * @param model
	 */
	public AbstractExtendTable(ExtendTableModel<ROW> model) {
		super(model);
		setRowHeight(FastFont.getDefaultFontSize()*2+1);
		
		
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				if(highlightSimilarCells) {
					repaint();				
				}
			}
		});
		getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int[] selCols = getSelectedColumns();
				if(selCols.length==1 && getModel().getColumn(convertColumnIndexToModel(selCols[0]))==getModel().COLUMN_ROWNO) {
					setColumnSelectionInterval(0, getModel().getColumns().size()-1);
				} else if(highlightSimilarCells) {
					//OK				
				}
				repaint();				
			}
		});
		
		
		addAncestorListener(new AncestorListener() {			
			@Override
			public void ancestorRemoved(AncestorEvent arg0) {}			
			@Override
			public void ancestorMoved(AncestorEvent arg0) {}			
			@Override
			public void ancestorAdded(AncestorEvent arg0) {
				//Update the mousewheel scroll if enabled
				if(AbstractExtendTable.this.getParent() instanceof JScrollPane) {
					((JScrollPane) AbstractExtendTable.this.getParent()).getVerticalScrollBar().setUnitIncrement(22);					
				} else if(AbstractExtendTable.this.getParent() instanceof JViewport && ((JViewport) AbstractExtendTable.this.getParent()).getParent() instanceof JScrollPane) {
					((JScrollPane)((JViewport) AbstractExtendTable.this.getParent()).getParent()).getVerticalScrollBar().setUnitIncrement(22);										
				}
			}
		});
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				resetPreferredColumnWidth();
			}									
		});
		setHeaderRenderers();
		
		setShowGrid(false);
		setGridColor(new Color(255,255,255,0));
	}

	public HeaderClickingPolicy getHeaderClickingPolicy() {
		return headerClickingPolicy;
	}
	
	public void setHeaderClickingPolicy(HeaderClickingPolicy headerClickingPolicy) {
		this.headerClickingPolicy = headerClickingPolicy;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ExtendTableModel<ROW> getModel() {
		return (ExtendTableModel<ROW>) super.getModel();
	}
		
	protected void fitRowHeight(int viewRow) {
		int maxHeight = getRowHeight();		
		for (int col: selectIndixes(getModel().getColumnCount(), 100)) {
			Column<ROW, ?> column = getModel().getColumn(convertColumnIndexToModel(col));
			if(column==null || !column.isMultiline()) continue;
			Component c = getCellRenderer(viewRow, col).getTableCellRendererComponent(this, getValueAt(viewRow, col), true, true, viewRow, col);
			Dimension dim = c.getPreferredSize();	
			int h = Math.max(getRowHeight(), (int) dim.getHeight());
			maxHeight = Math.max( h, maxHeight);
		}
		
		if(maxHeight>0) {
			setRowHeight(viewRow, maxHeight);
		}
	}
		
	/**
	 * Display the tooltip of the renderered component
	 */
	@Override
	public String getToolTipText(MouseEvent e) {
		Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);
        if(rowIndex<0 || colIndex<0) return null;
        
        
        //First check if the column defines a tooltip
        Column<ROW, ?> column = getModel().getColumn(convertColumnIndexToModel(colIndex));
        String tooltip = column.getToolTipText(getModel().getRow(rowIndex));
        if(tooltip!=null) return tooltip;
        
        //Then check if the rendered component defines a tooltip
        Component c = getCellRenderer(rowIndex, colIndex).getTableCellRendererComponent(this, getValueAt(rowIndex, colIndex), false, false, rowIndex, colIndex);
        if(c instanceof JComponent) {
        	tooltip = ((JComponent) c).getToolTipText();
        }
    	return tooltip;
	}
	
	protected void setHeaderRenderers() {
		for (int col = 0; col < getColumnModel().getColumnCount(); col++) {
			TableColumn column = getColumnModel().getColumn(col);
			if(column.getHeaderRenderer()!=renderer) {
				column.setHeaderRenderer(renderer);
			}
		}			
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void setModel(TableModel model) {
		if(!(model instanceof ExtendTableModel)) throw new IllegalArgumentException("Invalid Model: "+model);
		
		boolean hasChangeListener = false;
		for (TableModelListener l : ((ExtendTableModel<ROW>)model).getTableModelListeners()) {
			if(l instanceof AbstractExtendTable.MyTableModelListener) {
				hasChangeListener = true; break;
			}
		}
		if(!hasChangeListener) {
			model.addTableModelListener(new MyTableModelListener());
		}

		super.setModel(model);

		for (Column<ROW,?> col : getModel().getAllColumns()) {
			col.setTable(AbstractExtendTable.this);
		}
		
	}

	public void resetPreferredColumnWidth() {
		if(!tableChanged) return;
		
		boolean reset = true;

		if(reset) setHeaderRenderers();
		if(getRowCount()==0) return;
		
		
		getModel().reorderTree();
		int nColumns = Math.min(getModel().getColumnCount(), getColumnModel().getColumnCount());
		
		//Calculate the maxWidth: ie the size of the parent
		int maxWidth = -1;
		Component comp = getParent();
		while(maxWidth<=0 && comp!=null) {
			maxWidth = comp.getWidth()-25;
			comp = comp.getParent();
		}
		if(maxWidth<=0) {
			maxWidth = UIUtils.getMainFrame()!=null? UIUtils.getMainFrame().getWidth()-400: Toolkit.getDefaultToolkit().getScreenSize().width-400;
			System.err.println("Could not find the parent of " + getClass()+ " ( in "+(getParent()==null?"":getParent().getClass())+") - make sure you add it to the window before setting the rows: use default: "+maxWidth+ " / parent="+getParent()+" width="+getWidth());
		}
		
		
		Map<Integer, Integer> col2MinWidth = new HashMap<>();
		Map<Integer, Integer> col2PrefWidth = new HashMap<>();
		Map<Integer, Integer> col2MaxWidth = new HashMap<>();
		Map<Integer, Integer> wrappingCol2PrefWidth = new HashMap<>();
		int totalInWrapping = 0;
		
		//For each column, find the preferred width (without considering wrapping)		
		List<Integer> explore = selectIndixes(getRows().size(), getModel().getMaxRowsToExplore());
		int sumMinWidth = 0;
		int sumPrefWidth = 0; 
		int[] widths = new int[3];
		for (int col = 0; col < nColumns; col++) {
			
			if(sumMinWidth<maxWidth) {
				widths = getWidthsForColumn(col, explore);
			}
			if(widths[1]+10>=widths[2]) widths[1] = widths[2];
			//update the variables
			col2MinWidth.put(col, widths[0]); 
			col2PrefWidth.put(col, widths[1]);
			col2MaxWidth.put(col, widths[2]);
			
			sumMinWidth+=widths[0];
			sumPrefWidth+=widths[1];	
			Column<ROW, ?> co = getModel().getColumn(convertColumnIndexToModel(col));
			if(co.isAutoWrap()) {
				wrappingCol2PrefWidth.put(col, widths[1]);
				totalInWrapping+=widths[1];
			}
		}

		if(DEBUG) {
			for (int col = 0; col < nColumns; col++) {
				System.out.println(getClass().getName()+" Col."+col+"\t"+col2MinWidth.get(col)+"\t"+col2PrefWidth.get(col)+"\t"+col2MaxWidth.get(col)+"\t");
			}
			System.out.println(getClass().getName()+" Total\t"+sumMinWidth+"\t"+sumPrefWidth);
			System.out.println(getClass().getName()+" Max\t"+maxWidth);
		}

		//Adapt columns depending of the preferedSize
		if(reset && nColumns>0) {
			
			//Adjust col2PrefWidth to limit the width variability.
			//Ex: if the columns widths are 10,12,20,40,50,80,120,140, then we increase them to 20,20,20,50,50,80,140,140, so that different widths always differ by 20px or more   
			{
				List<Integer> allWidths = new ArrayList<>();
				List<Integer> acceptedWidths = new ArrayList<>();
				for (int col = 0; col < nColumns; col++) {
					allWidths.add(col2PrefWidth.get(col));
				}
				Collections.sort(allWidths);
				int lastWidth = allWidths.get(allWidths.size()-1);
				Map<Integer, Integer> oldWidth2newWidth = new HashMap<>();			
				acceptedWidths.add(lastWidth);		
				for (int i = allWidths.size()-1; i >=0; i--) {
					if(allWidths.get(i)<lastWidth-20) {
						lastWidth = allWidths.get(i);
						acceptedWidths.add(lastWidth);
					} 
					oldWidth2newWidth.put(allWidths.get(i), lastWidth);
				}
				for (int col = 0; col < nColumns; col++) {
					col2PrefWidth.put(col, oldWidth2newWidth.get(col2PrefWidth.get(col)));
				}
				//Recalculate width
				sumPrefWidth = 0; for (int col = 0; col < nColumns; col++) sumPrefWidth+=col2PrefWidth.get(col);
			}
//			System.out.println("AbstractExtendTable reset.1 sumPrefWidth=" + sumPrefWidth+" / "+maxWidth);
			
			
			if(sumPrefWidth>maxWidth && totalInWrapping>0) {
				//We must reduce the size of some columns by enabling wrapping 
				//maxWidth = totalInWrapping/nlines + (totalWidth-totalInWrapping)
			
				int nLines = 0;
				if(sumPrefWidth-totalInWrapping>=maxWidth) {
					nLines = 1; //Impossible to compress in one page
				} else {
					nLines = 1+totalInWrapping/(maxWidth - (sumPrefWidth-totalInWrapping));
					if(nLines>3) nLines = 3;
				}
				int newWidth = totalInWrapping/nLines + (sumPrefWidth-totalInWrapping);
				int moreAvailable = Math.max(0, maxWidth-newWidth-20);
				
				if(nLines>0) {
					for(int col: wrappingCol2PrefWidth.keySet()) {
						int minWidth = col2MinWidth.get(col);
						int prefWidth = wrappingCol2PrefWidth.get(col);
						int newPrefWidth = Math.max(minWidth, Math.min(prefWidth, prefWidth/nLines + moreAvailable/wrappingCol2PrefWidth.size()));
						if(Math.abs(newPrefWidth-prefWidth)>20) {
							col2PrefWidth.put(col, newPrefWidth);
						}
					}
				}
				//Recalculate width
				sumPrefWidth = 0; for (int col = 0; col < nColumns; col++) sumPrefWidth+=col2PrefWidth.get(col);
			}
//			System.out.println("AbstractExtendTable reset.2 sumPrefWidth=" + sumPrefWidth+" / "+maxWidth);
				
			if(sumPrefWidth<maxWidth) {
				//
				//If the widths are less than the max, increase the size of some columns
//				int left = maxWidth-sumPrefWidth;
//				int nToIncrease = 0;
//				for (int col = 0; col < nColumns; col++) {
//					if(col2MaxWidth.get(col)==null || col2MaxWidth.get(col)<=col2PrefWidth.get(col)) continue;
//					nToIncrease++;
//				}
//				for (int col = 0; left>0 && col < nColumns; col++) {
//					if(col2MaxWidth.get(col)==null || col2MaxWidth.get(col)<=col2PrefWidth.get(col)) continue;
//					int inc = Math.min(left/nToIncrease, col2MaxWidth.get(col)-col2PrefWidth.get(col));
//					
//					int newPrefWidth = col2PrefWidth.get(col) + inc;
//					left-=inc;
//					col2PrefWidth.put(col, newPrefWidth);
//					nToIncrease--;
//				}				
				int left = maxWidth-sumPrefWidth;
				double factor = 0;
				int total = maxWidth;
				for (int col = 0; col < nColumns; col++) {
					if(col2MaxWidth.get(col)==null || col2MaxWidth.get(col)<=col2PrefWidth.get(col)) {
						total-=col2PrefWidth.get(col);
					} else {
						factor += col2MaxWidth.get(col);
					}
				}
				if(total>0) {
					if(DEBUG) {
						System.out.println("Factor = " + factor + " / " + total + " = " + (factor/total));
					}
					for (int col = 0; left>0 && col < nColumns; col++) {
						if(col2MaxWidth.get(col)==null || col2MaxWidth.get(col)<=col2PrefWidth.get(col)) continue;					
						int newWidth = Math.min(col2PrefWidth.get(col)+left, Math.max(col2MaxWidth.get(col), (int)(factor * col2PrefWidth.get(col) / total)));
						left -= (newWidth-col2PrefWidth.get(col));
						col2PrefWidth.put(col, newWidth);
					}
				}
			} else if(sumPrefWidth>maxWidth) {
				//reduce prefWidth if sumPrefWidth>maxWidth
				int totalToReduce = sumPrefWidth-maxWidth;
				int reducable = 0;
				int n = 0;
				for (int col = 0; col < nColumns; col++) {
					int minWidth = col2MinWidth.get(col);
					int prefWidth = col2PrefWidth.get(col);
					if(minWidth>=0 && prefWidth>minWidth) {
						n++;
						reducable += prefWidth-minWidth;
					}
				}
				totalToReduce = Math.min(reducable, totalToReduce);
				if(totalToReduce>0 && n>0) {			
					for (int col = 0; n>0 && totalToReduce>0 && col < nColumns; col++) {
						int minWidth = col2MinWidth.get(col);
						int prefWidth = col2PrefWidth.get(col);
						if(minWidth>=0 && prefWidth>minWidth) {
							int toReduce = Math.min(prefWidth-minWidth, totalToReduce/n);
							col2PrefWidth.put(col, prefWidth-toReduce);
							totalToReduce -= toReduce;
							n--;
						}
					}
				}
			}
		}

		if(DEBUG) {
			for (int col = 0; col < nColumns; col++) {
				System.out.println(getClass().getName()+" Set Col."+col+"\t"+col2PrefWidth.get(col));
			}
			sumPrefWidth = 0; for (int col = 0; col < nColumns; col++) sumPrefWidth+=col2PrefWidth.get(col);
			System.out.println(getClass().getName()+" Total."+ sumPrefWidth+" / "+maxWidth);
		}

		//////////////////////////////////////////////////////////////
		//Update Column Width
		for (int col = 0; col < nColumns; col++) {
			TableColumn column = getColumnModel().getColumn(col);
			int prefWidth = col2PrefWidth.get(col);
			if(reset || column.getPreferredWidth()<prefWidth) {
				column.setPreferredWidth(prefWidth);
			}	
		}
		
	}
		
	
	private int[] getWidthsForColumn(int col, Collection<Integer> explore) {
//		TableColumn column = getColumnModel().getColumn(col);
		Column<ROW, ?> co = getModel().getColumn(convertColumnIndexToModel(col));
		int colMinWidth;
		int colPrefWidth;
		int colMaxWidth;
		
		boolean isEditable = false;
		if(co.getMaxWidth()<=co.getMinWidth()+5) {
			colMinWidth = co.getMinWidth();
			colPrefWidth = co.getMaxWidth();
			colMaxWidth = co.getMaxWidth();
		} else {
			colMinWidth = co.getMinWidth();
			colPrefWidth = co.getMinWidth();
			colMaxWidth = co.getMinWidth();
			
			//Check Header width
//			if(column.getHeaderRenderer()!=null) {
//				Component c = column.getHeaderRenderer().getTableCellRendererComponent(this, column.getHeaderValue(), true, true, -1, col);
//				int cellPrefWidth = c.getPreferredSize().width;
//				colMaxWidth = Math.max(colMaxWidth, cellPrefWidth);
//			}

			//Check Data width
			int colAvgWidth = 0;
			int count = 0;

			setDefaultRenderer(Object.class, new ExtendTableCellRenderer<ROW>(this));

			for(int row: explore) {
				TableCellRenderer renderer = getCellRenderer(row, col);
				assert renderer!=null: "renderer for "+row+"x"+col+" is null: "+getValueAt(row, col);
				Component c = renderer.getTableCellRendererComponent(this, getValueAt(row, col), true, true, row, col);
				if(co.isAutoWrap() && (c instanceof JLabelNoRepaint)) {
					((JLabelNoRepaint)c).setWrappingWidth(-1); //disable wrapping to estimate the size without wrapping
				}
				int cellMinWidth = c.getMinimumSize().width;
				int cellPrefWidth = c.getPreferredSize().width;
				if(cellPrefWidth>co.getMaxWidth()) cellPrefWidth = co.getMaxWidth();
				if(isEditable() && co.isEditable(getRows().get(row))) {
					isEditable = true;
					cellPrefWidth = Math.max(60, cellPrefWidth);
				}				
				if(cellPrefWidth<cellMinWidth) cellPrefWidth = cellMinWidth;
				
				colMinWidth = Math.max(colMinWidth, cellMinWidth);
				colAvgWidth += cellPrefWidth;
				colPrefWidth = Math.max(colPrefWidth, cellPrefWidth);
				colMaxWidth  = Math.max(colMaxWidth, cellPrefWidth);
				count++;
			}
			if(count>0) {
				colAvgWidth /= count;
				colPrefWidth = colAvgWidth + (colPrefWidth-colAvgWidth)/5; 
			} else {
				colPrefWidth = (colMinWidth + colPrefWidth)/2;
			}
			if(DEBUG) {
				System.out.println(col+" colMinWidth="+colMinWidth+" colPrefWidth=" + colPrefWidth+ " colAvgWidth="+colAvgWidth+" colMaxWidth="+colMaxWidth+" max="+co.getMaxWidth());
			}
		}
		
		int colPrefWidth2 = Math.max(colMinWidth, colPrefWidth);
		int colMaxWidth2 = Math.min(co.getMaxWidth(), Math.max(colMaxWidth, isEditable? colPrefWidth*2 : colPrefWidth));

		
		return new int[] {colMinWidth, colPrefWidth2, colMaxWidth2};
	}

	/**
	 * MenuItem for adding columns
	 * @author freyssj
	 *
	 */
	protected class ColumnCheckbox extends JCheckBox {
				
		public ColumnCheckbox(final Column<ROW, ?> column) {			
			super("");
			assert column!=null;
			assert column.getName()!=null;

			String name = column.getName();
			name = name.replace('\n', '.').replaceAll("^\\.+", "");

			setText(name);				
				
			
			final ExtendTableModel<ROW> model = AbstractExtendTable.this.getModel();
			setSelected(model.getColumns().contains(column));
			
			
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					boolean selected = isSelected();
					if(column.isHideable() && model.getAllColumns().contains(column)) {
						model.showHideable(column, selected);
					} else {					
						List<Column<ROW, ?>> list = new ArrayList<>();
						list.add(column);
						if(selected) {
							model.addColumns(list, true);
						} else {
							model.removeColumns(list);
						}
					}
					resetPreferredColumnWidth();
				}
			});
		}		
	}
	protected class TreeViewCheckBox extends JCheckBoxMenuItem {
		public TreeViewCheckBox(final boolean selected) {
			super("Show Hierarchy", selected);
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					AbstractExtendTable.this.getModel().setTreeViewActive(isSelected());
				}
			});
		}
	}
	
	@Override
	public Dimension getPreferredScrollableViewportSize() {
	    Dimension size = super.getPreferredScrollableViewportSize();
	    return new Dimension(Math.min(getPreferredSize().width, size.width), size.height);
	}
	
	public List<ROW> getSelection() {
		int[] sel = getSelectedRows();
		List<ROW> res = new ArrayList<ROW>();
		for (int i : sel) {
			if(i<getModel().getRows().size()) {
				res.add(getModel().getRow(i));
			}
		}
		return res;
	}
	
	
	public void setSelection(Collection<ROW> selection) {		
		//Check if we need to change the selection
		List<ROW> currentSel = getSelection();
		if((selection==null && currentSel.size()==0) || (selection!=null && currentSel.size()==selection.size() && currentSel.containsAll(selection))) return;
		
		//Update the selection
		getSelectionModel().setValueIsAdjusting(true);
		try {
			getSelectionModel().clearSelection();
			int lastR = -1;
			if(selection!=null) {
				for (ROW b : selection) {
					if(b==null) continue;
					int r = getModel().getRows().indexOf(b);
					if(r>=0) {
						try {
							getSelectionModel().addSelectionInterval(r, r);
							lastR = r;
						} catch (Exception e) {
							//e.printStackTrace();
						}
					} else {
						System.err.println("ExtendTable.setSelection() could not select "+b);
					}
				}
			}
			if(lastR>=0) {
				scrollRectToVisible(getCellRect(lastR, 0, true));
				if(getModel().getTreeColumn()!=null) {
					int selCol = getModel().getColumns().indexOf(getModel().getTreeColumn());
					selCol = convertColumnIndexToView(selCol);
					setColumnSelectionInterval(selCol, selCol);
				} else if(getColumnCount()>0) {
					setColumnSelectionInterval(0, getColumnCount()-1);
				}
			}
		} finally {
			getSelectionModel().setValueIsAdjusting(false);
			repaint();
		}
	}

	public void scrollToSelection() {
		ROW row = getSelection().size()>0? getSelection().get(0): null;
		if(row!=null) scrollTo(row); 
	}
	public void scrollTo(ROW sel) {
		int r = getModel().getRows().indexOf(sel);
		if(r>=0) {
			scrollRectToVisible(getCellRect(Math.max(0, r-4), 0, true));
			scrollRectToVisible(getCellRect(Math.min(getRows().size()-1, r+4), 0, true));
		}
	}
	
	public List<ROW> getRows() {
		return getModel().getRows();
	}
	
	/**
	 * Set Rows and fire events (tableChanged, and resetcolumnWidth)
	 * 
	 * @param data (null to empty the complete model)
	 */
	public void setRows(final List<ROW> data) {
		getModel().setRows(data);
		resetPreferredColumnWidth();
	}
	
	public void clear() {
		setRows(null);
		getModel().clear();
	}

	/**
	 * Get the sort column
	 * 1-> sorted on the model.getColumns().get(0) columns
	 * -1-> reverse sorted on the model.getColumns().get(0) columns
	 * @return
	 */
	public Integer getCurrentSortColumn() {
		return currentSortColumn;
	}
	
	
	protected final Map<Integer, Object> valuesCacheMap = new HashMap<Integer, Object>();
	
	@Override
	public Object getValueAt(int row, int column) {
		
		ROW rowObject = getModel().getRow(row);
		if(rowObject==null) return null;

		
		assert row>=0 && row<getRowCount();
		assert column>=0 && column<getColumnCount();
		int modelCol = convertColumnIndexToModel(column);

		try {
			Column<ROW,?> colObject = getModel().getColumn(modelCol);
			return colObject.getValue(rowObject, row);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * @param highlightSimilarCells the highlightSimilarCells to set
	 */
	public void setHighlightSimilarCells(boolean highlightSimilarCells) {
		this.highlightSimilarCells = highlightSimilarCells;
	}
	/**
	 * @return the highlightSimilarCells
	 */
	public boolean isHighlightSimilarCells() {
		return highlightSimilarCells;
	}


	public BorderStrategy getBorderStrategy() {
		return borderStrategy;
	}
	public void setBorderStrategy(BorderStrategy borderStrategy) {
		this.borderStrategy = borderStrategy;
	}

	/**
	 * @param highlightRows the highlightRows to set
	 */
	public void setHighlightRows(boolean highlightRows) {
		this.highlightRows = highlightRows;
	}

	/**
	 * @return the highlightRows
	 */
	public boolean isHighlightRows() {
		return highlightRows;
	}
	
	
	public void sortBy(final int columnIndex) {
		sortBy(getModel().getColumns().get(columnIndex));		
	}
		
	public void sortBy(final Column<ROW, ?> col) {		
		sortBy(col, 0, new Comparator<ROW>() {
			@Override
			public int compare(ROW o1, ROW o2) {
				return CompareUtils.compare(col.getValue(o1), col.getValue(o2));
			}
		});		
	}
	
	/**
	 * Sort the table using the values and the order specified by the column and the given comparator to compare the values
	 * @param column
	 * @param comparator
	 * @param sortKey (if there are several sort for the same column, use different sortkey)
	 */
	public void sortBy(final Column<ROW, ?> col,  int sortKey, final Comparator<ROW> comparator) {
		if(comparator==null) return;
		if(col==null) return;

		int columnIndex = getModel().getColumns().indexOf(col);
		if(columnIndex<0) return;
		if(this instanceof ExcelTable) ((ExcelTable<ROW>) this).getUndoManager().discardAllEdits();
		
		final int direction = currentSortColumn!=null && currentSortColumn==(columnIndex+1) && sortKey==this.sortKey? -1 : 1;
		
		if(this instanceof ExcelTable) ((ExcelTable<ROW>) this).getUndoManager().discardAllEdits();
		
		//Sort using the given direction (note: equal element will stay in the same order)
		Collections.sort(getModel().getRows(), new Comparator<ROW>() {
			@Override
			public int compare(ROW r1, ROW r2) {
				int cmp = comparator.compare(r1, r2);
				return direction * cmp;				
			}
		});

		
		getModel().reorderTree();
		getModel().fireTableDataChanged();
		this.currentSortColumn = (columnIndex+1) * direction;
		this.sortKey = sortKey;
		getTableHeader().repaint();
	}

	protected void postProcess(ROW row, int rowNo, Object value, JComponent c) {
		
	}
	
	public boolean isEditable() {
		return this instanceof ExcelTable;
	}
	
	/**
	 * Can be overridden to decide when 2 cells can be merged.
	 * By default, we merge if all cells within the same category are equals
	 * @param col
	 * @param row1 a valid rowNo
	 * @param row2 a valid rowNo
	 * @return
	 */
	protected boolean shouldMerge(int viewCol, int row1, int row2) {	
		//Border if one or more values differs among the same category of the subsequent columns
		try{
			
			if(isCellEditable(row1, viewCol)!=isCellEditable(row2, viewCol)) return false;

			Column<ROW, ?> c2 = getModel().getColumn(convertColumnIndexToModel(viewCol));
			if(!c2.shouldMerge(getRows().get(row1), getRows().get(row2))) return false;
			return true;
		} catch(Throwable e) {
			e.printStackTrace();
			return false;
		}
	}

	
	

	public boolean isUseSmartWidth() {
		return useSmartWidth;
	}

	public void setUseSmartWidth(boolean useSmartWidth) {
		this.useSmartWidth = useSmartWidth;
	}

	public boolean isUseSmartHeight() {
		return useSmartHeight;
	}

	public void setUseSmartHeight(boolean useSmartHeight) {
		this.useSmartHeight = useSmartHeight;
	}

	public boolean isUseAlphaForMergedCells() {
		return useAlphaForMergedCells;
	}

	public void setUseAlphaForMergedCells(boolean useAlphaForMergedCells) {
		this.useAlphaForMergedCells = useAlphaForMergedCells;
	}

	
	/**
	 * Gets a double array with the values of the table
	 * If the values contain TABS, the columns are split accordingly
	 */
	@Override
	public String[][] getTabDelimitedTable() {
		int rows = getRowCount();
		int cols = getColumnCount();
		
		if(rows==0 || cols==0) return new String[0][0];
		
		String[][] res = new String[rows+1][];		
		//First the headers
		List<String> headers = new ArrayList<String>();
		for (int col = 0; col < cols; col++) {		
			int modelCol = convertColumnIndexToModel(col);
			Column<ROW, ?> column = getModel().getColumn(modelCol);

			headers.add(formatForTab(column.getName()));
		}
		res[0] = headers.toArray(new String[headers.size()]);
		
		//Then the rows
		for (int row = 0; row < rows; row++) {
			List<String> data = new ArrayList<String>();
			for (int col = 0; col < cols; col++) {
				int modelCol = convertColumnIndexToModel(col);
				Column<ROW, ?> column = getModel().getColumn(modelCol);

				data.add(formatForTab(column.getValue(getModel().getRow(row), row)));
			}
			res[row+1] = data.toArray(new String[data.size()]);
		}
		
		//PostProcessing: split cells with tabulation. ie columns containing data with tabs will be splitted
		int[] col2nNewCols = new int[cols];
		for (int col = 0; col < cols; col++) {
			for (int row = 0; row < res.length; row++) {
				String[] split = res[row][col].split("\t");
				int n = split.length;
				while(n>0 && split[n-1].length()==0) n--;
				col2nNewCols[col] = Math.max(col2nNewCols[col], n);
			}			
		}
		int newCols = 0;
		for (int col = 0; col < cols; col++) {
			newCols+=col2nNewCols[col];
		}
		
		String[][] newRes = new String[res.length][newCols];		
		for (int col = 0, newCol = 0; col < cols; col++) {
			for (int row = 0; row < res.length; row++) {
				String[] split = res[row][col].split("\t");
				for (int i = 0; i < col2nNewCols[col]; i++) {
					newRes[row][newCol+i] = i<split.length? split[i]: "";
				}
			}
			newCol+=col2nNewCols[col];
		}
		
		//Remove the rowNo
		if(newRes[0][0].equals("#")) {
			for (int row = 0; row < res.length; row++) {
				newRes[row] = Arrays.copyOfRange(newRes[row], 1, newRes[row].length);
			}
		}
		return newRes;
	}
	
	public static String formatForTab(Object s) {
		if(s==null) return "";
		if(s instanceof Date) {
			return FormatterUtils.formatDateTime((Date)s);
		} else if(s instanceof Boolean) {
			return s==Boolean.TRUE?"X":"";
		}
		
		String str = s.toString();
		if(str.length()>3 && str.charAt(0)=='<' && str.charAt(2)=='>') str = str.substring(3);
		return str;
	}

	/**
	 * Function that can be derived by sublasses to pupulate the header popupMenu.
	 * First we should show the sorting columns, the the columns specific infos, then the table infos if needed  
	 * @param popupMenu
	 * @param column
	 */
	protected void populateHeaderPopup(JPopupMenu popupMenu, Column<ROW, ?> column) {}


	
	/**
	 * Extract n indexes from the [0,N-1] using a progressive increment
	 * @param biosamples
	 * @param size
	 * @return
	 */
	public static List<Integer> selectIndixes(int N, int size) {
		List<Integer> res = new ArrayList<>();
		//We choose alpha such as sum(1+(alpha*i), i, 0, n-1)) = list.size
		int alpha = -2*(size-N-1)/(size*(size-1));
		int index = 0;
		for(int i=0; i<size; i++) {
			if(index>=N) return res;
			res.add(index);
			index += 1 + (int) (i * alpha);
		}
		return res;		
	}
	
}
