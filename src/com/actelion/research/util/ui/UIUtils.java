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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Box.Filler;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

public final class UIUtils {

	private static Map<Integer, Color> colorMap = new HashMap<Integer, Color>();
	public static final Color TITLEPANEL_BACKGROUND = UIUtils.getColor(183,204,223);
	
	public static Frame getMainFrame() {
		Frame res = null;
		for(Frame f:  Frame.getFrames()) {
			if(f.isShowing() && !f.isUndecorated()) {res = f; } 
		}
		return res;
	}

	public static Color getDilutedColor(Color color1, Color color2) {
		return getDilutedColor(color1, color2, .5);
	}
	
	/**
	 * Get Colors from (small) cache
	 */
	public static Color getColor(int r, int g, int b) {
		return getColor(r, g, b, 255);
	}
	public static Color getColor(Color color, int alpha) {
		return getColor(color.getRed(),color.getGreen(), color.getBlue(), alpha);
	}
	public static Color getColor(int r, int g, int b, int alpha) {
		int rgb =  ((alpha & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8)  | ((b & 0xFF) << 0);
		return getColor(rgb);
	}
	public static Color getColor(int rgb) {
		Color res = colorMap.get(rgb);
		if(res==null) {
			if(colorMap.size()>1000) colorMap.clear();
			res = new Color(rgb, true);
			colorMap.put(rgb, res);
		}
		return res;
		
	}
	
	public static Color getDilutedColor(Color color1, Color color2, double coeff) {
		if(color1==null) return color2;
		if(color2==null) return color1;
		return getColor((int)((color1.getRed() - color2.getRed())*coeff+color2.getRed()),
				(int)((color1.getGreen() - color2.getGreen())*coeff+color2.getGreen()), 
						(int)((color1.getBlue() - color2.getBlue())*coeff+color2.getBlue()),
						(int)((color1.getAlpha() - color2.getAlpha())*coeff+color2.getAlpha()));
	}
	
	public static void drawRoundRect3D(Graphics2D g, int x, int y, int width, int height, int radius) {
		height -=1;
		width -=1;
		
		g.drawLine(x+radius, y, x+width-radius, y);
		g.drawLine(x+radius, y+height, x+width-radius, y+height);
		g.drawLine(x, y+radius, x, y+height-radius);
		g.drawLine(x+width, y+radius, x+width, y+height-radius);

		g.drawArc(x, y, radius*2, radius*2, 90, 90);
		g.drawArc(x+width-2*radius, y, radius*2, radius*2, 0, 90);
		g.drawArc(x, y+height-2*radius, radius*2, radius*2, 180, 90);
		g.drawArc(x+width-2*radius, y+height-2*radius, radius*2, radius*2, 270, 90);				
	}
	public static void fillRoundRect3D(Graphics2D g, int x, int y, int width, int height, int radius) {
		height -=1;
		width -=1;
		Area area = new Area(new Area(new Rectangle(x, y+radius, width, height-radius*2)));
		area.add(new Area(new Rectangle(x+radius, y, width-radius*2, height)));
		area.add(new Area(new Ellipse2D.Double(x, y, radius*2, radius*2)));
		area.add(new Area(new Ellipse2D.Double(x+width-2*radius, y, radius*2, radius*2)));
		area.add(new Area(new Ellipse2D.Double(x, y+height-2*radius, radius*2, radius*2)));
		area.add(new Area(new Ellipse2D.Double(x+width-2*radius, y+height-2*radius, radius*2, radius*2)));
		g.fill(area);		
	}
	
	public static Color getRandomBackground(int seed) {
		return getRandomBackground(seed, .6f, .9f);
	}
	public static Color getRandomBackground(int seed, float maxSaturation, float minBrightness) {
		Random rand = new Random(seed*7);
		
		if(seed==0) {
			return getColor(240, 240, 240);
		} else {
			float h = rand.nextFloat()*1479;
			float s = maxSaturation/2 + rand.nextFloat()*maxSaturation/2;
			float b = 1f - (rand.nextFloat()*(1f-minBrightness));
			int c = Color.HSBtoRGB(h, s, b);
			Color c2 = getColor(c);
			return c2;
		}
		
	}
	public static Color getRandomForeground(int seed) {
		return getRandomBackground(seed, .7f, .3f);
	}
	public static Color getRandomForeground(int seed, float minSaturation, float maxBrightness) {
		Random rand = new Random(seed*7);
		
		if(seed==0) {
			return Color.BLACK;
		} else {
			float h = rand.nextFloat()*1479;
			float s = minSaturation + rand.nextFloat()*(1-minSaturation);
			float b = maxBrightness/2 + rand.nextFloat()*maxBrightness/2;
			int c = Color.HSBtoRGB(h, s, b);
			Color c2 = getColor(c);
			return c2;
		}
		
	}
	public static JPanel createTable(List<? extends Component> comps) {
		return createTable(comps.toArray(new Component[comps.size()]));
	}

	/**
	 * Creates a form panel
	 * <pre>
	 * comp1	comp2
	 * comp3	comp4
	 * </pre>
	 * 
	 * 
	 * @param comps (an even number of components)
	 * @return
	 */
	public static JPanel createTable(Component... comps) {
		return createTable(2, comps);
	}
	public static JPanel createTable(int columns, List<? extends Component> comps) {
		return createTable(columns, 0, 0, comps.toArray(new Component[comps.size()]));
	}
	
	public static JPanel createTable(int columns, int ipadx, int ipady, List<? extends Component> comps) {
		return createTable(columns, ipadx, ipady, comps.toArray(new Component[comps.size()]));
	}
	
	public static JPanel createTable(int columns, Component... comps) {
		return createTable(columns, 0, 0, comps);
	}
	
	public static JPanel createTable(int columns, int ipadx, int ipady, Component... comps) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = ipadx;
		c.ipady = ipady;
		int count = 0;
		for (Component comp : comps) {
			c.anchor = GridBagConstraints.WEST;
			c.weighty = 0;
			c.weightx = count%columns < columns-1? 0: 1;
			c.gridx = count%columns;
			c.gridy = count/columns;
			if(comp!=null) panel.add(comp, c);
			count++;
		}
		c.gridy++; c.weighty= 1; panel.add(Box.createGlue(), c);
		return panel;
	}
	
