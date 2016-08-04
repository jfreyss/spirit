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

package com.actelion.research.spiritapp.spirit.ui;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import com.actelion.research.spiritapp.animalcare.AnimalCare;
import com.actelion.research.spiritapp.bioviewer.BioViewer;
import com.actelion.research.spiritapp.slidecare.SlideCare;
import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.admin.ChangePasswordDlg;
import com.actelion.research.spiritapp.spirit.ui.admin.database.DatabaseSettingsDlg;
import com.actelion.research.spiritapp.spirit.ui.config.ConfigDlg;
import com.actelion.research.spiritapp.spirit.ui.help.HelpDlg;
import com.actelion.research.spiritapp.spirit.ui.print.BrotherLabelsDlg;
import com.actelion.research.spiritapp.spirit.ui.util.LoginDlg;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.stockcare.StockCare;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.UsageLog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class SpiritAction {

	public static class Action_Refresh extends AbstractAction {
		private AbstractAction nextAction; 
		public Action_Refresh(final Spirit spirit) {			
			this(new AbstractAction() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					spirit.recreateTabs();
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
				JPAUtil.refresh();
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
			if(nextAction!=null) nextAction.actionPerformed(e);
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
			super("Login");
			this.top = top;
			this.app = app;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('o'));
		}
		
		public Action_Relogin(Frame top, String app, String msg) {			
			super("Login");
			this.top = top;
			this.app = app;
			this.msg = msg;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('o'));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			//Open dialog
			LoginDlg.openLoginDialog(top, (app==null?"": app + " ") +"Login", msg);
			SpiritChangeListener.fireModelChanged(SpiritChangeType.LOGIN);

			//SatusBar
			SpiritUser user = Spirit.getUser();
			if(user==null) {
				SpiritContextListener.setStatus("");								
				SpiritContextListener.setUser("No user logged in");								
			} else {
				SpiritContextListener.setStatus(Spirit.getUser().getUsername() +" logged");								
				SpiritContextListener.setUser(user.getUsername() + " ("+ (user.getMainGroup()==null?"NoDept":user.getMainGroup().getName())+ ") " + (user.getRolesString().length()>0? " - " + user.getRolesString():""));								
			}
			
			//Log usage
			if(DBAdapter.getAdapter().isInActelionDomain()) {
				if(Spirit.getUser()!=null) {
					UsageLog.logUsage("Spirit", Spirit.getUser().getUsername(), null, UsageLog.ACTION_LOGON, "Spirit");
				}
			}
			
		}
		public void updateStatus() {
			
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
			new HelpDlg();
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
	
	public static class Action_OpenSpirit extends AbstractAction {
		public Action_OpenSpirit() {
			super("Open Spirit");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('s'));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			Spirit.open();
		}
	}
	
	public static class Action_OpenAnimalCare extends AbstractAction {
		public Action_OpenAnimalCare() {
			super("Open AnimalCare");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('a'));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			AnimalCare.open();
		}
	}
	
	public static class Action_OpenSlideCare extends AbstractAction {
		public Action_OpenSlideCare() {
			super("Open SlideCare");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('l'));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			SlideCare.open();
		}
	}
	
	public static class Action_OpenStockCare extends AbstractAction {
		public Action_OpenStockCare() {
			super("Open StockCare");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('s'));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			StockCare.open();
		}
	}
	
	public static class Action_OpenBioViewer extends AbstractAction {
		public Action_OpenBioViewer() {
			super("Open BioViewer");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('s'));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			BioViewer.open();
		}
	}
	
	public static class Action_DatabaseSettings extends AbstractAction {
		public Action_DatabaseSettings() {
			super("Database settings");
			putValue(AbstractAction.SMALL_ICON, IconType.ADMIN.getIcon());
			putValue(AbstractAction.MNEMONIC_KEY, (int)('s'));
			setEnabled(SpiritRights.isSuperAdmin(Spirit.getUser()));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new DatabaseSettingsDlg(false);
		}
	}
	
}
