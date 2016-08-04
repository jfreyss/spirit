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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleComboBox;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleList;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.study.sampling.AddExceptionalSamplingDlg;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Metadata;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.Formatter;
import com.actelion.research.util.ui.DateTextField;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.LongTaskDlg;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;
import com.itextpdf.text.Font;

public class SetLivingStatusDlg extends JSpiritEscapeDialog {
	
	private final Study study;
	
	private BiosampleList animalList = new BiosampleList();
	private BiosampleComboBox replaceByComboBox = new BiosampleComboBox();
	private DateTextField dateField = new DateTextField(true);
	
	private JRadioButton removedAnimalRadioButton = new JRadioButton("Move to Reserve ");
	private JRadioButton statusAliveRadioButton = new JRadioButton("Change Status to: Alive");
	private JRadioButton statusNecropsiedRadioButton = new JRadioButton("Change Status to: Necropsied");
	private JRadioButton statusDeadRadioButton = new JRadioButton("Change Status to: Found Dead (Samples will not be generated)");
	private JRadioButton statusKilledRadioButton = new JRadioButton("Change Status to: Killed (Samples will not be generated)");
	private PhaseComboBox phaseComboBox;
	
	private JCustomTextField observationField = new JCustomTextField();
	
	public SetLivingStatusDlg(final Study s, List<Biosample> selectedBiosamples) {
		super(UIUtils.getMainFrame(), "Set Living Status", SetLivingStatusDlg.class.getName());
		this.study = JPAUtil.reattach(s);
		
		if(study==null) {
			JExceptionDialog.showError("Study cannot be null");
			return;
		}

		selectedBiosamples = JPAUtil.reattach(selectedBiosamples);
		
		
		phaseComboBox = new PhaseComboBox(study.getPhases());	
		phaseComboBox.setSelection(study.getPhase(JPAUtil.getCurrentDateFromDatabase()));
		dateField.setText(Formatter.formatDate(new Date()));
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.VERTICAL;
		
		//Find animals in the reserve		
		java.util.List<Biosample> biosamples = new ArrayList<Biosample>(study.getAttachedBiosamples());
		java.util.List<Biosample> reserve = new ArrayList<Biosample>();
		for (Biosample b : biosamples) {	
			if(b.getInheritedGroup()==null) {
				reserve.add(b);			
			}
		}
		Collections.sort(reserve);
		
		//Prepare the UI
		replaceByComboBox.setValues(reserve, "");
		
		animalList.setBiosamples(biosamples);
		animalList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				eventAnimalChanged();
			}
		});
		
		
		observationField.setEnabled(false);

		
		//Action when setting action
		ButtonGroup group = new ButtonGroup();
		group.add(removedAnimalRadioButton);
		group.add(statusAliveRadioButton);
		group.add(statusNecropsiedRadioButton);
		group.add(statusKilledRadioButton);
		group.add(statusDeadRadioButton);
		removedAnimalRadioButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				replaceByComboBox.setEnabled(true);
				dateField.setEnabled(false);
			}
		});
		ActionListener al = new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				replaceByComboBox.setEnabled(true);
				replaceByComboBox.setSelection(null);
				dateField.setEnabled(!statusAliveRadioButton.isSelected());
				dateField.setText(statusAliveRadioButton.isSelected()?"": Formatter.formatDate(phaseComboBox.getSelection()==null || phaseComboBox.getSelection().getAbsoluteDate()==null? new Date(): phaseComboBox.getSelection().getAbsoluteDate()));
				Status s = getSelectedStatus();
				observationField.setText(s==null || s==Status.INLAB?"": s.getName()) ;

			}
		};
		statusAliveRadioButton.addActionListener(al);
		statusNecropsiedRadioButton.addActionListener(al);
		statusKilledRadioButton.addActionListener(al);
		statusDeadRadioButton.addActionListener(al);
				
		//Action when setting phase
		phaseComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dateField.setText(statusAliveRadioButton.isSelected()?"": Formatter.formatDate(phaseComboBox.getSelection()==null || phaseComboBox.getSelection().getAbsoluteDate()==null? new Date(): phaseComboBox.getSelection().getAbsoluteDate()));
				observationField.setEnabled(phaseComboBox.getSelection()!=null);
			}
		});
		
		JButton saveButton = new JIconButton(IconType.SAVE, "Set Status");
		saveButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					eventOk();
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, study);
					dispose();
				} catch (Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.NORTH, UIUtils.createTitleBox("Select the biosamples and the action to perform", UIUtils.createVerticalBox(
					new JScrollPane(animalList),
					UIUtils.createVerticalBox(
							UIUtils.createHorizontalBox(removedAnimalRadioButton, Box.createHorizontalGlue()),
							UIUtils.createHorizontalBox(statusAliveRadioButton, Box.createHorizontalGlue()),
							UIUtils.createHorizontalBox(statusNecropsiedRadioButton, Box.createHorizontalGlue()),
							UIUtils.createHorizontalBox(statusDeadRadioButton, Box.createHorizontalGlue()),
							UIUtils.createHorizontalBox(statusKilledRadioButton, Box.createHorizontalGlue())))));
				
		contentPanel.add(BorderLayout.CENTER, UIUtils.createVerticalBox(
					
				UIUtils.createTitleBox("When", UIUtils.createVerticalBox(
						UIUtils.createHorizontalBox(new JLabel("Phase: (optional) "), phaseComboBox, Box.createHorizontalStrut(20), new JLabel("DateOfDeath: "), dateField, Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(new JLabel("Observation: "), observationField, Box.createHorizontalGlue()))),

				UIUtils.createTitleBox("Replacement", 
						UIUtils.createHorizontalBox(new JLabel("To be replaced by: (optional)"), replaceByComboBox, new JCustomLabel(" (optional)", Font.ITALIC, Color.LIGHT_GRAY), Box.createHorizontalGlue()))));
		
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), saveButton));
		
		
		if(selectedBiosamples!=null && selectedBiosamples.size()>0) animalList.setSelection(selectedBiosamples);

		setContentPane(contentPanel);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
		
		
	}
	
	private void eventAnimalChanged() {
		List<Biosample> animals = animalList.getSelection();
		removedAnimalRadioButton.setEnabled(animals.size()>0);
		statusAliveRadioButton.setEnabled(animals.size()>0);
		statusDeadRadioButton.setEnabled(animals.size()>0);
		statusKilledRadioButton.setEnabled(animals.size()>0);
		statusNecropsiedRadioButton.setEnabled(animals.size()>0);
		for (Biosample a : animals) {
			removedAnimalRadioButton.setEnabled(removedAnimalRadioButton.isEnabled() && a.getInheritedGroup()!=null  && a.getStatus().isAvailable());
			statusAliveRadioButton.setEnabled(statusAliveRadioButton.isEnabled() && !a.getStatus().isAvailable());
			statusDeadRadioButton.setEnabled(statusDeadRadioButton.isEnabled() && a.getStatus().isAvailable());
			statusKilledRadioButton.setEnabled(statusKilledRadioButton.isEnabled() && a.getStatus().isAvailable());
			statusNecropsiedRadioButton.setEnabled(statusNecropsiedRadioButton.isEnabled() && a.getStatus().isAvailable());
		}
		
	}
	
	private Status getSelectedStatus() {
		return statusAliveRadioButton.isSelected()? Status.INLAB:
			statusDeadRadioButton.isSelected()? Status.DEAD:
			statusKilledRadioButton.isSelected()? Status.KILLED:
			statusNecropsiedRadioButton.isSelected()? Status.NECROPSY: null;
	}
	
	private void eventOk() throws Exception {
		
		//Validate the form
		final SpiritUser user = Spirit.askForAuthentication();
		
		List<Biosample> animals = animalList.getSelection();
		if(animals.size()==0) throw new Exception("You must select an animal");
		

		Status status = getSelectedStatus();
		if(status==null && !removedAnimalRadioButton.isSelected()) {
			throw new Exception("You must select an action");
		}
		
		Biosample replacement = replaceByComboBox.getSelection();
		if(replacement!=null && animals.size()>1) throw new Exception("You can only set a replacement if you select one animal");
		
		Date dod = Formatter.parseDate(dateField.getText());
		if(dateField.getText().length()>0 && dod==null) throw new Exception("You must give a valid date of death");
		
		Phase phase = phaseComboBox.getSelection();
		
		if(phase!=null && !phase.getStudy().equals(study)) throw new Exception("the phase does not match the study");
		
		if(status!=null && study!=null && phase==null && (status==Status.DEAD || status==Status.KILLED || status==Status.NECROPSY)) {
			throw new Exception("You must enter a phase");
		}
		String observation = observationField.isEnabled()? observationField.getText(): null;
		

		////////////////////////////////////////////////////////////////////////
		//update the db
		final List<Result> resultsToSave = new ArrayList<Result>();
		final List<Biosample> biosamplesToSave = new ArrayList<Biosample>();
		final List<Biosample> samplesFromNecropsy = new ArrayList<Biosample>();

		for(Biosample animal: animals) {
			//update the status
			if(status!=null) {
				animal.setStatus(status, phase);
			}
			
	
			// set the dod if possible
			if (status!=null && !status.isAvailable() && animal.getMetadata(Metadata.DATEOFDEATH) != null) {
				animal.setContainer(null);
				if (dod != null) {
					animal.setMetadata(Metadata.DATEOFDEATH, Formatter.formatDate(dod));
				} else {
					animal.setMetadata(Metadata.DATEOFDEATH, null);
				}
			}
			biosamplesToSave.add(animal);
		
				
			//Replacement Animal, only valid if one animal was selected
			if (replacement != null) {
				
				// Attach the replacement
				replacement.setAttached(animal.getInheritedGroup().getStudy(), animal.getInheritedGroup(), animal.getInheritedSubGroup());
	
				// Set the comments of the replacement and the animal
				String comments1 = "Replaced by " + replacement.getSampleId();
				String comments2 = "Replacing " + animal.getSampleId();
				replacement.setComments((replacement.getComments() == null || replacement.getComments().length() == 0 ? "" : replacement.getComments() + " ") + comments2);
				animal.setComments((animal.getComments() == null || animal.getComments().length() == 0 ? "" : animal.getComments() + " ") + comments1);
	
				// Exchange the container of the replacement and the animal
//				Container ctn = replacement.getContainer();
				replacement.setContainer(animal.getContainer());
//				animal.setContainer(ctn);
				biosamplesToSave.add(replacement);
			}

			
			//Move Necropsied Samples to the given phase		
			if(phase!=null) {
				for(Biosample b: animal.getHierarchy(HierarchyMode.ATTACHED_SAMPLES)) {
					if(b.getAttachedSampling()==null) continue;
					if(b.getAttachedSampling().getNamedSampling()==null) continue;
					if(!b.getAttachedSampling().getNamedSampling().isNecropsy()) continue;
					
					samplesFromNecropsy.add(b);
				}
			}
			
			//Add the Observation?
			if(phase!=null && observation!=null && observation.length()>0) {
				Test t = DAOTest.getTest(DAOTest.OBSERVATION_TESTNAME);
				if(t==null) throw new Exception("The test "+DAOTest.OBSERVATION_TESTNAME+" does not exist");
				Result r = new Result();
				r.setElb(phase.getStudy().getStudyId());
				r.setTest(t);
				r.setBiosample(animal);
				r.setPhase(phase);
				r.setFirstOutputValue(observation);
				resultsToSave.add(r);
			}
			
			if(removedAnimalRadioButton.isSelected()) {
				animal.setInheritedGroup(null);
			}


		}
			
		if(samplesFromNecropsy.size()>0 && phase!=null) {
			BiosampleTable table = new BiosampleTable();
			table.setRows(samplesFromNecropsy);
			table.setPreferredSize(new Dimension(600, 150));
			int res = JOptionPane.showOptionDialog(this, 
					UIUtils.createBox(new JScrollPane(table), new JLabel("Do you want to reassign the following samples from the scheduled necropsy to "+phase+"?")), 
					"Samples from Necropsy", 
					JOptionPane.YES_NO_CANCEL_OPTION, 
					JOptionPane.PLAIN_MESSAGE, null, new String[] {"Reassign", "Don't Move", "Cancel"},
					"Reassign");
			if(res==0) {
				for (Biosample b : samplesFromNecropsy) {
					b.setInheritedPhase(phase);
					biosamplesToSave.add(b);
				}				
//			} else if(res==1) {
//				//ok
			} else {
				return;
			}
			
		}
		
		
		
		new LongTaskDlg("Saving") {			
			@Override
			public void longTask() throws Exception {
				//Persist biosamples
				DAOBiosample.persistBiosamples(biosamplesToSave, user);
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamplesToSave);
				
				//Persist results
				if(resultsToSave.size()>0) {
					DAOResult.persistResults(resultsToSave, user);
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_ADDED, Result.class, resultsToSave);
				}

			}
		};

		if(phase!=null && study.isSynchronizeSamples()) {
			int res = JOptionPane.showConfirmDialog(this, "The status is now updated to "+status+". Do you want to add an exceptional sampling?", "Add Sampling?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null);
			if(res==JOptionPane.YES_OPTION) {
				new AddExceptionalSamplingDlg(study, animals, phase);
			}
		}
	
		
	}

}
