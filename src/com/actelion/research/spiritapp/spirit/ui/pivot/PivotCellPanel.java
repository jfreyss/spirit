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

import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.pivot.Computed;
import com.actelion.research.spiritcore.business.pivot.PivotCell;
import com.actelion.research.spiritcore.business.pivot.PivotCellKey;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotCell.Margins;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Aggregation;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Deviation;
import com.actelion.research.spiritcore.util.Formatter;
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
	
	public void setPivotCell(PivotCell pivotCell) {
		this.template = pivotCell.getTable().getTemplate();
		this.cell = pivotCell; 
	}	
	
	/**
	 * Get the margins, ie the x coordinates where:
	 * - the key should be displayed: x=2
	 * - the value should be displayed. m1Value
	 * - the computer value should be displayed. m2Computed
	 * - the std should be displayed. m3Std
	 * - ...
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
		Font f = FastFont.REGULAR_CONDENSED;		
		for (PivotCellKey key : cell.getNestedKeys()) {
			String s = key.getKey();
			if(s.length()>0) {
				margin.m1Value = Math.max(margin.m1Value, getFontMetrics(f).stringWidth(s+":")+2);
			}
		}		
		margin.m1Value = Math.max(4, margin.m1Value); 
		
		//Values
		margin.m2Computed = margin.m1Value;			
		for (PivotCellKey key : cell.getNestedKeys()) {
			PivotCell vl = cell.getNested(key);
			Object m = vl.getValue();
			
			if(m instanceof Double) {
				f = FastFont.REGULAR;
				String s = Formatter.formatMax3((Double) m);
				margin.m2Computed = Math.max(margin.m2Computed, getFontMetrics(f).stringWidth(s)+margin.m1Value+2);
			} else if(m instanceof Integer) {
				f = FastFont.REGULAR;
				String s = Integer.toString((Integer) m);
				margin.m2Computed = Math.max(margin.m2Computed, getFontMetrics(f).stringWidth(s)+margin.m1Value+2);
			} else if(m!=null) {
				String s = m.toString();
				f = FastFont.REGULAR;
				if(s.length()>0) margin.m2Computed = Math.max(margin.m2Computed, getFontMetrics(f).stringWidth(s)+margin.m1Value+2);
			}
		}

		//Computed  
		f = FastFont.REGULAR;
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
						String s = coeff>999? "999": "+"+Formatter.format1(coeff);
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
	
	
//	private Rectangle rect = new Rectangle();
	private final int lineHeight = 11;

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
		Margins margins = cell.getMargins();		
		if(margins==null) return;
		
		Graphics2D g = (Graphics2D) graphics;
		g.setBackground(getBackground());
		g.clearRect(0, 0, getWidth(), getHeight());

		
		int y = 1;
		
		int offset = 0; //getWidth() - Math.max(40, (margins.m5Width - margins.m1Value)) - margins.m1Value;
//		int offset = getWidth() - ((margins.m5Width - margins.m1Value-1)/10+1)*10 - margins.m1Value;
		
//		g.getClipBounds(rect);
//		if(rect.y>0) y = Math.min(rect.y-2, getHeight()-2-lineHeight*cell.getNestedKeys().size());

		
		//Display keys (if needed)
		g.setFont(FastFont.REGULAR_CONDENSED);
		g.setColor(UIUtils.getColor(150, 75, 0));
		int line = 0;
		for (PivotCellKey key : cell.getNestedKeys()) {
			String s = key.getKey();
			if(s.length()>0) {
				g.drawString(s  + (template.getAggregation()==Aggregation.HIDE?"": ": "), 2+offset, y+(line+1)*lineHeight);
			}
			line++;
		}
		
		//Display values
		line = 0;
		
		for (PivotCellKey key : cell.getNestedKeys()) {
			PivotCell vl = cell.getNested(key);
			Object m = vl.getValue();
			String s;
			int x;
			int sWidth;
			if(m==null) {
				s = "";
				sWidth = 0;
			} else if(m instanceof Double) {
				g.setFont(FastFont.REGULAR);
				s = Formatter.formatMax3((Double) m);
				sWidth = g.getFontMetrics().stringWidth(s);
			} else if(m instanceof Integer) {
				g.setFont(FastFont.REGULAR);
				s = Integer.toString((Integer)m);
				sWidth = g.getFontMetrics().stringWidth(s);
			} else {
				s = m.toString();
				g.setFont(FastFont.REGULAR);
				sWidth = g.getFontMetrics().stringWidth(s);
			}
			x = margins.m1Value;
			
			g.setColor(getForeground());
			if(vl.getN()>0) {
				Quality q = vl.getQuality();
				if(q!=null && q.getBackground()!=null) {
					g.setColor(q.getBackground());
					g.fillRect(x, line*lineHeight+2, sWidth, lineHeight);
					g.setColor(getForeground());
				}
			}			
			g.drawString(s, offset+x, y+(line+1)*lineHeight);
			line++;
		}
	


		//Display Computed
		g.setFont(FastFont.REGULAR);		
		if(template.getComputed()!=null && template.getComputed()!=Computed.NONE) {
			line = 0;
			g.setFont(FastFont.SMALL);
			for (PivotCellKey key : cell.getNestedKeys()) {
				PivotCell vl = cell.getNested(key);
				if(vl==null) continue;
				String o = template.getComputed().format(vl.getComputed());
				int x = margins.m2Computed; 
				if(o!=null) {
					g.setColor(vl.getComputed()!=null && vl.getComputed()<0? Color.RED: Color.BLUE);
					g.drawString(o, offset+x, y+(line+1)*lineHeight);						
				}
				line++;
			}
		}
		
		//Display STD
		if(template.getDeviation()!=Deviation.NONE) {
			line = 0;
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
						s = vl.getStd()>999? "+++": '\u00b1' + Formatter.format1(vl.getStd());
					}
					int x = margins.m3Std;						
					g.drawString(s, offset+x, y+(line+1)*lineHeight);
					
				}
				line++;
			}
		}
		

		
		
		//Display N
		if(template.isShowN()) {
			line = 0;
			for (PivotCellKey key : cell.getNestedKeys()) {
				PivotCell vl = cell.getNested(key);
				if(vl.getN()>1) {
					String s = "("+vl.getN()+")";
					g.setColor(Color.BLUE);
					g.setFont(FastFont.SMALL);
					int x = margins.m4N;
					g.drawString(s, offset+x, y+(line+1)*lineHeight);
				}
				line++;
			}			
		}
		

	}
	
	@Override
	public Dimension getPreferredSize() {
		Margins margin = getMargins();		
		int n = cell.getNestedKeys().size();
		dim.width = Math.max(margin.m5Width, 
				(cell.getNestedKeys().size()>1? 40:0) + 60 + (template.getDeviation()!=Deviation.NONE?25:0) + (template.getComputed()!=Computed.NONE?25:0));
		dim.height = n*11+4;
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
