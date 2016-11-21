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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.StudyWizardDlg;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.PhaseFormat;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;

public class PhaseDlg extends JEscapeDialog {

	private final StudyWizardDlg dlg;
	private final Study study;
		
	private JGenericComboBox<PhaseFormat> formatComboBox = new JGenericComboBox<PhaseFormat>(PhaseFormat.values(), false);
	private JCustomTextField startingDayPicker = new JCustomTextField(JCustomTextField.DATE);
		
	private JLabel startingDateLabel = new JLabel();
	
	private PhaseEditTable phaseTable;
	private int push = 0;
	
	public PhaseDlg(final StudyWizardDlg dlg, final Study study) {
		super(dlg, "Study Wizard - Edit Phases");
		this.dlg = dlg;
		this.study = study;
		
		//Count biosamples/results
		Map<Phase, Pair<Integer, Integer>> phase2count = DAOStudy.countBiosampleAndResultsByPhase(study);
		
		phaseTable = new PhaseEditTable(study, phase2count);
		
		//formatPanel
		formatComboBox.setSelection(study.getPhaseFormat());
		formatComboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(formatComboBox.getSelection()==study.getPhaseFormat()) return;
				try {					
					for (Phase p : study.getPhases()) {
						if(formatComboBox.getSelection()==PhaseFormat.NUMBER && (p.getMinutes()!=0 || p.getHours()!=0)) {
							formatComboBox.setSelection(study.getPhaseFormat());
							throw new Exception("You cannot change the format to " + PhaseFormat.NUMBER.getDescription() + " if you have hours or minutes in your phases");
						}
					}
					for (Phase p : study.getPhases()) {
						p.setName(Phase.cleanName(p.getName(), formatComboBox.getSelection()));
					}					
					study.setPhaseFormat(formatComboBox.getSelection());
				} catch(Exception ex ) { 
					JExceptionDialog.showError(ex);
				}
				refresh();
				dlg.refresh();
			}
		});

		startingDayPicker.setTextDate(study.getFirstDate());
		startingDayPicker.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					study.setStartingDate(startingDayPicker.getTextDate());
					dlg.refresh();
				} catch(Exception e2) {
					JExceptionDialog.showError(e2);
				}
			}
		});
		startingDayPicker.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				try {
					study.setStartingDate(startingDayPicker.getTextDate());
					dlg.refresh();
				} catch(Exception e2) {
					JExceptionDialog.showError(e2);
				}
			}
		});
		JPanel formatPanel = UIUtils.createTable(
				new JLabel("Phase Format: "), formatComboBox, 
				new JLabel("Starting Date (opt.): "), startingDayPicker);

		
		refresh();
		phaseTable.setAutoscrolls(true);
		phaseTable.getModel().addTableModelListener(new TableModelListener() {			
			@Override
			public void tableChanged(TableModelEvent e) {		
				synchroTable();
			}
		});
		
		JButton addButton = new JButton("Add Phases");
		addButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				new PhaseAddDlg(PhaseDlg.this);
				refresh();
			}
		});
		JButton removeButton = new JButton("Remove");
		removeButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					phaseTable.getModel().getRows().removeAll(phaseTable.getSelection());
					phaseTable.getModel().fireTableDataChanged();
					synchroTable();
				} catch(Exception ex) {
					JExceptionDialog.showError(PhaseDlg.this, ex);
				}
			}
		});
		JButton selectEmpty = new JButton("Select Empty Phases");
		selectEmpty.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				selectEmpty();
			}
		});
		//ContentPane
		add(BorderLayout.CENTER, UIUtils.createBox(
				UIUtils.createTitleBox("Phases", 
					UIUtils.createBox(new JScrollPane(phaseTable), null, null, null, 
							UIUtils.createVerticalBox(
								UIUtils.createHorizontalBox(addButton, Box.createHorizontalGlue()),
								UIUtils.createHorizontalBox(selectEmpty, removeButton, Box.createHorizontalGlue()),
								Box.createVerticalGlue(),
								new JInfoLabel("<html><ul>"
								+ "<li>To rename, edit the cell"
								+ "<li>To insert a phase, right-click and insert"
								+ "<li>To remove a phase, right-click and remove"
								+ "<li>Phases will be sorted lexicographically")
							)
				)),					
				UIUtils.createTitleBox(null, formatPanel), Box.createGlue()));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), new JButton(new CloseAction())));
		UIUtils.adaptSize(this, 650, 660);
		refresh();
		setLocationRelativeTo(dlg);
		setVisible(true);
	}
	
	
	public Study getStudy() {
		return study;
	}
	
	private void synchroTable() {
		if(push>0) return;
		List<Phase> rows = phaseTable.getNonEmptyRows();
		System.out.println("PhaseDlg.synchroTable() rows="+rows);
		try {
			//Add new phases				
			Set<Phase> newPhases = new HashSet<>(rows);
			newPhases.removeAll(study.getPhases());
			for (Phase phase : newPhases) {
				phase.setStudy(study);
			}
			
			//Remove phases
			Set<Phase> removePhases = new HashSet<>(study.getPhases());
			removePhases.removeAll(rows);
			removePhases(removePhases);
			
			
			System.out.println("PhaseDlg.synchroTable() newPhases="+newPhases+" removePhases="+removePhases+" phases="+study.getPhases());
			
			//Sort phases
			study.setStartingDate(startingDayPicker.getTextDate());
			study.resetCache();
		} catch(Exception ex) {
			JExceptionDialog.showError(PhaseDlg.this, ex);
			refresh();
		} finally {
			dlg.refresh();					
		}
	}
	
	
	private void selectEmpty() {
		List<Phase> emptyPhases = new ArrayList<>();
		
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
		
		phaseTable.setSelection(emptyPhases);
		
	}
	
	public static void checkCanDelete(Phase phase) throws Exception {
		if(phase.getId()<=0) return;
		
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
	
	private void removePhases(Collection<Phase> phases) throws Exception {
		
		//Test that the user can delete the phase
		for(Phase phase: phases) {
			checkCanDelete(phase);			
		}
				
		//Delete
		for(Phase phase: phases) {
			phase.remove();
		}
		
	}
	
	public void refresh() {
		push++;
		try {
			List<Phase> phases = new ArrayList<>(study.getPhases());
			Collections.sort(phases);
			phaseTable.setRows(phases);
			startingDayPicker.setEnabled(study.getPhaseFormat()==PhaseFormat.DAY_MINUTES);
			startingDateLabel.setText(study.getPhaseFormat()!=PhaseFormat.DAY_MINUTES? "": study.getFirstDate()==null? ": N/A": ": "+FormatterUtils.formatDate(study.getFirstDate()));
		} finally {
			push--;
		}
		
	}
}
