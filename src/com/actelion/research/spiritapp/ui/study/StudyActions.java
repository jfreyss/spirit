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

package com.actelion.research.spiritapp.ui.study;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
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

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.audit.StudyHistoryDlg;
import com.actelion.research.spiritapp.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.ui.biosample.BiosampleActions.Action_SetLivingStatus;
import com.actelion.research.spiritapp.ui.study.depictor.StudyDepictor;
import com.actelion.research.spiritapp.ui.study.edit.StudyDiscardDlg;
import com.actelion.research.spiritapp.ui.study.monitor.MonitoringOverviewDlg;
import com.actelion.research.spiritapp.ui.study.randomize.RandomizationDlg;
import com.actelion.research.spiritapp.ui.study.sampleweighing.SampleWeighingDlg;
import com.actelion.research.spiritapp.ui.study.wizard.StudyDesignDlg;
import com.actelion.research.spiritapp.ui.study.wizard.StudyInfoDlg;
import com.actelion.research.spiritapp.ui.util.SpiritAction;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.ui.util.lf.UserIdComboBox;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
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
			setEnabled(SpiritRights.canAdmin((Study)null, SpiritFrame.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			Study study = new Study();
			study.setState(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_DEFAULTSTATE));
			if(SpiritFrame.getUser()!=null) {
				study.setAdminUsers(SpiritFrame.getUser().getUsername());
			}
			if(SpiritFrame.getUser().getMainGroup()!=null) study.setEmployeeGroups(Collections.singletonList(SpiritFrame.getUser().getMainGroup()));
			StudyDesignDlg.editStudy(study);
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
			setEnabled(SpiritRights.canAdmin(study, SpiritFrame.getUser()));
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
			setEnabled(SpiritRights.canAdmin(study, SpiritFrame.getUser()));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			StudyDesignDlg.editStudy(study);
		}
	}

	//	public static class Action_Promote extends AbstractAction {
	//		private final Study study;
	//		public Action_Promote(Study study) {
	//			super("Change Workflow Status");
	//			this.study = study;
	//			putValue(AbstractAction.MNEMONIC_KEY, (int)('s'));
	//			putValue(AbstractAction.SMALL_ICON, IconType.STATUS.getIcon());
	//			setEnabled(SpiritRights.canPromote(study, SpiritFrame.getUser()));
	//		}
	//
	//		@Override
	//		public void actionPerformed(ActionEvent e) {
	//			new PromoteDlg(study);
	//		}
	//	}

	public static class Action_Duplicate extends AbstractAction {
		private final Study study;
		public Action_Duplicate(Study study) {
			super("Duplicate Study");
			this.study = study;
			putValue(AbstractAction.SMALL_ICON, IconType.DUPLICATE.getIcon());
			setEnabled(SpiritRights.canRead(study, SpiritFrame.getUser()));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			StudyDesignDlg.duplicateStudy(study);
		}
	}

	public static class Action_Delete extends AbstractAction {
		private final Study study;

		public Action_Delete(Study study) {
			super("Delete (owner only)");
			this.study = study;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			putValue(Action.SMALL_ICON, IconType.DELETE.getIcon());

			setEnabled(SpiritRights.canDelete(study, SpiritFrame.getUser()));
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
			setEnabled(SpiritRights.isSuperAdmin(SpiritFrame.getUser()));
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
			JPAUtil.pushEditableContext(SpiritFrame.getUser());
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

	//	public static class Action_LinkParticipants extends AbstractAction {
	//		private final Study study;
	//		public Action_LinkParticipants(Study study) {
	//			super("Link to existing participants");
	//			this.study = study;
	//			putValue(AbstractAction.MNEMONIC_KEY, (int)('a'));
	//			putValue(AbstractAction.SMALL_ICON, IconType.LINK.getIcon());
	//			setEnabled(SpiritRights.canExpert(study, SpiritFrame.getUser()) && !SpiritRights.isBlindAll(study, Spirit.getUser()));
	//		}
	//
	//		@Override
	//		public void actionPerformed(ActionEvent e) {
	//			new AttachSamplesManuallyDlg(study);
	//		}
	//	}


	public static class Action_GroupAssignment extends AbstractAction {
		private Phase phase;

		public Action_GroupAssignment(Phase phase) {
			super(phase.toString());
			this.phase = phase;
			putValue(AbstractAction.SMALL_ICON, IconType.LINK.getIcon());
			setEnabled(phase.getStudy()!=null && phase.getStudy().getPhasesWithGroupAssignments().size()>0 && SpiritRights.canExpert(phase.getStudy(), SpiritFrame.getUser()));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Study study = phase.getStudy();

			if(study==null || !SpiritRights.canBlind(study, SpiritFrame.getUser())) return;

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
			setEnabled(study!=null && study.getPhases().size()>0 && SpiritRights.canBlind(study, SpiritFrame.getUser()) && study.getParticipants().size()>0 );
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

	public static class Action_ManageSamples extends AbstractAction {
		private final Study study;
		public Action_ManageSamples(Study study) {
			super("Manage / Print Samples");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('p'));

			this.study = study;
			putValue(AbstractAction.SMALL_ICON, IconType.PRINT.getIcon());
			setEnabled(study!=null && (SpiritRights.canExpert(study, SpiritFrame.getUser()) || SpiritRights.canBlind(study, SpiritFrame.getUser())) && study.getParticipants().size()>0);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(study==null) return;
			new ManageSamplesDlg(study);
		}
	}

	public static class Action_MeasurementSamples extends AbstractAction {
		private final Study study;
		public Action_MeasurementSamples(Study study) {
			super("Sample Measurement");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('m'));

			this.study = study;
			putValue(AbstractAction.SMALL_ICON, IconType.BALANCE.getIcon());
			setEnabled(study!=null && (study.getPhases().size()>0 && SpiritRights.canBlind(study, SpiritFrame.getUser())) && study.getParticipants().size()>0);
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
			setEnabled(study!=null && SpiritRights.canExpert(study, SpiritFrame.getUser()));
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
			super("Audit Trail");
			this.study = study;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('a'));
			putValue(Action.SMALL_ICON, IconType.HISTORY.getIcon());
			setEnabled(SpiritRights.canExpert(study, SpiritFrame.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				new StudyHistoryDlg(study);
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

	public static JPopupMenu createPopup(Study s) {
		//Reload the study to make sure the object is accurate
		final Study study = s==null? null: DAOStudy.getStudy(s.getId());

		JPopupMenu popupMenu = new JPopupMenu();
		if(study!=null) {
			popupMenu.add(new JCustomLabel("    Study: " + study.getStudyId(), Font.BOLD));
			popupMenu.add(new JSeparator());

			if(SpiritFrame.getUser()==null) {
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
			}

			//Assignment
			JMenu attachMenu = new JMenu("Participants");
			attachMenu.setIcon(IconType.LINK.getIcon());
			attachMenu.setMnemonic('a');
			popupMenu.add(attachMenu);
			if(SpiritRights.isBlindAll(study, Spirit.getUser())) {
				attachMenu.setEnabled(false);
			}
			{
				if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_ADVANCEDMODE)) {
					JMenu autoMenu = new JMenu("Automatic Assignment");
					autoMenu.setIcon(IconType.LINK.getIcon());
					autoMenu.setMnemonic('a');
					attachMenu.add(autoMenu);
					Set<Phase> randoPhases = study.getPhasesWithGroupAssignments();
					if(randoPhases.size()>0) {
						for (Phase phase : randoPhases) {
							assert phase.getStudy()!=null;
							autoMenu.add(new JMenuItem(new Action_GroupAssignment(phase)));
						}
					} else {
						autoMenu.add(new JMenuItem());
						autoMenu.setEnabled(false);
					}
					attachMenu.add(new JSeparator());
				}

				//				attachMenu.add(new JMenuItem(new Action_LinkParticipants(study)));
				//				attachMenu.add(new JSeparator());
				attachMenu.add(new JMenuItem(new BiosampleActions.Action_BatchEdit("Edit Participants", study, study.getParticipantsSorted().size()>0 && SpiritRights.canExpert(study, Spirit.getUser())) {
					@Override
					public List<Biosample> getBiosamples() {
						List<Biosample> res = new ArrayList<>(study.getParticipantsSorted());
						Collections.sort(res);
						return res;
					}
				}));
				attachMenu.add(new JMenuItem(new BiosampleActions.Action_BatchEdit("Add Participants", study, SpiritRights.canExpert(study, Spirit.getUser())) {
					@Override
					public List<Biosample> getBiosamples() {
						Study s = JPAUtil.reattach(study);
						//Creates a biosample (either a new animal or a sample similar to the existing participants)
						Biosample b = new Biosample();
						Biotype biotype = Biosample.getBiotype(s.getParticipants());
						b.setBiotype(biotype==null? DAOBiotype.getBiotype(Biotype.ANIMAL): biotype);
						b.setContainerType(Biosample.getContainerType(s.getParticipants()));
						if(b.getBiotype()!=null) {
							for (BiotypeMetadata bType : b.getBiotype().getMetadata()) {
								Set<String> vals = Biosample.getMetadata(bType, s.getParticipants());
								b.setMetadataValue(bType, vals.size()==1? vals.iterator().next(): null);
							}
						}

						//Creates the list of participants to edit
						List<Biosample> res = new ArrayList<>();
						if(s.getParticipants().size()==0) {
							//Create an empty template from the existing animals
							for (Group group : s.getGroups()) {
								for(int subgroup=0; subgroup<Math.max(1, group.getNSubgroups()); subgroup++) {
									int n = group.getSubgroupSize(subgroup);
									if(n==0 && group.getFromGroup()==null) n=1;
									for(int i=0; i<n; i++) {
										Biosample b2 = b.clone();
										b2.setAttached(s, group, subgroup);
										res.add(b2);
									}
								}
							}
						}
						if(res.isEmpty()) {
							b.setAttached(s, null, 0);
							res.add(b);
						}

						return res;
					}
				}));
			}


			if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_ADVANCEDMODE)) {
				popupMenu.add(new JSeparator());
				popupMenu.add(new Action_AnimalMonitoring(study));
				popupMenu.add(new Action_SetLivingStatus(study));
				popupMenu.add(new JSeparator());
				popupMenu.add(new Action_ManageSamples(study));
				popupMenu.add(new Action_MeasurementSamples(study));
			}

			popupMenu.add(new JSeparator());
			popupMenu.add(new Action_Report(study));
			popupMenu.add(new JSeparator());

			//Advanced
			popupMenu.add(new Action_History(study));
			JMenu systemMenu = new JMenu("Advanced");
			systemMenu.setIcon(IconType.ADMIN.getIcon());
			systemMenu.add(new Action_Delete(study));
			systemMenu.add(new JSeparator());
			systemMenu.add(new Action_AssignTo(study));
			popupMenu.add(systemMenu);


		} else {
			popupMenu.add(new JCustomLabel("   Study Menu", Font.BOLD));
			popupMenu.add(new JSeparator());
			popupMenu.add(new JMenuItem(new Action_New()));
		}
		return popupMenu;
	}

}
