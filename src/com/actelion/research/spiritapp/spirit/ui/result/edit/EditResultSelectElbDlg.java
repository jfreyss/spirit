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
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.UIUtils;

public class EditResultSelectElbDlg extends JEscapeDialog {
	
	private JTextComboBox comboBox = new JTextComboBox();
	private String returnedValue;
	
	
	public EditResultSelectElbDlg() {
		super(UIUtils.getMainFrame(), "Results - New", true);
		
		final List<String> recentElbs = DAOResult.getRecentElbs(SpiritFrame.getUser());
		comboBox.setChoices(recentElbs);

		JLabel header = new JLabel(
				"<html><body><b>Enter the ELB (electronic lab journal) or <br>" +
				" select an existing one to edit/appends results.</b></body></html>");
		header.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		JButton okButton = new JButton("Continue");
		
		JPanel contentPanel = new  JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.NORTH, header);
		contentPanel.add(BorderLayout.CENTER, UIUtils.createHorizontalBox(new JLabel("ELB: "), comboBox, Box.createHorizontalGlue()));
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));
		setContentPane(contentPanel);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		
		okButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent ev) {
				String res = comboBox.getText();
				try {
					
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
					if(res.startsWith("ELB")) {
						comboBox.selectAll();
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
		
		if(DBAdapter.getAdapter().isInActelionDomain()) {
			comboBox.setText("ELB9999-9999");
		} else {
			comboBox.setText(DAOResult.suggestElb(SpiritFrame.getUsername()));
		}

		comboBox.selectAll();
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
