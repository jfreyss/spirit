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
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.pivot.Computed;
import com.actelion.research.spiritcore.business.pivot.PivotCell;
import com.actelion.research.spiritcore.business.pivot.PivotCell.Margins;
import com.actelion.research.spiritcore.business.pivot.PivotCellKey;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Aggregation;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Deviation;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JComponentNoRepaint;


/**
 * Component used to display a PivotCell (list of values grouped by key) in a JTable
 * @author freyssj
 *
 */
public class PivotCellPanel extends JComponentNoRepaint {

	private static final Color COLOR_VERYBAD = new Color(200, 100, 100); 
	private static final Color COLOR_BAD = new Color(180, 110, 100); 
	private static final Color COLOR_DUBIOUS = new Color(180, 150, 100);
	private static final Color COLOR_OK = new Color(130, 180, 100);
	
	private PivotTemplate template;
	private PivotCell cell;
	private Dimension dim = new Dimension(50,12);

	
	public PivotCellPanel() {
	}
	
	
	@Override
	public String getToolTipText() {
		if(cell.getResults().size()==0) return null;
		if(cell.getResults().size()>20) return cell.getResults()+" results...";
		StringBuilder sb = new StringBuilder();
		sb.append("<html><table border=0 cellpadding=1 cellmargin=0 style='font-size:8px'>");
		
		for(Result r: cell.getResults()) {
			sb.append("<tr>");
			sb.append("<td>" + MiscUtils.removeHtmlAndNewLines(r.getBiosample().getInheritedGroupString(Spirit.getUsername())) + "</td>");
			sb.append("<td>" + MiscUtils.removeHtmlAndNewLines(r.getBiosample().getTopParentInSameStudy().getSampleIdName()) + "</td>");
			sb.append("<td>" + r.getInheritedPhase()==null?"": MiscUtils.removeHtmlAndNewLines(r.getInheritedPhase().getShortName()) + "</td>");
			sb.append("<td>" + MiscUtils.removeHtmlAndNewLines(r.getBiosample().getSampleIdName()) + "</td>");
			sb.append("<td>" + MiscUtils.removeHtmlAndNewLines(r.getInputResultValuesAsString()) + "</td>");
			sb.append("<td>" + MiscUtils.removeHtmlAndNewLines(r.getOutputResultValuesAsString()) + "</td>");
			sb.append("<td>" + MiscUtils.removeHtmlAndNewLines(r.getInfoResultValuesAsString()) + "</td>");
			sb.append("</tr>");
		}
		sb.append("</table>");
		return sb.toString();
	}
	
	public void setPivotCell(PivotCell pivotCell) {
		this.template = pivotCell.getTable().getTemplate();
		this.cell = pivotCell; 
	}	
	
