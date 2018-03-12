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

package com.actelion.research.spiritapp.ui;

import java.awt.Component;
import java.awt.Toolkit;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
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

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTab;
import com.actelion.research.spiritapp.ui.location.LocationActions;
import com.actelion.research.spiritapp.ui.location.LocationTab;
import com.actelion.research.spiritapp.ui.result.ResultActions;
import com.actelion.research.spiritapp.ui.result.ResultTab;
import com.actelion.research.spiritapp.ui.study.StudyActions;
import com.actelion.research.spiritapp.ui.util.HelpBinder;
import com.actelion.research.spiritapp.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.ui.util.ISpiritContextObserver;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.ui.util.icons.ImageFactory;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.HierarchyMode;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.Cache;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.Config;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.ApplicationErrorLog;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JStatusBar;
import com.actelion.research.util.ui.SplashScreen;
import com.actelion.research.util.ui.SplashScreen.SplashConfig;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;

/**
 * Frame used as the top frame for all Spirit based application.
 * When the application consists of SpiritTab, each tab being responsible for one type of perspective.
 * The SpiritFrame is responsible:
 * - for handling and dispatching the event to the appropriate tab.
 * - for all specific UI events (popups)
 *
 * @author Joel Freyss
 */
public abstract class SpiritFrame extends JFrame implements ISpiritChangeObserver, ISpiritContextObserver {

	protected static SplashConfig splashConfig = new SplashScreen.SplashConfig(Spirit.class.getResource("spirit.jpg"), "Spirit", "Spirit v." + Spirit.class.getPackage().getImplementationVersion() + "<br> (C) Idorsia - J.Freyss");
	protected static SpiritFrame _instance = null;;

	protected String copyright;
	protected PopupHelper popupHelper = new PopupHelper();

	private SpiritTabbedPane tabbedPane = new SpiritTabbedPane();
	private JStatusBar statusBar;

	private Runnable afterLoginAction = null;
	private RightLevel studyLevel = RightLevel.READ;
	private boolean multipleChoices = true;


	public SpiritFrame(String title, String copyright) {
		this(title, copyright, null);
	}

