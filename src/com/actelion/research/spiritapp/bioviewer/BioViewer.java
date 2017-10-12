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

package com.actelion.research.spiritapp.bioviewer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFrame;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.SpiritTab;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTab;
import com.actelion.research.spiritapp.spirit.ui.location.LocationTab;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTab;
import com.actelion.research.spiritapp.spirit.ui.study.StudyTab;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritAction;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.SplashScreen;
import com.actelion.research.util.ui.SplashScreen.SplashConfig;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class BioViewer extends SpiritFrame {

	private static SplashConfig splashConfig = new SplashConfig(BioViewer.class.getResource("bioviewer.jpg"), "BioViewer", "BioViewer v" + Spirit.class.getPackage().getImplementationVersion() + "<br> (C) Actelion - J.Freyss");

	public BioViewer() {
		super("BioViewer", "BioViewer - (C) Joel Freyss - Idorsia Pharmaceuticals Ltd");

	}

	@Override
	public List<SpiritTab> getTabs() {
		List<SpiritTab> tabs = new ArrayList<>();
		tabs.add(new BioViewerTab(this));
		tabs.add(new StudyTab(this));
		tabs.add(new BiosampleTab(this));
		tabs.add(new LocationTab(this));
		tabs.add(new ResultTab(this));
		return tabs;
	}


	final AbstractAction LOGIN_ACTION = new SpiritAction.Action_Relogin(this, "BioViewer");

	public void eventUserChanged() {
		LOGIN_ACTION.actionPerformed(null);
	}
	//
	//	public void createMenu() {
	//		final JMenuBar menuBar = new JMenuBar();
	//
	//		JMenu edit = new JMenu("Edit");
	//		edit.setMnemonic('e');
	//		menuBar.add(edit);
	//		JMenuItem selectAll = new JMenuItem("Select All");
	//		selectAll.setAccelerator(KeyStroke.getKeyStroke("ctrl released A"));
	//		selectAll.setMnemonic('a');
	//		selectAll.addActionListener(new ActionListener() {
	//			@Override
	//			public void actionPerformed(ActionEvent e) {
	//				biosampleTab.getBiosampleTable().selectAll();
	//				selectionChanged();
	//			}
	//		});
	//		edit.add(selectAll);
	//
	//		JMenuItem clearAll = new JMenuItem(new ClearAction());
	//		clearAll.setMnemonic('c');
	//		edit.add(clearAll);
	//		JMenuItem refreshAll = new JMenuItem("Refresh", IconType.REFRESH.getIcon());
	//		refreshAll.setMnemonic('r');
	//		refreshAll.setAccelerator(KeyStroke.getKeyStroke("F5"));
	//		refreshAll.addActionListener(new ActionListener() {
	//			@Override
	//			public void actionPerformed(ActionEvent e) {
	//				refresh();
	//			}
	//		});
	//		edit.add(refreshAll);
	//		edit.add(new JSeparator());
	//		edit.add(new BiosampleActions.Action_NewBatch());
	//		edit.add(new ResultActions.Action_New());
	//
	//		menuBar.add(SpiritMenu.getToolsMenu());
	//		menuBar.add(SpiritMenu.getHelpMenu(splashConfig));
	//
	//		setJMenuBar(menuBar);
	//	}


	public static void main(String[] args) {

		SplashScreen.show(splashConfig);

		new SwingWorkerExtended() {
			@Override
			protected void doInBackground() throws Exception {
				SpiritAction.logUsage("BioViewer");
				JPAUtil.getManager();
			}
			@Override
			protected void done() {
				Spirit.initUI();
				new BioViewer();
			}
		};

	}

	public static void open() {
		BioViewer app = new BioViewer();
		app.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
}
