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
import java.awt.Image;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.IBiosampleDetail;
import com.actelion.research.spiritapp.spirit.ui.lf.SpiritHyperlinkListener;
import com.actelion.research.spiritcore.business.biosample.ActionBiosample;
import com.actelion.research.spiritcore.business.biosample.ActionComments;
import com.actelion.research.spiritcore.business.biosample.ActionContainer;
import com.actelion.research.spiritcore.business.biosample.ActionLocation;
import com.actelion.research.spiritcore.business.biosample.ActionMoveGroup;
import com.actelion.research.spiritcore.business.biosample.ActionOwnership;
import com.actelion.research.spiritcore.business.biosample.ActionStatus;
import com.actelion.research.spiritcore.business.biosample.ActionTreatment;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.util.Formatter;
import com.actelion.research.util.ui.UIUtils;

public class BiosampleHistoryPanel extends JEditorPane implements IBiosampleDetail {
	
	private int display;
	private Container container;
	private Collection<Biosample> biosamples;
	private HTMLEditorKit kit = new HTMLEditorKit();
	private final static Hashtable<URL, Image> imageCache = new Hashtable<URL, Image>();
	
	public BiosampleHistoryPanel() {
		super("text/html", "");
		setBorder(BorderFactory.createEmptyBorder());
		
		setOpaque(false);
		setEditable(false);
		setBackground(Color.white);		
		setEditorKit(kit);
		getDocument().putProperty("imageCache", imageCache);
		
		//
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
	
	
	
	public Container getSelection() {
		return container;
	}	
	
	@Override
	public Collection<Biosample> getBiosamples() {
		return biosamples;
	}
	

	public void setBiosample(Biosample biosample) {
		if(biosample==null) {
			setSelection(false, null, null);
		} else {
			setSelection(false, biosample.getContainer(), Collections.singletonList(biosample));
		}
	}
	
	
	@Override
	public void setBiosamples(Collection<Biosample> biosamples) {
		Collection<Container> containers = Biosample.getContainers(biosamples, true);
		if(containers!=null && containers.size()==1) {
			setSelection(true, containers.iterator().next(), biosamples);
		} else {
			setSelection(false, null, null);
		}
	}

	private void setSelection(boolean containerSelected, Container container, Collection<Biosample> biosamples) {
		this.container = container;
		this.biosamples = biosamples==null? new ArrayList<Biosample>(): biosamples;
		refresh();
	}
	

	
	private void refresh() {
		
		if(biosamples==null || biosamples.size()==0) {
			setText("");
			return;
		}
		
//		new SwingWorkerExtended() {
			
//			private 
			StringBuilder txt;
			
//			@Override
//			protected void doInBackground() throws Exception {
				txt = new StringBuilder();
				txt.append("<html><body>");
				try {			
					
				
					for(Biosample b: biosamples) {
					
						txt.append("<div style='border-bottom: solid 1px #999999; background:#FFFFFF;padding:3px'>");
						txt.append("Owner: <i>"+b.getCreUser()+" " +(b.getEmployeeGroup()==null?"": " (" + b.getEmployeeGroup().getName()+")") + "</i> - <span style='font-size:9px'>" + Formatter.formatDateOrTime(b.getCreDate()) + "</span><br>");
						txt.append("Last Update: <i>"+b.getUpdUser()+"</i> - <span style='font-size:9px'>" + Formatter.formatDateOrTime(b.getUpdDate()) + "</span>");
						txt.append("</div>");
						for (ActionBiosample action: b.getActions(null, true)) {
							if(SpiritRights.isBlind(b.getInheritedStudy(), Spirit.getUser()) && ((action instanceof ActionMoveGroup) || (action instanceof ActionTreatment)) ) {
								continue;
							}

							txt.append("<tr style='background:"+UIUtils.getHtmlColor(getColor(action))+"'>");
							if(biosamples.size()>1) txt.append("<th style='white-space:nowrap'>" + b.getSampleId() +"</th>");
							txt.append("<th style='white-space:nowrap'>&nbsp;" + Formatter.formatDate(action.getUpdDate()) +"</th>");
							txt.append("<th style='white-space:nowrap'>&nbsp;" + Formatter.formatTime(action.getUpdDate()) +"</th>");
							txt.append("<td style='white-space:nowrap'>&nbsp;" + action.getDetails()+"</td>");
							txt.append("<td style='white-space:nowrap'>&nbsp;<b>" + (action.getPhase()!=null? "at "+action.getPhase().getShortName():"") + "</b></td>");
							txt.append("</tr>");
						}
						if(b.getCreDate()!=null && b.getCreUser()!=null) {
							txt.append("<tr style='background:#DDDDDD'>");
							if(biosamples.size()>1) txt.append("<th style='white-space:nowrap'>" + b.getSampleId() +"</th>");
							txt.append("<th style='white-space:nowrap'>&nbsp;" + Formatter.formatDate(b.getCreDate()) +"</th>");
							txt.append("<th style='white-space:nowrap'>&nbsp;" + Formatter.formatTime(b.getCreDate()) +"</th>");
							txt.append("<td style='white-space:nowrap'>&nbsp;Created</td>");
							txt.append("<td style='white-space:nowrap'>&nbsp;</td>");
						}
						txt.append("</table>");
					}
											
				} catch (Exception e) {
					e.printStackTrace();
				}
				txt.append("</body></html>");
				
				setText(txt.toString());
				setCaretPosition(0);
		
	}
	
	public static Color getColor(ActionBiosample action) {
		if(action instanceof ActionOwnership) {
			return new Color(255,255,180);
		} else if(action instanceof ActionComments) {
			return new Color(255,255,180);
		} else if(action instanceof ActionContainer) {
			return new Color(230,255,230);
		} else if(action instanceof ActionLocation) {
			return new Color(230,255,230);
		} else if(action instanceof ActionComments) {
			return new Color(255,255,180);
		} else if(action instanceof ActionMoveGroup) {
			return new Color(220,220,255);
		} else if(action instanceof ActionStatus) {
			return new Color(255,235,215);
		} else if(action instanceof ActionTreatment) {
			return new Color(225,255,195);
		}

		return null;
	}
	
}
