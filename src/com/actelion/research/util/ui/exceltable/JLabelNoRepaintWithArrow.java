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
import java.awt.Graphics;

public class JLabelNoRepaintWithArrow extends JLabelNoRepaint {
	private int arrow = 0;
	
	public void setArrow(int arrow) {
		this.arrow = arrow;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if(arrow!=0) {
			int x = getWidth() - 7;
			int y = 6;
			
			g.setColor(Color.WHITE);
			g.fillPolygon(
					new int[] {x-4, x+4, x}, 
					new int[] {y+5*arrow, y+5*arrow, y-5*arrow}, 3);

			g.setColor(Color.BLACK);
			g.fillPolygon(
					new int[] {x-3, x+3, x}, 
					new int[] {y+4*arrow, y+4*arrow, y-4*arrow}, 3);
		}
	}
}