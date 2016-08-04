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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;

import com.actelion.research.util.ui.JComboCheckBox;

public class MultiNode extends AbstractNode<String> {
	
	private final JComboCheckBox comboCheckBox;
	
	public MultiNode(FormTree tree, String label) {
		this(tree, label, new ArrayList<String>(), null);
	}
		
	public MultiNode(FormTree tree, String label, Strategy<String> accessor) {
		this(tree, label, new ArrayList<String>(), accessor);
	}	
	
	public MultiNode(FormTree tree, String label, List<String> values, Strategy<String> accessor) {
		super(tree, label, accessor);
		
		this.comboCheckBox = new JComboCheckBox(values);

		
		comboCheckBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				getTree().firePropertyChange(FormTree.PROPERTY_SUBMIT_PERFORMED, false, true);
			}
		});
		
		
		comboCheckBox.setTextWhenEmpty(label);
		comboCheckBox.setToolTipText("<html> <u><b>"+label + "</b></u><br></html>");

	}
	
	public void setValues(List<String> choices) {
		Collections.sort(choices);
		comboCheckBox.setChoices(choices);
	}
	
	@Override
	public JComponent getComponent() {
		return comboCheckBox;
	}
	
	@Override
	protected void updateModel() {
		if(strategy==null) {
			System.err.println("No Strategy defined for "+getLabel());
			return;
		}
		String s = comboCheckBox.getText();
		if(s.length()>0) {
			s = "*" + s.replaceAll("\\s*;\\s*", "*;*") + "*";
		}
		strategy.setModel(s);
	}
	
	@Override
	protected void updateView() {
		String model = strategy.getModel();
		if(model!=null && model.length()>2) model = model.substring(1, model.length()-2).replaceAll("\\*;\\*", "; ");
		comboCheckBox.setText(model);
	}
	@Override
	protected boolean isFilled() {
		return comboCheckBox.getText().length()>0;
	}
	
	@Override
	public JComponent getFocusable() {
		return comboCheckBox;
	}
	

	public JComboCheckBox getComboCheckBox() {
		return comboCheckBox;
	}
	
}
