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

import java.awt.Desktop;
import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;

import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritapp.spirit.ui.util.lf.LF;
import com.actelion.research.spiritapp.spirit.ui.util.lf.SpiritHyperlinkListener;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Triple;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.IOUtils;
import com.actelion.research.util.ui.JExceptionDialog;

public class StudyQuickLinksPane extends ImageEditorPane {

	private Study study;

	private final String NIOBE_LINK = "\\\\actelch02\\PGM\\ActelionResearch\\Niobe\\Niobe.lnk";

	public StudyQuickLinksPane() {
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
					} else {
						super.hyperlinkUpdate(e);
					}

				}
			}
		});


	}

	public void setStudy(final Study study) {
		this.study = study;
		//Set the editor pane text
		final StringBuilder sb = new StringBuilder();
		if(study!=null) {
			sb.append("<html><body>");

			/////////////////////////////////////
			//Study Infos

			sb.append("<span style='font-size:120%;font-weight:bold'>" + study.getStudyId() + "</span> ");
			if(study.getLocalId()!=null) sb.append(" / " + MiscUtils.removeHtml(study.getLocalId()) + "");



			try {
				sb.append("<table style='white-space:nowrap;width:100%;padding-top:5px;padding-bottom:5px;font-size:98%'><tr><td valign=top>");

				//Count Biosamples
				Map<Study, Map<Biotype, Triple<Integer, String, Date>>> countBio = DAOStudy.countSamplesByStudyBiotype(Collections.singletonList(study));
				formatNumberBiosamples(sb, study, countBio.get(study));

				sb.append("</td><td>&nbsp;&nbsp;</td><td style='border-left:solid 1px lightgray'>&nbsp;</td><td valign=top>");

				//Count Results
				Map<Study, Map<Test, Triple<Integer, String, Date>>> countRes = DAOStudy.countResultsByStudyTest(Collections.singletonList(study));
				formatNumberResults(sb, study, countRes.get(study));

				sb.append("</td></tr></table>");
			} catch(Exception e) {
				e.printStackTrace();
				sb.append(e.getMessage());
			}

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
			sb.append("<a style='font-weight:bold' href='test:" + study.getStudyId() + "'>Results:</a>&nbsp;(" + count + ") ");
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
