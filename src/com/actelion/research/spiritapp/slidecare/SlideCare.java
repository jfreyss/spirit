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

package com.actelion.research.spiritapp.slidecare;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.spiritapp.slidecare.ui.ContainerCreatorDlg;
import com.actelion.research.spiritapp.slidecare.ui.InventoryPanel;
import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.SpiritMenu;
import com.actelion.research.spiritapp.spirit.ui.lf.StudyComboBox;
import com.actelion.research.spiritapp.spirit.ui.study.StudyDetailPanel;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritContextObserver;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritAction;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JStatusBar;
import com.actelion.research.util.ui.SplashScreen2;
import com.actelion.research.util.ui.SplashScreen2.SplashConfig;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;

public class SlideCare extends JFrame implements ISpiritChangeObserver, ISpiritContextObserver {

	private static SplashConfig splashConfig = new SplashConfig(SlideCare.class.getResource("slidecare.jpg"), "SlideCare", "SlideCare v" + Spirit.class.getPackage().getImplementationVersion() + "<br> (C) Actelion - J.Freyss");
	private StudyComboBox studyComboBox = new StudyComboBox(RightLevel.ADMIN);

	private StudyDetailPanel studyDetailPanel = new StudyDetailPanel(JSplitPane.VERTICAL_SPLIT);
	private JButton createCassetteButton = new JButton(new Action_CreateCassette());
	private JButton createSlideButton = new JButton(new Action_CreateSlide());

	private JTabbedPane tabbedPane = new JCustomTabbedPane();
	private InventoryPanel cassettePanel = new InventoryPanel(ContainerType.K7) {
		@Override
		public JComponent createEastPanel() {
			return createCassetteButton;
		}
	};
	private InventoryPanel slidePanel = new InventoryPanel(ContainerType.SLIDE) {
		@Override
		public JComponent createEastPanel() {
			return createSlideButton;
		}
	};
	private JStatusBar statusBar = new JStatusBar();


