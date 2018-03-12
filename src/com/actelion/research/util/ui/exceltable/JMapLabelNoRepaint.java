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

package com.actelion.research.util.ui.exceltable;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;
import java.util.Map.Entry;

import com.actelion.research.util.ui.FastFont;

public class JMapLabelNoRepaint extends JComponentNoRepaint {
	
	private Map<String, String> map;
	private int separatorPixel = 18;
	
	public JMapLabelNoRepaint() {}
	
	public void setSeparatorPixel(int separatorPixel) {
		this.separatorPixel = separatorPixel;
	}

	
	public void setMap(Map<String, String> map) {
		this.map = map;
		prefDim = null;
	}
	
	private Dimension prefDim = null;
	
	private Dimension calculateDim(Font font) {
		Dimension dim = new Dimension();
		dim.height = font.getSize()*2+1;
		if(map==null) {
			dim.width = 80;
		} else {
			int x = 1;
			for (Entry<String, String> entry : map.entrySet()) {
				if(entry.getKey()==null || entry.getValue()==null || entry.getValue().length()==0) continue;
				
				int w = Math.max(20, getFontMetrics(font).stringWidth(entry.getValue()))+4;
				x+=w;
				x+=(separatorPixel-x%separatorPixel)%separatorPixel; //next multiple of separatorPixel
			}
			
			dim.width = x;
		}
		return dim;
	}
	
	@Override
	public Dimension getPreferredSize() {		
		if(prefDim==null) {
			prefDim = calculateDim(FastFont.REGULAR);
		}
		return prefDim;		
	}
	
	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}
	
	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}
	
	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		super.paintComponent(g);

		if(isOpaque()) {
			g.setBackground(getBackground());
			g.clearRect(0, 0, getWidth(), getHeight());
		}
		if(map==null) return;
		
		
		int x = 1;
		Font keyFont = FastFont.SMALL;
		Font valueFont = FastFont.REGULAR;
		for (Entry<String, String> entry : map.entrySet()) {
			if(entry.getKey()==null || entry.getValue()==null) continue;
			g.setColor(getBackground());
			g.clearRect(x-4, 0, getWidth(), getHeight());
			g.setColor(Color.GRAY);
			g.setFont(keyFont);
			g.drawString(entry.getKey(), x, keyFont.getSize()-1);

			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(x-2, 2, x-2, getHeight()-3);
			
			g.setColor(getForeground());
			g.setFont(valueFont);
			g.drawString(entry.getValue(), x, keyFont.getSize()+valueFont.getSize());
			int w = Math.max(20, g.getFontMetrics().stringWidth(entry.getValue()))+4;			
			x+=w;
			x+=(separatorPixel-x%separatorPixel)%separatorPixel; //next multiple of separatorPixel
		}
	}
}
