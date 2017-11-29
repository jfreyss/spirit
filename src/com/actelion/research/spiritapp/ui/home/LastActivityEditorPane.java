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

package com.actelion.research.spiritapp.ui.home;

import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.study.StudyActions;
import com.actelion.research.spiritapp.ui.study.StudyEditorPane;
import com.actelion.research.spiritapp.ui.study.depictor.StudyDepictor;
import com.actelion.research.spiritapp.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritapp.ui.util.lf.SpiritHyperlinkListener;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.HSQLFileAdapter;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Triple;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.WikiNewsFeed;
import com.actelion.research.util.WikiNewsFeed.News;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class LastActivityEditorPane extends ImageEditorPane {

	private boolean showDesign = false;
	private boolean showExpertOnly = true;
	private int days;
	private final static Hashtable<String, Image> imageCache = new Hashtable<>();

	public LastActivityEditorPane(final SpiritFrame spirit) {
		super(imageCache);
		setOpaque(false);
		setEditable(false);

		days = SpiritProperties.getInstance().getValueInt(PropertyKey.SYSTEM_HOMEDAYS);
		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.add(new JLabel("TEST"));
		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				popupMenu.removeAll();
				Point pt = getMousePosition();
				int pos = viewToModel(pt);
				try {
					String s = getDocument().getText(Math.max(0, pos-30), 60);
					//Find study
					Matcher m = Pattern.compile("S-\\d{5}").matcher(s);
					if(m.find()) {
						String studyId = m.group();
						Study study = DAOStudy.getStudyByStudyId(studyId);
						if(study!=null) {
							JPopupMenu menu = StudyActions.createPopup(study);
							System.out.println("LastActivityEditorPane "+studyId);
							for(Component comp : menu.getComponents()) {
								System.out.println(comp);
								popupMenu.add(comp);
							}
						}
					}
				} catch(Exception ex) {
					//Ignore
				}
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {}
		});
		setComponentPopupMenu(popupMenu);
		addHyperlinkListener(new SpiritHyperlinkListener());
		addHyperlinkListener(e-> {
			if(e.getEventType()==EventType.ACTIVATED) {
				if(e.getDescription().startsWith("dateQuery:")) {
					days = Integer.parseInt(e.getDescription().substring(e.getDescription().indexOf(":")+1));
					updateRecentChanges();
				} else if(e.getDescription().startsWith("dateType:")) {
					updateRecentChanges();
				} else if(e.getDescription().startsWith("design:true")) {
					showDesign = true;
					updateRecentChanges();
				} else if(e.getDescription().startsWith("design:false")) {
					showDesign = false;
					updateRecentChanges();
				} else if(e.getDescription().startsWith("expert:true")) {
					showExpertOnly = true;
					updateRecentChanges();
				} else if(e.getDescription().startsWith("expert:false")) {
					showExpertOnly = false;
					updateRecentChanges();
				} else if(e.getDescription().startsWith("refresh:")) {
					if(spirit!=null) {
						new SwingWorkerExtended("Refreshing", LastActivityEditorPane.this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
							@Override
							protected void done() {
								try {
									JPAUtil.closeFactory();
									spirit.recreateUI();
								} catch(Exception e) {
									JExceptionDialog.showError(e);
								}
							}
						};
					}
				}
			}
		});

		updateEditorPane();
		updateNews();
		updateRecentChanges();
	}

	private String recentChanges = "";
	private String recentNews = "";

	public void updateNews() {
		//Use a Thread and not the swingworkerextended to avoid timeouts
		new Thread() {
			@Override
			public void run() {
				recentNews = getNews();
				SwingUtilities.invokeLater(() -> updateEditorPane());
			}
		}.start();
	}

	public void updateRecentChanges() {
		recentChanges = "Analyzing your last activity...<br>";
		updateEditorPane();

		new SwingWorkerExtended("Loading Activity...", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			@Override
			protected void doInBackground() throws Exception {
				recentChanges = getRecentChangesFromDatabase();
			}

			@Override
			protected void done() {
				updateEditorPane();
			}

		};
	}

	private void updateEditorPane() {
		setText("<html><body style='font-family:Arial'>"
				+ recentNews
				+ "<div style='background:#FAFAFA; padding:5px; border:solid 1px #550000; width:100%'>"
				+ recentChanges
				+ "</div>"
				+ "</body></html>");
		setCaretPosition(0);

	}


	private String getNews() {
		StringBuilder newsBuilder = new StringBuilder();
		if(DBAdapter.getInstance().isInActelionDomain()) {
			//News from the ACT wiki
			try {
				List<News> news = WikiNewsFeed.getNews("Documentation.Spirit.News");
				if(news.size()>0) {
					newsBuilder.append("<div style='margin-bottom:15px;color:#550000;background:#EEEEDD; border:solid 1px #550000; padding:3px;width:100%'>");
					newsBuilder.append("<div style='color:black;font-weight:bold;background:#FFFFAA;width:100%;padding:2px;border-bottom:solid 1px #550000'>Here are some important changes</div>");
					for (News n: news) {
						newsBuilder.append("<div style='padding:5px'>");
						newsBuilder.append("<span style='font-size:80%'>"+FormatterUtils.formatDate(n.getDate()) + "</span><br>");
						if(n.getTitle()!=null && n.getTitle().length()>0) newsBuilder.append("<b>" + n.getTitle() + "</b><br>");
						if(n.getContent()!=null && n.getContent().length()>0) newsBuilder.append(n.getContent());
						newsBuilder.append("</div>");

					}
					newsBuilder.append("</div>");
				}

			} catch(Exception e) {
				e.printStackTrace();
			}
		} else if(DBAdapter.getInstance().getClass()==HSQLFileAdapter.class) {
			try {
				List<Study> studies = DAOStudy.queryStudies(StudyQuery.createForState("EXAMPLE"), null);
				if(studies.size()>0 && (System.currentTimeMillis() - studies.get(0).getUpdDate().getTime()<7*24*3600*1000L)) {
					newsBuilder.append("<div style='margin-bottom:15px;color:#550000;background:#FFFFDD; border:solid 1px #550000; padding:0px;width:100%'>");
					newsBuilder.append("<div style='color:black;font-weight:bold; background:#FFFFAA;width:100%;padding:2px;border-bottom:solid 1px #550000'>Spirit LIMS</div>");
					newsBuilder.append("<div style='padding:2px'><p>The Spirit database is now created and prefilled with some example data (tests/biotypes/studies).</p>"
							+ "<p>It is locally available on your computer, but it is recommended to setup your own database and configure it in <i>Admin: Database Settings</i></p>");
					newsBuilder.append("<p>Spirit is divided into 5 tabs:</p>");
					newsBuilder.append("<ul>");
					newsBuilder.append("<li><b>Home:</b>shows the latest changes to the database, links are used to quickly access recent studies</li>");
					newsBuilder.append("<li><b>Study:</b>to query/view the design of studies and the attached samples</li>");
					newsBuilder.append("<li><b>Biosamples:</b> to query/view the biosamples</li>");
					newsBuilder.append("<li><b>Locations:</b> to query/view the content of locations</li>");
					newsBuilder.append("<li><b>Results:</b> to query/view the content of results</li>");
					newsBuilder.append("</ul>");
					newsBuilder.append("</div></div>");
				}
			} catch(Exception e) {

			}

		}

		return newsBuilder.toString();
	}


	public static String getStudyTableRows(Collection<Study> studies, boolean showDesign, Map<Study, String> extraColumn) {

		StringBuilder sb = new StringBuilder();


		//Count Biosamples/Results
		Map<Study, Map<Biotype, Triple<Integer, String, Date>>> countBio = DAOStudy.countSamplesByStudyBiotype(studies);
		Map<Study, Map<Test, Triple<Integer, String, Date>>> countRes = DAOStudy.countResultsByStudyTest(studies);

		SpiritUser user = SpiritFrame.getUser();
		for(Study s: studies) {
			//			Pair<String, Date> last = getLastUserIdDate(s, countBio, countRes);

			boolean isResp = s.isMember(user.getUsername());
			sb.append("<tr style='margin:0px; padding:0px; border: solid 1px #CCCCCC;" + (isResp?"background:#FFFFEE":"background:#FFFFFF") + "'>");

			if(extraColumn!=null) {
				if(extraColumn.get(s)!=null) {
					sb.append(extraColumn.get(s));
				} else {
					sb.append("<td></td>");
				}
			}
			sb.append("<td valign=top style='width:50%;white-space:nowrap; margin:0px; padding:0px; border-right: dashed 1px #EEEEEE'>");
			{
				//StudyId
				sb.append("<span style='font-size:130%'><a href='stu:" + s.getId() + "' style='font-weight:bold'>" + s.getStudyId() + "</a> ");
				if(s.getLocalId()!=null && s.getLocalId().length()>0) sb.append(" / <b>" + s.getLocalId() + "</b>");
				sb.append("</span>");

				//StudyDates / States
				sb.append("<div style='font-size:100%%'>");
				sb.append(s.getState());
				if(s.getFirstDate()!=null) {
					Date startDate = s.getFirstDate();
					Date endDate = s.getLastDate();
					sb.append("&nbsp;&nbsp;<b style='color:black'>" + FormatterUtils.formatDateFull(startDate) + "</b> ---&gt; <b style='color:black'>" + FormatterUtils.formatDateFull(endDate) + "</b>&nbsp;&nbsp;");
				}
				sb.append("<span style='color:" + StudyEditorPane.getColor(s.getUpdDate()) + "'> [" +  FormatterUtils.formatDateOrTime(s.getUpdDate())+" - " + s.getUpdUser() +"] </span><br> ");
				sb.append("</div>");

				//StudyTitle
				if(s.getTitle()!=null) sb.append(" <div style='white-space:wrap;'>" + s.getTitle() + "</div>");
				if(s.getNotes()!=null && s.getNotes().length()>0) {
					String notes = s.getNotes();
					if(notes.length()>500) {
						notes = notes.substring(0, 500);
					}
					sb.append(" <div style='white-space:wrap;color:#444444; margin:2px'>" + MiscUtils.convert2Html(notes) + "</div>");
				}
			}

			sb.append("</td><td valign=top width=400 style='white-space:nowrap; margin: 0px; padding:0px'>");
			{
				//Biosample and results
				sb.append("<table><tr><td align=top width=200 style='padding-left:3px'>");
				StudyEditorPane.formatNumberBiosamples(sb, s, countBio.get(s));
				sb.append("</td><td align=top width=200>");
				if(SpiritProperties.getInstance().isChecked(PropertyKey.TAB_RESULT)) {
					StudyEditorPane.formatNumberResults(sb, s, countRes.get(s));
				}
				sb.append("</td style='padding-left:3px'></tr>");
				sb.append("<td style='width:100%'></td>");
				sb.append("</table>");
			}
			sb.append("</td>");



			if(showDesign) {
				sb.append("<td valign=top style='white-space:nowrap; margin:0.5px 0px 0px 0px; padding:0px>");
				//Display design
				try {
					String url = "study_"+s.getId();
					if(imageCache.get(url)==null) {
						BufferedImage img = StudyDepictor.getImage(s, 500, 90);
						imageCache.put(url, img);
					}
					sb.append("<a href='stu:" + s.getId() + "'>");
					sb.append("<img border=0 src='" + url + "'>");
					sb.append("</a>");
				} catch (Exception e) {
					e.printStackTrace();
				}
				sb.append("</td>");
			}
			sb.append("</tr>");


		}
		return sb.toString();

	}


	private String getRecentChangesFromDatabase() {

		//Query last changes
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(JPAUtil.getCurrentDateFromDatabase());
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR)-days);
			Date date = cal.getTime();

			List<Study> studies = new ArrayList<>();
			for(Study s: DAOStudy.getRecentChanges(date)) {
				if(showExpertOnly && !SpiritRights.canBlind(s, SpiritFrame.getUser())) continue;
				if(!showExpertOnly && !SpiritRights.canRead(s, SpiritFrame.getUser())) continue;
				studies.add(s);
			}
			StringBuilder sb = new StringBuilder();
			sb.append(getStudyTableRows(studies, showDesign, null));

			Map<Biotype, Triple<Integer, String, Date>> m1 = DAOStudy.countRecentSamplesByBiotype(date);
			if(m1!=null) {
				sb.append("<tr style='margin:0.5px 1px 0px 0px; padding:0px'>");
				sb.append("<td valign=top>");

				sb.append("<table style='padding:2px'>");
				for (Biotype type: m1.keySet()) {
					sb.append("<tr><td>");
					sb.append("<a href='bios::" + type.getName() + ":" + days + "'><b>" + type.getName() + "</b></a> (" + m1.get(type).getFirst() + ") ");
					sb.append("</td><td style='padding-left:5px'>");
					sb.append("<span style='color:" + StudyEditorPane.getColor(m1.get(type).getThird()) + "'>" + FormatterUtils.formatDateOrTime(m1.get(type).getThird())+ " - " + m1.get(type).getSecond() +"</span>");
					sb.append("</td></tr>");
				}
				sb.append("</table>");
				sb.append("</td></tr>");
			}

			StringBuilder sb2 = new StringBuilder();
			sb2.append("<table width=100%><tr><td width=50%>Welcome, <b>" + MiscUtils.removeHtml(SpiritFrame.getUsername()) + "</b> <br>here are the recent changes made to the database:</td>");
			sb2.append("<td width=50% style='white-space:nowrap;text-align:right'>(");
			for(int d: new int[]{1,3,7,15,31,90,365}) {
				if(d==days) {
					sb2.append("<b>" + d + " day"+ (d<=1? "": "s") + "</b> | ");
				} else {
					sb2.append("<a href='dateQuery:"+d+"'>" + d + " day"+ (d<=1? "": "s") + "</a> | ");
				}
			}
			sb2.setLength(sb2.length()-3);
			sb2.append(")<br>");
			if(!showDesign) {
				sb2.append("(<a href='design:true'>Show Design</a> | <b>Hide Design</b>)");
			} else {
				sb2.append("(<b>Show Design</b> | <a href='design:false'>Hide Design</a>)");
			}
			if(showExpertOnly) {
				sb2.append(" &nbsp;&nbsp; (<a href='expert:false'>With Read Access</a> | <b>With Write Access</b>)");
			} else {
				sb2.append(" &nbsp;&nbsp; (<b>With Read Access</b> | <a href='expert:true'>With Write Access</a>)");
			}
			sb2.append("</td></tr></table>");

			//Display
			if(sb.length()==0) {
				sb2.append("<br><br>No changes were made in the last " + days + " day" + (days<=1? "": "s"));
			} else {
				sb2.append("<br><br><table style='width:100%; background:#EEEEEE;'>" + sb + "</table>");
			}
			return sb2.toString();
		} catch (Throwable e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}


}
