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

package com.actelion.research.spiritapp.spirit.ui.util.formtree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

public abstract class AbstractCheckboxNode<T> extends AbstractNode<T> {

	protected final JCheckBox checkbox = new JCheckBox();
	
	public AbstractCheckboxNode(FormTree tree, String label) {
		this(tree, label, null);
	}
	public AbstractCheckboxNode(FormTree tree, String label, final Strategy<T> accessor) {
		super(tree, label, accessor);
		checkbox.setOpaque(false);				
		checkbox.addItemListener(new ItemListener() {			
			@Override
			public void itemStateChanged(ItemEvent e) {
				getTree().firePropertyChange(FormTree.PROPERTY_CHANGED, false, true);
				if(strategy!=null) strategy.onChange();
			}
		});
		checkbox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				getTree().firePropertyChange(FormTree.PROPERTY_CHANGED, false, true);
				if(strategy!=null) strategy.onAction();
			}
		});
	}
	
	@Override
	protected boolean isFilled() {
		return checkbox.isEnabled() && checkbox.isSelected();
	}

	@Override
	public final JComponent getFocusable() {
		return checkbox;
	}

	
	public final JCheckBox getCheckbox() {
		return checkbox;
	}
	
	
}
