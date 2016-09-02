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

package com.actelion.research.spiritapp.spirit.ui.biosample;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.SwingConstants;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.icons.ImageFactory;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JComponentNoRepaint;

/**
 * Label that shows the Id of the sample:
 * - icon (always shown)
 * - sampleId (optional)
 * - sampleName (optional)
 * - group/phase (optional)
 * @author freyssj
 *
 */
public class SampleIdLabel extends JComponentNoRepaint {
	private static final int iconW = 20;

	private Biosample biosample;
	private boolean displayName;
	private boolean displayGroup;
	private Dimension preferredDim = new Dimension(80, 20);
	private boolean highlight = false;
	private boolean error = false;
	private BiosampleLinker extraDisplay = null;
	private boolean extraSameLine = true;
	private int sizeIncrement;
	private int verticalAlignment = SwingConstants.TOP;
	
	public SampleIdLabel(boolean displayName, boolean displayGroup) {
		this.displayName = displayName;
		this.displayGroup = displayGroup;
		setForeground(Color.black);
		setOpaque(true);
	}
	
	/**
	 * SampleIdLabel which displays everything: the sampleid, samplename, group
	 */
	public SampleIdLabel() {	
		this(null);
	}
	public SampleIdLabel(Biosample biosample) {
		this(biosample, true, true);	
	}
	public SampleIdLabel(Biosample biosample, boolean displayName, boolean displayGroup) {
		this(displayName, displayGroup);	
		setBiosample(biosample);		
	}
	public void setBiosample(Biosample biosample) {
		this.biosample = biosample;
		recomputePreferredWidth();
		
	}
	private void recomputePreferredWidth() {
		int preferredWidth;
		int preferredHeight = 24 + sizeIncrement*2;
		if(biosample==null) {
			preferredWidth = 75;
		} else {
			int textWidth;

			boolean paintName = displayName && (biosample.getBiotype()==null || biosample.getBiotype().getSampleNameLabel()!=null);
			int sampleIdWidth = getFontMetrics(FastFont.REGULAR.increaseSize(sizeIncrement)).stringWidth(biosample.getSampleId()==null?"": biosample.getSampleId());
			if(paintName) {
				int nameWidth = getFontMetrics(highlight? FastFont.BOLD: FastFont.REGULAR).stringWidth(biosample.getSampleName()==null || biosample.getSampleName()==null?"": biosample.getSampleName());
				if(extraDisplay!=null && !extraSameLine) {
					nameWidth += getFontMetrics(FastFont.MEDIUM).stringWidth(extraDisplay.getValue(biosample)==null?"":extraDisplay.getValue(biosample));
				}
				textWidth = Math.max(sampleIdWidth, nameWidth);
			} else {
				textWidth = sampleIdWidth;				
			}			
			
			preferredWidth =  iconW + 4 + textWidth + (displayGroup && biosample.getInheritedGroup()!=null?32:0);
			
			
			if(preferredWidth>200) preferredWidth = 200;
		}
		if(extraDisplay!=null && !extraSameLine) {
			preferredHeight+=15;
			
		}

		preferredDim = new Dimension(preferredWidth, preferredHeight);
	}

	public void setExtraDisplay(BiosampleLinker extraLinker, boolean sameLine) {
		this.extraDisplay = extraLinker;
		this.extraSameLine = sameLine;
	}
		
	public void setBiosample(Biotype type, String text) {
		setBiosample(new Biosample(type, text));		
		setToolTipText(null);
	}
	
	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getPreferredSize() {
		return preferredDim; //new Dimension(preferredWidth, 20 + sizeIncrement*2 + (extraDisplay!=null && !extraSameLine /*&& extraDisplay.getValue(biosample)!=null*/? 15 + sizeIncrement: 0) );
	}
	
	@Override
	public Dimension getMaximumSize() {
		return super.getPreferredSize();
	}
	
	public int getVerticalAlignment() {
		return verticalAlignment;
	}
	public void setVerticalAlignment(int verticalAlignment) {
		this.verticalAlignment = verticalAlignment;
	}
	
