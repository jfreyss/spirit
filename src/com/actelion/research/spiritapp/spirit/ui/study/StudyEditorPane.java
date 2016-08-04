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

package com.actelion.research.spiritapp.spirit.ui.study;

import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.actelion.research.spiritapp.spirit.ui.lf.SpiritHyperlinkListener;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.PropertyKey;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.Document.DocumentType;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.ConfigProperties;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.DAOResult.ElbLink;
import com.actelion.research.spiritcore.util.Formatter;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Triple;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.HtmlUtils;
import com.actelion.research.util.IOUtils;
import com.actelion.research.util.ui.JExceptionDialog;

public class StudyEditorPane extends JEditorPane {

	private Study study;
	
	private boolean displayResults = true;
	private final String NIOBE_LINK = "\\\\actelch02\\PGM\\ActelionResearch\\Niobe\\Niobe.lnk";
	private String sort = "date";
	
	public StudyEditorPane() {
		super("text/html", "");
		setEditable(false);
		setOpaque(true);
		setBackground(Color.RED);
		HTMLEditorKit kit = new HTMLEditorKit();
		StyleSheet stylesheet = kit.getStyleSheet();
		stylesheet.addRule("td, th {margin:0px;padding-left:2px; vertical-align:top; text-align:left}");
		setEditorKit(kit);

		
		addHyperlinkListener(new SpiritHyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType()==EventType.ACTIVATED) {
					String desc = e.getDescription(); 
					if(desc.startsWith("file:")) {
						study = JPAUtil.reattach(study);
						int docId = Integer.parseInt(desc.substring("file:".length()));
						Document d = null;
						for (Document doc : study.getDocuments()) {
							if(doc.getId()==docId) d = doc;
						}
						try {
							if(d==null) throw new Exception("Invalid document");
							
							//Save the doc in tmp dir
							File f = new File(System.getProperty("java.io.tmpdir"), d.getFileName());
							f.deleteOnExit();
							IOUtils.bytesToFile(d.getBytes(), f);
							//Execute on windows platform
							Desktop.getDesktop().open(f);
						} catch (Exception ex) {							
							JExceptionDialog.showError(ex);
						}
						return;
					} else if(desc.startsWith("niobe:")) {
						//Launch Niobe
						String param = desc.substring("niobe:".length());
						File f = new File(NIOBE_LINK);
						try {
							if(!f.exists()) throw new Exception(f+" could not be found");
							Runtime.getRuntime().exec("cmd /c " + NIOBE_LINK + " -elb " + param);							
						} catch (Exception e2) {
							JExceptionDialog.showError(e2);
						}
						return;
					} else if(desc.startsWith("study:")) {
						//Launch Niobe
						String param = desc.substring("study:".length());
						File f = new File(NIOBE_LINK);
						try {
							if(!f.exists()) throw new Exception(f+" could not be found");
							Runtime.getRuntime().exec("cmd /c " + NIOBE_LINK + " -study " + param);							
						} catch (Exception e2) {
							JExceptionDialog.showError(e2);
						}
						return;
					} else if(desc.startsWith("sort:")) {
						setSort(desc.substring("sort:".length()));
						return;					
					} else {
						super.hyperlinkUpdate(e);
					}
					
				}
			}
		});
		

	}
	
	
	
	
		
	public void setDisplayResults(boolean displayResults) {
		this.displayResults = displayResults;
	}
	
	public void setSort(String sort) {
		this.sort = sort;
		setStudy(study);
	}
	
	public void setStudy(final Study study) {
		this.study = study;
		//Set the editor pane text
		final StringBuilder sb = new StringBuilder();
		if(study!=null) {
			sb.append("<html><body style='white-space:nowrap'>");
			
			/////////////////////////////////////
			//Study Infos
			
			sb.append("<span style='font-size:12px;font-weight:bold'>" + study.getStudyId() + "</span> ");
			if(study.getIvv()!=null) sb.append(" <span style='font-size:10px'>  " + MiscUtils.removeHtml(study.getIvv()) + "</span> ");
			sb.append("<br>");
			if(study.getTitle()!=null) {
				sb.append("<span style='font-size:9px;color:#000000'>" + MiscUtils.removeHtml(study.getTitle()) + "</span><br>");
			}
			
			//Display metadata
			sb.append("<table style='font-size:8px'>");
			for(Entry<String, String> entry: study.getMetadata().entrySet()) {
				String name = ConfigProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_NAME, entry.getKey());
				if(name!=null && name.length()>0 && entry.getValue().length()>0) {
					sb.append("<tr><td>" + MiscUtils.removeHtml(name) + ":</td><td>" + MiscUtils.removeHtml(entry.getValue()) + "</td></tr>");
				}
			}
			sb.append("</table>");
						
			//Display notes
			if(study.getNotes()!=null && study.getNotes().length()>0) {
				sb.append("<div style='margin-top:5px;color:#444444;font-size:8px'>" + HtmlUtils.convert2Html(study.getNotes()) + "</div><br>");
			}
			
			sb.append("<hr>");
			sb.append("<table style='font-size:8px'>");
			sb.append("<tr><td>State:</td><td><b>" + (study.getState()==null?"": MiscUtils.removeHtml(study.getState())) + "</b></td></tr>");
			
			if(!ConfigProperties.getInstance().isChecked(PropertyKey.RIGHT_ROLEONLY)) {
				Set<String> adminSet = new LinkedHashSet<>();
				adminSet.addAll(Arrays.asList(ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES_ADMIN, study.getState())));
				adminSet.addAll(study.getAdminUsersAsSet());
				
				sb.append("<tr><td>Admin:</td><td><b>" + (adminSet.size()==0?"-": MiscUtils.flatten(adminSet, ", ")) + "</b></td></tr>");
				
				Set<String> expertSet = new LinkedHashSet<>();
				expertSet.addAll(Arrays.asList(ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES_EXPERT, study.getState())));
				expertSet.addAll(study.getExpertUsersAsSet());
				expertSet.addAll(EmployeeGroup.getNames(study.getEmployeeGroups()));
				sb.append("<tr><td>Expert:</td><td><b>" + (expertSet.size()==0? "-": MiscUtils.flatten(expertSet, ", ")) + "</b></td></tr>");
				
				Set<String> viewSet = new LinkedHashSet<>();
				viewSet.addAll(Arrays.asList(ConfigProperties.getInstance().getValues(PropertyKey.STUDY_STATES_READ, study.getState())));
				sb.append("<tr><td>View:</td><td><b>" + (viewSet.size()==0? "-": MiscUtils.flatten(viewSet, ", ")) + "</b></td></tr>");
				
				if(study.getBlindDetailsUsersAsSet().size()>0) sb.append("<tr><td>Blind-Names:</td><td><b>" + MiscUtils.flatten(study.getBlindDetailsUsersAsSet(), ", ") + "</b></td></tr>");
				if(study.getBlindAllUsersAsSet().size()>0) sb.append("<tr><td>Blind-All:</td><td><b>" + MiscUtils.flatten(study.getBlindAllUsersAsSet(), ", ") + "</b></td></tr>");
			}
			sb.append("</table>");
			sb.append("<hr>");

			if(displayResults) {
				
				//Display Documents
				Map<DocumentType, List<Document>> docs = Document.mapDocumentTypes(study.getDocuments());
				if(study.getDocuments().size()>0) {
					for (DocumentType docType : docs.keySet()) {
						sb.append("<div style='margin-top:5px'><b>"+(docType!=null? docType: null)+"</b>:<br>");
						for (Document doc : docs.get(docType)) {
							String name = doc.getFileName();
							if(name.length()>40) name = name.substring(0, 30) + "..." + name.substring(name.length()-8);
							sb.append(" - <a href='file:" + doc.getId() + "'>" + name + "</a><br>");
						}
						sb.append("</div>");
					}
					sb.append("<hr>");
				}

				
				sb.append("<br>");
				try {
					//Count Biosamples
					Map<Study, Map<Biotype, Triple<Integer, String, Date>>> countBio = DAOStudy.countSamplesByStudyBiotype(Collections.singletonList(study));
					formatNumberBiosamples(sb, study, countBio.get(study));
					sb.append("<br>");
					//Count Results
					Map<Study, Map<Test, Triple<Integer, String, Date>>> countRes = DAOStudy.countResultsByStudyTest(Collections.singletonList(study));
					formatNumberResults(sb, study, countRes.get(study));
					
				} catch(Exception e) {
					e.printStackTrace();
					sb.append(e.getMessage());
				}
				
				
				if(DBAdapter.getAdapter().isInActelionDomain()) {
					//Find related ELBs
					List<ElbLink> elbs = DAOResult.getNiobeLinksForStudy(study);
					if(elbs.size()>0) {
						
						sb.append("<br><hr><span style='font-size:10px'><b>Niobe:</b></span>");
	
						if("date".equals(sort)) {
							Collections.sort(elbs, new Comparator<ElbLink>() {
								@Override public int compare(ElbLink o1, ElbLink o2) {return CompareUtils.compare(o1.getCreDate(), o2.getCreDate());}
							});
						} else if("elb".equals(sort)) {
							Collections.sort(elbs, new Comparator<ElbLink>() {
								@Override public int compare(ElbLink o1, ElbLink o2) {return CompareUtils.compare(o1.getElb(), o2.getElb());}
							});
						} else if("title".equals(sort)) {
							Collections.sort(elbs, new Comparator<ElbLink>() {
								@Override public int compare(ElbLink o1, ElbLink o2) {return CompareUtils.compare(o1.getTitle(), o2.getTitle());}
							});
						} else if("scientist".equals(sort)) {
							Collections.sort(elbs, new Comparator<ElbLink>() {
								@Override public int compare(ElbLink o1, ElbLink o2) {return CompareUtils.compare(o1.getScientist(), o2.getScientist());}
							});
						} else {
							System.err.println("Invalid sort: "+sort);
						}
	
						//Check if we have niobe experiments
						boolean hasNiobeLinks = false;
						for (ElbLink l: elbs) {
							if(l.isInNiobe()) hasNiobeLinks = true;
						}
						if(hasNiobeLinks) {
							sb.append("    [ <a style='font-size:8px' href='study:" + study.getStudyId() + "'>Query in Niobe</a> ]");
						}
						sb.append("<br>");
						
						//Display all Elbs
						sb.append("<table style='border:0; padding:1px'>");
						sb.append("<tr style='background:#EEEEDD;padding:0px'><th colspan=2><a href=\"sort:elb\">ELB<a> / <a href=\"sort:title\">Title<a></th><th><a href=\"sort:scientist\">Scientist<a></th><th><a href=\"sort:date\">Date<a></th></tr>");
						for (int i = 0; i < elbs.size(); i++) {
							ElbLink elbLink = elbs.get(i);
							sb.append("<tr style='background:#FFFFEE'>");
							sb.append("<td>&nbsp;<b>" + elbLink.getElb() + "</b></td>"); 
							sb.append("<td style='white-space:nowrap; font-size:8px'>&nbsp; [ ");
							if(elbLink.isInSpirit() || elbLink.getTitle()!=null) {
								if(elbLink.isInSpirit()) {
									sb.append("<a href='elb:" + elbLink.getElb() + "'>Spirit</a>");
								}					
								if(elbLink.isInNiobe()) {
									if(elbLink.isInSpirit()) sb.append(" | ");
									
									sb.append("<a href='niobe:" + elbLink.getElb() + "'>Open in Niobe</a>");
	
									if(elbLink.getUrl()!=null) {
										sb.append(" | <a href='" + elbLink.getUrl().toString() + "'>PDF</a>");
									}
								}						
								sb.append(" ] </td> ");
							}
							
							if(elbLink.isInNiobe() && elbLink.getTitle()!=null ) {
								sb.append("<td style='color:#999999;font-size:8px'>&nbsp;" + elbLink.getScientist() + "</td>");
								sb.append("<td style='color:#999999;font-size:8px;white-space:nowrap'>&nbsp; " + Formatter.formatDate(elbLink.getCreDate()) + " -> " + (elbLink.getPubDate()==null?" Unsealed": Formatter.formatDate(elbLink.getPubDate()))+"</td>");
							} 
							sb.append("</tr>");
							if(elbLink.isInNiobe() && elbLink.getTitle()!=null ) {
								sb.append("<tr style='background:#FFFFEE'><td colspan=4 style='background:#FFFFFF; font-size:8px'>");
								sb.append(elbLink.getTitle());
								sb.append("</td></tr>");							
							}
						}
						sb.append("</table>");
					}
				}
				
			}
		
			sb.append("<div style='font-size:8px; color:gray'>");
			sb.append("Created the " + Formatter.formatDateTimeShort(study.getCreDate()) + " by " + study.getCreUser() + "<br>");
			if (study.getUpdDate() != null && study.getUpdDate().getTime() > study.getCreDate().getTime() + 1000) {
				sb.append("Updated the " + Formatter.formatDateTimeShort(study.getUpdDate()) + " by " + study.getUpdUser() + "<br>");				
			}
			sb.append("</div>");

			sb.append("</body></html>");
		}
		
		setText(sb.toString());
		setCaretPosition(0);
				
	}

	public static void formatNumberBiosamples(StringBuilder sb, Study study, Map<Biotype, Triple<Integer, String, Date>> m1) {		
		if(m1!=null && m1.size()>0) {
			int count=0; for (Biotype type: m1.keySet()) {
				if(m1.get(type)!=null) count+=m1.get(type).getFirst();
			}
			sb.append("<table><tr><td colspan=2>");
			sb.append("<a style='font-weight:bold' href='bios:" + study.getStudyId() + "'>Biosamples:</a>&nbsp;(" + count + ")");
			sb.append("</td></tr>");
			for (Biotype t: m1.keySet()) {					
				sb.append("<tr><td><a href='bios:" + study.getStudyId() + ":" + t.getName() + "'>" + t.getName() + "</a> (" + m1.get(t).getFirst() + ")");																								
				sb.append("</td><td style='padding-left:5px;color:" + getColor(m1.get(t).getThird()) + "'>");
				sb.append("  [" + Formatter.formatDateOrTime(m1.get(t).getThird()) + " - " + m1.get(t).getSecond() +"]");
				sb.append("</td></tr>");
			}
			sb.append("</table>");
		}
	}

	public static void formatNumberResults(StringBuilder sb, Study study, Map<Test, Triple<Integer, String, Date>> m2) {		
		if(m2!=null && m2.size()>0) {
			int count=0; for (Test test: m2.keySet()) {
				assert m2.get(test)!=null: test+" gives null in "+m2+" for "+study; 
				count+=m2.get(test).getFirst();
			}
			sb.append("<table><tr><td colspan=2>");
			sb.append("<a style='font-weight:bold' href='test:" + study.getStudyId() + "'>Results:</a>&nbsp;(" + count + ") >> <a href='analyze:" + study.getId() + "'>Analyze</a>");
			sb.append("</td></tr>");
			for (Test t: m2.keySet()) {					
				sb.append("<tr><td><a href='test:" + study.getStudyId() + ":" + t.getId() + "'>" + t.getName() + "</a>&nbsp;(" + m2.get(t).getFirst() + ")");																								
				sb.append("</td><td style='padding-left:5px;color:" + getColor(m2.get(t).getThird()) + "'>");
				sb.append("  [" + Formatter.formatDateOrTime(m2.get(t).getThird()) + " - " + m2.get(t).getSecond() +"]");
				sb.append("</td></tr>");
			}
			sb.append("</table>");
		}
	}



	private static Date lastDay;
	private static Date yesterday;

	public static String getColor(Date date) {
		if(lastDay==null) {
			//Set the last date to 0h
			Calendar cal = Calendar.getInstance();
			cal.setTime(JPAUtil.getCurrentDateFromDatabase());
			
			cal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY)-4);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			
			cal.set(Calendar.HOUR_OF_DAY, 0);
			lastDay = cal.getTime();
			
			cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR)-1);
			yesterday = cal.getTime();
		}
		if(date.after(lastDay)) return "#FF0000";
		if(date.after(yesterday)) return "#BB6666";
		return "#888888";
	}
}
