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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.ui.util.formtree.TextComboBoxNode;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.UIUtils;

public class StudyNode extends TextComboBoxNode {

	private RightLevel level;
	private Map<String, Study> quickCache = null;

	public StudyNode(FormTree tree, RightLevel level, boolean multiple, Strategy<String> strategy) {
		super(tree, "StudyId", strategy);
		this.level = level;

		getComponent().setMultipleChoices(multiple);
		getComponent().setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				Study study = quickCache.get(value);
				String user = SpiritFrame.getUser()==null? null: SpiritFrame.getUser().getUsername();
				if(study==null) {
					setText("<html><div>" + value +"<br></html>");
				} else {
					String title = (study.getTitle()==null?"":study.getTitle());
					int maxLength = 100 - (study.getLocalId()==null?0: study.getLocalId().length()+2);
					if(title.length()>maxLength) title = title.substring(0,maxLength-2) + "...";
					boolean resp = study.isMember(user);
					setText("<html><div style='white-space:nowrap'>" +
							(study.getStudyId()!=null? "<b style='font-size:10px'>" + study.getStudyId() + "</b>:&nbsp;&nbsp;": "") +
							(study.getLocalId()!=null? "<span style='font-size:10px'>"+study.getLocalId()+"</span>&nbsp;&nbsp;": "") +
							"<span style='color:gray;white-space:nowrap;font-size:8px'>" + (study.getLocalId()!=null? ": ": "") + title + "</span>" +
							"<span style='white-space:nowrap;font-size:8px'>" + (study.getCreUser()	!=null? " ["+study.getCreUser()+"]": "") + "</span>" +
							"</html>");
					if(resp) setBackground(UIUtils.getDilutedColor(getBackground(), Color.YELLOW, .9));
				}
				setPreferredSize(new Dimension(500, 20));
				return this;
			}
		});

		getComponent().addPropertyChangeListener(JTextComboBox.PROPERTY_TEXTCHANGED, e-> cleanValue());
	}

	private void cleanValue() {
		if(getSelection()==null) return;
		String newVal = StudyComboBox.cleanValue(getSelection());
		if(!getSelection().equals(newVal)) {
			setSelection(newVal);
		}
	}

	public void loadStudies() {
		if(quickCache==null && SpiritFrame.getUser()!=null) {
			quickCache = new LinkedHashMap<>();
			List<String> l = new ArrayList<>();
			for (Study s : DAOStudy.getRecentStudies(SpiritFrame.getUser(), level)) {
				l.add(s.getStudyId());
				quickCache.put(s.getStudyId(), s);
			}
		}
	}

	@Override
	public Collection<String> getChoices() {
		if(SpiritFrame.getUser()==null) return new ArrayList<String>();
		loadStudies();

		return quickCache.keySet() ;
	}

	public void setSelection(String studyId) {
		getComponent().setText(studyId);
	}

	public Study getStudy() {
		List<Study> res = getStudies();
		return res.size()==1? res.get(0): null;
	}

	public List<Study> getStudies() {
		String sel = getSelection();
		List<Study> res = new ArrayList<Study>();
		for(String s: MiscUtils.split(sel)) {
			Study study = quickCache==null? null: quickCache.get(s);
			if(study==null) {
				study = DAOStudy.getStudyByStudyId(s);
			}
			if(study==null || !SpiritRights.canEditBiosamples(study, SpiritFrame.getUser())) continue;
			res.add(study);
		}
		return res;
	}

	public void repopulate() {
		quickCache=null;
	}


}
