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

package com.actelion.research.spiritapp.spirit.ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class ELBRenameDlg extends JEscapeDialog {
	
	private JCustomTextField oldElbTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC,10);
	private JCustomTextField newElbTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC,10);
	private JCustomLabel infoLabel = new JCustomLabel(" ", Font.ITALIC, Color.RED);
	
	public ELBRenameDlg() {
		super(UIUtils.getMainFrame(), "Admin - Rename ELB");
		
	
		oldElbTextField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				if(newElbTextField.getText().length()==0) {
					newElbTextField.setText(oldElbTextField.getText());
					newElbTextField.selectAll();
				}
				
				try {
					List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForElb(oldElbTextField.getText()), null);
	
					String studyId = "";
					for (Result result : results) {
						if(result.getBiosample()!=null && result.getBiosample().getInheritedStudy()!=null) {
							if(!studyId.contains(result.getBiosample().getInheritedStudy().getId()+" ")) {
								studyId += result.getBiosample().getInheritedStudy().getStudyId()+" ";
							}
						} 
					}
					infoLabel.setText(results.size()+" results (study="+(studyId.length()==0?"N/A": studyId.trim())+")");
				} catch(Exception ex) {
					ex.printStackTrace();
					infoLabel.setText(ex.toString());
				}
			}
		});
		
		JButton renameButton = new JIconButton(IconType.SAVE, "Rename");
		renameButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				eventOk();
			}
		});
				
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.CENTER, UIUtils.createTable(2, 
				new JLabel("Old ELB: "), oldElbTextField,
				null, infoLabel,
				new JLabel("New ELB: "), newElbTextField));
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), renameButton));
		
		setContentPane(contentPanel);
		setSize(300, 160);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
		
		
	}
	
	
	private void eventOk() {
		if(!Spirit.getUser().isSuperAdmin()) {
			JExceptionDialog.showError(ELBRenameDlg.this, "Only an admin can rename an ELB");
			return;
		}
		try {
			JPAUtil.pushEditableContext(Spirit.getUser());
			String user = Spirit.getUser().getUsername();
			EntityManager em = JPAUtil.getManager();
			EntityTransaction txn = null;
			try {
				List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForElb(newElbTextField.getText()), null);
				if(results.size()>0) throw new Exception("The elb "+newElbTextField.getText()+" contains already "+results.size()+" results");
				
				results = DAOResult.queryResults(ResultQuery.createQueryForElb(oldElbTextField.getText()), null);
				Date now = JPAUtil.getCurrentDateFromDatabase();
				txn = em.getTransaction();
				txn.begin();
				for (Result result : results) {
					result.setElb(newElbTextField.getText());
					result.setUpdUser(user);
					result.setUpdDate(now);
				}
				txn.commit();
				txn = null;
				JOptionPane.showMessageDialog(ELBRenameDlg.this, results.size() +" results updated to " + newElbTextField.getText());
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Result.class, results);
				oldElbTextField.setText("");			
				oldElbTextField.requestFocusInWindow();
			} catch (Exception e) {
				if(txn!=null) {txn.rollback();}
				txn = null;
				JExceptionDialog.showError(e);			
			} 
		} finally {
			JPAUtil.popEditableContext();
		}
	}
}
