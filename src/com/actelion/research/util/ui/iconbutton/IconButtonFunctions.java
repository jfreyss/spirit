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

package com.actelion.research.util.ui.iconbutton;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;

/**
 * IconButtonFunctions
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Oct 9, 2013 MvK Start implementation, taken from Joel Freyss
 */
public class IconButtonFunctions {

	
	public static JButton createIconButton(JComponent parent, Image img, int size){
		
		
		JButton jButton = new JButton();
		
		jButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		Icon up = IconButtonFunctions.createIcon(parent, img, null, true, size*2, size, true);
		Icon down = IconButtonFunctions.createIcon(parent, img, null, false, size*2, size, true);
		jButton.setIcon(up);
		jButton.setPressedIcon(down);
		jButton.setSelectedIcon(down);
		jButton.setRolloverIcon(up);
		jButton.setRolloverSelectedIcon(down);
		
		return jButton;
	}
	
	public static JButton createTextButton(JComponent parent, String txt, int size){
		
		
		JButton jButton = new JButton();
		
		jButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		Icon up = IconButtonFunctions.createIcon(parent, null, txt, true, size*2, size, true);
		Icon down = IconButtonFunctions.createIcon(parent, null, txt, false, size*2, size, true);
		jButton.setIcon(up);
		jButton.setPressedIcon(down);
		jButton.setSelectedIcon(down);
		jButton.setRolloverIcon(up);
		jButton.setRolloverSelectedIcon(down);
		
		return jButton;
	}
	
	public static Icon createIcon(JComponent parent, Image img, String text, boolean up, int w, int h, boolean oneTimeUse) {
		
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) image.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, w, h);
		if(up) {
			if(oneTimeUse) {
				g.setPaint(new GradientPaint(0, 0, new Color(210, 210, 210), w, h, new Color(130, 130, 130)));
			} else {
				g.setPaint(new GradientPaint(0, 0, new Color(190, 190, 240), w, h, new Color(130, 130, 130)));
			}
			g.fillRect(1, 1, w-2, h-2);
		} else {
			if(oneTimeUse) {
				g.setPaint(new GradientPaint(0, 0, new Color(100, 100, 100), w, h, new Color(180, 180, 180)));
			} else {
				g.setPaint(new GradientPaint(0, 0, new Color(80, 80, 130), w, h, new Color(180, 180, 180)));
			}
			g.fillRect(1, 1, w-2, h-2);			
		}
		
		if(img!=null) {
			g.drawImage(img, (w - img.getWidth(parent)) / 2 + (up?0:1), (h - img.getHeight(parent)) / 2 + (up?0:1), parent);
		}
		
		if(text!=null) {
			g.setColor(Color.BLACK);
			g.setFont(new Font(Font.DIALOG, Font.BOLD, text.length()>=8? 9: text.length()>=4?11: 14));
			g.drawString(text, w/2 - g.getFontMetrics().stringWidth(text)/2 + (up?0: 1), h/2 + g.getFontMetrics().getMaxDescent() + (up?0: 1));
		}
		
		
		if(up) {
			g.setColor(Color.WHITE);
			g.drawLine(0, 0, 0, h-1);
			g.drawLine(0, 0, w-1, 0);
			g.setColor(Color.DARK_GRAY);
			g.drawRect(w-1, 0, 0, h-1);
			g.drawRect(0, h-1, w-1, 0);
		} else {
			g.setColor(Color.DARK_GRAY);
			g.drawLine(0, 0, 0, h);
			g.drawLine(0, 0, w, 0);
			g.setColor(Color.LIGHT_GRAY);
			g.drawRect(w-1, 0, 0, h-1);
			g.drawRect(0, h-1, w-1, 0);
			
		}
		
		return new ImageIcon(image);
	}


}
