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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

/**
 * Fast Label without the complex features of JLabel (html, repaint, DND)
 * to be used in tables for fast repaint in tables. 
 * 
 * Some of the extra features added by this component are:
 * - It can display text on multiple lines: '\n' is used as a newline separator
 * - It can wrap text automatically: setWrappingWidth(widthPx) has to be called to force the width (getPreferredSize will then return this width)
 * - It can set some style: the style is set for the entire line; <B> for bold, <I> for italic, <y> for yellow, <m> for magenta, <b> for blue, <c> for cyan, <r> for red, <g> for green.
 * - It can condense the text automatically to fit the component's width: iti is possible to call setCondenseText(Boolean) to force the automatic behaviour
 * 
 * By default, it is opaque.
 * @author freyssj
 *
 */
public class JLabelNoRepaint extends JComponentNoRepaint {

	private static String[] specialExtensions;
	
	private String text;
    private int horizontalAlign = SwingConstants.LEFT;
    private int verticalAlign = SwingConstants.CENTER; 
    private int wrappingWidth = -1;
    private Boolean condenseText; 

	private String lines[] = new String[0];             
    private Color background = Color.WHITE;
    private Color foreground = Color.BLACK;

    private final Dimension preferredDim = new Dimension();
    
    private Icon icon;
    private boolean useIconsForKnownExtensions = false;
    private String extension = null;

    static {
    	setSpecialExtensions(new String[] {"jpg", "png", "gif", "pdf", "doc", "xls", "xlsx", "docx", "html", "txt"});    	
    }
    
    public JLabelNoRepaint() {
		this("");
		
	}
    
    public JLabelNoRepaint(String text) {
        setText(text);
        setDoubleBuffered(false);
        setFont(FastFont.REGULAR);
    }
    
	public void setIcon(Icon icon) {
		this.icon = icon;
		preferredDim.width = -1;
	}
    public void setWrappingWidth(int wrappingWidth) {
		this.wrappingWidth = wrappingWidth;
		preferredDim.width = -1;
	}
    
    public int getWrappingWidth() {
		return wrappingWidth;
	}
    
    /**
     * {@link SwingConstants}
     */
    public void setVerticalAlignment(int verticalAlign) {
    	this.verticalAlign = verticalAlign;
    }
    
    @Override
    public Dimension getPreferredSize() {
    	if(preferredDim.width<=0) calculateSize();
    	return preferredDim;
    }
    
    @Override
    public Dimension getMinimumSize() {    	
    	return super.getMinimumSize();
    }
    
	public void setText(String text) {
		if(text==null) text = "";
		this.text = text;
		lines = text.split("\n", 0);		
		preferredDim.width = -1;

		//extension?
		extension = null;
		if(useIconsForKnownExtensions && getText().length()>4 && getText().indexOf("\n")<0) {
			if(getText().startsWith("http://") || getText().startsWith("https://")) {
				extension = text;
				if(text.length()>8) {
					extension = text.substring(0, 7)+"...";
				}
			} else {
				int ind = getText().lastIndexOf('.');
				if(ind>=0 && ind>=getText().length()-5) {
					String tmp  = getText().substring(ind+1).toLowerCase();
					if(specialExtensions!=null && Arrays.binarySearch(specialExtensions, tmp)>=0) {
						extension = text;
						if(text.length()>8) {
							extension = text.substring(0, 7)+"...";
						}
					}
				}
			}
		}
		
	}
	public String getText() {
		return text;
	}
	
	
		
	private void calculateSize() {
		//First test special cases
		if(extension!=null) {
			preferredDim.width = 34; 
			preferredDim.height = 24;
			return;
		}
		
		//Then calculate normal size
		Font font;
		if(condenseText==Boolean.TRUE) {
			font = FastFont.REGULAR_CONDENSED.deriveFont(getFont().getStyle(), getFont().getSize());					
		} else if(condenseText==Boolean.FALSE) {
			font = FastFont.REGULAR.deriveFont(getFont().getStyle(), getFont().getSize());		
		} else {
			font = getFont();
		}

		FontMetrics fm = getFontMetrics(font);
		int offset = (icon==null?0: icon.getIconWidth()+2); 
		
		int lineHeight = font.getSize();//-1;
		if(getWrappingWidth()<=0) {
			int maxWidth = 0;
			int maxWidth2 = 0;
			for (int i = 0; i < lines.length; i++) {
				int width = fm.stringWidth(lines[i]);
				if(width>maxWidth) {
					maxWidth2 = maxWidth;
					maxWidth = width;
				} else if(width>maxWidth2) {
					maxWidth2 = width;
				}
			}
			preferredDim.width = offset + (lines.length>3 && maxWidth2>0 && maxWidth2*4/3<maxWidth? maxWidth2+5: maxWidth) + 4;
			preferredDim.height = lines.length * lineHeight + (lineHeight<=1? 4: 4);
		} else {
			preferredDim.width = getWrappingWidth();
			
			int y = 0;
			for (int i = 0; i < lines.length; i++) {
				int lineWidth = fm.stringWidth(lines[i]);
				int preferredLines = (lineWidth > getWrappingWidth()?2:1);
				y += preferredLines * lineHeight;
			}
			
			preferredDim.height = y-2;
		}
		if(preferredDim.height<18) preferredDim.height = 18;
		
	}
	