	@Override
	protected void paintComponent(Graphics graphics) {		
		
		Graphics2D g = (Graphics2D) graphics;
		Insets insets = getInsets();
		

		if(isOpaque()) {
			g.setBackground(getBackground());
			g.clearRect(0, 0, getWidth(), getHeight());			
		}		
		
		if(!isVisible() || biosample==null) return;
		
		String extraDisplayValue = extraDisplay==null || extraDisplay.getValue(biosample)==null || extraDisplay.getValue(biosample).length()==0? null: extraDisplay.getValue(biosample);		
		boolean paintName = displayName && (biosample.getBiotype()==null || biosample.getBiotype().getSampleNameLabel()!=null || extraDisplayValue!=null);
		boolean paintGroup = displayGroup;
		Color fgColor = biosample!=null && (error || biosample.getId()<0)? LF.COLOR_ERROR_FOREGROUND: Color.BLACK;
		
		//Quality?
		if(biosample.getQuality()!=null && biosample.getQuality().getBackground()!=null) {
			g.setColor(biosample.getQuality().getBackground());
			g.fillRect(iconW+1, 1, getWidth(), getHeight()-3);	
		}
		

		//Status
		if(biosample.getStatus()!=null) {
			if(biosample.getStatus().getBackground()!=null) {
				g.setColor(biosample.getStatus().getBackground());
				g.fillRect(1, 1, iconW, getHeight()-3);		
			}			
		}		
		
		
		//Some checks on the display 
		
		int x = iconW;		
		int y = verticalAlignment==SwingConstants.TOP? 1: 1 + (getHeight()-24)/2;		
		
		//Name
		int x2 = x;
		if(paintName) {
			FastFont f = highlight? FastFont.BOLD: FastFont.REGULAR; 
			String name = biosample.getSampleName();
			if(biosample.getBiotype()!=null && biosample.getBiotype().getSampleNameLabel()!=null && name!=null) {
				g.setFont(f.increaseSize(sizeIncrement));
				g.setColor(fgColor);
				g.drawString(name, x, y + 20 + sizeIncrement*2);
				x2 = x+g.getFontMetrics().stringWidth(name);
			}
		}
		//Paint extra display
		if(extraDisplayValue!=null) {
			Biosample linked = extraDisplay.getLinked(biosample);
			if(linked!=biosample && linked.getBiotype()!=null) {
				Image img = ImageFactory.getImage(linked, iconW);
				if(img!=null) {
					g.drawImage(img, 1, 20, this);
				}				
			}
			g.setFont(FastFont.REGULAR.increaseSize(sizeIncrement));
			g.setColor(UIUtils.getColor(84, 90, 167));
			
			if(extraSameLine) {
				g.drawString((biosample.getSampleName()!=null? " | ": "") + extraDisplayValue, x2, 20+sizeIncrement*2);					
			} else {
				g.drawString(extraDisplayValue, iconW, 34+sizeIncrement*3);
			}				
		}
			
		//SampleId
		FastFont f = paintName?FastFont.SMALL: highlight? FastFont.BOLD: FastFont.REGULAR;
		String s = biosample.getSampleId()==null?"": biosample.getSampleId();
		g.setFont(f.increaseSize(sizeIncrement));
		if(biosample.getBiotype()!=null && biosample.getBiotype().isHideSampleId() && paintName) {
			g.setColor(Color.LIGHT_GRAY);
		} else {
			g.setColor(fgColor);
		}				
		g.drawString(s, x, y + 15 + sizeIncrement - (paintName? 7:0));
		x += g.getFontMetrics().stringWidth(s)+2;
		
		//Group & phase
		if(paintGroup && biosample.getInheritedGroup()!=null && !SpiritRights.isBlindAll(biosample.getInheritedGroup().getStudy(), Spirit.getUser())) {				

			//Group Background				
			Font font1 = FastFont.REGULAR_CONDENSED.increaseSize(sizeIncrement);
			Font font2 = FastFont.SMALL_CONDENSED.increaseSize(sizeIncrement);
			Font font3 = FastFont.REGULAR_CONDENSED.increaseSize(sizeIncrement);
			
			String s1 = biosample.getInheritedGroup().getShortName();
			String s2 = biosample.getInheritedGroup().getNSubgroups()>1? "'" + (biosample.getInheritedSubGroup()+1): "";
			
			String s3 = biosample.getInheritedPhase()!=null? " "+biosample.getInheritedPhase().getShortName(): "";
			if(s3.length()>9) s3 = s3.substring(0,9); 

			
			int w1 = g.getFontMetrics(font1).stringWidth(s1);				
			int w2 = g.getFontMetrics(font2).stringWidth(s2);
			int w3 = g.getFontMetrics(font3).stringWidth(s3);
			if(w3<18) w3=18;
			int x1 = getWidth() - insets.right - w1 - w2 - w3 - 2;

			g.setColor(UIUtils.getDilutedColor(biosample.getInheritedGroup()==null?Color.WHITE: biosample.getInheritedGroup().getColor(), getBackground()));
			g.fillRect(x1-2, 0, y + w1+w2+w3+3, 13+sizeIncrement);
					
			g.setFont(font1);
			g.setColor(Color.BLACK);					
			g.drawString(s1, x1, y+11+sizeIncrement);
			
			g.setFont(font2);
			g.setColor(Color.DARK_GRAY);
			g.drawString(s2, x1+w1, y+8+sizeIncrement);
			
			g.setFont(font3);
			g.setColor(Color.BLUE);
			g.drawString(s3, x1+w1+w2, y+11+sizeIncrement);
							
		}
		
		//Draw Icon
		if(biosample.getBiotype()!=null) {
			Image img = ImageFactory.getImage(biosample, iconW+sizeIncrement);
			if(img!=null) {
				g.drawImage(img, -sizeIncrement, y, this);
			}
		}		
		
	}
	
	public boolean isDisplayGroup() {
		return displayGroup;
	}

	public void setDisplayGroup(boolean displayGroup) {
		this.displayGroup = displayGroup;
	}	
	
	public void setDisplayName(boolean displayName) {
		this.displayName = displayName;
	}
	

	public boolean isDisplayName() {
		return displayName;
	}
	
	public void setHighlight(boolean highlight) {
		this.highlight = highlight;
	}
	
	public void setSizeIncrement(int sizeIncrement) {
		this.sizeIncrement = sizeIncrement;
	}
	
	public int getSizeIncrement() {
		return sizeIncrement;
	}
	
	public void setError(boolean error) {
		this.error = error;
	}
	
	public boolean isError() {
		return error;
	}
	
}