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

package com.actelion.research.spiritapp.animalcare.ui.randomize;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.spiritapp.spirit.ui.study.edit.AttachedBiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.study.edit.AttachedBiosampleTableModel;
import com.actelion.research.spiritapp.spirit.ui.study.edit.AttachedBiosampleTableModel.Mode;
import com.actelion.research.spiritapp.spirit.ui.util.component.BalanceDecorator;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

/**
 * Randomization Tab to let the user enter the animal Ids and their weights
 * 
 * @author freyssj
 *
 */
public class DataTab extends WizardPanel {
	private JLabel resetLabel = new JLabel();
	private RandomizationDlg dlg;
	private AttachedBiosampleTable dataTable;
	private AttachedBiosampleTable realTable;
	
	private SpinnerNumberModel animalsSpinnerModel = new SpinnerNumberModel(0, 0, 999, 1);
	private JSpinner animalsSpinner = new JSpinner(animalsSpinnerModel);
	private JLabel minAnimalLabel = new JCustomLabel("", FastFont.SMALL);

	private SpinnerNumberModel dataSpinnerModel = new SpinnerNumberModel(0, 0, 10, 1);
	private JSpinner dataSpinner = new JSpinner(dataSpinnerModel);

	private JButton reuseAnimalsButton = new JIconButton(IconType.STUDY, "Reuse Specimen (Crossover)");	
	private JButton resetButton = new JIconButton(IconType.NEW, "Synchronize from current situation");	
	private final BalanceDecorator balanceDecorator;
	private final JPanel realPanel;
	