	public int getHorizontalAlignment() {		
		return horizontalAlign;
	}

	public void setHorizontalAlignment(int horizontalAlignment) {
		this.horizontalAlign = horizontalAlignment;
	}

	@Override
	public Color getBackground() {
		return background;
	}

	@Override
	public void setBackground(Color background) {
		this.background = background;
	}

	@Override
	public Color getForeground() {
		return foreground;
	}

	@Override
	public void setForeground(Color foreground) {
		this.foreground = foreground;
	}


	/**
	 * Font1 is the font used for displaying (except if there is a font2 used, ie for the last line)
	 */
	@Override
	public void setFont(Font font) {
		super.setFont(font);
		preferredDim.width = -1;
	}
	
	/**
	 * Principle.
	 * - If the text can be entirely seen: -> Perfect
	 * - Otherwise if condense == null, can we condense it to make it fit on line: -> perfect
	 * - Otherwise, can we make it on 2 lines: -> perfect 
	 * - Otherwise, we condense it on 2 lines. 
	 */
	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;

		Toolkit tk = Toolkit.getDefaultToolkit();
  		@SuppressWarnings("rawtypes")
		Map map = (Map)(tk.getDesktopProperty("awt.font.desktophints"));
  		if (map != null) {
  		    ((Graphics2D)g).addRenderingHints(map);
  		}
		
		int width = getWidth();
		int height = getHeight();

		if(isOpaque()) {
			g.setBackground(background);
			g.clearRect(0, 0, width, height);
		}
				
		if(lines==null || !isVisible()) return;
		
		Font font;
		if(condenseText==Boolean.TRUE) {
			font = FastFont.REGULAR_CONDENSED.deriveFont(getFont().getStyle(), getFont().getSize());					
		} else if(condenseText==Boolean.FALSE) {
			font = FastFont.REGULAR.deriveFont(getFont().getStyle(), getFont().getSize());
		} else {
			font = getFont();
		}
		g.setFont(font);
		
		//First treat special cases
		if(useIconsForKnownExtensions) {
			if(extension!=null) {
				int y = 2;
				g.setFont(font);
				int w = g.getFontMetrics().stringWidth(extension)+6;
				g.setColor(UIUtils.getDilutedColor(getBackground(), Color.LIGHT_GRAY));
				g.fillRect(3, y, w, 16);
				g.setColor(Color.GRAY);
				g.drawRect(3, y, w, 16);
				g.drawLine(3+w-6, y, 3+w, y+5);
				
				g.setColor(UIUtils.getColor(50,50,150));
				g.drawString(extension, 6, y+12);	
				paintChildren(g);
				return;
				
			}
		}

		//Wrapping? 
		List<String> wrappedLines = new ArrayList<>();
		for(int l=0; l<lines.length; l++) {	
			String line = lines[l];
			
			if(getWrappingWidth()>0 && line.length()>0) {


				int maxWidth = getWrappingWidth();
				String left = line;
				while(left.length()>0) {
					int lineWidth = g.getFontMetrics(font).stringWidth(left)*75/100;
					if(lineWidth<maxWidth) {
						wrappedLines.add(left);
						break;
					}
					int cut = 0;
					int testCut = 0;
					while(testCut<left.length()) {
						if(left.charAt(testCut)==' ' || left.charAt(testCut)==';' || left.charAt(testCut)==',' || left.charAt(testCut)==':' || left.charAt(testCut)=='/' ) {							
							int cutWidth = g.getFontMetrics(font).stringWidth(left.substring(0, testCut+1))*75/100;
							if(cutWidth<maxWidth) {
								cut = testCut;
							} else {
								break;
							}
						}
						testCut++;
					}				
					if(cut==0) cut = left.length()-1;
					wrappedLines.add(left.substring(0, cut+1));
					left = left.substring(cut+1);
				}
			} else {
				wrappedLines.add(line);
			}
		}
		
		//Vertical Alignment
		int y;		
		int lineHeight = font.getSize();//-1;
		int recommendedHeight = lineHeight * wrappedLines.size();

