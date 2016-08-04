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

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("rawtypes")
public class CheckboxNode extends AbstractCheckboxNode<Boolean> {

	private final JLabel lbl;
	private final JPanel res;
	
	public CheckboxNode(FormTree tree, String label) {
		this(tree, label, null);
	}
	public CheckboxNode(FormTree tree, String label, Strategy<Boolean> strategy) {
	
		super(tree, label, strategy);
		this.lbl = new JLabel(label);
		
		res = new JPanel();		
		res.setOpaque(false);
		res.setLayout(new BoxLayout(res, BoxLayout.X_AXIS));
		res.add(checkbox);
		res.add(lbl);
		addEventsToComponent();
		
	}	
	
	
	
	@Override
	public JComponent getComponent() {		
		return res;
	}
	
	public CheckboxNode setBold(boolean bold) {
		lbl.setFont(lbl.getFont().deriveFont(bold ? Font.BOLD: Font.PLAIN));
		return this;
	}
	public AbstractNode setFontSize(int fontSize) {
		lbl.setFont(lbl.getFont().deriveFont((float)fontSize));
		return this;
	}
	public AbstractNode setForeground(Color color) {
		lbl.setForeground(color);
		return this;
	}
	
	public AbstractNode setImage(Image img) {
		if(img!=null) {
			lbl.setIcon(new ImageIcon(img));
		} else {
			lbl.setIcon(null);			
		}
		return this;
	}
	
	@Override
	protected void updateModel() {
		if(strategy!=null) strategy.setModel(checkbox.isSelected());
	}
	@Override
	protected void updateView() {
		if(strategy!=null) {
			boolean value = strategy.getModel()==Boolean.TRUE;
			boolean mustChange = checkbox.isSelected() != value;
			if(mustChange) {		
				checkbox.setSelected(value);
			}
		}
	}
	

	@Override
	public String toString() {
		return lbl.getText();
	}
	
}
