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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleComboBox;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerTypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.helper.CreateSamplesHelper;
import com.actelion.research.spiritapp.spirit.ui.lf.BiotypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

public class ManageSamplesDlg extends JEscapeDialog implements ISpiritChangeObserver {

	private Study study;
	private List<Biosample> animals;
	private Set<Biosample> allSamples;	
	private BiosampleTable biosampleTable = new BiosampleTable();
	private BiosampleComboBox biosampleFilter = new BiosampleComboBox();
	private GroupComboBox groupFilter = new GroupComboBox();
	private PhaseComboBox phaseFilter = new PhaseComboBox();
	private BiotypeComboBox biotypeFilter = new BiotypeComboBox();
	private ContainerTypeComboBox containerTypeFilter = new ContainerTypeComboBox();
	private JCustomTextField biosearchTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC);
	private JCheckBox hideWithoutContainersCheckbox = new JCheckBox("Hide without container", false);
	private JCheckBox hideDeadCheckBox = new JCheckBox("Hide dead/used up", false);
	private JLabel selectedLabel = new JLabel();
	
	private BiosampleTabbedPane detailPane = new BiosampleTabbedPane(); 
	private int push = 0;
	
	public ManageSamplesDlg(Study study) {
		this(study, null);
	}
	
	public ManageSamplesDlg(Study s, Collection<Biosample> animals) {
		super(UIUtils.getMainFrame(), "Manage " + s + " Samples");
				
		try {			
			if(s.isSynchronizeSamples()) {
				//Always synchronize samples to match the study design				
				boolean res = CreateSamplesHelper.synchronizeSamples(s);
				if(!res) return;
			} 					
			
			this.study = DAOStudy.getStudy(s.getId());
			this.animals = animals==null? null: JPAUtil.reattach(animals);
			
			//Retrieve all samples to be printed			
			if(study.getTopAttachedBiosamples().size()==0) throw new Exception("You must first assign some samples to this study");
			
			//Init components
			SpiritChangeListener.register(this);
			
			JButton printButton = new JButton(new BiosampleActions.Action_Print(null) {
				@Override
				public List<Biosample> getBiosamples() {
					return biosampleTable.getSelection().size()>=1? biosampleTable.getSelection(): biosampleTable.getRows();
				}
			});
			
			JButton assignButton = new JButton(new BiosampleActions.Action_AssignTo(null) {
				@Override
				public List<Biosample> getBiosamples() {
					return biosampleTable.getSelection().size()>=1? biosampleTable.getSelection(): biosampleTable.getRows();
				}
			});

			JButton extraSamplingButton = new JButton(new StudyActions.Action_ExtraSampling(study));
			
			
			extraSamplingButton.setEnabled(false);
			boolean hasSamplings = false;
			for(NamedSampling ns: study.getNamedSamplings()) {
				if(ns.getAllSamplings().size()>0) {
					hasSamplings = true;
				}
			}
			if(hasSamplings) {
				extraSamplingButton.setEnabled(true);
			}
			
			biosampleTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {				
				@Override
				public void valueChanged(ListSelectionEvent e) {
					detailPane.setBiosamples(biosampleTable.getSelection());
					refreshSelectedLabel();
				}
			});
			
			BiosampleActions.attachPopup(biosampleTable);
			
			//ContentPane
			JPanel centerPane = new JPanel(new BorderLayout());
			centerPane.add(BorderLayout.NORTH, 
					UIUtils.createTitleBox("Filters",
						UIUtils.createHorizontalBox(
								UIUtils.createVerticalBox(new JLabel("ContainerType:"), containerTypeFilter), Box.createHorizontalStrut(20),							
								UIUtils.createVerticalBox(new JLabel("TopSample:"), biosampleFilter), Box.createHorizontalStrut(20),							
								UIUtils.createVerticalBox(new JLabel("Group:"), groupFilter), Box.createHorizontalStrut(20),
								UIUtils.createVerticalBox(new JLabel("Phase:"), phaseFilter), Box.createHorizontalStrut(20),
								UIUtils.createVerticalBox(new JLabel("Biotype:"), biotypeFilter), Box.createHorizontalStrut(20),
								UIUtils.createVerticalBox(new JLabel("Keywords:"), biosearchTextField), Box.createHorizontalStrut(20),
								UIUtils.createVerticalBox(hideWithoutContainersCheckbox, hideDeadCheckBox),
								Box.createHorizontalGlue())));
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(biosampleTable), detailPane);
			
			centerPane.add(BorderLayout.CENTER, splitPane);
			centerPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(
					new JLabel("Select some biosamples and select an action: "),
					selectedLabel, 
					Box.createHorizontalGlue(), 
					extraSamplingButton, 
					assignButton, 
					printButton));
			
			
			//Add events
			ActionListener queryActionListener = new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					query(false);
				}
			};
			
			containerTypeFilter.setTextWhenEmpty("Container...");
			biosampleFilter.setTextWhenEmpty("TopSample...");
			groupFilter.setTextWhenEmpty("Group...");
			phaseFilter.setTextWhenEmpty("Phase...");
			biotypeFilter.setTextWhenEmpty("Biotype...");
			
			biosampleFilter.addActionListener(queryActionListener);
			containerTypeFilter.addActionListener(queryActionListener);
			groupFilter.addActionListener(queryActionListener);
			phaseFilter.addActionListener(queryActionListener);
			biotypeFilter.addActionListener(queryActionListener);
			biosearchTextField.addActionListener(queryActionListener);
			hideWithoutContainersCheckbox.addActionListener(queryActionListener);
			hideDeadCheckBox.addActionListener(queryActionListener);
			biosearchTextField.addActionListener(queryActionListener);
