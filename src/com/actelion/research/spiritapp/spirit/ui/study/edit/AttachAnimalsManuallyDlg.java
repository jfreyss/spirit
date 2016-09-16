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

package com.actelion.research.spiritapp.spirit.ui.study.edit;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.helper.AttachBiosamplesHelper;
import com.actelion.research.spiritapp.spirit.ui.lf.BiotypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.study.edit.AttachedBiosampleTableModel.Mode;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class AttachAnimalsManuallyDlg extends JSpiritEscapeDialog {

	private Study study;
	private AttachedBiosampleTable animalTable;
	private BiotypeComboBox biotypeComboBox; 
	
	public AttachAnimalsManuallyDlg(Study s) {
		super(UIUtils.getMainFrame(), "Study - Assign Samples", AttachAnimalsManuallyDlg.class.getName());
		this.study = DAOStudy.getStudy(s.getId());
			
		AttachedBiosampleTableModel model = new AttachedBiosampleTableModel(Mode.MANUALASSIGN, study);
		
		animalTable = new AttachedBiosampleTable(model, false);
		animalTable.setCanAddRow(true);
		animalTable.setCanSort(true);

		JTextField studyField = new JTextField(8);
		studyField.setText(study.getStudyId());
		studyField.setEnabled(false);
		
		
		//biotype
		biotypeComboBox = new BiotypeComboBox(Biotype.removeAbstract(DAOBiotype.getBiotypes()), "Biotype");
		
		//CenterPane
		JButton attachButton = new JIconButton(IconType.SAVE, "Attach samples");
		attachButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new SwingWorkerExtended("Saving", animalTable, false) {
					@Override
					protected void done() {
						try {
							AttachBiosamplesHelper.attachSamples(study, animalTable.getRows(), null, false, Spirit.askForAuthentication());
							dispose();
							SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, study);			
						} catch (Exception ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(AttachAnimalsManuallyDlg.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						}
					}					
				};
			}
		});
		
		
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(BorderLayout.NORTH, UIUtils.createVerticalBox(BorderFactory.createEtchedBorder(), 
				UIUtils.createHorizontalBox(new JLabel("StudyId: "), studyField, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(new JLabel("Biotype: "), biotypeComboBox, Box.createHorizontalGlue())));		
		centerPanel.add(BorderLayout.CENTER, new JScrollPane(animalTable));	
		centerPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), new JButton(animalTable.new RegenerateSampleIdAction()), attachButton));
		setContentPane(centerPanel);
		
		refreshTable();
		initBiotype();
		
		biotypeComboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					animalTable.getModel().setBiotype(biotypeComboBox.getSelection());
					animalTable.resetPreferredColumnWidth();
				} catch(Exception ex) {
					initBiotype();
					JExceptionDialog.showError(ex);
				}
				
			}
		});

		

		
		UIUtils.adaptSize(this, 1050, 800);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
		
	}
	
	private void initBiotype() {
		Biotype biotype = null;
		List<Biosample> biosamples = AttachedBiosample.getBiosamples(animalTable.getRows());
		if(biosamples.size()>0) {
//			canChooseBiotype = true;
			for(Biosample b: biosamples) {
				if(b.getBiotype()==null) continue;
//				if(b.getMetadataAsString().length()>0) canChooseBiotype = false;
				if(biotype==null) {
					biotype = b.getBiotype();
				} else if(!biotype.equals(b.getBiotype())) {
					biotype = null;
//					canChooseBiotype = false;
					break;
				}
			}
		} else {
			biotype = DAOBiotype.getBiotype(Biotype.ANIMAL);
//			canChooseBiotype = true;
		}
		biotypeComboBox.setSelectedItem(biotype);
		
		try {
			animalTable.getModel().setBiotype(biotype);
			animalTable.resetPreferredColumnWidth();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void refreshTable() {
		
		
		//Recreate animal table, group per group
		List<AttachedBiosample> rows = new ArrayList<>();
		List<Biosample> topAttached = study.getTopAttachedBiosamples();
		if(topAttached.size()>0) {

			//Create a template from the existing animals
			for (Biosample b : topAttached) {
							
				AttachedBiosample row = new AttachedBiosample();
				row.setSampleId(b.getSampleId());
				row.setGroup(b.getInheritedGroup());
				row.setSubGroup(b.getInheritedSubGroup());
				row.setBiosample(b);
				row.setSampleName(b.getSampleName());
				if(b.getBiotype().getCategory()==BiotypeCategory.LIVING) {
					row.setContainerId(b.getContainerId());
				} else {
					row.setContainerId(null);					
				}
				rows.add(row);
			}
			
		} else {
			
			//Create an empty template from the existing animals
			for (Group group : study.getGroups()) {			
				for(int subgroup=0; subgroup<Math.max(1, group.getNSubgroups()); subgroup++) {
					int subgroupNo = group.getNSubgroups()<=1? 0: subgroup;
					List<AttachedBiosample> subRows = new ArrayList<AttachedBiosample>();
					for(int i=0; i<group.getSubgroupSize(subgroupNo); i++) {
						AttachedBiosample row = new AttachedBiosample();
						row.setGroup(group);
						row.setSubGroup(subgroupNo);
						subRows.add(row);
					}
					
					rows.addAll(subRows);
				}
			}
		}
				
		
		//always add some empty rows
		for(int n=0;n<10; n++) {
			rows.add(new AttachedBiosample());
		}
			
		animalTable.setRows(rows);
		
	}
	
	
		
	
	
	

			
	
}
