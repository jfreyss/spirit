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

package com.actelion.research.util.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.View;

public class CustomTabbedPaneUI extends BasicTabbedPaneUI  {

	private final Color SELECTED_BACKGROUND = UIUtils.getColor(144,186,223);
    private Polygon shape;

    public static ComponentUI createUI(JComponent c) {
        return new CustomTabbedPaneUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
    }
    
    
    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2D = (Graphics2D) g;
        GradientPaint gradientShadow;
        int xp[] = null; 
        int yp[] = null;
        switch (tabPlacement) {
            case LEFT:
                xp = new int[]{x,   x,     x+3, x+w, x+w, x+3, x};
                yp = new int[]{y+3, x+h-3, y+h, y+h, y,   y,   y+3};
                gradientShadow = new GradientPaint(x, y, SELECTED_BACKGROUND, x+w-2, y, tabPane.getBackgroundAt(tabIndex));
                break;
            case RIGHT:
                xp = new int[]{x, x, x + w - 2, x + w - 2, x};
                yp = new int[]{y, y + h - 3, y + h - 3, y, y};
                gradientShadow = new GradientPaint(x, y, tabPane.getBackgroundAt(tabIndex), x, y + h, SELECTED_BACKGROUND);
                break;
            case BOTTOM:
                xp = new int[]{x, x, x + 3, x + w - 3 - 6, x + w - 3 - 2, x + w - 3, x + w - 3, x};
                yp = new int[]{y, y + h - 3, y + h, y + h, y + h - 1, y + h - 3, y, y};
                gradientShadow = new GradientPaint(x, y, tabPane.getBackgroundAt(tabIndex), x, y + h, SELECTED_BACKGROUND);
                break;
            case TOP:
            default:
                xp = new int[]{x, x, x + 3, x + w - 3 - 6, x + w - 3 - 2, x + w - 3, x + w - 3, x};
                yp = new int[]{y + h, y + 3, y, y, y + 1, y + 3, y + h, y + h};
                gradientShadow = new GradientPaint(x, y, SELECTED_BACKGROUND, x, y + h, tabPane.getBackgroundAt(tabIndex));
                break;
        }
        // ;
        shape = new Polygon(xp, yp, xp.length);
        if (isSelected) {
            g2D.setPaint(gradientShadow);
        } else {
            if (tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex)) {
                g2D.setColor(UIUtils.darker(tabPane.getBackgroundAt(tabIndex), .92));
            } else {
                g2D.setColor(Color.LIGHT_GRAY);
            }
        }
        g2D.fill(shape);
	}

    @Override
    protected void paintIcon(Graphics g, int tabPlacement, int tabIndex, Icon icon, Rectangle iconRect, boolean isSelected) {
    	iconRect.x-=2;
    	super.paintIcon(g, tabPlacement, tabIndex, icon, iconRect, isSelected);
    }
    
    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
    	((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    	textRect.x+=2;
    	g.setFont(font);

        View v = getTextViewForTab(tabIndex);
        if (v != null) {
        	v.paint(g, textRect);
        } else {
            // plain text
            if (tabPane.isEnabled() && tabPane.isEnabledAt(tabIndex)) {
                Color fg = tabPane.getForegroundAt(tabIndex);
                if (!isSelected) {
                    fg = Color.DARK_GRAY;
                }
                g.setColor(fg);
                g.drawString(title, textRect.x, textRect.y + metrics.getAscent());
            } else { // tab disabled
                g.setColor(tabPane.getBackgroundAt(tabIndex).brighter());
                g.drawString(title, textRect.x-1, textRect.y + metrics.getAscent()-1);
                g.setColor(tabPane.getBackgroundAt(tabIndex).darker());
                g.drawString(title, textRect.x-1, textRect.y + metrics.getAscent()-1);
            }
        }
    }
    
    @Override
    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
        return 10 + 3 + super.calculateTabWidth(tabPlacement, tabIndex, metrics);
    }

    /*
    @Override
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        if (tabPlacement == LEFT || tabPlacement == RIGHT) {
            return super.calculateTabHeight(tabPlacement, tabIndex, fontHeight);
        } else {
            return anchoFocoH + super.calculateTabHeight(tabPlacement, tabIndex, fontHeight);
        }
    }
    */
    
    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
    	((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    	g.setColor(lightHighlight);    	
        switch (tabPlacement) {
          case LEFT:
              g.drawLine(x, y+4, x, y+h-6); // left highlight
              g.drawLine(x, y+4, x+4, y); // top-left highlight
              g.drawLine(x+4, y, x+w-1, y); // top highlight

              g.setColor(shadow);
              g.drawLine(x+4, y+h-2, x, y+h-6); // bottom-left highlight
              g.drawLine(x+4, y+h-2, x+w-1, y+h-2); // bottom shadow
              break;
          case RIGHT:
              g.drawLine(x, y, x+w-3, y); // top highlight
              g.setColor(shadow);
              g.drawLine(x, y+h-2, x+w-3, y+h-2); // bottom shadow
              g.drawLine(x+w-2, y+2, x+w-2, y+h-3); // right shadow
              break;
          case BOTTOM:
              g.drawLine(x+1, y, x+1, y+h-4); // left highlight
              g.drawLine(x+1, y+h-4, x+4, y+h-1); // bottom-left highlight

              g.setColor(shadow);
              g.drawLine(x+4, y+h-1, x+w-4, y+h-1); // bottom shadow
              g.drawLine(x+w-4, y+h-1, x+w-2, y+h-4); // bottom-right highlight
              g.drawLine(x+w-2, y+h-4, x+w-2, y); // right shadow

//              g.setColor(darkShadow);
//              g.drawLine(x+2, y+h-1, x+w-3, y+h-1); // bottom dark shadow
//              g.drawLine(x+w-2, y+h-2, x+w-2, y+h-2); // bottom-right dark shadow
//              g.drawLine(x+w-1, y, x+w-1, y+h-3); // right dark shadow
              break;
          case TOP:
          default:
              g.drawLine(x, y+4, x, y+h-2); // left highlight
              g.drawLine(x, y+4, x+3, y); // top-left highlight
              g.drawLine(x+3, y, x+w-4, y); // top highlight


              g.setColor(shadow);
              g.drawLine(x+w-4, y, x+w-2, y+4); // top-right shadow
              g.drawLine(x+w-2, y+4, x+w-2, y+h-2); // right shadow

//            g.setColor(darkShadow);
//              g.drawLine(x+w-4, y, x+w-2, y+4); // top-right shadow
//          g.drawLine(x+w-1, y+2, x+w-1, y+h-1); // right dark-shadow
        }
    }

    /*
    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        if (tabPane.hasFocus() && isSelected) {
            g.setColor(UIManager.getColor("ScrollBar.thumbShadow"));
            if(shape!=null) g.drawPolygon(shape);
        }
    }
*/
    
    protected Color hazAlfa(int fila) {
        int alfa = 0;
        if (fila >= 0) {
            alfa = 50 + (fila > 7 ? 70 : 10 * fila);
        }
        return new Color(0, 0, 0, alfa);
    }
    
}
