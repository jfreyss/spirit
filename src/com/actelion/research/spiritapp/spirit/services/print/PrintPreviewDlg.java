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

package com.actelion.research.spiritapp.spirit.services.print;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.util.ui.JEscapeDialog;

public class PrintPreviewDlg extends JEscapeDialog {

	public PrintPreviewDlg(final Printable printable) {
		super((JFrame) null, "Print Preview", true);
		
		JPanel previewPane = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				try {
					printable.print(g, new PageFormat(), 0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(612, 792);
			}
		};
		previewPane.setOpaque(true);
		previewPane.setBackground(Color.WHITE);
		previewPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		JButton printButton = new JButton("Print");
		Box buttons = Box.createHorizontalBox();
		buttons.add(printButton);
		printButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, new JScrollPane(previewPane));
		contentPane.add(BorderLayout.NORTH, buttons);
		
		
		
		setContentPane(contentPane);
		setSize(800, 600);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}
}
