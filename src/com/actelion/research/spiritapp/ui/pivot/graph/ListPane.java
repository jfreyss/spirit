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

package com.actelion.research.spiritapp.ui.pivot.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.util.ui.UIUtils;

/**
 * The ListPane is a UI component, where the user can select components
 * The components are zoomable (ctrl-mousescroll) and scrollable vertically.
 *
 * @author Joel Freyss
 * @param <T>
 */
public class ListPane<T extends JPanel> extends JComponent implements Scrollable {

	public static String PROPERTY_DOUBLECLICK = "property_doubleclick";

	private int MINWIDTH = 120;
	private int MAXWIDTH = 220;
	private int maxWidth = MAXWIDTH;

	private Border focusBorder = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.BLACK),
			BorderFactory.createLineBorder(Color.BLUE, 2));
	private Border selectedBorder = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(UIUtils.WHITESMOKE),
			BorderFactory.createLineBorder(Color.BLUE, 2));
	private Border hoverBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIUtils.WHITESMOKE), BorderFactory.createLoweredBevelBorder());
	private Border unhoverBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIUtils.WHITESMOKE), BorderFactory.createRaisedBevelBorder());
	private ListSelectionListener listener;

	private List<T> panels = new ArrayList<>();
	private Set<Integer> selectedIndexes = new TreeSet<>();
	private Map<T, Integer> map2Index = new java.util.HashMap<>();
	private int lastIndexClicked = -1;
	private int nCols = 1;
	private int focus = -1;

	public ListPane() {
		super();

		//Mouse scroll and zoom
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if(e.isControlDown()) {
					if(e.getWheelRotation()<0 && nCols>1) {
						maxWidth = getWidth()/(nCols-1);
						maxWidth += maxWidth/nCols/2;
					} else if(e.getWheelRotation()>0 && maxWidth>100) {
						maxWidth = getWidth()/(nCols+1);
						maxWidth += maxWidth/nCols/2;
					}
					doLayout();
					getParent().validate();
					e.consume();
				} else if(getParent() instanceof JViewport) {
					Point pt = ((JViewport) getParent()).getViewPosition();
					if(e.getWheelRotation()<0) {
						((JViewport) getParent()).setViewPosition(new Point(pt.x, Math.max(0, pt.y-100)));
					} else {
						((JViewport) getParent()).setViewPosition(new Point(pt.x, Math.min(getHeight(), pt.y+100)));
					}
				}
			}
		});

		//KeyListener
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				boolean modifier = e.isControlDown() || e.isShiftDown();
				if(e.getKeyCode()==KeyEvent.VK_A && e.isControlDown()) {
					setSelectedItems(panels);
					e.consume();
				} else if(e.getKeyCode()==KeyEvent.VK_LEFT) {
					setFocus(Math.max(0, focus-1), modifier);
					e.consume();
				} else if(e.getKeyCode()==KeyEvent.VK_RIGHT) {
					setFocus(Math.min(panels.size()-1, focus+1), modifier);
					e.consume();
				} else if(e.getKeyCode()==KeyEvent.VK_UP) {
					setFocus(Math.max(0, focus-nCols), modifier);
					e.consume();
				} else if(e.getKeyCode()==KeyEvent.VK_DOWN) {
					setFocus(Math.min(panels.size()-1, focus+nCols), modifier);
					e.consume();
				}
			}
		});

		setFocusable(true);
		setRequestFocusEnabled(true);

	}

	private void setFocus(int focus, boolean modifiers) {
		if(!modifiers) {
			selectedIndexes.clear();
		}
		if(focus>=0 && focus<panels.size()) {
			panels.get(focus).setBorder(selectedIndexes.contains(focus)? selectedBorder: unhoverBorder);
			panels.get(focus).repaint();
		}
		this.focus = focus;
		if(focus>=0 && focus<panels.size()) {
			panels.get(focus).setBorder(focusBorder);
		}
		if(!modifiers) {
			setSelectedItem(panels.get(focus));
		} else {
			addSelectedItem(panels.get(focus));
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.setColor(panels.size()==0? getBackground(): Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
	}

	/**
	 * Gets the items, which can be selected by the user
	 * @return
	 */
	public List<T> getItems() {
		return panels;
	}

	/**
	 * Sets the items, which can be selected by the user
	 * @param panels
	 */
	public void setItems(List<T> panels) {
		this.panels = panels;
		map2Index.clear();
		removeAll();
		for (int i = 0; i < panels.size(); i++) {
			final int index = i;
			final T panel = panels.get(i);
			map2Index.put(panel, index);
			panel.setBorder(unhoverBorder);
			panel.setFocusable(false);
			panel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseExited(MouseEvent e) {
					panel.setBorder(focus==index?focusBorder: selectedIndexes.contains(index)? selectedBorder: unhoverBorder);
					panel.repaint();
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					panel.setBorder(focus==index?focusBorder: selectedIndexes.contains(index)? selectedBorder: hoverBorder);
					panel.repaint();
				}

				@Override
				public void mouseClicked(MouseEvent e) {
					focus = index;
					if(e.getClickCount()>=2) {
						firePropertyChange(PROPERTY_DOUBLECLICK, -1, focus);
					}
					if(e.isControlDown()) {
						if(selectedIndexes.contains(index)) {
							removeSelectedItem(panel);
						} else {
							addSelectedItem(panel);
						}
						lastIndexClicked = index;
					} else if(e.isShiftDown() && lastIndexClicked>=0) {
						boolean set = selectedIndexes.contains(lastIndexClicked);
						for(int x = Math.min(lastIndexClicked, index); x<=Math.max(lastIndexClicked, index); x++) {
							if(!set) {
								removeSelectedItem(panels.get(x));
							} else {
								addSelectedItem(panels.get(x));
							}
						}
					} else {
						lastIndexClicked = index;
						setSelectedItem(panel);
					}
					requestFocusInWindow();
				}
			});
			add(panel);
		}

		//Set size, so that all graphs fit on the screen
		if(getParent()!=null && getParent().getHeight()>0 && panels.size()>0) {
			maxWidth = (int) Math.sqrt(Math.max(50, (getWidth()-40)*1.2) * getParent().getHeight()*4.0/3/panels.size());
			if(maxWidth<MAXWIDTH) maxWidth = MAXWIDTH;
			if(maxWidth>getParent().getHeight()*4/3) maxWidth = Math.max(MINWIDTH, getParent().getHeight()*4/3);
		}

		validate();
		getParent().repaint();
	}

	/**
	 * Sets the listSelectionListener, called when the user selects an item
	 * @param listener
	 */
	public void setListSelectionListener(ListSelectionListener listener) {
		this.listener = listener;
	}

	/**
	 * Gets the selected items
	 * @return
	 */
	public List<T> getSelectedItems() {
		List<T> res = new ArrayList<>();
		for (int index : selectedIndexes) {
			if(index>=0 && index<panels.size()) {
				res.add(panels.get(index));
			}
		}
		return res;
	}

	/**
	 * Sets the selected item
	 * @param sel
	 */
	public void setSelectedItem(T sel) {
		selectedIndexes.clear();
		addSelectedItem(sel);
	}

	/**
	 * Sets the selected items
	 * @param sel
	 */
	public void setSelectedItems(Collection<T> sel) {
		selectedIndexes.clear();
		for (T t : sel) {
			Integer index = map2Index.get(t);
			if(index!=null) selectedIndexes.add(index);
		}
		updateBorders();
		fireSelectionChanged();
	}

	/**
	 * Adds an item to the selection
	 * @param sel
	 */
	public void addSelectedItem(T sel) {
		Integer index = map2Index.get(sel);
		if(index!=null) selectedIndexes.add(index);
		updateBorders();
		fireSelectionChanged();
	}

	public void removeSelectedItem(T sel) {
		Integer index = map2Index.get(sel);
		if(index!=null) selectedIndexes.remove(index);
		updateBorders();
		fireSelectionChanged();
	}

	/**
	 * Gets the indexes from the selected items
	 * @return
	 */
	public Set<Integer> getSelectedIndexes() {
		return Collections.unmodifiableSet(selectedIndexes);
	}

	private void fireSelectionChanged() {
		if(listener!=null) {
			listener.valueChanged(new ListSelectionEvent(ListPane.this, 0, panels.size(), false));
		}
	}


	private void updateBorders() {
		for (int i = 0; i < panels.size(); i++) {
			final int index = i;
			final T panel = panels.get(i);
			if(focus==index) {
				panel.setBorder(focusBorder);
			} else if(selectedIndexes.contains(index)) {
				panel.setBorder(selectedBorder);
			} else {
				panel.setBorder(unhoverBorder);
			}
		}
	}

	@Override
	public void doLayout() {
		int width = getWidth();
		if(getParent() instanceof JViewport) {
			width = ((JViewport)getParent()).getParent().getWidth()-25;
		}
		nCols = (int) Math.ceil(Math.max(1, width / (double)maxWidth));//(int) Math.ceil(Math.sqrt(n));
		int nRows = (int) Math.ceil(getComponentCount() / (double)nCols);
		int height = width / nCols * 3 / 4;
		for (int i = 0; i < getComponentCount(); i++) {
			int col = i%nCols;
			int row = i/nCols;
			getComponent(i).setBounds(col * width / nCols, row*height, width / nCols, height);
		}
		setPreferredSize(new Dimension(width, height*nRows));
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return null;
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 100;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 100;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}
