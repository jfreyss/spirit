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

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jdesktop.swingx.JXDatePicker;

import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.StudyWizardDlg;
import com.actelion.research.spiritcore.business.study.PhaseFormat;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class PhaseDlg extends JEscapeDialog {

	private final StudyWizardDlg dlg;
	private final Study study;
		
	private JGenericComboBox<PhaseFormat> formatComboBox = new JGenericComboBox<PhaseFormat>(PhaseFormat.values(), false);
	private JXDatePicker startingDayPicker = new JXDatePicker();
		
	private JLabel startingDateLabel = new JLabel();
	private JTabbedPane tabbedPane = new JCustomTabbedPane();
	
	private final PhaseList phaseList1 = new PhaseList();
	private final PhaseList phaseList2 = new PhaseList();
	
	public PhaseDlg(final StudyWizardDlg dlg, final Study study) {
		super(dlg, "Study Wizard - Edit Phases");
		this.dlg  = dlg;
		this.study = study;
		
		
		
		
		//formatPanel
		formatComboBox.setSelection(study.getPhaseFormat());
		formatComboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(formatComboBox.getSelection()==study.getPhaseFormat()) return;
				try {					
					study.setPhaseFormat(formatComboBox.getSelection());
				} catch(Exception ex ) { 
					JExceptionDialog.showError(ex);
				}
				recreateTabPane();
				refresh();
			}
		});

		startingDayPicker.setDate(study.getFirstDate());
		startingDayPicker.setFormats(FormatterUtils.dateFormat);		
		startingDayPicker.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				study.setStartingDate(startingDayPicker.getDate());
				refresh();
			}
		});
		JPanel formatPanel = UIUtils.createVerticalBox(
				UIUtils.createHorizontalBox(new JLabel("Phase Format: "), formatComboBox, Box.createHorizontalGlue()), 
				UIUtils.createHorizontalBox(new JLabel("Starting Date (opt.): "), startingDayPicker, Box.createHorizontalGlue()));

		
		//TabbedPane
		recreateTabPane();
		
		//ContentPane
		add(BorderLayout.CENTER, UIUtils.createBox(tabbedPane, UIUtils.createTitleBox(null, formatPanel), Box.createGlue()));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), new JButton(new CloseAction())));
		UIUtils.adaptSize(this, 650, 620);
		refresh();
		setLocationRelativeTo(dlg);
		setVisible(true);
	}
	
	
	public Study getStudy() {
		return study;
	}
	
	public void recreateTabPane() {
		int index = tabbedPane.getSelectedIndex();
		tabbedPane.removeAll();
		phaseList1.setPhases(study.getPhases());
		phaseList2.setPhases(study.getPhases());

		
		if(formatComboBox.getSelection()==PhaseFormat.DAY_MINUTES) {
			tabbedPane.addTab("Add Phases", new PhaseAddPanel(this));		
			tabbedPane.addTab("Update Phases", new PhaseUpdatePanel(this, phaseList1));
			tabbedPane.addTab("Remove Phases", new PhaseRemovePanel(this, phaseList2));
		} else {
			tabbedPane.addTab("Set Phases", new PhaseAddPanel(this));		
			tabbedPane.addTab("Remove Phases", new PhaseRemovePanel(this, phaseList2));			
		}
		
		tabbedPane.setSelectedIndex(index>=0 && index<tabbedPane.getTabCount()? index: 0);
		tabbedPane.validate();
	}
	

	public void refresh() {
		recreateTabPane();
		startingDayPicker.setEnabled(study.getPhaseFormat()==PhaseFormat.DAY_MINUTES);
		startingDateLabel.setText(study.getPhaseFormat()!=PhaseFormat.DAY_MINUTES? "": study.getFirstDate()==null? ": N/A": ": "+FormatterUtils.formatDate(study.getFirstDate()));
		
		dlg.refresh();
	}
}
