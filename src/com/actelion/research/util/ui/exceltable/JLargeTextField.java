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

package com.actelion.research.util.ui.exceltable;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.actelion.research.util.ui.UIUtils;

/**
 * JLargeTextField is used to edit large text (>4000 characters)
 * @author freyssj
 *
 */
public class JLargeTextField extends JPanel {
	private String text = "";
	private JTextField textField = new JTextField(20);
	private JButton button = new JButton(".");
	
	public JLargeTextField() {
		super(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0; c.weightx = 1; add(textField, c);
		c.gridx = 1; c.weightx = 0; add(button, c);
		
		textField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				openPopup();
			}
		});
		textField.setEditable(false);
		button.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				openPopup();				
			}
		});
	}
	
	private void openPopup() {
		JTextArea ta = new JTextArea(25, 45);
		ta.setText(text);
		ta.setCaretPosition(0);
		JScrollPane sp = new JScrollPane(ta);
		int res = JOptionPane.showOptionDialog(UIUtils.getMainFrame(), sp, "Text Editor", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
		if(res==JOptionPane.YES_OPTION) {
			textField.setText(ta.getText());
			text = ta.getText();
		}
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text==null?"": text;
		textField.setText(text);
	}
}