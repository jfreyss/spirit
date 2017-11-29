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

package com.actelion.research.spiritapp.ui.study.randomize;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.report.StudyGroupAssignmentReport;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.study.edit.AttachedBiosampleTable;
import com.actelion.research.spiritapp.ui.study.edit.AttachedBiosampleTableModel;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Randomization;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class SummaryTab extends WizardPanel {
	
	private final RandomizationDlg dlg;
	private Study study;
	private AttachedBiosampleTable rndTable;
	
	private final JButton numberingButton = new JButton("Automatic Renumbering");
	private final JCheckBox saveWeightCheckbox = new JCheckBox("Save weights as results", true);
	
	public SummaryTab(final RandomizationDlg dlg) {
		this.dlg = dlg;
		this.study = dlg.getStudy();
		
		//Create the table
		rndTable = new AttachedBiosampleTable(new AttachedBiosampleTableModel(AttachedBiosampleTableModel.Mode.RND_SUMMARY, study, null, dlg.getPhase()), false);
		rndTable.setDragEnabled(false);
		rndTable.setDropTarget(null);
		
		JPanel centerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1; c.fill = GridBagConstraints.BOTH;
		c.weighty = 0; 
		c.weighty = 1; 
		c.gridx = 0; c.gridy = 1; centerPanel.add(new JScrollPane(rndTable), c);
		c.weightx = 0; c.weighty = 0; c.fill = GridBagConstraints.NONE;		
		
		rndTable.setGoNextOnEnter(false);
		rndTable.setCanSort(true);
		
		JButton reportButton = new JIconButton(IconType.EXCEL, "Report");
		reportButton.setEnabled(dlg.getStudy()!=null);
		reportButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					StudyGroupAssignmentReport rep = new StudyGroupAssignmentReport();
					rep.populateReport(dlg.getStudy());
					rep.export(null);
				} catch (Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});
		
		
		JButton saveButton = new JIconButton(IconType.SAVE, "Make the assignments");
		saveButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					terminateRandomization();
					dlg.dispose();					
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, dlg.getStudy());
					JExceptionDialog.showInfo(dlg, "Randomization and assignments saved");
				} catch (Exception ex) {
					JExceptionDialog.showError(dlg, ex);
				}

				
			}
		});
		
		numberingButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				//Count the number of samples
				int needed = 0;
				for (AttachedBiosample rndSample : rndTable.getRows()) {
					if(rndSample.getSampleName()==null || rndSample.getSampleName().length()==0) {
						needed++;
					}
				}

				if(needed==0) {
					JOptionPane.showMessageDialog(SummaryTab.this, "Renumbering is not possible before clearing the existing numbers", "Renumbering", JOptionPane.WARNING_MESSAGE);
					renumberEvent(-1);
					return;
				}
				
				int n = suggestFirstNumber();				
				Object res = JOptionPane.showInputDialog(SummaryTab.this, "Enter the first No:", "Renumbering", JOptionPane.QUESTION_MESSAGE, null, null, ""+n);
				if(res==null) return;
				try {
					n = Integer.parseInt(res.toString());
				} catch(Exception ex) {
					JExceptionDialog.showError(SummaryTab.this, res + " is not a valid number");
					return;
				}	
				renumberEvent(n);
			}
		});
		
		add(BorderLayout.NORTH, UIUtils.createTitleBox("", UIUtils.createVerticalBox(
				new JLabel("Please confirm the assignment after checking the samples and their group/subgroups"), 
				UIUtils.createHorizontalBox(numberingButton, new JInfoLabel("Renumber the animals sequentially starting at a given number"), Box.createHorizontalGlue()))));
		add(BorderLayout.CENTER, centerPanel);
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(/*dlg.createSaveButton(),*/ reportButton, Box.createHorizontalGlue(), saveWeightCheckbox, saveButton));

	}

	@Override
	public void updateModel(boolean allowDialogs) throws Exception {
		
	}

	/**
	 * Find last number from the animals used in the study
	 * @return
	 */
	public int suggestFirstNumber() {
		int max = 0;
		for(Biosample b: study.getParticipantsSorted()) {
			try {
				max = Math.max(max, Integer.parseInt(b.getSampleName()));
			} catch(Exception e) {
				
			}			
		}
		return max + 1;		
	}
	private void renumberEvent(int start) {
		dlg.setMustAskForExit(true);
		List<AttachedBiosample> samples = rndTable.getRows();		
		//Renumber
		if(start>0) {
			for (AttachedBiosample rndSample : samples) {
				if(rndSample.getSampleName()==null || rndSample.getSampleName().length()==0) {
					rndSample.setSampleName("" +  (start++));
				}
			}
		}
		reassignSubGroups(study, samples);
		repaint();
	}
	
	@Override
	public void updateView() {
		List<AttachedBiosample> samples = new ArrayList<AttachedBiosample>(dlg.getRandomization().getSamples());
		Collections.sort(samples, new Comparator<AttachedBiosample>() {
			@Override
			public int compare(AttachedBiosample o1, AttachedBiosample o2) {
				int c = CompareUtils.compare(o1.getGroup(), o2.getGroup());
				if(c!=0) return c;
				c = CompareUtils.compare(o1.getContainerId(), o2.getContainerId());
				if(c!=0) return c;
				return CompareUtils.compare(o1.getSampleName(), o2.getSampleName());
			}
		});
			
		rndTable.getModel().setNData(dlg.getRandomization().getNData());
		try {
			rndTable.getModel().setBiotype(dlg.getBiotype());
			rndTable.resetPreferredColumnWidth();
		} catch(Exception e) {
			JExceptionDialog.showError(e);
		}
		rndTable.setRows(samples);
		
		
		//renumber if none has a number or if one of the 
		boolean hasNumber = false;
		boolean hasSubGroups = false;
		for (AttachedBiosample rndSample : samples) {
			if(rndSample.getSampleName()!=null) hasNumber = true;
			if(rndSample.getSubGroup()>1) hasSubGroups = true;
		}

		if(!hasNumber || !hasSubGroups) renumberEvent(-1);
		
	}
	
	
	
	
	
	public void terminateRandomization() throws Exception {
		if(study==null) throw new Exception("This option is only possible in the context of a study");
		boolean saveWeights = saveWeightCheckbox.isSelected();
		
		SpiritUser user = Spirit.askForAuthentication();
		Randomization randomization = dlg.getRandomization();

		
		//save
		DAOStudy.persistStudies(Collections.singleton(study), SpiritFrame.getUser());
		AttachBiosamplesHelper.attachSamples(study, randomization.getSamples(), saveWeights? dlg.getPhase(): null, saveWeights, user);

	}

	public static void reassignSubGroups(Study study, List<AttachedBiosample> samples) {
		
		//
		//Assign subgroups when possible
		//Create a map of group -> array of subgroup -> n of allowed animals
		Map<Group, int[]> group2left = new HashMap<Group, int[]>();
		for(Group gr: study.getGroups()) {
			if(gr.getNSubgroups()>0) {
				int[] left = new int[gr.getNSubgroups()];
				for (int i = 0; i < left.length; i++) {
					left[i] = gr.getSubgroupSize(i);
				}
				group2left.put(gr, left);
			}
		}
		
		
		
		//assign the sample to subgroups
		loop: for (AttachedBiosample rndSample : samples) {
			rndSample.setSubGroup(1);

			if(rndSample.getGroup()==null) continue;
			
			
			int[] left = group2left.get(rndSample.getGroup());
			if(left==null) continue;
			
			//Create a new subgroup
			for (int i = 0; i < left.length; i++) {
				if(left[i]>0) {
					rndSample.setSubGroup(i);
					left[i]--;
					continue loop;
				}
			}
			rndSample.setSubGroup(left.length-1); //always the last one by default
		}
		
		
	
		
	}
}
