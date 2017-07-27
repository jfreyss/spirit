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

package com.actelion.research.spiritapp.spirit.ui.util.lf;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.UIUtils;

public class UserIdTextArea extends JPanel {

	private final JTextArea textArea;
	private final UserIdComboBox userId1ComboBox = new UserIdComboBox();
	private JButton add1Button = new JButton("Add");
	private JButton remove1Button = new JButton("Remove");


	public UserIdTextArea(int cols, int rows) {
		super(new BorderLayout());
		textArea = new JTextArea(cols, rows);

		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		textArea.addCaretListener(e-> {
			if(!textArea.isFocusOwner()) return;
			int c = e.getDot();
			String text = textArea.getText();
			text = text.replaceAll("["+MiscUtils.SPLIT_SEPARATORS_WITH_SPACE+"]", " ");
			int index1 =  text.lastIndexOf(" ", c-1)+1;
			int index2 =  text.indexOf(" ", c);
			if(index1<0) index1 = 0;
			if(index2<0) index2 = text.length();
			if(index1>=index2) {
				userId1ComboBox.setText("");
				return;
			}
			String name = text.substring(index1, index2);

			userId1ComboBox.setText(name);
		});

		add1Button.addActionListener(e-> {
			updateUsers(userId1ComboBox.getText(), null);
		});
		remove1Button.addActionListener(e-> {
			updateUsers(null, userId1ComboBox.getText());
		});
		userId1ComboBox.addActionListener(e-> {
			updateUsers(userId1ComboBox.getText(), null);
		});


		add(BorderLayout.CENTER, new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalStrut(10), new JLabel("Add: "), userId1ComboBox, add1Button, remove1Button));

	}

	private void updateUsers(String toAdd, String toRemove) {
		Set<String> set = new TreeSet<String>(Arrays.asList(MiscUtils.split(textArea.getText())));
		if(toAdd!=null) set.add(toAdd);
		if(toRemove!=null) set.remove(toRemove);
		int scrollTo = -1;

		if(toRemove!=null) {
			scrollTo = textArea.getText().indexOf(toRemove);
		}

		textArea.setText(MiscUtils.flatten(set, ", "));

		if(toAdd!=null) {
			scrollTo = textArea.getText().indexOf(toAdd);
		}

		if(scrollTo>=0) textArea.setCaretPosition(scrollTo);
	}

	@Override
	public void setEnabled(boolean enabled) {
		textArea.setEnabled(enabled);
		userId1ComboBox.setEnabled(enabled);
		add1Button.setEnabled(enabled);
		remove1Button.setEnabled(enabled);

	}

	public void setText(String s) {
		textArea.setText(s);

		//And clean
		updateUsers(null, null);
	}
	public String getText() {
		return textArea.getText();
	}

}
