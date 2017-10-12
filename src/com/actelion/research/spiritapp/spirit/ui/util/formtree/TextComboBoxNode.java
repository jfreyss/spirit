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

import java.util.Collection;

import javax.swing.JComponent;

import com.actelion.research.util.ui.JTextComboBox;

public class TextComboBoxNode extends AbstractNode<String> {

	private final JTextComboBox comboBox;
	private Collection<String> choices;

	public TextComboBoxNode(final FormTree tree, String label, final Strategy<String> accessor) {
		this(tree, label, false, accessor);
	}

	public TextComboBoxNode(final FormTree tree, String label, Collection<String> choices, final Strategy<String> accessor) {
		this(tree, label, false, choices, accessor);
	}

	public TextComboBoxNode(final FormTree tree, String label, boolean multiple, final Strategy<String> accessor) {
		this(tree, label, multiple, null, accessor);
	}

	public TextComboBoxNode(final FormTree tree, String label, boolean multiple, Collection<String> choices, final Strategy<String> accessor) {
		super(tree, label, accessor);

		this.comboBox = new JTextComboBox() {
			@Override
			public Collection<String> getChoices() {
				return TextComboBoxNode.this.getChoices();
			}
		};

		this.choices = choices;

		comboBox.setMultipleChoices(multiple);

		comboBox.addPropertyChangeListener(JTextComboBox.PROPERTY_TEXTCHANGED, evt-> {
			getTree().firePropertyChange(FormTree.PROPERTY_CHANGED, false, true);
			if(strategy!=null) strategy.onChange();
		});
		comboBox.addActionListener(e-> {
			if(strategy!=null) strategy.onAction();
		});

		comboBox.setFont(editFont);
		comboBox.setTextWhenEmpty(label);
	}



	/**
	 * To be overriden
	 * @return
	 */
	public Collection<String> getChoices() {
		return choices;
	}

	public void setChoices(Collection<String> choices) {
		this.choices = choices;
	}

	@Override
	public JTextComboBox getComponent() {
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
		comboBox.setText(strategy.getModel());
	}
	@Override
	protected boolean isFilled() {
		return getSelection()!=null && getSelection().length()>0;
	}

	public String getSelection() {
		return comboBox.getText();
	}


	@Override
	public JComponent getFocusable() {
		return comboBox;
	}


}
