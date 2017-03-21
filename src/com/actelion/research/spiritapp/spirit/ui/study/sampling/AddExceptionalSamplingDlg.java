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

package com.actelion.research.spiritapp.spirit.ui.study.sampling;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleList;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritapp.spirit.ui.study.PhaseComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 * Class used to add/move a sampling for a synchronized study
 * @author freyssj
 *
 */
public class AddExceptionalSamplingDlg extends JEscapeDialog {
	
	
	private Study study;
	private PhaseComboBox phaseComboBox = new PhaseComboBox();
	private BiosampleList biosampleList = new BiosampleList();
	private NamedSamplingComboBox namedSamplingComboBox;

	
	public AddExceptionalSamplingDlg(Study study) {
		this(study, null, null);
		
	}

	public AddExceptionalSamplingDlg(Study s, List<Biosample> specimen, Phase phase) {
		super(UIUtils.getMainFrame(), "Add Exceptional Sampling");
		
		this.study = JPAUtil.reattach(s);

		if(!study.isSynchronizeSamples()) {
			JExceptionDialog.showError(new Exception("This feature can only be used for synchronized studies"));
			return;
		}
				
		namedSamplingComboBox = new NamedSamplingComboBox(study.getNamedSamplings(), true);
		
		//Components
//		JButton newSamplingButton = new JButton("New Sampling");
//		newSamplingButton.addActionListener(e -> {
//			CreateChildrenDlg
//		});
		
		JButton newPhaseButton = new JButton("New Phase");
		newPhaseButton.addActionListener(e-> {
			String phaseName = JOptionPane.showInputDialog(AddExceptionalSamplingDlg.this, "Please enter the new phase (Format: " + study.getPhaseFormat() + ")", "New Phase", JOptionPane.QUESTION_MESSAGE);
			if(phaseName==null) return;
			try {
				Phase newPhase = new Phase(phaseName);
				if(study.getPhase(newPhase.getShortName())!=null) {
					throw new Exception("The phase "+newPhase+" exists already");
				}
						
				List<Phase> phases = new ArrayList<>();
				phases.addAll(study.getPhases());
				phases.add(newPhase);
				phaseComboBox.setValues(phases);
				phaseComboBox.setSelection(newPhase);
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});		
		
		List<Phase> phases = new ArrayList<>();
		phases.addAll(study.getPhases());
		phaseComboBox.setValues(phases);
		phaseComboBox.selectCurrentPhase();
				
		final List<Biosample> animalInGroups = new ArrayList<>();
		for(Biosample b: study.getTopAttachedBiosamples()) {
			if(b.getInheritedGroup()!=null) animalInGroups.add(b);
		}		
		
		biosampleList.setBiosamples(animalInGroups);		
		
		JButton okButton = new JIconButton(IconType.SAVE.getIcon(), "Add Sampling & Create Samples");
		okButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					eventOk();
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});

