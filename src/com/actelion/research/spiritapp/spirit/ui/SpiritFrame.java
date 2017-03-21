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

import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.SpiritDB;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTab;
import com.actelion.research.spiritapp.spirit.ui.exchange.ExchangeActions;
import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.icons.ImageFactory;
import com.actelion.research.spiritapp.spirit.ui.location.LocationActions;
import com.actelion.research.spiritapp.spirit.ui.location.LocationTab;
import com.actelion.research.spiritapp.spirit.ui.result.ResultActions;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTab;
import com.actelion.research.spiritapp.spirit.ui.study.StudyActions;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritContextObserver;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.Config;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.ApplicationErrorLog;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JStatusBar;
import com.actelion.research.util.ui.SplashScreen2;
import com.actelion.research.util.ui.SplashScreen2.SplashConfig;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;

/**
 * Frame used to build all Spirit based application. when the application consists of SpiritTab, each tab being responsible for one type of perspective.
 * The SpiritFrame is responsible for handling and dispatching the event to the appropriate tab.
 *
 * @author Joel Freyss
 */
public abstract class SpiritFrame extends JFrame implements ISpiritChangeObserver, ISpiritContextObserver {

	protected static SplashConfig splashConfig = new SplashScreen2.SplashConfig(Spirit.class.getResource("spirit.jpg"), "Spirit", "Spirit v." + Spirit.class.getPackage().getImplementationVersion() + "<br> (C) Actelion - J.Freyss");
	protected String copyright;
	private SpiritTabbedPane tabbedPane;

	private JStatusBar statusBar;
	private static SpiritUser user;
	private Runnable afterLoginAction = null;
	private RightLevel studyLevel = RightLevel.READ;
	private boolean multipleChoices = true;

	protected static SpiritFrame _instance = null;;

	public SpiritFrame(String title, String copyright) {
		this(title, copyright, null);
	}

	public SpiritFrame(String title, String copyright, Runnable afterLoginAction) {
		super(title);
		this.copyright = copyright;
		this.afterLoginAction = afterLoginAction;
		SpiritFrame._instance = this;

		SpiritChangeListener.register(this);
		SpiritContextListener.register(this);

		URL url = getClass().getResource("ico.png");
		if(url!=null) setIconImage(Toolkit.getDefaultToolkit().createImage(url));

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		UIUtils.adaptSize(this, 1920, 1080);
		setVisible(true);

		SpiritDB.checkAndLogin();

		recreateUI();
		toFront();
	}

	public abstract List<SpiritTab> getTabs();


	public void setStudyLevel(RightLevel studyLevel, boolean multipleChoices) {
		this.studyLevel = studyLevel;
		this.multipleChoices = multipleChoices;
		tabbedPane.setStudyVisible(studyLevel!=null);
		tabbedPane.setStudyLevel(studyLevel, multipleChoices);
	}

	public SpiritTab getSelectedTab() {
		if(!(tabbedPane.getSelectedComponent() instanceof SpiritTab)) return null;
		return (SpiritTab) tabbedPane.getSelectedComponent();
	}


	/**
	 * Set the UI preferences, recreate the menu, the tabs
	 * This function can be called from outside the EventDispatcherThread
	 */
	public void recreateUI() {
		if(!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(()-> {
				recreateUI();
			});
			return;
		}
		initUI();
		recreateTabs();

		int index = tabbedPane.getSelectedIndex();
		String user = statusBar==null? "": statusBar.getUser();
		statusBar = new JStatusBar();
		statusBar.setCopyright(copyright);
		statusBar.setUser(user);
		setContentPane(UIUtils.createBox(tabbedPane, null, statusBar));
		createMenu();
		tabbedPane.setSelectedIndex(index);
		setStudyLevel(studyLevel, multipleChoices);
		validate();
	}

