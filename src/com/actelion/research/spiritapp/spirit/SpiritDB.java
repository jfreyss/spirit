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

import java.awt.Dimension;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import javax.persistence.EntityManager;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritapp.spirit.ui.SpiritAction;
import com.actelion.research.spiritapp.spirit.ui.admin.database.DatabaseMigrationDlg;
import com.actelion.research.spiritapp.spirit.ui.admin.database.DatabaseSettingsDlg;
import com.actelion.research.spiritapp.spirit.ui.admin.user.UserAdminDlg;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.DBAdapter.UserAdministrationMode;
import com.actelion.research.spiritcore.adapter.HSQLFileAdapter;
import com.actelion.research.spiritcore.adapter.SchemaCreator;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOExchange;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.Importer;
import com.actelion.research.spiritcore.services.migration.MigrationScript;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class SpiritDB {

	public static void check() {

		
		//Check DB Version
		LoggerFactory.getLogger(Spirit.class).debug("check dbVersion");
		try {
			String dbVersion = MigrationScript.getDBVersion();
			if(dbVersion==null) {
				SchemaCreator.recreateTables(DBAdapter.getAdapter());
			} else if(!dbVersion.equals(MigrationScript.getExpectedDBVersion())) {
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
			if(DBAdapter.getAdapter().isInActelionDomain()) {
				//Login freyssj automatically 
				if(System.getProperty("user.name").equals("freyssj")) {
					try {
						user = DAOSpiritUser.loadUser("freyssj");
						if(user==null) throw new Exception("Could not load user freyssj");
						Spirit.setUser(user);
									
					} catch (Exception e) {
						System.err.println(e);
					}
				}
			} else if(DBAdapter.getAdapter().getUserManagedMode()==UserAdministrationMode.READ_WRITE) {
				List<Employee> employees = DBAdapter.getAdapter().getEmployees();
				boolean admins = false;
				
				for (Employee emp : employees) {
					if(emp.getRoles().contains(SpiritUser.ROLE_ADMIN)) {admins=true; continue;}
				}
				if(!admins) {						
					JOptionPane.showMessageDialog(null, "There are no admins in the system, you must create some users and give them admin rights", "User Admin Error", JOptionPane.ERROR_MESSAGE);
					new UserAdminDlg();
				} else if(employees.size()==1 && employees.get(0).getUserName().equals("admin")) {
					msg.append("Note: To connect use 'admin' without any password");
				}
			} else if(DBAdapter.getAdapter().getUserManagedMode()==UserAdministrationMode.UNIQUE_USER) {
				//Log the system user
				user = new SpiritUser(System.getProperty("user.name"));
				user.setRole(SpiritUser.ROLE_ADMIN, true);
				Spirit.setUser(user);				
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
		
		if(Spirit.getUser()==null) {
			SwingUtilities.invokeLater(new Runnable() {				
				@Override
				public void run() {
					new SpiritAction.Action_Relogin(UIUtils.getMainFrame(), "Spirit", msg.toString()).actionPerformed(null);
					if(Spirit.getUser()==null) System.exit(1);
					importExamples(false);
				}
			});
		}
		importExamples(false);
	}
	
	public static void importExamples(boolean force) {
		//Check emptyness?
		SpiritUser user = Spirit.getUser();
		if(user==null || DBAdapter.getAdapter().isInActelionDomain() || !SpiritRights.isSuperAdmin(user)) return;
		try(InputStream is = DBAdapter.class.getResourceAsStream("demo.spirit")) {
			if(is==null) {
				throw new Exception("The system could not find the default configuration. The system is therefore empty");
			}
			
			Exchange exchange = Importer.read(new InputStreamReader(is));			
			List<Study> exampleStudies = DAOStudy.queryStudies(StudyQuery.createForState("EXAMPLE"), null);
			if(exampleStudies.size()==0) exampleStudies = DAOStudy.queryStudies(StudyQuery.createForState("TEST"), null);
			
			
			
			boolean importDemo = force || DAOTest.getTests().size()==0;
			boolean askToRewrite = !importDemo && DBAdapter.getAdapter().getClass()==HSQLFileAdapter.class && exampleStudies.size()<exchange.getStudies().size();
			if(askToRewrite) {
				int res = JOptionPane.showConfirmDialog(UIUtils.getMainFrame(), "There are new examples available!\nDo you want to update the current examples with the new ones?", "Examples", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if(res==JOptionPane.YES_OPTION) {
					importDemo = true;
				}
			}
			if(importDemo) {
				
				if(force) {
					//Clean
					EntityManager session = null;
					try {
						JPAUtil.pushEditableContext(user);
						session = JPAUtil.getManager();
						session.getTransaction().begin();
//						DAOResult.deleteResults(session, DAOResult.queryResults(session, ResultQuery.createQueryForSids(JPAUtil.getIds(exampleStudies)), user), user);
//						DAOBiosample.deleteBiosamples(session, DAOBiosample.queryBiosamples(session, BiosampleQuery.createQueryForSids(JPAUtil.getIds(exampleStudies)), user), user);
						DAOStudy.deleteStudies(session, exampleStudies, true, user);
						LoggerFactory.getLogger(SpiritDB.class).info("examples deleted: "+exampleStudies);						
						session.getTransaction().commit();									
					} catch(Throwable e3) {
						e3.printStackTrace();
						if(session!=null && session.getTransaction().isActive()) session.getTransaction().rollback();
					} finally {
						JPAUtil.popEditableContext();
					}								
				}
				
				try {
					JPAUtil.pushEditableContext(user);
					ExchangeMapping mapping = new ExchangeMapping(exchange);
					DAOExchange.persist(mapping, user);
					LoggerFactory.getLogger(SpiritDB.class).info("examples persisted: "+exampleStudies);
				} catch(Throwable e2) {
					LoggerFactory.getLogger(SpiritDB.class).error("Could not persist examples: replace old examples", e2);
					e2.printStackTrace();
					force=true;
				} finally {
					JPAUtil.popEditableContext();
				}
				
				
			}
		} catch(Exception e) {
			JExceptionDialog.showError(UIUtils.getMainFrame(), e);
		}
		
	}
	
}