	/**
	 * Get the margins, ie the x coordinates where:
	 * - label: x=2
	 * - value: x=m1Value
	 * - computed: x=m2Computed
	 * - std: x=m3Std
	 * - n: x=m4N
	 * 
	 * The margins are calculated such as the data fits exactly, and they are valid for each nested row
	 * @return
	 */
	public Margins getMargins() {
		if(cell.getMargins()!=null) return cell.getMargins();
		
		Margins margin = new Margins();
		cell.setMargin(margin);
		margin.m1Value = 1;
		
		//Key
		Font f = FastFont.MEDIUM;		
		for (PivotCellKey key : cell.getNestedKeys()) {
			String s = key.getKey();
			if(s.length()>0) {
				margin.m1Value = Math.max(margin.m1Value, getFontMetrics(f).stringWidth(s+":")+4);
			}
		}		
		margin.m1Value = Math.max(4, margin.m1Value); 
		
		//Values
		f = FastFont.REGULAR;
		margin.m2Computed = margin.m1Value;			
		for (PivotCellKey key : cell.getNestedKeys()) {
			PivotCell vl = cell.getNested(key);
			Object m = vl.getValue();
			
			if(m instanceof Double) {
				String s = FormatterUtils.formatMax3((Double) m);
				margin.m2Computed = Math.max(margin.m2Computed, getFontMetrics(f).stringWidth(s)+margin.m1Value+2);
			} else if(m instanceof Integer) {
				String s = Integer.toString((Integer) m);
				margin.m2Computed = Math.max(margin.m2Computed, getFontMetrics(f).stringWidth(s)+margin.m1Value+2);
			} else if(m!=null) {
				String s = m.toString();
				if(s.length()>0) margin.m2Computed = Math.max(margin.m2Computed, getFontMetrics(f).stringWidth(s)+margin.m1Value+2);
			}
		}

		//Computed  
		f = FastFont.MEDIUM;
		margin.m3Std = margin.m2Computed;
		if(template.getComputed()!=null && template.getComputed()!=Computed.NONE) {
			for (PivotCellKey key : cell.getNestedKeys()) {
				PivotCell vl = cell.getNested(key);
				String o = template.getComputed().format(vl.getComputed());
				if(o!=null && o.length()>0) {
					String s = o;
					margin.m3Std = Math.max(margin.m3Std, getFontMetrics(f).stringWidth(s)+margin.m2Computed+8);
				}
			}
		}

		
		//STD 
		margin.m4N = margin.m3Std;
		if(template.getDeviation()!=null && template.getDeviation()!=Deviation.NONE) {
			for (PivotCellKey key : cell.getNestedKeys()) {
				PivotCell vl = cell.getNested(key);
		
				if(vl.getStd()!=null) {
					Double coeff = vl.getStd();
					if(coeff!=null) {
						String s = coeff>999? "999": "+"+FormatterUtils.format1(coeff);
						margin.m4N = Math.max(margin.m4N, getFontMetrics(f).stringWidth(s) + margin.m3Std + 8);
					}				
				}
			}
		}
		
		//N
		f = FastFont.SMALL;
		margin.m5Width = margin.m4N;
		if(template.isShowN()) {
			for (PivotCellKey key : cell.getNestedKeys()) {
				PivotCell vl = cell.getNested(key);
				if(vl.getN()>1) {
					margin.m5Width = Math.max(margin.m5Width, getFontMetrics(f).stringWidth("("+vl.getN()+")") + margin.m4N + 8);
				}
			}
		}
		
		return margin;
	}
	
	@Override
	public void update(Graphics g) {
		paint(g);
	}	
	
	
	@Override
	/**
	 * Paint the component using the following alignment
	 * <pre> 
	 * 0...........m1...........WIDTH-m52....WIDTH-m53....WIDTH-m54....WIDTH
	 *   label             value  computed     std                  (N)
	 * </pre>
	 *
	 */
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		UIUtils.applyDesktopProperties(g);
		super.paintComponent(g);

		Margins margins = cell.getMargins();		
		if(margins==null) return;

		
		int y = 0;
		int offset = 0; 
		final int lineHeight = FastFont.getDefaultFontSize()-1;


		//Left-aligned: labels (if needed)
		g.setFont(FastFont.MEDIUM);
		g.setColor(UIUtils.getColor(160, 95, 0));
		int line = 0;
		for (PivotCellKey key : cell.getNestedKeys()) {
			String s = key.getKey();
			if(s.length()>0) {
				g.drawString(s + (template.getAggregation()==Aggregation.HIDE?"": ": "), 2+offset, y+(line+1)*lineHeight);
			}
			line++;
		}
		
		//Clear text to display values
		g.clearRect(Math.max(0, getWidth() - margins.m5Width + margins.m1Value - 4), 1, getWidth(), getHeight());
		
