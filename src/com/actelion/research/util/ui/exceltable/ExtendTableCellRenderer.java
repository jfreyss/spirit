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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;

import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.AbstractExtendTable.BorderStrategy;
import com.actelion.research.util.ui.exceltable.ExtendTableModel.Node;


public class ExtendTableCellRenderer<ROW>  implements TableCellRenderer, Serializable {

	private static final Color highlightBackground = new Color(240, 240, 200);
	private static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(0, 1, 0, 1);
	private static final Border SAFE_FOCUS_BORDER = BorderFactory.createLineBorder(new Color(57,105,138));

	private static BufferedImage plusImg;
	private static BufferedImage minusImg;
	private static BufferedImage leafImg;

	private static BufferedImage tree1Img;
	private static BufferedImage tree2Img;
	private static BufferedImage tree3Img;

	public static final Color COLOR_LIGHTBORDER = new Color(230,230,230);

	protected Set<Integer> calculatedHeights = new HashSet<Integer>();
	private static final HierarchyPanel hierarchyPanel = new HierarchyPanel();

	/**
	 * Cache for borders
	 */
	private Map<String, Boolean> row_col2BottomBorder = new HashMap<>();

	static {
		try {
			plusImg =  ImageIO.read(ExtendTableCellRenderer.class.getResource("tree_plus.png"));
			minusImg = ImageIO.read(ExtendTableCellRenderer.class.getResource("tree_minus.png"));
			leafImg = ImageIO.read(ExtendTableCellRenderer.class.getResource("tree_leaf.png"));
			tree1Img = ImageIO.read(ExtendTableCellRenderer.class.getResource("tree1.png"));
			tree2Img = ImageIO.read(ExtendTableCellRenderer.class.getResource("tree2.png"));
			tree3Img = ImageIO.read(ExtendTableCellRenderer.class.getResource("tree3.png"));
		} catch (Exception e) {
			//Ignore the images
		}
	}


