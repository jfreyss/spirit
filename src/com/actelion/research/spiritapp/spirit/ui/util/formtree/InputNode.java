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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JTextField;

import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.QueryTokenizer;
import com.actelion.research.util.ui.JCustomTextField;

public class InputNode extends AbstractNode<String> {

	protected final JCustomTextField textField = new JCustomTextField();

	public InputNode(FormTree tree, String label, final Strategy<String> accessor) {
		this(tree, null, label, accessor);
	}

	public InputNode(FormTree tree, FieldType fieldType, String label, final Strategy<String> accessor) {

		super(tree, label, accessor);

		textField.setFont(editFont);
		textField.addActionListener(e-> {
			textField.selectAll();
			getTree().firePropertyChange(FormTree.PROPERTY_SUBMIT_PERFORMED, false, true);
			if(strategy!=null) strategy.onAction();
		});

		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				getTree().firePropertyChange(FormTree.PROPERTY_CHANGED, false, true);
				if(strategy!=null) strategy.onChange();
			}
		});


		textField.setTextWhenEmpty(label);
		textField.setToolTipText("<html> <b>" + MiscUtils.removeHtml(label) + "</b><br>"
				+ (fieldType==FieldType.OR_CLAUSE? QueryTokenizer.getHelp(false): fieldType==FieldType.AND_CLAUSE? QueryTokenizer.getHelp(true): "") + "</html>");
		addEventsToComponent();
	}



	@Override
	public JComponent getComponent() {
		return textField;
	}

	public JTextField getTextField() {
		return textField;
	}

	@Override
	protected void updateModel() {
		strategy.setModel(textField.getText());
	}
	@Override
	protected void updateView() {
		textField.setText(strategy==null || strategy.getModel()==null?"":strategy.getModel().toString());
	}
	@Override
	protected boolean isFilled() {
		return textField.getText().length()>0;
	}

	@Override
	public JComponent getFocusable() {
		return textField;
	}

}
