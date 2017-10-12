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

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collection;

import javax.swing.JComponent;

import com.actelion.research.util.ui.JObjectComboBox;
import com.actelion.research.util.ui.JTextComboBox;

public class ObjectComboBoxNode<T> extends AbstractNode<T> {

	private final JObjectComboBox<T> comboBox;

	public ObjectComboBoxNode(final FormTree tree, String label, Collection<T> choices, final Strategy<T> accessor) {
		super(tree, label, accessor);

		this.comboBox = new JObjectComboBox<T>() {
			@Override
			public Collection<T> getValues() {
				return choices;
			}
		};
		init();
	}

	public ObjectComboBoxNode(final FormTree tree, String label, JObjectComboBox<T> comboBox, final Strategy<T> accessor) {
		super(tree, label, accessor);
		this.comboBox  = comboBox;
		init();
	}

	private void init() {
		this.comboBox.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				comboBox.selectAll();
			}
		});

		this.comboBox.addPropertyChangeListener(JTextComboBox.PROPERTY_TEXTCHANGED, evt-> {
			getTree().firePropertyChange(FormTree.PROPERTY_CHANGED, false, true);
			if(strategy!=null) strategy.onChange();
		});
		this.comboBox.addActionListener(e-> {
			getTree().firePropertyChange(FormTree.PROPERTY_SUBMIT_PERFORMED, false, true);
			if(strategy!=null) strategy.onAction();
		});

		this.comboBox.setFont(editFont);
		this.comboBox.setTextWhenEmpty(label);
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
		strategy.setModel(comboBox.getSelection());
	}

	@Override
	protected void updateView() {
		comboBox.setSelection(strategy.getModel());
	}

	@Override
	protected boolean isFilled() {
		return getSelection()!=null;
	}

	public T getSelection() {
		return comboBox.getSelection();
	}

	public void setSelection(T sel) {
		comboBox.setSelection(sel);
	}

	public void setValues(Collection<T> choices) {
		comboBox.setValues(choices);
	}

	public Collection<T> getValues() {
		return comboBox.getValues();
	}

	@Override
	public JComponent getFocusable() {
		return comboBox;
	}


}
