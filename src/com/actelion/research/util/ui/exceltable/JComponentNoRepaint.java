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
import java.awt.Graphics2D;

import javax.swing.JComponent;

import com.actelion.research.util.ui.UIUtils;

/**
 * JComponent but faster, ignore validate, repaint,...
 * @author J
 *
 */
public class JComponentNoRepaint extends JComponent {

	private float alpha = 0f;
	private boolean opaque = true;

	@Override
	public void setOpaque(boolean isOpaque) {
		this.opaque = isOpaque;
	}

	/**
	 * Overridden for performance reasons. See the <a
	 * href="#override">Implementation Note</a> for more information.
	 */
	@Override
	public boolean isOpaque() {
		return opaque;
	}

	@Override
	public void invalidate() {
	}

	/**
	 * Overridden for performance reasons. See the <a
	 * href="#override">Implementation Note</a> for more information.
	 */
	@Override
	public void validate() {
	}

	/**
	 * Overridden for performance reasons. See the <a
	 * href="#override">Implementation Note</a> for more information.
	 */
	@Override
	public void revalidate() {
	}

	/**
	 * Overridden for performance reasons. See the <a
	 * href="#override">Implementation Note</a> for more information.
	 */
	@Override
	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		// Strings get interned...
		if (propertyName == "text" || propertyName == "labelFor" || propertyName == "displayedMnemonic"
				|| ((propertyName == "font" || propertyName == "foreground") && oldValue != newValue && getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey) != null)) {
			super.firePropertyChange(propertyName, oldValue, newValue);
		}
	}

	/**
	 * Overridden for performance reasons. See the <a
	 * href="#override">Implementation Note</a> for more information.
	 */
	@Override
	public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
	}


	public float getAlpha() {
		return alpha;
	}


	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	@Override
	public void paint(Graphics g) {
		paintComponent(g);

		if(alpha>0) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setColor(UIUtils.getColor(getBackground(), (int)(255*alpha)));
			g2d.fillRect(0, 0, getWidth(), getHeight());
		}
		paintBorder(g);
	}

	@Override
	protected void paintComponent(Graphics g) {
		UIUtils.applyDesktopProperties(g);
		if(isOpaque()) {
			((Graphics2D)g).setBackground(getBackground());
			g.clearRect(0, 0, getWidth(), getHeight());
		}
	}
}
