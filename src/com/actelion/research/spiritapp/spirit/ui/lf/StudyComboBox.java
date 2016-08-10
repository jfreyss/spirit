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

package com.actelion.research.spiritapp.spirit.ui.lf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.UIUtils;

public class StudyComboBox extends JTextComboBox {

	private RightLevel level;
	private Map<String, Study> quickCache = new LinkedHashMap<>();
	
	public StudyComboBox() {
		this(null, null);
	}
	
	public StudyComboBox(String label) {
		this(null, label);
	}
	
	public StudyComboBox(RightLevel level) {
		this(level, null);
	}
	
	public StudyComboBox(RightLevel level, final String label) {
		super(true);
		setTextWhenEmpty(label);

		this.level = level;
		reload();
		
		setListCellRenderer(new DefaultListCellRenderer() {
	
			
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				Study study = quickCache==null? null: quickCache.get((String)value);
				String user = Spirit.getUser()==null? null: Spirit.getUser().getUsername();
				if(study==null) {
					setText("<html><div>" + value +"<br></html>");
				} else {
					String title = (study.getTitle()==null?"":study.getTitle());
					int maxLength = 100 - (study.getIvv()==null?0: study.getIvv().length()+2);
					if(title.length()>maxLength) title = title.substring(0,maxLength-2) + "...";
					boolean resp = study.isMentioned(user);
					
					setText("<html><div style='white-space:nowrap'>" +
							(study.getStudyId()!=null? "<b style='font-size:10px'>" + study.getStudyId() + "</b>:&nbsp;&nbsp;": "") +
							(study.getIvv()!=null? "<span style='font-size:10px'>"+study.getIvv()+"</span>&nbsp;&nbsp;": "") +
							"<span style='color:gray;white-space:nowrap;font-size:8px'>" + (study.getIvv()!=null? ": ": "") + title + "</span>" +
							"<span style='white-space:nowrap;font-size:8px'>" + (study.getCreUser()	!=null? " ["+study.getCreUser()+"]": "") + "</span>" +
							"</html>");
					if(resp) setBackground(UIUtils.getDilutedColor(getBackground(), Color.YELLOW, .9));
//					setIcon(study.getStatus()==StudyStatus.ONGOING? IconType.ORANGE_FLAG.getIcon(): study.getStatus()==StudyStatus.FINISHED? IconType.GREEN_FLAG.getIcon(): IconType.RED_FLAG.getIcon());
				}
				setPreferredSize(new Dimension(500, 20));
				return this;
			}
		});
		
		addPropertyChangeListener(JTextComboBox.PROPERTY_TEXTCHANGED, new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				cleanValue();
				
			}
		});
	}
	
	public void reload() {
		quickCache = null;
		loadStudies();
	}
	
	public void loadStudies() {
		if(quickCache==null) {
			setValues(DAOStudy.getRecentStudies(Spirit.getUser(), level));
		}
	}
	
	@Override
	public Collection<String> getChoices() {
		loadStudies();		
		return quickCache.keySet() ;
	}
	
	
	public void setValues(List<Study> studies) {
		quickCache = new LinkedHashMap<>();
		for (Study s : studies) {
			quickCache.put(s.getStudyId(), s);
		}
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
	
	
//	/**
//	 * TODO remove the reload (should not be done by component)
//	 * Use getText
//	 * @return
//	 */
//	@Deprecated
//	public Study getStudy() {
//		String s = getText();
//		Study study = quickCache==null? null: quickCache.get(s);
//		if(study==null) { //always load from db to have a valid study
//			study = DAOStudy.getStudyByStudyId(s);
//		} else {
//			study = JPAUtil.reattach(study);
//		}
//			
//		if(level==RightLevel.BLIND && !SpiritRights.canBlind(study, Spirit.getUser())) return null;
//		if(level==RightLevel.WRITE && !SpiritRights.canEdit(study, Spirit.getUser())) return null;
//		if(level==RightLevel.READ && !SpiritRights.canRead(study, Spirit.getUser())) return null;
//		if(level==RightLevel.VIEW && !SpiritRights.canView(study, Spirit.getUser())) return null;
//		return study;
//	}

	
}
