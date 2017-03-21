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

package com.actelion.research.spiritapp.spirit.ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.hibernate.envers.RevisionType;

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.location.LocationTable;
import com.actelion.research.spiritapp.spirit.ui.result.ResultActions;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTable;
import com.actelion.research.spiritapp.spirit.ui.study.StudyDetailPanel;
import com.actelion.research.spiritapp.spirit.ui.study.StudyTable;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.DateTextField;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class RecentChancesDlg extends JEscapeDialog {


	private final DefaultListModel<Revision> revisionModel = new DefaultListModel<Revision>();
	private final JList<Revision> revisionList = new JList<Revision>(revisionModel);

	private SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
	private JCustomTextField userTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 6);
	private DateTextField dateTextField = new DateTextField(true);
	private JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
	private JPanel contentPanel = new JPanel(new GridLayout());

	private JCheckBox studyCheckBox = new JCheckBox("Studies  ", true);
	private JCheckBox sampleCheckBox = new JCheckBox("Biosamples  ", true);
	private JCheckBox resultCheckBox = new JCheckBox("Results  ", true);
	private JCheckBox locationCheckBox = new JCheckBox("Locations  ", true);
	private JCheckBox adminCheckBox = new JCheckBox("Biotypes / Tests", true);
	private JCheckBox skipSmallChanges = new JCheckBox("Show only big changes", false);

	private Date now = JPAUtil.getCurrentDateFromDatabase();

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
		filterButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadRevisions();
			}
		});
		dateTextField.setText(df.format(now));
		daySpinner.setPreferredSize(new Dimension(50, 24));
		JPanel revisionQueryPanel = UIUtils.createTitleBox("Filters",
				UIUtils.createTable(
						new JLabel("UserId: "), UIUtils.createHorizontalBox(userTextField),
						Box.createVerticalStrut(5), null,
						new JLabel("From: "), UIUtils.createHorizontalBox(dateTextField, new JLabel(" and up to: "), daySpinner, new JLabel("days before")),
						Box.createVerticalStrut(5), null,
						null, UIUtils.createVerticalBox(
								UIUtils.createHorizontalBox(studyCheckBox, sampleCheckBox, resultCheckBox, Box.createHorizontalGlue()),
								UIUtils.createHorizontalBox(locationCheckBox, adminCheckBox, Box.createHorizontalGlue()),
								UIUtils.createHorizontalBox(skipSmallChanges, Box.createHorizontalGlue())),
						null, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), filterButton)));
		getRootPane().setDefaultButton(filterButton);

		JPanel revisionPanel = new JPanel(new BorderLayout());
		revisionPanel.add(BorderLayout.NORTH, revisionQueryPanel);
		revisionPanel.add(BorderLayout.CENTER, new JScrollPane(revisionList));
		revisionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		revisionList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				refreshSelection();
			}
		});
		revisionList.addMouseListener(new PopupAdapter() {
			@Override
			protected void showPopup(MouseEvent e) {
				Revision rev = revisionList.getSelectedValue();
				if(rev==null) return;
				JPopupMenu popupMenu = new JPopupMenu();
				popupMenu.add(new AdminActions.Action_Revert(rev));
				popupMenu.show(revisionList, e.getX(), e.getY());

			}
		});
		revisionList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				Revision rev = (Revision) value;
				if(!isSelected) {
					if(rev.getRevisionType()==RevisionType.ADD) {
						setForeground(new Color(0, 80, 0));
					} else if(rev.getRevisionType()==RevisionType.DEL) {
						setForeground(new Color(170, 0, 0));
					} else {
						setForeground(new Color(150, 100, 0));
					}
				}

				if(rev.getStudies().size()>0) {
					setIcon(IconType.STUDY.getIcon());
				} else if(rev.getBiosamples().size()>0) {
					setIcon(IconType.BIOSAMPLE.getIcon());
				} else if(rev.getResults().size()>0) {
					setIcon(IconType.RESULT.getIcon());
				} else if(rev.getLocations().size()>0) {
					setIcon(IconType.LOCATION.getIcon());
				} else {
					setIcon(IconType.ADMIN.getIcon());
				}
				return this;
			}
		});


		//ContentPanel
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, revisionPanel, contentPanel);
		splitPane.setDividerLocation(400);
		setContentPane(splitPane);

		loadRevisions();

		setSize(1500, 850);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}


	private void loadRevisions() {
		revisionList.setSelectedIndex(-1);
		final String user = userTextField.getText().toLowerCase();
		Date d;
		try {
			d = df.parse(dateTextField.getText());
		} catch (Exception e) {
			d = now;
		}
		final Date date = d;

		new SwingWorkerExtended("Loading recent revisions", contentPanel) {

			List<Revision> revisions;

			@Override
			protected void doInBackground() throws Exception {
				revisions = new ArrayList<>();
				for(Revision r: DAORevision.getRevisions(user, date, (Integer) daySpinner.getValue(), studyCheckBox.isSelected(), sampleCheckBox.isSelected(), resultCheckBox.isSelected(), locationCheckBox.isSelected(), adminCheckBox.isSelected())) {
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
				revisionModel.clear();
				if(revisions!=null) {
					for (Revision r : revisions) {
						revisionModel.addElement(r);
					}
				}
				revisionList.repaint();
			}
		};

	}

	private void refreshSelection() {
		new SwingWorkerExtended("Loading Revision", contentPanel, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
			private Revision rev;
			private JTabbedPane detailPanel = new JCustomTabbedPane();
			@Override
			protected void doInBackground() throws Exception {
				rev = revisionList.getSelectedValue();

				if(rev==null) return;


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
					final BiosampleTabbedPane detail = new BiosampleTabbedPane(true);
					BiosampleActions.attachRevisionPopup(table);
					table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
						@Override
						public void valueChanged(ListSelectionEvent e) {
							detail.setBiosamples(table.getSelection());
						}
					});

					JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(table), detail);
					panel.setDividerLocation(420);

					detailPanel.addTab(rev.getBiosamples().size()+ " Biosample", panel);
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

}
