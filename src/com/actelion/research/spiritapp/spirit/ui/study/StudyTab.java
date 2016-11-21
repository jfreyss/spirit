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

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritTab;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.bgpane.JBGScrollPane;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class StudyTab extends JPanel implements ISpiritTab {
	private final StudyTable studyTable = new StudyTable();
	private final StudySearchPane searchPane = new StudySearchPane(studyTable);
	
	private final StudyDetailPanel studyDetailPanel = new StudyDetailPanel(JSplitPane.HORIZONTAL_SPLIT);

	private final JSplitPane northPane;
	private final JSplitPane contentPane;
	private boolean initialized = false;

	public StudyTab() {
		
		final JScrollPane studyScrollPane = new JBGScrollPane(studyTable, 3);

		
		northPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, searchPane, studyScrollPane);
		northPane.setDividerLocation(250);
		
		contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, northPane, studyDetailPanel);
		contentPane.setDividerLocation(250);
		contentPane.setOneTouchExpandable(true);
		
		studyDetailPanel.showInfos();
		
		studyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				final List<Study> sel = studyTable.getSelection();
				studyDetailPanel.setStudy(sel.size()==1? sel.get(0): null);
			}
		});		
		

		StudyActions.attachPopup(studyTable);
		StudyActions.attachPopup(studyScrollPane);
		StudyActions.attachPopup(studyDetailPanel);

		searchPane.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				StudyTab.this.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
			}
		});

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, contentPane);
		
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				if(getRootPane()!=null && searchPane!=null){
					getRootPane().setDefaultButton(searchPane.getSearchButton());		
					if(!initialized && getRootPane()!=null) {
						searchPane.reset();
						initialized = true;
					}
				}
			}
		});		
	}	

	@Override
	public<T> void fireModelChanged(SpiritChangeType action, Class<T> what, List<T> details) {
		if(!isShowing()) return;
		if(action==SpiritChangeType.MODEL_DELETED && what==Study.class) {
			studyTable.getModel().getRows().removeAll(details);
			studyTable.getModel().fireTableDataChanged();
			studyDetailPanel.setStudy(null);
		} else if(action==SpiritChangeType.MODEL_ADDED && what==Study.class) {
			setStudyIds(((Study)details.get(0)).getStudyId());
		} else if(action==SpiritChangeType.MODEL_UPDATED && what==Study.class) {
			setStudyIds(null);
			setStudyIds(((Study)details.get(0)).getStudyId());
		}
		
		studyTable.reload();
	}
			
	@Override
	public void refreshFilters() {
	}
	
	
	@Override
	public String getStudyIds() {
		String studyIds = searchPane.getSearchTree().getStudyIds();
		if(studyIds.length()==0) {
			StringBuilder sb = new StringBuilder();
			if(studyTable.getRowCount()<=1) {
				for(Study study: studyTable.getRows()) {
					sb.append((sb.length()>0?", ":"") + study.getStudyId());
				}				
			} else {
				for(Study study: studyTable.getSelection()) {
					sb.append((sb.length()>0?", ":"") + study.getStudyId());
				}
			}
			studyIds = sb.toString();
		}
		return studyIds;
	}
	
	@Override
	public void setStudyIds(final String studyIds) {
		this.initialized = true;
		if(studyIds==null || studyIds.length()==0) return;		
		if(studyIds.equals(getStudyIds())) return; //no need to refresh
		searchPane.setStudyIds(studyIds);
		
		//Execute this thread after the others		
		new SwingWorkerExtended("Loading Studies", studyTable, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
			List<Study> studies;
			@Override
			protected void doInBackground() throws Exception {
				StudyQuery q = new StudyQuery();
				q.setStudyIds(studyIds);
				
				studies = DAOStudy.queryStudies(q, Spirit.getUser());
			}
			@Override
			protected void done() {
				if(!studyTable.getRows().containsAll(studies)) {
					studyTable.setRows(studies);
				}
				studyTable.setSelection(studies);
			}
		};
	}
	
	
	public List<Study> getStudies() {
		return studyDetailPanel.getStudy()==null? new ArrayList<Study>(): Collections.singletonList(studyDetailPanel.getStudy());
	}
	

}
