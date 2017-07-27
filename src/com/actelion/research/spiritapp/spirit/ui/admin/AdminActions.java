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

package com.actelion.research.spiritapp.spirit.ui.admin;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.admin.user.UserAdminDlg;
import com.actelion.research.spiritapp.spirit.ui.audit.LogEntryDlg;
import com.actelion.research.spiritapp.spirit.ui.audit.RecentChancesDlg;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserAdministrationMode;
import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;

public class AdminActions {


	public static class Action_AdminBiotypes extends AbstractAction {
		public Action_AdminBiotypes() {
			super("Edit Biotypes (Admin)");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('t'));
			putValue(AbstractAction.SMALL_ICON, IconType.ADMIN.getIcon());
			setEnabled(SpiritRights.isSuperAdmin(SpiritFrame.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new BiotypeOverviewDlg();
		}
	}

	public static class Action_AdminTests extends AbstractAction {
		public Action_AdminTests() {
			super("Edit Tests (Admin)");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('t'));
			putValue(AbstractAction.SMALL_ICON, IconType.ADMIN.getIcon());
			setEnabled(SpiritRights.isSuperAdmin(SpiritFrame.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new TestOverviewDlg();
		}
	}


	public static class Action_Restore extends AbstractAction {
		private final List<? extends IObject> objects;

		public Action_Restore(List<? extends IObject> objects) {
			super("Restore this version");
			this.objects = objects;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('r'));
			setEnabled(SpiritRights.isSuperAdmin(SpiritFrame.getUser()));
		}

		@Override
		public void actionPerformed(ActionEvent ev) {
			final List<Biosample> biosamples = new ArrayList<>();
			final List<Result> results = new ArrayList<>();
			final List<Study> studies = new ArrayList<>();
			for (IObject o : objects) {
				if(o instanceof Biosample) {
					biosamples.add((Biosample)o);
				} else if(o instanceof Result) {
					results.add((Result)o);
				} else if(o instanceof Study) {
					studies.add((Study)o);
				}
			}
			String details = (biosamples.size()>0? biosamples.size()+" samples, ": "") + (results.size()>0? results.size()+" results, ": "") + (studies.size()>0? studies.size()+" studies, ": "");
			if(details.length()>0) details = details.substring(0, details.length()-2);

			final String reason = JOptionPane.showInputDialog(UIUtils.getMainFrame(), "If you are sure to restore this version ("+details+"). Please give a reason!", "Reason", JOptionPane.QUESTION_MESSAGE);
			if(reason!=null) {
				new SwingWorkerExtended("Rollback", UIUtils.getMainFrame()) {
					@Override
					protected void doInBackground() throws Exception {
						DAORevision.restore(objects, SpiritFrame.getUser(), "Rollback" + (reason.length()==0?"":"(" + reason + ")"));
					}
					@Override
					protected void done() {
						JExceptionDialog.showInfo(UIUtils.getMainFrame(), objects.size()+" objects rollbacked");

						if(biosamples.size()>0) {
							SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamples);
						}
						if(results.size()>0) {
							SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Result.class, results);
						}
						if(studies.size()>0) {
							SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, studies);
						}
					}
				};
			} else {
				JOptionPane.showMessageDialog(UIUtils.getMainFrame(), "Rollback canceled");
			}
		}
	}

	public static class Action_Revisions extends AbstractAction {
		private String userId;
		public Action_Revisions(String userId) {
			super("Recent Changes (" + (userId==null?"Admin": userId==""?"NA": userId) + ")");
			this.userId = userId;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('r'));
			putValue(AbstractAction.SMALL_ICON, userId==null? IconType.ADMIN.getIcon(): IconType.HISTORY.getIcon());
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new RecentChancesDlg(userId);
		}
	}

	public static class Action_RenameElb extends AbstractAction {
		public Action_RenameElb() {
			super("Rename ELB (Admin)");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('r'));
			putValue(AbstractAction.SMALL_ICON, IconType.ADMIN.getIcon());
			setEnabled(SpiritRights.isSuperAdmin(SpiritFrame.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new ELBRenameDlg();
		}
	}

	public static class Action_LastLogins extends AbstractAction {
		public Action_LastLogins() {
			super("Recent Connections (Admin)");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('c'));
			putValue(AbstractAction.SMALL_ICON, IconType.ADMIN.getIcon());
			setEnabled(DBAdapter.getAdapter().getUserManagedMode()!=UserAdministrationMode.UNIQUE_USER && (SpiritFrame.getUser()==null || SpiritRights.isSuperAdmin(SpiritFrame.getUser())));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new LogEntryDlg();
		}
	}

	public static class Action_ManageUsers extends AbstractAction {
		public Action_ManageUsers() {
			super("Edit Users/Groups (Admin)");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('r'));
			putValue(AbstractAction.SMALL_ICON, IconType.ADMIN.getIcon());
			setEnabled(DBAdapter.getAdapter().getUserManagedMode()==UserAdministrationMode.READ_WRITE && (SpiritFrame.getUser()==null || SpiritRights.isSuperAdmin(SpiritFrame.getUser())));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new UserAdminDlg();
		}
	}


	public static class Action_ExpiredSamples extends AbstractAction {
		public Action_ExpiredSamples() {
			super("Query Expired Samples");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('e'));
			putValue(AbstractAction.SMALL_ICON, IconType.SANDGLASS.getIcon());
			setEnabled(SpiritFrame.getUser()==null || SpiritRights.isSuperAdmin(SpiritFrame.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new ExpiredSamplesDlg();

		}
	}


}
