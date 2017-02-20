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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.animalcare.ui.monitor.MonitoringOverviewDlg;
import com.actelion.research.spiritapp.animalcare.ui.randomize.RandomizationDlg;
import com.actelion.research.spiritapp.animalcare.ui.sampleweighing.SampleWeighingDlg;
import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritAction;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions.Action_SetLivingStatus;
import com.actelion.research.spiritapp.spirit.ui.lf.StudyComboBox;
import com.actelion.research.spiritapp.spirit.ui.lf.UserIdComboBox;
import com.actelion.research.spiritapp.spirit.ui.study.depictor.StudyDepictor;
import com.actelion.research.spiritapp.spirit.ui.study.edit.AttachAnimalsManuallyDlg;
import com.actelion.research.spiritapp.spirit.ui.study.edit.StudyDiscardDlg;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.StudyInfoDlg;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.StudyWizardDlg;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;

public class StudyActions {

	
	public static class Action_New extends AbstractAction {
		public Action_New() {
			super("New Study");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('e'));
			putValue(AbstractAction.SMALL_ICON, IconType.STUDY.getIcon());
			setEnabled(SpiritRights.canAdmin((Study)null, Spirit.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			Study study = new Study();
			study.setState(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_STATE_DEFAULT));
			if(Spirit.getUser()!=null) {
				study.setAdminUsers(Spirit.getUser().getUsername());
			}
			if(Spirit.getUser().getMainGroup()!=null) study.setEmployeeGroups(Collections.singletonList(Spirit.getUser().getMainGroup()));
			StudyWizardDlg.editStudy(study);
			SpiritContextListener.setStudy(study);
		}
	}	
	
	public static class Action_EditInfos extends AbstractAction {
		private final Study study;
		public Action_EditInfos(Study study) {
			super("Study Infos");
			this.study = study;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			putValue(AbstractAction.SMALL_ICON, IconType.EDIT.getIcon());
			setEnabled(SpiritRights.canAdmin(study, Spirit.getUser()));
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			new StudyInfoDlg(study, true);
		}
	}	

	public static class Action_EditDesign extends AbstractAction {
		private final Study study;
		public Action_EditDesign(Study study) {
			super("Study Design");
			this.study = study;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			putValue(AbstractAction.SMALL_ICON, IconType.STUDY.getIcon());
			setEnabled(SpiritRights.canAdmin(study, Spirit.getUser()));
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			StudyWizardDlg.editStudy(study);
		}
	}	
	
