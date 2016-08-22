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

package com.actelion.research.spiritapp.spirit.ui.admin.database;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.services.dao.ConfigProperties;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.migration.MigrationScript;
import com.actelion.research.spiritcore.services.migration.MigrationScript.ILogger;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

/**
 * Class responsible for setting/testing the connection (ie. dbadapter.initAdapter(true) should be successful) 
 * 
 * @author freyssj
 *
 */
public class DatabaseMigrationDlg extends JDialog {
	
	private boolean hasErrors = false;
	private final JTextArea sqlPane = new JTextArea();
	private final JEditorPane errorPane = new JEditorPane("text/html", "<html>");

	public DatabaseMigrationDlg() {
		super(UIUtils.getMainFrame(), "Spirit - DB Migration", true);
		
		sqlPane.setEditable(false);
		sqlPane.setCaretPosition(0);
		JScrollPane sp1 = new JScrollPane(sqlPane);
		sp1.setPreferredSize(new Dimension(750, 300));
		
		errorPane.setEditable(false);
		errorPane.setCaretPosition(0);
		JScrollPane sp2 = new JScrollPane(errorPane);
		sp2.setPreferredSize(new Dimension(750, 400));
		
		JButton updateButton = new JButton("Run script");
		updateButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				updateDB();
			}
		});
		
		JButton closeButton = new JButton("Test & Close");
		closeButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				new SwingWorkerExtended(errorPane, true) {
					boolean ok = false;
					@Override
					protected void doInBackground() throws Exception {
						ok = testSchema(DBAdapter.getAdapter());
					}
					@Override
					protected void done() {
						if(ok) {
							dispose();
						} else {							
							int res = JOptionPane.showConfirmDialog(null, "The DB is still invalid. Do you want to close this dialog anyway?", "Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
							if(res==JOptionPane.YES_OPTION) {
								dispose();
							}
						}
					}
				};				
			}
		});
		
		//Init script
		try {
			String script = MigrationScript.getSql(DBAdapter.getAdapter().getVendor());
			sqlPane.setText(script);
		} catch(Exception e) {
			errorPane.setText(e.getMessage());
		}
		String version;
		try {
			version = MigrationScript.getDBVersion();
		} catch(Exception e) {
			version = null;
		}
		
		setContentPane(
				UIUtils.createBox(UIUtils.createBox(sp2, sp1),
						
				UIUtils.createVerticalBox(new JCustomLabel("Your database is not up to date (currently: " + (version==null?"NA":version) + ")", FastFont.BOLD, Color.RED),
						new JLabel("You must update the DB to version " + MigrationScript.getExpectedDBVersion() + " to continue. Do you accept the update?")),
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), updateButton, closeButton)));
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
		
	}
	
	private void updateDB() {

		hasErrors = false;
		final ILogger logger = new ILogger() {				
			@Override
			public void info(String sql, String msg) {
				try {

					SimpleAttributeSet att1 = new SimpleAttributeSet();
					StyleConstants.setFontSize(att1, 11);
					SimpleAttributeSet att2 = new SimpleAttributeSet();
				    StyleConstants.setForeground(att2, UIUtils.getColor(0, 128, 0));

				    javax.swing.text.Document doc = errorPane.getDocument();
					doc.insertString(doc.getLength(), sql.replaceAll("\n", " ")+"\n", att1);
					doc.insertString(doc.getLength(), " > " + msg.replaceAll("\n", " ")+"\n", att2);

					errorPane.setCaretPosition(doc.getLength());
				} catch(Exception e2) {
					e2.printStackTrace();
				}
			}
			@Override
			public void error(String sql, Exception e) {
				try {							
					
					hasErrors = true;
					
					SimpleAttributeSet att1 = new SimpleAttributeSet();
					StyleConstants.setFontSize(att1, 11);
				    SimpleAttributeSet att2 = new SimpleAttributeSet();
				    StyleConstants.setForeground(att2, Color.RED);
					
				    javax.swing.text.Document doc = errorPane.getDocument();								
					doc.insertString(doc.getLength(), sql.replaceAll("\n", " ")+"\n", att1);
					doc.insertString(doc.getLength(), " > " + e.getMessage().replaceAll("\n", " ")+"\n", att2);

					errorPane.setCaretPosition(doc.getLength());
				} catch(Exception e2) {
					e2.printStackTrace();
				}
			}
		};
		new SwingWorkerExtended(errorPane, true) {
			@Override
			protected void doInBackground() throws Exception {
				errorPane.setText("<html><div style='white-space:nowrap'>");
				MigrationScript.updateDB(DBAdapter.getAdapter().getVendor(), logger);
			}
			
			@Override
			protected void done() {
				try {
					boolean ok = testSchema(DBAdapter.getAdapter());
					
					if(ok) {
						if(hasErrors) {
							JExceptionDialog.showWarning(DatabaseMigrationDlg.this, "The database was successfully migrated. There were however some errors, please check.");
						} else {
							JExceptionDialog.showInfo(DatabaseMigrationDlg.this, "The database was successfully migrated.");
							dispose();
						}
					} else {
						JExceptionDialog.showError(DatabaseMigrationDlg.this, "The DB could not be migrated. Please check the errors.");							
					}
				} catch(Exception e) {
					JExceptionDialog.showError(DatabaseMigrationDlg.this, e);												
				}
			}
		};
	}
	
	public static boolean testSchema(DBAdapter adapter) throws Exception {
		boolean ok;
		try {			
			adapter.preInit();
			adapter.validate();
			ok = true;
		} catch(Exception e) {
			
			e.printStackTrace();
			int res = JOptionPane.showConfirmDialog(null, "There were some errors (" + e.getCause() + ").\n\nDo you want Spirit to fix the errors?", "Error", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(res==JOptionPane.YES_OPTION) {
				JPAUtil.initFactory(adapter, "update");
				ok = true;
			} else {
				ok = false;
			}
		}
		if(ok && adapter==DBAdapter.getAdapter()) {
			ConfigProperties.getInstance().setDBVersion(MigrationScript.getExpectedDBVersion());
			ConfigProperties.getInstance().saveValues();
		}
		return ok;

	}
	
	
	
	public static void main(String[] args) {
		new DatabaseMigrationDlg();
	}
}
