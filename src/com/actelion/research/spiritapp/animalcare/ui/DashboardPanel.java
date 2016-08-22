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

package com.actelion.research.spiritapp.animalcare.ui;

import java.awt.BorderLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.home.LastActivityEditorPane;
import com.actelion.research.spiritapp.spirit.ui.lf.SpiritHyperlinkListener;
import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.Formatter;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class DashboardPanel extends JPanel {

	private final Hashtable<String, Image> imageCache = new Hashtable<>();
	private JEditorPane editorPane = new ImageEditorPane(imageCache);
	private int dayOffset = 0;

	public DashboardPanel() {
		super(new BorderLayout());
		
		editorPane.addHyperlinkListener(new SpiritHyperlinkListener());
		editorPane.addHyperlinkListener(new HyperlinkListener() {
			
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType()==EventType.ACTIVATED) {
					if(e.getDescription().startsWith("date:")) {
						dayOffset = Integer.parseInt(e.getDescription().substring(5));
						refresh();
					} else if(e.getDescription().startsWith("refresh:")) {
						refresh();
					}
				}
			}
		});
		add(BorderLayout.CENTER, new JScrollPane(editorPane));
		refresh();
	}
	
	public void refresh() {
		new SwingWorkerExtended("Loading...", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			private StringBuilder sb; 
			
			@Override
			protected void doInBackground() throws Exception {
				
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR)-10);
				Date d0 = cal.getTime(); 
				
				cal = Calendar.getInstance();
				cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR)+10);
				Date d1 = cal.getTime(); 
				Date now = JPAUtil.getCurrentDateFromDatabase();
				String user = Spirit.getUsername();
				if(user==null) return;
				
				sb = new StringBuilder();
				sb.append("Dashboard for: ");
				if(dayOffset==-1) sb.append("<b>yesterday</b> | ");
				else sb.append("<a href='date:-1'>yesterday</a> | ");
				if(dayOffset==0) sb.append("<b>today (" + Formatter.formatDate(now) + ")</b>");
				else sb.append("<a href='date:0'>today (" + Formatter.formatDate(now) + ")</a>");
				for(int i=1; i<6; i++) {
					if(dayOffset==i) sb.append(" | <b>day+"+i+"</b>");
					else sb.append(" | <a href='date:"+i+"'>day+"+i+"</a>");
				}
				sb.append(" | <a href='refresh:'>Refresh</a>");
				sb.append("<br><br>");
				
				cal.setTime(now);
				cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR)+dayOffset);
				Date dashDate = cal.getTime();
				
				
				StudyQuery q = new StudyQuery();
				q.setRecentStartDays(365);
				
				List<Study> studies = new ArrayList<>(); 
				for(Study s: DAOStudy.queryStudies(q, Spirit.getUser())) {
					if(!SpiritRights.canBlind(s, Spirit.getUser())) continue;
					Date startDate = s.getFirstDate();
					if(startDate==null || startDate.after(d1)) continue;

					Date endDate = s.getLastDate();
					if(endDate==null || endDate.before(d0)) continue;
					
					studies.add(s);
				}
				
				Map<Study, String> extraColumn = new HashMap<>(); 
				for (Study s : studies) {
					
					//Is there something to do today?
					String desc = "";
					Phase nextPhase = null;
					String nextDesc = "";
					for(Phase p: s.getPhases()) {
						if(Phase.isSameDay(p.getAbsoluteDate(), dashDate)) {
							String d = p.getDescription();
							if(d.length()>0) {
								desc += "<br><b>" + p.getShortName() + ":</b><br><span style='font-size:8px'>" + d.replace(" + ", "+") + "</span>";
							}
						} else if(p.getAbsoluteDate().after(dashDate)) {
							if(nextPhase==null || nextPhase.getAbsoluteDate()==null || nextPhase.getAbsoluteDate().after(p.getAbsoluteDate())) {
								String d = p.getDescription();
								if(d.length()>0) {
									nextPhase = p;
									nextDesc = "<br><b>" + p.getShortName() + ":</b><br><span style='font-size:8px'>" + d.replace(" + ", "+") + "</span>";
								}								
							}
							
						}
					}
					StringBuilder sb = new StringBuilder();
					sb.append("<td valign=top valign=center style='width:110px;color:#666; font-size:9px; margin:1px; padding:1px'>");
					//sb.append("<a href='stu:" + s.getId() + "' style='font-size:12px;font-weight:bold'>" + s.getStudyId() + "</a>");
					if(desc.length()>0) {
						sb.append("<div style='border:solid 1px #FFAAAA; background:#FFFAFA'>");
						sb.append("<u>" + (dayOffset==0?"(" + Formatter.formatDate(dashDate)  + ")": "Day"+ (dayOffset>0?"+":"")+dayOffset) + "</u> - " + desc);
						sb.append("</div");
					}
					if(nextPhase!=null && nextDesc.length()>0) {
						sb.append("<div>");
						sb.append("<u>(" + Formatter.formatDate(nextPhase.getAbsoluteDate())  + ")</u> - " + nextDesc);
						sb.append("</div");
					}
					sb.append("</td>");
					extraColumn.put(s, sb.toString());
				}
				
				String table = LastActivityEditorPane.getStudyTableRows(studies, false, extraColumn);
				sb.append("<table style='background:#F5F5F50'>");
				sb.append(table);				
				sb.append("</table>");
			}
			
			
			@Override
			protected void done() {
				if(sb==null) return;
				editorPane.setText(sb.toString());
				editorPane.setCaretPosition(0);
			}
			
		};
	}
	
}
