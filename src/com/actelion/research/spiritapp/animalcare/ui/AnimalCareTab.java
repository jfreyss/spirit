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

package com.actelion.research.spiritapp.animalcare.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.animalcare.ui.monitor.MonitoringOverviewDlg;
import com.actelion.research.spiritapp.animalcare.ui.randomize.RandomizationDlg;
import com.actelion.research.spiritapp.spirit.ui.IStudyTab;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.SpiritTab;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.print.PrintingDlg;
import com.actelion.research.spiritapp.spirit.ui.study.PhaseComboBox;
import com.actelion.research.spiritapp.spirit.ui.study.StudyActions;
import com.actelion.research.spiritapp.spirit.ui.study.StudyDetailPanel;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

/**
 * Customized tab for AnimalCare
 *
 * @author Joel Freyss
 *
 */
public class AnimalCareTab extends SpiritTab implements IStudyTab {

	private final JEditorPane studyEditorPane = new JEditorPane();
	private final JButton editInfosButton = new JIconButton(IconType.EDIT, "Edit Infos");
	private final JButton editDesignButton = new JIconButton(IconType.STUDY, "Edit Design");

	private final StudyDetailPanel studyDetailPanel = new StudyDetailPanel(JSplitPane.VERTICAL_SPLIT);

	private final JButton groupAssignButton;
	private final JButton cageButton = new JIconButton(IconType.PRINT, "", "Print cage labels");

	private final JButton weighingButton = new JBigButton(new StudyActions.Action_AnimalMonitoring(null));
	private final JButton markDeadButton = new JBigButton(new BiosampleActions.Action_SetLivingStatus((Study)null));

	private final JButton manageButton = new JBigButton(new StudyActions.Action_ManageSamples(null));
	private final JButton measurementButton = new JBigButton(new StudyActions.Action_MeasurementSamples(null));

	private final JButton reportButton = new JBigButton(new StudyActions.Action_Report(null));


