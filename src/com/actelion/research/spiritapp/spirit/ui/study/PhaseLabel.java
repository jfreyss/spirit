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

import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.exceltable.JComponentNoRepaint;

public class PhaseLabel extends JComponentNoRepaint {

	//	private Color groupColor;
	private String phaseLabel;
	private String shortName;

	private Dimension dim = new Dimension();

	public PhaseLabel() {
		setMinimumSize(new Dimension(40, 22));
	}
	public PhaseLabel(Phase phase, Group group) {
		this();
		setPhase(phase, group);
	}

	public void setPhase(Phase phase) {
		setPhase(phase, null);
	}


	@Override
	public Dimension getPreferredSize() {
		dim.width = getFontMetrics(FastFont.REGULAR).stringWidth(shortName==null?"":shortName);
		dim.height = 22;
		return dim;
	}

	public void setPhase(Phase phase, Group group) {
		//		this.groupColor = group==null? null: group.getBlindedColor(SpiritFrame.getUsername());
		this.shortName  = phase==null? null: phase.getShortName();
		this.phaseLabel = phase==null? null: phase.getLabel();
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		super.paintComponent(g);
		int width = getWidth();
		int height = getHeight();
		if(isOpaque()) {
			Color bgColor = getBackground();
			//			if(groupColor!=null) {
			//				bgColor = UIUtils.getDilutedColor(getBackground(), groupColor);
			//			}
			g.setBackground(bgColor);
			g.clearRect(0, 0, width, height);
		}

		if(shortName==null || !isVisible()) return;


		if(phaseLabel==null || phaseLabel.length()==0) {
			g.setFont(FastFont.REGULAR);
			g.setColor(getForeground());
			g.drawString(shortName, 2, FastFont.REGULAR.getSize()+4);
		} else {
			g.setFont(FastFont.SMALLER);
			g.setColor(Color.GRAY);
			g.drawString(phaseLabel, 2, FastFont.REGULAR.getSize()+FastFont.SMALLER.getSize()-2);

			g.setFont(FastFont.REGULAR);
			g.setColor(getForeground());
			g.drawString(shortName, 2, FastFont.REGULAR.getSize()-1);

		}
	}
}
