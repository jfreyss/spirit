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

import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class JSplitPaneWithZeroSizeDivider extends JSplitPane {

	private int DIVIDER_SIZE = 4;
	/**
	 * The size of the transparent drag area.
	 */
	private int dividerDragSize = 15;

	/**
	 * The offset of the transparent drag area relative to the visible divider
	 * line. Positive offset moves the drag area left/top to the divider line.
	 * If zero then the drag area is right/bottom of the divider line. Useful
	 * values are in the range 0 to {@link #dividerDragSize}. Default is
	 * centered.
	 */
	private int dividerDragOffset = dividerDragSize/2;

	public JSplitPaneWithZeroSizeDivider() {
		this(HORIZONTAL_SPLIT);
	}

	public JSplitPaneWithZeroSizeDivider(int orientation) {
		super(orientation);
		setContinuousLayout(true);
		setDividerSize(DIVIDER_SIZE);
	}

	public JSplitPaneWithZeroSizeDivider(int orientation, JComponent comp1, JComponent comp2) {
		super(orientation, comp1, comp2);
		setContinuousLayout(true);
		setDividerSize(DIVIDER_SIZE);
	}

	public int getDividerDragSize() {
		return dividerDragSize;
	}

	public void setDividerDragSize(int dividerDragSize) {
		this.dividerDragSize = dividerDragSize;
		revalidate();
	}

	public int getDividerDragOffset() {
		return dividerDragOffset;
	}

	public void setDividerDragOffset(int dividerDragOffset) {
		this.dividerDragOffset = dividerDragOffset;
		revalidate();
	}

	@Override
	public void doLayout() {
		super.doLayout();

		// increase divider width or height
		BasicSplitPaneDivider divider = ((BasicSplitPaneUI) getUI()).getDivider();
		Rectangle bounds = divider.getBounds();
		if (orientation == HORIZONTAL_SPLIT) {
			bounds.x -= dividerDragOffset;
			bounds.width = dividerDragSize;
		} else {
			bounds.y -= dividerDragOffset;
			bounds.height = dividerDragSize;
		}
		divider.setBounds(bounds);
	}

	@Override
	public void updateUI() {
		setUI(new SplitPaneWithZeroSizeDividerUI());
		revalidate();
	}

	// ---- class SplitPaneWithZeroSizeDividerUI -------------------------------

	private class SplitPaneWithZeroSizeDividerUI extends BasicSplitPaneUI {
		@Override
		public BasicSplitPaneDivider createDefaultDivider() {
			return new ZeroSizeDivider(this);
		}
	}

	// ---- class ZeroSizeDivider ----------------------------------------------

	private class ZeroSizeDivider extends BasicSplitPaneDivider {

		public ZeroSizeDivider(BasicSplitPaneUI ui) {
			super(ui);
			super.setBorder(null);
			setBackground(UIManager.getColor("controlShadow"));
		}

		@Override
		public void setBorder(Border border) {
		}

		@Override
		public void paint(Graphics g) {
			g.setColor(getBackground());
			if (orientation == HORIZONTAL_SPLIT) {
				g.drawLine(dividerDragOffset, 0, dividerDragOffset, getHeight() - 1);
			} else {
				g.drawLine(0, dividerDragOffset, getWidth() - 1, dividerDragOffset);
			}
		}

		@Override
		protected void dragDividerTo(int location) {
			super.dragDividerTo(location + dividerDragOffset);
		}

		@Override
		protected void finishDraggingTo(int location) {
			super.finishDraggingTo(location + dividerDragOffset);
		}
	}
}