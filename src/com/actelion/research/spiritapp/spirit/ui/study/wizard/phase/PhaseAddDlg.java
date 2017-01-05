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
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.PhaseFormat;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class PhaseAddDlg extends JEscapeDialog {
	
//	private final PhaseDlg dlg;
	private final Study study;

	private JSpinner firstDaySpinner = new JSpinner(new SpinnerNumberModel(0, -99, 999, 1));		
	private JSpinner firstHourSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));		
	private JSpinner firstMinuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60, 1));		
	private JSpinner nSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
	private JSpinner dayIncreaseSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 900, 1));
	private JEditorPane overviewEditor = new ImageEditorPane();
	

	
	
	public PhaseAddDlg(final PhaseDlg dlg) {
		super(dlg, "Add Phases");
		this.study = dlg.getStudy();
		
		overviewEditor.setEnabled(false);
		LF.initComp(overviewEditor);
		
		JScrollPane sp = new JScrollPane(overviewEditor, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);		
		sp.setPreferredSize(new Dimension(80, 60));
		overviewEditor.setEditable(false);
		ChangeListener cl = new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				int n = (Integer) nSpinner.getValue();
				dayIncreaseSpinner.setEnabled(n>1);
				showPhases();
			}
		};
		cl.stateChanged(null);
		firstDaySpinner.addChangeListener(cl);
		firstHourSpinner.addChangeListener(cl);
		firstMinuteSpinner.addChangeListener(cl);
		dayIncreaseSpinner.addChangeListener(cl);
		nSpinner.addChangeListener(cl);
		
						
		if(study.getPhaseFormat()==PhaseFormat.DAY_MINUTES) {
			/////////////////////////////////////////////////////
			//Format = DAY_MINUTES
			JButton severalButton = new JButton("Add Phases");
			severalButton.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent e) {
					try {					
						Set<Phase> phases = getPhasesToBeAdded();
						for (Phase phase : phases) {
							phase.setStudy(study);
						}
						study.getPhases().addAll(phases);	
						dispose();
						dlg.refresh();
					} catch(Exception ex ) { 
						JExceptionDialog.showError(ex);
					}
				}
			});
			JPanel addSeveralPhasePanel = UIUtils.createTitleBox(UIUtils.createBox(
					sp,
					UIUtils.createTable(8, 5, 0, 
							new JLabel("Day: "), firstDaySpinner, Box.createHorizontalStrut(15), new JLabel("Hour: "), firstHourSpinner, Box.createHorizontalStrut(15), new JLabel("Minute: "), firstMinuteSpinner,
							Box.createVerticalStrut(20), null, null, null, null, null, null, null, 
							new JLabel("Repeat: "), nSpinner, new JLabel("every:"), null, null, null, null, null, 		
							new JLabel("Day: "), dayIncreaseSpinner)));
	
			
			add(BorderLayout.CENTER, addSeveralPhasePanel);
			add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), severalButton));
		} else {
			return;
		}
		pack();
		setLocationRelativeTo(dlg);
		setVisible(true);

		
	}		
	
	private Set<Phase> getPhasesToBeAdded() {
		Set<Phase> phases = new LinkedHashSet<>();

		Integer firstDay = (Integer) firstDaySpinner.getValue();
		Integer firstHour = (Integer) firstHourSpinner.getValue();
		Integer firstMinute = (Integer) firstMinuteSpinner.getValue();
		Integer n = (Integer) nSpinner.getValue();
		Integer incrementDay = (Integer) dayIncreaseSpinner.getValue();
		if(firstDay==null || n==null || firstHour==null || incrementDay==null || incrementDay==null) return phases;

		
		for (int i = 0; i < n; i++) {
			Phase p = new Phase(study.getPhaseFormat().getName(firstDay+i*incrementDay, firstHour, firstMinute, null));
			if(!study.getPhases().contains(p)) {
				phases.add(p);
			}
		}
		return phases;
	}
	

	private void showPhases() {
		Set<Phase> phases = getPhasesToBeAdded();
		System.out.println("PhaseAddDlg.showPhases() "+phases);
		StringBuffer sb = new StringBuffer();
		for (Phase phase : phases) {
			if(sb.length()>0) sb.append(", ");
			sb.append(phase.toString());
		}
		overviewEditor.setText("<html>" + (phases.size()>0? "<span style='font-size:9px; color:black'>" +phases.size()+" new phases: </span><br>": "")+ "<span style='font-size:9px; color:#666666'>"+sb+"</span></html>");
		overviewEditor.setCaretPosition(0);
	}
	

}
