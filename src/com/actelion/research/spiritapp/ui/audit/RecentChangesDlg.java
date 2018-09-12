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

import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.apache.commons.lang3.time.DateUtils;

import com.actelion.research.spiritapp.ui.admin.AdminActions.Action_ExportChangeEvents;
import com.actelion.research.spiritapp.ui.util.DatePicker;
import com.actelion.research.spiritapp.ui.util.component.StudyComboBox;
import com.actelion.research.spiritapp.ui.util.component.UserIdComboBox;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.audit.RevisionQuery;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class RecentChangesDlg extends JEscapeDialog {

	private final RevisionPanel revisionPanel = new RevisionPanel();

	private UserIdComboBox userTextField = new UserIdComboBox();
	private DatePicker fromTextField = new DatePicker();
	private DatePicker toTextField = new DatePicker();
	private StudyComboBox studyComboBox = new StudyComboBox();

	private Action_ExportChangeEvents exportChangeEventsAction = new Action_ExportChangeEvents();

	//	private JCheckBox userChanges = new JCheckBox("Entity changes", true);
	private JCheckBox adminChanges = new JCheckBox("Administrative changes", true);
	private JCheckBox studyCheckBox = new JCheckBox("Studies  ", true);
	private JCheckBox sampleCheckBox = new JCheckBox("Biosamples  ", true);
	private JCheckBox resultCheckBox = new JCheckBox("Results  ", true);
	private JCheckBox locationCheckBox = new JCheckBox("Locations  ", true);

	private JCheckBox byFieldCheckbox = new JCheckBox("Changes per field", true);

	public RecentChangesDlg(String userId) {
		super(UIUtils.getMainFrame(), "Recent Changes");
		if(userId==null) {
			userTextField.setText("");
			userTextField.setEnabled(true);
		} else {
			userTextField.setText(userId.length()==0?"NA": userId);
			userTextField.setEnabled(false);
		}
		userTextField.setTextWhenEmpty("UserId");

		fromTextField.setText(FormatterUtils.formatDate(DateUtils.addDays(new Date(), -3)));
		toTextField.setText(FormatterUtils.formatDate(DateUtils.addDays(new Date(), 1)));

		//RevisionPanel
		JButton filterButton = new JIconButton(IconType.SEARCH, "Search");
		filterButton.addActionListener(e-> loadRevisions());
		userTextField.addActionListener(e->loadRevisions());
		//fromTextField.addActionListener(e->loadRevisions());
		//toTextField.addActionListener(e->loadRevisions());
		studyComboBox.addActionListener(e->loadRevisions());
		byFieldCheckbox.addActionListener(e->revisionPanel.setSingular(byFieldCheckbox.isSelected()));
		byFieldCheckbox.setVisible(SpiritProperties.getInstance().isAdvancedMode());

		exportChangeEventsAction.setParentDlg(this);
		JIconButton exportChangeEventsButton = new JIconButton(IconType.PDF, "Export Change Events...", exportChangeEventsAction);
		JPanel actionPanel = UIUtils.createHorizontalBox(Box.createHorizontalGlue(), exportChangeEventsButton);

		JPanel revisionQueryPanel = UIUtils.createTitleBox("Filters",
				UIUtils.createVerticalBox(
						UIUtils.createTable(4, new JLabel("From: "), fromTextField, new JLabel(" To: "), toTextField),
						UIUtils.createHorizontalBox(byFieldCheckbox, Box.createHorizontalGlue()),
						Box.createVerticalStrut(10),
						new JSeparator(JSeparator.HORIZONTAL),
						UIUtils.createHorizontalBox(studyCheckBox, sampleCheckBox, locationCheckBox, resultCheckBox, Box.createHorizontalGlue()),
						UIUtils.createBox(BorderFactory.createEmptyBorder(10, 10, 10, 0),
								UIUtils.createTable(2,
										new JLabel("StudyId: "), studyComboBox/*,
										new JLabel("UserId: "), userTextField*/)),
						new JSeparator(JSeparator.HORIZONTAL),
						UIUtils.createHorizontalBox(adminChanges, Box.createHorizontalGlue()),
						Box.createVerticalStrut(20),
						new JSeparator(JSeparator.HORIZONTAL),
						UIUtils.createHorizontalBox(Box.createHorizontalGlue(), filterButton),
						Box.createVerticalGlue()));
		getRootPane().setDefaultButton(filterButton);


		if(!SpiritProperties.getInstance().isChecked(PropertyKey.SYSTEM_RESULT)) {
			resultCheckBox.setVisible(false);
			resultCheckBox.setSelected(false);
		}

		//ContentPane
		setContentPane(UIUtils.createBox(revisionPanel, null, actionPanel, revisionQueryPanel, null));
		UIUtils.adaptSize(this, 1200, 800);
		setVisible(true);
	}


	private void loadRevisions() {
		revisionPanel.clear();
		try {
			final String userId = userTextField.getText();
			final String studyId = studyComboBox.getText();
			final Date fromDate = FormatterUtils.parseDateTime(fromTextField.getText());
			final Date toDate = FormatterUtils.parseDateTime(toTextField.getText());
			if((userId==null || userId.length()==0) && (studyId==null || studyId.length()==0) && fromDate==null && toDate==null) {
				throw new Exception("Please enter some criteria");
			}

			final RevisionQuery query = new RevisionQuery();
			query.setStudyIdFilter(studyId);
			query.setUserIdFilter(userId);
			query.setFromDate(fromDate);
			query.setToDate(toDate);
			query.setStudies(studyCheckBox.isSelected());
			query.setSamples(sampleCheckBox.isSelected());
			query.setLocations(locationCheckBox.isSelected());
			query.setResults(resultCheckBox.isSelected());
			query.setAdmin(adminChanges.isSelected());


			new SwingWorkerExtended("Loading revisions", revisionPanel) {
				List<Revision> revisions;

				@Override
				protected void doInBackground() throws Exception {
					revisions = DAORevision.queryRevisions(query);
				}
				@Override
				protected void done() {
					revisionPanel.setSingular(byFieldCheckbox.isSelected());
					revisionPanel.setRows(revisions);
					exportChangeEventsAction.setRevisions(revisions);
				}
			};
		} catch (Exception e) {
			JExceptionDialog.showError(this, e);
		}
	}

}