	/**
	 * Creates a default table cell renderer.
	 */
	public ExtendTableCellRenderer(AbstractExtendTable<ROW> table) {
		super();

		if(table.getModel()!=null) table.getModel().addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent evt) {
				reset();
			}
		});


		table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {}

			@Override
			public void columnRemoved(TableColumnModelEvent e) {reset();}

			@Override
			public void columnMoved(TableColumnModelEvent e) {reset();}

			@Override
			public void columnMarginChanged(ChangeEvent e) {reset();}

			@Override
			public void columnAdded(TableColumnModelEvent e) {}
		});
	}

	private void reset() {
		row_col2BottomBorder.clear();
		calculatedHeights.clear();
	}


	/**
	 * Not to be overriden, override getComponent instead
	 * if (isSelected==hasFocus==true) then the rowheight can be adjusted
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		try {
			AbstractExtendTable myTable = (AbstractExtendTable) table;
			ExtendTableModel<ROW> model = myTable.getModel();
			int modelColumn = table.convertColumnIndexToModel(column);
			int nextModelColumn = column+1>=table.getColumnCount() ? -1: table.convertColumnIndexToModel(column+1);

			Column<ROW, ?> col = model.getColumn(modelColumn);
			Column<ROW, ?> nextCol = nextModelColumn>=0? model.getColumn(nextModelColumn): null;
			ROW object = model.getRow(row);

			//Initializes values from table
			Color foreground = table.getForeground();
			Border border = hasFocus? SAFE_FOCUS_BORDER: SAFE_NO_FOCUS_BORDER;

			if(col==null) {
				return new JLabel(""+value);
			}

			//Get Component
			JComponent c = col.getCellComponent(myTable, object, row, value);

			//Autowrap
			if(c instanceof JLabelNoRepaint) {
				if(col.isAutoWrap() && (c instanceof JLabelNoRepaint) ) {
					int w = table.getColumnModel().getColumn(column).getWidth();
					((JLabelNoRepaint) c).setWrappingWidth(w);
				} else {
					((JLabelNoRepaint) c).setWrappingWidth(-1);
				}
			}
			boolean justGuessingSize = isSelected && hasFocus;
			//Adapt size if it was not done before and if not called from resetwidth (ie not when selected=hasfocus=true)
			if(object!=null && !justGuessingSize && !calculatedHeights.contains(row)) {
				calculatedHeights.add(row);
				myTable.fitRowHeight(row);
			}

			//Udate Colors/Borders + Post Process
			c.setBackground(UIManager.getColor("Table.background"));
			c.setForeground(foreground);
			c.setBorder(border);
			col.postProcess(myTable, object, row, value, c);
			myTable.postProcess(object, row, value, c);

			//Show selection
			if(isSelected) {
				c.setBackground(table.getSelectionBackground());
			}


			//Highlight cells similar to selection or rows?
			boolean highlight = false;
			if(myTable.isHighlightSimilarCells()) {
				if(table.getSelectedRow()>=0 && table.getSelectedColumn()>=0){
					Object selValue = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn());
					if(value!=null && value.equals(selValue)) {
						highlight = true;
					}
				}
			}
			if(myTable.getHighlightRows()!=null && myTable.getHighlightRows().contains(object)) {
				highlight = true;
			}
			if(highlight) {
				c.setBackground(UIUtils.getDilutedColor(c.getBackground(), highlightBackground, .7));
			}


			//Make non-enable table or non-editable cells gray
			if (!myTable.isEnabled() || (model.isEditable() && !isSelected && !model.isCellEditable(row, modelColumn))) {
				c.setBackground(UIUtils.darker(c.getBackground(), .9));
				c.setForeground(UIUtils.getDilutedColor(c.getForeground(), c.getBackground()));
			}

			//Add bottom border if value differs
			//Add right border if category differs
			boolean rightBorder = nextCol==null || col.getCategory().length()==0 || !col.getCategory().equals(nextCol.getCategory());
			Color lightBorder = UIManager.getColor("Table.gridColor");
			if(lightBorder==null) lightBorder = COLOR_LIGHTBORDER;
			Color darkBorder = UIUtils.darker(lightBorder, .7);

			if( myTable.getBorderStrategy()==BorderStrategy.ALL_BORDER) {
				c.setBorder(BorderFactory.createCompoundBorder(
						new ZoneBorder(null, null, darkBorder, rightBorder? darkBorder: lightBorder),
						c.getBorder()));

			} else if( myTable.getBorderStrategy()==BorderStrategy.WHEN_DIFFERENT_VALUE) {

				if(!myTable.isEditable() && myTable.isUseAlphaForMergedCells()) {
					Boolean bPrev = shouldMergeWithBottom(myTable, column, row-1);
					if(bPrev==Boolean.TRUE) {
						if(c instanceof JComponentNoRepaint) {
							((JComponentNoRepaint)c).setAlpha(.8f);
						} else {
							c.setVisible(false);
						}
					} else {
						if(c instanceof JComponentNoRepaint) {
							((JComponentNoRepaint)c).setAlpha(0f);
						} else {
							c.setVisible(true);
						}
					}
				} else {
					if(c instanceof JComponentNoRepaint) {
						((JComponentNoRepaint)c).setAlpha(0f);
					} else {
						c.setVisible(true);
					}
				}


				boolean merge = shouldMergeWithBottom(myTable, column, row);
				if(!merge) {
					c.setBorder(BorderFactory.createCompoundBorder(
							new ZoneBorder(null, null, darkBorder, rightBorder? darkBorder: null),
							c.getBorder()));
				} else {
					c.setBorder(BorderFactory.createCompoundBorder(
							new ZoneBorder(null, null, null, rightBorder? darkBorder: null),
							c.getBorder()));
				}
			}


			//Create the hierarchy if enabled
			if(model.getTreeColumn()==col && model.isTreeViewActive()) {
				Node node = model.getNode(object);
				if(node==null || node.pattern==null) {
					try {
						myTable.getModel().reorderTree();
						return c;
					} catch (Exception e) {
						return new JLabel("No treeinfo for "+object);
					}
				}

				if(node.leaf==null && !justGuessingSize) {
					try {
						node.leaf = myTable.getModel().getTreeChildren(object).isEmpty();
					} catch(Exception e) {
						node.leaf = true;
					}
				}


				hierarchyPanel.set(myTable, node, c);


				return hierarchyPanel;
			}

			return c;
		} catch (Throwable e) {
			e.printStackTrace();
			return new JCustomLabel(e.toString(), Color.RED);
		}
	}

	private boolean shouldMergeWithBottom(AbstractExtendTable<ROW> table, int viewCol, int row) {
		final ExtendTableModel<ROW> model = table.getModel();
		String key = row+"_"+viewCol;
		Boolean b = row_col2BottomBorder.get(key);
		if(b==null) {
			if(row<0) {
				b = false;
			} else if(row+1>=model.getRowCount()) {
				b = false;
			} else if(viewCol>0 && table.getColumnCount()<100
					&& !shouldMergeWithBottom(table, viewCol-1, row)
					&& ((table.getValueAt(row, viewCol)==null && viewCol>=2)
							|| table.getModel().getColumn(table.convertColumnIndexToModel(viewCol-1)).getCategory().equals(table.getModel().getColumn(table.convertColumnIndexToModel(viewCol)).getCategory()))
					) {
				b = false;
			} else {
				b = table.shouldMerge(viewCol, row, row+1);
			}

			row_col2BottomBorder.put(key, b);
		}
		return b;
	}



	/*
	 * The following methods are overridden as a performance measure to to prune
	 * code-paths are often called in the case of renders but which we know are
	 * unnecessary. Great care should be taken when writing your own renderer to
	 * weigh the benefits and drawbacks of overriding methods like these.
	 */

	/**
	 * Overridden for performance reasons. See the <a
	 * href="#override">Implementation Note</a> for more information.
	 */
	public boolean isOpaque() {
		return true;
	}

	private static class HierarchyPanel extends JPanel {
		private JComponent c;
		private String pattern = "";
		private Dimension prefDim = new Dimension();

		public HierarchyPanel() {
			super(null);
			setOpaque(false);
		}

		@Override
		public void paint(Graphics graphics) {

			Graphics2D g = (Graphics2D) graphics;
			g.setBackground(c.getBackground());
			g.clearRect(0, 0, getWidth(), getHeight());

			int x = 1;
			int y = 0;
			for (int i = 0; i < pattern.length(); i++) {
				char ch = pattern.charAt(i);

				BufferedImage img;
				switch(ch) {
				case '0': img = null; break;
				case '1': img = tree1Img; break;
				case '2': img = tree2Img; break;
				case '3': img = tree3Img; break;
				case '.': img = leafImg; break;
				case '/': img = null; break;
				case '+': img = plusImg; break;
				case '-': img = minusImg; break;
				default: continue;
				}
				if(img!=null) {
					g.drawImage(img, x, y, this);
				}
				x+=6;
			}
			super.paint(g);
			paintBorder(g);

		}



		int offset = 0;
		@Override
		public void doLayout() {
			c.setBounds(offset, 0, getWidth()-offset, getHeight());
		}

		@Override
		public Dimension getPreferredSize() {
			return prefDim;
		}
		@Override
		public Dimension getMinimumSize() {
			return prefDim;
		}


		@Override
		public String getToolTipText() {
			return c.getToolTipText();
		}

		public void set(AbstractExtendTable<?> table, Node info, JComponent c) {
			this.c = c;
			this.pattern = info.pattern;

			//L&F
			setBackground(c.getBackground());
			setForeground(c.getForeground());
			if(c.getBackground().getRed()==255 && c.getBackground().getBlue()==255 && c.getBackground().getGreen()==255) setBackground(Color.WHITE);

			setBorder(c.getBorder());
			c.setBorder(null);

			removeAll();
			add(c);
			offset = 1+pattern.length()*6;
			if( table.getModel().isCanExpand() && !(table instanceof ExcelTable)) {
				//Add +/- sign so the user can expand the row (if non editable)
				if(info.leaf==Boolean.TRUE && info.pattern.length()>0) pattern += '.';
				else if(info.leaf==Boolean.TRUE) pattern += '/';
				else if(info.expanded) pattern += '-';
				else pattern += '+';
				offset+=9;
			}
			prefDim.width = offset + c.getPreferredSize().width;
			prefDim.height = c.getPreferredSize().height;


		}

	}

}
