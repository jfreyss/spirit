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

package com.actelion.research.spiritapp.spirit.ui.result.edit;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class EditResultSelectElbDlg extends JEscapeDialog {
	
	private DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
	private JComboBox<String> comboBox = new JComboBox<String>(model);
	private String returnedValue;
	
	
	public EditResultSelectElbDlg(String initial) {
		super(UIUtils.getMainFrame(), "Results - New", true);
		
		final List<String> recentElbs = new ArrayList<String>(); 
		try {
			recentElbs.addAll( DAOResult.getRecentElbs(Spirit.getUser()));

			model.addElement("");
			for (String elb : recentElbs) {
				model.addElement(elb);
			}
			comboBox.setEditable(true);
		} catch (Exception e) {
			JExceptionDialog.showError(e);
			return;
		}
		

		JLabel header = new JLabel(
				"<html><body><b>Enter the ELB (electronic lab journal) or <br>" +
		" select an existing one to edit/appends results.</b></body></html>");
		header.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		JButton okButton = new JButton("Continue");
		
		JPanel contentPanel = new  JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.NORTH, header);
		contentPanel.add(BorderLayout.WEST, new JLabel("ELB: "));
		contentPanel.add(BorderLayout.CENTER, comboBox);
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));
		setContentPane(contentPanel);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		
		final JTextComponent editor = (JTextComponent) comboBox.getEditor().getEditorComponent();
		okButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					String res = (String) comboBox.getSelectedItem();
					
					if(DBAdapter.getAdapter().isInActelionDomain() && res.startsWith("ELB") && !recentElbs.contains(res)) {
						if(res.length()<12) throw new Exception("The ELB is not well formatted");
					}
					
					if(res.length()==0 || res.equalsIgnoreCase("ELB9999-9999")) {
						throw new Exception("Please enter an ELB");
					}
					returnedValue = res;
					dispose();
				} catch(Exception e) {
					JExceptionDialog.showError(EditResultSelectElbDlg.this, e);
					if(((String)comboBox.getSelectedItem()).startsWith("ELB")) {
						editor.selectAll();
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								comboBox.requestFocus();
							}
						});
					}	
				}
				
			}
		});
		
		if(initial!=null) {
			comboBox.setSelectedItem(initial);
		} else if(DBAdapter.getAdapter().isInActelionDomain()) {
			comboBox.setSelectedItem("ELB9999-9999");
		} else {
			comboBox.setSelectedItem("ELB-" + Spirit.getUsername()+"-"+System.currentTimeMillis());
		}

		editor.selectAll();
		getRootPane().setDefaultButton(okButton);
		setVisible(true);
		
		
	}


	/**
	 * @return the returnedValue
	 */
	public String getReturnedValue() {
		return returnedValue;
	}
	
	

}
