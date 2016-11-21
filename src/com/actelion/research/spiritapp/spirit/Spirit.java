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

package com.actelion.research.spiritapp.spirit;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritapp.spirit.ui.SpiritAction;
import com.actelion.research.spiritapp.spirit.ui.admin.database.DatabaseSettingsDlg;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTab;
import com.actelion.research.spiritapp.spirit.ui.exchange.ExchangeActions;
import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.home.HomeTab;
import com.actelion.research.spiritapp.spirit.ui.location.LocationActions;
import com.actelion.research.spiritapp.spirit.ui.location.LocationTab;
import com.actelion.research.spiritapp.spirit.ui.result.ResultActions;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTab;
import com.actelion.research.spiritapp.spirit.ui.study.StudyActions;
import com.actelion.research.spiritapp.spirit.ui.study.StudyTab;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritContextObserver;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritTab;
import com.actelion.research.spiritapp.spirit.ui.util.LoginDlg;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.migration.MigrationScript.FatalException;
import com.actelion.research.spiritcore.util.Config;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ArgumentParser;
import com.actelion.research.util.ui.ApplicationErrorLog;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JStatusBar;
import com.actelion.research.util.ui.SplashScreen2;
import com.actelion.research.util.ui.SplashScreen2.SplashConfig;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

/**
 * Spirit Main application
 * 
 * @author freyssj
 *
 */
public class Spirit extends JFrame implements ISpiritChangeObserver, ISpiritContextObserver {

	public static abstract class Action {
		public abstract void doAction() throws Exception;
	}

	private static SplashConfig splashConfig = new SplashScreen2.SplashConfig(Spirit.class.getResource("spirit.jpg"), "Spirit", "Spirit v." + Spirit.class.getPackage().getImplementationVersion() + "<br> (C) Actelion - J.Freyss"); 
	private static Spirit _instance = null;;
	private static List<Action> afterLoginActions = Collections.synchronizedList(new ArrayList<Action>());

	private JStatusBar statusBar;
	private final JTabbedPane tabbedPane;
	private HomeTab homeTab;
	private BiosampleTab biosampleTab;
	private LocationTab locationTab;
	private StudyTab studyTab;
	private ResultTab resultTab;

	private static SpiritUser user; 	
	
	public Spirit() {
		super("Spirit");
		
		_instance = this;
		SpiritChangeListener.register(this);
		SpiritContextListener.register(this);
		
		URL url = getClass().getResource("ico.png");
		if(url!=null) setIconImage(Toolkit.getDefaultToolkit().createImage(url));
		
		statusBar = new JStatusBar();
		tabbedPane = new JCustomTabbedPane();
		
		// Keep the same Study when we change tabs
		final ChangeListener studyListener = new ChangeListener() {
			private ISpiritTab currentTab;
			@Override
			public void stateChanged(ChangeEvent e) {
				//Reset the current Study
				String studyIds;
				if(currentTab==null) {
					studyIds = "";  
				} else {
					studyIds = currentTab.getStudyIds();
				}
				statusBar.setInfos("");
				if(tabbedPane.getSelectedComponent() instanceof ISpiritTab) {		
					currentTab = ((ISpiritTab)tabbedPane.getSelectedComponent());
					if(studyIds!=null) currentTab.setStudyIds(studyIds);
				} 
			}
		};
		tabbedPane.addChangeListener(studyListener);
	
		setContentPane(UIUtils.createBox(tabbedPane, null, statusBar));		
		createMenu();
		
		statusBar.setCopyright("Spirit - (C) Joel Freyss - Actelion");		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);				
		UIUtils.adaptSize(this, 1600, 1200);		
		setVisible(true);


