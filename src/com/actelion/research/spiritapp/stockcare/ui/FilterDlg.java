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

package com.actelion.research.spiritapp.stockcare.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.UIUtils;

public class FilterDlg extends JEscapeDialog {

	private JTextComboBox[] textComboBoxes;
	private JLabel infoLabel = new JLabel(" ");
	private boolean success = false; 
	
	public FilterDlg(String title, BiotypeMetadata[] filters) {
		super(UIUtils.getMainFrame(), "Select the filters");
		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.ipadx = c.ipady = 2;
		textComboBoxes = new JTextComboBox[filters.length];		
		for (int i = 0; i < filters.length; i++) {
			final BiotypeMetadata bm = filters[i];
			JTextComboBox textComboBox = new JTextComboBox() {
				@Override
				public Collection<String> getChoices() {
					if(bm.getDataType()==DataType.LIST || bm.getDataType()==DataType.MULTI) {
						return Arrays.asList(bm.getParametersArray());
					} else if(bm.getDataType()==DataType.AUTO) {
						return DAOBiotype.getAutoCompletionFields(bm, null);
					} else {
						return new ArrayList<String>();
					}
				}
			};
			textComboBox.setPreferredSize(new Dimension(250, 24));
			c.fill = GridBagConstraints.NONE; c.weightx = 0; c.gridx = 0; c.gridy = i; panel.add(new JLabel(bm.getName()+": "), c);
			c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; c.gridx = 1; c.gridy = i; panel.add(textComboBox, c);
			textComboBoxes[i] = textComboBox;
			textComboBox.addPropertyChangeListener(JTextComboBox.PROPERTY_TEXTCHANGED, new PropertyChangeListener() {				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					onFilterChange();
				}
			});
		}
		
		infoLabel.setFont(FastFont.BOLD);
		infoLabel.setForeground(Color.BLUE);
		c.weighty = 1; c.weightx = 1; c.gridwidth = 1; c.gridx = 1; c.gridy = filters.length+1; panel.add(infoLabel, c);

		
		
		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				success = true;
				dispose();
			}
		});
		
		panel.setBorder(BorderFactory.createEtchedBorder());
		
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.NORTH, new JCustomLabel(title, FastFont.BOLD.deriveSize(14)));
		contentPane.add(BorderLayout.CENTER, panel);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));
		
		setContentPane(contentPane);
		pack();
		onFilterChange();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
		
	}
	
	public void onFilterChange() {
		
	}
	
	public void setInfo(String label) {
		infoLabel.setText(label);
	}
	
	public String[] getFilters() {
		String[] selection = new String[textComboBoxes.length];
		for (int i = 0; i < textComboBoxes.length; i++) {					
			selection[i] = textComboBoxes[i].getText();
		}
		return selection;
	}
	
	public boolean isSuccess() {
		return success;
	}
}