		if(verticalAlign==SwingConstants.TOP) {
			y = wrappedLines.size()>1? font.getSize()-1: font.getSize()+1;
		} else if(verticalAlign==SwingConstants.CENTER) {
			y = lineHeight - 2 + (height - recommendedHeight)/2;
			y = Math.max(y, font.getSize()-2);
		} else { //Bottom
			y = lineHeight - 3 + (height - recommendedHeight);
		}
		
//		//Make sure the top text is visible even if the top is clipped (bug)
//		if(getHeight()>MIN_HEIGHT_FOR_REPAINT) {
//			g.getClipBounds(rect);
//			if(y < rect.y + font.getSize()-2 && wrappedLines.size() * lineHeight < rect.height) {
//				y = rect.y + font.getSize()-2;
//			}
//		}
		
		
		int offset = 1;
		if(icon!=null) {
			icon.paintIcon(this, g, offset, Math.max(1, y-icon.getIconHeight()));
			offset+=icon.getIconWidth()+1;
		}
		
		
		
		
		
		//Draw the text line per line
		for(int l=0; l<wrappedLines.size(); l++) {
			String line = wrappedLines.get(l);

			g.setColor(foreground);
			
			if(line.startsWith("<B>")) {
				line = line.substring(3);
				g.setFont(font.deriveFont(Font.BOLD));
			} else if(line.startsWith("<I>")) {
				line = line.substring(3);
				g.setFont(font.deriveFont(Font.ITALIC));
			} else if(line.startsWith("<m>")) {
				line = line.substring(3);
				g.setColor(UIUtils.getColor(128, 0, 128));
			} else if(line.startsWith("<b>")) {
				line = line.substring(3);
				g.setColor(UIUtils.getColor(0, 0, 196));
			} else if(line.startsWith("<c>")) {
				line = line.substring(3);
				g.setColor(UIUtils.getColor(0, 64, 96));
			} else if(line.startsWith("<y>")) {
				line = line.substring(3);
				g.setColor(UIUtils.getColor(64, 96, 0));
			} else if(line.startsWith("<r>")) {
				line = line.substring(3);
				g.setColor(UIUtils.getColor(196, 0, 0));
			} else if(line.startsWith("<g>")) {
				line = line.substring(3);
				g.setColor(Color.DARK_GRAY);
			} else {
				g.setFont(font);
			}
			
			if(condenseText==null && (wrappedLines.size()>lines.length  || g.getFontMetrics(font).stringWidth(line)+4>getWidth())) {
				g.setFont(FastFont.REGULAR_CONDENSED.deriveFont(font.getStyle(), font.getSize()));					
			}
			
			//Draw the line
			if(horizontalAlign==SwingConstants.TRAILING || horizontalAlign==SwingConstants.LEFT) {
				g.drawString(line, offset, y);
			} else if(horizontalAlign==SwingConstants.CENTER) {
				int lineWidth = g.getFontMetrics().stringWidth(line);
				g.drawString(line, Math.max(offset, (width - lineWidth)/2), y);
			} else {
				g.drawString(line, Math.max(offset, width - 6 - g.getFontMetrics().stringWidth(line)), y);
			}				
			y += lineHeight;
		}
	}
	
	@Override
	public String getToolTipText() {
		if(super.getToolTipText()!=null && super.getToolTipText().length()>0) {
			String txt = super.getToolTipText();
			return txt.length()==0? null: "<html>" + txt.replace("\n", "<br>") + "</html>";
		} else if(extension!=null) {
			String txt = getText();
			return txt.length()==0? null: "<html>" + txt.replace("\n", "<br>") + "</html>";
		} else {
			return null;
		}
	}
	
	public Boolean getCondenseText() {
		return condenseText;
	}
	
	public void setCondenseText(Boolean condenseText) {
		this.condenseText = condenseText;
		preferredDim.width = -1;
	}
	
	@Override
	public String toString() {
		return "JLabelNoRepaint:text=" + getText() + ",wrapping=" + getWrappingWidth() + "," + getPreferredSize();
	}

	
	public void setUseIconsForKnownExtensions(boolean useIconsForKnownExtensions) {
		this.useIconsForKnownExtensions = useIconsForKnownExtensions;
	}

	public boolean isUseIconsForKnownExtensions() {
		return useIconsForKnownExtensions;
	}
	
	
	public static void setSpecialExtensions(String[] specialExtensions) {
		JLabelNoRepaint.specialExtensions = specialExtensions;
    	if(JLabelNoRepaint.specialExtensions!=null) Arrays.sort(JLabelNoRepaint.specialExtensions);
	}
	
	
}