	public static class Action_Promote extends AbstractAction {
		private final Study study;
		public Action_Promote(Study study) {
			super("Change Workflow Status");
			this.study = study;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('s'));
			putValue(AbstractAction.SMALL_ICON, IconType.STATUS.getIcon());
			setEnabled(SpiritRights.canPromote(study, Spirit.getUser()));
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			new PromoteDlg(study);
		}
	}	
	
	public static class Action_Duplicate extends AbstractAction {
		private final Study study;
		public Action_Duplicate(Study study) {
			super("Duplicate Study");
			this.study = study;
			putValue(AbstractAction.SMALL_ICON, IconType.DUPLICATE.getIcon());
			setEnabled(SpiritRights.canRead(study, Spirit.getUser()));
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			StudyWizardDlg.duplicateStudy(study);
		}
	}	
	
	public static class Action_Delete extends AbstractAction {
		private final Study study;

		public Action_Delete(Study study) {
			super("Delete (owner only)");
			this.study = study;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			putValue(Action.SMALL_ICON, IconType.DELETE.getIcon());

			setEnabled(SpiritRights.canDelete(study, Spirit.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {			
			try {
				StudyDiscardDlg.createDialogForDelete(study);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}
	
	public static class Action_AssignTo extends AbstractAction {
		private Study study;
		public Action_AssignTo(Study study) {
			super("Change Ownership (admin)");
			this.study = study;
			
			putValue(AbstractAction.SMALL_ICON, IconType.ADMIN.getIcon());
			putValue(AbstractAction.MNEMONIC_KEY, (int)'o');
			setEnabled(SpiritRights.isSuperAdmin(Spirit.getUser()));
		}
		
		@Override
		public void actionPerformed(ActionEvent ev) {

			UserIdComboBox userIdComboBox = new UserIdComboBox();
			int res = JOptionPane.showOptionDialog(UIUtils.getMainFrame(), 
					UIUtils.createVerticalBox( 
							new JLabel("To whom would you like to assign the study " + study.getStudyId() + "?"),
							UIUtils.createHorizontalBox(userIdComboBox, Box.createHorizontalGlue())),												
					"Change ownership",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					null,
					null);
			if(res!=JOptionPane.YES_OPTION) return;
			
			String name = userIdComboBox.getText();
			if(name==null) return;
			JPAUtil.pushEditableContext(Spirit.getUser());
			try {
				study = JPAUtil.reattach(study);
				SpiritUser admin = Spirit.askForAuthentication();
				SpiritUser u = DAOSpiritUser.loadUser(name);
				if(u==null) throw new Exception(name + " is an invalid user");
				res = JOptionPane.showConfirmDialog(null, "Are you sure to update the owner to " + u.getUsername()+"?", "Change Ownership", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(res!=JOptionPane.YES_OPTION) return;
				
				DAOStudy.changeOwnership(study, u, admin);
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, study);
			} catch (Exception e) {
				JExceptionDialog.showError(e);
			} finally {
				JPAUtil.popEditableContext();
			}
			
		}
	}
	
	public static class Action_ManualAssignment extends AbstractAction {
		private final Study study;
		public Action_ManualAssignment(Study study) {
			super("Manual Assignment");
			this.study = study;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('a'));			
			putValue(AbstractAction.SMALL_ICON, IconType.LINK.getIcon());			
			setEnabled(SpiritRights.canExpert(study, Spirit.getUser()));
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			new AttachAnimalsManuallyDlg(study);
		}
	}
	
	public static class Action_GroupAssignmentSelecter extends AbstractAction {
		private StudyComboBox comboBox;
		
		public Action_GroupAssignmentSelecter(StudyComboBox comboBox) {			
			super("Group Assignment");
			putValue(AbstractAction.SMALL_ICON, IconType.LINK.getIcon());
			this.comboBox = comboBox;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Study study = DAOStudy.getStudyByStudyId(comboBox.getText());
			System.out.println("StudyActions.Action_GroupAssignmentSelecter.actionPerformed() "+study+" / "+comboBox.getText());			
			if(study==null || !SpiritRights.canBlind(study, Spirit.getUser())) return;
			
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
	
	
	
	public static class Action_GroupAssignment extends AbstractAction {
		private Phase phase;
		
		public Action_GroupAssignment(Phase phase) {
			super(phase.toString());
			this.phase = phase;
			putValue(AbstractAction.SMALL_ICON, IconType.LINK.getIcon());			
			setEnabled(phase.getStudy()!=null && phase.getStudy().getPhasesWithGroupAssignments().size()>0 && SpiritRights.canExpert(phase.getStudy(), Spirit.getUser()));
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Study study = phase.getStudy();
			
			if(study==null || !SpiritRights.canBlind(study, Spirit.getUser())) return;
			
			if(phase==null) {
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
			}
			
			new RandomizationDlg(phase);
			
		}
	}
	
	
	public static class Action_AnimalMonitoring extends AbstractAction {
		private final Study study;
		public Action_AnimalMonitoring(Study study) {
			super("Live Monitoring");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('m'));			
			putValue(AbstractAction.SMALL_ICON, IconType.FOOD.getIcon());			
			
			this.study = study;
			setEnabled(study!=null && study.getPhases().size()>0 && SpiritRights.canBlind(study, Spirit.getUser()) && study.getAttachedBiosamples().size()>0 );
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if(study==null) return;
				new MonitoringOverviewDlg(study);
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}
	
	/*
	public static class Action_ExtraSampling extends AbstractAction {
		private final Study study;
		public Action_ExtraSampling(Study study) {
			super(study==null?"Sampling": study.isSynchronizeSamples()? "Add Exceptional Sampling": "Apply Sampling");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('s'));			
			
			this.study = study;
			putValue(AbstractAction.SMALL_ICON, IconType.BIOSAMPLE.getIcon());
			putValue(AbstractAction.SHORT_DESCRIPTION, "Modify the design to add an extra sampling for one sample or a group of sample");			
			setEnabled(SpiritRights.canBlind(study, Spirit.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if(study==null) return;
				if(study.isSynchronizeSamples()) {
					new AddExceptionalSamplingDlg(study);					
				} else {
					new ApplySamplingDlg(study);					
				}
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}
	*/
	public static class Action_ManageSamples extends AbstractAction {
		private final Study study;
		public Action_ManageSamples(Study study) {
			super("Manage / Print Samples");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('p'));			
			
			this.study = study;
			putValue(AbstractAction.SMALL_ICON, IconType.PRINT.getIcon());
			setEnabled(study!=null && (SpiritRights.canExpert(study, Spirit.getUser()) || SpiritRights.canBlind(study, Spirit.getUser())) && study.getAttachedBiosamples().size()>0);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if(study==null) return;
				new ManageSamplesDlg(study);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}
	
	public static class Action_MeasurementSamples extends AbstractAction {
		private final Study study;
		public Action_MeasurementSamples(Study study) {
			super("Sample Measurement");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('m'));			
			
			this.study = study;
			putValue(AbstractAction.SMALL_ICON, IconType.BALANCE.getIcon());
			setEnabled(study!=null && (study.getPhases().size()>0 && SpiritRights.canBlind(study, Spirit.getUser())) && study.getAttachedBiosamples().size()>0);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				if(study==null) return;
				new SampleWeighingDlg(study);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}

	
	public static class Action_Report extends AbstractAction {
		private Study study;
		public Action_Report(Study study) {
			super("Reports");
			this.study = study;
			putValue(Action.SMALL_ICON, IconType.EXCEL.getIcon());
			setEnabled(study!=null && SpiritRights.canExpert(study, Spirit.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(study==null) return;
			new ReportDlg(study);
		}
	}
	
	public static class Action_History extends AbstractAction {
		private final Study study;
		public Action_History(Study study) {
			super("View Change History");
			this.study = study;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('h'));
			putValue(Action.SMALL_ICON, IconType.HISTORY.getIcon());
			setEnabled(SpiritRights.canExpert(study, Spirit.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				List<Revision> revisions = DAORevision.getRevisions(study);
				new StudyHistoryDlg(revisions);
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}
	
	
	
	/**
	 * Attach "new Study" popup to a component
	 * @param comp
	 */
	public static void attachPopup(final JComponent comp) {
		comp.addMouseListener(new PopupAdapter() {
			@Override
			protected void showPopup(MouseEvent e) {
				StudyActions.createPopup((Study)null).show(comp, e.getX(), e.getY());				
			}
		});
	}	
	

	/**
	 * Attach "edit Study" popup to a component
	 * @param valueListPanel
	 */
	public static void attachPopup(final StudyTable table) {
		table.addMouseListener(new PopupAdapter(table) {
			@Override
			protected void showPopup(MouseEvent e) {
				Study study;
				if(table.getSelection().size()==1) {
					study = table.getSelection().get(0); //StudyDAO.loadStudyWithGroupsPhases(table.getSelection().get(0), false);
				} else {
					study = null;
				}
				StudyActions.createPopup(study).show(table, e.getX(), e.getY());				
			}
		});
	}	
	
	/**
	 * Attach "edit Study" popup to a depictor
	 * @param valueListPanel
	 */
	public static void attachPopup(final StudyDetailPanel panel) {
		attachPopup(panel.getStudyDepictor());
		panel.getStudyPane().addMouseListener(new PopupAdapter() {			
			@Override
			protected void showPopup(MouseEvent e) {
				StudyActions.createPopup(panel.getStudy()).show(panel.getStudyPane(), e.getX(), e.getY());								
			}
		});
	}
	public static void attachPopup(final StudyDepictor panel) {
		panel.addMouseListener(new PopupAdapter() {			
			@Override
			protected void showPopup(MouseEvent e) {
				StudyActions.createPopup(panel.getStudy()).show(panel, e.getX(), e.getY());								
			}
		});		
	}
	
	public static JPopupMenu createPopup(Study study) {
		//Reload the study to make sure the object is accurate
		study = JPAUtil.reattach(study);
		
		JPopupMenu popupMenu = new JPopupMenu();
		if(study!=null) {
			popupMenu.add(new JCustomLabel("    Study: " + study.getStudyId(), Font.BOLD));	
			popupMenu.add(new JSeparator());
			
			if(Spirit.getUser()==null) {
				popupMenu.add(new SpiritAction.Action_Relogin(null, null));			
				return popupMenu;
			}

			//New
			JMenu newMenu = new JMenu("New");
			newMenu.setIcon(IconType.NEW.getIcon());
			newMenu.setMnemonic('n');
			popupMenu.add(newMenu);
			{
				newMenu.add(new Action_New());
				newMenu.add(new Action_Duplicate(study));
			}
			//Edit
			JMenu editMenu = new JMenu("Edit");
			editMenu.setIcon(IconType.EDIT.getIcon());
			editMenu.setMnemonic('e');
			popupMenu.add(editMenu);
			{
				editMenu.add(new JMenuItem(new Action_EditInfos(study)));			
				editMenu.add(new JMenuItem(new Action_EditDesign(study)));	
				if(SpiritProperties.getInstance().hasStudyWorkflow()) {
					editMenu.add(new JMenuItem(new Action_Promote(study)));
				}
				editMenu.add(new JSeparator());
				
				//Attach
				JMenu attachMenu = new JMenu("Automatic Assignment");
				attachMenu.setIcon(IconType.LINK.getIcon());
				attachMenu.setMnemonic('a');
				editMenu.add(attachMenu);
				Set<Phase> randoPhases = study.getPhasesWithGroupAssignments();
				if(randoPhases.size()>0) {					
					for (Phase phase : randoPhases) {
						assert phase.getStudy()!=null;
						attachMenu.add(new JMenuItem(new Action_GroupAssignment(phase)));
					}
				} else {
					attachMenu.add(new JMenuItem());
					attachMenu.setEnabled(false);
				}
					
				editMenu.add(new JMenuItem(new Action_ManualAssignment(study)));
			}
			
			popupMenu.add(new JSeparator());
			popupMenu.add(new Action_AnimalMonitoring(study));	
			popupMenu.add(new Action_SetLivingStatus(study));
			popupMenu.add(new JSeparator());
			popupMenu.add(new Action_ManageSamples(study));
			popupMenu.add(new Action_MeasurementSamples(study));
			
			
			popupMenu.add(new JSeparator());
			popupMenu.add(new Action_Report(study));
			popupMenu.add(new JSeparator());
			
			//System
			JMenu systemMenu = new JMenu("Advanced"); 
			systemMenu.setIcon(IconType.ADMIN.getIcon());
			systemMenu.add(new Action_Delete(study));
			systemMenu.add(new JSeparator());
			systemMenu.add(new Action_AssignTo(study));
			systemMenu.add(new JSeparator());
			systemMenu.add(new Action_History(study));		
			popupMenu.add(systemMenu);
			
							
		} else {
			popupMenu.add(new JCustomLabel("   Study Menu", Font.BOLD));
			popupMenu.add(new JSeparator());
			popupMenu.add(new JMenuItem(new Action_New()));		
		}
		return popupMenu;
	}

}
