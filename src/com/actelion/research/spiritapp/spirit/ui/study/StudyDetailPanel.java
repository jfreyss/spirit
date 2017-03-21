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
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTableModel.Mode;
import com.actelion.research.spiritapp.spirit.ui.pivot.graph.GraphPanel;
import com.actelion.research.spiritapp.spirit.ui.study.depictor.StudyDepictor;
import com.actelion.research.spiritapp.spirit.ui.study.sampling.NamedSamplingEditorPane;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.spirit.ui.util.bgpane.JBGScrollPane;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

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

	private final StudyDepictor studyDepictor = new StudyDepictor();

	private final JTabbedPane infoTabbedPane = new JCustomTabbedPane();
	private final StudyEditorPane editorPane = new StudyEditorPane();
	private final BiosampleTable animalTable = new BiosampleTable();
	private final GraphPanel graphPanel = new GraphPanel();
	private final NamedSamplingEditorPane samplingsEditorPane = new NamedSamplingEditorPane();

	private JSplitPane attachedSamplesSplitPane;
	private final JSplitPane studySsplitPane;

	private CardLayout cardLayout = new CardLayout();
	private JPanel infoCardTabbedPane = new JPanel(cardLayout);
	private JLabel infoLabel = new JLabel();

	/**
	 * Creates a StudyDetailPanel with an orientation:
	 * - JSplitPane.HORIZONTAL_SPLIT
	 * - JSplitPane.VERTICAL_SPLIT
	 * @param orientation
	 */
	public StudyDetailPanel(final int orientation) {

		//init depictor
		studyDepictor.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				StudyDetailPanel.this.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
			}
		});

		final BiosampleTabbedPane biosamplePane = new BiosampleTabbedPane();
		biosamplePane.setSelectedTab(BiosampleTabbedPane.HISTORY_TITLE);
		ListSelectionListener l = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				biosamplePane.setBiosamples(animalTable.getSelection().size()==1? animalTable.getSelection(): null);
			}
		};
		animalTable.getSelectionModel().addListSelectionListener(l);
		animalTable.getColumnModel().getSelectionModel().addListSelectionListener(l);
		animalTable.getModel().setMode(Mode.COMPACT);

		attachedSamplesSplitPane = new JSplitPane(orientation==SwingUtilities.HORIZONTAL? JSplitPane.HORIZONTAL_SPLIT: JSplitPane.VERTICAL_SPLIT, new JBGScrollPane(animalTable, 3), biosamplePane);
		//		if(orientation==SwingUtilities.HORIZONTAL) {
		//
		//			((JSplitPane)animalTab).setDividerLocation(600);
		//
		//		} else if(orientation==SwingUtilities.VERTICAL) {
		//
		//			animalTab = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JBGScrollPane(animalTable, 3), biosamplePane);
		//
		//		}
		attachedSamplesSplitPane.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(java.awt.event.ComponentEvent e) {
				attachedSamplesSplitPane.setDividerLocation(.5);
			};
		});

		JSplitPane infoSplitPane = new JSplitPane(orientation==SwingUtilities.HORIZONTAL? JSplitPane.HORIZONTAL_SPLIT: JSplitPane.VERTICAL_SPLIT, new JScrollPane(editorPane), graphPanel);
		infoSplitPane.setOneTouchExpandable(true);

		infoTabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
		infoTabbedPane.add("Samples", attachedSamplesSplitPane);
		infoTabbedPane.add("Samplings", new JScrollPane(samplingsEditorPane));
		infoTabbedPane.add("Infos", infoSplitPane);
		infoTabbedPane.setSelectedIndex(1);

		infoCardTabbedPane.add("read", infoTabbedPane);
		infoCardTabbedPane.add("view", UIUtils.createVerticalBox(infoLabel, Box.createVerticalGlue()));
		cardLayout.show(infoCardTabbedPane, "read");

		studySsplitPane = new JSplitPane(orientation, new JScrollPane(studyDepictor), infoCardTabbedPane);
		studySsplitPane.setOneTouchExpandable(true);
		SwingUtilities.invokeLater(() -> {
			if(orientation==JSplitPane.HORIZONTAL_SPLIT) {
				attachedSamplesSplitPane.setDividerLocation(.6);
				studySsplitPane.setDividerLocation(.7);
			} else {
				attachedSamplesSplitPane.setDividerLocation(.5);
				studySsplitPane.setDividerLocation(.5);
			}
			infoSplitPane.setDividerLocation(.5);
		});

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, studySsplitPane);
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
			BiosampleActions.attachRevisionPopup(animalTable);
		} else {
			StudyActions.attachPopup(studyDepictor);
			StudyActions.attachPopup(editorPane);
			BiosampleActions.attachPopup(animalTable);
		}

	}

	public void setForRevision(boolean forRevision) {
		editorPane.setForRevision(forRevision);
		this.forRevision = forRevision;
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

	//	/**
	//	 * Refresh the tabbedPane in a worker.
	//	 */
	//	private void refresh() {
	//		if(study==null || forRevision) {
	//			studyDepictor.setStudy(study);
	//			refreshTabbedPane();
	//		} else {
	//			new SwingWorkerExtended("Loading Study", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
	//				@Override
	//				protected void done() {
	//					study = JPAUtil.reattach(study);
	//					refreshTabbedPane();
	//					studyDepictor.setStudy(study);
	//				}
	//			};
	//		}
	//	}
	//
	//	/**
	//	 * Refresh the tabbedPane, without reattaching the study
	//	 */
	//	private void refreshTabbedPane() {
	//		if(study==null) {
	//			cardLayout.show(infoCardTabbedPane, "view");
	//			infoLabel.setText("");
	//		} else if(SpiritRights.canRead(study, SpiritFrame.getUser())) {
	//			if(infoTabbedPane.getSelectedIndex()==0) {
	//				final List<Biosample> animals = study==null?new ArrayList<Biosample>(): new ArrayList<>(study.getAttachedBiosamples());
	//				Collections.sort(animals);
	//				animalTable.setRows(animals);
	//			} else if(infoTabbedPane.getSelectedIndex()==1) {
	//				samplingsEditorPane.setStudy(study);
	//			} else if(infoTabbedPane.getSelectedIndex()==2) {
	//				editorPane.setStudy(null);
	//				graphPanel.setResults(new ArrayList<>());
	//				new SwingWorkerExtended(graphPanel, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
	//					private List<Result> results;
	//					@Override
	//					protected void doInBackground() throws Exception {
	//						results = DAOResult.queryResults(ResultQuery.createQueryForSids(Collections.singleton(study.getId())), SpiritFrame.getUser());
	//					}
	//					@Override
	//					protected void done() {
	//						editorPane.setStudy(study);
	//						graphPanel.setResults(results);
	//					}
	//				};
	//			}
	//			cardLayout.show(infoCardTabbedPane, "read");
	//		} else {
	//			cardLayout.show(infoCardTabbedPane, "view");
	//			infoLabel.setText("<html><div style='color:red'>You don't have sufficient right on this study.<br> You may request permission to one of the responsibles:</b><br><ul><li> "+MiscUtils.flatten(study.getAdminUsersAsSet(), "<li>") + "</ul>");
	//		}
	//	}


	/**
	 * Refresh the tabbedPane in a worker.
	 */
	private void refresh() {
		if(study==null) {
			cardLayout.show(infoCardTabbedPane, "view");
			infoLabel.setText("");
			studyDepictor.setStudy(null);
		} else if(SpiritRights.canRead(study, SpiritFrame.getUser())) {
			studyDepictor.setStudy(null);
			editorPane.setStudy(null);
			graphPanel.setResults(new ArrayList<>());
			new SwingWorkerExtended("Loading Study", this, forRevision? SwingWorkerExtended.FLAG_SYNCHRONOUS: SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
				@Override
				protected void doInBackground() throws Exception {
					if(!forRevision) {
						study = JPAUtil.reattach(study);
					}
					if(study!=null) {
						study.getAttachedBiosamples();
						study.getNamedTreatments();
						study.getNamedSamplings();
						study.getStudyActions();
					}
				}
				@Override
				protected void done() {
					studyDepictor.setStudy(study);
				}
			}.afterDone(() -> {
				refreshTabbedPane();
			});

		} else {
			cardLayout.show(infoCardTabbedPane, "view");
			infoLabel.setText("<html><div style='color:red'>You don't have sufficient right on this study.<br> You may request permission to one of the responsibles:</b><br><ul><li> "+MiscUtils.flatten(study.getAdminUsersAsSet(), "<li>") + "</ul>");
			studyDepictor.setStudy(null);
		}
	}



	private void refreshTabbedPane() {
		if(study==null) return;
		if(infoTabbedPane.getSelectedIndex()==0) {
			new SwingWorkerExtended("Loading Samples", this, forRevision? SwingWorkerExtended.FLAG_SYNCHRONOUS: SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
				private List<Biosample> attached = null;
				@Override
				protected void doInBackground() throws Exception {
					attached = study==null?new ArrayList<Biosample>(): new ArrayList<>(study.getAttachedBiosamples());
					Collections.sort(attached);
				}

				@Override
				protected void done() {
					animalTable.setRows(attached);
					cardLayout.show(infoCardTabbedPane, "read");
				}
			};
		} else if(infoTabbedPane.getSelectedIndex()==1) {
			new SwingWorkerExtended("Loading Samplings", this, forRevision? SwingWorkerExtended.FLAG_SYNCHRONOUS: SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
				@Override
				protected void done() {
					samplingsEditorPane.setStudy(study);
					cardLayout.show(infoCardTabbedPane, "read");
				}
			};
		} else if(infoTabbedPane.getSelectedIndex()==2) {
			new SwingWorkerExtended("Loading Details", this, forRevision? SwingWorkerExtended.FLAG_SYNCHRONOUS: SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
				@Override
				protected void done() {
					editorPane.setStudy(study);
					cardLayout.show(infoCardTabbedPane, "read");
				}
			}.afterDone(() -> {
				new SwingWorkerExtended("Loading Results", this, forRevision? SwingWorkerExtended.FLAG_SYNCHRONOUS: SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
					private List<Result> results = null;
					private int maxResults = 4000;
					@Override
					protected void doInBackground() throws Exception {
						ResultQuery q = ResultQuery.createQueryForSids(Collections.singleton(study.getId()));
						q.setMaxResults(maxResults);
						results = DAOResult.queryResults(q, SpiritFrame.getUser());
					}

					@Override
					protected void done() {
						if(results.size()>=maxResults) {
							graphPanel.setErrorText("There are too many results to be displayed (>" + maxResults + ")");
						} else {
							graphPanel.setResults(results);
						}
					};
				};
			});
		}
	}

	public void showAttached() {
		infoTabbedPane.setSelectedIndex(0);
		refresh();
	}

	public void showInfos() {
		infoTabbedPane.setSelectedIndex(2);
		refresh();
	}


	public Study getStudy() {
		return study;
	}

}
