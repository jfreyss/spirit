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

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

public class MapNode extends AbstractNode<String[]> {
	
	private JComboBox<String> keyComboBox;
	private JTextField valueTextField = new JTextField(8);
	private DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
	private JPanel res = new JPanel();
	
	public MapNode(FormTree tree, Strategy<String[]> strategy) {
		this(tree, new ArrayList<String>(), strategy);
	}
	public MapNode(FormTree tree, List<String> keys, Strategy<String[]> strategy) {
		super(tree, null, strategy);
		keyComboBox = new JComboBox<>(model);
		setKeys(keys);
		keyComboBox.setFont(editFont);
		valueTextField.setFont(editFont);
		AutoCompleteDecorator.decorate(keyComboBox);
		valueTextField.setFont(valueTextField.getFont().deriveFont(9f));
		keyComboBox.setFont(valueTextField.getFont().deriveFont(9f));
		keyComboBox.setBorder(null);
		
		keyComboBox.setPreferredSize(new Dimension(80, 20));
		keyComboBox.setMaximumSize(new Dimension(150, 20));
		
		valueTextField.setPreferredSize(new Dimension(80, 20));
		valueTextField.setMaximumSize(new Dimension(150, 20));
		
		valueTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				getTree().firePropertyChange(FormTree.PROPERTY_CHANGED, false, true);
			}
		});
		
		
		res.setLayout(new BoxLayout(res, BoxLayout.X_AXIS));
		res.add(keyComboBox);
		res.add(new JLabel(":"));
		res.add(valueTextField);
		res.setOpaque(false);
		
	}

	public void setKeys(List<String> keys) {
		model.removeAllElements();
		model.addElement("");
		for (String key : keys) {
			model.addElement(key);					
		}
	}
	
	@Override
	public JComponent getComponent() {
		return res;
	}
	
	@Override
	protected boolean isFilled() {
		return keyComboBox.getSelectedIndex()>0 && valueTextField.getText().length()>0;
	}

	@Override
	protected void updateModel() {
		strategy.setModel(new String[] {(String)keyComboBox.getSelectedItem(), valueTextField.getText()});

	}

	@Override
	protected void updateView() {
		String[] v = (String[]) strategy.getModel();
		keyComboBox.setSelectedItem(v[0]);
		valueTextField.setText(v[1]);
	}

	@Override
	public JComponent getFocusable() {
		return valueTextField;
	}
	
}
