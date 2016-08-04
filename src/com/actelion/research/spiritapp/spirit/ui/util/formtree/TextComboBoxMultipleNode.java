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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import javax.swing.JComponent;

import com.actelion.research.spiritcore.util.QueryTokenizer;
import com.actelion.research.util.ui.JTextComboBox;

public abstract class TextComboBoxMultipleNode extends AbstractNode<String[]> {
	
	private final JTextComboBox comboBox;
	private final FieldType fieldType;

	public TextComboBoxMultipleNode(FormTree tree, FieldType fieldType, String label) {
		this(tree, fieldType, label, null);
	}
		
	public TextComboBoxMultipleNode(final FormTree tree, String label, final Strategy<String[]> accessor) {
		this(tree, FieldType.AND_CLAUSE, label, accessor);
	}	
	public TextComboBoxMultipleNode(final FormTree tree, FieldType fieldType, String label, final Strategy<String[]> accessor) {
		super(tree, label, accessor);
		
		this.fieldType = fieldType;
		this.comboBox = new JTextComboBox() {
			@Override
			public Collection<String> getChoices() {
				return TextComboBoxMultipleNode.this.getChoices();
			}
		};
//		comboBox.setEscapeSpaceWithQuotes(true);
		this.comboBox.addFocusListener(new FocusAdapter() {			
			@Override
			public void focusGained(FocusEvent e) {
				comboBox.selectAll();
			}
		});
		
		boolean multiple = fieldType==FieldType.AND_CLAUSE || fieldType==FieldType.OR_CLAUSE;
		comboBox.setMultipleChoices(multiple);
		comboBox.addPropertyChangeListener(JTextComboBox.PROPERTY_TEXTCHANGED, new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				getTree().firePropertyChange(FormTree.PROPERTY_CHANGED, false, true);
				if(strategy!=null) strategy.onChange();
			}
		});
		
		comboBox.setFont(editFont);		
		comboBox.setTextWhenEmpty(label);

		comboBox.setToolTipText("<html> <u><b>"+label + "</b></u><br>"
				+ (fieldType==FieldType.OR_CLAUSE? QueryTokenizer.getHelp(false): fieldType==FieldType.AND_CLAUSE? QueryTokenizer.getHelp(true): "") + "<br>"  
				+ (multiple?"Use <b>Ctrl-Click</b> or <b>Shift-Click</b> to select more than one " + label:"") + "</html>");

	}
	
	public FieldType getFieldType() {
		return fieldType;
	}

	
	/**
	 * To be overriden
	 * @return
	 */
	public abstract Collection<String> getChoices();
	
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
		strategy.setModel(comboBox.getSelectionArray());
	}
	
	@Override
	protected void updateView() {
		comboBox.setSelectionArray(strategy.getModel());
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
