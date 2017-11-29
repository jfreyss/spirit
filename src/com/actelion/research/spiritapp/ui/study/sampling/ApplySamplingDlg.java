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

package com.actelion.research.spiritapp.ui.study.sampling;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.ui.biosample.BiosampleList;
import com.actelion.research.spiritapp.ui.biosample.edit.EditBiosampleDlg;
import com.actelion.research.spiritapp.ui.location.ContainerTypeComboBox;
import com.actelion.research.spiritapp.ui.study.PhaseComboBox;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.helper.BiosampleCreationHelper;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 * Class used to apply a sampling for a non-synchronized study
 *
 * @author freyssj
 *
 */
public class ApplySamplingDlg extends JEscapeDialog {

	private Study study;
	private List<Biosample> biosamples = null;


	private final JGenericComboBox<NamedSampling> samplingComboBox = new JGenericComboBox<>();
	private final ContainerTypeComboBox containerTypeComboBox = new ContainerTypeComboBox();
	private final PhaseComboBox phaseComboBox = new PhaseComboBox();
	private final BiosampleList biosampleList = new BiosampleList();
	private final JCheckBox showDeadCheckBox = new JCheckBox("Show dead/non available");

	public ApplySamplingDlg(Study study) {
		super(UIUtils.getMainFrame(), "Apply Sampling");
		if(study==null) {
			JExceptionDialog.showError("Study cannot be null");
			return;
		}


		study = JPAUtil.reattach(study);
		this.study = study;

		biosampleList.setBiosamples(study.getParticipantsSorted());

		phaseComboBox.setTextWhenEmpty("All Phases...");

		JPanel filterPanel = UIUtils.createTable(
				new JLabel("Sampling: "), samplingComboBox,
				new JLabel("Container: "), containerTypeComboBox,
				new JLabel("Phase: "), phaseComboBox,
				null, showDeadCheckBox);


		samplingComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateFilters();
			}
		});
		showDeadCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateFilters();
			}
		});


		JButton okButton = new JIconButton(IconType.NEW, "Next");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createTemplate();
			}
		});


		//ContentPane
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.CENTER, UIUtils.createTitleBox("Add sampling for:",
				UIUtils.createVerticalBox(
						filterPanel,
						new JLabel("Select one or more samples:"),
						new JScrollPane(biosampleList))));
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));
		getRootPane().setDefaultButton(okButton);


		updateFilters();


		setContentPane(contentPanel);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);


	}


	private int push = 0;
	private void updateFilters() {
		if(push>0) return;
		try {
			push++;

			//Filter possible sampling types
			Set<NamedSampling> samplings = new LinkedHashSet<NamedSampling>();
			for(StudyAction ns: study.getStudyActions()) {
				if(ns.getNamedSampling1()!=null) samplings.add(ns.getNamedSampling1());
				if(ns.getNamedSampling2()!=null) samplings.add(ns.getNamedSampling2());
			}


			samplingComboBox.setValues(samplings, "All Samplings...");
			if(samplings.size()==1) samplingComboBox.setSelection(samplings.iterator().next());

			// Filter containers
			Set<ContainerType> containerTypes = new TreeSet<ContainerType>();
			for(NamedSampling ns: samplings) {
				if(samplingComboBox.getSelection()!=null && !samplingComboBox.getSelection().equals(ns)) continue;
				for(Sampling s: ns.getAllSamplings()) {
					if(s.getContainerType()!=null) containerTypes.add(s.getContainerType());
				}
			}
			containerTypeComboBox.setValues(containerTypes);


			//Filter phases
			if(phaseComboBox!=null) {
				Set<Phase> phases = new TreeSet<Phase>();
				for(StudyAction action : study.getStudyActions()) {
					if(action.getNamedSampling1()==null && action.getNamedSampling2()==null) continue;
					if(samplingComboBox.getSelection()!=null && !samplingComboBox.getSelection().equals(action.getNamedSampling1()) && !samplingComboBox.getSelection().equals(action.getNamedSampling2())) continue;
					phases.add(action.getPhase());
				}
				phaseComboBox.setValues(phases);
			}

			//Filter Animals
			List<Biosample> biosamples = new ArrayList<Biosample>();
			for(Biosample b : study.getParticipantsSorted()) {
				if(!showDeadCheckBox.isSelected() && ((phaseComboBox.getSelection()!=null && b.isDeadAt(phaseComboBox.getSelection())) || (phaseComboBox.getSelection()==null && !b.getStatus().isAvailable()))) continue;
				biosamples.add(b);
			}
			List<Biosample> sel = biosampleList.getSelection();
			biosampleList.setBiosamples(biosamples);
			biosampleList.setSelection(sel);



		} finally {
			push--;
		}


	}


	public void createTemplate() {
		try {

			//Get the filters
			Phase selectedPhase = phaseComboBox.getSelection();
			NamedSampling namedSampling = samplingComboBox.getSelection();
			ContainerType selectedContainer = containerTypeComboBox.getSelection();

			//Some test
			if(namedSampling!=null && namedSampling.getAllSamplings().size()==0) {
				throw new Exception("You should define the samples you want in the study design because the "+namedSampling+"'s template is empty");
			}


			List<Phase> selectedPhases = selectedPhase==null? null: Collections.singletonList(selectedPhase);

			//Generate samples
			biosamples = null;
			List<Biosample> res = BiosampleCreationHelper.processTemplateInStudy(study, namedSampling, selectedPhases, selectedContainer, biosampleList.getSelection().size()==0? null: biosampleList.getSelection());


			biosamples = res;
			if(biosamples==null || biosamples.size()==0) throw new Exception("There are no samples to be created, matching those criterias");

			//Open an Edit Dlg transactionLess (we are already in a transaction)
			EditBiosampleDlg editDlg =  EditBiosampleDlg.createDialogForEditInTransactionMode(biosamples);
			editDlg.setVisible(true);

			dispose();
		} catch(Exception e) {
			biosamples = null;
			JExceptionDialog.showError(e);
		}
	}


	public List<Biosample> getBiosamples() {
		return biosamples;
	}


}
