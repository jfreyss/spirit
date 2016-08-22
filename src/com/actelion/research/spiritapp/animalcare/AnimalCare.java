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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;

import com.actelion.research.spiritapp.animalcare.ui.AnimalCarePanel;
import com.actelion.research.spiritapp.animalcare.ui.DashboardPanel;
import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.SpiritMenu;
import com.actelion.research.spiritapp.spirit.ui.SpiritAction;
import com.actelion.research.spiritapp.spirit.ui.study.StudyActions;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritContextObserver;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JStatusBar;
import com.actelion.research.util.ui.SplashScreen2;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.SplashScreen2.SplashConfig;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class AnimalCare extends JFrame implements ISpiritChangeObserver, ISpiritContextObserver {

	private static SplashConfig splashConfig = new SplashConfig(AnimalCare.class.getResource("animalcare.jpg"), "AnimalCare", "AnimalCare v" + Spirit.class.getPackage().getImplementationVersion() + "<br> (C) Actelion - J.Freyss");
	
	private final JTabbedPane centerPane = new JCustomTabbedPane();
	private final AnimalCarePanel animalCarePanel = new AnimalCarePanel();
	private final DashboardPanel dashboardPanel = new DashboardPanel();
	private final JStatusBar statusBar = new JStatusBar();

	
	public AnimalCare() {
		super("AnimalCare");
		URL url = getClass().getResource("ico.png");
		if(url!=null) setIconImage(Toolkit.getDefaultToolkit().createImage(url));
		
		SpiritChangeListener.register(this);
		SpiritContextListener.register(this);

		//Menu
		JMenuBar menuBar = new JMenuBar();
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('e');
		editMenu.add(new StudyActions.Action_New());
		editMenu.add(new Action_Refresh());
		menuBar.add(editMenu);
		
		menuBar.add(SpiritMenu.getToolsMenu());		
		menuBar.add(SpiritMenu.getDatabaseMenu());		
		menuBar.add(SpiritMenu.getAdminMenu());
		menuBar.add(SpiritMenu.getHelpMenu(splashConfig));
		
		setJMenuBar(menuBar);
		

		statusBar.setCopyright("AnimalCare - (C) Joel Freyss - Actelion 2012");	

		centerPane.setFont(FastFont.BOLD.deriveSize(14));
		centerPane.add("Dashboard ", dashboardPanel);
		centerPane.setIconAt(0, IconType.HOME.getIcon());

		centerPane.add("Study ", animalCarePanel);
		centerPane.setIconAt(1, IconType.STUDY.getIcon());
		
		
		//ContenPanel
		setContentPane(UIUtils.createBox(centerPane, null, statusBar));

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
   		setSize(dim.width-50, dim.height-100);
		UIUtils.adaptSize(this, 1600, 1200);		
   		setLocationRelativeTo(null);
   		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
   		
   		//Login
		if(System.getProperty("user.name").equals("freyssj")) {
			try {Spirit.setUser(DAOSpiritUser.loadUser("freyssj"));} catch (Exception e) {}
			eventUserChanged();
		} 
				
		if(Spirit.getUser()==null) {
			new SpiritAction.Action_Relogin(AnimalCare.this, "AnimalCare").actionPerformed(null);
			if(Spirit.getUser()==null) System.exit(1);

		}		
		
		
	}
	
	@Override
	public <T> void actionModelChanged(SpiritChangeType action, Class<T> what, List<T> details) {
		
		if(action==SpiritChangeType.LOGIN) {
			eventUserChanged();
		} else {
			animalCarePanel.refresh();
		}
	}
	
	public void eventUserChanged() {
		if(Spirit.getUser()!=null) {
			statusBar.setUser("Logged in as " + Spirit.getUser().getUsername() + " - " + (Spirit.getUser().getMainGroup()!=null? Spirit.getUser().getMainGroup().getName():""));
		}
		dashboardPanel.refresh();
		animalCarePanel.refresh();
	}	

	
	
	public static void main(String[] args) {
		
		Spirit.initUI();

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
				new AnimalCare();
			}
		};
		
	}
	
	public static void open() {
		AnimalCare animalCare = new AnimalCare();
		animalCare.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		animalCare.eventUserChanged();		
	}

	
	public class Action_Refresh extends AbstractAction {
		public Action_Refresh() {
			super("Refresh");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('r'));
			putValue(AbstractAction.SMALL_ICON, IconType.REFRESH.getIcon());

		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				JPAUtil.refresh();
				eventUserChanged();
			} catch(Exception ex) {
				ex.printStackTrace();				
			}
		}
	}


	@Override
	public void setStudy(Study study) {
		centerPane.setSelectedIndex(1);
		animalCarePanel.setStudy(study);
	}

	@Override
	public void setBiosamples(List<Biosample> biosamples) {
	}
	@Override
	public void setRack(Location rack) {
	}
	@Override
	public void setLocation(Location location, int pos) {
	}

	@Override
	public void setResults(List<Result> results, PivotTemplate template) {
	}

	@Override
	public void query(BiosampleQuery q) {
	}

	@Override
	public void query(ResultQuery q) {
	}

	@Override
	public void setStatus(String status) {
		statusBar.setInfos(status);
	}

	@Override
	public void setUser(String status) {
		statusBar.setUser(status);
	}

	

}
