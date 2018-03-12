/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.audit.StudyHistoryDlg;
import com.actelion.research.spiritapp.ui.study.depictor.StudyDepictor;
import com.actelion.research.spiritapp.ui.study.edit.PromoteDlg;
import com.actelion.research.spiritapp.ui.study.edit.StudyDiscardDlg;
import com.actelion.research.spiritapp.ui.study.monitor.MonitoringOverviewDlg;
import com.actelion.research.spiritapp.ui.study.randomize.RandomizationDlg;
import com.actelion.research.spiritapp.ui.study.sampleweighing.SampleWeighingDlg;
import com.actelion.research.spiritapp.ui.study.wizard.StudyDesignDlg;
import com.actelion.research.spiritapp.ui.study.wizard.StudyInfoDlg;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.ui.util.component.UserIdComboBox;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
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
			setEnabled(SpiritRights.canCreateStudy(SpiritFrame.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			Study study = new Study();
			study.setState(SpiritProperties.getInstance().getValue(PropertyKey.STUDY_DEFAULTSTATE));
			if(SpiritFrame.getUser()!=null) {
				study.setAdminUsers(SpiritFrame.getUser().getUsername());
			}
			if(SpiritFrame.getUser().getMainGroup()!=null) study.setEmployeeGroups(Collections.singletonList(SpiritFrame.getUser().getMainGroup()));

			if(SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_STUDYDESIGN)) {
				StudyDesignDlg.editStudy(study);
			} else {
				StudyInfoDlg.editStudy(study);
			}
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
			setEnabled(SpiritRights.canEdit(study, SpiritFrame.getUser()));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			StudyInfoDlg.editStudy(study);
		}
	}

	public static class Action_EditDesign extends AbstractAction {
		private final Study study;
		public Action_EditDesign(Study study) {
			super("Study Design");
			this.study = study;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			putValue(AbstractAction.SMALL_ICON, IconType.STUDY.getIcon());
			boolean designMode = SpiritProperties.getInstance().isChecked(PropertyKey.STUDY_FEATURE_STUDYDESIGN);
			setEnabled(designMode && SpiritRights.canEdit(study, SpiritFrame.getUser()));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			StudyDesignDlg.editStudy(study);
		}
	}

	public static class Action_Promote extends AbstractAction {
		private final Study study;
		public Action_Promote(Study study) {
			super("Change Status");
			this.study = study;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('s'));
			putValue(AbstractAction.SMALL_ICON, IconType.STATUS.getIcon());
			setEnabled(SpiritRights.canPromote(study, SpiritFrame.getUser()) && SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES).length>1);
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
			super("Delete");
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
			super("Change Ownership/CreatedBy (admin)");
			this.study = study;

			putValue(AbstractAction.SMALL_ICON, IconType.ADMIN.getIcon());
			putValue(AbstractAction.MNEMONIC_KEY, (int)'o');
			setEnabled(SpiritRights.canEdit(study, SpiritFrame.getUser()));
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
				res = JOptionPane.showConfirmDialog(null, "Are you sure to update the owner/createdBy to " + u.getUsername()+"?", "Change Ownership", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
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


	public static class Action_GroupAssignment extends AbstractAction {
		private Phase phase;

		public Action_GroupAssignment(Phase phase) {
			super(phase.toString());
			this.phase = phase;
			putValue(AbstractAction.SMALL_ICON, IconType.LINK.getIcon());
			setEnabled(phase.getStudy()!=null && phase.getStudy().getPhasesWithGroupAssignments().size()>0 && SpiritRights.canEditBiosamples(phase.getStudy(), SpiritFrame.getUser()));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Study study = phase.getStudy();
			if(study==null || !SpiritRights.canBlind(study, SpiritFrame.getUser())) return;
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
			setEnabled(study!=null && (SpiritRights.canEditBiosamples(study, SpiritFrame.getUser()) || SpiritRights.canBlind(study, SpiritFrame.getUser())) && study.getParticipants().size()>0);
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
			setEnabled(study!=null && SpiritRights.canEditBiosamples(study, SpiritFrame.getUser()));
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
			setEnabled(SpiritRights.canRead(study, SpiritFrame.getUser()));
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



	public static class Action_SetLivingStatus extends AbstractAction {

		private List<Biosample> biosamples;
		private final Study study;

		public Action_SetLivingStatus(Study study) {
			this(study==null? new ArrayList<Biosample>(): new ArrayList<>(study.getParticipantsSorted()));
		}

		public Action_SetLivingStatus(List<Biosample> biosamples) {
			super("Set Living Status");
			this.biosamples = biosamples;

			putValue(AbstractAction.MNEMONIC_KEY, (int)('e'));
			putValue(Action.SMALL_ICON, IconType.STUDY.getIcon());

			Set<Study> studies = new HashSet<>();
			boolean canEdit = true;
			for (Biosample b : biosamples) {
				canEdit = canEdit && SpiritRights.canEdit(b, SpiritFrame.getUser());
				studies.add(b.getAttachedStudy());
			}
			study = studies.size()==1? studies.iterator().next(): null;
			setEnabled(study!=null && canEdit);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(study==null) return;
			try {
				new SetLivingStatusDlg(study, biosamples);
			} catch (Exception ex) {
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
		return SpiritFrame.getInstance().getPopupHelper().createStudyPopup(study);
	}

}
