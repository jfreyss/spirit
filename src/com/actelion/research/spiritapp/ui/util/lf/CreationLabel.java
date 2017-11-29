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

package com.actelion.research.spiritapp.ui.util.lf;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Date;

import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.exceltable.JComponentNoRepaint;


public class CreationLabel extends JComponentNoRepaint {


	private final FastFont FONT_DEPT = FastFont.SMALLER;

	private String dept = "";
	private String user = "";
	private Date date;
	private RightLevel level;

	public CreationLabel() {
		setOpaque(true);
	}

	public void setValue(String user, EmployeeGroup dept, Date date, RightLevel level) {
		this.user = user;
		this.dept = dept==null? null: dept.getName();
		this.date = date;
		this.level = level;
	}

	@Override
	public String getToolTipText() {
		if(date==null && user==null) return null;
		return (user==null?"": user + " ") + (date==null? "": FormatterUtils.formatDateTime(date));
	}

	public void setOwner(String owner, boolean canEdit) {
		setOwner(owner, canEdit? RightLevel.ADMIN: RightLevel.WRITE);
	}

	public void setOwner(String owner, RightLevel level) {
		this.level = level;
		this.date = null;
		if(owner!=null) {
			int index = owner.lastIndexOf(" - ");
			if(index>=0) {
				dept = owner.substring(0, index).toLowerCase().trim();
				user = owner.substring(index+3).toLowerCase();
			} else {
				dept = "";
				user = owner.toLowerCase();
			}
		} else {
			dept = "";
			user = "";
		}
	}

	@Override
	public Dimension getPreferredSize() {
		return date==null? new Dimension(65,25): new Dimension(getFontMetrics(FastFont.REGULAR).stringWidth( (user==null?"":user) + "__00/00/00"),25);
	}


	@Override
	protected void paintComponent(Graphics graphics) {

		Graphics2D g = (Graphics2D) graphics;
		super.paintComponent(g);
		if(!isVisible()) return;

		boolean hasDept = dept!=null && dept.length()>0;

		int y = getHeight()/2 + FastFont.REGULAR.getSize()/2 - 6 + (hasDept?FONT_DEPT.getSize():4);
		if(date!=null) {
			g.setColor(Color.BLACK);
			g.setFont(FastFont.REGULAR);
			String s = FormatterUtils.formatDate(date);
			g.drawString(s, getWidth()-2-g.getFontMetrics().stringWidth(s), y);
		}

		Color fgColor = level==RightLevel.ADMIN? LF.FGCOLOR_ADMIN:
			level==RightLevel.WRITE? LF.FGCOLOR_WRITE:
				level==RightLevel.READ? LF.FGCOLOR_READ:
					LF.FGCOLOR_VIEW;

		g.setColor(fgColor);
		if(hasDept) {
			g.setFont(FONT_DEPT);
			g.drawString(dept, 2, y - FastFont.REGULAR.getSize());
		}
		if(user!=null && user.length()>0) {
			g.setFont(FastFont.REGULAR);
			g.clearRect(2, y-FastFont.REGULAR.getSize()+1, g.getFontMetrics().stringWidth(user), FastFont.REGULAR.getSize());
			g.drawString(user, 2, y);
		}



	}

}
