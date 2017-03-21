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

package com.actelion.research.util.ui;

import java.awt.Dimension;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

/**
 * Fix 22.01.2013 - Java7 Compatible
 * @author freyssj
 *
 */
public class JComboBoxBigPopup<T> extends JComboBox<T> {
	
	private int preferredWidth;

	public JComboBoxBigPopup(int preferredWidth, ComboBoxModel<T> model) {
		super(model);
		this.preferredWidth = preferredWidth;
		setMaximumRowCount(20);
	}

	public JComboBoxBigPopup(int preferredWidth, Vector<T> objects) {
		super(objects);
		this.preferredWidth = preferredWidth;
		setMaximumRowCount(20);
	}
	
	public int getPreferredWidth() {
		return preferredWidth;
	}

	public void setPreferredWidth(int preferredWidth) {
		this.preferredWidth = preferredWidth;
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(Math.min(super.getPreferredSize().width, getPreferredWidth()), 26);
	}

	@Override
    public Dimension getSize(){ 
    	Dimension dim = super.getSize(); 
    	
    	Object comp = getUI().getAccessibleChild(this, 0);
		if (!(comp instanceof JPopupMenu) || !(((JPopupMenu) comp).getComponent(0) instanceof JScrollPane)) {
			System.err.println("Not a JPopupMenu? "+(comp instanceof JPopupMenu));
			return dim;
		}
		JScrollPane scrollPane = (JScrollPane) ((JPopupMenu) comp).getComponent(0);
		Dimension size = scrollPane.getPreferredSize();			
        dim.width = Math.max(dim.width, size.width+15);
     
        return dim; 
    } 
    
}