	public SlideCare() {
		super("SlideCare");

		URL url = getClass().getResource("ico.png");
		if(url!=null) setIconImage(Toolkit.getDefaultToolkit().createImage(url));


		SpiritChangeListener.register(this);
		SpiritContextListener.register(this);

		//TopPanel
		JPanel studySelecterPanel = UIUtils.createHorizontalBox(new JLabel("Study: "), studyComboBox);
		studySelecterPanel.setOpaque(true);


		JPanel topPane = new JPanel(new BorderLayout());
		topPane.add(BorderLayout.WEST, studySelecterPanel);
		topPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(5,5,5,5)));


		//TabbedPane
		tabbedPane.setFont(FastFont.BOLD);
		tabbedPane.add("Study Design", studyDetailPanel);
		tabbedPane.setIconAt(0, IconType.STUDY.getIcon());
		tabbedPane.add("Embedding", cassettePanel);
		tabbedPane.setIconAt(1, new ImageIcon(ContainerType.K7.getImage(22)));
		tabbedPane.add("Sectioning", slidePanel);
		tabbedPane.setIconAt(2, new ImageIcon(ContainerType.SLIDE.getImage(22)));
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				eventTabChanged();
			}
		});


		//StatusBar
		statusBar.setCopyright("SlideCare - (C) Joel Freyss - Actelion 2013");


		//contentPanel
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.NORTH, topPane);
		contentPanel.add(BorderLayout.CENTER, tabbedPane);
		contentPanel.add(BorderLayout.SOUTH, statusBar);
		setContentPane(contentPanel);

		UIUtils.adaptSize(this, 1600, 1200);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


		initMenu();

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


		studyComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(studyComboBox.getText()!=null)  Spirit.getConfig().setProperty("slideCare.study", studyComboBox.getText());
				eventTabChanged();
			}
		});



		setVisible(true);

		//Login
		if(System.getProperty("user.name").equals("freyssj")) {
			try {
				final SpiritUser user = DAOSpiritUser.loadUser("freyssj");
				if(user==null) throw new Exception("Could not load user birkood1");
				SpiritFrame.setUser(user);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						eventUserChanged();
					}
				});
			} catch (Exception e) {
				System.err.println(e);
			}
		}

		if(SpiritFrame.getUser()==null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					new SpiritAction.Action_Relogin(SlideCare.this, "SlideCare").actionPerformed(null);
					if(SpiritFrame.getUser()==null) System.exit(1);

					statusBar.setUser(SpiritFrame.getUser().getUsername()+ " logged in");
				}
			});
		}
		setPreferredSize(new Dimension(300, 200));

	}
	private void eventUserChanged() {
		studyComboBox.setText(Spirit.getConfig().getProperty("slideCare.study", ""));
		eventTabChanged();
		updateStatus();
	}
	public void updateStatus() {

		String userStatus;
		if(SpiritFrame.getUser()==null) {
			userStatus = "No user logged in";
		} else {
			userStatus = SpiritFrame.getUser().getUsername() + " ("+ (SpiritFrame.getUser().getMainGroup()==null?"NoDept":SpiritFrame.getUser().getMainGroup().getName())+ ") logged in";
		}
		statusBar.setUser(userStatus);

	}

	private InventoryPanel lastSelectedInventoryPanel;
	private void eventTabChanged() {

		statusBar.setInfos("");
		Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
		if(tabbedPane.getSelectedIndex()==0) {
			studyDetailPanel.setStudy(study);
		} else if(tabbedPane.getSelectedIndex()==1) {
			cassettePanel.updateFilters(study, lastSelectedInventoryPanel);
			lastSelectedInventoryPanel = cassettePanel;
		} else if(tabbedPane.getSelectedIndex()==2) {
			slidePanel.updateFilters(study, lastSelectedInventoryPanel);
			lastSelectedInventoryPanel = slidePanel;
		}
	}

	public void initMenu() {
		JMenuBar menuBar = new JMenuBar();

		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('e');
		editMenu.add(new Action_CreateCassette());
		editMenu.add(new Action_CreateSlide());
		SpiritMenu.addEditMenuItems(editMenu, null);
		menuBar.add(editMenu);

		menuBar.add(SpiritMenu.getDevicesMenu());
		menuBar.add(SpiritMenu.getToolsMenu());
		menuBar.add(SpiritMenu.getAdminMenu());
		menuBar.add(SpiritMenu.getHelpMenu(splashConfig));

		setJMenuBar(menuBar);

	}

	public class Action_CreateCassette extends AbstractAction {
		public Action_CreateCassette() {
			super("Create Cassettes");
			putValue(AbstractAction.MNEMONIC_KEY, (int)'c');
			putValue(AbstractAction.SMALL_ICON, new ImageIcon(ContainerType.K7.getImage(22)));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
				if(study==null || !SpiritRights.canAdmin(study, SpiritFrame.getUser())) throw new Exception("You must select a study");
				new ContainerCreatorDlg(study, ContainerType.K7);
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}

		}
	}

	public class Action_CreateSlide extends AbstractAction {
		public Action_CreateSlide() {
			super("Create Slides");
			putValue(AbstractAction.MNEMONIC_KEY, (int)'s');
			putValue(AbstractAction.SMALL_ICON, new ImageIcon(ContainerType.SLIDE.getImage(22)));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
				if(study==null || !SpiritRights.canAdmin(study, SpiritFrame.getUser())) throw new Exception("You must select a study");
				new ContainerCreatorDlg(study, ContainerType.SLIDE);
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}



	public static void main(String[] args) {

		Spirit.initUI();
		SplashScreen2.show(splashConfig);


		new SwingWorkerExtended() {

			@Override
			protected void doInBackground() throws Exception {
				SpiritAction.logUsage("SlideCare");
				JPAUtil.getManager();
			}

			@Override
			protected void done() {
				Spirit.initUI();
				new SlideCare();
			}
		};
	}

	@Override
	public <T> void actionModelChanged(SpiritChangeType action, Class<T> what, List<T> details) {
		if(action==SpiritChangeType.LOGIN) {
			eventUserChanged();
		} else {
			eventTabChanged();
		}
	}

	@Override
	public void setStudy(Study study) {
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
	public void setResults(List<Result> results) {
	}
	@Override
	public void query(BiosampleQuery q) {
	}
	@Override
	public void query(ResultQuery q, int graphIndex) {
	}
	@Override
	public void setStatus(String status) {
		statusBar.setInfos(status);
	}
	@Override
	public void setUser(String status) {
		statusBar.setUser(status);
	}

	public static void open() {
		SlideCare app = new SlideCare();
		app.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		app.eventUserChanged();
	}


}
