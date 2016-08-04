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

package com.actelion.research.util.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;

import com.actelion.research.util.FormatterUtils;



/**
 * TextField for the date, with a TODAY button
 * the date can be yyyy, MM.yyyy, dd.MM.yyyy
 * @author freyssj
 *
 */
public class DateTextField extends JCustomTextField {

	private JButton button = new JButton("Today");

	
	public DateTextField(boolean showToday) {
		super(JCustomTextField.DATE);
		setColumns(8);
		setTextWhenEmpty("dd.MM.yyyy");
		setToolTipText("Date: yyyy or MM.yyyy or dd.MM.yyyy or Time: dd.MM.yyyy HH:mm:ss");
		setLayout(null);
		
		button.setFont(FastFont.SMALLER_CONDENSED);
		button.setBorder(null);
		button.setToolTipText("Find Sample");
		button.setVisible(showToday);
		add(button);
		
		button.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				setText(FormatterUtils.cleanDateTime(FormatterUtils.formatDate(new Date())));
			}
		});
		
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				cleanDate();
			}
		});
	}
	
	private void cleanDate() {
		String s = FormatterUtils.cleanDateTime(super.getText());
		if(s!=null) super.setText(s);
	}
	
	@Override
	public void doLayout() {
		if(button.isVisible()) button.setBounds(getWidth()-29, 2, 28, getHeight()-4);
	}
	
	
	
	@Override
	public void setBorder(Border border) {
		if(border==null) return;
		super.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(0, 0, 0, 8)));			
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		button.setEnabled(enabled);
		super.setEnabled(enabled);
	}


	
}
