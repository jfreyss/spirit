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

package com.actelion.research.spiritapp.spirit.ui.util.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.UIUtils;

public class JColorChooserButton extends JButton {

	private Color color = Color.WHITE;
	
	public JColorChooserButton(final boolean background) {
		super("    ");
		addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {				
				ColorDialog colorChooser = new ColorDialog(color, background);
				setColor(colorChooser.getColor());				
			}
		});
	}
	
	public void setColor(Color color) {
		this.color = color;
		setBackground(color);
	}
	
	public Color getColor() {
		return color;
	}
	
	public Integer getColorRgb() {
		return color==null? null: color.getRGB();
	}
	
	public void setColorRgb(Integer rgb) {
		setColor(rgb==null? null: new Color(rgb));
	}

	private static class ColorDialog extends JEscapeDialog {
		private Color color;
		public ColorDialog(Color selection, final boolean background) {
			super(UIUtils.getMainFrame(), "Please select a " + (background?"background": "foreground") + " color");
			this.color = selection==null? Color.LIGHT_GRAY: selection;
			assert color!=null;
			
			int nx = background? 15: 12;
			int ny = background? 7: 1;
			int size = 48;
			int offset = 2;
			JPanel panel = new JPanel(null);
			panel.setPreferredSize(new Dimension((size+offset)*nx+offset, (size+offset)*ny+offset));
			panel.setBackground(Color.WHITE);
			panel.setOpaque(true);
			ButtonGroup gr = new ButtonGroup();
			
			JToggleButton closestButton = null;
			double closestDist = 1000000;
			
			for(int y=0; y<ny; y++) {
				for(int x=0; x<nx; x++) {
					float h;
					float s = .85f*y/(ny-1);
					float b;
					h = 1f*x/(nx);
					if(x>y*nx/ny) {
						b = 1 - .6f*(x-y)/(nx-y);
					} else {
						b = 1f;
					}
					if(!background) {
						s = 1;
					}
					final Color c = Color.getHSBColor(h, s, b);
					
					
					
					JToggleButton button = new JToggleButton("Txt");
					button.setFont(FastFont.REGULAR);
					button.setMargin(new Insets(0,0,0,0));
					if(background) {
						button.setBackground(c);
					} else {
						button.setForeground(c);
					}
					gr.add(button);
					button.addActionListener(new ActionListener() {							
						@Override
						public void actionPerformed(ActionEvent e) {
							color = c;
							dispose();
						}
					});
					
					int total = (size+offset)*nx;
					int margin = ((int)(1.0 * ((size+offset) * (nx)) - total)) / 2;
					
					button.setBounds(margin + x * total / nx, offset+(size+offset)*y, size, size);
					panel.add(button);
					
					double dist = Math.abs(color.getRed()-c.getRed())*Math.abs(color.getRed()-c.getRed())
							+ Math.abs(color.getGreen()-c.getGreen())*Math.abs(color.getGreen()-c.getGreen())
							+ Math.abs(color.getBlue()-c.getBlue())*Math.abs(color.getBlue()-c.getBlue());
					if(dist<closestDist) {
						closestDist = dist;
						closestButton = button;
					}
					
				}				
			}
			
			
			panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(10, 10, 10, 10) ));
			add(BorderLayout.CENTER, panel);
			
			
			pack();
			setLocationRelativeTo(UIUtils.getMainFrame());
			
			
			
			final JToggleButton toFocus = closestButton;
			toFocus.setSelected(true);
			
			SwingUtilities.invokeLater(new Runnable() {				
				@Override
				public void run() {
					try{Thread.sleep(10);}catch(Exception e){}
					
					toFocus.grabFocus();
				}
			});
			
			setVisible(true);
		}
		public Color getColor() {
			return color;
		}
	}
	
	
}
