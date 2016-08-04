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

package com.actelion.research.spiritapp.spirit.ui.biosample.column;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTableModel;
import com.actelion.research.spiritapp.spirit.ui.biosample.editor.ContainerIdCellEditor;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerLabel;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerLabel.ContainerDisplayMode;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.biosample.LocationFormat;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable;
import com.actelion.research.util.ui.exceltable.Column;

public class ContainerFullColumn extends Column<Biosample, String> {
	
	private static ContainerLabel containerLabel = new ContainerLabel(ContainerDisplayMode.FULL);
	
	public ContainerFullColumn() {		
		super("Container\n", String.class, 45);		
	}

	@Override
	public float getSortingKey() {return 2.0f;}
	
	@Override
	public String getValue(Biosample row) {		
		if(row.getBiotype()!=null && (row.getBiotype().getCategory()==BiotypeCategory.LIBRARY || row.getBiotype().isAbstract())) return null;
				
		StringBuilder sb = new StringBuilder();
		sb.append(row.getContainerType()==null?"": row.getContainerType().getName());
		sb.append("\t");
		sb.append(row.getContainerId()==null?"": row.getContainerId());
		sb.append("\t");
		sb.append(row.getAmount()==null?"": row.getAmount());
		sb.append("\t");
		sb.append(row.getLocation()==null?"": row.getLocationString(LocationFormat.FULL_POS, Spirit.getUser()));
		return sb.toString();
	}
	
	@Override
	public String getCopyValue(Biosample row, int rowNo) {
		return row.getContainerId()==null?"": row.getContainerId();
	}
	
	
	@Override
	public void setValue(Biosample row, String value) {
		if(value==null) {
			row.setContainer(null);
		} else {
			try {
				String[] split = value.split("\t",-1);
				if(split.length==1) {
					row.setContainerId(value);
				} else {
					if(split.length>0) row.setContainerType(ContainerType.get(split[0]));
					if(split.length>1) row.setContainerId(split[1]);
					if(split.length>2) row.setAmount(split[2].length()>0? Double.parseDouble(split[2]): null);
					if(split.length>3) {
						Location loc = DAOLocation.getCompatibleLocation(split[3], Spirit.getUser());
						row.setLocation(loc);
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	
	
	@Override
	public JComponent getCellComponent(AbstractExtendTable<Biosample> table, Biosample b, int rowNo, Object value) {
		containerLabel.setDisplayMode(ContainerDisplayMode.FULL);
		containerLabel.setBiosample(b);
		return containerLabel;
	}
	
	@Override
	public void postProcess(AbstractExtendTable<Biosample> table, Biosample row, int rowNo, Object value, JComponent comp) {		
		comp.setBackground(LF.BGCOLOR_LOCATION);
	}
	@Override
	public boolean isEditable(Biosample row) {
		return false;
//		return row.getContainerType()!=null && !row.getContainerType().isMultiple() && row.getContainerType().getBarcodeType()!=BarcodeType.NOBARCODE && row.getLocation()==null;
	}
	
	@Override
	public void paste(Biosample row, String value) throws Exception {
		setValue(row, value);
	}
	

	@Override
	public TableCellEditor getCellEditor(AbstractExtendTable<Biosample> table) {		
		return new ContainerIdCellEditor();
	}
	
	@Override
	public boolean isMultiline() {
		return true;
	}
	
	
	@Override
	public void populateHeaderPopup(final AbstractExtendTable<Biosample> table, JPopupMenu popupMenu) {
		Biotype type = table.getModel() instanceof BiosampleTableModel? ((BiosampleTableModel) table.getModel()).getType():null;
		
		popupMenu.add(new JSeparator());
		popupMenu.add(new JCustomLabel("Sort", Font.BOLD));
		if(type==null || !type.isHideContainer()) {
			popupMenu.add(new AbstractAction("Sort by ContainerId") {
				@Override
				public void actionPerformed(ActionEvent e) {
					table.sortBy(ContainerFullColumn.this, 1, new Comparator<Biosample>() {
						@Override
						public int compare(Biosample o1, Biosample o2) {
							String c1 = o1.getContainerId();
							String c2 = o2.getContainerId();
							int c = CompareUtils.compare(c1, c2);
							return c;
						}
					});
				}
			});
			popupMenu.add(new AbstractAction("Sort by BlocNo") {
				@Override
				public void actionPerformed(ActionEvent e) {
					table.sortBy(ContainerFullColumn.this, 1, new Comparator<Biosample>() {
						@Override
						public int compare(Biosample o1, Biosample o2) {
							Integer c1 = o1.getBlocNo();
							Integer c2 = o2.getBlocNo();
							int c = CompareUtils.compare(c1, c2);
							return c;
						}
					});
				}
			});
		}
		if(type==null || type.getAmountUnit()!=null) {
			popupMenu.add(new AbstractAction("Sort by Amount") {
				@Override
				public void actionPerformed(ActionEvent e) {
					table.sortBy(ContainerFullColumn.this, 2, new Comparator<Biosample>() {
						@Override
						public int compare(Biosample o1, Biosample o2) {
							Container c1 = o1.getContainer();
							Container c2 = o2.getContainer();
							if(c1==null && c2==null) return 0; 
							if(c1==null) return 1; 
							if(c2==null) return -1; 
							int c = CompareUtils.compare(c1.getAmount(), c2.getAmount());
							return c;
						}
					});
				}
			});
		}
		ContainerFullColumn.populateLocationHeaderPopupStatic(this, table, popupMenu);
	}

	public static void populateLocationHeaderPopupStatic(final Column<Biosample, ?> column, final AbstractExtendTable<Biosample> table, JPopupMenu popupMenu) {		
		
		popupMenu.add(new AbstractAction("Sort by Location/Rows") {
			@Override
			public void actionPerformed(ActionEvent e) {
				table.sortBy(column, 3, new Comparator<Biosample>() {
					@Override
					public int compare(Biosample o1, Biosample o2) {
						int c = CompareUtils.compare(o1.getLocation(), o2.getLocation());
						if(c!=0) return c;
						c = o1.getRow() - o2.getRow();
						if(c!=0) return c;
						return o1.getCol() - o2.getCol();
					}
				});
			}
		});
		
		popupMenu.add(new AbstractAction("Sort by Location/Column") {
			@Override
			public void actionPerformed(ActionEvent e) {
				table.sortBy(column, 4, new Comparator<Biosample>() {
					@Override
					public int compare(Biosample o1, Biosample o2) {
						int c = CompareUtils.compare(o1.getLocation(), o2.getLocation());
						if(c!=0) return c;
						c = o1.getCol() - o2.getCol();
						if(c!=0) return c;
						return o1.getRow() - o2.getRow();
					}
				});
			}
		});
	}	
}