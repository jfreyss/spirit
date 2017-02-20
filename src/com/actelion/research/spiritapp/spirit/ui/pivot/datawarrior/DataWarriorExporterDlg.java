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

package com.actelion.research.spiritapp.spirit.ui.pivot.datawarrior;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorConfig;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorExporter;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class DataWarriorExporterDlg extends JEscapeDialog {

	private List<Result> results;
	
	private DataWarriorConfigPanel dataWarriorModelPanel;


	public DataWarriorExporterDlg(List<Result> results, Set<TestAttribute> skippedAttributes, PivotTemplate tpl) {
		super(UIUtils.getMainFrame(), "Export to DataWarrior");
		this.results = JPAUtil.reattach(results);
		if(results.size()==0) {
			JExceptionDialog.showError("You must perform the search first");
			return;
		}
		
		DataWarriorConfig model = DataWarriorConfig.createCustomModel(tpl);
		model.setSkippedAttributes(skippedAttributes);

		dataWarriorModelPanel = new DataWarriorConfigPanel(results, skippedAttributes);
		dataWarriorModelPanel.setDataWarriorModel(model);

		JButton okButton = new JIconButton(IconType.DATAWARRIOR, "Open DataWarrior");
		getRootPane().setDefaultButton(okButton);

		okButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					eventOk();
					dispose();
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});		
		
		JPanel centerPanel = new JPanel(new BorderLayout());		
		centerPanel.add(BorderLayout.NORTH, new JCustomLabel("<html><div style='width:100%; text-align:center'>Please select how you want your data to be exported:</div></html>", FastFont.BIGGEST));
		centerPanel.add(BorderLayout.CENTER, dataWarriorModelPanel /*UIUtils.createBox(dataWarriorModelPanel, choicePanel, null, null, null)*/);
		centerPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));
		centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		
		setContentPane(centerPanel);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
		
				
	}
	
//	private void refreshModel() {
//		DataWarriorConfig model;
//		if(groupButton.isSelected()) {
//			model = DataWarriorConfig.createGroupModel();
//		} else if(phaseButton.isSelected()) {
//			model = DataWarriorConfig.createPhaseModel();
//		} else {
//			model = DataWarriorConfig.createCustomModel(tpl);
//		}
//		dataWarriorModelPanel.setDataWarriorModel(model);					
//	}
	
	private void eventOk() throws Exception {
		StringBuilder sb;
		DataWarriorConfig model = dataWarriorModelPanel.getDataWarriorModel() ;
						
		sb = DataWarriorExporter.getDwar(results, model, Spirit.getUser());
		
		if(sb==null) throw new Exception("Exporting to DW is not supported in this view");
		
		File f = File.createTempFile("spirit_", ".dwar");
		FileWriter w = new FileWriter(f);
		com.actelion.research.util.IOUtils.redirect(new StringReader(sb.toString()), w);
		w.close();
		
		Desktop.getDesktop().open(f);
	}
	
}
