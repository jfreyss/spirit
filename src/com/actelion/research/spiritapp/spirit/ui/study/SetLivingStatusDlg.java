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
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleComboBox;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleList;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.biosample.edit.CreateChildrenDlg;
import com.actelion.research.spiritapp.spirit.ui.biosample.edit.EditBiosampleDlg;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.Status;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.DateTextField;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.LongTaskDlg;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class SetLivingStatusDlg extends JSpiritEscapeDialog {

	private final Study study;

	private BiosampleList animalList = new BiosampleList();
	private BiosampleComboBox replaceByComboBox = new BiosampleComboBox();
	private DateTextField dateField = new DateTextField(true);

	private JRadioButton unsetGroupRadioButton = new JRadioButton("Move to Reserve ");
	private JRadioButton statusAliveRadioButton = new JRadioButton("Change Status to: Alive");
	private JRadioButton statusNecropsiedRadioButton = new JRadioButton("Change Status to: Necropsied");
	private JRadioButton statusDeadRadioButton = new JRadioButton("Change Status to: Found Dead");
	private JRadioButton statusKilledRadioButton = new JRadioButton("Change Status to: Killed");
	private PhaseComboBox phaseComboBox;

	private JCustomTextField observationField = new JCustomTextField();

	public SetLivingStatusDlg(final Study myStudy, List<Biosample> selectedBiosamples) {
		super(UIUtils.getMainFrame(), "Set Living Status", SetLivingStatusDlg.class.getName());
		this.study = JPAUtil.reattach(myStudy);

		if(study==null) {
			JExceptionDialog.showError("Study cannot be null");
			return;
		}

		List<Biosample> biosamples = new ArrayList<>(study.getAttachedBiosamples());
		Collections.sort(biosamples);


		phaseComboBox = new PhaseComboBox(study.getPhases());
		phaseComboBox.setSelection(study.getPhase(JPAUtil.getCurrentDateFromDatabase()));
		dateField.setText(FormatterUtils.formatDate(new Date()));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.VERTICAL;

		//Find animals in the reserve
		List<Biosample> reserve = new ArrayList<>();
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

		//Action when setting action
		ButtonGroup group = new ButtonGroup();
		group.add(unsetGroupRadioButton);
		group.add(statusAliveRadioButton);
		group.add(statusNecropsiedRadioButton);
		group.add(statusKilledRadioButton);
		group.add(statusDeadRadioButton);
		unsetGroupRadioButton.addActionListener(new ActionListener() {
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
				dateField.setText(statusAliveRadioButton.isSelected()?"": FormatterUtils.formatDate(phaseComboBox.getSelection()==null || phaseComboBox.getSelection().getAbsoluteDate()==null? new Date(): phaseComboBox.getSelection().getAbsoluteDate()));
				Status s = getSelectedStatus();
				observationField.setText(s==null || s==Status.INLAB?"": s.getName()) ;

			}
		};
		statusAliveRadioButton.addActionListener(al);
		statusNecropsiedRadioButton.addActionListener(al);
		statusKilledRadioButton.addActionListener(al);
		statusDeadRadioButton.addActionListener(al);

		//Action when setting phase
		phaseComboBox.addTextChangeListener(e -> {
			dateField.setText(statusAliveRadioButton.isSelected()?"": FormatterUtils.formatDate(phaseComboBox.getSelection()==null || phaseComboBox.getSelection().getAbsoluteDate()==null? new Date(): phaseComboBox.getSelection().getAbsoluteDate()));
			observationField.setEnabled(phaseComboBox.getSelection()!=null);
		});

		JButton saveButton = new JIconButton(IconType.SAVE, "Set Status");
		saveButton.addActionListener(e-> {
			try {
				eventOk();
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, study);
				dispose();
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.NORTH, UIUtils.createTitleBox("New Status", UIUtils.createBox(
				new JScrollPane(animalList),
				null, null, null,
				UIUtils.createVerticalBox(
						UIUtils.createHorizontalBox(unsetGroupRadioButton, Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(statusAliveRadioButton, new JInfoLabel(" (set status and reset the previous container)"), Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(statusNecropsiedRadioButton, new JInfoLabel(" (set status, remove container and move samples from necropsy to this phase)"), Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(statusDeadRadioButton, new JInfoLabel(" (set status and remove container)") , Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(statusKilledRadioButton, new JInfoLabel(" (set status and remove container)"), Box.createHorizontalGlue()),
						Box.createVerticalGlue()
						))));

		contentPanel.add(BorderLayout.CENTER, UIUtils.createVerticalBox(
				UIUtils.createTitleBox("When", UIUtils.createVerticalBox(
						UIUtils.createHorizontalBox(new JLabel("Phase: "), phaseComboBox, Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(new JLabel("DateOfDeath: (optional)"), dateField, Box.createHorizontalGlue()))),
				UIUtils.createTitleBox("Observation", UIUtils.createVerticalBox(
						new JInfoLabel("The observation will be stored as an observation result at the given phase"),
						UIUtils.createHorizontalBox(new JLabel("Observation (optional): "), observationField, Box.createHorizontalGlue()))),
				UIUtils.createTitleBox("Replacement", UIUtils.createVerticalBox(
						new JInfoLabel("The group/container will be exchanged with this sample"),
						UIUtils.createHorizontalBox(new JLabel("To be replaced by: (optional)"), replaceByComboBox, Box.createHorizontalGlue())))));

		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), saveButton));


		if(selectedBiosamples!=null && selectedBiosamples.size()>0) animalList.setSelection(selectedBiosamples);

		setContentPane(contentPanel);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);

	}

	private void eventAnimalChanged() {
		List<Biosample> animals = animalList.getSelection();
		unsetGroupRadioButton.setEnabled(animals.size()>0);
		statusAliveRadioButton.setEnabled(animals.size()>0);
		statusDeadRadioButton.setEnabled(animals.size()>0);
		statusKilledRadioButton.setEnabled(animals.size()>0);
		statusNecropsiedRadioButton.setEnabled(animals.size()>0);
		for (Biosample a : animals) {
			unsetGroupRadioButton.setEnabled(unsetGroupRadioButton.isEnabled() && a.getInheritedGroup()!=null  && a.getStatus().isAvailable());
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
		if(animals.size()==0) throw new Exception("You must select a sample");


		Status status = getSelectedStatus();
		if(status==null && !unsetGroupRadioButton.isSelected()) {
			throw new Exception("You must select an action");
		}

		//Validate replacement (cannot be set if more than 1 sample is selected)
		Biosample replacement = replaceByComboBox.getSelection();
		if(replacement!=null && animals.size()>1) throw new Exception("You can only set a replacement if you select one sample");

		//Validate DOD
		Date dod = FormatterUtils.parseDate(dateField.getText());
		if(dateField.getText().length()>0 && dod==null) throw new Exception("You must give a valid date of death");

		//Validate Phase (mandatory for dead, killed, necropsy
		Phase phase = phaseComboBox.getSelection();
		if(phase!=null && !phase.getStudy().equals(study)) throw new Exception("the phase does not match the study");
		if(status!=null && study!=null && phase==null && (status==Status.DEAD || status==Status.KILLED || status==Status.NECROPSY)) {
			throw new Exception("You must enter a phase");
		}
		String observation = observationField.isEnabled()? observationField.getText(): null;


		////////////////////////////////////////////////////////////////////////
		//update the db
		final List<Result> resultsToSave = new ArrayList<>();
		final List<Biosample> biosamplesToSave = new ArrayList<>();
		final List<Biosample> samplesFromNecropsy = new ArrayList<>();

		String elb = DAOResult.suggestElb(SpiritFrame.getUsername());
		for(Biosample animal: animals) {
			if(status!=null) {
				//update the status
				animal.setStatus(status, phase);
			}

			if (status!=null && !status.isAvailable() && animal.getMetadataValue(Biotype.DATEOFDEATH) != null) {
				//set the dod if possible
				animal.setContainer(null);
				if (dod != null) {
					animal.setMetadataValue(Biotype.DATEOFDEATH, FormatterUtils.formatDate(dod));
				} else {
					animal.setMetadataValue(Biotype.DATEOFDEATH, null);
				}
			} else if(status==null || status.isAvailable()) {
				//reset the container if possible
				List<Biosample> history = DAORevision.getHistory(animal);
				for (Biosample h : history) {
					if(h.getContainer()!=null && h.getStatus()==null || h.getStatus().isAvailable()) {
						animal.setContainer(h.getContainer());
						break;
					}
				}
			}
			biosamplesToSave.add(animal);


			//Replacement Animal, only valid if one animal was selected
			if (replacement != null) {

				replacement.setAttached(animal.getInheritedGroup().getStudy(), animal.getInheritedGroup(), animal.getInheritedSubGroup());

				// Set the comments of the replacement and the animal
				String comments1 = "Replaced by " + replacement.getSampleId();
				String comments2 = "Replacing " + animal.getSampleId();
				replacement.setComments((replacement.getComments() == null || replacement.getComments().length() == 0 ? "" : replacement.getComments() + " ") + comments2);
				animal.setComments((animal.getComments() == null || animal.getComments().length() == 0 ? "" : animal.getComments() + " ") + comments1);

				// Exchange the container of the replacement and the animal
				replacement.setContainer(animal.getContainer());
				biosamplesToSave.add(replacement);
			}


			//Find necropsied samples to be moved to the given phase
			if(phase!=null) {
				for(Biosample b: animal.getHierarchy(HierarchyMode.ATTACHED_SAMPLES)) {
					if(b.getAttachedSampling()==null) continue;
					if(b.getAttachedSampling().getNamedSampling()==null) continue;
					if(!b.getAttachedSampling().getNamedSampling().isNecropsy()) continue;

					samplesFromNecropsy.add(b);
				}
			}

			//Add the observation Result
			if(phase!=null && observation!=null && observation.length()>0) {
				Test t = DAOTest.getTest(DAOTest.OBSERVATION_TESTNAME);
				if(t==null) throw new Exception("The test "+DAOTest.OBSERVATION_TESTNAME+" does not exist");
				Result r = new Result();
				r.setElb(elb);
				r.setTest(t);
				r.setBiosample(animal);
				r.setPhase(phase);
				r.setFirstOutputValue(observation);
				resultsToSave.add(r);
			}

			if(unsetGroupRadioButton.isSelected()) {
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
					JOptionPane.PLAIN_MESSAGE, null, new String[] {"Reassign", "Cancel"},
					"Reassign");
			if(res==0) {
				for (Biosample b : samplesFromNecropsy) {
					b.setInheritedPhase(phase);
					biosamplesToSave.add(b);
				}
			} else {
				return;
			}

		}



		new LongTaskDlg("Saving") {
			@Override
			public void longTask() throws Exception {
				//Persist biosamples
				DAOBiosample.persistBiosamples(biosamplesToSave, user);

				//Persist results
				if(resultsToSave.size()>0) {
					DAOResult.persistResults(resultsToSave, user);
				}

				//Fire events
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamplesToSave);
				if(resultsToSave.size()>0) {
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_ADDED, Result.class, resultsToSave);
				}

			}
		};

		if(phase!=null && status!=null && !status.isAvailable() && study.isSynchronizeSamples()) {
			int res = JOptionPane.showConfirmDialog(this, "The status is now updated to "+status+". Do you want to add some sampling?", "Add Sampling?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null);
			if(res==JOptionPane.YES_OPTION) {
				try {
					CreateChildrenDlg dlg = new CreateChildrenDlg(animals, phase);
					dlg.setVisible(true);
					List<Biosample> children = dlg.getChildren();
					if(children!=null) {
						EditBiosampleDlg dlg2 = EditBiosampleDlg.createDialogForEditInTransactionMode(children);
						dlg2.setVisible(true);
					}
				} catch (Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		}


	}

}