	/**
	 * Function to be called when preferences are modified or user is logged
	 */
	private void recreateTabs() {
		try {
			tabbedPane = new SpiritTabbedPane();

			if(user!=null) {
				//Add the tabs
				tabbedPane.setFont(FastFont.BIGGER);
				for (SpiritTab tab : getTabs()) {
					tabbedPane.addTab(tab.getName(), tab);
					tabbedPane.setIconAt(tabbedPane.getTabCount()-1, tab.getIcon());
				}

				//Apply actions after login
				System.out.println("SpiritFrame.recreateTabs() afterLoginAction= "+ afterLoginAction);
				if(afterLoginAction!=null && user!=null) {
					new SwingWorkerExtended(tabbedPane, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
						@Override
						protected void doInBackground() throws Exception {
							System.out.println("SpiritFrame.recreateTabs() APPLY "+ afterLoginAction);
							try {
								afterLoginAction.run();
								afterLoginAction = null;
							} catch(Exception ex) {
								ex.printStackTrace();
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

	public SpiritTabbedPane getTabbedPane() {
		return tabbedPane;
	}

	public void createMenu() {
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

		SpiritMenu.addEditMenuItems(editMenu, this);

		menuBar.add(SpiritMenu.getDevicesMenu());


		menuBar.add(SpiritMenu.getToolsMenu());

		//		menuBar.add(SpiritMenu.getPerspectivesMenu());

		menuBar.add(SpiritMenu.getAdminMenu());

		menuBar.add(SpiritMenu.getHelpMenu(splashConfig));

		setJMenuBar(menuBar);

	}

	public static void initUI() {
		try {
			System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
			System.setProperty("awt.useSystemAAFontSettings","on");
			System.setProperty("swing.aatext", "true");

			UIManager.put("nimbusSelectionBackground", new Color(173,207,231));
			UIManager.put("Table.alternateRowColor", UIManager.getColor("Table.background"));
			UIManager.put("Table.background", Color.WHITE);
			//			UIManager.setLookAndFeel(new SubstanceDustLookAndFeel());
			// 			listUIProperties();
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
				System.out.println("SpiritFrame.uncaughtException()");
				e.printStackTrace();
				JExceptionDialog.showError(UIUtils.getMainFrame(), e);
			}
		});

		//Bind help
		HelpBinder.bindHelp();

		try {
			//			String lf = Spirit.getConfig().getProperty("preferences.lf", "Nimbus");
			//			if(lf.equalsIgnoreCase("System")) {
			//				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			//			} else {
			UIManager.setLookAndFeel(new NimbusLookAndFeel());
			//			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				//				FastFont.setDefaultFontSize(Spirit.getConfig().getProperty("preferences.fontSize", FastFont.getDefaultFontSize()));
				//				FastFont.setDefaultFontFamily(Spirit.getConfig().getProperty("preferences.fontFamily", FastFont.getDefaultFontFamily()));
				ImageFactory.clearCache();
				IconType.clearCache();
			}
		});
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
		BiosampleTab tab = (BiosampleTab) tabbedPane.getTab(BiosampleTab.class);
		if(q==null || tab==null || tabbedPane==null) return;
		tab.setSelectedStudyId(q.getStudyIds());
		tabbedPane.setStudyId(q.getStudyIds());
		tabbedPane.setSelectedComponent(tab);
		tab.query(q);
	}

	@Override
	public void query(final ResultQuery q, int graphIndex) {
		ResultTab tab = (ResultTab) tabbedPane.getTab(ResultTab.class);
		if(q==null || tab==null || tabbedPane==null) return;
		System.out.println("SpiritFrame.query() set "+q.getStudyIds());
		tab.setSelectedStudyId(q.getStudyIds());
		tabbedPane.setStudyId(q.getStudyIds());
		tabbedPane.setSelectedComponent(tab);
		tab.query(q, graphIndex);
	}

	@Override
	public void setResults(final List<Result> results) {
		ResultTab tab = (ResultTab) tabbedPane.getTab(ResultTab.class);
		if(tab==null || tabbedPane==null) return;
		tab.setSelectedStudyId(tabbedPane.getStudyId());
		tabbedPane.setSelectedComponent(tab);
		tab.setResults(results);
		statusBar.setInfos(results.size()+ " results");
	}

	@Override
	public void setBiosamples(final List<Biosample> biosamples) {
		IBiosampleTab tab = (IBiosampleTab) tabbedPane.getTab(IBiosampleTab.class);
		if(tab==null || tabbedPane==null) return;
		((SpiritTab)tab).setSelectedStudyId(tabbedPane.getStudyId());

		Set<Location> locations = Biosample.getLocations(biosamples);
		Location loc = locations.size()==1? locations.iterator().next(): null;

		if(tab instanceof SpiritTab) {
			//Avoid firing studyEvent
			((SpiritTab)tab).setSelectedStudyId(tabbedPane.getStudyId());
		}

		tabbedPane.setSelectedComponent((Component)tab);
		if(loc!=null && loc.getSize()>0 && biosamples.size()==loc.getBiosamples().size() && loc.getBiosamples().containsAll(biosamples)) {
			tab.setRack(loc);
		} else {
			if(biosamples.size()==1) {
				Biosample b = biosamples.get(0);
				tab.setBiosamples(new ArrayList<>(b.getHierarchy(HierarchyMode.ALL_MAX2)));

			} else {
				tab.setBiosamples(biosamples);
			}
			tab.setSelectedBiosamples(biosamples);
		}
		if(biosamples!=null) statusBar.setInfos(biosamples.size()+ " biosamples");
	}

	@Override
	public void setRack(Location loc) {
		IBiosampleTab tab = (IBiosampleTab) tabbedPane.getTab(IBiosampleTab.class);
		if(tab==null || tabbedPane==null) return;
		((SpiritTab)tab).setSelectedStudyId(tabbedPane.getStudyId());

		tabbedPane.setSelectedComponent((Component)tab);
		tab.setRack(loc);
		if(loc!=null) statusBar.setInfos("Rack "+loc + ": "+loc.getBiosamples().size());
	}


	@Override
	public void setLocation(final Location location, final int pos) {
		LocationTab tab = (LocationTab) tabbedPane.getTab(LocationTab.class);
		if(tab==null || tabbedPane==null) return;
		tab.setSelectedStudyId(tabbedPane.getStudyId());

		if(tabbedPane==null) return;
		tabbedPane.setSelectedComponent(tab);
		tab.setBioLocation(location, pos);
		if(location!=null) statusBar.setInfos(location + " selected");
	}

	@Override
	public void setStudy(final Study study) {
		tabbedPane.setStudyId(study==null?"": study.getStudyId());

		IStudyTab tab = (IStudyTab) tabbedPane.getTab(IStudyTab.class);
		if(tab==null || tabbedPane==null) return;
		((SpiritTab)tab).setSelectedStudyId(tabbedPane.getStudyId());

		tabbedPane.setSelectedComponent((Component) tab);
		tab.setStudy(study);
		if(study!=null) statusBar.setInfos(study + " selected");
	}

	public static final Config getConfig() {
		return Config.getInstance(".spirit");
	}

	@Override
	public <T> void actionModelChanged(final SpiritChangeType action, final Class<T> w, final List<T> details) {
		LoggerFactory.getLogger(getClass()).debug("changed " + action + " " + (w==null?"":w.getName()) + " n="+(details==null? 0: details.size()));
		new SwingWorkerExtended("Refreshing", this,  SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
			@Override
			protected void done() {
				if(action==SpiritChangeType.LOGIN) {
					recreateUI();
				} else if(w==Biotype.class || w==Test.class) {
					//Refresh all tabs to refresh filters
					for (Component c : tabbedPane.getComponents()) {
						if(c instanceof SpiritTab) {
							((SpiritTab) c).fireModelChanged(action, w, details);
						}
					}
				} else {
					//Switch to appropriate tab
					SpiritTab tab = null;
					if(w==Study.class) {
						tab = tabbedPane.getTab(IStudyTab.class);
					} else if(w==Biosample.class) {
						tab = tabbedPane.getTab(IBiosampleTab.class);
					} else if(w==Location.class) {
						tab = tabbedPane.getTab(LocationTab.class);
					} else if(w==Result.class) {
						tab = tabbedPane.getTab(ResultTab.class);
					}

					if(tab!=null) {
						//Prevent the event onStudyTab to be fired
						tab.setSelectedStudyId(tabbedPane.getStudyId());

						//Select component and fire modelChanged event
						tabbedPane.setSelectedComponent(tab);
						tab.fireModelChanged(action, w, details);
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
		if(tabbedPane==null) return exchange;
		SpiritTab tab = (SpiritTab) tabbedPane.getSelectedComponent();
		if(tab instanceof IStudyTab) {
			Study study = ((IStudyTab)tab).getStudy();
			if(study!=null) {
				List<Study> studies = JPAUtil.reattach(Collections.singletonList(study));
				exchange.addStudies(studies);

				try {
					BiosampleQuery q = BiosampleQuery.createQueryForStudyIds(MiscUtils.flatten(Study.mapStudyId(studies).keySet(), " "));
					exchange.addBiosamples(DAOBiosample.queryBiosamples(q, SpiritFrame.getUser()));

					ResultQuery q2 = ResultQuery.createQueryForStudyIds(MiscUtils.flatten(Study.mapStudyId(studies).keySet(), " "));
					exchange.addResults(DAOResult.queryResults(q2, user));
				} catch(Exception e) {
					//Should not happen
					throw new RuntimeException(e);
				}
			}
		} else if(tab instanceof IBiosampleTab) {
			exchange.addBiosamples(JPAUtil.reattach(((IBiosampleTab)tab).getBiosamples()));
		} else if(tab instanceof LocationTab) {
			exchange.addLocations(JPAUtil.reattach(((LocationTab)tab).getLocations()));
		} else if(tab instanceof ResultTab) {
			exchange.addResults(JPAUtil.reattach(((ResultTab)tab).getResults()));
		} else {
			return null;
		}
		return exchange;
	}


	public static String getUsername() {
		return user==null? null: user.getUsername();
	}


	public static SpiritUser getUser() {
		return user;
	}


	/**
	 * Set the user for the current Spirit context.
	 * Whereas everything under DD_Spirit works for multiple users, everything under Spirit works with this given user
	 * @param user
	 */
	public static void setUser(SpiritUser user) {
		SpiritFrame.user = user;
	}

	public void setStudyId(String studyId) {
		if(studyId!=null && studyId.equals(tabbedPane.getStudyId())) return;
		tabbedPane.setStudyId(studyId);
		if(getSelectedTab()!=null) {
			getSelectedTab().onStudySelect();
		}
	}

	public String getStudyId() {
		return tabbedPane.getStudyId();
	}

	public Study getStudy() {
		return DAOStudy.getStudyByStudyId(tabbedPane.getStudyId());
	}



}