	public AnimalCareTab(SpiritFrame frame) {
		super(frame, "AnimalCare", IconType.STUDY.getIcon());
		groupAssignButton = new JBigButton(new Action_GroupAssignmentSelecter());
		studyDetailPanel.showParticipants();

		StudyActions.attachPopup(studyDetailPanel);

		//Tooltip
		groupAssignButton.setToolTipText("Assign the groups through the Group Assignment Wizard");

		//Button Menu
		JPanel buttonPanel = UIUtils.createTitleBox("", UIUtils.createVerticalBox(
				UIUtils.createBox(groupAssignButton, null, null, null, cageButton),
				Box.createVerticalStrut(10),
				weighingButton,
				markDeadButton,
				Box.createVerticalStrut(10),
				manageButton,
				measurementButton,
				Box.createVerticalStrut(10),
				reportButton
				));

		JPanel westPanel = UIUtils.createBox(
				UIUtils.createBox(BorderFactory.createEtchedBorder(), new JScrollPane(studyEditorPane), null, UIUtils.createHorizontalBox(editInfosButton, editDesignButton, Box.createHorizontalGlue())),
				buttonPanel);

		editInfosButton.addActionListener(e-> {
			try {
				Study study = getStudy();
				if(!SpiritRights.canAdmin(study, SpiritFrame.getUser())) throw new Exception("You must be an admin to edit");
				new StudyActions.Action_EditInfos(study).actionPerformed(null);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});

		editDesignButton.addActionListener(e-> {
			try {
				Study study = getStudy();
				if(!SpiritRights.canAdmin(study, SpiritFrame.getUser())) throw new Exception("You must be an admin to edit");
				new StudyActions.Action_EditDesign(study).actionPerformed(null);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});

		westPanel.setPreferredSize(new Dimension(250, 300));
		setLayout(new BorderLayout());
		add(BorderLayout.WEST, westPanel);
		add(BorderLayout.CENTER, studyDetailPanel);

		cageButton.addActionListener(e-> {
			try {
				Study study = getStudy();
				if(study==null || !SpiritRights.canBlind(study, SpiritFrame.getUser())) throw new Exception("You must select a study");
				new PrintingDlg(study.getTopParticipants());
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});

		weighingButton.addActionListener(e-> {
			try {
				Study study = getStudy();
				if(study==null || !SpiritRights.canBlind(study, SpiritFrame.getUser())) throw new Exception("You must select a study");
				new MonitoringOverviewDlg(study);
			} catch (Exception ex) {
				JExceptionDialog.showError(AnimalCareTab.this, ex);
			}
		});

		manageButton.addActionListener(e-> {
			try {
				Study study = getStudy();
				if(study==null || !SpiritRights.canBlind(study, SpiritFrame.getUser())) throw new Exception("You must select a study");
				new StudyActions.Action_ManageSamples(study).actionPerformed(e);
			} catch (Exception ex) {
				JExceptionDialog.showError(AnimalCareTab.this, ex);
			}
		});

		measurementButton.addActionListener(e-> {
			try {
				Study study = getStudy();
				if(study==null || !SpiritRights.canBlind(study, SpiritFrame.getUser())) throw new Exception("You must select a study");
				new StudyActions.Action_MeasurementSamples(study).actionPerformed(e);
			} catch (Exception ex) {
				JExceptionDialog.showError(AnimalCareTab.this, ex);
			}
		});

		reportButton.addActionListener(e-> {
			try {
				Study study = getStudy();
				if(study==null || !SpiritRights.canBlind(study, SpiritFrame.getUser())) throw new Exception("You must select a study");
				new StudyActions.Action_Report(study).actionPerformed(e);
			} catch (Exception ex) {
				JExceptionDialog.showError(AnimalCareTab.this, ex);
			}
		});

		markDeadButton.addActionListener(e-> {
			try {
				Study study = getStudy();
				if(study==null || !SpiritRights.canBlind(study, SpiritFrame.getUser())) throw new Exception("You must select a study");
				new BiosampleActions.Action_SetLivingStatus(study).actionPerformed(e);
			} catch (Exception ex) {
				JExceptionDialog.showError(AnimalCareTab.this, ex);
			}
		});

	}



	public class Action_GroupAssignmentSelecter extends AbstractAction {

		public Action_GroupAssignmentSelecter() {
			super("Group Assignment");
			putValue(AbstractAction.SMALL_ICON, IconType.LINK.getIcon());
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Study study = getStudy();
			if(study==null || !SpiritRights.canBlind(study, SpiritFrame.getUser())) return;

			Phase phase;
			Set<Phase> phases = study.getPhasesWithGroupAssignments();
			PhaseComboBox phaseComboBox = new PhaseComboBox(phases);

			if(phases.size()==1) {
				phase = phases.iterator().next();
			} else {
				int res = JOptionPane.showOptionDialog(null, UIUtils.createVerticalBox(
						UIUtils.createHorizontalBox(new JLabel("Please select a phase for the group assignment: "), Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(new JLabel("Phase: "), phaseComboBox, Box.createHorizontalGlue())), "Group Assignment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);

				if(res!=JOptionPane.YES_OPTION || phaseComboBox.getSelection()==null) return;
				phase = phaseComboBox.getSelection();
			}

			new RandomizationDlg(phase);

		}
	}


	@Override
	public void setStudy(Study study) {
		studyDetailPanel.setStudy(study);
		onStudySelect();
	}


	@Override
	public Study getStudy() {
		return studyDetailPanel.getStudy();
	}


	@Override
	public void onTabSelect() {
	}

	@Override
	public void onStudySelect() {
		Study study = getFrame().getStudy();
		if(!SpiritRights.canBlind(study, SpiritFrame.getUser())) return;
		studyDetailPanel.setStudy(study);

		boolean canBlind = study!=null && SpiritRights.canBlind(study, SpiritFrame.getUser());
		boolean canRead = study!=null && SpiritRights.canExpert(study, SpiritFrame.getUser());

		int nAnimals = study==null? 0: study.getTopParticipants().size();

		groupAssignButton.setEnabled(canBlind && !SpiritRights.isBlindAll(study, SpiritFrame.getUser()));
		reportButton.setEnabled(canRead && nAnimals>0);
		markDeadButton.setEnabled(canBlind && nAnimals>0);
		weighingButton.setEnabled(canBlind && nAnimals>0);
		manageButton.setEnabled(canBlind && nAnimals>0);
		measurementButton.setEnabled(canBlind && nAnimals>0);
		cageButton.setEnabled(canBlind && nAnimals>0);

		studyEditorPane.setText(MiscUtils.convert2Html(study.getNotes()));
		editInfosButton.setEnabled(study!=null);
		editDesignButton.setEnabled(study!=null);
	}


	@Override
	public <T> void fireModelChanged(SpiritChangeType action, Class<T> what, Collection<T> details) {
		onStudySelect();
	}


}
