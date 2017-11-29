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

package com.actelion.research.spiritapp.ui.study;

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

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;

import com.actelion.research.spiritapp.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritapp.ui.util.lf.LF;
import com.actelion.research.spiritapp.ui.util.lf.SpiritHyperlinkListener;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.Document.DocumentType;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOResult.ElbLink;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Triple;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.IOUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class StudyEditorPane extends ImageEditorPane {

	private Study study;

	//	private boolean displayResults = true;
	//	private boolean forRevision;
	private boolean simplified = false;
	private final String NIOBE_LINK = "\\\\actelch02\\PGM\\ActelionResearch\\Niobe\\Niobe.lnk";
	private String sort = "date";

	public StudyEditorPane() {
		super();
		setEditable(false);
		setOpaque(true);
		LF.initComp(this);


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

	public void setSimplified(boolean simplified) {
		this.simplified = simplified;
	}

	public boolean isSimplified() {
		return simplified;
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
			sb.append("<html><body>");

			/////////////////////////////////////
			//Study Infos

			sb.append("<span style='font-size:125%;font-weight:bold'>" + study.getStudyId() + "</span> ");
			if(study.getLocalId()!=null && study.getLocalId().length()>0) sb.append(" / " + MiscUtils.removeHtml(study.getLocalId()) + "");
			sb.append("<br>");
			if(study.getTitle()!=null) {
				sb.append("<span style='font-size:95%'>" + MiscUtils.removeHtml(study.getTitle()) + "</span><br>");
			}

			sb.append("<table style='width:100%;border-top:solid 1px gray;border-bottom:solid 1px gray'><tr><td valign=top>");
			{
				//Display metadata
				sb.append("<table>");
				if(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES).length>0) {
					sb.append("<tr><td>State:</td><td style='white-space:nowrap'>" + (study.getState()==null?"": MiscUtils.removeHtml(study.getState())) + "</td></tr>");
				}
				if(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_TYPES).length>0) {
					sb.append("<tr><td>Type:</td><td style='white-space:nowrap'><b>" + MiscUtils.removeHtml(study.getType()==null || study.getType().length()==0?"NA": study.getType()) + "</b></td></tr>");
				}
				for(Entry<String, String> entry: study.getMetadataMap().entrySet()) {
					String name = SpiritProperties.getInstance().getValue(PropertyKey.STUDY_METADATA_NAME, entry.getKey());
					if(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_TYPES).length>0 && !MiscUtils.contains(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_TYPES), study.getType())) {
						continue;
					}
					sb.append("<tr><td style='white-space:nowrap'>" + MiscUtils.removeHtml(name) + ":</td><td style='white-space:nowrap'>" + MiscUtils.removeHtml(entry.getValue()) + "</td></tr>");
				}
				sb.append("</table>");
			}
			sb.append("</td><td>&nbsp;&nbsp;</td><td valign=top style='border-left:solid 1px light-gray'>&nbsp;</td><td valign=top>");
			{
				//Display state/rights
				sb.append("<table>");

				if(SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)) {
					Set<String> adminSet = new LinkedHashSet<>();
					adminSet.addAll(Arrays.asList(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES_ADMIN, study.getState())));
					adminSet.addAll(study.getAdminUsersAsSet());
					sb.append("<tr><td>Admin:</td><td>" + (adminSet.size()==0?"-": MiscUtils.flatten(adminSet, ", ")) + "</td></tr>");

					Set<String> expertSet = new LinkedHashSet<>();
					expertSet.addAll(Arrays.asList(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES_EXPERT, study.getState())));
					expertSet.addAll(study.getExpertUsersAsSet());
					expertSet.addAll(EmployeeGroup.getNames(study.getEmployeeGroups()));
					if(expertSet.size()>0) {
						sb.append("<tr><td>Expert:</td><td>" + (expertSet.size()==0? "-": MiscUtils.flatten(expertSet, ", ")) + "</td></tr>");
					}

					Set<String> viewSet = new LinkedHashSet<>();
					viewSet.addAll(Arrays.asList(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES_READ, study.getState())));
					if(viewSet.size()>0) {
						sb.append("<tr><td>Read:</td><td>" + (viewSet.size()==0? "-": MiscUtils.flatten(viewSet, ", ")) + "</td></tr>");
					}

					if(study.getBlindDetailsUsersAsSet().size()>0) sb.append("<tr><td>Blind-Names:</td><td>" + MiscUtils.flatten(study.getBlindDetailsUsersAsSet(), ", ") + "</td></tr>");
					if(study.getBlindAllUsersAsSet().size()>0) sb.append("<tr><td>Blind-All:</td><td>" + MiscUtils.flatten(study.getBlindAllUsersAsSet(), ", ") + "</td></tr>");

				}
				sb.append("</table>");
			}
			sb.append("</tr></td></table>");

			//Display notes
			if(study.getNotes()!=null && study.getNotes().length()>0) {
				sb.append("<div style='width:100%;padding-top:5px;font-size:95%;padding-bottom:5px;border-bottom:solid 1px gray'>" + MiscUtils.convert2Html(study.getNotes()) + "</div>");
			}

			if(!simplified) {
				if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_ADVANCEDMODE)) {
					//Add the treatment info
					if(study.getNamedTreatments().size()>0) {
						sb.append("<h2 style='font-size:12px; color:#990000'>Treatments</h2>");
						sb.append("<table style='white-space:nowrap>");
						for (NamedTreatment nt : study.getNamedTreatments()) {
							sb.append("<tr><td style='color:" + UIUtils.getHtmlColor(nt.getColor()) + "'>" + nt.getName() + "</td><td>" + nt.getCompoundAndUnits() + "</td></tr>");
						}
						sb.append("</table>");
					}

					//Add the sampling info
					if(study.getNamedSamplings().size()>0) {
						sb.append("<h2 style='font-size:12px; color:#990000'>Samplings</h2>");
						sb.append("<table>");
						for (NamedSampling ns : study.getNamedSamplings()) {
							sb.append("<tr><td valign=top style='white-space:nowrap;margin:1px'>");
							if(ns.getName()!=null) sb.append("<b style='font-size:11px'><u>" + ns.getName() + "</u></b><br>");

							sb.append(ns.getHtmlBySampling());
							sb.append("</td></td>");
						}
						sb.append("</tr></table>");
					}
				}
				//Display Documents
				Map<DocumentType, List<Document>> docs = Document.mapDocumentTypes(study.getDocuments());
				if(study.getDocuments().size()>0) {
					for (DocumentType docType : docs.keySet()) {
						sb.append("<div style='margin-top:5px;font-size:95%'><b>"+(docType!=null? docType + "</b>:<br>": ""));
						for (Document doc : docs.get(docType)) {
							String name = doc.getFileName();
							if(name.length()>40) name = name.substring(0, 30) + "..." + name.substring(name.length()-8);
							sb.append(" - <a href='file:" + doc.getId() + "'>" + name + "</a><br>");
						}
						sb.append("</div>");
					}
				}

				if(DBAdapter.getInstance().isInActelionDomain()) {
					//Find related ELBs
					List<ElbLink> elbs = DAOResult.getNiobeLinksForStudy(study);
					if(elbs.size()>0) {

						sb.append("<b>Niobe:</b");

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
							sb.append("    [ <a style='font-size:80%' href='study:" + study.getStudyId() + "'>Query in Niobe</a> ]");
						}
						sb.append("<br>");

						//Display all Elbs
						sb.append("<table style='border:0; padding:1px;font-size:95%'>");
						sb.append("<tr style='background:#EEEEDD;padding:0px'><th colspan=2><a href=\"sort:elb\">ELB<a> / <a href=\"sort:title\">Title<a></th><th><a href=\"sort:scientist\">Scientist<a></th><th><a href=\"sort:date\">Date<a></th></tr>");
						for (int i = 0; i < elbs.size(); i++) {
							ElbLink elbLink = elbs.get(i);
							sb.append("<tr style='background:#FFFFEE'>");
							sb.append("<td>&nbsp;<b>" + elbLink.getElb() + "</b></td>");
							sb.append("<td style='white-space:nowrap;'>&nbsp; [ ");
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
								sb.append("<td style='color:#999999;>&nbsp;" + elbLink.getScientist() + "</td>");
								sb.append("<td style='color:#999999;white-space:nowrap'>&nbsp; " + FormatterUtils.formatDate(elbLink.getCreDate()) + " -> " + (elbLink.getPubDate()==null?" Unsealed": FormatterUtils.formatDate(elbLink.getPubDate()))+"</td>");
							}
							sb.append("</tr>");
							if(elbLink.isInNiobe() && elbLink.getTitle()!=null ) {
								sb.append("<tr style='background:#FFFFEE'><td colspan=4 style='background:#FFFFFF'>");
								sb.append(elbLink.getTitle());
								sb.append("</td></tr>");
							}
						}
						sb.append("</table>");
					}
				}


				sb.append("<div style='font-size:90%; color:gray'>");
				sb.append("Created the " + FormatterUtils.formatDateTimeShort(study.getCreDate()) + " by " + study.getCreUser() + "<br>");
				if (study.getUpdDate() != null && study.getUpdDate().getTime() > study.getCreDate().getTime() + 1000) {
					sb.append("Updated the " + FormatterUtils.formatDateTimeShort(study.getUpdDate()) + " by " + study.getUpdUser() + "<br>");
				}

				sb.append("</body></html>");
			}
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
				sb.append("<tr><td><a href='bios:" + study.getStudyId() + ":" + t.getName() + "'>" + t.getName() + "</a>&nbsp;<span style='font-size:90%'>(" + m1.get(t).getFirst() + ")</span>");
				sb.append("</td><td style='padding-left:5px;color:" + getColor(m1.get(t).getThird()) + "'>");
				sb.append(" <span style='font-size:90%'> [" + FormatterUtils.formatDateOrTime(m1.get(t).getThird()) + " - " + m1.get(t).getSecond() +"]</span>");
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
			sb.append("<a style='font-weight:bold' href='test:" + study.getStudyId() + "'>Results:</a>&nbsp;(" + count + ") "
					//					+ ">> <a href='analyze:" + study.getId() + "'>Analyze</a>"
					);
			sb.append("</td></tr>");
			for (Test t: m2.keySet()) {
				sb.append("<tr><td><a href='test:" + study.getStudyId() + ":" + t.getId() + "'>" + t.getName() + "</a>&nbsp;<span style='font-size:90%'>(" + m2.get(t).getFirst() + ")</span>");
				sb.append("</td><td style='padding-left:5px;color:" + getColor(m2.get(t).getThird()) + "'>");
				sb.append(" <span style='font-size:90%'> [" + FormatterUtils.formatDateOrTime(m2.get(t).getThird()) + " - " + m2.get(t).getSecond() +"]</span>");
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
