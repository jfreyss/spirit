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

package com.actelion.research.spiritapp.ui.pivot.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
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
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

/**
 * BoxPlot is the class used to represent a boxPlot graph showing the 1st quartile, median, 3rd quartile for each of the given series.
 * Each series shows the numeric/alphanumeric values of a group over one or several phases.
 *
 * If there are more than one phase, the graph is a hybrid version between a boxplot and a line graph
 *
 * A typical use is:
 * <pre>
 * BoxPlot boxPlot = new BoxPlot();
 * boxPlot.setTitle1(title1);
 * boxPlot.setTitle2(title2);
 * boxPlot.setSeries(allSeries);
 * boxPlot.setxLabels(xLabels);
 * boxPlot.setLogScale(false);
 * boxPlot.setKruskalWallis(kw);
 * boxPlot.setBackground(UIUtils.WHITESMOKE);
 * </pre>
 *
 * @author Joel Freyss
 *
 */
public class BoxPlot extends JPanel {

	//Graph parameters
	private String title1;
	private String title2;
	private boolean logScale;
	private Double kw;
	private List<Series> series = new ArrayList<>();

	//Calculated automatically
	private List<String> xLabels = new ArrayList<>();
	private Map<Point, List<SimpleResult>> tooltips = new HashMap<>();

	public BoxPlot() {
		ToolTipManager.sharedInstance().registerComponent(this);
	}

	@Override
	public BoxPlot clone() {
		BoxPlot res = new BoxPlot();
		res.setTitle1(title1);
		res.setTitle2(title2);
		res.setLogScale(logScale);
		res.setKruskalWallis(kw);
		res.setSeries(series);
		res.setxLabels(xLabels);
		res.setBackground(getBackground());

		return res;
	}


	@Override
	public String getToolTipText(MouseEvent event) {

		if(event.getY()<FastFont.BIGGER.getSize()*2) {
			return title1 + (title2==null? "": "\n" + title2) + "\n Significance level: "+((kw==null?"NA":FormatterUtils.format3(kw)));
		} else {
			double closestDistance = 25;
			List<SimpleResult> closestTooltip = null;
			for (Map.Entry<Point, List<SimpleResult>> tooltip: tooltips.entrySet()) {
				double dist = (tooltip.getKey().getX()-event.getX())*(tooltip.getKey().getX()-event.getX()) +
						(tooltip.getKey().getY()-event.getY())*(tooltip.getKey().getY()-event.getY());
				if(dist<closestDistance) {
					closestDistance = dist;
					closestTooltip = tooltip.getValue();
					if(dist==0) break;
				}
			}
			if(closestTooltip==null) return null;

			return closestTooltip.stream().map(r->MiscUtils.removeHtml(r.toString())).collect(Collectors.joining("<br>", "<html>", "</html>"));
		}
	}

	public Double getKruskalWallis() {
		return kw;
	}

	public void setKruskalWallis(Double kw) {
		this.kw = kw;
	}

	public void setLogScale(boolean logScale) {
		this.logScale = logScale;
	}

	public String getTitle1() {
		return title1;
	}

	public void setTitle1(String title) {
		this.title1 = title;
	}

	public String getTitle2() {
		return title2;
	}


	public void setTitle2(String title2) {
		this.title2 = title2;
	}

	public List<Series> getSeries() {
		return series;
	}

	/**
	 * Sets the series and init the x labels. The x labels are sorted based on the order of the given series.
	 * @param series
	 */
	public void setSeries(List<Series> series) {
		this.series = series;

	}

	public void setxLabels(List<String> xLabels) {
		this.xLabels = xLabels;
	}

