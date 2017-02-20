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

package com.actelion.research.spiritapp.spirit.ui.pivot.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import com.actelion.research.spiritcore.business.pivot.analyzer.SimpleResult;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

public class BoxPlot extends JPanel {
	private String title;

	private String yAxis;
	private boolean logScale;
	private List<Series> series = new ArrayList<>();
	private List<String> xLabels = new ArrayList<>();
	
	private Map<Point, String> tooltips = new HashMap<>();
	
	public BoxPlot() {
		ToolTipManager.sharedInstance().registerComponent(this);

	}
	
	@Override
	public String getToolTipText(MouseEvent event) {
		
		if(event.getY()<10) {
			return title;
		} else {		
			double closestDistance = 25;
			String closestTooltip = null;
			for (Map.Entry<Point, String> tooltip: tooltips.entrySet()) {
				double dist = (tooltip.getKey().getX()-event.getX())*(tooltip.getKey().getX()-event.getX()) + 
						(tooltip.getKey().getY()-event.getY())*(tooltip.getKey().getY()-event.getY());
				if(dist<closestDistance) {
					closestDistance = dist;
					closestTooltip = tooltip.getValue();
					if(dist==0) break;
				}
			}
			return closestTooltip;
		}
	}
	
	public void setLogScale(boolean logScale) {
		this.logScale = logScale;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getyAxis() {
		return yAxis;
	}


	public void setyAxis(String yAxis) {
		this.yAxis = yAxis;
	}

	public List<Series> getSeries() {
		return series;
	}
	
	public void setSeries(List<Series> series) {
		this.series = series;
		LinkedHashSet<String> set = new LinkedHashSet<>();
		for (Series s : series) {
			set.addAll(s.getLabels());
		}
		xLabels = new ArrayList<>(set);
	}
	
	public double getMin() {
		return series.stream().mapToDouble(s->s.getMin()).min().getAsDouble();
	}
	
	public double getMax() {
		return series.stream().mapToDouble(s->s.getMax()).max().getAsDouble();		
	}
	
	@Override
	protected void paintComponent(Graphics graphics) {
		tooltips.clear();
		Graphics2D g = (Graphics2D) graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		UIUtils.applyDesktopProperties(g);
		g.setBackground(getBackground());
		g.clearRect(0, 0, getWidth(), getHeight());
		
		int top = FastFont.BIGGER.getSize()+FastFont.BOLD.getSize()+8;
		int bottom = getHeight() - FastFont.REGULAR.getSize() * 2;
		int legend = getHeight() - FastFont.REGULAR.getSize() * 2 + FastFont.SMALL.getSize();
		int left = 32;
		int right = getWidth()-5;
		
		//Title
		g.setColor(Color.BLACK);
		g.setFont(FastFont.BIGGER);
		if(g.getFontMetrics().stringWidth(getTitle())>getWidth()-4) {
			g.setFont(FastFont.BOLD);
		}
		g.drawString(getTitle(), 2, FastFont.BIGGER.getSize());
		
		//Ylabel
		if(yAxis!=null && yAxis.length()>0) {
//			g.setFont(FastFont.BOLD);
//			g.translate(g.getFont().getSize(), (top+bottom)/2);
//			g.rotate(-Math.PI/2);
//			g.drawString(yAxis, -g.getFontMetrics().stringWidth(yAxis)/2, 0);
//			g.rotate(Math.PI/2);
//			g.translate(-g.getFont().getSize(), -(top+bottom)/2);
			g.setFont(FastFont.REGULAR);
			g.drawString(yAxis, 2, FastFont.BIGGER.getSize()+FastFont.REGULAR.getSize());
		}
		
		
		//draw graph box
		g.setColor(Color.WHITE);
		g.fillRect(left, top-4, right-left, bottom-(top-4));
		g.setColor(Color.BLACK);
		g.drawLine(left, top-4, left, bottom);
		g.drawLine(left, top-4, left-2, top-2);
		g.drawLine(left, top-4, left+2, top-2);
		g.drawLine(left, bottom, right, bottom);
		g.drawLine(right-2, bottom-2, right, bottom);
		g.drawLine(right-2, bottom+2, right, bottom);

		if(xLabels.size()==0) return;
		double widthPerLabel = (double)(right-left) / xLabels.size();
		
		
		//draw XLabel
		g.setFont(FastFont.SMALLER);
		int seriesIndex=0;
		int lastx = 0;
		for (String xLabel : xLabels) {			
			int x1 = left + (int) (widthPerLabel*(seriesIndex+.5));
			g.drawLine(x1, bottom, x1, bottom+2);
			int x2 = x1 - g.getFontMetrics().stringWidth(xLabel)/2;
			if(x2>=lastx) {
				g.drawString(xLabel, x2, bottom + g.getFont().getSize());
				lastx = x2 + g.getFontMetrics().stringWidth(xLabel);
			}
			seriesIndex++;
		}
		//draw legend		
		g.setFont(FastFont.SMALLER);
		seriesIndex = 0;
		double widthPerLegend = (getWidth()-2.0)/series.size();
		for (Series s : series) {
			int x1 = 2 + (int)(seriesIndex * widthPerLegend) + 2; 
			int x2 = x1 + 6;
			int x3 = 2 + (int)((seriesIndex+1) * widthPerLegend);
			g.setColor(s.getColor());
			g.fillRect(x1, legend+2, x2-x1, getHeight()-5-(legend+2));
			g.setColor(Color.BLACK);
			g.drawRect(x1, legend+2, x2-x1, getHeight()-5-(legend+2));
			UIUtils.drawString(g, s.getName()==null || s.getName().length()==0? "NA": s.getName(), x2+2, getHeight()-5, x3-x2-4, 0, true);
			
			seriesIndex++;
		}
		

		
		//Y Scales
		double yMin = getMin();
		double yMax = getMax();
		if(yMin>0 && yMin-(yMax - yMin)/2<0) yMin = 0;
		if(yMin<=0) logScale = false;
		if(yMax>yMin) {		
			//draw YLabel
			g.setFont(FastFont.SMALLER);
			for(int i = 0; i<=3; i++) {			
				int y1 = top + (int) ((bottom-top)/3.0*i);
				g.drawLine(left-2, y1, left, y1);
				String s;
				DecimalFormat df = yMax>=10000 || yMax<0.01? new DecimalFormat("0.0E0"): yMax>100? new DecimalFormat("0"): yMax>10? new DecimalFormat("0.#"): new DecimalFormat("0.##");
				if(logScale) {
					s = df.format(Math.exp(Math.log(yMax) + (Math.log(yMin)-Math.log(yMax))/3.0*i)); 
				} else {
					s = df.format(yMax + (yMin-yMax)/3.0*i); 
				}
				g.drawString(s, left - g.getFontMetrics().stringWidth(s)-2, y1 + g.getFont().getSize()/2-1);
			}
		}
		
		//draw Boxes
		double widthPerSeries = widthPerLabel / 2.0 / series.size();
		g.setColor(Color.BLACK);
		seriesIndex = 0;
		for (Series s : series) {
			if(s.isNumeric()) {
				for (int xIndex = 0; xIndex < xLabels.size(); xIndex++) {
					String lbl = xLabels.get(xIndex);
					double[] fences = s.getFences(lbl);
					if(fences==null) continue;
					int[] y = new int[3];
					int x = left + (int) (widthPerLabel * (xIndex+.5) + widthPerSeries * (seriesIndex - series.size()/2.0 + .5) );
					for (int j = 0; j < 3; j++) {
						if(logScale) {
							y[j] = (int) (top + (Math.log(yMax)-Math.log(fences[j])) / (Math.log(yMax)-Math.log(yMin)) * (bottom - top));
						} else {
							y[j] = (int) (top + (yMax-fences[j]) / (yMax-yMin) * (bottom - top));
						}					
					}
					if(widthPerSeries<=1) {
						g.setStroke(new BasicStroke(1f));
						g.setPaint(s.getColor());
						g.drawLine(x, y[2], x, y[0]);
					} else {
						int x1 = x - Math.min(15, Math.max(2, (int)(widthPerSeries/2)));
						int x2 = x + Math.min(15, Math.max(1, (int)((widthPerSeries+1)/2)-1));
						g.setStroke(new BasicStroke(1f));
						g.setPaint(s.getColor());
						g.fillRect(x1, y[2], x2-x1, y[0]-y[2]);
						g.setColor(Color.BLACK);
						g.drawRect(x1, y[2], x2-x1, y[0]-y[2]);
						g.drawLine(x1, y[1]-1, x2, y[1]-1);
						g.drawLine(x1, y[1], x2, y[1]);
						g.drawLine(x1, y[1]+1, x2, y[1]+1);
					}
				}
			}
			seriesIndex++;
		}

		//draw Points
		g.setStroke(new BasicStroke(1));
		seriesIndex=0;
		Set<String> seen = new HashSet<>();
		for (Series s : series) {
			if(s.isNumeric()) {
				g.setColor(s.getColor().darker());
				for (SimpleResult p: s.getValues()) {
					if(p.getDoubleValue()==null) continue;
					int xIndex = xLabels.indexOf(p.getPhaseString());
					String lbl = xLabels.get(xIndex);
					double[] fences = s.getFences(lbl);
					int x = left + (int) (widthPerLabel * (xIndex+.5) + widthPerSeries * (seriesIndex - series.size()/2.0 + .5) );
	
					int y;
					if(logScale) {
						y = (int) (top + (Math.log(yMax)-Math.log(p.getDoubleValue())) / (Math.log(yMax)-Math.log(yMin)) * (bottom - top));
					} else {
						y = (int) (top + (yMax-p.getDoubleValue()) / (yMax-yMin) * (bottom - top));
					}
	
					if(fences!=null && s.getValues().size()>20 && p.getDoubleValue()>=fences[0] && p.getDoubleValue()<=fences[2]) {
						//skip points in the box
						continue;
					}
					int l = s.getValues().size()>100? 0: s.getValues().size()>20? 1: 2;
					g.drawLine(x-l, y-l, x+l, y+l);
					g.drawLine(x-l, y+l, x+l, y-l);
					tooltips.put(new Point(x, y), p.toString());
				}
			} else {
				g.setColor(s.getColor().darker());
				g.setFont(FastFont.SMALLEST);
				for(String label: xLabels) {
					int xIndex = xLabels.indexOf(label);
					int x = left + (int) (widthPerLabel * (xIndex+.5) + widthPerSeries * (seriesIndex - series.size()/2.0 + .5) );
					Map<String, List<SimpleResult>> count = s.countValues(label);
					for (String key : count.keySet()) {
						int y = top+Math.abs(key.hashCode())%(bottom-top);
						int l = count.get(key).size()+2;
						g.fillOval(x-l, y-l, l*2+1, l*2+1);
						g.drawString(key, x+l+1, y-g.getFont().getSize()/2);
						tooltips.put(new Point(x, y), count.get(key).stream().map(r->MiscUtils.removeHtml(r.toString())).collect(Collectors.joining("<br>", "<html>", "</html>")));
						seen.add(key);
					}
				}
			}
			seriesIndex++;
		}
		
		//Draw lines
		seriesIndex = 0;
		for (Series s : series) {
			int lastX = 0; 
			int lastY = 0; 
			for (int xIndex = 0; xIndex < xLabels.size(); xIndex++) {
				String lbl = xLabels.get(xIndex);
				double[] fences = s.getFences(lbl);
				if(fences==null) continue;
				int[] y = new int[3];
				int x = left + (int) (widthPerLabel * (xIndex+.5) + widthPerSeries * (seriesIndex - series.size()/2.0 + .5) );
				for (int j = 0; j < 3; j++) {
					if(logScale) {
						y[j] = (int) (top + (Math.log(yMax)-Math.log(fences[j])) / (Math.log(yMax)-Math.log(yMin)) * (bottom - top));
					} else {
						y[j] = (int) (top + (yMax-fences[j]) / (yMax-yMin) * (bottom - top));
					}					
				}
				if(lastX>0) {
					g.setColor(s.getColor());
					g.setStroke(new BasicStroke(3f));
					g.drawLine(lastX, lastY, x, y[1]);
					g.setColor(Color.BLACK);
					g.setStroke(new BasicStroke(1f));
					g.drawLine(lastX, lastY, x, y[1]);
				}
				lastX = x;
				lastY = y[1];
			}
			seriesIndex++;
		}
			
		
	}
	
}
