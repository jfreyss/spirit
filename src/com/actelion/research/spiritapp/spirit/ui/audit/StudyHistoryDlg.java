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

package com.actelion.research.spiritapp.spirit.ui.audit;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.spirit.ui.admin.AdminActions;
import com.actelion.research.spiritapp.spirit.ui.study.StudyDetailPanel;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

/**
 * Dialog to show the audit history of a study.
 *
 * @author Joel Freyss
 *
 */
public class StudyHistoryDlg extends JEscapeDialog {

	final RevisionTable revisionList = new RevisionTable(false);

	public StudyHistoryDlg(Study study) {
		super(UIUtils.getMainFrame(), "Study - History");

		//dialog events
		final StudyDetailPanel detailPanel = new StudyDetailPanel(JSplitPane.VERTICAL_SPLIT);
		detailPanel.setForRevision(true);

		revisionList.getSelectionModel().addListSelectionListener(e-> {
			if(e.getValueIsAdjusting()) return;
			List<Revision> s = revisionList.getSelection();
			detailPanel.setStudy(s.size()==1 && s.get(0).getStudies().size()>0? s.get(0).getStudies().get(0): null);
		});
		revisionList.addMouseListener(new PopupAdapter() {
			@Override
			protected void showPopup(MouseEvent e) {
				List<Revision> s = revisionList.getSelection();
				if(s.size()==1 && s.get(0).getStudies().size()==1) {
					JPopupMenu menu = new JPopupMenu();
					menu.add(new AdminActions.Action_Restore(s.get(0).getStudies()));
					menu.show(revisionList, e.getX(), e.getY());
				}
			}
		});

		//Create layout
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				UIUtils.createTitleBox("Revisions", new JScrollPane(revisionList)),
				UIUtils.createTitleBox("Study Revision", detailPanel));
		splitPane.setDividerLocation(400);
		setContentPane(splitPane);

		//Load revisions in background
		new SwingWorkerExtended(revisionList, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			private List<Revision> revisions;
			private Map<Revision, String> changeMap;
			@Override
			protected void doInBackground() throws Exception {
				revisions = DAORevision.getRevisions(study);
				changeMap = getChangeMap(revisions);
			}

			@Override
			protected void done() {
				if(revisions.size()==0) {
					JExceptionDialog.showError("There are no revisions saved");
				}
				revisionList.setRows(revisions, changeMap);
			}
		};



		//show dialog
		UIUtils.adaptSize(this, 1124, 800);
		setVisible(true);

	}


	private Map<Revision, String> getChangeMap(List<Revision> revisions ) {
		Map<Revision, String> changeMap = new HashMap<>();
		List<Revision> revs = new ArrayList<>();
		for (int i = 0; i < revisions.size(); i++) {
			Study b1 = revisions.get(i).getStudies().get(0);
			String diff;
			if(i+1<revisions.size()) {
				Study b2 = revisions.get(i+1).getStudies().get(0);
				diff = b1.getDifference(b2);
			} else {
				Study b2 = revisions.get(0).getStudies().get(0);
				diff = b1.getDifference(b2);
				if(diff.length()==0) diff = "First version";
			}

			revs.add(revisions.get(i));
			changeMap.put(revisions.get(i), diff);
		}
		return changeMap;

	}

}
