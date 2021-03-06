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

import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.admin.database.DatabaseMigrationDlg;
import com.actelion.research.spiritapp.ui.admin.database.DatabaseSettingsDlg;
import com.actelion.research.spiritapp.ui.admin.user.UserAdminDlg;
import com.actelion.research.spiritapp.ui.util.SpiritAction;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserManagedMode;
import com.actelion.research.spiritcore.adapter.HSQLFileAdapter;
import com.actelion.research.spiritcore.adapter.SchemaCreator;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.migration.MigrationScript;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

/**
 * User Interface Utility to checks that the DB Version is correct and that the user is logged in
 *
 * @author Joel Freyss
 */
public class SpiritDB {

	public static void checkAndLogin() {
		//Check DB Version
		LoggerFactory.getLogger(Spirit.class).debug("check dbVersion");
		try {
			String dbVersion = MigrationScript.getDBVersion();
			if(dbVersion==null) {
				SchemaCreator.recreateTables(DBAdapter.getInstance());
			} else if(dbVersion.compareTo(MigrationScript.getExpectedDBVersion())<0) {
				new DatabaseMigrationDlg();
			}
		} catch(Exception e) {
			JExceptionDialog.showError(e);
			new DatabaseSettingsDlg(false);
		}

		SpiritUser user;

		//Login
		final StringBuilder msg = new StringBuilder();
		try {
			LoggerFactory.getLogger(Spirit.class).debug("check Users");
			if(DBAdapter.getInstance().isInActelionDomain()) {
				//Login freyssj automatically
				if(System.getProperty("user.name").equals("freyssj") && InetAddress.getLocalHost().getHostAddress().equals("10.100.227.35") ) {
					try {
						user = DAOSpiritUser.loadUser("freyssj");
						if(user==null) throw new Exception("Could not load user freyssj");
						SpiritFrame.setUser(user);
					} catch (Exception e) {
						System.err.println(e);
					}
				}
			} else if(DBAdapter.getInstance().getUserManagedMode()==UserManagedMode.WRITE_PWD) {
				List<Employee> employees = DBAdapter.getInstance().getEmployees();
				boolean admins = false;

				for (Employee emp : employees) {
					if(emp.getRoles().contains(SpiritUser.ROLE_ADMIN)) {admins=true; continue;}
				}
				if(!admins) {
					JOptionPane.showMessageDialog(null, "There are no admins in the system, you must create some users and give them admin rights", "User Admin Error", JOptionPane.ERROR_MESSAGE);
					new UserAdminDlg();
				} else if(employees.size()==1 && employees.get(0).getUserName().equals("admin")) {
					msg.append("<span style='font-size:105%;color:red'>To connect use 'admin' without any password (until you create users)</span>");
				}
			} else if(DBAdapter.getInstance().getUserManagedMode()==UserManagedMode.UNIQUE_USER) {
				//Log the system user
				user = new SpiritUser(System.getProperty("user.name"));
				user.setRole(SpiritUser.ROLE_ADMIN, true);
				SpiritFrame.setUser(user);
			}
		} catch(Throwable e) {
			e.printStackTrace();
			StringWriter w = new StringWriter();
			e.printStackTrace(new PrintWriter(w));
			JTextArea ta = new JTextArea(w.getBuffer().toString());
			ta.setEditable(false);
			JScrollPane sp = new JScrollPane(ta);
			sp.setPreferredSize(new Dimension(600, 350));
			JOptionPane.showMessageDialog(UIUtils.getMainFrame(), UIUtils.createBox(sp, new JLabel("<html><b>Database Error</b><br>The tables may not be up to dates, please contact support with the following trace!"), null, null, null), "DB Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if(SpiritFrame.getUser()==null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					new SpiritAction.Action_Relogin(UIUtils.getMainFrame(), DBAdapter.getInstance().getSoftwareName(), "<html>"+msg.toString()).actionPerformed(null);
					if(SpiritFrame.getUser()==null) System.exit(1);
					checkImportExamples(false);
				}
			});
		} else {
			checkImportExamples(false);
		}
	}


	public static void checkImportExamples(boolean force) {
		//Check emptyness?
		SpiritUser user = SpiritFrame.getUser();
		if(DBAdapter.getInstance().isInActelionDomain() || (user!=null && !SpiritRights.isSuperAdmin(user))) return;
		try {

			List<Study> exampleStudies = DAOStudy.queryStudies(StudyQuery.createForState("EXAMPLE"), null);
			if(exampleStudies.size()==0) exampleStudies = DAOStudy.queryStudies(StudyQuery.createForState("TEST"), null);

			boolean importDemo;
			if(force || DAOTest.getTests().size()==0) {
				importDemo = true;
			} else if(DBAdapter.getInstance().getClass()==HSQLFileAdapter.class && exampleStudies.size()<3) {
				int res = JOptionPane.showConfirmDialog(UIUtils.getMainFrame(), "There are new examples available!\nDo you want to update the current examples with the new ones?", "Examples", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				importDemo = res==JOptionPane.YES_OPTION;
			} else {
				importDemo = false;
			}
			if(importDemo) {
				try {
					JPAUtil.pushEditableContext(user);
					if(force) {
						SchemaCreator.clearExamples(user);
					}
					SchemaCreator.createExamples(user);
				} finally {
					JPAUtil.popEditableContext();
				}
			}
		} catch(Exception e) {
			JExceptionDialog.showError(UIUtils.getMainFrame(), e);
		}
	}

}
