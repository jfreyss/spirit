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

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.lf.StudyComboBox;
import com.actelion.research.spiritapp.ui.util.lf.UserIdComboBox;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.audit.RevisionQuery;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class RecentChancesDlg extends JEscapeDialog {


	private final RevisionTable revisionTable = new RevisionTable(true);

	private UserIdComboBox userTextField = new UserIdComboBox();
	private JCustomTextField fromTextField = new JCustomTextField(CustomFieldType.DATE);
	private JCustomTextField toTextField = new JCustomTextField(CustomFieldType.DATE);
	private RevisionDetailPanel revisionDetailPanel = new RevisionDetailPanel();
	private StudyComboBox studyComboBox = new StudyComboBox();

	private JRadioButton userChanges = new JRadioButton("User changes", true);
	private JRadioButton adminChanges = new JRadioButton("Admin changes");
	private JCheckBox studyCheckBox = new JCheckBox("Studies  ", true);
	private JCheckBox sampleCheckBox = new JCheckBox("Biosamples  ", true);
	private JCheckBox resultCheckBox = new JCheckBox("Results  ", true);
	private JCheckBox locationCheckBox = new JCheckBox("Locations  ", true);

	private JCheckBox skipSmallChanges = new JCheckBox("Show only big changes", false);

	public RecentChancesDlg(String userId) {
		super(UIUtils.getMainFrame(), "Recent Changes");
		if(userId==null) {
			userTextField.setText("");
			userTextField.setEnabled(true);
		} else {
			userTextField.setText(userId==""?"NA": userId);
			userTextField.setEnabled(false);

		}
		userTextField.setTextWhenEmpty("UserId");

		fromTextField.setTextDate(new Date());

		//RevisionPanel
		JButton filterButton = new JIconButton(IconType.SEARCH, "Query");
		filterButton.addActionListener(e-> loadRevisions());
		userTextField.addActionListener(e->loadRevisions());
		fromTextField.addActionListener(e->loadRevisions());
		toTextField.addActionListener(e->loadRevisions());
		studyComboBox.addActionListener(e->loadRevisions());

		ButtonGroup gr = new ButtonGroup();
		gr.add(adminChanges);
		gr.add(userChanges);


		JPanel revisionQueryPanel = UIUtils.createTitleBox("Filters",
				UIUtils.createVerticalBox(
						UIUtils.createTable(4, new JLabel("From: "), fromTextField, new JLabel("To: "), toTextField),
						new JSeparator(JSeparator.HORIZONTAL),
						UIUtils.createHorizontalBox(
								UIUtils.createVerticalBox(
										UIUtils.createHorizontalBox(userChanges, Box.createHorizontalGlue()),
										UIUtils.createTable(4, new JLabel("StudyId: "), studyComboBox, new JLabel("UserId: "), userTextField),
										Box.createVerticalStrut(5), null,
										UIUtils.createHorizontalBox(studyCheckBox, sampleCheckBox, resultCheckBox, locationCheckBox, Box.createHorizontalGlue()),
										UIUtils.createHorizontalBox(skipSmallChanges, Box.createHorizontalGlue()),
										Box.createVerticalGlue()),
								new JSeparator(JSeparator.VERTICAL),
								UIUtils.createVerticalBox(
										UIUtils.createHorizontalBox(adminChanges),
										Box.createVerticalGlue()),
								Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(Box.createHorizontalGlue(), filterButton)));
		getRootPane().setDefaultButton(filterButton);

		JPanel revisionPanel = UIUtils.createBox(new JScrollPane(revisionTable), revisionQueryPanel);
		revisionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		revisionTable.getSelectionModel().addListSelectionListener(e-> {
			if(e.getValueIsAdjusting()) return;
			refreshSelection();
		});
		revisionTable.addMouseListener(new PopupAdapter() {
			@Override
			protected void showPopup(MouseEvent e) {
				List<Revision> sel = revisionTable.getSelection();
				if(sel.size()!=1) return;
				JPopupMenu popupMenu = new JPopupMenu();
				popupMenu.add(new Action_Revert(sel.get(0)));
				popupMenu.show(revisionTable, e.getX(), e.getY());

			}
		});


		//ContentPane
		JSplitPane splitPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, revisionPanel, revisionDetailPanel);
		splitPane.setDividerLocation(700);
		setContentPane(splitPane);
		UIUtils.adaptSize(this, 1500, 850);
		setVisible(true);
	}


	private void loadRevisions() {
		revisionTable.clearSelection();
		try {
			final String userId = userTextField.getText();
			final String studyId = studyComboBox.getText();
			final Date fromDate = fromTextField.getTextDate();
			final Date toDate = toTextField.getTextDate();
			if((userId==null || userId.length()==0) && (studyId==null || studyId.length()==0) && fromDate==null && toDate==null) {
				throw new Exception("Please enter some criteria");
			}

			final RevisionQuery query = new RevisionQuery();
			query.setStudyIdFilter(studyId);
			query.setUserIdFilter(userId);
			query.setFromDate(fromDate);
			query.setToDate(toDate);
			query.setStudies(userChanges.isSelected() && studyCheckBox.isSelected());
			query.setSamples(userChanges.isSelected() && sampleCheckBox.isSelected());
			query.setLocations(userChanges.isSelected() && locationCheckBox.isSelected());
			query.setResults(userChanges.isSelected() && resultCheckBox.isSelected());
			query.setAdmin(adminChanges.isSelected());


			new SwingWorkerExtended("Loading revisions", revisionTable) {
				List<Revision> revisions;
				Map<Revision, String> changeMap;
				@Override
				protected void doInBackground() throws Exception {
					revisions = new ArrayList<>();

					for(Revision r: DAORevision.queryRevisions(query)) {
						if(!skipSmallChanges.isSelected()) {
							revisions.add(r);
						} else {
							revisions.add(r);
						}
					}
					changeMap = DAORevision.getLastChanges(revisions);
				}
				@Override
				protected void done() {
					revisionTable.setRows(revisions, changeMap);
				}
			};
		} catch (Exception e) {
			JExceptionDialog.showError(this, e);
		}

	}

	private void refreshSelection() {
		revisionDetailPanel.setRevision(revisionTable.getSelection().size()!=1? null: revisionTable.getSelection().get(0));
	}

	public class Action_Revert extends AbstractAction {
		private Revision revision;

		public Action_Revert(Revision revision) {
			super("Cancel this change");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('v'));
			setEnabled(SpiritRights.isSuperAdmin(SpiritFrame.getUser()) || (revision.getUser()!=null && revision.getUser().equals(SpiritFrame.getUsername())));
			this.revision = revision;

		}
		@Override
		public void actionPerformed(ActionEvent e) {
			final String reason = JOptionPane.showInputDialog(getContentPane(), "Do you want to restore the entities to the previous state?\nPlease give a reason!", "Reason", JOptionPane.QUESTION_MESSAGE);
			if(reason!=null) {
				new SwingWorkerExtended("Revert", UIUtils.getMainFrame()) {
					@Override
					protected void doInBackground() throws Exception {
						DAORevision.revert(revision, SpiritFrame.getUser(), "Revert" + (reason.length()==0?"":"(" + reason + ")"));
					}
					@Override
					protected void done() {
						JExceptionDialog.showInfo(UIUtils.getMainFrame(), "The changes have been successfully reverted");

						//full refresh
						SpiritChangeListener.fireModelChanged(SpiritChangeType.LOGIN);
					}
				};
			} else {
				JOptionPane.showMessageDialog(UIUtils.getMainFrame(), "Revert canceled");
			}
		}
	}

}
