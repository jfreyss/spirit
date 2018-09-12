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

package com.actelion.research.spiritapp.ui.study;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.ui.pivot.graph.GraphPanel;
import com.actelion.research.spiritapp.ui.study.depictor.StudyDepictor;
import com.actelion.research.spiritapp.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;

/**
 * Panel with the studydepictor and the animals
 * Slow component that could be called in a Swingworker
 *
 * @author freyssj
 *
 */
public class StudyDetailPanel extends JPanel {

	private Study study;

	/**
	 * If the tab is forRevision, the study is never refreshed
	 */
	private boolean forRevision;
	private final JTabbedPane infoTabbedPane = new JCustomTabbedPane();

	private final StudyDepictor studyDepictor = new StudyDepictor();
	private final StudyEditorPane editorPane = new StudyEditorPane();
	private final StudyQuickLinksPane quickLinkPanel = new StudyQuickLinksPane();
	private final BiosampleTable participantTable = new BiosampleTable();
	private final GraphPanel graphPanel = new GraphPanel();
	private final JSplitPane studySplitPane;

	//	private CardLayout cardLayout = new CardLayout();
	//	private JPanel infoCardTabbedPane = new JPanel(cardLayout);
	//	private JLabel infoLabel = new JLabel();

	/**
	 * Creates a StudyDetailPanel with an orientation:
	 * - JSplitPane.HORIZONTAL_SPLIT
	 * - JSplitPane.VERTICAL_SPLIT
	 * @param orientation
	 */
	public StudyDetailPanel(final int orientation) {

		//init depictor
		studyDepictor.addPropertyChangeListener(evt -> {
			StudyDetailPanel.this.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
		});

		final BiosampleTabbedPane biosamplePane = new BiosampleTabbedPane();
		biosamplePane.setSelectedTab(BiosampleTabbedPane.HISTORY_TITLE);
		ListSelectionListener l = e-> {
			if(e.getValueIsAdjusting()) return;
			biosamplePane.setBiosamples(participantTable.getSelection().size()==1? participantTable.getSelection(): null);
		};
		participantTable.getSelectionModel().addListSelectionListener(l);
		participantTable.getColumnModel().getSelectionModel().addListSelectionListener(l);
		//		participantTable.getModel().setMode(Mode.COMPACT);

		JSplitPane quickViewSplitPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.VERTICAL_SPLIT, new JScrollPane(quickLinkPanel), graphPanel);

		infoTabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
		infoTabbedPane.add("Infos", new JScrollPane(editorPane));
		infoTabbedPane.add("Participants", new JScrollPane(participantTable));
		if(SpiritProperties.getInstance().isChecked(PropertyKey.SYSTEM_RESULT)) {
			infoTabbedPane.add("Data", quickViewSplitPane);
		}

		studySplitPane = new JSplitPaneWithZeroSizeDivider(orientation, new JScrollPane(studyDepictor), infoTabbedPane);
		SwingUtilities.invokeLater(() -> {
			if(orientation==JSplitPane.HORIZONTAL_SPLIT) {
				studySplitPane.setDividerLocation(.7);
				quickViewSplitPane.setDividerLocation(200);
			} else {
				studySplitPane.setDividerLocation(400);
			}
		});

