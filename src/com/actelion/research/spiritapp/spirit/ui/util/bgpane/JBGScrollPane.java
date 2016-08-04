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

package com.actelion.research.spiritapp.spirit.ui.util.bgpane;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JScrollPane;
import javax.swing.JViewport;

public class JBGScrollPane extends JScrollPane {
	
	private final int scheme;
//	private Image backgroundImage;
	
	public JBGScrollPane(Component comp, int scheme) {
		super(comp);
		this.scheme = scheme;
//		backgroundImage = getBackground(scheme);
		getViewport().setOpaque(false);
		getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
	}
	
//	public static Image getBackground(int scheme) {
//		try {
//			InputStream is = JBGScrollPane.class.getResourceAsStream("scheme" + scheme + ".png");
//			if(is==null) throw new Exception("Invalid scheme: "+scheme);
//			Image backgroundImage = ImageIO.read(is);
//			is.close();
//			return backgroundImage;
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}		
//	}
	
	private final Color SCHEME1_1 = new Color(239,243,255);
	private final Color SCHEME1_2 = new Color(255,255,255);
	private final Color SCHEME2_1 = new Color(239,255,251);
	private final Color SCHEME2_2 = new Color(255,255,255);
	private final Color SCHEME3_1 = new Color(255,254,239);
	private final Color SCHEME3_2 = new Color(255,255,255);
	
	
	@Override
	public void paint(Graphics g) {
		
		Graphics2D g2d = (Graphics2D) g;
		if(scheme==1) {
			g2d.setPaint(new GradientPaint(0, 0, SCHEME1_1, getWidth(), getHeight(), SCHEME1_2));
		} else if(scheme==2) {
			g2d.setPaint(new GradientPaint(0, 0, SCHEME2_1, getWidth(), getHeight(), SCHEME2_2));
		} else  {
			g2d.setPaint(new GradientPaint(0, 0, SCHEME3_1, getWidth(), getHeight(), SCHEME3_2));
		}
		g2d.fillRect(0, 0, getWidth(), getHeight());
		
//		if( backgroundImage != null ) {
//			if(backgroundImage.getWidth(this)>=getWidth() && backgroundImage.getHeight(this)>=getHeight()) {
//				g.drawImage(backgroundImage, 0, 0, this);
//			} else {
//				g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), 0, 0, Math.min(getWidth(), backgroundImage.getWidth(this)), Math.min(getHeight(), backgroundImage.getHeight(this)), this);				
//			}
//			g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), 0, 0, backgroundImage.getWidth(this), backgroundImage.getHeight(this), this);
//		}
		super.paint( g );
	}

//	public Image getBackgroundImage() {
//		return backgroundImage;
//	}
//	public void setBackgroundImage(Image backgroundImage) {
//		this.backgroundImage = backgroundImage;
//	}	

}
