/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.ui.util.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Collection;

import javax.swing.JLabel;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JObjectComboBox;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.UIUtils;

/**
 * ComboBox to allow the user to select a study, based on his user rights. The ComboBox can be made multiple and the level of rights can
 * be configured (READ per default)
 *
 * @author Joel Freyss
 */
public class StudyComboBox extends JObjectComboBox<Study> {

	private RightLevel level;
	private StudyLabel studyLabel = new StudyLabel();

	private class StudyLabel extends JLabel {
		Study study;

		public StudyLabel() {}

		private void setStudy(Study s) {
			this.study = s;
		}

		@Override
		protected void paintComponent(Graphics g) {
			UIUtils.applyDesktopProperties(g);
			super.paintComponent(g);
			String user = SpiritFrame.getUser()==null? null: SpiritFrame.getUser().getUsername();
			if(study==null) {
				setText(" ");
				return;
			} else {
				String title = (study.getTitle()==null?"":study.getTitle());
				boolean resp = study.isMember(user);

				Color bg = getBackground();
				if(resp) {
					bg = UIUtils.getDilutedColor(bg, Color.YELLOW, .9);
				}

				g.setColor(bg);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(getForeground());
				g.setFont(FastFont.BOLD);
				int x = 2;
				int y = getHeight()-4;
				g.drawString(study.getStudyId(), x, y);
				x += g.getFontMetrics().stringWidth(study.getStudyId()) + 5;

				if(study.getLocalId()!=null && study.getLocalId().length()>0) {
					g.setFont(FastFont.REGULAR);
					g.drawString(study.getLocalId(), x, y);
					x += g.getFontMetrics().stringWidth(study.getLocalId()) + 3;
				}
				g.setFont(FastFont.SMALL);
				g.drawString(title, x, y);
				x += g.getFontMetrics().stringWidth(title);

				if(study.getCreUser()!=null && study.getCreUser().length()>0) {
					String s = "[" + study.getCreUser() + "]";
					g.setColor(bg);
					g.fillRect(getWidth() - g.getFontMetrics().stringWidth(s) - 4, 0, getWidth(), getHeight());
					g.setColor(Color.GRAY);
					g.drawString(s, getWidth() - g.getFontMetrics().stringWidth(s) - 2, y);
				}
			}
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(500, 20);
		}
	}

	public StudyComboBox() {
		this(RightLevel.READ, "StudyId");
	}

	public StudyComboBox(String label) {
		this(RightLevel.READ, label);
	}

	public StudyComboBox(RightLevel level) {
		this(level, "StudyId");
	}

	@Override
	public Study getSelection() {
		Study study = super.getSelection();
		if(study!=null) return study;
		return DAOStudy.getStudyByStudyId(getText());
	}

	public StudyComboBox(RightLevel level, final String label) {
		setTextWhenEmpty(label);
		setAllowTyping(true);
		this.level = level;

		addPropertyChangeListener(JTextComboBox.PROPERTY_TEXTCHANGED, evt-> {
			cleanValue();
		});
	}

	@Override
	public Component processCellRenderer(JLabel comp, String value, int index) {
		Study study = getMap().get(value);
		if(study==null) {
			studyLabel.setStudy(null);
		} else {
			studyLabel.setStudy(study);
		}
		return studyLabel;
	}

	public void setLevel(RightLevel level) {
		this.level = level;
	}

	@Override
	public Collection<Study> getValues() {
		return DAOStudy.getRecentStudies(SpiritFrame.getUser(), level);
	}

	public static String cleanValue(String studyIds) {
		String[] tokens = MiscUtils.split(studyIds);
		StringBuilder sb = new StringBuilder();
		for (String sid : tokens) {
			if(!sid.toUpperCase().equals(sid)) {
				sid = sid.toUpperCase();
			}
			if(sid.length()>0 && sid.length()<7 && !sid.equals("0")) {
				//Normalize the studyId
				String s = sid.startsWith("S-")? sid.substring(2): sid.startsWith("S")? sid.substring(1): sid;
				try {
					Double.parseDouble(s);
					while(s.length()<5) s = "0" + s;
					sid = "S-" + s;
				} catch(Exception e) {
					//nothing
				}
			}
			if(sb.length()>0) sb.append(", ");
			sb.append(sid);
		}
		return sb.toString();
	}

	private void cleanValue() {
		if(getText()==null) return;

		String newVal = cleanValue(getText());
		if(!getText().equals(newVal)) {
			setText(newVal);
		}
	}

	@Override
	public String convertObjectToString(Study obj) {
		return obj==null?"": obj.getStudyId();
	}


}