	public DataTab(final RandomizationDlg dlg) {
		super(new BorderLayout());
		this.dlg = dlg;
		this.balanceDecorator = new BalanceDecorator(dlg);		
		this.dataTable = new AttachedBiosampleTable(new AttachedBiosampleTableModel(Mode.RND_WEIGHING, dlg.getStudy(), null, dlg.getPhase(), dlg.getBiotype()), false);
		this.realTable = new AttachedBiosampleTable(new AttachedBiosampleTableModel(Mode.RND_WEIGHING, dlg.getStudy(), null, dlg.getPhase(), dlg.getBiotype()), false);
		dataTable.setGoNextOnEnter(false);
		
		animalsSpinner.addChangeListener(new ChangeListener() {
			private int push = 0;
			@Override
			public void stateChanged(ChangeEvent e) {
				if(push>0) return;
				push++;
				try {
					int val = (Integer) animalsSpinner.getValue();
					try {
						dlg.getRandomization().setNAnimals(val);	
						updateView();
					} catch(Exception ex) {
						JExceptionDialog.showError(ex);
						animalsSpinner.setValue(dlg.getRandomization().getNAnimals());
					}
				} finally {
					push--;
				}
				
			}
		});
		
		dataSpinner.addChangeListener(new ChangeListener() {
			private int push = 0;
			@Override
			public void stateChanged(ChangeEvent e) {
				if(push>0) return;
				push++;
				try {
					int val = (Integer) dataSpinner.getValue();
					try {
						dlg.getRandomization().setNData(val);	
						updateView();
					} catch(Exception ex) {
						JExceptionDialog.showError(ex);
						animalsSpinner.setValue(dlg.getRandomization().getNAnimals());
					}
				} finally {
					push--;
				}
				
			}
		});
		
		reuseAnimalsButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				populateTables(true, false);
				repaint();
			}
		});
		
		resetButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				populateTables(false, false);
				repaint();

			}
		});
		resetButton.setVisible(false);
		realPanel = UIUtils.createTitleBox("Current Data (if groups have been changed)", new JScrollPane(realTable));
		JSplitPane centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				UIUtils.createTitleBox("Randomization Data",
						UIUtils.createBox(new JScrollPane(dataTable), UIUtils.createHorizontalBox(resetLabel, reuseAnimalsButton, resetButton, Box.createHorizontalGlue()))),
				realPanel);
		centerPanel.setDividerLocation(350);
		realTable.setEnabled(false);
		
		
		//Layout
		add(BorderLayout.CENTER, UIUtils.createBox(
				centerPanel,
				UIUtils.createTitleBox("",  UIUtils.createTable(3,
						new JLabel("Number of attached samples: "), animalsSpinner, minAnimalLabel, 
						new JLabel("Number of other data: "), dataSpinner, new JInfoLabel("if the randomization must be on other parameters than the weight"))),
				UIUtils.createHorizontalBox(balanceDecorator.getBalanceCheckBox(), new JButton(dataTable.new RegenerateSampleIdAction()), Box.createHorizontalGlue(), getNextButton())));
		
		
		

		//Fill in real tables
		populateTables(false, true);
		
		//Fill in data table
		updateView();
		
		//hide real table if the 2 tables are the same
		if(realTable.getRows().size()==dataTable.getRows().size()) {
			boolean equal = true;
			for (int i = 0; equal && i < realTable.getRows().size(); i++) {
				AttachedBiosample r1 = realTable.getRows().get(i);
				AttachedBiosample r2 = dataTable.getRows().get(i);
				if(!CompareUtils.equals(r1.getBiosample(), r2.getBiosample())) equal = false;
				if(!CompareUtils.equals(r1.getSampleId(), r2.getSampleId())) equal = false;
				if(!CompareUtils.equals(r1.getNo(), r2.getNo())) equal = false;
				if(!CompareUtils.equals(r1.getGroup(), r2.getGroup())) equal = false;
				if(!CompareUtils.equals(r1.getContainerId(), r2.getContainerId())) equal = false;
				if(!CompareUtils.equals(r1.getSubGroup(), r2.getSubGroup())) equal = false;
				
			}
			if(equal) {
				realPanel.setVisible(false);
			}
		}

	}
	
	/**
	 * Retrieve the samples from the study and not from the saved randomization
	 */
	private void populateTables(boolean comingFromCrossover, boolean populateRealTable) {
		List<AttachedBiosample> rows = new ArrayList<>();
		
		Study study = dlg.getStudy();
		
		//Find accepted groups
		boolean isGroupSplitting = true;
		Set<Group> acceptedGroups = new HashSet<>();
		for (Group group : dlg.getGroups()) {
			if(!dlg.getPhase().equals(group.getFromPhase())) continue;
			if(group.getFromGroup()==null) isGroupSplitting = false;

			acceptedGroups.add(group);
			acceptedGroups.add(group.getFromGroup());
		}
		
		//Find samples coming from the db
		Set<Integer> usedNo = new HashSet<>(); 
		int count = 0;
		try {
			DAOResult.attachOrCreateStudyResultsToSpecimen(study, study.getTopAttachedBiosamples(), dlg.getPhase(), false);
		} catch(Exception e) {
			JExceptionDialog.showError(e);
		}
		
		for(Biosample b: study.getTopAttachedBiosamples()) {
			
			//Skip this sample if it belongs to a group which should not be randomized
			if(!comingFromCrossover) {
				if(!isGroupSplitting && b.getInheritedGroup()==null) {
					//OK		
				} else if(b.getInheritedGroup()!=null &&
						(acceptedGroups.contains(b.getInheritedGroup()) ||
								(b.getInheritedGroup().getFromGroup()!=null && dlg.getPhase().equals(b.getInheritedGroup().getFromGroup().getFromPhase()) && acceptedGroups.contains(b.getInheritedGroup().getFromGroup())))) {
					//Ok		
				} else {
					continue;
				}
			}
			Test test = DAOTest.getTest(DAOTest.WEIGHING_TESTNAME);
			Result weighResult = b.getAuxResult(test, dlg.getPhase());

			AttachedBiosample s = new AttachedBiosample();
			s.setSampleId(b.getSampleId());
			s.setBiosample(b);
			
			try {
				s.setNo(Integer.parseInt(b.getSampleName()));
			} catch(Exception e) {
				s.setNo(count+1);	
			}
			
			s.setSampleName(b.getSampleName());
			
			if(!comingFromCrossover) {
				s.setGroup(b.getInheritedGroup());
				s.setSubGroup(b.getInheritedSubGroup());
			}
			
			s.setContainerId(b.getContainerId());
			s.setWeight(weighResult==null || weighResult.getOutputResultValues().size()<1? null: weighResult.getOutputResultValues().get(0).getDoubleValue());
			
			
			rows.add(s);
			usedNo.add(rows.get(count).getNo());
			count++;
		}
		

		
		Collections.sort(rows);
		
		if(populateRealTable) {
			realTable.getModel().setNData(dlg.getRandomization().getNData());
			realTable.setRows(rows);
			realPanel.setVisible(rows.size()>0);
		} else {
			dlg.getRandomization().setSamples(rows);
			realTable.getModel().setNData(dlg.getRandomization().getNData());
			dataTable.setRows(rows);
			animalsSpinner.setValue(rows.size());
			dataSpinner.setValue(0);
		}
	}
	
	@Override
	public void updateModel(boolean allowDialogs) throws Exception {	

		//Check that animals are not duplicated and not empty
		Set<String> sampleIds = new HashSet<String>();
		List<AttachedBiosample> notEmpty = new ArrayList<AttachedBiosample>();
		for(AttachedBiosample r: dataTable.getRows()) {
			if(r.getSampleId()==null || r.getSampleId().length()==0) continue;
			if(sampleIds.contains(r.getSampleId())) throw new Exception("The SampleId "+r.getSampleId()+" is duplicated");
			sampleIds.add(r.getSampleId());
			notEmpty.add(r);
		}
		
		if(allowDialogs && notEmpty.size()!=dataTable.getRows().size()) {
			int res = JOptionPane.showConfirmDialog(this, "Would you like to remove the empty samples and keep only " + notEmpty.size()+ " samples?", "Empty Samples", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(res==JOptionPane.YES_OPTION) {
				//remove empty
				dlg.getRandomization().setSamples(notEmpty);
			} else if (res==JOptionPane.NO_OPTION) {
				//keep empty (strange??)
				dlg.getRandomization().setSamples(dataTable.getRows());
			}
		} else {
			//ok
			dlg.getRandomization().setSamples(dataTable.getRows());
		}
	}
	
	@Override
	public void updateView() {
		try { 
			dataTable.getModel().setBiotype(dlg.getBiotype());
			dataTable.resetPreferredColumnWidth();
		} catch(Exception e) {
			JExceptionDialog.showError(e);
		}
			
		
		//Update the number of animals
		int v = dlg.getRandomization().getNAnimals();
		if(v<=0) v = getNeededAnimalsInRando();
		animalsSpinner.setValue(v);
		try {
			dlg.getRandomization().setNAnimals(v);
		} catch(Exception e) {
			
		}
		
		dataSpinner.setValue(dlg.getRandomization().getNData());

		
		List<AttachedBiosample> samples = dlg.getRandomization().getSamples();
		Collections.sort(samples);

		//Check if the rando was started already
		boolean alreadyStarted = false;
		for (AttachedBiosample rndSample : samples) {
			if(rndSample.getSampleId()!=null && rndSample.getSampleId().length()>0) alreadyStarted = true;
		}
		
		//Check the number of attached samples coming from the corresponding groups
		int nAttached = 0;
		boolean hasGroupSplitting = false;
		for(Group g: dlg.getStudy().getGroups()) {
			if(!dlg.getPhase().equals( g.getFromPhase())) continue;
			if(g.getFromGroup()!=null) hasGroupSplitting = true;
			nAttached += g.getTopAttachedBiosamples().size();
			if(g.getFromGroup()!=null) nAttached += g.getFromGroup().getTopAttachedBiosamples().size();			
		}
		
		//Update view
		resetButton.setVisible(nAttached>0);
		reuseAnimalsButton.setVisible(nAttached==0 && dlg.getStudy().getTopAttachedBiosamples().size()>0 && !hasGroupSplitting);		
		realTable.getModel().setNData(dlg.getRandomization().getNData());
		dataTable.getModel().setNData(dlg.getRandomization().getNData());
		dataTable.setRows(samples);
		
		if(!alreadyStarted) {
			if(nAttached==0) {
				resetLabel.setText("Please enter the samplesIds and their weights");
				resetLabel.setForeground(Color.DARK_GRAY);
			} else {
				populateTables(false, false);
				resetLabel.setText("Those sampleIds are retrieved from the current situation in the database.");
				resetLabel.setForeground(Color.BLUE);
			}			
		} else {
			resetLabel.setText("Those samples are loaded from the last saved settings (and may not reflect the current situation)");
			resetLabel.setForeground(Color.DARK_GRAY);
		}
			

	}
	
	private int getNeededAnimalsInRando() {
		int minAnimals = 0;
		Set<Group> seen = new HashSet<Group>();
		
		for (Group group : dlg.getGroups()) {
			if(!dlg.getPhase().equals(group.getFromPhase())) continue;
			if(group.getFromGroup()!=null) {
				if(!seen.contains(group.getFromGroup())) {
					seen.add(group.getFromGroup());
					minAnimals += group.getFromGroup().getNAnimals(dlg.getPhase());
				}
			}				
			minAnimals += group.getNAnimals(dlg.getPhase());
		}
		minAnimalLabel.setText("(>="+minAnimals+")");
		return minAnimals;
	}

}
