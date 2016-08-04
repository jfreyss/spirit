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

package com.actelion.research.spiritapp.spirit.ui.util.correction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.TextChangeListener;
import com.actelion.research.util.ui.UIUtils;

public abstract class CorrectionDlg<ATTRIBUTE, DATA> extends JEscapeDialog {
	
	public static final int OK = 1;

	private int returnCode;
	private List<JButton> allButtons = new ArrayList<JButton>();	
	private JDialog parent;
	
	public CorrectionDlg(JDialog parent, final CorrectionMap<ATTRIBUTE, DATA> correctionMap) {
		super(parent, "Typos correction", true);
		this.parent = parent;
		
		List<Component> panels = new ArrayList<>();
		for (ATTRIBUTE att : correctionMap.keySet()) {			
			panels.add(UIUtils.createTitleBox(getName(att), createPanel(correctionMap.get(att))));
		}		
		panels.add(Box.createHorizontalGlue());
		JPanel centerPane = UIUtils.createVerticalBox(panels); 

		
		JButton skipButton = new JButton("Continue");
		skipButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				returnCode = OK;
				dispose();
			}
		});
		
		JButton replaceButton = new JButton("Replace All");
		getRootPane().setDefaultButton(skipButton);
		replaceButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				for (JButton button : allButtons) {
					if(button.isEnabled()) {
						button.getActionListeners()[0].actionPerformed(null);
					}
				}
				returnCode = OK;
				dispose();
			}
		});
		
		JPanel contentPane = new JPanel(new BorderLayout());
		
		JLabel label = new JLabel("<html><b style='color:#AA8800'>Warning:</b> Some values may be incorrect.<br> Please check or replace with the suggested values if needed.</html>");
		label.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
		
		contentPane.add(BorderLayout.NORTH, label);
		if(centerPane.getPreferredSize().getHeight()>500) {
			contentPane.add(BorderLayout.CENTER, new JScrollPane(centerPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));			
		} else {
			contentPane.add(BorderLayout.CENTER, centerPane);
		}
		
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createGlue(), replaceButton, skipButton));
		
		setContentPane(contentPane);
		pack();
		if(getSize().height>700) {
			setSize(new Dimension(getSize().width, 700));
		}
		setLocationRelativeTo(parent);
		setVisible(true);
		
	}


	private JPanel createPanel(List<Correction<ATTRIBUTE, DATA>> list) {
		final ATTRIBUTE att = list.get(0).getAttribute();

		List<Component> comps = new ArrayList<>();
		for (final Correction<ATTRIBUTE, DATA> correction : list) {
			
//			JTextField oldValueTextField = new JTextField(correction.getValue());
//			oldValueTextField.setEditable(false);
//			oldValueTextField.setBackground(Color.LIGHT_GRAY);
			

			final JTextComboBox newValueComboBox = new JTextComboBox(correction.getSuggestedValues());
			newValueComboBox.setText(correction.getSuggestedValue());
			
			final JButton replaceButton = new JButton("Replace");			
			final JLabel doneLabel = new JCustomLabel("                  ", new Font("Arial", Font.PLAIN, 9));
			int score = (int)(100*correction.getScore());
			doneLabel.setOpaque(true);
			doneLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
			doneLabel.setBackground(score>=80? new Color(200, 255, 200): score>=70? new Color(255, 240, 200): score>0? new Color(255, 200, 200): Color.RED);
			if(score>0) {
				doneLabel.setText(" "+score+"% match ");
			} else {
				doneLabel.setText(" No match ");					
			}
			
			
			comps.add(doneLabel); 
			comps.add(new JCustomLabel(correction.getValue(), Font.BOLD));
			comps.add(new JLabel(" is a new attribute. Did you mean? "));
			comps.add(newValueComboBox);
			comps.add(replaceButton);

			newValueComboBox.addTextChangeListener(new TextChangeListener() {
				@Override
				public void textChanged(JComponent src) {
					replaceButton.setEnabled(true);
				}
			});
			replaceButton.setEnabled(newValueComboBox.getText().length()>0);
			replaceButton.addActionListener(new ActionListener() {					
				@Override
				public void actionPerformed(ActionEvent e) {
					String newVal = newValueComboBox.getText();
					
					performCorrection(correction, newVal);						
					doneLabel.setText("Done");
					doneLabel.setForeground(new Color(0,100,0));
					parent.repaint();
				}
			});				
			allButtons.add(replaceButton);			
		}			
		JPanel panel = UIUtils.createTable(5, comps);
		return panel;
	}
	

	/**
	 * @return the returnCode
	 */
	public int getReturnCode() {
		return returnCode;
	}

	protected abstract String getSuperCategory(ATTRIBUTE att);
	protected abstract String getName(ATTRIBUTE att);
	protected abstract void performCorrection(Correction<ATTRIBUTE, DATA> correction, String newValue);
	
}
