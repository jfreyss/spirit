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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.ui.IStudyTab;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.SpiritTab;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.bgpane.JBGScrollPane;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.iconbutton.IconType;

public class StudyTab extends SpiritTab implements IStudyTab {

	private final StudyTable studyTable = new StudyTable();
	private final StudySearchPane searchPane;

	private final StudyDetailPanel studyDetailPanel = new StudyDetailPanel(JSplitPane.HORIZONTAL_SPLIT);

	private boolean initialized = false;

	public StudyTab(SpiritFrame frame) {
		super(frame, "Studies", IconType.STUDY.getIcon());
		searchPane = new StudySearchPane(frame, studyTable);
		final JScrollPane studyScrollPane = new JBGScrollPane(studyTable, 3);


		JSplitPane northPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, searchPane, studyScrollPane);
		northPane.setDividerLocation(250);

		JSplitPane contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, northPane, studyDetailPanel);
		contentPane.setDividerLocation(250);
		contentPane.setOneTouchExpandable(true);

		studyDetailPanel.showInfos();

		studyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				final List<Study> sel = studyTable.getSelection();
				studyDetailPanel.setStudy(sel.size()==1? sel.get(0): null);

				frame.setStudyId(MiscUtils.flatten(Study.getStudyIds(sel)));
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


	}

	@Override
	public<T> void fireModelChanged(SpiritChangeType action, Class<T> what, Collection<T> details) {
		if(!isShowing()) return;
		if(action==SpiritChangeType.MODEL_DELETED && what==Study.class) {
			studyTable.getModel().getRows().removeAll(details);
			studyTable.getModel().fireTableDataChanged();
			studyDetailPanel.setStudy(null);
		} else if((action==SpiritChangeType.MODEL_UPDATED || action==SpiritChangeType.MODEL_ADDED) && what==Study.class) {
			getFrame().setStudyId(((Study)details.iterator().next()).getStudyId());
		}

		studyTable.reload();
	}

	@Override
	public void setStudy(Study study) {
		getFrame().setStudyId(study==null?"": study.getStudyId());
		searchPane.query().afterDone(() -> {
			studyTable.setSelection(study==null? null: Collections.singleton(study));
		});
	}

	@Override
	public Study getStudy() {
		return studyDetailPanel.getStudy()==null? null: studyDetailPanel.getStudy();
	}


	@Override
	public void onTabSelect() {
		onStudySelect();
		if(getRootPane()!=null){
			getRootPane().setDefaultButton(searchPane.getSearchButton());
			if(!initialized) {
				searchPane.reset();
				initialized = true;
			}
		}
	}

	@Override
	public void onStudySelect() {
		String studyIds = getFrame().getStudyId();
		if(studyIds==null || studyIds.length()==0) return;

		this.initialized = true;

		//Execute this thread after the others
		new SwingWorkerExtended("Loading Studies", studyTable, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
			List<Study> studies;
			@Override
			protected void doInBackground() throws Exception {
				StudyQuery q = new StudyQuery();
				q.setStudyIds(studyIds);
				studies = DAOStudy.queryStudies(q, SpiritFrame.getUser());
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


}
