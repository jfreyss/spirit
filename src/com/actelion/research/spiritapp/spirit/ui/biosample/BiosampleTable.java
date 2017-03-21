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

package com.actelion.research.spiritapp.spirit.ui.biosample;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableColumn;

import com.actelion.research.spiritapp.spirit.ui.biosample.column.ContainerAmountColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.ContainerFullColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.ContainerIdColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.ContainerLocationPosColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.ContainerTypeColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.edit.EditBiosampleTableModel;
import com.actelion.research.spiritapp.spirit.ui.biosample.linker.AbstractLinkerColumn;
import com.actelion.research.spiritapp.spirit.ui.biosample.linker.LinkerColumnFactory;
import com.actelion.research.spiritapp.spirit.ui.biosample.linker.SampleIdColumn;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritapp.spirit.ui.lf.SpiritExtendTable;
import com.actelion.research.spiritcore.business.biosample.BarcodeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerMethod;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.pivot.PivotRow;
import com.actelion.research.spiritcore.util.ListHashMap;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;
import com.actelion.research.util.ui.exceltable.FastHeaderRenderer;

/**
 * Table for biosample
 * @author freyssj
 *
 */
public class BiosampleTable extends SpiritExtendTable<Biosample> {
	

	/**
	 * smartColumns allow the reinitialization of the columns after each call to setRows.
	 * If not set, columns have to be set programatically
	 */
	private boolean smartColumns = true;
	
	
	public BiosampleTable() {
		this(new BiosampleTableModel());
	}
	
