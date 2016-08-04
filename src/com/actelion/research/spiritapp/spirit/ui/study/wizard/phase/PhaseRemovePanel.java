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

package com.actelion.research.spiritapp.spirit.ui.study.wizard.phase;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class PhaseRemovePanel extends JPanel {
	

	private final PhaseDlg dlg;
	
	private final Study study;
	private final PhaseList phaseList;

	
	public PhaseRemovePanel(final PhaseDlg dlg, PhaseList list) {
		this.dlg = dlg;
		this.study = dlg.getStudy();
		this.phaseList = list;

				
		
		JButton selectEmptyButton = new JButton("Select Empty Phases");
		selectEmptyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					selectEmpty();
				} catch (Exception e) {
					JExceptionDialog.showError(e);
				}
				
			}
		});
		
		JButton removeButton = new JButton("Remove Selected Phases");
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					removePhases();
					dlg.refresh();
				} catch (Exception e) {
					JExceptionDialog.showError(e);
				}
				
			}
		});
	
		
		setLayout(new BorderLayout());

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
				UIUtils.createTitleBox("Select...", new JScrollPane(phaseList)),
				UIUtils.createVerticalBox(
//						UIUtils.createHorizontalTitlePanel("Remove"), 
//						new JLabel("Note: Only phases without any biosamples or results can be removed"),
						UIUtils.createHorizontalBox(selectEmptyButton, Box.createHorizontalGlue()),
						UIUtils.createHorizontalBox(removeButton, Box.createHorizontalGlue()),
						Box.createVerticalGlue()));
		splitPane.setDividerLocation(100);		

		add(BorderLayout.CENTER, splitPane);
		
	}		
	
	private void selectEmpty() throws Exception {
		List<Phase> emptyPhases = new ArrayList<Phase>();
		
		for(Phase phase: study.getPhases()) {
			if(phase.getId()<=0) {
				emptyPhases.add(phase);	
				
			} else if(!phase.hasRandomization()) {
			
				boolean empty = true;			
				for(StudyAction action: study.getStudyActions(phase)) {
					if(!action.isEmpty()){
						empty = false;
						break;
					}
				}
				if(empty) emptyPhases.add(phase);
			}
		}
		
		if(emptyPhases.size()==0) throw new Exception("There are no empty phases");
		if(emptyPhases.size()==study.getPhases().size()) throw new Exception("All phases would be empty. This must be an error");
		
		phaseList.setSelectedPhases(emptyPhases);

		
	}
	
	private void removePhases() throws Exception {
		if(phaseList.getSelectedPhases().size()==0) throw new Exception("You must select a phase");
		
		//Test that the user can delete the phase
		for(Phase phase: phaseList.getSelectedPhases()) {		
			if(phase.getId()<=0) continue;
			
			//Exception if there are samples associated to this phase
			List<Biosample> samples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForPhase(phase), null);
			if(samples.size()>0) {
				throw new Exception("You cannot delete the phase "+phase+" because there are " +samples.size()+ " biosamples associated to it");
			}
			//Exception if there are results associated to this phase
			List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForPhase(phase), null);
			if(results.size()>0) {
				throw new Exception("You cannot delete the phase "+phase+" because there are " +results.size()+ " results associated to it");
			}
			
		}
		
		//Ask for confirmation
		int res = JOptionPane.showConfirmDialog(dlg, "Are you sure you want to delete " + (phaseList.getSelectedPhases().size()==1? phaseList.getSelectedPhases().get(0): "those "+phaseList.getSelectedPhases().size()+" phases")+ "?", "Delete Phases", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if(res!=JOptionPane.YES_OPTION) return;
		
		//Delete
		for(Phase phase: phaseList.getSelectedPhases()) {
			phase.remove();
		}


		
	}
	
}