	public SpiritFrame(String title, String copyright, Runnable afterLoginAction) {
		super(title);
		this.copyright = copyright;
		this.afterLoginAction = afterLoginAction;
		assert SpiritFrame._instance==null;
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

	/**
	 * Returns the unique instance of SpiritFrame (if any)
	 * @return
	 */
	public static SpiritFrame getInstance() {
		return _instance;
	}

	/**
	 * Gets the tabs associated to the SpiritFrame context
	 * @return
	 */
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

			if(getUser()!=null) {
				//Add the tabs
				tabbedPane.setFont(FastFont.BIGGER);
				List<SpiritTab> tabs = getTabs();
				if(tabs!=null) {
					for (SpiritTab tab : getTabs()) {
						tabbedPane.addTab(tab.getName(), tab);
						tabbedPane.setIconAt(tabbedPane.getTabCount()-1, tab.getIcon());
					}
				}

				//Apply actions after login
				new SwingWorkerExtended(tabbedPane, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
					@Override
					protected void doInBackground() throws Exception {
						if(afterLoginAction!=null) {
							try {
								afterLoginAction.run();
								afterLoginAction = null;
							} catch(Exception ex) {
								ex.printStackTrace();
							}
						}
					}
					@Override
					protected void done() {
						if(getSelectedTab()!=null) {
							getSelectedTab().onTabSelect();
						}
					}
				};
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
		if(SpiritProperties.getInstance().isChecked(PropertyKey.TAB_RESULT)) {
			editMenu.add(new ResultActions.Action_New());
		}
		editMenu.add(new JSeparator());

		SpiritMenu.addEditMenuItems(editMenu);

		menuBar.add(SpiritMenu.getDevicesMenu());


		menuBar.add(SpiritMenu.getToolsMenu());

		menuBar.add(SpiritMenu.getAdminMenu());

		menuBar.add(SpiritMenu.getHelpMenu(splashConfig));

		setJMenuBar(menuBar);

	}

	/**
	 * Generic function to initaiate L&F (Numbus)
	 */
	public static void initUI() {
		//ToolTop binder
		ToolTipManager.sharedInstance().setInitialDelay(750);
		ToolTipManager.sharedInstance().setDismissDelay(20000);
		ToolTipManager.sharedInstance().setReshowDelay(300);

		//Error log for Actelion/Idorsia
		try {
			ApplicationErrorLog.setApplication(DBAdapter.getInstance().getSoftwareName() + " v" + Spirit.class.getPackage().getImplementationVersion());
			ApplicationErrorLog.setActivated(DBAdapter.getInstance().isInActelionDomain());
		} catch (Exception e) {
			e.printStackTrace();
		}

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

		try {
			UIManager.setLookAndFeel(new NimbusLookAndFeel());
			FastFont.setDefaultFontSize(Spirit.getConfig().getProperty("preferences.fontSize", 12));
		} catch (Exception e) {
			e.printStackTrace();
		}

		SwingUtilities.invokeLater(() -> {
			ImageFactory.clearCache();
			IconType.clearCache();
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
		try {
			tabbedPane.push();
			BiosampleTab tab = (BiosampleTab) tabbedPane.getTab(BiosampleTab.class);
			if(q==null || tab==null || tabbedPane==null) return;
			tab.setSelectedStudyId(q.getStudyIds());
			tabbedPane.setStudyId(q.getStudyIds());
			tabbedPane.setSelectedComponent(tab);
			tab.query(q);
		} finally {
			tabbedPane.pop();
		}
	}

	@Override
	public void query(final ResultQuery q, int graphIndex) {
		try {
			tabbedPane.push();
			ResultTab tab = (ResultTab) tabbedPane.getTab(ResultTab.class);
			if(q==null || tab==null) return;
			System.out.println("SpiritFrame.query() set "+q.getStudyIds());
			tab.setSelectedStudyId(q.getStudyIds());
			tabbedPane.setStudyId(q.getStudyIds());
			tabbedPane.setSelectedComponent(tab);
			tab.query(q, graphIndex);
		} finally {
			tabbedPane.pop();
		}
	}

	@Override
	public void setResults(final List<Result> results) {
		try {
			tabbedPane.push();
			ResultTab tab = (ResultTab) tabbedPane.getTab(ResultTab.class);
			if(tab==null) return;
			tab.setSelectedStudyId(tabbedPane.getStudyId());
			tabbedPane.setSelectedComponent(tab);
			tab.setResults(results);
			statusBar.setInfos(results.size()+ " results");
		} finally {
			tabbedPane.pop();
		}
	}

	@Override
	public void setBiosamples(List<Biosample> biosamples) {
		try {
			tabbedPane.push();
			IBiosampleTab tab = (IBiosampleTab) getTabbedPane().getTab(IBiosampleTab.class);
			if(tab==null) return;
			((SpiritTab)tab).setSelectedStudyId(getTabbedPane().getStudyId());

			Set<Location> locations = Biosample.getLocations(biosamples);
			Location loc = locations.size()==1? locations.iterator().next(): null;

			if(tab instanceof SpiritTab) {
				//Avoid firing studyEvent
				((SpiritTab)tab).setSelectedStudyId(getTabbedPane().getStudyId());
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
			statusBar.setInfos(biosamples.size()+ " biosamples");
		} finally {
			tabbedPane.pop();
		}
	}

	@Override
	public void setRack(Location loc) {
		try {
			getTabbedPane().push();
			IBiosampleTab tab = (IBiosampleTab) getTabbedPane().getTab(IBiosampleTab.class);
			if(tab==null) return;
			((SpiritTab)tab).setSelectedStudyId(getTabbedPane().getStudyId());

			getTabbedPane().setSelectedComponent((Component)tab);
			tab.setRack(loc);
			if(loc!=null) statusBar.setInfos("Rack "+loc + ": "+loc.getBiosamples().size());
		} finally {
			tabbedPane.pop();
		}
	}


	@Override
	public void setLocation(final Location location, final int pos) {
		try {
			getTabbedPane().push();
			LocationTab tab = (LocationTab) getTabbedPane().getTab(LocationTab.class);
			if(tab==null) return;
			tab.setSelectedStudyId(getTabbedPane().getStudyId());

			getTabbedPane().setSelectedComponent(tab);
			tab.setBioLocation(location, pos);
			if(location!=null) statusBar.setInfos(location + " selected");
		} finally {
			tabbedPane.pop();
		}
	}

	@Override
	public void setStudy(final Study study) {
		try {
			getTabbedPane().push();
			getTabbedPane().setStudyId(study==null?"": study.getStudyId());
			IStudyTab tab = (IStudyTab) getTabbedPane().getTab(IStudyTab.class);
			if(tab==null) return;
			((SpiritTab)tab).setSelectedStudyId(getTabbedPane().getStudyId());
			tabbedPane.setSelectedComponent((Component) tab);
			tab.setStudy(study);
			if(study!=null) statusBar.setInfos(study + " selected");
		} finally {
			getTabbedPane().pop();
		}
	}

	/**
	 * Return the local config options
	 * @return
	 */
	public static final Config getConfig() {
		return Config.getInstance(".spirit");
	}

	/**
	 * Fired when the model changed, and somethings needs to be refreshed
	 */
	@Override
	public <T> void actionModelChanged(final SpiritChangeType action, final Class<T> w, final Collection<T> details) {
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
					SpiritTab tab = getSelectedTab();
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
	public Exchange getExchange() throws Exception {
		Exchange exchange = new Exchange();
		if(tabbedPane==null) return exchange;
		SpiritTab tab = (SpiritTab) tabbedPane.getSelectedComponent();
		if(tab instanceof IStudyTab) {
			List<Study> studies = JPAUtil.reattach(((IStudyTab)tab).getStudies());
			if(studies.size()>20) studies = JPAUtil.reattach(Collections.singleton(((IStudyTab)tab).getStudy()));
			exchange.addStudies(studies);

			try {
				BiosampleQuery q = BiosampleQuery.createQueryForStudyIds(MiscUtils.flatten(Study.mapStudyId(studies).keySet(), " "));
				exchange.addBiosamples(DAOBiosample.queryBiosamples(q, getUser()));

				ResultQuery q2 = ResultQuery.createQueryForStudyIds(MiscUtils.flatten(Study.mapStudyId(studies).keySet(), " "));
				exchange.addResults(DAOResult.queryResults(q2, getUser()));
			} catch(Exception e) {
				//Should not happen
				throw new RuntimeException(e);
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


	/**
	 * Returns the username
	 * @return
	 */
	public static String getUsername() {
		return getUser()==null? null: getUser().getUsername();
	}

	/**
	 * Returns the current logged in user (only one can be logged per instance)
	 * @return
	 */
	public static SpiritUser getUser() {
		return JPAUtil.getSpiritUser();
	}


	/**
	 * Set the user for the current Spirit context.
	 * Whereas everything under DD_Spirit works for multiple users, everything under Spirit works with this given user
	 * @param user
	 */
	public static void setUser(SpiritUser user) {
		JPAUtil.setSpiritUser(user);
	}

	public void setStudyId(String studyId) {
		if(studyId!=null && studyId.equals(tabbedPane.getStudyId())) return;
		tabbedPane.setStudyId(studyId);
		if(getSelectedTab()!=null) {
			getSelectedTab().onStudySelect();
		}
	}

	public final static String getStudyId() {
		return getStudy()==null? null: getStudy().getStudyId();
	}

	public final static Study getStudy() {
		return _instance==null? null: DAOStudy.getStudyByStudyId(_instance.getTabbedPane().getStudyId());
	}

	public static void clearAll() {
		SwingWorkerExtended.awaitTermination();
		JPAUtil.clearAll();
		Cache.getInstance().clear();
	}

	public PopupHelper getPopupHelper() {
		return popupHelper;
	}
}
