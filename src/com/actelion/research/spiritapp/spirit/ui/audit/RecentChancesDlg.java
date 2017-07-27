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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.admin.BioTypeDocumentPane;
import com.actelion.research.spiritapp.spirit.ui.admin.TestDocumentPane;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.biosample.column.LastChangeColumn;
import com.actelion.research.spiritapp.spirit.ui.location.LocationTable;
import com.actelion.research.spiritapp.spirit.ui.result.ResultActions;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTable;
import com.actelion.research.spiritapp.spirit.ui.study.StudyDetailPanel;
import com.actelion.research.spiritapp.spirit.ui.study.StudyTable;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.lf.StudyComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.lf.UserIdComboBox;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.util.ui.DateTextField;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class RecentChancesDlg extends JEscapeDialog {


	private final RevisionTable revisionList = new RevisionTable(true);

	private UserIdComboBox userTextField = new UserIdComboBox();
	private DateTextField fromTextField = new DateTextField(false);
	private DateTextField toTextField = new DateTextField(true);
	private JPanel contentPanel = new JPanel(new GridLayout());
	private StudyComboBox studyComboBox = new StudyComboBox();

	private JCheckBox studyCheckBox = new JCheckBox("Studies  ", true);
	private JCheckBox sampleCheckBox = new JCheckBox("Biosamples  ", true);
	private JCheckBox resultCheckBox = new JCheckBox("Results  ", true);
	private JCheckBox locationCheckBox = new JCheckBox("Locations  ", true);
	private JCheckBox adminCheckBox = new JCheckBox("Biotypes / Tests", true);
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

		//RevisionPanel
		JButton filterButton = new JIconButton(IconType.SEARCH, "Query");
		filterButton.addActionListener(e-> {
			loadRevisions();
		});

		JPanel revisionQueryPanel = UIUtils.createTitleBox("Filters",
				UIUtils.createVerticalBox(
						UIUtils.createTable(4,
								new JLabel("StudyId: "), studyComboBox, new JLabel(" From: "), fromTextField,
								new JLabel("UserId: "), userTextField,  new JLabel(" To: "), toTextField),
						Box.createVerticalStrut(5), null,
						UIUtils.createTable(3,
								studyCheckBox, sampleCheckBox, resultCheckBox,
								locationCheckBox, adminCheckBox, null),
						UIUtils.createHorizontalBox(skipSmallChanges, Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(Box.createHorizontalGlue(), filterButton)));
		getRootPane().setDefaultButton(filterButton);

		JPanel revisionPanel = UIUtils.createBox(new JScrollPane(revisionList), revisionQueryPanel);
		revisionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		revisionList.getSelectionModel().addListSelectionListener(e-> {
			if(e.getValueIsAdjusting()) return;
			refreshSelection();
		});
		revisionList.addMouseListener(new PopupAdapter() {
			@Override
			protected void showPopup(MouseEvent e) {
				List<Revision> sel = revisionList.getSelection();
				if(sel.size()!=1) return;
				JPopupMenu popupMenu = new JPopupMenu();
				popupMenu.add(new Action_Revert(sel.get(0)));
				popupMenu.show(revisionList, e.getX(), e.getY());

			}
		});

		//ContentPanel
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, revisionPanel, contentPanel);
		splitPane.setDividerLocation(400);
		setContentPane(splitPane);

		UIUtils.adaptSize(this, 1500, 850);
		setVisible(true);
	}


	private void loadRevisions() {
		revisionList.clearSelection();
		try {
			final String userId = userTextField.getText();
			final String studyId = studyComboBox.getText();
			final Date fromDate = fromTextField.getTextDate();
			final Date toDate = toTextField.getTextDate();
			if((userId==null || userId.length()==0) && (studyId==null || studyId.length()==0) && fromDate==null && toDate==null) {
				throw new Exception("Please enter some criteria");
			}

			new SwingWorkerExtended("Loading recent revisions", contentPanel) {

				List<Revision> revisions;
				@Override
				protected void doInBackground() throws Exception {
					revisions = new ArrayList<>();
					for(Revision r: DAORevision.getRevisions(userId, studyId, fromDate, toDate, studyCheckBox.isSelected(), sampleCheckBox.isSelected(), resultCheckBox.isSelected(), locationCheckBox.isSelected(), adminCheckBox.isSelected())) {
						if(!skipSmallChanges.isSelected()) {
							revisions.add(r);
						} else {
							if(r.getBiosamples().size()>1 || r.getResults().size()>1 || r.getLocations().size()>0 || r.getStudies().size()>0 || r.getBiotypes().size()>0 || r.getTests().size()>0) {
								revisions.add(r);
							}
						}
					}
				}

				@Override
				protected void done() {
					revisionList.setRows(revisions);
				}
			};
		} catch (Exception e) {
			JExceptionDialog.showError(this, e);
		}

	}

	private void refreshSelection() {
		new SwingWorkerExtended("Loading Revision", contentPanel, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
			private JTabbedPane detailPanel = new JCustomTabbedPane();
			@Override
			protected void doInBackground() throws Exception {
				List<Revision> sel = revisionList.getSelection();

				if(sel.size()!=1) return;

				Revision rev = sel.get(0);
				rev = DAORevision.getRevision(rev.getRevId());
				if(rev.getTests().size()>0) {
					Box panel = Box.createVerticalBox();
					for (Test t : rev.getTests()) {
						TestDocumentPane doc = new TestDocumentPane();
						panel.add(doc);

						doc.setSelection(t);
					}
					detailPanel.addTab(rev.getTests().size()+ " Tests", new JScrollPane(panel));
				}
				if(rev.getBiotypes().size()>0) {
					Box panel = Box.createVerticalBox();
					for (Biotype t : rev.getBiotypes()) {
						BioTypeDocumentPane doc = new BioTypeDocumentPane();
						panel.add(doc);

						doc.setSelection(t);
					}
					detailPanel.addTab(rev.getBiotypes().size()+ " Biotypes", new JScrollPane(panel));
				}

				if(rev.getStudies().size()>0) {


					if(rev.getStudies().size()==1) {
						rev.getStudies().get(0).getAttachedBiosamples();
						final StudyDetailPanel detail = new StudyDetailPanel(JSplitPane.VERTICAL_SPLIT);
						detail.setForRevision(true);
						detailPanel.addTab(rev.getStudies().get(0).getStudyId(), detail);
						detail.setStudy(rev.getStudies().get(0));
					} else {
						final StudyTable table = new StudyTable();
						detailPanel.addTab(rev.getStudies().size()+ " Studies", new JScrollPane(table));
						Collections.sort(rev.getStudies());
						table.setRows(rev.getStudies());
					}
				}
				if(rev.getBiosamples().size()>0) {
					final BiosampleTable table = new BiosampleTable();
					table.getModel().setCanExpand(false);
					table.getModel().showHideable(new LastChangeColumn(), true);
					LastChangeColumn.setRevId(rev.getRevId());

					//					final RecentChangesBiosamplePanel detail = new RecentChangesBiosamplePanel();
					//					BiosampleActions.attachRevisionPopup(table);
					//					detail.setRevision(rev);
					//					JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(detail), );
					//					panel.setDividerLocation(300);

					detailPanel.addTab(rev.getBiosamples().size()+ " Biosample", new JScrollPane(table));
					Collections.sort(rev.getBiosamples());
					table.setRows(rev.getBiosamples());

				}
				if(rev.getLocations().size()>0) {
					Box panel = Box.createVerticalBox();
					LocationTable table = new LocationTable();
					panel.add(new JScrollPane(table));
					table.setRows(rev.getLocations());
					detailPanel.addTab(rev.getLocations().size()+ " Locations", new JScrollPane(panel));
				}
				if(rev.getResults().size()>0) {
					ResultTable table = new ResultTable();
					ResultActions.attachRevisionPopup(table);
					detailPanel.addTab(rev.getResults().size()+ " Results", new JScrollPane(table));

					Collections.sort(rev.getResults());
					table.setRows(rev.getResults());
				}
			}

			@Override
			protected void done() {
				contentPanel.removeAll();
				contentPanel.add(detailPanel);
				contentPanel.validate();
				contentPanel.repaint();
			}
		};

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
