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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTableModel.Mode;
import com.actelion.research.spiritapp.spirit.ui.study.depictor.StudyDepictor;
import com.actelion.research.spiritapp.spirit.ui.study.depictor.ZoomScrollPane;
import com.actelion.research.spiritapp.spirit.ui.study.sampling.NamedSamplingEditorPane;
import com.actelion.research.spiritapp.spirit.ui.util.bgpane.JBGScrollPane;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
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
	private final NamedSamplingEditorPane samplingsEditorPane = new NamedSamplingEditorPane();
	
	private JComponent animalTab;
	private final JSplitPane splitPane;

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
		
		if(orientation==SwingUtilities.HORIZONTAL) {
			
			animalTab = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JBGScrollPane(animalTable, 3), biosamplePane);
			((JSplitPane)animalTab).setDividerLocation(600);
			
		} else if(orientation==SwingUtilities.VERTICAL) {
			
			biosamplePane.setTabPlacement(JTabbedPane.RIGHT);
			animalTab = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JBGScrollPane(animalTable, 3), biosamplePane);
			((JSplitPane)animalTab).setDividerSize(4);
			
			animalTab.addComponentListener(new ComponentAdapter() {
				public void componentShown(java.awt.event.ComponentEvent e) {
					((JSplitPane)animalTab).setDividerLocation(animalTab.getHeight()-175);
				};
			});
		}

		
		infoTabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
		infoTabbedPane.add("Samples", animalTab);
		infoTabbedPane.add("Samplings", new JScrollPane(samplingsEditorPane));
		infoTabbedPane.add("Infos", new JScrollPane(editorPane));		
		infoTabbedPane.setSelectedIndex(1);
		ZoomScrollPane depictorScrollPane = new ZoomScrollPane(studyDepictor);

		depictorScrollPane.setBorder(BorderFactory.createEmptyBorder());
		
		infoCardTabbedPane.add("read", infoTabbedPane);
		infoCardTabbedPane.add("view", UIUtils.createVerticalBox(infoLabel, Box.createVerticalGlue()));
		cardLayout.show(infoCardTabbedPane, "read");
		
		splitPane = new JSplitPane(orientation, depictorScrollPane, infoCardTabbedPane);
		splitPane.setOneTouchExpandable(true);
		Dimension dim = UIUtils.getMainFrame()==null? new Dimension(1600,1000) :UIUtils.getMainFrame().getSize(); 
		if(orientation==JSplitPane.HORIZONTAL_SPLIT) { 
			splitPane.setDividerLocation(dim.width-400);
		} else {
			splitPane.setDividerLocation(400);
		}
		
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, splitPane);
		setOpaque(true);
		setPreferredSize(new Dimension(orientation==JSplitPane.HORIZONTAL_SPLIT? 800: 320, orientation==JSplitPane.HORIZONTAL_SPLIT? 350: 700));
		
		infoTabbedPane.addChangeListener(new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				refreshTabbedPane();
			}
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

	private void refresh() {
		if(study==null || forRevision) {
			studyDepictor.setStudy(study);
			refreshTabbedPane();
		} else {
			new SwingWorkerExtended("Loading Study", this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
				@Override
				protected void done() {
					study = JPAUtil.reattach(study);
					refreshTabbedPane();
					studyDepictor.setStudy(study);
				}
			};
		}
	}
	
	private void refreshTabbedPane() {
		if(study==null) {
			cardLayout.show(infoCardTabbedPane, "view");
			infoLabel.setText("");
		} else if(SpiritRights.canRead(study, Spirit.getUser())) {
			if(infoTabbedPane.getSelectedIndex()==0) {
				final List<Biosample> animals = study==null?new ArrayList<Biosample>(): new ArrayList<>(study.getAttachedBiosamples());
				Collections.sort(animals);			
				animalTable.setRows(animals);			
			} else if(infoTabbedPane.getSelectedIndex()==1) {
				samplingsEditorPane.setStudy(study);
			} else {				
				editorPane.setStudy(study);	
			}
			cardLayout.show(infoCardTabbedPane, "read");
		} else {
			cardLayout.show(infoCardTabbedPane, "view");
			infoLabel.setText("<html><div style='color:red'>You don't have sufficient right on this study.<br> You may request permission to one of the responsibles:</b><br><ul><li> "+MiscUtils.flatten(study.getAdminUsersAsSet(), "<li>") + "</ul>");
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