		SpiritDB.check();		
		recreateUI();		
		toFront();
	}
	
	/**
	 * Recreate the menu, the tabs, and apply the actions
	 * This function can be called from outside the EventDispatcherThread
	 */
	public void recreateUI() {
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				createMenu();
				recreateTabs();						
			}
		});		
	}
	
	/**
	 * Function to be called when preferences are modified or user is logged
	 */
	private void recreateTabs() {
		try {					 
			tabbedPane.removeAll();
			homeTab = new HomeTab(Spirit.this);
			studyTab = new StudyTab();
			biosampleTab = new BiosampleTab(null);
			locationTab = new LocationTab(null);
			resultTab = new ResultTab();
			
			if(user!=null) {
				//Add the tabs
				tabbedPane.setFont(FastFont.BOLD.deriveSize(14));
				if(homeTab!=null) {
					tabbedPane.addTab("", homeTab);		
					tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.HOME.getIcon());
				}
				if(studyTab!=null) {
					tabbedPane.addTab("Studies ", studyTab);		
					tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.STUDY.getIcon());
				}
				if(biosampleTab!=null) {
					tabbedPane.addTab("Biosamples ", biosampleTab);		
					tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.BIOSAMPLE.getIcon());
				}
				if(locationTab!=null) {
					tabbedPane.addTab("Locations ", locationTab);
					tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.LOCATION.getIcon());
				}
				if(resultTab!=null) {
					tabbedPane.addTab("Results ", resultTab);
					tabbedPane.setIconAt(tabbedPane.getTabCount()-1, IconType.RESULT.getIcon());
				}
				
				//Apply actions after login
				if(!afterLoginActions.isEmpty() && user!=null) {
					new SwingWorkerExtended(tabbedPane, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
						@Override
						protected void doInBackground() throws Exception {
							while(!afterLoginActions.isEmpty()) {
								Action action = afterLoginActions.remove(0);
								try {
									action.doAction();
								} catch(Exception ex) {
									ex.printStackTrace();
								}
							}	
						}						
					};

				}
			}
		} catch(Exception e) {
			JExceptionDialog.showError(e);
			tabbedPane.removeAll();
		}
	}
	
	private void createMenu() {
		JMenuBar menuBar = new JMenuBar();		
		
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('e');
		menuBar.add(editMenu);
		editMenu.add(new StudyActions.Action_New());
		editMenu.add(new BiosampleActions.Action_NewBatch());				
		editMenu.add(new LocationActions.Action_New());
		editMenu.add(new ResultActions.Action_New());
		editMenu.add(new JSeparator());
		editMenu.add(new ExchangeActions.Action_ExportExchange(this));
		editMenu.add(new ExchangeActions.Action_ImportExchange());
		editMenu.add(new JSeparator());
		editMenu.add(new SpiritAction.Action_Refresh(this));
		editMenu.add(new SpiritAction.Action_Exit());
		
		menuBar.add(Box.createHorizontalStrut(10));
		
		menuBar.add(new JSeparator(JSeparator.VERTICAL) {
			@Override
			public Dimension getMaximumSize() {return new Dimension(20, 60);}
		});
		menuBar.add(Box.createHorizontalStrut(10));
		
		
		menuBar.add(SpiritMenu.getToolsMenu());
		

		menuBar.add(Box.createHorizontalStrut(10));
		menuBar.add(new JSeparator(JSeparator.VERTICAL) {
			@Override
			public Dimension getMaximumSize() {return new Dimension(20, 60);}
		});
		menuBar.add(Box.createHorizontalStrut(10));
		menuBar.add(SpiritMenu.getDatabaseMenu());
		
		menuBar.add(Box.createHorizontalStrut(10));
		menuBar.add(new JSeparator(JSeparator.VERTICAL) {
			@Override
			public Dimension getMaximumSize() {return new Dimension(20, 60);}
		});
		menuBar.add(Box.createHorizontalStrut(10));

		menuBar.add(SpiritMenu.getAdminMenu());	
		
		menuBar.add(Box.createHorizontalStrut(10));
		menuBar.add(new JSeparator(JSeparator.VERTICAL) {
			@Override
			public Dimension getMaximumSize() {return new Dimension(20, 60);}
		});
		menuBar.add(Box.createHorizontalStrut(10));

		menuBar.add(SpiritMenu.getHelpMenu(splashConfig));
		
		menuBar.add(Box.createHorizontalGlue());
		
		setJMenuBar(menuBar);
		
	}
	
	
	public ISpiritTab getSelectedTab() {
		return (ISpiritTab) tabbedPane.getSelectedComponent();
	}
	
	void refreshTabs() {
		for(Component c: tabbedPane.getComponents()) {
			if(c instanceof ISpiritTab) {
				((ISpiritTab) c).refreshFilters();
			}	
		}
		createMenu();
	}
	
	public static void initUI() {

  		try {
			System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");			
			UIManager.put("nimbusSelectionBackground", new Color(173,207,231));
		} catch (Exception e2) {
			e2.printStackTrace();
		}
  		
  		
	  	//ToolTop binder
		ToolTipManager.sharedInstance().setInitialDelay(750); 
		ToolTipManager.sharedInstance().setDismissDelay(20000); 
		ToolTipManager.sharedInstance().setReshowDelay(300); 		

		//Error log for Actelion
		ApplicationErrorLog.setApplication("Spirit v" + Spirit.class.getPackage().getImplementationVersion());
		ApplicationErrorLog.setActivated(DBAdapter.getAdapter().isInActelionDomain());
		
		//Add a UncaughtExceptionHandler
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {			
			@Override
			public void uncaughtException(final Thread t, final Throwable e) {
				e.printStackTrace();
				JExceptionDialog.showError(UIUtils.getMainFrame(), e);										
			}
		});		
		
		//Bind help
		HelpBinder.bindHelp();		
	}
	
	public static void preLoadDAO() throws Exception {
		JPAUtil.getManager();		
		DAOEmployee.getEmployeeGroups();
		DAOBiotype.getBiotypes();
		DAOLocation.getLocationRoots(null);
		DAOTest.getTests();
	}
	
	/**
	 * Set the user for the current Spirit context.
	 * Whereas everything under DD_Spirit works for multiple users, everything under Spirit works with this given user   
	 * @param user
	 */
	public static void setUser(SpiritUser user) {
		Spirit.user = user;		
	}
	
	public static SpiritUser getUser() {
		return Spirit.user;
	}
	
	public static String getUsername() {
		return Spirit.user==null? null: Spirit.user.getUsername();
	}

	public static SpiritUser askForAuthentication() throws Exception {
		if(Spirit.getUser()==null) {
			LoginDlg.openLoginDialog(UIUtils.getMainFrame(), "Spirit Login");
			if(Spirit.getUser()==null) throw new Exception("You must be logged in");
		}				
		return Spirit.getUser();
	}
	
	@Override
	public void setStatus(String status) {
		statusBar.setInfos(status);
	}
	
	@Override
	public void setUser(String status) {
		statusBar.setUser(status);
	}
	
	public JStatusBar getStatusBar() {
		return statusBar;
	}
		
	@Override
	public void query(final BiosampleQuery q) {
		tabbedPane.setSelectedComponent(biosampleTab);
		if(q==null) return;
		biosampleTab.query(q);
	}
	
	@Override
	public void query(final ResultQuery q) {
		tabbedPane.setSelectedComponent(resultTab);
		if(q==null) return;
		resultTab.query(q);
	}
	
	@Override
	public void setBiosamples(final List<Biosample> biosamples) {
		Set<Location> locations = Biosample.getLocations(biosamples);		
		Location loc = locations.size()==1? locations.iterator().next(): null;

		tabbedPane.setSelectedComponent(biosampleTab);
		if(loc!=null && loc.getSize()>0 && biosamples.size()==loc.getBiosamples().size() && loc.getBiosamples().containsAll(biosamples)) {
			biosampleTab.setRack(loc);
		} else {
			if(biosamples.size()==1) {
				Biosample b = biosamples.get(0);
				biosampleTab.setBiosamples(new ArrayList<>(b.getHierarchy(HierarchyMode.ALL_MAX2)));
				biosampleTab.getTableOrRackTab().setSelectedBiosamples(Collections.singletonList(b));
				
			} else {
				biosampleTab.setBiosamples(biosamples);
			}
		}
		if(biosamples!=null) statusBar.setInfos(biosamples.size()+ " biosamples");
	}
	
	@Override
	public void setRack(Location loc) {
		tabbedPane.setSelectedComponent(biosampleTab);
		biosampleTab.setRack(loc);
		if(loc!=null) statusBar.setInfos("Rack "+loc + ": "+loc.getBiosamples().size());
	}
	
	@Override
	public void setResults(final List<Result> results, final PivotTemplate template) {
		tabbedPane.setSelectedComponent(resultTab);
		resultTab.setResults(results, template);
		statusBar.setInfos(results.size()+ " results");
	}
	
	@Override
	public void setLocation(final Location location, final int pos) {
		tabbedPane.setSelectedComponent(locationTab);
		locationTab.setBioLocation(location, pos);
		if(location!=null) statusBar.setInfos(location + " selected");
	}
	
	@Override
	public void setStudy(final Study study) {
		final String studyId = study==null?"": study.getStudyId();
		studyTab.setStudyIds(studyId);
		tabbedPane.setSelectedComponent(studyTab);
		if(study!=null) statusBar.setInfos(study + " selected");
	}
	
	public static final Config getConfig() {
		return Config.getInstance(".spirit");
	}
		
	@Override
	public <T> void actionModelChanged(final SpiritChangeType action, final Class<T> w, final List<T> details) {
		new SwingWorkerExtended("Refreshing", this,  SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			@Override
			protected void done() {
				if(action==SpiritChangeType.LOGIN) {					
					recreateUI();
				} else if(w==Biotype.class || w==Test.class) {					
					//Refresh all tabs to refresh filters
					refreshTabs();
				} else {					
					Component c = tabbedPane.getSelectedComponent();
					if(c instanceof ISpiritTab) {
						((ISpiritTab) c).fireModelChanged(action, w, details);
					}
				}
			}
		};
	}
	
	/**
	 * Builds the Exchange file, based on the active view (study, biosamples, locations, results)
	 * @return
	 */
	public Exchange getExchange() {
		Exchange exchange = new Exchange();
		if(tabbedPane.getSelectedComponent()==studyTab) {
			exchange.addStudies(JPAUtil.reattach(studyTab.getStudies()));

			try {
				BiosampleQuery q = BiosampleQuery.createQueryForStudyIds(MiscUtils.flatten(Study.mapStudyId(studyTab.getStudies()).keySet(), " "));
				exchange.addBiosamples(DAOBiosample.queryBiosamples(q, Spirit.getUser()));
				
				ResultQuery q2 = ResultQuery.createQueryForStudyIds(MiscUtils.flatten(Study.mapStudyId(studyTab.getStudies()).keySet(), " "));
				exchange.addResults(DAOResult.queryResults(q2, user));
			} catch(Exception e) {
				//Should not happen
				throw new RuntimeException(e);
			}			
		} else if(tabbedPane.getSelectedComponent()==biosampleTab) {
			exchange.addBiosamples(JPAUtil.reattach(biosampleTab.getBiosamples()));
		} else if(tabbedPane.getSelectedComponent()==locationTab) {
			exchange.addLocations(JPAUtil.reattach(locationTab.getLocations()));
		} else if(tabbedPane.getSelectedComponent()==resultTab) {
			exchange.addResults(JPAUtil.reattach(resultTab.getResults()));
		} else {
			return null;
		}
		return exchange;				
	}
	
	/**
	 * Special executor for Actelion, to put Spirit toFront with the proper settings
	 * @param args
	 * @throws Exception
	 */
	public static void initSingleApplication(final String[] args) throws Exception {
		if(_instance==null) {
			main(args);
		} else {
			_instance.toFront();
		}


		//Process arguments
		final ArgumentParser argumentParser = new ArgumentParser(args);
		String studyId = argumentParser.getArgument("studyId");
		if(studyId!=null) {
			Study s = DAOStudy.getStudyByStudyId(studyId);
			LoggerFactory.getLogger(Spirit.class).info("Init with studyId=" + studyId);
			SpiritContextListener.setStudy(s);
		}
    }
	
	public static void main(final String[] args) throws Exception {
		initUI();
		SplashScreen2.show(splashConfig);				
		
		final ArgumentParser argumentParser = new ArgumentParser(args);
		try {
			argumentParser.validate("studyId");
		} catch(Exception e) {
			System.out.println("Invalid syntax: Spirit -studyId {S-######}");
			System.exit(1);
		}
		

		new SwingWorkerExtended("Starting Spirit", null, SwingWorkerExtended.FLAG_ASYNCHRONOUS) {
			private Throwable throwable = null;
					
			@Override
			protected void doInBackground() throws Exception {				
				try {					
					SpiritAction.logUsage("Spirit");					
					JPAUtil.getManager();
				} catch(Throwable e) {
					throwable = e; 
				}
			}

			@Override
			protected void done() {
				if(throwable!=null) {					
					JExceptionDialog.showError(throwable);
					if(throwable instanceof FatalException) System.exit(1);
					new DatabaseSettingsDlg(false);
				}
				Spirit spirit;
				try {
					LoggerFactory.getLogger(Spirit.class).debug("start Spirit");
					spirit = new Spirit();
					addAfterLoginAction(new Action() {
						public void doAction() throws Exception {
							initSingleApplication(args);
						}
					});
					JOptionPane.setRootFrame(spirit);					
				} catch(Throwable e) {
					JExceptionDialog.showError(e);
					new DatabaseSettingsDlg(false);
					return;
				}				
			}
			
		};		
	}

	public static void open() {
		Spirit spirit = new Spirit();
		spirit.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	public static void addAfterLoginAction(Action action) {
		afterLoginActions.add(action);
	}
	
}	
