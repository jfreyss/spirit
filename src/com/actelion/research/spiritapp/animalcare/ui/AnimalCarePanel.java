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
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.animalcare.ui.monitor.MonitoringOverviewDlg;
import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.lf.StudyComboBox;
import com.actelion.research.spiritapp.spirit.ui.print.PrintingDlg;
import com.actelion.research.spiritapp.spirit.ui.study.StudyActions;
import com.actelion.research.spiritapp.spirit.ui.study.StudyDetailPanel;
import com.actelion.research.spiritapp.spirit.ui.study.StudyEditorPane;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class AnimalCarePanel extends JPanel {

	private final StudyEditorPane studyEditorPane = new StudyEditorPane(); 
	private final JButton editInfosButton = new JIconButton(IconType.EDIT, "Edit Infos");
	private final JButton editDesignButton = new JIconButton(IconType.STUDY, "Edit Design");
	
	private final StudyComboBox studyComboBox;
	private final StudyDetailPanel studyDetailPanel = new StudyDetailPanel(JSplitPane.VERTICAL_SPLIT, false);

	private final JButton groupAssignButton;	
	private final JButton cageButton = new JIconButton(IconType.PRINT, "", "Print cage labels ");
	
	private final JButton weighingButton = new JBigButton(new StudyActions.Action_AnimalMonitoring(null));
	private final JButton markDeadButton = new JBigButton(new BiosampleActions.Action_SetLivingStatus((Study)null));

	private final JButton manageButton = new JBigButton(new StudyActions.Action_ManageSamples(null));
	private final JButton extraSamplingButton = new JBigButton(new StudyActions.Action_ExtraSampling(null));
	private final JButton measurementButton = new JBigButton(new StudyActions.Action_MeasurementSamples(null));
		
	private final JButton reportButton = new JBigButton(new StudyActions.Action_Report(null));
	
	
	
	public AnimalCarePanel() {
		studyComboBox = new StudyComboBox(RightLevel.BLIND);
		studyComboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				eventStudyChanged();
			}
		});
		groupAssignButton = new JBigButton(new StudyActions.Action_GroupAssignmentSelecter(studyComboBox));	
		JPanel studyPanel = UIUtils.createTitleBox("", UIUtils.createHorizontalBox(studyComboBox, Box.createHorizontalGlue()));
		studyDetailPanel.showSpecimen();
		
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
					extraSamplingButton,
					measurementButton,
					Box.createVerticalStrut(10),
					reportButton
				));
		
		JPanel westPanel = UIUtils.createBox(
				UIUtils.createBox(BorderFactory.createEtchedBorder(), new JScrollPane(studyEditorPane), null, UIUtils.createHorizontalBox(editInfosButton, editDesignButton, Box.createHorizontalGlue())), 
				UIUtils.createBox(buttonPanel, studyPanel, null),
				null);

		editInfosButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
					if(!SpiritRights.canAdmin(study, Spirit.getUser())) throw new Exception("You must be an admin to edit");
					new StudyActions.Action_EditInfos(study).actionPerformed(null);
				} catch (Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});
		
		editDesignButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
					if(!SpiritRights.canAdmin(study, Spirit.getUser())) throw new Exception("You must be an admin to edit");
					new StudyActions.Action_EditDesign(study).actionPerformed(null);
				} catch (Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});
		
		westPanel.setPreferredSize(new Dimension(250, 300));
		setLayout(new BorderLayout());
		add(BorderLayout.WEST, westPanel);		
		add(BorderLayout.CENTER, studyDetailPanel);

		cageButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {				
				try {
					Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
					if(study==null || !SpiritRights.canBlind(study, Spirit.getUser())) throw new Exception("You must select a study");
					new PrintingDlg(study.getTopAttachedBiosamples());
				} catch (Exception ex) {
					JExceptionDialog.showError(ex);
				} 
				
			}
		});
		
		weighingButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
					if(study==null || !SpiritRights.canBlind(study, Spirit.getUser())) throw new Exception("You must select a study");
					new MonitoringOverviewDlg(study);
				} catch (Exception ex) {
					JExceptionDialog.showError(AnimalCarePanel.this, ex);
				}
			}
		});
		
		
		extraSamplingButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
					if(study==null || !SpiritRights.canBlind(study, Spirit.getUser())) throw new Exception("You must select a study");
					new StudyActions.Action_ExtraSampling(study).actionPerformed(e);
				} catch (Exception ex) {
					JExceptionDialog.showError(AnimalCarePanel.this, ex);
				}
			}
		});
		
		manageButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
					if(study==null || !SpiritRights.canBlind(study, Spirit.getUser())) throw new Exception("You must select a study");
					new StudyActions.Action_ManageSamples(study).actionPerformed(e);
				} catch (Exception ex) {
					JExceptionDialog.showError(AnimalCarePanel.this, ex);
				}
			}
		});
		
		measurementButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
					if(study==null || !SpiritRights.canBlind(study, Spirit.getUser())) throw new Exception("You must select a study");
					new StudyActions.Action_MeasurementSamples(study).actionPerformed(e);
				} catch (Exception ex) {
					JExceptionDialog.showError(AnimalCarePanel.this, ex);
				}				
			}
		});
		
		reportButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
					if(study==null || !SpiritRights.canBlind(study, Spirit.getUser())) throw new Exception("You must select a study");
					new StudyActions.Action_Report(study).actionPerformed(e);
				} catch (Exception ex) {
					JExceptionDialog.showError(AnimalCarePanel.this, ex);
				}
			}
		});
		
		markDeadButton.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
					if(study==null || !SpiritRights.canBlind(study, Spirit.getUser())) throw new Exception("You must select a study");
					new BiosampleActions.Action_SetLivingStatus(study).actionPerformed(e);
				} catch (Exception ex) {
					JExceptionDialog.showError(AnimalCarePanel.this, ex);
				}
			}			
		});

	}
	
	


	
	public void eventStudyChanged() {
			Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
			if(!SpiritRights.canBlind(study, Spirit.getUser())) return;
			studyDetailPanel.setStudy(study);			

			boolean canBlind = study!=null && SpiritRights.canBlind(study, Spirit.getUser());
			boolean canRead = study!=null && SpiritRights.canExpert(study, Spirit.getUser());
			
			int nAnimals = study==null? 0: study.getTopAttachedBiosamples().size();

			groupAssignButton.setEnabled(canBlind && !SpiritRights.isBlindAll(study, Spirit.getUser()));
			reportButton.setEnabled(canRead && nAnimals>0);
			markDeadButton.setEnabled(canBlind && nAnimals>0);
			extraSamplingButton.setEnabled(canBlind && nAnimals>0);
			weighingButton.setEnabled(canBlind && nAnimals>0);
			manageButton.setEnabled(canBlind && nAnimals>0);
			measurementButton.setEnabled(canBlind && nAnimals>0);
			cageButton.setEnabled(canBlind && nAnimals>0);
				
			studyEditorPane.setStudy(study);
			editInfosButton.setEnabled(study!=null);
			editDesignButton.setEnabled(study!=null);
	}
			
	
	
//	private void editNotes() throws Exception{
//		Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
//		if(study==null || !SpiritRights.canBlind(study, Spirit.getUser())) throw new Exception("You must select a study");
//				
//		JTextArea ta = new JTextArea(study.getNotes());
//		JScrollPane scrollPane = new JScrollPane(ta);		
//		scrollPane.setPreferredSize(new Dimension(350, 150));
//		
//		// pass the scrollpane to the joptionpane.				
//		int res = JOptionPane.showConfirmDialog(this, scrollPane, "Edit Notes of "+study, JOptionPane.OK_CANCEL_OPTION);
//		if(res!=JOptionPane.YES_OPTION) return;
//		try {
//			JPAUtil.pushEditableContext(Spirit.getUser());
//			study.setNotes(ta.getText());
//			DAOStudy.persistStudy(study, Spirit.askForAuthentication());
//		} finally {
//			JPAUtil.popEditableContext();
//			eventStudyChanged();
//		}
//		
//	}
	
	public void refresh() {
		studyComboBox.reload();
		eventStudyChanged();
	}
	
	public void setStudy(Study study) {
		studyComboBox.setText(study==null? null: study.getStudyId());
		eventStudyChanged();
	}
	
}