	public List<String> getxLabels() {
		if(xLabels==null) {
			LinkedHashSet<String> set = new LinkedHashSet<>();
			for (Series s : series) {
				set.addAll(s.getLabels());
			}
			xLabels = new ArrayList<>(set);
			System.out.println("BoxPlot.setSeries() " + title1 + " > "+xLabels);
		}
		return xLabels;
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

		int top = FastFont.BOLD.getSize()*2+8;
		int bottom = getHeight() - FastFont.REGULAR.getSize() * 2;
		int legend = getHeight() - FastFont.REGULAR.getSize() * 2 + FastFont.SMALL.getSize();
		int left = 32;
		int right = getWidth()-5;

		//Title
		g.setColor(Color.BLACK);
		if(title1!=null) {
			g.setFont(FastFont.BOLD);
			g.drawString(title1, 2, FastFont.BOLD.getSize());
		}

		if(title2!=null && title2.length()>0) {
			g.setFont(FastFont.BOLD);
			g.drawString(title2, 2, FastFont.BOLD.getSize()*2);
		}

		//Kruskal-Wallis
		if(kw!=null) {
			Color c =
					kw>.2? UIUtils.getColor(255, 180, 200):
						kw>.05? UIUtils.getColor(255, 220, 180):
							kw>.01? UIUtils.getColor(200, 255, 200):
								UIUtils.getColor(180, 255, 180);
							String s = kw>.2? "": kw>.05? "*": kw>.01? "**": "***";
							g.setColor(c);
							int polySize = FastFont.BOLD.getSize()*2+1;
							g.fillPolygon(new int[]{getWidth(), getWidth()-polySize, getWidth()}, new int[]{0, 0, polySize}, 3);

							g.setColor(UIUtils.getDilutedColor(Color.BLACK, c, .5));
							g.setFont(FastFont.BIGGER);
							if(s.equals("***")) {
								g.drawString("**", getWidth() - polySize/4 - g.getFontMetrics().stringWidth("**")/2 - 2, polySize/2+2);
								g.drawString("*", getWidth() - polySize/4 - g.getFontMetrics().stringWidth("*")/2 - 2, polySize-6);
							} else {
								g.drawString(s, getWidth() - polySize/4 - g.getFontMetrics().stringWidth(s)/2 - 2, polySize/2+2);
							}
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
		List<String> xLabels = getxLabels();
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
			UIUtils.drawString(g, s.getName()==null || s.getName().length()==0? "NA": s.getName(), x2+2, getHeight()-5, x3-x2-2, 0, true);

			seriesIndex++;
		}

		//Y Scales
		double yMin = getMin();
		double yMax = getMax();
		if(logScale && yMin<1) logScale = false;
		if(!logScale && yMin>0 && yMin-(yMax - yMin)/2<0) yMin = 0;
		g.setFont(FastFont.SMALLER);
		DecimalFormat df = yMax<=0? new DecimalFormat("0"):
			yMax>=10000 || yMax<0.01? new DecimalFormat("0.0E0"):
				yMax>100? new DecimalFormat("0"):
					yMax>10? new DecimalFormat("0.#"):
						new DecimalFormat("0.##");
					if(yMax>yMin) {
						//			//draw YLabel
						//			for(int i = 0; i<=3; i++) {
						//				int y1 = top + (int) ((bottom-top)/3.0*i);
						//				g.drawLine(left-2, y1, left, y1);
						//				String s;
						//				if(logScale) {
						//					s = df.format(Math.exp(log(yMax) + (log(yMin)-log(yMax))/3.0*i));
						//				} else {
						//					s = df.format(yMax + (yMin-yMax)/3.0*i);
						//				}
						//				g.drawString(s, left - g.getFontMetrics().stringWidth(s)-2, y1 + g.getFont().getSize()/2-1);
						//			}

						//draw YLabel
						for(int i = 0; i<=4; i++) {
							double v = yMin + (yMax-yMin)*i/4;
							String s = df.format(v);
							int y1;
							if(yMin>=yMax) {
								y1 = (top+bottom)/2;
							} else if(logScale) {
								y1 = (int) (top + (log(yMax)-log(v)) / (log(yMax)-log(yMin)) * (bottom - top));
							} else {
								y1 = (int) (top + (yMax-v) / (yMax-yMin) * (bottom - top));
							}
							g.drawLine(left-2, y1, left, y1);
							g.drawString(s, left - g.getFontMetrics().stringWidth(s)-2, y1 + g.getFont().getSize()/2-1);
						}
					}

					//draw Boxes for numeric values
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
									if(yMin>=yMax) {
										y[j] = (top+bottom)/2;
									} else if(logScale) {
										y[j] = (int) (top + (log(yMax)-log(fences[j])) / (log(yMax)-log(yMin)) * (bottom - top));
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
							//For numeric values, we draw crosses for each point
							//If there are more than 20 points, points inside the boxplots are skipped
							g.setColor(s.getColor().darker());
							for (SimpleResult p: s.getValues()) {
								if(p.getDoubleValue()==null) continue;
								int xIndex = xLabels.indexOf(p.getPhaseString());
								if(xIndex<0) {
									System.err.println(p.getPhaseString() + " not found in "+xLabels);
									return;
								}
								String lbl = xLabels.get(xIndex);
								double[] fences = s.getFences(lbl);
								int x = left + (int) (widthPerLabel * (xIndex+.5) + widthPerSeries * (seriesIndex - series.size()/2.0 + .5) );

								int y;
								if(yMin>=yMax) {
									y = (top+bottom)/2;
								} else if(logScale) {
									y = (int) (top + (log(yMax)-log(p.getDoubleValue())) / (log(yMax)-log(yMin)) * (bottom - top));
								} else {
									y = (int) (top + (yMax-p.getDoubleValue()) / (yMax-yMin) * (bottom - top));
								}

								if(fences!=null && s.getValues().size()>20 && p.getDoubleValue()>=fences[0] && p.getDoubleValue()<=fences[2]) {
									//skip points in the box
									continue;
								}
								int l = s.getValues().size()>20? 1: 2;
								g.drawLine(x-l, y-l, x+l, y+l);
								g.drawLine(x-l, y+l, x+l, y-l);
								tooltips.put(new Point(x, y), Collections.singletonList(p));
							}
						} else {
							//For non numeric values, we categorize the items and draw circles where data is found
							g.setColor(s.getColor().darker());
							g.setFont(FastFont.SMALLEST);
							for(String label: xLabels) {
								int xIndex = xLabels.indexOf(label);
								int x = left + (int) (widthPerLabel * (xIndex+.5) + widthPerSeries * (seriesIndex - series.size()/2.0 + .5) );
								Map<String, List<SimpleResult>> count = s.countValues(label);
								for (String key : count.keySet()) {
									int y = top + (int) ((double)Math.abs(key.hashCode())/Integer.MAX_VALUE * (bottom-top));
									int l = (int)Math.sqrt(count.get(key).size())+2;
									g.fillOval(x-l, y-l, l*2+1, l*2+1);
									g.drawString(key, x+l+1, y-g.getFont().getSize()/2);
									tooltips.put(new Point(x, y), count.get(key));
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
								if(yMin>=yMax) {
									y[j] = (top+bottom)/2;
								} else if(logScale) {
									y[j] = (int) (top + (log(yMax)-log(fences[j])) / (log(yMax)-log(yMin)) * (bottom - top));
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


	private static double log(double v) {
		if(v<=1) return 0;
		return Math.log(v);
	}
}
