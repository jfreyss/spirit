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

package com.actelion.research.util.ui.exceltable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.Border;

public class ZoneBorder implements Border {   
    private Color colorTop, colorRight, colorBottom, colorLeft;   

    public ZoneBorder(Color colorTop, Color colorLeft, Color colorBottom, Color colorRight) {   
        this.colorTop = colorTop;   
        this.colorRight = colorRight;   
        this.colorBottom = colorBottom;   
        this.colorLeft = colorLeft;   
    }   

    public boolean isBorderOpaque() {   
        return false;   
    }   

    public Insets getBorderInsets(Component c) {   
        return new Insets(colorTop==null?0:1,colorRight==null?0:1,colorBottom==null?0:1,colorLeft==null?0:1);   
    }   

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {   
        Color old = g.getColor();   
        if (colorTop != null) {   
            g.setColor(colorTop);   
            g.fillRect(x, y, width, 1);   
        }   
        if (colorRight != null) {   
            g.setColor(colorRight);   
            g.fillRect(x+width-1, y, 1, height);   
        }   
        if (colorBottom != null) {   
            g.setColor(colorBottom);   
            g.fillRect(x, y+height-1, width, 1);   
        }   
        if (colorLeft != null) {   
            g.setColor(colorLeft);   
            g.fillRect(x, y, 1, height);   
        }   
        g.setColor(old);   
    }   
}  