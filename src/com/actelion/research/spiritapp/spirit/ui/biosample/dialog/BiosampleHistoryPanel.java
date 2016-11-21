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

package com.actelion.research.spiritapp.spirit.ui.biosample.dialog;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.IBiosampleDetail;
import com.actelion.research.spiritapp.spirit.ui.lf.SpiritHyperlinkListener;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.util.FormatterUtils;

public class BiosampleHistoryPanel extends JEditorPane implements IBiosampleDetail {
	
	private int display;
	private Collection<Biosample> biosamples;
	private HTMLEditorKit kit = new HTMLEditorKit();
	
	public BiosampleHistoryPanel() {
		super("text/html", "");
		setBorder(BorderFactory.createEmptyBorder());
		
		setOpaque(false);
		setEditable(false);
		setBackground(Color.white);		
		setEditorKit(kit);
		
		StyleSheet stylesheet = kit.getStyleSheet();
		stylesheet.addRule("td, th {margin:0px;padding:0px}");
		stylesheet.addRule(".description th {padding-left:0px;padding-right:2px;margin-top:0px;font-weight:plain;text-align:right;font-size:9px;color:gray}");
		stylesheet.addRule(".description td {padding-left:0px;margin-top:0px;font-weight:plain;text-align:left; font-size:9px}");
		
		setBackground(new java.awt.Color(0,0,0,0));
	
		BiosampleActions.attachPopup(this);
		
		addHyperlinkListener(new SpiritHyperlinkListener());
	}

	public void setDisplay(int display) {
		this.display = display;
		refresh();
	}
	
	public int getDisplay() {
		return display;
	}
	
	@Override
	public Collection<Biosample> getBiosamples() {
		return biosamples;
	}	

	@Override
	public void setBiosamples(Collection<Biosample> biosamples) {
		Collection<Container> containers = Biosample.getContainers(biosamples, true);
		if(containers!=null && containers.size()==1) {
			this.biosamples = biosamples==null? new ArrayList<Biosample>(): biosamples;
		} else {
			this.biosamples = null;
		}
		refresh();
	}

	private void refresh() {
		
		if(biosamples==null || biosamples.size()==0) {
			setText("");
			return;
		}
		
		StringBuilder txt = new StringBuilder();
		txt.append("<html><body style='background:white'>");
		try {						
		
			for(Biosample b: biosamples) {
			
//				txt.append("<div style='border-bottom: solid 1px #999999; background:#FFFFFF; padding:3px; white-space:nowrap'>");
//				txt.append("Owner: <i>"+b.getCreUser()+" " +(b.getEmployeeGroup()==null?"": " (" + b.getEmployeeGroup().getName()+")") + "</i>");
//				txt.append("Last Update: <i>"+b.getUpdUser()+"</i> - <span style='font-size:9px'>" + FormatterUtils.formatDateOrTime(b.getUpdDate()) + "</span>");
//				txt.append("</div>");
				
				txt.append("<b>Change history</b><table>");
				try {
					List<Revision> revisions = DAORevision.getRevisions(b);
					for (int i = 0; i < revisions.size(); i++) {
						Revision rev = revisions.get(i);
						
						String diff;
						Biosample b1 = revisions.get(i).getBiosamples().get(0);
						if(i+1<revisions.size()) {
							Biosample b2 = revisions.get(i+1).getBiosamples().get(0);
							diff = b1.getDifference(b2, Spirit.getUsername());
						} else {
							Biosample b2 = revisions.get(0).getBiosamples().get(0);
							diff = b1.getDifference(b2, Spirit.getUsername());
							if(diff.length()==0) diff = "First version";
						}
						
						if(diff.length()==0) continue;
						txt.append("<tr>");
						txt.append("<th style='white-space:nowrap' valign=top>&nbsp;" + FormatterUtils.formatDateTimeShort(rev.getDate()) + "</th>");
						txt.append("<th style='white-space:nowrap' valign=top>&nbsp;" + rev.getUser() + "&nbsp;</th>");
						txt.append("<td style='white-space:nowrap' valign=top>" + diff.replace(";", "<br>") +"</td>");
						txt.append("</tr>");
					}
					txt.append("</table>");
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
									
		} catch (Exception e) {
			e.printStackTrace();
		}
		txt.append("</body></html>");
		
		setText(txt.toString());
		setCaretPosition(0);
		
	}
	
	
}
