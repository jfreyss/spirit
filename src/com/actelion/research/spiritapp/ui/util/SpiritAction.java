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

package com.actelion.research.spiritapp.ui.util;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.admin.ChangePasswordDlg;
import com.actelion.research.spiritapp.ui.admin.config.ConfigDlg;
import com.actelion.research.spiritapp.ui.admin.database.DatabaseSettingsDlg;
import com.actelion.research.spiritapp.ui.admin.database.SpiritPropertyDlg;
import com.actelion.research.spiritapp.ui.biosample.batchaliquot.BatchAliquotDlg;
import com.actelion.research.spiritapp.ui.biosample.batchassign.BatchAssignDlg;
import com.actelion.research.spiritapp.ui.biosample.edit.EditBiosampleDlg;
import com.actelion.research.spiritapp.ui.print.BrotherLabelsDlg;
import com.actelion.research.spiritapp.ui.util.component.PreferencesDlg;
import com.actelion.research.spiritapp.ui.util.scanner.SpiritScanner;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.UsageLog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.iconbutton.IconType;

public class SpiritAction {

	public static class Action_Refresh extends AbstractAction {
		private AbstractAction nextAction;
		public Action_Refresh() {
			this(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if(SpiritFrame.getInstance()!=null) SpiritFrame.getInstance().recreateUI();
				}
			});
		}

		public Action_Refresh(AbstractAction nextAction) {
			super("Refresh");
			this.nextAction = nextAction;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('r'));
			putValue(AbstractAction.SMALL_ICON, IconType.REFRESH.getIcon());

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				JPAUtil.closeFactory();
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
			if(nextAction!=null) nextAction.actionPerformed(e);
		}
	}

	public static class Action_Preferences extends AbstractAction {
		public Action_Preferences() {
			super("Preferences");
			putValue(AbstractAction.SMALL_ICON, IconType.SETUP.getIcon());
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			new PreferencesDlg();
		}
	}

	public static class Action_Exit extends AbstractAction {
		public Action_Exit() {
			super("Exit");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}


	public static class Action_Relogin extends AbstractAction {
		private Frame top;
		private String app;
		private String msg;

		public Action_Relogin(Frame top, String app) {
			super("Logout");
			this.top = top;
			this.app = app;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('o'));
		}

		public Action_Relogin(Frame top, String app, String msg) {
			super("Logout");
			this.top = top;
			this.app = app;
			this.msg = msg;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('o'));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			//Open dialog
			SpiritFrame.setUser((SpiritUser) null);
			LoginDlg.openLoginDialog(top, (app==null?"": app + " ") +"Login", msg);
			SpiritChangeListener.fireModelChanged(SpiritChangeType.LOGIN);

			//SatusBar
			SpiritUser user = SpiritFrame.getUser();
			if(user==null) {
				System.exit(1);
			} else {
				SpiritContextListener.setStatus(SpiritFrame.getUser().getUsername() +" logged");

				String userMsg = user.getUsername()
						+ (SpiritProperties.getInstance().isChecked(PropertyKey.USER_USEGROUPS)? " ("+ (user.getMainGroup()==null?"NoDept":user.getMainGroup().getName())+ ") ": "")
						+ (user.getRolesString().length()>0? " - " + user.getRolesString():"");
				SpiritContextListener.setUser(userMsg);
			}
		}
	}

	public static void logUsage(final String app) {
		String version = Spirit.class.getPackage().getImplementationVersion();
		if(DBAdapter.getInstance().isInActelionDomain()) {
			//Record usage and version
			if(version==null) return;
			UsageLog.logUsage("Spirit", SpiritFrame.getUsername(), null, UsageLog.ACTION_LOGON, "app=" + app + ";v="+version);
		} else if(!"false".equalsIgnoreCase(System.getProperty("jnlp.logusage"))) {
			new Thread() {
				@Override
				public void run() {
					try {
						//Record usage
						new URL("http://c.statcounter.com/11069822/0/f9288463/1/").getContent();
					} catch(Exception e) {
						e.printStackTrace();
					}
				};
			}.start();
		}


	}

	public static class Action_ChangePassword extends AbstractAction {
		public Action_ChangePassword() {
			super("Change Password");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('p'));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new ChangePasswordDlg();
		}
	}

	public static class Action_Config extends AbstractAction {
		public Action_Config() {
			super("Config (Balances,...)");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('c'));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new ConfigDlg();
		}
	}

	public static class Action_Help extends AbstractAction {
		public Action_Help() {
			super("Help");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('H'));
			putValue(AbstractAction.SMALL_ICON, IconType.HELP.getIcon());
			putValue(AbstractAction.MNEMONIC_KEY, KeyStroke.getKeyStroke("F1").getKeyCode());
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			HelpBinder.showHelp("");
		}
	}

	public static class Action_PrintLabels extends AbstractAction {
		public Action_PrintLabels() {
			super("Print Raw P-Touch Labels");
			putValue(AbstractAction.SMALL_ICON, IconType.PRINT.getIcon());
			putValue(AbstractAction.MNEMONIC_KEY, (int)('r'));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new BrotherLabelsDlg();
		}
	}

	public static class Action_DatabaseConnection extends AbstractAction {
		public Action_DatabaseConnection() {
			super("Database connection");
			putValue(AbstractAction.SMALL_ICON, IconType.ADMIN.getIcon());
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			setEnabled(SpiritRights.isSuperAdmin(SpiritFrame.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new DatabaseSettingsDlg(false);
		}
	}

	public static class Action_DatabaseProperties extends AbstractAction {
		public Action_DatabaseProperties() {
			super("Settings");
			putValue(AbstractAction.SMALL_ICON, IconType.ADMIN.getIcon());
			putValue(AbstractAction.MNEMONIC_KEY, (int)('s'));
			setEnabled(SpiritRights.isSuperAdmin(SpiritFrame.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new SpiritPropertyDlg();
		}
	}


	public static class Action_Scan extends AbstractAction {
		public Action_Scan() {
			super("Scan");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('r'));
			putValue(Action.SMALL_ICON, IconType.SCANNER.getIcon());
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				SpiritScanner scanner = new SpiritScanner();
				Location rack = scanner.scan(null, false);
				if(rack==null) return;

				SpiritContextListener.setRack(rack);

			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}

	public static class Action_ScanAndSetLocation extends AbstractAction {
		public Action_ScanAndSetLocation() {
			super("Scan & Set Location");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('r'));
			putValue(Action.SMALL_ICON, IconType.SCANNER.getIcon());
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				SpiritScanner scanner = new SpiritScanner();
				Location rack = scanner.scan(null, true);
				if(rack==null) return;

				if(rack.getName()==null || rack.getName().length()==0) {
					//Simple Scan
					SpiritContextListener.setRack(rack);
				} else {
					//Scan and save
					List<Biosample> biosamples = new ArrayList<>(rack.getBiosamples());
					Collections.sort(biosamples);
					for (Biosample b : biosamples) {
						b.setLocPos(rack, rack.parsePosition(b.getScannedPosition()));
						b.setScannedPosition(null);
					}
					try {
						JPAUtil.pushEditableContext(SpiritFrame.getUser());
						EditBiosampleDlg.createDialogForEditSameTransaction(biosamples).setVisible(true);
					} finally {
						JPAUtil.popEditableContext();
					}
					SpiritContextListener.setRack(rack);
				}


			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}

	public static class Action_ScanAndAssign extends AbstractAction {
		public Action_ScanAndAssign() {
			super("Scan & Assign samples");
			putValue(Action.SMALL_ICON, IconType.SCANNER.getIcon());
			putValue(AbstractAction.SHORT_DESCRIPTION, "Scan Tubes in order to assign existing biosamples to containerIds");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			new BatchAssignDlg().setVisible(true);
		}
	}

	public static class Action_ScanAndAliquot extends AbstractAction {
		public Action_ScanAndAliquot() {
			super("Scan & Create aliquots from samples");
			putValue(Action.SMALL_ICON, IconType.SCANNER.getIcon());
			putValue(AbstractAction.SHORT_DESCRIPTION, "Scan Tubes in order to create aliquots and assign them to containerIds");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			new BatchAliquotDlg().setVisible(true);
		}
	}

}
