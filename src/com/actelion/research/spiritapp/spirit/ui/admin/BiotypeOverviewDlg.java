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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.lf.BiotypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class BiotypeOverviewDlg extends JEscapeDialog {

	private BiotypeComboBox biosampleTypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes(true));	
	
	private final BioTypeDocumentPane helpPane = new BioTypeDocumentPane(); 
	
	public BiotypeOverviewDlg() {
		super(UIUtils.getMainFrame(), "Admin - Biotype", true); 
		
		
		final JButton addButton = new JIconButton(IconType.NEW, "New Biotype");
		final JButton editButton = new JIconButton(IconType.EDIT, "Edit");
		final JButton deleteButton = new JIconButton(IconType.DELETE, "Delete");
		JButton closeButton = new JButton("Close");
				
		
		//Help
		helpPane.setBiotype(null);
		JScrollPane sp = new JScrollPane(helpPane);
		sp.setPreferredSize(new Dimension(440, 250));
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.NORTH, UIUtils.createTitleBox("Biotypes", UIUtils.createVerticalBox(
				UIUtils.createHorizontalBox(new JLabel("Biotype: "), biosampleTypeComboBox, editButton, deleteButton, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(addButton, Box.createHorizontalGlue()))));
		contentPanel.add(BorderLayout.CENTER, UIUtils.createTitleBox("Biotype details", sp));
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), closeButton));
		setContentPane(contentPanel);


		biosampleTypeComboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				helpPane.setBiotype(biosampleTypeComboBox.getSelection());
				editButton.setEnabled(biosampleTypeComboBox.getSelection()!=null);
				deleteButton.setEnabled(biosampleTypeComboBox.getSelection()!=null);
			}
		});
		
		
		editButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(biosampleTypeComboBox.getSelection()!=null) {
					new BiotypeEditDlg(biosampleTypeComboBox.getSelection());
					JPAUtil.clear();
					biosampleTypeComboBox.setValues(DAOBiotype.getBiotypes(true));
					biosampleTypeComboBox.setSelection(biosampleTypeComboBox.getSelection());
				}
			}
		});
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Biotype biotype = biosampleTypeComboBox.getSelection();
				if(biotype!=null) {
					int r = JOptionPane.showConfirmDialog(BiotypeOverviewDlg.this, "Are you sure you want to delete " + biotype +"?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if(r!=JOptionPane.YES_OPTION) return;
					try {
						if(biotype==null || biotype.getId()<=0) throw new Exception("Nothing to delete!?");
						DAOBiotype.removeBiotype(biotype, Spirit.askForAuthentication());
						SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Biotype.class, biotype);
						biosampleTypeComboBox.setValues(DAOBiotype.getBiotypes(true));	
						helpPane.setBiotype(null);
						JOptionPane.showMessageDialog(BiotypeOverviewDlg.this, biotype.getName() + " Deleted", "Success", JOptionPane.INFORMATION_MESSAGE);
					} catch (Exception e2) {
						JExceptionDialog.showError(e2);		
					} 
				}
			}
		});
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Biotype type = new Biotype();
				new BiotypeEditDlg(type);
				JPAUtil.clear();
				biosampleTypeComboBox.setValues(DAOBiotype.getBiotypes(true));
				biosampleTypeComboBox.setSelection(type);
				editButton.setEnabled(biosampleTypeComboBox.getSelection()!=null);

			}
		});
		closeButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		editButton.setEnabled(biosampleTypeComboBox.getSelection()!=null);
		deleteButton.setEnabled(biosampleTypeComboBox.getSelection()!=null);

		UIUtils.adaptSize(this, 1000, 800);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}
	
}
