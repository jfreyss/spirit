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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.PhaseFormat;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class PhaseUpdatePanel extends JPanel {
	


	private final PhaseDlg dlg;
	
	private final Study study;
	private final PhaseList phaseList;

	
	private final JCustomTextField renameToTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 16);
	private final JButton renameButton = new JButton("Rename selected phase");
	
	private final JSpinner postponeSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
	private final JButton postponeButton = new JButton("Delay selected phases");
	private final JEditorPane postponeEditorPane = new JEditorPane("text/html", "");
	
	public PhaseUpdatePanel(final PhaseDlg dlg, PhaseList list) {
		this.dlg = dlg;
		this.study = dlg.getStudy();
		this.phaseList = list;
		
		
		renameButton.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					
					if(phaseList.getSelectedPhases().size()!=1) throw new Exception("You must select one phase");
					Phase sel = phaseList.getSelectedPhases().get(0);
					
					//Test creation of new phase
					Phase renameTo = new Phase(); 	
					renameTo.setStudy(study);
					renameTo.setName(renameToTextField.getText());
					
					//Rename
					renamePhase(study, sel, renameTo, null, false);
					
					dlg.refresh();
					
				} catch(Exception e) {
					JExceptionDialog.showError(PhaseUpdatePanel.this, e);
				}	
			}
		});
		
		postponeButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					List<Phase> sel = phaseList.getSelectedPhases();
					if(sel.size()<1) throw new Exception("You must select at least one phase");
					int increment = (Integer) postponeSpinner.getValue();
					if(increment==0) throw new Exception("You must select a value different from  0");
					//Check rename
					for(Phase p: sel) {
						Phase renameTo = new Phase(PhaseFormat.DAY_MINUTES.getName(p.getDays() + increment, p.getHours(), p.getMinutes(), p.getLabel()));
						renameTo.setStudy(study);
						renamePhase(study, p, renameTo, sel, true);
					}
					//Rename
					for(Phase p: sel) {
						Phase renameTo = new Phase(PhaseFormat.DAY_MINUTES.getName(p.getDays() + increment, p.getHours(), p.getMinutes(), p.getLabel()));
						renameTo.setStudy(study);
						renamePhase(study, p, renameTo, sel, false);
					}
					dlg.refresh();
				} catch(Exception e) {
					JExceptionDialog.showError(PhaseUpdatePanel.this, e);
				}	
			}
		});
		
		postponeEditorPane.setEnabled(false);
		postponeEditorPane.setEditable(false);
		postponeEditorPane.setPreferredSize(new Dimension(150, 80));
		
		phaseList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				refresh();
			}
		});
		postponeSpinner.addChangeListener(new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				refresh();
				
			}
		});
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
				UIUtils.createTitleBox("Select...", new JScrollPane(phaseList)),
				UIUtils.createBox(
						UIUtils.createTitleBox("Move several phases", 
								UIUtils.createBox(
									new JScrollPane(postponeEditorPane),
									UIUtils.createHorizontalBox(new JLabel("day: "), postponeSpinner, postponeButton, Box.createHorizontalGlue()))),
						UIUtils.createTitleBox("Rename/Move one phase", 
							UIUtils.createVerticalBox(
								UIUtils.createHorizontalBox(new JLabel("<html>Select a phase on the left, and enter the new name.<br> You can add also a label.<br><i>Accepted Format: 'd5',  'd5_1h', 'd5_1h Treatment'</i></html>"), Box.createHorizontalGlue()),
								UIUtils.createHorizontalBox(renameToTextField, renameButton, Box.createHorizontalGlue()),
								Box.createVerticalStrut(50)))));
		splitPane.setDividerLocation(100);		

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, splitPane);
		
		refresh();
		
		
		
	}		
	
	private void refresh() {
		
		if(study.getPhaseFormat()!=PhaseFormat.DAY_MINUTES) {

			renameToTextField.setText("");
			renameButton.setEnabled(false);
			renameToTextField.setEnabled(false);
			postponeSpinner.setEnabled(false);
			postponeButton.setEnabled(false);
		} else {
			List<Phase> sel = phaseList.getSelectedPhases();
			int increment = (Integer) postponeSpinner.getValue();

			renameToTextField.setText(sel.size()!=1?"": sel.get(0).toString());
			renameButton.setEnabled(sel.size()==1);
			renameToTextField.setEnabled(sel.size()==1);
			postponeSpinner.setEnabled(true);

			if(increment==0) {
				postponeButton.setEnabled(false);
				postponeEditorPane.setText("");				
			} else {
				postponeButton.setEnabled(sel.size()>=1);
					
				StringBuilder sb = new StringBuilder();
				for(Phase p: phaseList.getSelectedPhases()) {
					Phase renameTo = new Phase(p.getName());
					renameTo.setStudy(study);
					sb.append(p + " --&gt; "+ renameTo + " <br> ");
				}
				postponeEditorPane.setText("<div style='font-size:9px;color:gray'>"+sb);
				postponeEditorPane.setCaretPosition(0);
			}
		}
	}
	
	private void renamePhase(Study study, Phase phase, Phase renameTo, Collection<Phase> ignore, boolean checkOnly) throws Exception {		
		//Check unicity (excluding the phase being modified)
		for(Phase p: study.getPhases()) {
			if(p==phase) continue;
			if(ignore!=null && ignore.contains(p)) continue;
			if(p.toString().equals(renameTo.toString())) {
				throw new Exception("The phase "+renameTo+" must be unique");
			}
		}
		
		
		if(!checkOnly) {
			phase.setName(renameTo.getName());
			
		}
		dlg.refresh();
	}
	
}
