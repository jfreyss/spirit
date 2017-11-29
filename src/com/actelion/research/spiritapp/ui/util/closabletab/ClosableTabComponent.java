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

package com.actelion.research.spiritapp.ui.util.closabletab;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicButtonUI;

public class ClosableTabComponent extends JPanel {
	private final JClosableTabbedPane pane;

	public ClosableTabComponent(final JClosableTabbedPane pane) {
		// unset default FlowLayout' gaps
		super(new FlowLayout(FlowLayout.LEFT, 0, 0));
		if (pane == null) {
			throw new NullPointerException("TabbedPane is null");
		}
		this.pane = pane;
		setOpaque(false);

		// make JLabel read titles from JTabbedPane
		JLabel label = new JLabel("<html>text</html>") {
			@Override
			public String getText() {
				int i = pane.indexOfTabComponent(ClosableTabComponent.this);
				if (i != -1) {
					super.setText(pane.getTitleAt(i));
					return super.getText(); //trick to allow html
					//return pane.getTitleAt(i);
				}
				return null;
			}
		};
		
		add(label);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
		// tab button
		JButton button = new TabButton();
		add(button);
		setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
	}
	
	private class TabButton extends JButton implements ActionListener {
		public TabButton() {
			int size = 17;
			setPreferredSize(new Dimension(size, size));
			setUI(new BasicButtonUI());
			setContentAreaFilled(false);
			setFocusable(false);
			setBorder(BorderFactory.createEtchedBorder());
			setBorderPainted(false);
			setRolloverEnabled(true);
			setToolTipText("Close");
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int i = pane.indexOfTabComponent(ClosableTabComponent.this);
			if (i != -1) {
				pane.closeTab(i);
			}
		}

		// we don't want to update UI for this button
		@Override
		public void updateUI() {
		}

		// paint the cross
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			// shift the image for pressed buttons
			if (getModel().isPressed()) {
				g2.translate(1, 1);
			}
			g2.setStroke(new BasicStroke(2));
			g2.setColor(Color.BLACK);
			if (getModel().isRollover()) {
				g2.setColor(Color.MAGENTA);
			}
			int delta = 6;
			g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
			g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
			g2.dispose();
		}
	}
	
}