		setLayout(new BorderLayout());
		if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_STUDYDESIGN)) {
			add(BorderLayout.CENTER, studySplitPane);
		} else {
			add(BorderLayout.CENTER, infoTabbedPane);
		}


		setOpaque(true);
		setPreferredSize(new Dimension(orientation==JSplitPane.HORIZONTAL_SPLIT? 800: 320, orientation==JSplitPane.HORIZONTAL_SPLIT? 350: 700));

		infoTabbedPane.addChangeListener(e-> {
			refreshTabbedPane();
		});

		graphPanel.setListSelectionListener(e-> {
			if(study==null) return;
			SpiritContextListener.query(ResultQuery.createQueryForStudyIds(study.getStudyId()), graphPanel.getSelectedIndex());
		});

		if(forRevision) {
			BiosampleActions.attachRevisionPopup(participantTable);
		} else {
			StudyActions.attachPopup(studyDepictor);
			StudyActions.attachPopup(editorPane);
			BiosampleActions.attachPopup(participantTable);
		}

	}

	public void setForRevision(boolean forRevision) {
		this.forRevision = forRevision;
		studyDepictor.setForRevision(forRevision);
		if(infoTabbedPane.getTabCount()>2) infoTabbedPane.setEnabledAt(2, !forRevision);
	}

	public StudyDepictor getStudyDepictor() {
		return studyDepictor;
	}

	public StudyEditorPane getStudyPane() {
		return editorPane;
	}

	public void setStudy(final Study study) {
		if(this.study==study) return;
		this.study = study;
		refresh();

	}
	/**
	 * Refresh the tabbedPane in a worker.
	 */
	private void refresh() {
		studyDepictor.setStudy(null);
		editorPane.setStudy(null);
		graphPanel.setResults(new ArrayList<>());
		if(SpiritRights.canRead(study, SpiritFrame.getUser())) {
			studyDepictor.setStudy(null);
			editorPane.setStudy(null);
			graphPanel.setResults(new ArrayList<>());
			new SwingWorkerExtended("Loading Study", this, forRevision? SwingWorkerExtended.FLAG_SYNCHRONOUS: SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
				@Override
				protected void doInBackground() throws Exception {

				}
				@Override
				protected void done() {
					studyDepictor.setStudy(forRevision? study: JPAUtil.reattach(study));
				}
			}.afterDone(() -> {
				refreshTabbedPane();
			});
		}
	}

	private void refreshTabbedPane() {
		if(study==null) return;
		if(infoTabbedPane.getSelectedIndex()==0) {
			new SwingWorkerExtended("Loading Details", editorPane, forRevision? SwingWorkerExtended.FLAG_SYNCHRONOUS: SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
				@Override
				protected void done() {
					editorPane.setStudy(forRevision? study: JPAUtil.reattach(study));
				}
			};
		} else if(infoTabbedPane.getSelectedIndex()==1) {
			new SwingWorkerExtended("Loading Participants", participantTable, forRevision? SwingWorkerExtended.FLAG_SYNCHRONOUS: SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
				private List<Biosample> attached = null;
				@Override
				protected void doInBackground() throws Exception {
					attached = study==null?new ArrayList<Biosample>(): new ArrayList<>(JPAUtil.reattach(study).getParticipants());
					Collections.sort(attached);
				}

				@Override
				protected void done() {
					participantTable.setRows(attached);
				}
			};
		} else if(infoTabbedPane.getSelectedIndex()==2) {
			new SwingWorkerExtended("Loading Infos", quickLinkPanel, forRevision? SwingWorkerExtended.FLAG_SYNCHRONOUS: SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
				@Override
				protected void done() {
					quickLinkPanel.setStudy(study);
				}
			}.afterDone(() -> {
				new SwingWorkerExtended("Loading Results", graphPanel, forRevision? SwingWorkerExtended.FLAG_SYNCHRONOUS: SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
					private List<Result> results = null;
					@Override
					protected void doInBackground() throws Exception {
						if(study==null) return;
						ResultQuery q = ResultQuery.createQueryForSids(Collections.singleton(study.getId()));
						q.setMaxResults(4000);
						results = DAOResult.queryResults(q, SpiritFrame.getUser());
					}

					@Override
					protected void done() {
						if(results!=null && results.size()>=4000) {
							graphPanel.setErrorText("There are too many results to be displayed (>4000)");
						} else {
							graphPanel.setResults(results);
						}
					};
				};
			});
		}
	}

	public void showInfos() {
		infoTabbedPane.setSelectedIndex(0);
	}

	public void showParticipants() {
		infoTabbedPane.setSelectedIndex(1);
	}


	public Study getStudy() {
		return study;
	}

}
