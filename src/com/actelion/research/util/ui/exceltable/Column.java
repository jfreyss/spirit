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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;

import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;

/**
 * Column is used to represent any column in the AbstractExtendTable
 *
 * @author freyssj
 */
public abstract class Column<ROW, VALUE> implements Comparable<Column<?, ?>> {

	private static int staticCounter = 0;

	private String category = "";
	private String name;
	private Class<VALUE> columnClass;
	private int minWidth;
	private int maxWidth;
	private boolean hideable;
	private int internalCounter = staticCounter++;

	protected final JLabelNoRepaint lbl = new JLabelNoRepaint();
	private AbstractExtendTable<ROW> table;

	public Column(String name, Class<VALUE> columnClass) {
		this(name, columnClass, 15, 400);
	}
	public Column(String name, Class<VALUE> columnClass, int minWidth) {
		this(name, columnClass, minWidth, minWidth*3);
	}
	/**
	 * Creates a column
	 * @param name
	 * @param columnClass
	 * @param minWidth
	 */
	public Column(String name, Class<VALUE> columnClass, int minWidth, int maxWidth) {
		this.columnClass = columnClass;
		this.minWidth = FastFont.getAdaptedSize(minWidth);
		this.maxWidth = FastFont.getAdaptedSize(maxWidth);
		setName(name);
	}

	/**
	 * Set the name. Use "." or "\n" for multiline
	 * @param name
	 */
	protected void setName(String name) {
		//Set Name
		this.name = name;

		//Extract Category
		if(name.indexOf('\n')>=0) {
			String[] split = name.split("\n");
			this.category = split.length>=4? split[0]+"."+split[1]:
				split.length>=2? split[0]:
					name;
		} else if(name.indexOf('.')>0 && name.indexOf('.')<name.length()-1) {
			this.category = name.substring(0, name.indexOf('.'));
		} else {
			this.category = "";
		}

	}

	/**
	 * Returns the name with ".", "\n" separators (multiline)
	 * @return
	 */
	public String getName() {
		return name;
	}


	/**
	 * Returns the last part of the name without ".", "\n" (one line)
	 * @return
	 */
	public String getShortName() {
		String cut = name.trim().replaceAll("<.>", "");
		if(cut.length()<2) return cut;
		int index2 = cut.lastIndexOf('.', cut.length()-2);
		int index3 = cut.lastIndexOf('\n', cut.length()-2);
		int index = -1;
		if(index2>=0)  index = index2;
		if(index3>=0 && index3>index)  index = index3;

		return cut.substring(index+1);
	}

	/**
	 * Returns the category: normally the first part of the name before ".", "\n" (one line) or empty
	 * @return
	 */
	public String getCategory() {
		assert category!=null;
		return category;
	}