		//Layout		
		JEditorPane infoPanel = new ImageEditorPane( 
				"<html>Use this dialog to add a sampling not covered by the current study design (ex: sacrifice after some trigger).<br><br>"
				+ " The study design will then be updated to match this case."
				+ " </html>");
		LF.initComp(infoPanel);
		infoPanel.setEditable(false);
		infoPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(2, 2, 2, 2)));

		JScrollPane sp = new JScrollPane(biosampleList);
		sp.setPreferredSize(new Dimension(200,300));
		JPanel centerPane = UIUtils.createTitleBox("",
				UIUtils.createBox(UIUtils.createHorizontalBox(new JLabel("Sample(s): "), sp, Box.createHorizontalGlue()),
					null,
					UIUtils.createTable(
						new JLabel("Apply Template: "), UIUtils.createHorizontalBox(namedSamplingComboBox/*, newSamplingButton*/),
						new JLabel("At Phase: "), UIUtils.createHorizontalBox(phaseComboBox, newPhaseButton)				
						)));
		
		
		JPanel contentPane = UIUtils.createBox(centerPane, infoPanel, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));
		setContentPane(contentPane);
				
		if(specimen!=null) biosampleList.setSelection(specimen);
		if(phase!=null) phaseComboBox.setSelection(phase);		
		
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}
	
	private void eventOk() throws Exception {
		Phase phase = phaseComboBox.getSelection();
		if(phase==null) throw new Exception("You must select a phase");
		
		List<Biosample> animals = biosampleList.getSelection();
		if(animals.size()==0) throw new Exception("You must select one or more samples");
		Group group = animals.get(0).getInheritedGroup();
		int subgroup = animals.get(0).getInheritedSubGroup();
		for (int i = 0; i < animals.size(); i++) {
			if(animals.get(i).getInheritedGroup()==null) throw new Exception("The samples must belong to a group");
			if(!animals.get(i).getInheritedGroup().equals(group)) throw new Exception("The selected samples must all belong to the same group");
			if(animals.get(i).getInheritedSubGroup()!=subgroup) throw new Exception("The selected samples must all belong to the same subgroup");
		}
		
		NamedSampling ns = namedSamplingComboBox.getSelection();
		if(ns==null) throw new Exception("You must select a sampling");
				
		//Check the phase is not after a necropsy
		if(ns.isNecropsy()) {
			//OK
		} else {
			if(group.getEndPhase(subgroup)!=null && phase.compareTo(group.getEndPhase(subgroup))>0) throw new Exception("You cannot apply a sampling after the necropsy at "+group.getEndPhase(subgroup));
		}
		
		//Add the phase if needed
		if(phase.getId()<=0) {
			phase.setStudy(study);		
			study.getPhases().add(phase);
		}				

		//Proceed with this group/subgroup
		//Create 2 sets of animals: 1 that will be updated with this sampling (samplesToStayInSubgroup) and 1 that will be moved to a new subgroup (samplesToMoveInNewSubgroup)
		List<Biosample> toSave = new ArrayList<>();
		Set<Biosample> samplesToStayInSubgroup = new HashSet<>(study.getTopAttachedBiosamples(group, subgroup));
		for (Biosample animal : animals) {
			if(animal.getStatus().isAvailable()) {
				samplesToStayInSubgroup.remove(animal);				
			}
		}
		
		Set<Biosample> samplesToMoveInNewSubgroup = new HashSet<>(animals);
		
		LoggerFactory.getLogger(getClass()).debug("considered=                "+study.getTopAttachedBiosamples(group, subgroup));
		LoggerFactory.getLogger(getClass()).debug("samplesToStayInSubgroup=   "+samplesToStayInSubgroup+" / subgroup="+subgroup);
		LoggerFactory.getLogger(getClass()).debug("samplesToMoveInNewSubgroup="+samplesToMoveInNewSubgroup);
		
		if(samplesToStayInSubgroup.size()>0) {			
			//Create a new subgroup
			int newSubGroupNo = group.getNSubgroups();
			int[] newSubGroupSizeArray = new int[newSubGroupNo+1];
			for (int i = 0; i < subgroup; i++) {
				newSubGroupSizeArray[i] = group.getSubgroupSize(i);
			}
			newSubGroupSizeArray[subgroup] = group.getSubgroupSize(subgroup) - samplesToMoveInNewSubgroup.size();
			newSubGroupSizeArray[newSubGroupNo] = samplesToMoveInNewSubgroup.size();
			
			group.setSubgroupSizes(newSubGroupSizeArray);

			//Copy the actions to this new subgroup
			for(StudyAction a: new ArrayList<>(study.getStudyActions(group, subgroup))) {
				StudyAction action = new StudyAction(a);
				action.setSubGroup(newSubGroupNo);
				study.getStudyActions().add(action);
			}
			
			//Assign the biosamples to this new subgroup
			for(Biosample b: samplesToMoveInNewSubgroup) {
				b.setInheritedSubGroup(newSubGroupNo);
				toSave.add(b);
			}
			
			//We add the sampling for the selected animals, which remain in the former group/subgroup at the given phase
			study.resetCache();
			
			subgroup = newSubGroupNo;
		} 
		
		//Modify the existing subgroup
		if(ns.isNecropsy() && group.getEndPhase(subgroup)!=null) {
			StudyAction a = study.getStudyAction(group, subgroup, group.getEndPhase(subgroup));
			assert a!=null;
			if(a.getNamedSampling1()!=null && a.getNamedSampling1().isNecropsy()) a.setNamedSampling1(null);
			if(a.getNamedSampling2()!=null && a.getNamedSampling2().isNecropsy()) a.setNamedSampling2(null);
		}
		study.setNamedSampling(group, phase, subgroup, ns, true);
		LoggerFactory.getLogger(getClass()).debug("add sampling to " + group + " " + phase + " " + subgroup);
		
		//Save in a transaction
		JPAUtil.pushEditableContext(SpiritFrame.getUser());
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			DAOStudy.persistStudies(session, Collections.singletonList(study), SpiritFrame.getUser());
			if(toSave.size()>0) {
				DAOBiosample.persistBiosamples(session, toSave, SpiritFrame.getUser());
			}
			
			//Fire change Event
			txn.commit();
			txn = null;
			dispose();			
		} catch (Exception e) {
			throw e;
		} finally {
			if(txn!=null && txn.isActive()) try{ txn.rollback();} catch(Exception e2) {}
			JPAUtil.popEditableContext();
		}
		
		SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, study);
		if(toSave.size()>0) SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, toSave);

	}

	
}
