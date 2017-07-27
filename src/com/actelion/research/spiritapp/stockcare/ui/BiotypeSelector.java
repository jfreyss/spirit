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

package com.actelion.research.spiritapp.stockcare.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.ui.util.icons.ImageFactory;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.UIUtils;

public class BiotypeSelector {
	private static Biotype selection = null;
	public static Biotype createBiotypeSelectorDlg(Frame top, String title, Biotype[] biotypes) {
		selection = null;
		final JEscapeDialog dlg = new JEscapeDialog(top, "Select Biotype", true);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 0));
		for (final Biotype b : biotypes) {
			JButton button = new JButton(b.getName(), new ImageIcon(ImageFactory.getImageThumbnail(b)));
			button.setMinimumSize(new Dimension(60, 30));
			button.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					selection = b;
					dlg.dispose();
				}
			});
			buttonPanel.add(button);
		}
		
		JLabel topLabel = new JCustomLabel(title, FastFont.BOLD);
		topLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.NORTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), topLabel, Box.createHorizontalGlue()));
		contentPane.add(BorderLayout.CENTER, buttonPanel);
		
		dlg.setLocationRelativeTo(top);
		dlg.setContentPane(contentPane);
		dlg.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		dlg.pack();
		dlg.setResizable(false);
		dlg.setVisible(true);
		
		return selection;
	}
}
