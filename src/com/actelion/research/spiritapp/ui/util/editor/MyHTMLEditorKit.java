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

package com.actelion.research.spiritapp.ui.util.editor;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public class MyHTMLEditorKit extends HTMLEditorKit {
	
	private Map<String, Image> map;
	private MyHTMLFactory factory = new MyHTMLFactory();
	
	public MyHTMLEditorKit(Map<String, Image> map) {
		this.map = map;
	}
	
	@Override
	public ViewFactory getViewFactory() {
		return factory;
	}
	
	public class MyHTMLFactory extends HTMLFactory {

        /**
         * Creates a view from an element.
         *
         * @param elem the element
         * @return the view
         */
		@Override
        public View create(Element elem) {
			AttributeSet attrs = elem.getAttributes();
            Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
            Object o = (elementName != null) ? null : attrs.getAttribute(StyleConstants.NameAttribute);
            if (o instanceof HTML.Tag) {
                HTML.Tag kind = (HTML.Tag) o;
	            if (kind==HTML.Tag.IMG) {
	                return new MyImageView(elem);
	            } else {
	            	return super.create(elem);
	            }
    		} else {
            	return super.create(elem);
    		}
        }
	}
	public class MyImageView extends View {
		private final Image img; 
		private final int width, height; 
	    
		public MyImageView(Element elem) {
			super(elem);
			String key = (String) getElement().getAttributes().getAttribute(HTML.Attribute.SRC);
			img = map.get(key);
			width = img==null? 38: img.getWidth(null);
			height = img==null? 38: img.getHeight(null);
			
			if(img==null) {
				System.err.println("Could not get image src="+key);
			}
		}
		
		@Override
		public float getPreferredSpan(int axis) {
            switch (axis) {
            case View.X_AXIS:
                return width;
            case View.Y_AXIS:
                return height;
            default:
                throw new IllegalArgumentException("Invalid axis: " + axis);
            }
		}

		@Override
		public void paint(Graphics g, Shape a) {
			Rectangle rect = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
			Rectangle clip = g.getClipBounds();
			if (clip != null) {
				g.clipRect(rect.x, rect.y, rect.width, rect.height);
			}
			Container host = getContainer();
			if (img != null) {
				g.drawImage(img, rect.x, rect.y, width, height, null);
			} else {
				Icon icon = (Icon) UIManager.getLookAndFeelDefaults().get("html.missingImage");
				if (icon != null) {
					icon.paintIcon(host, g, rect.x, rect.y);
				}
			}
			if (clip != null) {
				// Reset clip.
				g.setClip(clip.x, clip.y, clip.width, clip.height);
			}
		}
		
		@Override
		public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
	        int p0 = getStartOffset();
	        int p1 = getEndOffset();
	        if ((pos >= p0) && (pos <= p1)) {
	            Rectangle r = a.getBounds();
	            if (pos == p1) {
	                r.x += r.width;
	            }
	            r.width = 0;
	            return r;
	        }
	        return null;
	    }
		
		@Override
	    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
	        Rectangle alloc = (Rectangle) a;
	        if (x < alloc.x + alloc.width) {
	            bias[0] = Position.Bias.Forward;
	            return getStartOffset();
	        }
	        bias[0] = Position.Bias.Backward;
	        return getEndOffset();
	    }

		
	}
}