		//Right-aligned
		//Display N
		g.setFont(FastFont.SMALL);
		if(template.isShowN()) {
			line = 0;
			int x = Math.max(margins.m4N-margins.m1Value, getWidth() - margins.m5Width + margins.m4N);
			for (PivotCellKey key : cell.getNestedKeys()) {
				PivotCell vl = cell.getNested(key);
				if(vl.getN()>1) {
					String s = "("+vl.getN()+")";
					g.setColor(UIUtils.getColor(100,100,255));
					g.setFont(FastFont.SMALL);
					g.drawString(s, x, y + (line+1)*lineHeight - 1);					
				}
				line++;
			}			
		}
		

		//Display STD
		if(template.getDeviation()!=Deviation.NONE) {
			line = 0;
			int x = Math.max(margins.m3Std-margins.m1Value, getWidth() - margins.m5Width + margins.m3Std);
			g.setFont(FastFont.SMALL);
			for (PivotCellKey key : cell.getNestedKeys()) {
				PivotCell vl = cell.getNested(key);
				String s;
				if(vl.getStd()!=null) {
					Integer coeff = vl.getCoeffVariation();
					if(coeff>80) g.setColor(COLOR_VERYBAD);
					else if(coeff>50) g.setColor(COLOR_BAD);
					else if(coeff>20) g.setColor(COLOR_DUBIOUS);
					else g.setColor(COLOR_OK);
					if(template.getDeviation()==Deviation.COEFF_VAR) {								
						s =  '\u00b1' + (Math.abs(coeff)>500?"??": coeff + "%");
					} else {
						s = vl.getStd()>999? "+++": '\u00b1' + FormatterUtils.format1(vl.getStd());
					}
					g.drawString(s, x, y+(line+1)*lineHeight);
					
				}
				line++;
			}
		}
		


		//Display Computed
		g.setFont(FastFont.MEDIUM);		
		if(template.getComputed()!=null && template.getComputed()!=Computed.NONE) {
			line = 0;
			int x = Math.max(margins.m2Computed-margins.m1Value, getWidth() - margins.m5Width + margins.m2Computed);
			for (PivotCellKey key : cell.getNestedKeys()) {
				PivotCell vl = cell.getNested(key);
				if(vl==null) continue;
				String o = template.getComputed().format(vl.getComputed());
				if(o!=null) {
					g.setColor(vl.getComputed()!=null && vl.getComputed()<0? Color.RED: Color.BLUE);
					g.drawString(o, x, y+(line+1)*lineHeight);						
				}
				line++;
			}
		}
		
		//Display values
		line = 0;
		g.setFont(FastFont.REGULAR);
		int x = Math.max(0, getWidth() - margins.m5Width + margins.m1Value);
		for (PivotCellKey key : cell.getNestedKeys()) {
			PivotCell vl = cell.getNested(key);
			Object m = vl.getValue();
			String s;
			if(m==null) {
				s = "";
			} else if(m instanceof Double) {
				s = FormatterUtils.formatMax3((Double) m);
			} else if(m instanceof Integer) {
				s = Integer.toString((Integer)m);
			} else {
				s = m.toString();
			}			
			g.setColor(getForeground());
			if(vl.getN()>0) {
				Quality q = vl.getQuality();
				if(q!=null && q.getBackground()!=null) {
					g.setColor(q.getBackground());
					g.fillRect(x-1, line*lineHeight+2, 1+g.getFontMetrics().stringWidth(s), lineHeight);
					g.setColor(getForeground());
				}
			}			
			g.drawString(s, x, y+(line+1)*lineHeight);
			line++;
		}
	

	}
	
	@Override
	public Dimension getPreferredSize() {
		Margins margin = getMargins();		
		int n = cell.getNestedKeys().size();
		dim.width = Math.max(margin.m5Width, 
				FastFont.getAdaptedSize((cell.getNestedKeys().size()>1? 40:0) + 60 + (template.getDeviation()!=Deviation.NONE?25:0) + (template.getComputed()!=Computed.NONE?25:0)));
		dim.height = n*(FastFont.defaultFontSize-1)+4;
		return dim;
	}
	
	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}
	
	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}
	
}
