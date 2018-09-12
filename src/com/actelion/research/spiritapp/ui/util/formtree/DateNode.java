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

package com.actelion.research.spiritapp.ui.util.formtree;

import javax.swing.JComponent;

import com.actelion.research.spiritapp.ui.util.DatePicker;

public class DateNode extends AbstractNode<String> {
	
	private DatePicker datePicker = new DatePicker();

	public DateNode(FormTree tree, String label) {
		this(tree, label, null);
	}
	public DateNode(FormTree tree, String label, Strategy<String> strategy) {	
		super(tree, label, strategy);
		datePicker.setTextWhenEmpty(label);
		datePicker.setToolTipText(label);
		addEventsToComponent();
	}	
	
	@Override
	public JComponent getComponent() {		
		return datePicker;
	}	
	
	@Override
	protected void updateModel() {
		if(strategy!=null) strategy.setModel(datePicker.getText());
	}
	@Override
	protected void updateView() {
		if(strategy!=null) {
			datePicker.setText(strategy.getModel());
		}
	}
	@Override
	protected boolean isFilled() {
		// TODO Auto-generated method stub
		return datePicker.getText().length() > 0;
	}
	@Override
	public JComponent getFocusable() {
		// TODO Auto-generated method stub
		return datePicker;
	}
	
}
