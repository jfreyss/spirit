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

package com.actelion.research.spiritapp.spirit.ui.pivot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

public class PivotTemplateButton extends JToggleButton {

	private final String title;
	private final PivotTemplate template;
	private int w = 94;
	private int h = 24;

	public PivotTemplateButton(PivotTemplate template) {
		this.template = template;
		this.title = template==null?"": template.getName();
		if(template!=null) {
			Image img;
			try {
				img = template.getThumbnail();
			} catch (Exception e) {
				img = null;
			}
			
			init(template.getName(), img);
		}
	}
	public PivotTemplateButton(String title, Image img) {
		this.title = title;
		this.template = null;
		w = 122;
		init(title, img);
	}
		
	
	private void init(String title, Image img) {
		
		setBorder(null);

		if(title==null) title = "";
		
		Icon up = createIcon(img, title, true, w, h);
		Icon down = createIcon(img, title, false, w, h);
				
		setIcon(up);
		setPressedIcon(down);
		setSelectedIcon(down);
		setRolloverIcon(down);
		setRolloverSelectedIcon(down);	
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(w, h);
	}
	@Override
	public Dimension getMaximumSize() {
		return super.getPreferredSize();
	}
	
	private Icon createIcon(Image img, String text, boolean up, int w, int h) {
		int round = 6;
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if(up) {
			g.setPaint(new GradientPaint(0, 0, UIUtils.TITLEPANEL_BACKGROUND, 0, h, Color.LIGHT_GRAY));
		} else {
			g.setPaint(new GradientPaint(0, 0, UIUtils.TITLEPANEL_BACKGROUND, 0, h, Color.WHITE));
		}
		g.fillRoundRect(1, 1, w-4, h-1, round, round);			
		
		if(img!=null) {
			g.drawImage(img, 3 + (up?0:1), (h-img.getHeight(this))/2 + (up?0:1), this);
		}
		
		if(text!=null) {			
			Font font = FastFont.BOLD;
			g.setFont(font);
			int x = img==null? 6: img.getWidth(this) + 8;				
			int y = h/2+5;

			g.setColor(up? Color.DARK_GRAY: Color.BLACK);
			g.drawString(text, x + (up?0:1), y  + (up?0:1));					
		}
		
		
		if(up) {
			g.setColor(Color.GRAY);
			g.drawRoundRect(0, 0, w-3, h-1, round, round);
		} else {
			g.setColor(Color.BLUE);
			g.drawRoundRect(1, 1, w-4, h-2, round, round);
		}
		
		return new ImageIcon(image);
	}


	/**
	 * @return the view
	 */
	public PivotTemplate getPivotTemplate() {
		return template;
	}

	public String getTitle() {
		return title;
	}
	
	@Override
	public String toString() {
		return title;
	}
		
}
