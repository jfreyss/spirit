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

package com.actelion.research.spiritapp.spirit.ui.result.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritcore.business.pivot.FlatPivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOResult.FindDuplicateMethod;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class QueryDuplicatesDlg extends JEscapeDialog {
	
	private JComboBox<String> elb1ComboBox;
	private JComboBox<String> elb2ComboBox;
	private JGenericComboBox<FindDuplicateMethod> methodComboBox = new JGenericComboBox<DAOResult.FindDuplicateMethod>(FindDuplicateMethod.values(), false);
	
	
	public QueryDuplicatesDlg() {
		super(UIUtils.getMainFrame(), "Find Duplicates Results", true);

		List<String> elbs = DAOResult.getRecentElbs(Spirit.getUser());
		elbs.add(0, "");
		elb1ComboBox = new JComboBox<String>(new Vector<String>(elbs));
		elb2ComboBox = new JComboBox<String>(new Vector<String>(elbs));
		elb1ComboBox.setEditable(true);
		elb2ComboBox.setEditable(true);
		
		//centerPanel
		JPanel centerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1; c.anchor = GridBagConstraints.WEST;
		c.gridwidth = 2;
		c.gridx = 0; c.gridy = 0; centerPanel.add(new JLabel("<html><i>This function will query all duplicates and display them in the interface.<br>You can give either:<br><li>One ELB to find duplicates among one ELB.<li>Two ELBs to find duplicates across 2 ELBS.</i>"), c);
		c.gridwidth = 1;
		
		c.gridx = 0; c.gridy = 2; centerPanel.add(new JLabel("ELB 1: "), c);
		c.gridx = 0; c.gridy = 3; centerPanel.add(new JLabel("ELB 2: (opt.)"), c);
		c.gridx = 1; c.gridy = 2; centerPanel.add(elb1ComboBox, c);
		c.gridx = 1; c.gridy = 3; centerPanel.add(elb2ComboBox, c);
		
		c.gridwidth = 2;
		c.gridx = 0; c.gridy = 4; centerPanel.add(methodComboBox, c);
		
		centerPanel.setBorder(BorderFactory.createEtchedBorder());
		
		//Buttons
		JButton queryButton = new JIconButton(IconType.SEARCH, "Query");
		queryButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent ev) {
				new SwingWorkerExtended("Duplicates", getContentPane(), true) {
					List<Result> results;
					@Override
					protected void doInBackground() throws Exception {
						String elb1 = elb1ComboBox.getSelectedItem().toString().trim();		
						if(elb1.length()==0) throw new Exception("You must select an elb");
						
						String elb2 = elb1ComboBox.getSelectedItem().toString().trim();
						if(elb2.length()==0) elb2 = elb1;

						results = DAOResult.findDuplicates(elb1, elb2, methodComboBox.getSelection(), Spirit.getUser());
					}
					@Override
					protected void done() {
						JOptionPane.showMessageDialog(QueryDuplicatesDlg.this, "The query returned " + results.size() + " results", "Results", JOptionPane.INFORMATION_MESSAGE);							
						SpiritContextListener.setResults(results, new FlatPivotTemplate());
						dispose();
					}					
				};				
			}
		});
		
		
		//contentPanel
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.CENTER, centerPanel);
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), queryButton));

		setContentPane(contentPanel);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
		
	}

}
