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

package com.actelion.research.spiritapp.spirit.ui.result;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.exceltable.JComponentNoRepaint;

public class ValuePanel extends JComponentNoRepaint {
	
	private final Dimension preferredDim = new Dimension(80, 16);
	
	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		Color valColor = isOutput? LF.COLOR_TEST: Color.BLACK;
		
		int width = getWidth();
		int height = getHeight();
		
		g.setBackground(getBackground());
		g.clearRect(0, 0, width, height);
		
		int w1 = 0;
		g.setColor(Color.GRAY);
		g.setFont(FastFont.SMALLER);
		g.drawString(attLabel, 1, 3 + g.getFontMetrics().getHeight());
		w1 += g.getFontMetrics().stringWidth(attLabel) + 4;
		
		
		if(linked!=null) {
			//Draw biosample
			g.setFont(FastFont.REGULAR);
			g.setColor(valColor);
			
			g.clearRect(w1 + 1, 0, width, height);
			g.drawString(valLabel, w1 + 1, height/2 -4 + g.getFontMetrics().getMaxDescent());
			
			if(linked.getBiotype()!=null && linked.getBiotype().getSampleNameLabel()!=null) {
				g.setColor(Color.MAGENTA);
				g.drawString(linked.getSampleName(), w1 + 1, height/2 + 6 + g.getFontMetrics().getMaxDescent());
			}
		} else if(valLabel!=null && valLabel.length()>0){ 
			//Draw Unit
			g.setFont(FastFont.SMALLER);
			g.setColor(valColor);
			int w2 = width-2-g.getFontMetrics().stringWidth(unitLabel);
			g.drawString(unitLabel, w2, height/2 + g.getFontMetrics().getMaxDescent());
			
			//Draw Val
			g.setFont(isOutput? FastFont.REGULAR: FastFont.REGULAR_CONDENSED);
			int w = g.getFontMetrics().stringWidth(valLabel);			
			if(isOutput) {
				g.clearRect(w2 - 1 - w, 0, w, height);
				g.drawString(valLabel, w2 - 1 - w, height/2 + g.getFontMetrics().getMaxDescent());				
			} else {
				g.clearRect(w2 - 1 - w, 0, w, height);
				g.drawString(valLabel, w1+1, height/2 + g.getFontMetrics().getMaxDescent());				
			}
		}
		
		
	}
	
	private boolean isOutput;
	private String attLabel;
	private String valLabel;
	private String unitLabel;
	private Biosample linked;
	
	public void setValue(boolean isOutput, ResultValue v) {
		if(v==null) {
			this.isOutput = isOutput;
			this.linked = null;
			this.attLabel = "";
			this.valLabel = "";
			this.unitLabel = "";
		} else {
			String val = v.getValue();
			String unit = v.getUnit();
			this.isOutput = isOutput;
			this.linked = v.getLinkedBiosample();
			if(!isOutput) {
				val = v.getValueWithoutDelegateUnit();
			}  
			
			this.attLabel = (v.getAttribute()==null? "??" : v.getAttribute().getName()) + ":";
			this.valLabel = val==null?"": val;
			
			if(val!=null && unit!=null && unit.length()>0) {
				this.unitLabel = " " +unit;
			} else {
				this.unitLabel = " ";
			}
		}
		preferredDim.width = getFontMetrics(FastFont.SMALLER).stringWidth(attLabel + ":") + getFontMetrics(isOutput? FastFont.REGULAR: FastFont.REGULAR_CONDENSED).stringWidth(valLabel + unitLabel);
	}
	
	@Override
	public Dimension getMinimumSize() {		
		return preferredDim;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return preferredDim;
	}
	
	
	

}