	public static JPanel createTitleBoxSmall(final String title, JComponent comp) {
		return createTitleBox(title, comp, FastFont.SMALL, Color.GRAY, TITLEPANEL_BACKGROUND,  6);
	}
	
	public static JPanel createTitleBox(JComponent comp) {
		return createTitleBox("", comp);
	}
	public static JPanel createTitleBox(final String title, JComponent comp) {
		return createTitleBox(title, comp, FastFont.BIGGER, Color.BLACK, TITLEPANEL_BACKGROUND, 10);
	}
	
	public static JPanel createTitleBoxBigger(final String title, JComponent comp) {
		return createTitleBox(title, comp, FastFont.BIGGEST, Color.BLACK, TITLEPANEL_BACKGROUND, 12);
	}
	
	public static JPanel createTitleBox(final String title, final JComponent comp, final Font font, final Color titleColor, final Color bgColor, final int radius) {
		final int h = font.getSize();             
		final int topBorder = h-7; 
		final int sideBorder = h>12? 5: h>10? 3: 1;
		final JPanel panel = new JPanel(new GridLayout(1,1)) {
			@Override
			protected void paintBorder(Graphics graphics) {
				Graphics2D g = (Graphics2D) graphics;
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(Color.WHITE);
				g.drawRoundRect(1, topBorder-1, getWidth()-4, getHeight()-topBorder-1, radius, radius);
				g.setColor(bgColor);
				g.fillRoundRect(2, topBorder, getWidth()-6, getHeight()-topBorder-2, radius, radius);
				g.setColor(Color.GRAY);
				g.drawRoundRect(2, topBorder, getWidth()-6, getHeight()-topBorder-2, radius, radius);	
				
				if(title!=null && title.length()>0) {
					g.setFont(font);
					g.setColor(bgColor);
					g.drawLine(radius-1, topBorder-1, radius + 3 + g.getFontMetrics().stringWidth(title), topBorder-1);
					g.setColor(bgColor);
					g.drawLine(radius-1, topBorder, radius + 3 + g.getFontMetrics().stringWidth(title), topBorder);					
					g.setColor(titleColor);
					g.drawString(title, radius+1, h-2);
				}
			}
		};
		comp.setOpaque(false);
		comp.setBackground(bgColor);
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(7+(h-10)*2, sideBorder+5, sideBorder+1, sideBorder+2));
//		panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK), BorderFactory.createEmptyBorder(7+(h-10)*2, sideBorder+2, sideBorder+1, sideBorder+2)));
		panel.add(comp);
		return panel;
	}

	
	public static JPanel createGrid(Component... components) {
		JPanel res = new JPanel(new GridLayout(1, components.length));
		res.setOpaque(false);
		for (Component component : components) {
			res.add(component);
		}
		return res;
	}

	public static JPanel createBox(Component center, Component north, Component south) {
		return createBox(null, center, north, south, null, null);
	}
	public static JPanel createBox(Component center, Component north) {
		return createBox(null, center, north, null, null, null);
	}
	public static JPanel createBox(Border border, Component center, Component north) {
		return createBox(border, center, north, null, null, null);
	}
	public static JPanel createBox(Border border, Component center, Component north, Component south) {
		return createBox(border, center, north, south, null, null);
	}
	public static JPanel createBox(Border border, Component center) {
		return createBox(border, center, null, null, null, null);
	}

	public static JPanel createBox(Component center, Component north, Component south, Component west, Component east) {
		return createBox(null, center, north, south, west, east);
	}
	
	public static JPanel createBox(Border border, Component center, Component north, Component south, Component west, Component east) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		if(center!=null) panel.add(BorderLayout.CENTER, center);
		if(north!=null) panel.add(BorderLayout.NORTH, north);
		if(south!=null) panel.add(BorderLayout.SOUTH, south);
		if(west!=null) panel.add(BorderLayout.WEST, west);
		if(east!=null) panel.add(BorderLayout.EAST, east);
		if(border!=null) panel.setBorder(border);
		return panel;
	}

	public static JPanel createHorizontalBox(Border border, Component... comps) {
		JPanel res = createHorizontalBox(comps);
		res.setBorder(border);
		return res;
	}
	public static JPanel createHorizontalBox(Component... comps) {
		return createHorizontalBox((int[])null, comps);
	}
	public static JPanel createHorizontalBox(int[] ratios, Component... comps) {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		c.gridx = GridBagConstraints.RELATIVE;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		for (int i = 0; i < comps.length; i++) {			
			Component comp = comps[i];
			if(comp==null) continue;
			if(comp instanceof Filler && ((Filler)comp).getMinimumSize().width==0) {
				c.weightx = c.weighty = 1;
			} else {
				c.weightx = ratios==null || i>=comps.length? 0.01: ratios[i];; 
				c.weighty = 1;				
			}
			panel.add(comp, c);			
		}
		panel.setOpaque(false);

		return panel;
	}
	
	public static JPanel createVerticalBox(Border border, Component... comps) {
		JPanel res = createVerticalBox(comps);
		res.setBorder(border);
		return res;
	}
	
	public static JPanel createVerticalBox(Border border, List<? extends Component> comps) {
		JPanel res = createVerticalBox(comps);
		res.setBorder(border);
		return res;
	}
	
	public static JPanel createVerticalBox(List<? extends Component> comps) {
		return createVerticalBox(comps.toArray(new Component[0]));
	}
	

	public static JPanel createVerticalBox(Component... comps) {
		return createVerticalBox((int[])null, comps);		
	}
	
	public static JPanel createVerticalBox(int[] ratios, Component... comps) {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
		for (int i = 0; i < comps.length; i++) {			
			Component comp = comps[i];
			if(comp==null) {
				continue;
			} else if(comp instanceof Filler && ((Filler)comp).getMinimumSize().height==0) {
				c.weightx = c.weighty = 1;
			} else {
				c.weightx = 1;
				c.weighty = ratios==null || i>=comps.length? 0.01: ratios[i];
			}
			panel.add(comp, c);			
		}
		panel.setOpaque(false);

		return panel;
	}

	
	public static JPanel createTopLeftPanel(Component comp) {		
		return UIUtils.createVerticalBox(UIUtils.createHorizontalBox(comp, Box.createHorizontalGlue()), Box.createVerticalGlue());
	}
	

	public static JPanel createCenterPanel(Component comp, boolean alignTop) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = c.weighty = 0;
		c.gridx = 2; c.gridy = 2; panel.add(comp, c);
		c.weightx = 1; c.weighty = alignTop?0: 1;
		c.gridx = 1; c.gridy = 2; panel.add(new JLabel(), c);
		c.weightx = 1; c.weighty = 1;
		c.gridx = 3; c.gridy = 3; panel.add(new JLabel(), c);
		return panel;
	}
	
	public static JPanel createCenterPanel(Border border, Component comp, boolean alignTop) {
		JPanel panel = createCenterPanel(comp, alignTop);
		panel.setBorder(border);
		return panel;
	}


	public static int drawString(Graphics g, String s, int x, int y, int width, int height) {
		return drawString(g, s, x, y, width, height, true);
	}
	
	public static int drawString(Graphics g, String s, int x, int y, int width, int height, boolean autowrap) {
		if(s==null || s.length()==0) return y;
		
		if(height<0) height = 1;
		int offset = 0; 
		int index = 1;
		int cy = y;
		for(;index<s.length() && cy<y+height; index++) {
			if(s.charAt(index)=='\n') {
				g.drawString(s.substring(offset, index), x, cy);
				offset = index;					
				cy+=g.getFont().getSize() - 1;
			} else if(g.getFontMetrics().stringWidth(s.substring(offset, index))>width) {
				g.drawString(s.substring(offset, index-1), x, cy);
				if(!autowrap) {
					//skip to next line
					index = s.indexOf('\n', index+1);
					if(index<0) index = s.length();
				}
				offset = index-1;
				cy+=g.getFont().getSize() - 1;
			}
		}
		if(cy<y+height) {
			g.drawString(s.substring(offset), x, cy);
		}
		return cy;
	}
	
	public static Color getTransparentColor(Color c) {
		return getTransparentColor(c, 0);
	}
	public static Color getTransparentColor(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}
	
	

	
	public static Color getForeground(Color c) {
		if(c==null || (c.getRed()+ c.getBlue()+c.getGreen())>96*3) {
			return Color.BLACK;
		} else {
			return Color.WHITE;
		}
	}
	
	public static Color getInverseColor(Color c) {
		return new Color(255-c.getRed(), 255-c.getGreen(), 255-c.getBlue());
	}
	
	/**
	 * Returns the HTML color (#RRGGBB) of the given color
	 * @param color (null means white)
	 * @return String 
	 */
	public static String getHtmlColor(Color color) {
		if(color==null) color = Color.WHITE;
		return "#"+ Integer.toHexString(color.getRGB()).substring(2).toUpperCase();
	}
	

	/**
	 * Makes the color darker
	 * @param col
	 * @param FACTOR 0 for black, 1 for no change
	 * @return
	 */
	public static Color darker(Color col, double FACTOR) {
		return getColor(Math.max((int) (col.getRed() * FACTOR), 0), Math.max((int) (col.getGreen() * FACTOR), 0), Math.max((int) (col.getBlue() * FACTOR), 0));
	}

	/**
	 * Makes the color darker
	 * @param col
	 * @param FACTOR 0 for white, 1 for no change
	 * @return
	 */
	public static Color brighter(Color col, double FACTOR) {
		return getColor(255-(int)((255-col.getRed())*FACTOR), 255-(int)((255-col.getGreen())*FACTOR), 255-(int)((255-col.getBlue())*FACTOR), 0);
	}

	public static JPanel createHorizontalTitlePanel(String title) {
		return createHorizontalTitlePanel(title, getColor(114, 160, 193));
	}
	
	public static JPanel createHorizontalTitlePanel(String title, Color color) {
		return createHorizontalTitlePanel(null, title, color);
	}

	public static JPanel createHorizontalTitlePanel(final Component comp, final String title, final Color color) {
		final Font font = FastFont.BOLD.deriveSize(16);
		final int x = 4 + (comp==null?0: comp.getPreferredSize().width);
		JPanel panel = new JPanel(new BorderLayout()) {
			@Override
			protected void paintComponent(Graphics graphics) {
				Graphics2D g = (Graphics2D) graphics;
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				if(color==null) {
					g.setPaint(Color.LIGHT_GRAY);
				} else {
					g.setPaint(new GradientPaint(0,0, color, 0, 22, UIUtils.getDilutedColor(Color.LIGHT_GRAY, color)));
				}
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setFont(font);
//				g.setColor(Color.LIGHT_GRAY);
//				g.drawString(title, x+1, 18);
				g.setColor(Color.BLACK);
				g.drawString(title, x, 17);
				
				g.setColor(Color.GRAY);
				g.drawLine(0, 0, getWidth(), 0);
				g.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
			}
		};
		int w = x + panel.getFontMetrics(font).stringWidth(title)+6;
		panel.setMinimumSize(new Dimension(w, 24));
		panel.setPreferredSize(new Dimension(w, 24));
		panel.setOpaque(true);
		if(comp!=null) panel.add(BorderLayout.WEST, comp);
		return panel;
		
	}
	
	public static JPanel createVerticalTitlePanel(String title) {
		return createVerticalTitlePanel(title, FastFont.BOLD.deriveSize(16), getColor(114, 160, 193));
	}
	public static JPanel createVerticalTitlePanel(final String title, final Font font, final Color background) {
		
		JPanel panel = new JPanel();
		final int x = 4;
		final int panelWidth = font.getSize()+8;
		final int panelHeight = x + panel.getFontMetrics(font).stringWidth(title==null?"": title)+6;

		panel = new JPanel(new BorderLayout()) {
			@Override
			protected void paintComponent(Graphics graphics) {
				Graphics2D g = (Graphics2D) graphics;
				g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				if(background==null) {
					g.setPaint(Color.LIGHT_GRAY);
				} else {
					g.setPaint(new GradientPaint(0,0, background, 0, 22, UIUtils.getDilutedColor(Color.LIGHT_GRAY, background)));
				}
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setFont(font);
				AffineTransform initial = g.getTransform();
				
				g.translate(0, getHeight());
				g.rotate(-Math.PI/2);
				
				
				if(title!=null) {
					g.setColor(Color.LIGHT_GRAY);
					g.drawString(title, getHeight() - getFontMetrics(font).stringWidth(title)-6 , font.getSize()+1);
					g.setColor(Color.BLACK);
					g.drawString(title, getHeight() - getFontMetrics(font).stringWidth(title)-6+1, font.getSize()+2);
				}
				
				g.setTransform(initial);
				
				g.setColor(Color.LIGHT_GRAY);
				g.drawLine(0, 0, getWidth(), 0);
				g.drawLine(0, 0, 0, getHeight()-1);
				g.setColor(Color.GRAY);
				g.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
				g.drawLine(getWidth()-1, 0, getWidth()-1, getHeight()-1);
			}
		};
		panel.setMinimumSize(new Dimension(panelWidth, panelHeight));
		panel.setPreferredSize(new Dimension(panelWidth, panelHeight));
		panel.setOpaque(true);
		return panel;
	}
	
	/**
	 * Set the size of the frame to the given size, or the size of the screen if it the screen is too small.
	 * If the size is bigger than the screen, the frame is maximized
	 * @param frame
	 * @param preferredWidth
	 * @param preferredHeight
	 */
	public static void adaptSize(JFrame frame, int preferredWidth, int preferredHeight) {
		java.awt.Toolkit toolkit = frame.getToolkit();
		Dimension sSize = toolkit.getScreenSize();
		if(preferredWidth>0 && preferredWidth>0) {
			frame.setSize(Math.min(preferredWidth, sSize.width-30), Math.min(preferredHeight, sSize.height-30));
			if(preferredWidth>sSize.width && preferredHeight>sSize.height) {
				frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
			}
		} else {
			frame.pack();
		}
		frame.setLocationRelativeTo(null);
		
	}
	
	/**
	 * Set the size of the dialog to the given size, or the size of the screen if it the screen is too small.	 * 
	 * @param frame
	 * @param preferredWidth
	 * @param preferredHeight
	 */
	public static void adaptSize(JDialog frame, int preferredWidth, int preferredHeight) {
		java.awt.Toolkit toolkit = frame.getToolkit();
		Dimension sSize = toolkit.getScreenSize();
		if(preferredWidth>0 && preferredWidth>0) {
			frame.setSize(Math.min(preferredWidth, sSize.width-30), Math.min(preferredHeight, sSize.height-30));
		} else {
			frame.pack();
		}
		frame.setLocationRelativeTo(null);		
	}
	
}
