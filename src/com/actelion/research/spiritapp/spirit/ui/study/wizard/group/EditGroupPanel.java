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

package com.actelion.research.spiritapp.spirit.ui.study.wizard.group;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.actelion.research.spiritapp.spirit.ui.study.GroupComboBox;
import com.actelion.research.spiritapp.spirit.ui.study.GroupLabel;
import com.actelion.research.spiritapp.spirit.ui.study.PhaseComboBox;
import com.actelion.research.spiritapp.spirit.ui.study.sampling.SamplingDlg;
import com.actelion.research.spiritapp.spirit.ui.study.sampling.SamplingLabel;
import com.actelion.research.spiritapp.spirit.ui.util.component.JColorChooserButton;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class EditGroupPanel extends JPanel {

	private final StudyGroupDlg dlg;
	private final Study study;
	private Group group;


	private final JPanel groupPanel;
	private final JCustomTextField shortField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 4);
	private final JCustomTextField nameField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 14);
	private final GroupLabel groupLabel = new GroupLabel();
	private final JColorChooserButton colorChooser = new JColorChooserButton(true);
	private final JGenericComboBox<String> model1ComboBox = new JGenericComboBox<>(new String[] {Group.DISEASE_NAIVE, Group.DISEASE_SHAM, Group.DISEASE_DISEASED}, true);
	private final JGenericComboBox<String> model2ComboBox = new JGenericComboBox<>(new String[] {Group.TREATMENT_NONTREATED, Group.TREATMENT_VEHICLE, Group.TREATMENT_COMPOUND}, true);

	private final JRadioButton splitRadioButton = new JRadioButton("Normal group assignement");
	private final PhaseComboBox splitPhaseComboBox;
	private final JGenericComboBox<Group> splitGroupComboBox;

	private final JRadioButton divideRadioButton = new JRadioButton("The initial samples are divided into subsamples (ex: animal -> epithelial cells)");
	private final PhaseComboBox dividePhaseComboBox;
	private final JGenericComboBox<Group> divideGroupComboBox;
	private final SamplingLabel divideSampleLabel = new SamplingLabel();
	private final JIconButton divideSampleButton = new JIconButton(IconType.EDIT);


	private final JPanel subgroupPanel = new JPanel();
	private final List<JSpinner> subGroupSizeSpinners = new ArrayList<>();

	private int push = 0;

	private final JButton addSubgroupButton = new JButton("Add subgroup");

	private int formerSubGroupCount;

	public EditGroupPanel(final StudyGroupDlg dlg) {
		this.dlg = dlg;
		this.study = dlg.getStudy();

		assert study!=null;

		//addGroupButton
		addSubgroupButton.addActionListener(e-> {
			try {
				updateModel();
				group.addSubgroup();
				updateSubGroupPanel();
				dlg.refreshStudy();
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});

		shortField.setMaxChars(4);

		//init phase/group comboboxes
		splitPhaseComboBox = new PhaseComboBox(study.getPhases());
		splitGroupComboBox = new GroupComboBox(dlg.getGroups());

		dividePhaseComboBox = new PhaseComboBox(study.getPhases(), "Phase");
		divideGroupComboBox = new GroupComboBox(dlg.getGroups());

		//init radio buttons
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(splitRadioButton);
		buttonGroup.add(divideRadioButton);
		splitRadioButton.setFont(FastFont.BOLD);
		divideRadioButton.setFont(FastFont.BOLD);

		ActionListener al = e-> refreshGroupAssignment();

		//		initialRadioButton.addActionListener(al);
		splitRadioButton.addActionListener(al);
		divideRadioButton.addActionListener(al);

		divideSampleButton.setOpaque(false);
		divideSampleButton.addActionListener(e-> {
			Sampling sampling = group.getDividingSampling();
			if(sampling==null) sampling = new Sampling();
			SamplingDlg dlg2 = new SamplingDlg(null, study, sampling, false);
			if(dlg2.isSuccess()) {
				divideSampleLabel.setSampling(sampling);
				repaint();
			}
		});




		//CenterPanel
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(1, 1, 1, 1);


		model1ComboBox.addActionListener(e->refreshLabel());

		//namePanel
		JPanel namePanel = UIUtils.createTable(3,
				new JLabel("Abbreviation: "), shortField, new JInfoLabel("required (Ex: 1, 1A); hidden for user in  'blind-all'"),
				new JLabel("Name: "), nameField, new JInfoLabel("hidden for users in 'blind-details'"),
				new JLabel("Color: "), colorChooser, new JInfoLabel("background, hidden for users in 'blind-all'")
				//				new JLabel("Disease Model: "), model1ComboBox, null,
				//				new JLabel("Treatment Model: "), model2ComboBox, null
				);

		//groupAssignementPanel
		JPanel groupAssignementPanel = UIUtils.createVerticalBox(
				UIUtils.createHorizontalBox(splitRadioButton, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(Box.createHorizontalStrut(20), UIUtils.createTable(3,
						new JLabel("When (phase): "), splitPhaseComboBox, new JInfoLabel("optional, only for the auto-randomization or for group-splitting"),
						new JLabel("From (group): "), splitGroupComboBox, new JInfoLabel("optional, if the group is splitted"))),
				Box.createVerticalStrut(5),
				UIUtils.createHorizontalBox(divideRadioButton, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(Box.createHorizontalStrut(20), UIUtils.createTable(3,
						new JLabel("From (group): "), divideGroupComboBox, new JInfoLabel("required"),
						new JLabel("When (phase): "), dividePhaseComboBox, new JInfoLabel("required"),
						new JLabel("Sample: "),UIUtils.createBox(divideSampleLabel, null, null, divideSampleButton, null), new JInfoLabel("required")), Box.createHorizontalGlue()),
				Box.createVerticalStrut(5));

		groupPanel = UIUtils.createBox(
				UIUtils.createTitleBox("Group Size", UIUtils.createBox(
						new JScrollPane(subgroupPanel),
						new JInfoLabel("<html>Subgroups are used to represents samples, which should be analyzed together  (ex. stratification) <br>"
								+ "<i>(optional)</i></html>"))),
				UIUtils.createVerticalBox(
						UIUtils.createTitleBox("Group Definition", UIUtils.createBox(namePanel, UIUtils.createBox(BorderFactory.createEtchedBorder(), groupLabel))),
						UIUtils.createTitleBox("Group Assignment", groupAssignementPanel)
						));


		nameField.setMaxChars(25);

		shortField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {refreshLabel();}
		});
		nameField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {refreshLabel();}
		});


		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, groupPanel);

	}

	public void setGroup(Group group) {
		this.group = group;

		groupPanel.setVisible(group!=null);
		if(group==null) return;

		formerSubGroupCount = group.getNSubgroups();

		//Initialize View
		shortField.setText(group.getShortName());
		nameField.setText(group.getNameWithoutShortName());
		colorChooser.setColorRgb(group.getColorRgb());
		model1ComboBox.setSelection(group.getDiseaseModel());
		model2ComboBox.setSelection(group.getTreatmentModel());


		if(group.getDividingSampling()==null) {
			splitRadioButton.setSelected(true);
			splitPhaseComboBox.setSelection(group.getFromPhase());
			splitGroupComboBox.setSelection(group.getFromGroup());
		} else {
			divideRadioButton.setSelected(true);
			dividePhaseComboBox.setSelection(group.getFromPhase());
			divideGroupComboBox.setSelection(group.getFromGroup());
			divideSampleLabel.setSampling(group.getDividingSampling());
		}



		//Refresh
		refreshGroupAssignment();
		updateSubGroupPanel();
		refreshLabel();

	}

	private void refreshGroupAssignment() {
		splitPhaseComboBox.setEnabled(splitRadioButton.isSelected());
		splitGroupComboBox.setEnabled(splitRadioButton.isSelected());

		divideSampleButton.setEnabled(divideRadioButton.isSelected());
		dividePhaseComboBox.setEnabled(divideRadioButton.isSelected());
		divideGroupComboBox.setEnabled(divideRadioButton.isSelected());


	}
	private void updateSubGroupPanel() {
		if(push>0) return;
		push++;

		int nSubGroups = group.getNSubgroups();
		if(nSubGroups<1) nSubGroups = 1;
		try {
			subgroupPanel.setLayout(new GridLayout());
			subgroupPanel.removeAll();
			List<JComponent> components = new ArrayList<>();

			subGroupSizeSpinners.clear();
			for (int subgroup = 0; subgroup < nSubGroups; subgroup++) {
				int n;
				n = group.getSubgroupSize(subgroup);

				JSpinner cb = new JSpinner(new SpinnerNumberModel(0, 0, null, 1));
				cb.setValue(n);
				cb.setPreferredSize(new Dimension(55, 24));
				subGroupSizeSpinners.add(cb);

				JButton deleteSubgroupButton = new JButton("Delete");
				deleteSubgroupButton.setEnabled(nSubGroups>0 && study.getTopAttachedBiosamples(group, subgroup).size()==0);
				final int index = subgroup;
				deleteSubgroupButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							updateModel();
							group.removeSubgroup(index);
							updateSubGroupPanel();
							dlg.refreshStudy();
						} catch(Exception ex) {
							JExceptionDialog.showError(ex);
						}

					}
				});

				JButton moveUpButton = new JButton("Move Up");
				moveUpButton.setVisible(subgroup>0);
				moveUpButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							updateModel();
							group.moveUp(index);
							updateSubGroupPanel();
							dlg.refreshStudy();
						} catch(Exception ex) {
							JExceptionDialog.showError(ex);
						}

					}
				});

				components.add(new JLabel("SubGroup "+(subgroup+1)+": "));
				components.add(UIUtils.createHorizontalBox(Box.createHorizontalStrut(5), new JLabel("N="), subGroupSizeSpinners.get(subgroup), deleteSubgroupButton, moveUpButton));
				components.add(new JInfoLabel(study.getTopAttachedBiosamples(group, subgroup).size()+" attached samples"));

			}
			components.add(null);
			components.add(addSubgroupButton);
			components.add(null);

			subgroupPanel.add(UIUtils.createTable(3, components));
			subgroupPanel.validate();
			subgroupPanel.repaint();

			//Copy the actions of the former subgroup to the new created subgroups
			if(formerSubGroupCount>=1 && formerSubGroupCount<nSubGroups ) {
				for(Phase phase: study.getPhases()) {
					StudyAction a = study.getStudyAction(group, formerSubGroupCount-1, phase);
					if(a==null) continue;
					for (int i = formerSubGroupCount; i < nSubGroups; i++) {

						StudyAction a2 = study.getOrCreateStudyAction(group, i, phase);
						a2.setMeasureFood(a.isMeasureFood());
						a2.setMeasureWater(a.isMeasureWater());
						a2.setMeasureWeight(a.isMeasureWeight());
						a2.setMeasurementString(a.getMeasurementString());
						a2.setLabel(a.getLabel());
						a2.setNamedSampling1(a.getNamedSampling1());
						a2.setNamedSampling2(a.getNamedSampling2());
						a2.setNamedTreatment(a.getNamedTreatment());
					}
				}
			}

		} finally {
			push--;
		}
	}

	private void refreshLabel() {
		Group group = new Group();
		group.setColorRgb(colorChooser.getColorRgb());
		group.setName(shortField.getText(), nameField.getText());
		groupLabel.setOpaque(true);
		groupLabel.setGroup(group);
		groupLabel.getParent().validate();
		groupLabel.repaint();

		if(model1ComboBox.getSelection()!=null && !model1ComboBox.getSelection().equals("Naive")) {
			model2ComboBox.setEnabled(true);
		} else {
			model2ComboBox.setSelection(null);
			model2ComboBox.setEnabled(false);
		}

	}

	public Group getGroup() {
		return group;
	}

	public void updateModel() throws Exception {
		if(group==null) return;
		//Validation
		String shortGroup = shortField.getText().trim();

		group.setName(shortGroup, nameField.getText());
		group.setStudy(study);
		group.setColorRgb(colorChooser.getColorRgb());
		group.setDiseaseModel(model1ComboBox.getSelection());
		group.setTreatmentModel(model2ComboBox.getSelection());

		//subgroup sizes
		int total = 0;
		int[] subGroupSizes = null;
		subGroupSizes = new int[group.getNSubgroups()];
		for (int i = 0; i < subGroupSizes.length; i++) {
			Integer n = (Integer) subGroupSizeSpinners.get(i).getValue();
			if(n==null) n = 0;
			total += n;
			subGroupSizes[i] = n;
		}

		if(splitRadioButton.isSelected()) {
			if(splitPhaseComboBox.getSelection()==null && splitGroupComboBox.getSelection()!=null) throw new Exception("The phase is required in case of a group splitting");
			if(group.equals(splitGroupComboBox.getSelection())) throw new Exception("The from (group) must be different");
			group.setFromPhase(splitPhaseComboBox.getSelection());
			group.setFromGroup(splitGroupComboBox.getSelection());
			group.setDividingSampling(null);
		} else if(divideRadioButton.isSelected()) {
			if(dividePhaseComboBox.getSelection()==null) throw new Exception("The phase is required");
			if(divideGroupComboBox.getSelection()==null) throw new Exception("The group is required");
			if(group.equals(divideGroupComboBox.getSelection())) throw new Exception("The group should be different");
			if(divideSampleLabel.getSampling()==null || divideSampleLabel.getSampling().getBiotype()==null) throw new Exception("The sample is required");
			if( total!=divideGroupComboBox.getSelection().getNAnimals()) throw new Exception("The total number of animals should be equals to: "+divideGroupComboBox.getSelection().getNAnimals());
			group.setFromPhase(dividePhaseComboBox.getSelection());
			group.setFromGroup(divideGroupComboBox.getSelection());
			group.setDividingSampling(divideSampleLabel.getSampling());

		} else {
			throw new Exception("No option selected???");
		}


		//Stratification?
		group.setSubgroupSizes(subGroupSizes);



		//Remove invalid actions
		for (StudyAction a : new ArrayList<StudyAction>(study.getStudyActions())) {
			if(a.getGroup()==null || a.getSubGroup()<0 || a.getSubGroup()>=a.getGroup().getNSubgroups()) {
				a.remove();
			}
		}

		//Update invalid subgroups
		boolean hasSampleMoved = false;
		for (Biosample b : study.getTopAttachedBiosamples(group)) {
			if(b.getInheritedSubGroup()<0 || b.getInheritedSubGroup()>=group.getNSubgroups()) {
				b.setAttached(study, group, 0);
				hasSampleMoved = true;
			}
		}

		if(hasSampleMoved) {
			JOptionPane.showMessageDialog(EditGroupPanel.this,
					(hasSampleMoved? "Some biosamples were moved to the subgroup'"+(group.getNSubgroups())+".\n":"")
					, "Warning", JOptionPane.WARNING_MESSAGE);
		}


		study.resetCache();
	}

}
