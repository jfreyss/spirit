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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JTextComboBox;

public class ComboBoxNode<T> extends AbstractNode<T> {
	
	private final JGenericComboBox<T> comboBox;
	
	public ComboBoxNode(FormTree tree, String label) {
		this(tree, label, new ArrayList<T>(), null);
	}
		
	public ComboBoxNode(FormTree tree, String label, Strategy<T> accessor) {
		this(tree, new JGenericComboBox<T>(), label, null, accessor);
	}	
	public ComboBoxNode(FormTree tree, String label, Collection<T> values, Strategy<T> accessor) {
		this(tree, new JGenericComboBox<T>(), label, values, accessor);
	}	
	public ComboBoxNode(final FormTree tree, JGenericComboBox<T> comboBox, String label, Strategy<T> accessor) {
		this(tree, comboBox, label, null, accessor);
	}
	public ComboBoxNode(final FormTree tree, final JGenericComboBox<T> comboBox, String label, Collection<T> values, Strategy<T> accessor) {
		super(tree, label, accessor);		
		this.comboBox = comboBox;

		
		if(values!=null && comboBox!=null) comboBox.setValues(values, label);
		comboBox.setFont(editFont);
		
		comboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(strategy!=null && comboBox.isFocusOwner()) {
					strategy.onAction();
				}
			}
		});
		comboBox.addPropertyChangeListener(JTextComboBox.PROPERTY_TEXTCHANGED, new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if(strategy!=null) {
					strategy.onChange();
				}
			}
		});

		comboBox.setTextWhenEmpty(label);
		comboBox.setToolTipText("<html><b><u>"+label+"</u></b></html>");

	}
	
	public void setValues(Collection<T> values) {
		comboBox.setValues(values, comboBox.getTextWhenEmpty());
	}
	@Override
	public JComponent getComponent() {
		return comboBox;
	}
	
	@Override
	protected void updateModel() {
		if(strategy==null) {
			System.err.println("No Strategy defined for "+getLabel());
			return;
		}
		strategy.setModel(getSelection());
	}
	
	@Override
	protected void updateView() {
		T model = strategy.getModel();
		comboBox.setSelection(model);
	}
	@Override
	protected boolean isFilled() {
		return comboBox.getSelectedItem()!=null && comboBox.getSelectedItem().toString().length()>0;
	}

	public T getSelection() {
		return comboBox.getSelection();
	}
	
	public void setSelection(T selection) {
		comboBox.setSelection(selection);
	}
	
	@Override
	public JComponent getFocusable() {
		return comboBox;
	}
	

	public JComboBox<T> getComboBox() {
		return comboBox;
	}
	
}