//			biosearchTextField.addFocusListener(new FocusAdapter() {
//				@Override
//				public void focusLost(FocusEvent e) {
//					query(false);
//				}
//			});

			
			
			//Preload all samples
			query(true);


			setContentPane(centerPane);
			UIUtils.adaptSize(this, 1400, 950);
			splitPane.setDividerLocation(1200);
			SwingUtilities.invokeLater(new Runnable() {				
				@Override
				public void run() {
					splitPane.setDividerLocation(getSize().width-250);
				}
			});
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setVisible(true);
						
		} catch(Exception e) {
			JExceptionDialog.showError(e);
			return;
		}
	}
	
	public void query(final boolean refreshFromDB) {
		if(push>0) return;
		new SwingWorkerExtended("Loading", getContentPane(), SwingWorkerExtended.FLAG_SYNCHRONOUS) {
			List<Biosample> tmp;
			@Override
			protected void doInBackground() throws Exception {
				push++;
				try {
					if(allSamples==null || refreshFromDB) {
						
						
						Study s = JPAUtil.reattach(study);
						allSamples = new LinkedHashSet<>();
						Collection<Biosample> tops = s.getTopAttachedBiosamples();
						for (Biosample animal : tops) {
							if(animals!=null && !animals.contains(animal)) continue;
							allSamples.addAll(animal.getHierarchy(HierarchyMode.AS_STUDY_DESIGN));
							allSamples.remove(animal);
						}
		
						List<Biotype> myBiotypes = new ArrayList<>(Biosample.getBiotypes(allSamples));
						List<ContainerType> myContainers = new ArrayList<>(Biosample.getContainerTypes(allSamples));
						List<Phase> myPhases = new ArrayList<>(Biosample.getPhases(allSamples));
						List<Group> myGroups = new ArrayList<>(Biosample.getGroups(allSamples));
						
						myBiotypes.remove(null);
						myContainers.remove(null);
						myGroups.remove(null);
						myPhases.remove(null);
						
						Collections.sort(myPhases);
						
						biotypeFilter.setValues(myBiotypes);
						containerTypeFilter.setValues(myContainers);
						biosampleFilter.setValues(tops);
						groupFilter.setValues(myGroups);
						phaseFilter.setValues(myPhases);
	
					}
	
					
					//Filter all without a container
					tmp  = new ArrayList<>();
					for (Biosample b : allSamples) {
						if(biotypeFilter.getSelection()!=null && !biotypeFilter.getSelection().equals(b.getBiotype())) continue; 
						if(containerTypeFilter.getSelection()!=null && !containerTypeFilter.getSelection().equals(b.getContainerType())) continue; 
						if(biosampleFilter.getSelection()!=null && !biosampleFilter.getSelection().equals(b.getTopParentInSameStudy())) continue; 
						if(groupFilter.getSelection()!=null && !groupFilter.getSelection().equals(b.getInheritedGroup())) continue; 
						if(phaseFilter.getSelection()!=null && !phaseFilter.getSelection().equals(b.getInheritedPhase())) continue; 
						if(hideDeadCheckBox.isSelected() && (!b.getStatus().isAvailable() || !b.getTopParent().getStatus().isAvailable()) ) continue; 
						if(hideWithoutContainersCheckbox.isSelected() && b.getContainerType()==null) continue; 
						if(biosearchTextField.getText().length()>0 && !b.isCompatible(biosearchTextField.getText(), null)) continue;
						tmp.add(b);
						
					}
				} finally {
					push--;
				}
			}
			@Override
			protected void done() {
				tmp = JPAUtil.reattach(tmp);
				biosampleTable.getModel().getExtraColumns().clear();
				biosampleTable.setRows(tmp);
				refreshSelectedLabel();
			}
			
		};
	}
	
	public void refreshSelectedLabel() {
		selectedLabel.setText("(" + (biosampleTable.getSelection().size()==0? biosampleTable.getRows().size():biosampleTable.getSelection().size()) + " selected)");
	}
	
	@Override
	public <T> void actionModelChanged(SpiritChangeType action, Class<T> what, List<T> details) {
		query(true);
	}
	

}
