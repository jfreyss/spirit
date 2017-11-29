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

package com.actelion.research.spiritapp.ui.study;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;

public class GroupLabel extends JLabelNoRepaint {

	private Group group;

	public GroupLabel() {
		setText(null);
		setMinimumSize(new Dimension(50, 22));
	}

	public GroupLabel(Group group) {
		this();
		setGroup(group);
	}

	public GroupLabel(String txt, Group group) {
		this();
		setText(txt, group);
	}

	public void setGroup(Group group) {
		setText(group == null ? "" : group.getBlindedName(SpiritFrame.getUsername()), group);
	}

	public void setText(String txt, Group group) {
		this.group = group;
		setText(txt);
	}

	@Override
	protected void paintComponent(Graphics graphics) {

		Graphics2D g = (Graphics2D) graphics;
		Color bgColor = getBackground();

		//Set Background
		if(isOpaque() && group != null) {
			bgColor = UIUtils.getDilutedColor(bgColor, group.getBlindedColor(SpiritFrame.getUsername()));
			setBackground(bgColor);
		}

		super.paintComponent(g);

		//Restore Background
		if(isOpaque()) {
			setBackground(bgColor);
		}
	}

}
