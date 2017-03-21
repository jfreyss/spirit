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

package com.actelion.research.spiritapp.animalcare;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import com.actelion.research.spiritapp.animalcare.ui.AnimalCareTab;
import com.actelion.research.spiritapp.animalcare.ui.DashboardTab;
import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.SpiritTab;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTab;
import com.actelion.research.spiritapp.spirit.ui.location.LocationTab;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTab;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritAction;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SplashScreen2;
import com.actelion.research.util.ui.SplashScreen2.SplashConfig;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class AnimalCare extends SpiritFrame {

	private static SplashConfig splashConfig = new SplashConfig(AnimalCare.class.getResource("animalcare.jpg"), "AnimalCare", "AnimalCare v" + Spirit.class.getPackage().getImplementationVersion() + "<br> (C) Actelion - J.Freyss");

	public AnimalCare() {
		super("AnimalCare", "AnimalCare - (C) Joel Freyss - Actelion");	
		setStudyLevel(RightLevel.BLIND, false);
		
	}
	
	@Override
	public List<SpiritTab> getTabs() {
		List<SpiritTab> tabs = new ArrayList<>();
		tabs.add(new DashboardTab(this));
		tabs.add(new AnimalCareTab(this));
		tabs.add(new BiosampleTab(this));
		tabs.add(new LocationTab(this));
		tabs.add(new ResultTab(this));
		return tabs;
	}
	
//	@Override
//	public <T> void actionModelChanged(final SpiritChangeType action, final Class<T> w, final List<T> details) {
//		new SwingWorkerExtended("Refreshing", this,  SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
//			@Override
//			protected void done() {
//				if(action==SpiritChangeType.LOGIN) {					
////					recreateUI();
//				} else if(w==Biotype.class || w==Test.class) {					
//					//Refresh all tabs to refresh filters
////					refreshTabs();
//				} else {					
//					
//					//Switch to appropriate tab
//					if(w==Study.class) {
//						tabbedPane.setSelectedComponent(studyTab);
//					} else if(w==Biosample.class) {
//						tabbedPane.setSelectedComponent(biosampleTab);
//					} else if(w==Location.class) {
//						tabbedPane.setSelectedComponent(locationTab);
//					} else if(w==Result.class) {
//						tabbedPane.setSelectedComponent(resultTab);
//					}
//					 
//					//Fire event
//					Component c = tabbedPane.getSelectedComponent();					
//					if(c instanceof SpiritTab) {
//						((SpiritTab) c).fireModelChanged(action, w, details);
//					}
//				}
//			}
//		};
//	}
	
//	public void eventUserChanged() {
//		if(SpiritFrame.getUser()!=null) {
//			statusBar.setUser("Logged in as " + SpiritFrame.getUser().getUsername() + " - " + (SpiritFrame.getUser().getMainGroup()!=null? SpiritFrame.getUser().getMainGroup().getName():""));
//		}
//		dashboardTab.refresh();
//		studyTab.refresh();
//	}	

	
	
	public static void main(String[] args) {
		

		SplashScreen2.show(splashConfig);

		new SwingWorkerExtended() {			
			@Override
			protected void doInBackground() throws Exception {
				try {
					SpiritAction.logUsage("AnimalCare");					
					JPAUtil.getManager();
				} catch(Exception e) {
					JExceptionDialog.showError(e);
					System.exit(1);	
				}
			}			
			@Override
			protected void done() {
				Spirit.initUI();
				new AnimalCare();
			}
		};
		
	}
	
	public static void open() {
		AnimalCare animalCare = new AnimalCare();
		animalCare.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//		animalCare.eventUserChanged();		
	}
	
	

}
