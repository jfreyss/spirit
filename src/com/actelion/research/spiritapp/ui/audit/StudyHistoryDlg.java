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

package com.actelion.research.spiritapp.ui.audit;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.ui.admin.AdminActions;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.audit.RevisionQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;

/**
 * Dialog to show the audit history of a study.
 *
 * @author Joel Freyss
 *
 */
public class StudyHistoryDlg extends JEscapeDialog {


	private Study study;
	private RevisionTable revisionTable = new RevisionTable(true);
	private JCheckBox studyCheckbox = new JCheckBox("Study", true);
	private JCheckBox samplesCheckbox = new JCheckBox("Biosamples");
	private JCheckBox resultsCheckbox = new JCheckBox("Results");

	public StudyHistoryDlg(Study study) {
		super(UIUtils.getMainFrame(), "Study - History");
		this.study = study;

		//dialog events
		final RevisionDetailPanel detailPanel = new RevisionDetailPanel();

		revisionTable.getSelectionModel().addListSelectionListener(e-> {
			if(e.getValueIsAdjusting()) return;
			List<Revision> s = revisionTable.getSelection();
			detailPanel.setRevision(s.size()!=1? null: s.get(0));
		});
		revisionTable.addMouseListener(new PopupAdapter() {
			@Override
			protected void showPopup(MouseEvent e) {
				List<Revision> s = revisionTable.getSelection();
				if(s.size()==1 && s.get(0).getStudies().size()==1) {
					JPopupMenu menu = new JPopupMenu();
					menu.add(new AdminActions.Action_Restore(s.get(0).getStudies()));
					menu.show(revisionTable, e.getX(), e.getY());
				}
			}
		});

		studyCheckbox.addActionListener(e->refreshInThread());
		samplesCheckbox.addActionListener(e->refreshInThread());
		resultsCheckbox.addActionListener(e->refreshInThread());

		//Create layout
		JSplitPane splitPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT,
				UIUtils.createBox(
						UIUtils.createTitleBox("Revisions", new JScrollPane(revisionTable)),
						UIUtils.createTitleBox("Show changes related to",  UIUtils.createHorizontalBox(studyCheckbox, samplesCheckbox, resultsCheckbox, Box.createHorizontalGlue()))),
				UIUtils.createTitleBox("Study Revision", detailPanel));
		splitPane.setDividerLocation(700);
		setContentPane(splitPane);

		//Load revisions in background
		refreshInThread();

		//show dialog
		UIUtils.adaptSize(this, 1500, 800);
		setVisible(true);

	}

	private void refreshInThread() {
		revisionTable.clear();
		new SwingWorkerExtended(getContentPane(), SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			private List<Revision> revisions;
			private Map<Revision, String> changeMap;
			@Override
			protected void doInBackground() throws Exception {

				RevisionQuery query = new RevisionQuery();
				query.setSidFilter(study.getId());
				query.setStudies(studyCheckbox.isSelected());
				query.setSamples(samplesCheckbox.isSelected());
				query.setResults(resultsCheckbox.isSelected());
				revisions = DAORevision.queryRevisions(query);
				changeMap = DAORevision.getLastChanges(revisions);
			}

			@Override
			protected void done() {
				if(revisions.size()==0) {
					JExceptionDialog.showError("There are no revisions saved");
				}
				revisionTable.setRows(revisions, changeMap);
			}
		};
	}


}