	public final Class<VALUE> getColumnClass() {
		return columnClass;
	}
	public int getMinWidth() {
		return minWidth;
	}
	public int getMaxWidth() {
		return maxWidth;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Column) || getName()==null) return false;
		return this==obj || getName().equals(((Column<?,?>)obj).getName());
	}

	protected VALUE getValue(ROW row, int rowNo) {
		try {
			return getValue(row);
		} catch(Throwable e) {
			return null;
		}
	}

	public abstract VALUE getValue(ROW row);


	/**
	 * Copy from an non editable value, will return this value (no tab allowed)
	 * @param row
	 * @param rowNo
	 * @return
	 */
	public String getCopyValue(ROW row, int rowNo) {
		VALUE v = getValue(row, rowNo);
		if(v==null) return "";
		String s = v.toString();
		s = s.indexOf('\t')>=0? s.substring(0, s.indexOf('\t')): s;
		if(s.length()>=3 && s.charAt(0)=='<' && s.charAt(2)=='>') s = s.substring(3);
		return s;
	}

	/**
	 * Editable by default
	 * @param row
	 * @return
	 */
	public boolean isEditable(ROW row) {return true;}

	/**
	 * To be implemented if isEditable returns true
	 * @param row
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public void paste(ROW row, String value) throws Exception {
		if(!isEditable(row)) return;
		try {
			if(value==null || value.length()==0) {
				setValue(row, (VALUE) null);
			} else if(columnClass==Double.class) {
				setValue(row, (VALUE) (Double) Double.parseDouble(value.trim()));
			} else if(columnClass==Integer.class) {
				setValue(row, (VALUE) (Integer) Integer.parseInt(value.trim()));
			} else if(columnClass==Date.class) {
				setValue(row, (VALUE) FormatterUtils.parseDateTime(value.trim()));
			} else {
				setValue(row, (VALUE) value);
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid Numeric Value: " + value + " for " + this);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Pasting is not possible for "+this);
		}
	}
	/**
	 * To be implemented if isEditable returns true
	 * @param row
	 * @param value
	 * @return
	 */
	public void setValue(ROW row, VALUE value) {throw new IllegalArgumentException("setValue(" + (row==null? null: row.getClass()) + ", " + (value==null?null:value.getClass())+"): Not implemented in "+this.getClass());}
	public void validate(ROW row) throws Exception {}


	/**
	 * Called by {@link ExtendTableCellRenderer}
	 * Can be overriden to define a custom cell renderer for this column
	 */
	public JComponent getCellComponent(AbstractExtendTable<ROW> table, ROW row, int rowNo, Object value) {
		if(value==null) {
			lbl.setText("");
		} else if (value instanceof Date) {
			lbl.setText(com.actelion.research.util.FormatterUtils.formatDate(((Date) value)));
			lbl.setToolTipText(com.actelion.research.util.FormatterUtils.formatDateTime(((Date) value)));
			lbl.setHorizontalAlignment(SwingConstants.RIGHT);
		} else if(value instanceof Number) {
			lbl.setText(value.toString());
			lbl.setHorizontalAlignment(SwingConstants.RIGHT);
		} else {
			lbl.setText(value.toString());
			lbl.setHorizontalAlignment(SwingConstants.LEFT);
		}


		return lbl;
	}

	/**
	 * Can be overriden to define a custom cell editor
	 * @param table
	 * @return
	 */
	public TableCellEditor getCellEditor(AbstractExtendTable<ROW> table) {
		return null;
	}

	/**
	 * Called by {@link ExtendTableCellRenderer}
	 * @param comp
	 */
	public void postProcess(AbstractExtendTable<ROW> table, ROW row, int rowNo, Object value, JComponent comp) {}

	/**
	 * Can be used to define custom sorting or more display columns.
	 * This is done directly after the "Column ..."
	 * By default it adds the "Sort", "Sort by name" menu items
	 */
	public void populateHeaderPopup(final AbstractExtendTable<ROW> table, final JPopupMenu popupMenu) {
		popupMenu.add(new JSeparator());
		if(table.isCanSort()) {
			popupMenu.add(new JCustomLabel("Sort", Font.BOLD));
			popupMenu.add(new AbstractAction("Sort by "+getShortName()) {
				@Override
				public void actionPerformed(ActionEvent e) {
					table.sortBy(Column.this);
				}
			});
		}

	}

	/**
	 * Returns the toolTipText for the cells
	 * @return
	 */
	public String getToolTipText(ROW row) {
		return null;
	}
	/**
	 * Returns the toolTipText for the header
	 * @return
	 */
	public String getToolTipText() {
		return name;
	}

	@Override
	public int hashCode() {
		return name==null? 0: name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}


	public float getSortingKey() {
		return internalCounter;
	}

	/**
	 * Only for default renderer, set to true if the column can wrap on several lines (and then columns will reajust)
	 * Default is false
	 * @return
	 */
	public boolean isAutoWrap() {
		return false;
	}

	/**
	 * Set to true (default=false) if the height of this component can be set higher than the default row value
	 * Default is false
	 * @return
	 */
	public boolean isMultiline() {
		return false;
	}

	/**
	 * Should we merge the 2 given rows (ie no border)
	 * Default is true if the value is the same
	 * @return
	 */
	public boolean shouldMerge(ROW r1, ROW r2) {
		VALUE o1 = getValue(r1);
		VALUE o2 = getValue(r2);

		if(o1==null && o2==null) return true;
		String s1 = o1==null? "": o1.toString();
		String s2 = o2==null? "": o2.toString();
		if(s1.length()==0 && s2.length()==0) return true;
		return s1.equals(s2);
	}

	/**
	 * Called when a cell is double-clicked.
	 *
	 * @param table
	 * @param row
	 * @param rowNo
	 * @param value
	 * @return true if the event has to be consumed
	 */
	public boolean mouseDoubleClicked(AbstractExtendTable<ROW> table, ROW row, int rowNo, Object value) {
		return false;
	}

	public boolean isHideable() {
		return hideable;
	}
	public Column<ROW, VALUE> setHideable(boolean hideable) {
		this.hideable = hideable;
		return this;
	}

	@Override
	public int compareTo(Column<?, ?> o) {
		return Float.compare(getSortingKey(), o.getSortingKey());
	}
	public AbstractExtendTable<ROW> getTable() {
		return table;
	}
	protected void setTable(AbstractExtendTable<ROW> table) {
		this.table = table;
	}
}