	public BiosampleTable(BiosampleTableModel model) {
		super(model);
		
		setBorderStrategy(BorderStrategy.WHEN_DIFFERENT_VALUE);
		setCellSelectionEnabled(true);		
		setFillsViewportHeight(false);
		setHeaderClickingPolicy(HeaderClickingPolicy.POPUP);
		
		getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				final int colNo = columnAtPoint(e.getPoint());
				if(e.getClickCount()>=2 && colNo>=0) {
					Column<Biosample, ?> col = getModel().getColumn(convertColumnIndexToModel(colNo));
					if(col instanceof ContainerFullColumn) {
						BiosampleTable.expandContainerLocation(BiosampleTable.this, true);
					} else if(col instanceof ContainerTypeColumn || col instanceof ContainerIdColumn || col instanceof ContainerLocationPosColumn) {
						BiosampleTable.expandContainerLocation(BiosampleTable.this, false);
					} else if((col instanceof AbstractLinkerColumn) && ((AbstractLinkerColumn<?>) col).getBiotype()!=null) {
						BiosampleTable.expandBiotype(BiosampleTable.this, ((AbstractLinkerColumn<?>) col).getBiotype().getName(), null);
					}
				}
			}
		});
		
	}

	@Override
	protected void populateHeaderPopup(JPopupMenu popupMenu, Column<Biosample, ?> column) {
		BiosampleTable.populateExpandPopup(this, popupMenu);
	}
	
	@Override
	public BiosampleTableModel getModel() {
		return (BiosampleTableModel) super.getModel();		
	}
	
	/**
	 * Sets the rows, init the columns (if smartColumns==true) and reset the column widths 
	 */
	@Override
	public void setRows(final List<Biosample> data) {	
		if(data!=null && !data.isEmpty()) {
			getModel().setRows(new ArrayList<>(data));
		} else {
			getModel().setRows(new ArrayList<>());
		}
		if(smartColumns) {
			//Smart column selections
			initColumns();
		}
		getModel().fireTableDataChanged();
		resetPreferredColumnWidth();
	}
	
	/**
	 * Inits columns based on the data.
	 * - Container is only shown if one column has a container
	 * - Parent is only shown if one sample has a parent not contained in the model
	 * - If the data model is based on one biotype, each metadata will be shown in its own column
	 * - Otherwise, all metadata will be shown in one column
	 */
	public void initColumns() {
		//Memorize the orders of columns
		Map<Column<Biosample,?>, Integer> formerColumnOrder = new HashMap<>();
		for(int i=0; i<getModel().getColumns().size(); i++) {
			formerColumnOrder.put(getModel().getColumn(convertColumnIndexToModel(i)), i);
		}
		
		getModel().initColumns();

		//Reorganize columns as it was previously (bubble sort)
		List<Column<Biosample, ?>> columns = new ArrayList<>(this.getModel().getColumns());
		boolean wasChanged = false; 
		boolean swapped; 
		do {
			swapped = false; 
			for (int i = 1; i < formerColumnOrder.size() && i < columns.size(); i++) {
				Integer index1 = formerColumnOrder.get(columns.get(convertColumnIndexToModel(i-1)));
				Integer index2 = formerColumnOrder.get(columns.get(convertColumnIndexToModel(i)));
				if(index1!=null && index2!=null && index1>index2) {
					columns.add(i-1, columns.remove(i));
					swapped = true;
					wasChanged = true;
				}
			}
		} while(swapped);
		if(wasChanged) {
			getModel().setColumns(columns);
		}
		
	}
	
	public boolean isSmartColumns() {
		return smartColumns;
	}
	
	/**
	 * By defaults, the columns are set as smart, ie columns adapt themselvers to the data
	 * If this property is set to false, the columns have to be set manually always (or through a call to getModel().initColumns() after a call to setRows(...))
	 * @param smartColumns
	 */
	public void setSmartColumns(boolean smartColumns) {
		this.smartColumns = smartColumns;
	}

	
	
	public Collection<Biosample> getHighlightedSamples() {
		int[] selRows = getSelectedRows();
		int[] selCols = getSelectedColumns();
		
		Set<Biosample> res = new LinkedHashSet<>();
		for (int c = 0; c < selCols.length; c++) {
			for (int r = 0; r < selRows.length; r++) {
				Biosample row = getModel().getRows().get(selRows[r]);
				Column<Biosample, ?> col = getModel().getColumn(convertColumnIndexToModel(selCols[c]));
				if(col.getColumnClass()==Biosample.class) {
					Biosample b = (Biosample) col.getValue(row);
					if(b!=null) res.add(b);
				} else if(col instanceof ContainerFullColumn) {
					if(row.getContainer()!=null) {
						res.addAll(row.getContainer().getBiosamples());
					}
				} else if(col instanceof AbstractLinkerColumn) {
					BiosampleLinker linker = ((AbstractLinkerColumn<?>)col).getLinker();
					Biosample linked = ((AbstractLinkerColumn<?>) col).getLinker().getLinked(row);
					if(linked!=null && linked.getContainer()!=null && ((col instanceof ContainerIdColumn) || (col instanceof ContainerTypeColumn) || (col instanceof ContainerLocationPosColumn))) {
						res.addAll(linked.getContainer().getBiosamples());
					} else if(linked!=null && (col instanceof SampleIdColumn)) {
						res.add(linked);
					} else if(!linker.isLinked()) {
						res.add(row);
					}
				}				
			}
		}
		return res;
	}
	
	/**
	 * Override this method to adjust the columns after expanding
	 * @param obj
	 * @param expand
	 * @param maxDepth
	 * @param fireEvents
	 */
	@Override
	protected void expandRow(Biosample obj, boolean expand, int maxDepth, boolean fireEvents) {
		final int[] sRows = getSelectedRows();
		final int[] sCols = getSelectedColumns();
		
		super.expandRow(obj, expand, maxDepth, false);
		if(fireEvents) {
			if(smartColumns) {
				initColumns();
			}
			getModel().fireTableDataChanged();
			resetPreferredColumnWidth();
			
			if(sCols.length>0 && sCols[0]<getColumnCount()) setColumnSelectionInterval(sCols[0], sCols[0]);
			if(sRows.length>0 && sRows[0]<getRowCount()) setRowSelectionInterval(sRows[0], sRows[0]);
		}
		
	}

	
	public static void expandContainerLocation(AbstractExtendTable<Biosample> table, boolean expand) {
		//Initialize variables
		final ExtendTableModel<Biosample> model = table.getModel();
		final Biotype biotype = (model instanceof EditBiosampleTableModel)? ((EditBiosampleTableModel) model).getBiotype():  ((BiosampleTableModel) model).getBiotype();
		final Set<BiosampleLinker> linkers = getPresentLinkers(table);
		
		if(biotype!=null && biotype.isHideContainer()) return;
		
		final List<Biosample> toExplore = table.getRows().subList(0, Math.min(model.getMaxRowsToExplore(), table.getRows().size()));
		
		if(expand) {
			List<Column<Biosample, ?>> toAdd = new ArrayList<Column<Biosample, ?>>();
			if(table.isEditable()) {
				//Always add those 4 editable columns
				
				if(biotype==null || biotype.getContainerType()==null) toAdd.add(new ContainerTypeColumn());
				if(biotype==null || biotype.getContainerType()==null || biotype.getContainerType().getBarcodeType()!=BarcodeType.NOBARCODE) toAdd.add(new ContainerIdColumn());
				toAdd.add(new ContainerLocationPosColumn());
				if(biotype==null || biotype.getAmountUnit()!=null) toAdd.add(new ContainerAmountColumn(biotype));								
			} else {
				//Add columns depending of context (may not be editable)
				for(Biosample b: toExplore) if(linkers==null || b.getContainerType()!=null) {toAdd.add(new ContainerTypeColumn()); break;}
				for(Biosample b: toExplore) if(linkers==null || b.getContainerId()!=null) {toAdd.add(new ContainerIdColumn()); break;}
				for(Biosample b: toExplore) if(linkers==null || b.getLocation()!=null) {toAdd.add(new ContainerLocationPosColumn()); break;}
				for(Biosample b: toExplore) if(linkers==null || b.getAmount()!=null) {toAdd.add(new ContainerAmountColumn(biotype)); break;}
			}

			List<Column<Biosample, ?>> toRemove = new ArrayList<Column<Biosample, ?>>();
			for (Column<Biosample,?> col : model.getColumns()) {
				if(col instanceof ContainerFullColumn) {					
					toRemove.add(col);
				}			
			}

			table.getModel().removeColumns(toRemove);
			table.getModel().addColumns(toAdd, true);
			table.resetPreferredColumnWidth();
		} else {
			List<Column<Biosample, ?>> toAdd = new ArrayList<Column<Biosample, ?>>();
			List<Column<Biosample, ?>> toRemove = new ArrayList<Column<Biosample, ?>>();
			
			for (Column<Biosample,?> col : model.getColumns()) {
				if("Container".equals(col.getCategory())) {					
					toRemove.add(col);
				}			
			}
			toAdd.add(new ContainerFullColumn());
			
			table.getModel().removeColumns(toRemove);
			table.getModel().addColumns(toAdd, true);
			table.resetPreferredColumnWidth();
		}
	}
	
	public static void expandBiotype(AbstractExtendTable<Biosample> table, String biotype, Boolean expand) {
		final ListHashMap<String, BiosampleLinker> keys = getBiotypeToLinkerKeys(table);
		if(keys.get(biotype)==null) {
			System.err.println("BiosampleTable.expandBiotype(): cannot expand biotype: "+biotype);
			return;
		}
		//If expand is not specified, we try to guess what to do
		if(expand==null) {
			if(table.getModel() instanceof BiosampleTableModel) {
				int notPresent = 0;
				for(BiosampleLinker linker: keys.get(biotype)) {		
					if(linker.getBiotypeMetadata()!=null && linker.getBiotypeMetadata().isSecundary()) continue;
					if(!((BiosampleTableModel) table.getModel()).getLinkers().contains(linker)) {
						notPresent++; 
					}
				}
				expand = notPresent>0;
			} else {
				expand = true;
			}
		}
		
		if(expand) {
			List<Column<Biosample, ?>> toAdd = new ArrayList<Column<Biosample, ?>>();
			for(BiosampleLinker linker: keys.get(biotype)) {
				Column<Biosample, ?> column = LinkerColumnFactory.create(linker);
				if(column instanceof SampleIdColumn) continue;
				toAdd.add(column);
			}
			
			
			table.getModel().addColumns(toAdd, true);
		} else {
			List<Column<Biosample, ?>> toRemove = new ArrayList<Column<Biosample, ?>>();
			for(BiosampleLinker linker: keys.get(biotype)) {
				Column<Biosample, ?> column = LinkerColumnFactory.create(linker);
				if(column instanceof SampleIdColumn) continue;
				toRemove.add(column);
			}
			table.getModel().removeColumns(toRemove);
		}
		
		for(Column<Biosample, ?> col: table.getModel().getColumns()) {
			if((col instanceof SampleIdColumn) && ((SampleIdColumn)col).getBiotype()!=null && biotype.equals(((SampleIdColumn)col).getBiotype().getName())) {
				((SampleIdColumn)col).setDisplayName(!expand);
			}
		}
		table.resetPreferredColumnWidth();

	}
	
	public static ListHashMap<String, BiosampleLinker> getBiotypeToLinkerKeys(final AbstractExtendTable<Biosample> table) {
		//Initialize variables
		final ExtendTableModel<Biosample> model = table.getModel();
		final Set<BiosampleLinker> linkers;
		if(model instanceof EditBiosampleTableModel) {
			linkers = null; //never show the expand biotype in edit
		} else if(model instanceof BiosampleTableModel) {
			Biotype biotype = ((BiosampleTableModel) model).getBiotype();
			if(biotype==null) {
				linkers = ((BiosampleTableModel) model).getLinkers();				
			} else {				
				linkers = ((BiosampleTableModel) model).getLinkers(); //was null				
			}
		} else {
			throw new IllegalArgumentException("Invalid model: "+model);
		}
				
		
		//Go through each linkers and check what are the available fields that could be proposed to expand/collapse
		//Create a map of bioTypeString->list of linkers
		final ListHashMap<String, BiosampleLinker> keys = new ListHashMap<String, BiosampleLinker>();
		final Set<String> toExpand = new HashSet<String>();
		if(linkers!=null) {
			
			//Get a list of all Linkers
			final List<Biosample> toExplore = table.getRows().subList(0, Math.min(model.getMaxRowsToExplore(), table.getRows().size()));
			final Set<BiosampleLinker> allLinkers = new TreeSet<BiosampleLinker>(); 
			allLinkers.addAll(BiosampleLinker.getLinkers(toExplore, LinkerMethod.INDIRECT_LINKS));
			allLinkers.addAll(BiosampleLinker.getLinkers(toExplore, LinkerMethod.DIRECT_LINKS));

			for(BiosampleLinker linker: allLinkers) {
		
				if(!linker.isLinked() && (linker.getType()!=LinkerType.SAMPLENAME)) continue;
				if(linker.getType()==LinkerType.SAMPLEID) continue;
				
				String key = linker.getHierarchyBiotype()!=null? linker.getHierarchyBiotype().getName(): 
					linker.getAggregatedMetadata()!=null? linker.getAggregatedMetadata().getName():
					linker.getBiotypeForLabel()!=null? linker.getBiotypeForLabel().getName():
					null;
				if(key==null) continue;
				
				
				keys.add(key, linker);
				if(!linkers.contains(linker)) {
					toExpand.add(key);
				}
			}
		}
		return keys;
	}
	
	private static Set<BiosampleLinker> getPresentLinkers(final AbstractExtendTable<Biosample> table){
		final Set<BiosampleLinker> presentLinkers;
		final ExtendTableModel<Biosample> model = table.getModel();
		final Biotype biotype;
		if(model instanceof EditBiosampleTableModel) {
			biotype = ((EditBiosampleTableModel) model).getBiotype();
			presentLinkers = null; //never show the expand biotype for edittable
		} else if(model instanceof BiosampleTableModel) {
			biotype = ((BiosampleTableModel) model).getBiotype();
			presentLinkers = biotype==null? null: ((BiosampleTableModel) model).getLinkers(); //show the expand biotype when the table shows only one type
		} else {
			throw new IllegalArgumentException("Invalid model: "+model);
		}
		return presentLinkers;
		
	}
	/**
	 * Create an Expand / Collapse Menu
	 * 
	 * Parent Biotype -> Expand / Collapse
	 * Biotype -> Expand / Collapse
	 */
	public static void populateExpandPopup(final AbstractExtendTable<Biosample> table, JPopupMenu menu) {
		final ExtendTableModel<Biosample> model = table.getModel();

		//Initialize variables
		final Biotype biotype = (model instanceof EditBiosampleTableModel)? ((EditBiosampleTableModel) model).getBiotype():  ((BiosampleTableModel) model).getBiotype();
		final Set<BiosampleLinker> presentLinkers = getPresentLinkers(table);

		
		//Check presence of container columns
		boolean hasContainer = false;
		boolean hasFullContainer = false;
		for (Column<Biosample,?> col : model.getColumns()) {
			if((col instanceof ContainerFullColumn)) {
				hasContainer = true;
				hasFullContainer = true;
				break;
			} else if("Container".equals(col.getCategory())) {					
				hasContainer = true;
			}			
		}
		

		//Go through each linkers and check what are the available fields that could be proposed to expand/collapse
		//Create a map of bioTypeString->list of linkers
		
		final ListHashMap<String, BiosampleLinker> keys = new ListHashMap<>();
		final Set<String> toExpand = new HashSet<>();
		if(presentLinkers!=null) {
			
			//Get a list of all Linkers
			final List<Biosample> toExplore = MiscUtils.subList(table.getRows(), model.getMaxRowsToExplore());
			final Set<BiosampleLinker> allLinkers = new TreeSet<>(); 
			allLinkers.addAll(BiosampleLinker.getLinkers(toExplore, LinkerMethod.INDIRECT_LINKS));
			allLinkers.addAll(BiosampleLinker.getLinkers(toExplore, LinkerMethod.DIRECT_LINKS));
			for(BiosampleLinker linker: allLinkers) {
		
				if(/*!linker.isLinked() &&*/ linker.getType()!=LinkerType.SAMPLENAME) continue;
				
				String key = linker.getHierarchyBiotype()!=null? linker.getHierarchyBiotype().getName(): 
					linker.getAggregatedMetadata()!=null? linker.getAggregatedMetadata().getName():
					linker.getBiotypeForLabel()!=null? linker.getBiotypeForLabel().getName():
					null;
					
				if(key==null) continue;
				
				
				keys.add(key, linker);
				if(!presentLinkers.contains(linker)) {
					toExpand.add(key);
				}
			}
		}
		
		////////////////////////////////////////
		//Expand
		if(keys.size()>0 || (hasContainer && (biotype==null || !biotype.isHideContainer()))) {
			menu.add(new JSeparator());
			menu.add(new JCustomLabel("Expanded Columns", Font.BOLD));
			
			//Expand-Location menu
			if(hasContainer && (biotype==null || !biotype.isHideContainer())) {
				final JCheckBoxMenuItem m = new JCheckBoxMenuItem("Container", !hasFullContainer);
				m.addActionListener(new ActionListener() {					
					@Override
					public void actionPerformed(ActionEvent e) {
						expandContainerLocation(table, m.isSelected());
					}
				});
				menu.add(m);
			}
			
			
			//Expand-Biotype menu
			for (final String key : keys.keySet()) {
				final JCheckBoxMenuItem m = new JCheckBoxMenuItem(key, !toExpand.contains(key));
				m.addActionListener(new ActionListener() {					
					@Override
					public void actionPerformed(ActionEvent e) {
						expandBiotype(table, key, m.isSelected());
					}
				});
				menu.add(m);				
			}
		}		
	
	}

	
	private final FastHeaderRenderer<PivotRow> renderer = new FastHeaderRenderer<PivotRow>() {
		private Color color = UIManager.getColor("Panel.background");
		
		@SuppressWarnings("unchecked")
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JComponent comp = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			int col = convertColumnIndexToModel(column);
			comp.setOpaque(true);
			if(col<getModel().getColumns().size() && getModel().getColumns().get(col) instanceof SampleIdColumn && !((SampleIdColumn)getModel().getColumns().get(col)).getLinker().isLinked()) {
				comp.setFont(FastFont.BOLD);
				comp.setBackground(color);
			} else if(col<getModel().getColumns().size() && !(getModel().getColumns().get(col) instanceof SampleIdColumn) && getModel().getColumns().get(col) instanceof AbstractLinkerColumn && ((AbstractLinkerColumn<Biosample>)getModel().getColumns().get(col)).getLinker().isLinked()) {
				comp.setFont(FastFont.REGULAR);
				comp.setBackground(UIUtils.getDilutedColor(color, LF.BGCOLOR_LINKED));			
			} else {
				comp.setFont(FastFont.REGULAR);
				comp.setBackground(color);			
			}		
			return comp;
		}
	};
	
	@Override
	protected void setHeaderRenderers() {
		for (int col = 0; col < getColumnModel().getColumnCount(); col++) {
			TableColumn column = getColumnModel().getColumn(col);
			if(column.getHeaderRenderer()!=renderer) {
				column.setHeaderRenderer(renderer);
			}
		}			
	}
	
	@Override
	protected boolean shouldMerge(int viewCol, int row1, int row2) {
		
		
		return super.shouldMerge(viewCol, row1, row2);
	}
}
