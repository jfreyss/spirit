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

import javax.swing.JMenu;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.spirit.ui.admin.AdminActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.result.ResultActions;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritAction;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserAdministrationMode;
import com.actelion.research.util.ui.SplashScreen2;
import com.actelion.research.util.ui.SplashScreen2.SplashConfig;
import com.actelion.research.util.ui.UIUtils;

public class SpiritMenu {
	
	public static void addEditMenuItems(JMenu editMenu, SpiritFrame spirit) {
		editMenu.add(new SpiritAction.Action_Preferences());
		editMenu.add(new JSeparator());
		if(DBAdapter.getAdapter().getUserManagedMode()==UserAdministrationMode.READ_WRITE) {
			editMenu.add(new SpiritAction.Action_ChangePassword());				
			editMenu.add(new JSeparator());
		}
		editMenu.add(new SpiritAction.Action_Refresh(spirit));
		editMenu.add(new SpiritAction.Action_Relogin(UIUtils.getMainFrame(), "Spirit"));
		editMenu.add(new SpiritAction.Action_Exit());
	}
	
	public static JMenu getDevicesMenu() {
		JMenu devicesMenu = new JMenu("Devices");
		devicesMenu.setMnemonic('d');
		devicesMenu.add(new SpiritAction.Action_Scan());
		devicesMenu.add(new SpiritAction.Action_ScanAndSetLocation());
		devicesMenu.add(new SpiritAction.Action_ScanAndAssign());
		devicesMenu.add(new SpiritAction.Action_ScanAndAliquot());
		devicesMenu.add(new JSeparator());
		devicesMenu.add(new SpiritAction.Action_Config());		
		devicesMenu.add(new JSeparator());
		devicesMenu.add(new SpiritAction.Action_PrintLabels());
		return devicesMenu;
	}
	
//	public static JMenu getPerspectivesMenu() {
//		JMenu perspectiveMenu = new JMenu("Perspective");
//		perspectiveMenu.setMnemonic('p');
//		
//		
////		perspectiveMenu.add(new SpiritAction.Action_OpenSpirit());
//		perspectiveMenu.add(new JCheckBoxMenuItem(new SpiritAction.Action_Perspective("Home", HomeTab.class, true)));
//		perspectiveMenu.add(new JSeparator());
//		perspectiveMenu.add(new JCheckBoxMenuItem(new SpiritAction.Action_Perspective("AnimalCare", AnimalCareTab.class, false)));
//		perspectiveMenu.add(new JCheckBoxMenuItem(new SpiritAction.Action_Perspective("BioViewer", BioViewerTab.class, false)));
////		perspectiveMenu.add(new SpiritAction.Action_OpenSlideCare()));
////		perspectiveMenu.add(new SpiritAction.Action_OpenStockCare());
//		return perspectiveMenu;
//	}
	

	public static JMenu getToolsMenu() {
		JMenu toolsMenu = new JMenu("Tools");		
		toolsMenu.setMnemonic('t');
		toolsMenu.add(new BiosampleActions.Action_Find_Duplicate_Biosamples());
		toolsMenu.add(new ResultActions.Action_Find_Duplicate_Results());
		toolsMenu.add(new AdminActions.Action_ExpiredSamples());
		toolsMenu.add(new AdminActions.Action_Revisions(SpiritFrame.getUser()==null?"": SpiritFrame.getUser().getUsername()));
		return toolsMenu;
	}
	
	public static JMenu getAdminMenu() {
		JMenu adminMenu = new JMenu("Admin");
		if( SpiritFrame.getUser()==null || SpiritFrame.getUser().isSuperAdmin()) {
			adminMenu.add(new SpiritAction.Action_DatabaseSettings());
			adminMenu.add(new AdminActions.Action_ManageUsers());
			adminMenu.add(new JSeparator());

			adminMenu.add(new AdminActions.Action_AdminBiotypes());
			adminMenu.add(new AdminActions.Action_AdminTests());
			adminMenu.add(new JSeparator());
			
			adminMenu.add(new AdminActions.Action_Revisions(null));		
			adminMenu.add(new AdminActions.Action_LastLogins());
	
			adminMenu.add(new JSeparator());						
			adminMenu.add(new AdminActions.Action_RenameElb());

			
		} else {
			adminMenu.setEnabled(false);
		}
		return adminMenu;
	}
	public static JMenu getHelpMenu(SplashConfig splashConfig) {
		JMenu helpMenu = new JMenu("Help");		
		helpMenu.setMnemonic('h');
		helpMenu.add(new SpiritAction.Action_Help());		
		helpMenu.add(SplashScreen2.createAboutAction(splashConfig));
		return helpMenu;
	}

}
