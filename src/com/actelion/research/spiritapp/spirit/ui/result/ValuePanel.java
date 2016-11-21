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

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.exceltable.JComponentNoRepaint;

public class ValuePanel extends JComponentNoRepaint {
	
	private final Dimension preferredDim = new Dimension(80, 16);
	private OutputType outputType;
	private String attLabel;
	private String valLabel;
	private Biosample linked;
//	private Document doc;
	
	public ValuePanel() {		
	}
	
	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		Color valColor = Color.BLACK;
		
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
			//Draw Val
			g.setColor(valColor);
			g.setFont(outputType==OutputType.OUTPUT? FastFont.REGULAR: FastFont.REGULAR_CONDENSED);
			int w = g.getFontMetrics().stringWidth(valLabel);			
			if(outputType==OutputType.OUTPUT) {
				g.clearRect(width - 1 - w, 0, w, height);
				g.drawString(valLabel, width - 1 - w, height/2 + g.getFontMetrics().getMaxDescent());				
			} else {
				g.clearRect(width - 1 - 1 - w, 0, w, height);
				g.drawString(valLabel, w1+1, height/2 + g.getFontMetrics().getMaxDescent());				
			}
		}
		
		
	}
	
	public void setValue(OutputType outputType, ResultValue v) {
		this.outputType = outputType;
		if(v==null) {
//			this.doc = null;
			this.linked = null;
			this.attLabel = "";
			this.valLabel = "";
		} else {
			String val = v.getAttribute().getDataType()==DataType.LARGE? (v.getLinkedDocument()==null? null: new String(v.getLinkedDocument().getBytes())): v.getValue();
//			this.doc = v.getAttribute().getDataType()==DataType.D_FILE? v.getLinkedDocument(): null;
			this.linked = v.getAttribute().getDataType()==DataType.BIOSAMPLE? v.getLinkedBiosample(): null;			
			this.attLabel = v.getAttribute().getName() + ":";
			this.valLabel = val==null?"": val;			
		}
		preferredDim.width = getFontMetrics(FastFont.SMALLER).stringWidth(attLabel + ":") + getFontMetrics(outputType==OutputType.OUTPUT? FastFont.REGULAR: FastFont.REGULAR_CONDENSED).stringWidth(valLabel);
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
