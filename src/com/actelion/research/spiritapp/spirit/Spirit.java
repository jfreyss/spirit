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

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.SpiritTab;
import com.actelion.research.spiritapp.spirit.ui.admin.database.DatabaseSettingsDlg;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTab;
import com.actelion.research.spiritapp.spirit.ui.home.HomeTab;
import com.actelion.research.spiritapp.spirit.ui.location.LocationTab;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTab;
import com.actelion.research.spiritapp.spirit.ui.study.StudyTab;
import com.actelion.research.spiritapp.spirit.ui.util.LoginDlg;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritAction;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.services.migration.MigrationScript.FatalException;
import com.actelion.research.util.ArgumentParser;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SplashScreen;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

/**
 * Spirit Main application
 *
 * @author freyssj
 *
 */
public class Spirit extends SpiritFrame {



	public Spirit(Runnable afterLogin) {
		super("Spirit", "Spirit - (C) Joel Freyss - Idorsia Pharmaceuticals Ltd", afterLogin);
	}

	@Override
	public List<SpiritTab> getTabs() {
		List<SpiritTab> tabs = new ArrayList<>();
		tabs.add(new HomeTab(this));
		tabs.add(new StudyTab(this));
		tabs.add(new BiosampleTab(this));
		tabs.add(new LocationTab(this));
		if(SpiritProperties.getInstance().isChecked(PropertyKey.TAB_RESULT)) {
			tabs.add(new ResultTab(this));
		}
		return tabs;
	}

	public static void preLoadDAO() throws Exception {
		JPAUtil.getManager();
		DAOEmployee.getEmployeeGroups();
		DAOBiotype.getBiotypes();
		DAOLocation.getLocationRoots(null);
		DAOTest.getTests();
	}

	public static SpiritUser askForAuthentication() throws Exception {
		if(SpiritFrame.getUser()==null) {
			LoginDlg.openLoginDialog(UIUtils.getMainFrame(), "Spirit Login");
			if(SpiritFrame.getUser()==null) throw new Exception("You must be logged in");
		}
		return SpiritFrame.getUser();
	}


	/**
	 * Special executor for Actelion, to put Spirit toFront with the proper settings
	 * @param args
	 * @throws Exception
	 */
	public static void initSingleApplication(final String[] args) {
		if(_instance==null) {
			try {
				main(args);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			_instance.toFront();
		}


		//Process arguments
		final ArgumentParser argumentParser = new ArgumentParser(args);
		final String studyId = argumentParser.getArgument("studyId");
		if(studyId!=null) {
			new SwingWorkerExtended("Load study", null, SwingWorkerExtended.FLAG_ASYNCHRONOUS500MS) {
				@Override
				protected void doInBackground() throws Exception {
					Study s = DAOStudy.getStudyByStudyId(studyId);
					LoggerFactory.getLogger(Spirit.class).info("Init with studyId=" + studyId);
					SpiritContextListener.setStudy(s);
				}
			};
		}
	}

	public static void main(final String[] args) throws Exception {

		SplashScreen.show(splashConfig);

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
				initUI();
				if(throwable!=null) {
					JExceptionDialog.showError(throwable);
					if(throwable instanceof FatalException) System.exit(1);
					new DatabaseSettingsDlg(false);
				}
				Spirit spirit;
				try {
					LoggerFactory.getLogger(Spirit.class).debug("start Spirit");
					spirit = new Spirit(()-> {
						initSingleApplication(args);
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

	@Override
	public void paintComponents(Graphics g) {
		UIUtils.applyDesktopProperties(g);
		super.paintComponents(g);
	}

}
