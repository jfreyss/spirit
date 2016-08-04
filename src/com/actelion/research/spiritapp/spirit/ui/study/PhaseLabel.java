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

package com.actelion.research.spiritapp.spirit.ui.study;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.SwingConstants;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JComponentNoRepaint;

public class PhaseLabel extends JComponentNoRepaint {

	private Phase phase;
	private Group group;
	private int verticalAlignment;
	
	public PhaseLabel() {
		setMinimumSize(new Dimension(45, 24));
		setPreferredSize(new Dimension(50, 24));
	}
	public PhaseLabel(Phase phase, Group group) {
		this();
		setPhase(phase, group);
	}
		
	public void setPhase(Phase phase) {
		setPhase(phase, null);
	}
	
	public void setPhase(Phase phase, Group group) {
		this.phase = phase;
		this.group = group;
	}
	
	public Phase getPhase() {
		return phase;
	}
	
	public void setVerticalAlignment(int verticalAlignment) {
		this.verticalAlignment = verticalAlignment;
	}
	
	public int getVerticalAlignment() {
		return verticalAlignment;
	}
	
	
	
	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		int width = getWidth();
		int height = getHeight();
		if(isOpaque()) {
			Color bgColor = getBackground();
			if(group!=null) {
				bgColor = UIUtils.getDilutedColor(getBackground(), group.getBlindedColor(Spirit.getUsername()));
			}
			g.setBackground(bgColor);
			g.clearRect(0, 0, width, height);
		}
		
		if(phase==null || !isVisible()) return;
		
		int y = verticalAlignment==SwingConstants.TOP? 1: 1 + (getHeight()-24)/2;
		
		
		if(phase.getLabel()==null || phase.getLabel().length()==0) {
			g.setFont(FastFont.REGULAR);
			g.setColor(getForeground());
			g.drawString(phase.getShortName(), 2, y+14);
		} else {
			g.setFont(FastFont.SMALLER);
			g.setColor(Color.GRAY);
			g.drawString(phase.getLabel(), 2, y+18);

			g.setFont(FastFont.REGULAR);
			g.setColor(getForeground());
			g.drawString(phase.getShortName(), 2, y+10);
			
		}
	}
}
