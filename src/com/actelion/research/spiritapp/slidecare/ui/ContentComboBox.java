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

package com.actelion.research.spiritapp.slidecare.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class ContentComboBox extends JGenericComboBox<String> {
	
	private Map<String, Integer> content2count = new HashMap<>();
	
	public ContentComboBox() {
		
	}
	
	public void setContent2count(Map<String, Integer> content2count) {
		this.content2count = content2count;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(160, 50);
	}
	
	private JLabelNoRepaint lbl = new JLabelNoRepaint() {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			Integer count = content2count.get(getText());
			if(count!=null) {
				String s = ""+count;
				g.setFont(FastFont.BOLD);
				g.setColor(Color.BLUE);
				g.drawString(s, getWidth()-g.getFontMetrics().stringWidth(s)-10, 12);
			}
		}
	};
	
	@Override
	public Component processCellRenderer(JLabel comp, String value, int index) {
		lbl.setText(value);
		lbl.setForeground(comp.getForeground());
		lbl.setBackground(comp.getBackground());
		lbl.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
		return lbl;
	}

}
