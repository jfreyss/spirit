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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.BorderFactory;
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

import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.PhaseFormat;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class PhaseAddPanel extends JPanel {
	
	private final PhaseDlg dlg;
	private final Study study;

	private JSpinner firstDaySpinner = new JSpinner(new SpinnerNumberModel(0, -99, 9999, 1));		
	private JSpinner firstHourSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));		
	private JSpinner lastDaySpinner = new JSpinner(new SpinnerNumberModel(7, -99, 9999, 1));
	private JSpinner dayIncreaseSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 900, 1));
	private JEditorPane overviewEditor = new JEditorPane("text/html", "");
	
	private final JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(0, -99, 9999, 1));
	private final JSpinner hourSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 24, 1));
	private final JSpinner minuteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60, 1));
	private final JCustomTextField labelTextField = new JCustomTextField(10, "", "Label");

	
	
	public PhaseAddPanel(final PhaseDlg dlg) {
		setLayout(new BorderLayout());
		this.dlg = dlg;
		this.study = dlg.getStudy();
		
		overviewEditor.setEnabled(false);

		JScrollPane sp = new JScrollPane(overviewEditor, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);		
		sp.setPreferredSize(new Dimension(80, 60));
		overviewEditor.setEditable(false);
		ChangeListener cl = new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				showPhases();
			}
		};
		showPhases();
		firstDaySpinner.addChangeListener(cl);
		lastDaySpinner.addChangeListener(cl);
		firstHourSpinner.addChangeListener(cl);
		dayIncreaseSpinner.addChangeListener(cl);
		
				
		
		if(study.getPhaseFormat()==PhaseFormat.DAY_MINUTES) {
			/////////////////////////////////////////////////////
			//Format = DAY_MINUTES
			JButton severalButton = new JButton("Add Phases");
			severalButton.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent e) {
					try {					
						Set<Phase> phases = getPhasesToBeAdded();
						study.getPhases().addAll(phases);						
						dlg.refresh();
					} catch(Exception ex ) { 
						JExceptionDialog.showError(ex);
					}
				}
			});
			JPanel addSeveralPhasePanel = UIUtils.createTitleBox("Option 1: Add incremental phases",
					UIUtils.createBox(
						sp,
						UIUtils.createTable(3, 5, 0, 
								new JCustomLabel("First Phase: ", FastFont.BOLD), new JLabel("Day: "), UIUtils.createHorizontalBox(firstDaySpinner, Box.createHorizontalStrut(15), new JLabel("Hour: "), firstHourSpinner),
								new JCustomLabel("Last Phase: ", FastFont.BOLD), new JLabel("Day: "), lastDaySpinner,		
								new JCustomLabel("Increment: ", FastFont.BOLD), new JLabel("Day: "), dayIncreaseSpinner),
						UIUtils.createHorizontalBox(Box.createHorizontalGlue(), severalButton)));
	
			
			//addOnePhasePanel
			JButton oneButton = new JButton("Add Phase");
			oneButton.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent e) {
					try {					
						addPhase();
					} catch(Exception ex ) { 
						JExceptionDialog.showError(ex);
					}
				}
			});
			final JEditorPane textarea = new JEditorPane();
			StringBuilder sb = new StringBuilder();
			if(study.getPhases().size()>0) {
				for(Phase p: study.getPhases()) {
					sb.append((sb.length()>0?", ":"") + p.getName());
				}
			}
			textarea.setText(sb.toString());
			
			JButton okButton = new JButton("Set/Update Phases");
			okButton.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent e) {
					try {					
						String[] lines = textarea.getText().split("[,\n]");
						Iterator<Phase> iterator = new ArrayList<>(study.getPhases()).iterator();
						for (String line : lines) {
							if(line.trim().length()==0) continue;
							if(iterator.hasNext()) {
								Phase phase = iterator.next();
								phase.setName(line);
							} else {
								Phase phase = new Phase(); 
								phase.setStudy(study);
								phase.setName(line);
								study.getPhases().add(phase);
							}	
												
						}
						while(iterator.hasNext()) {
							Phase phase = iterator.next();
							for(StudyAction a: study.getStudyActions(phase)) {
								if(!a.isEmpty()) throw new Exception("You must delete the actions of "+phase+" before deleting the phase");
							}
							phase.remove();
						}
						dlg.refresh();
					} catch(Exception ex ) { 
						JExceptionDialog.showError(ex);
					}
				}
			});
			JPanel freeFormatPanel = UIUtils.createTitleBox("Option 2: Free Text", 
					UIUtils.createBox(
						new JScrollPane(textarea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
						new JCustomLabel("Enter the phases separated by comma, using the appropriate format"),
					UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton)));
					
			//
			add(BorderLayout.CENTER, UIUtils.createBox(freeFormatPanel, addSeveralPhasePanel));
		} else {
			/////////////////////////////////////////////////////
			//Format = NUMBER
			final JEditorPane textarea = new JEditorPane();
			StringBuilder sb = new StringBuilder();
			if(study.getPhases().size()>0) {
				for(Phase p: study.getPhases()) {
					sb.append((sb.length()>0?", ":"") + p.getName());
				}
			} else {
				sb.append("1. Baseline\n2.\n3. EOF");
			}
			textarea.setText(sb.toString());
			
			JButton okButton = new JButton("Set/Update Phases");
			okButton.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent e) {
					try {					
						String[] lines = textarea.getText().split("[,\n]");
						Iterator<Phase> iterator = new ArrayList<>(study.getPhases()).iterator();
						for (String line : lines) {
							
							if(iterator.hasNext()) {
								Phase phase = iterator.next();
								phase.setName(line);
							} else {
								Phase phase = new Phase(); 
								phase.setStudy(study);
								phase.setName(line);
								study.getPhases().add(phase);
							}							
						}
						while(iterator.hasNext()) {
							Phase phase = iterator.next();
							for(StudyAction a: study.getStudyActions(phase)) {
								if(!a.isEmpty()) throw new Exception("You must delete the actions of "+phase+" before deleting the phase");
							}
							phase.remove();
						}
						dlg.refresh();
					} catch(Exception ex ) { 
						JExceptionDialog.showError(ex);
					}
				}
			});
			add(BorderLayout.CENTER, UIUtils.createBox(
					new JScrollPane(textarea),
					UIUtils.createVerticalBox(
							UIUtils.createHorizontalTitlePanel("Enter Phases"),
							new JLabel("Format: '1', '1. Beginning'")),
					UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton)));
		}
		setBorder(BorderFactory.createEtchedBorder());

		
	}		
	
	private void addPhase() throws Exception {
		Phase p = new Phase();
		p.setName(study.getPhaseFormat().getName((Integer) daySpinner.getValue(), (Integer) hourSpinner.getValue(), (Integer) minuteSpinner.getValue(), labelTextField.getText()));
		p.setStudy(study);
		
		if(study.getPhases().contains(p)) {
			throw new Exception(p.getShortName()+" exists already");
		}
		study.getPhases().add(p);
		
		dlg.refresh();

	}
	
	private Set<Phase> getPhasesToBeAdded() {
		Set<Phase> phases = new LinkedHashSet<Phase>();

		Integer firstDay = (Integer) firstDaySpinner.getValue();
		Integer firstHour = (Integer) firstHourSpinner.getValue();
		Integer lastDay = (Integer) lastDaySpinner.getValue();
		Integer incrementDay = (Integer) dayIncreaseSpinner.getValue();
		if(firstDay==null || lastDay==null || firstHour==null || incrementDay==null || incrementDay<=0) return phases;

		
		for (int i = firstDay; i <= lastDay; i+=incrementDay) {
			Phase p = new Phase(study.getPhaseFormat().getName(i, firstHour, 0, null));
			p.setStudy(study);

			if(!study.getPhases().contains(p)) {
				phases.add(p);
			}
		}
		return phases;
	}
	

	private void showPhases() {
		Set<Phase> phases = getPhasesToBeAdded();
		StringBuffer sb = new StringBuffer();
		for (Phase phase : phases) {
			if(sb.length()>0) sb.append(", ");
			sb.append(phase.toString());
		}
		overviewEditor.setText("<html>" + (phases.size()>0? "<span style='font-size:9px; color:black'>" +phases.size()+" new phases: </span><br>": "")+ "<span style='font-size:9px; color:#666666'>"+sb+"</span></html>");
		overviewEditor.setCaretPosition(0);
	}
	

}
