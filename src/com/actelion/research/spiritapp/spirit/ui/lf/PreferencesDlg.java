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

package com.actelion.research.spiritapp.spirit.ui.lf;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class PreferencesDlg extends JEscapeDialog {

	//	private JGenericComboBox<String> lfComboBox = new JGenericComboBox<>(new String[]{"Nimbus", "System"}, false);
	//	private JGenericComboBox<String> fontFamilyComboBox = new JGenericComboBox<>(new String[]{"Segoe UI", "Arial", "Times"}, false);
	private JGenericComboBox<Integer> fontSizeComboBox = new JGenericComboBox<>(new Integer[]{8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 21, 22, 23, 24}, false);

	public PreferencesDlg() {
		super(UIUtils.getMainFrame(), "User Preferences");

		//		lfComboBox.setSelection(Spirit.getConfig().getProperty("preferences.lf", "Nimbus"));
		//		fontFamilyComboBox.setSelection(Spirit.getConfig().getProperty("preferences.fontFamily", FastFont.getDefaultFontFamily()));
		fontSizeComboBox.setSelection(Spirit.getConfig().getProperty("preferences.fontSize", FastFont.getDefaultFontSize()));
		fontSizeComboBox.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				if(index>0 && (value instanceof Integer)) {
					setFont(FastFont.getFont((Integer) value));
				}
				return this;
			}
		});

		JButton okButton = new JButton("Update Preferences");
		okButton.addActionListener(e->ok());
		getRootPane().setDefaultButton(okButton);

		add(BorderLayout.CENTER, UIUtils.createBox(
				UIUtils.createTitleBox("Appearance", UIUtils.createTable(
						//						new JLabel("Look & Feel: "), lfComboBox,
						//						new JLabel("Font Family: "), fontFamilyComboBox,
						new JLabel("Font Size: "), fontSizeComboBox
						)),
				null,
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new CloseAction()), okButton)));

		UIUtils.adaptSize(this, -1, -1);
		setVisible(true);
	}

	private void ok() {
		//		Spirit.getConfig().setProperty("preferences.lf", lfComboBox.getSelection());
		//		Spirit.getConfig().setProperty("preferences.fontFamily", fontFamilyComboBox.getSelection());
		Spirit.getConfig().setProperty("preferences.fontSize", fontSizeComboBox.getSelection());
		dispose();
		SpiritChangeListener.fireModelChanged(SpiritChangeType.LOGIN);
	}

}
