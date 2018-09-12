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

package com.actelion.research.spiritapp.ui.audit;

import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.ui.study.StudyActions.Action_ExportStudyEvents;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.audit.RevisionQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 * Dialog to show the audit history of a study.
 *
 * @author Joel Freyss
 *
 */
public class StudyHistoryDlg extends JEscapeDialog {


	private Study study;
	private RevisionPanel revisionPanel = new RevisionPanel();
	//	private JCheckBox studyCheckbox = new JCheckBox("Study", true);
	//	private JCheckBox samplesCheckbox = new JCheckBox("Samples", true);
	//	private JCheckBox locationsCheckbox = new JCheckBox("Locations", true);
	//	private JCheckBox resultsCheckbox = new JCheckBox("Results");
	private JCheckBox byFieldCheckbox = new JCheckBox("Changes per field", true);

	private Action_ExportStudyEvents exportStudyEventsAction = new Action_ExportStudyEvents();

	public StudyHistoryDlg(Study study) {
		super(UIUtils.getMainFrame(), "Study - Audit Trail - " + study.getStudyId());
		this.study = study;


		//		studyCheckbox.addActionListener(e->refreshInThread());
		//		samplesCheckbox.addActionListener(e->refreshInThread());
		//		locationsCheckbox.addActionListener(e->refreshInThread());
		//		resultsCheckbox.addActionListener(e->refreshInThread());
		byFieldCheckbox.addActionListener(e->revisionPanel.setSingular(byFieldCheckbox.isSelected()));
		//		if(!SpiritProperties.getInstance().isChecked(PropertyKey.SYSTEM_RESULT)) {
		//			resultsCheckbox.setVisible(false);
		//		}

		exportStudyEventsAction.setParentDlg(this);
		exportStudyEventsAction.setFilters(null, null, study.getId());
		JIconButton exportStudyEventsButton = new JIconButton(IconType.PDF, "Export Study Events...", exportStudyEventsAction);
		JPanel actionPanel = UIUtils.createHorizontalBox(Box.createHorizontalGlue(), exportStudyEventsButton);

		JPanel topPanel = UIUtils.createTitleBox(UIUtils.createHorizontalBox(Box.createHorizontalGlue(), byFieldCheckbox));
		topPanel.setVisible(SpiritProperties.getInstance().isAdvancedMode());
		setContentPane(UIUtils.createBox(revisionPanel, topPanel, actionPanel));

		//Load revisions in background
		refreshInThread();

		//show dialog
		UIUtils.adaptSize(this, 1200, 800);
		setVisible(true);
	}

	private void refreshInThread() {
		revisionPanel.clear();
		revisionPanel.setSingular(byFieldCheckbox.isSelected());
		revisionPanel.setFilters(null, null, study.getId());
		new SwingWorkerExtended(getContentPane(), SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			private List<Revision> revisions;
			@Override
			protected void doInBackground() throws Exception {

				RevisionQuery query = new RevisionQuery();
				query.setSidFilter(study.getId());
				query.setStudies(true);
				query.setSamples(true);
				query.setLocations(true);
				query.setResults(false);
				query.setAdmin(false);

				revisions = DAORevision.queryRevisions(query);
			}

			@Override
			protected void done() {
				revisionPanel.setRows(revisions);
				exportStudyEventsAction.setRevisions(revisions);
				exportStudyEventsAction.setStudy(study);
			}
		};
	}

}
