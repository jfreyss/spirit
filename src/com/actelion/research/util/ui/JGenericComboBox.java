/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;

/**
 * Custom JCombobox that works with Generics (even without Java 7).
 * Features:
 *  - Empty value possible
 *  - Text when empty
 *  - a custom renderer can be specified by overiding 'processCellRenderer'
 *  
 * @author freyssj
 */
public class JGenericComboBox<T> extends JComboBoxBigPopup<T> {
	
	private String textWhenEmpty;
	
	public class JGenericComboBoxRenderer extends DefaultListCellRenderer {
		
		@SuppressWarnings("unchecked")
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			Component comp = super.getListCellRendererComponent(list, value==null?" ": value, index, isSelected, cellHasFocus);
			Color bg = comp.getBackground();
			Color fg = comp.getForeground();
			if( value instanceof String && ((String)value).length()==0) {
				JLabel lbl = new JLabel();
				lbl.setOpaque(true);
				if(!isEnabled()) {
					lbl.setForeground(Color.LIGHT_GRAY);
					lbl.setBackground(bg);
				} else if(isSelected) {
					lbl.setForeground(fg);
					lbl.setBackground(bg);
				} else {
					lbl.setForeground(JCustomTextField.LABEL_COLOR);
					lbl.setBackground(Color.WHITE);
				}
				lbl.setFont(getFont());
				lbl.setText(textWhenEmpty);
				lbl.setPreferredSize(new Dimension(20,20));

				return lbl;
			} else {
				try {
					Component comp2 = processCellRenderer(this, (T) value, index);					
					if(comp2!=null) {
						if((comp2 instanceof JComponent) && (isSelected || (!((JComponent)comp2).isOpaque() && ((JComponent)comp2).getBackground()!=null))) {
							((JComponent)comp2).setOpaque(true);
							comp2.setForeground(fg);
							comp2.setBackground(bg);
						}
						return comp2;
					}
				} catch(Exception e) {
					System.err.println("Error at  index="+index+" / value="+value);
					e.printStackTrace();
				}
				return comp;
			}
		}
		
	}
	
	public JGenericComboBox() {
		super(155, new DefaultComboBoxModel<T>());

		setRenderer(new JGenericComboBoxRenderer());
		setFont(FastFont.REGULAR);
	}
	
	public JGenericComboBox(T[] values, boolean allowNull) {
		this(Arrays.asList(values), allowNull );
	}
	
	public JGenericComboBox(T[] values, String allowNull) {
		this(Arrays.asList(values), allowNull );
	}
	
	public JGenericComboBox(Collection<T> values, boolean allowNull) {
		this();
		setValues(values, allowNull);
	}
	
	public JGenericComboBox(Collection<T> values, String allowNull) {
		this();
		setValues(values, allowNull);		
	}
	
	public final void setValues(Collection<T> values) {
		setValues(values, getTextWhenEmpty());
	}
	public final void setValues(Collection<T> values, boolean allowNull) {
		setValues(values, allowNull? "": null);
	}
	public void setValues(Collection<T> values, String textWhenEmpty) {
		T oldValue = getSelection();		
		DefaultComboBoxModel<T> model = new DefaultComboBoxModel<T>();
		if(textWhenEmpty!=null) {
			setTextWhenEmpty(textWhenEmpty);
			model.addElement(null);
		}
		if(values!=null) {
			for (T value : values) {
				if(value!=null) {
					model.addElement(value);
				}
			}
		}
		setModel(model);		
		if(values!=null && oldValue!=null && values.contains(oldValue)) {
			setSelection(oldValue);
		}
	}
	
	public List<T> getValues(){
		List<T> res = new ArrayList<T>();
		for (int i = 0; i < getModel().getSize(); i++) {
			if(textWhenEmpty!=null && i==0) continue;
			T option = (T) getModel().getElementAt(i);
			res.add(option);
		}
		return res;
	}
	
	
	public void setSelectionString(String value) {
		for (int i = 0; i < getModel().getSize(); i++) {
			T option = (T) getModel().getElementAt(i);
			if(option!=null && option.toString().equals(value)) {setSelectedIndex(i); selectedItemChanged(); return;}
		}
		
		setSelectedItem(value);
	}
	public void setSelection(T value) {		
		if(textWhenEmpty!=null && value==null && getModel().getSize()>0) setSelectedIndex(0);
		else setSelectedItem(value);
	}
	
	
	@SuppressWarnings("unchecked")
	public T getSelection() {
		if(textWhenEmpty!=null && getSelectedIndex()==0) return null;
		return (T) super.getSelectedItem();
	}
	
	

	public void setTextWhenEmpty(String textWhenEmpty) {
		this.textWhenEmpty = textWhenEmpty;
		setToolTipText(textWhenEmpty==null || textWhenEmpty.length()==0? null: textWhenEmpty);
	}
	
	public String getTextWhenEmpty() {
		return textWhenEmpty;
	}
	
	@Override
	public Dimension getMinimumSize() {
		Dimension dim = super.getMinimumSize();
		if(textWhenEmpty!=null) {
			int minWidth = Math.max(60, getFontMetrics(getFont()).stringWidth(textWhenEmpty)+40);
			if(dim.width<minWidth) dim.width = minWidth;
		}
		return dim;
	}
			
	@Override
	public Dimension getPreferredSize() {
		Dimension dim = super.getPreferredSize();
		if(textWhenEmpty!=null) {
			int minWidth = Math.max(60, getFontMetrics(getFont()).stringWidth(textWhenEmpty)+40);
			if(dim.width<minWidth) dim.width = minWidth;
		}
		return dim;
	}
	
	/**
	 * Custom renderer
	 * To be overidden
	 * @return null to use the default renderer 
	 */
	public Component processCellRenderer(JLabel comp, T value, int index) {
		return comp;
	}
	
	
	
}
