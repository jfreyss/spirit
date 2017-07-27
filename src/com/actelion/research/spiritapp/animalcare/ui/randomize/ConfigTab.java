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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.lf.BiotypeComboBox;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class ConfigTab extends WizardPanel {
	private final RandomizationDlg dlg;
	

	private JCustomTextField licenseNoTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 12);
	private JCustomTextField experimenterTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 12);
	private JLabel groupsLabel = new JLabel();
	private BiotypeComboBox biotypeComboBox;
	private JButton resetButton = new JIconButton(IconType.CLEAR, "Reset and restart this randomization");
	
	public ConfigTab(final RandomizationDlg dlg) {
		super(new BorderLayout());
		this.dlg = dlg;

		biotypeComboBox = new BiotypeComboBox(Biotype.removeAbstract(DAOBiotype.getBiotypes()), "Biotype");
		
		
		//RandomizePanel
		experimenterTextField.setText(SpiritFrame.getUser()==null?"":SpiritFrame.getUser().getUsername());
		
		//GroupPanel
		refreshGroupPanel();
		JPanel groupsPanel = UIUtils.createTitleBox("Groups to be assigned", 
				UIUtils.createBox(groupsLabel, null, UIUtils.createHorizontalBox(resetButton, Box.createHorizontalGlue())));		
		
		//GeneralInfoPanel
		JPanel generalPanel = UIUtils.createTitleBox("General Info", UIUtils.createTable(
				new JLabel("Biotype: "), biotypeComboBox,
				new JLabel("Licence: "), licenseNoTextField,
				new JLabel("Experimenter: "), experimenterTextField));		
		
		resetButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				int res = JOptionPane.showConfirmDialog(UIUtils.getMainFrame(), "Are you sure you want to reset this randomization?", "Reset", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(res!=JOptionPane.YES_OPTION) return;
				dlg.getPhase().resetRandomization();
			}
		});
				
		add(BorderLayout.CENTER, UIUtils.createVerticalBox(
				groupsPanel,
				generalPanel,
				Box.createVerticalGlue()));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), getNextButton()));
		
		updateView();
	}
	
	private void refreshGroupPanel() {
		StringBuilder sb = new StringBuilder();
		Set<Group> seen = new HashSet<Group>();
		String user = SpiritFrame.getUsername();
		for (Group group : dlg.getGroups()) {
			
			if(!dlg.getPhase().equals(group.getFromPhase())) continue;
			
			if(group.getFromGroup()!=null) {
				if(!seen.contains(group.getFromGroup())) {
					int n = group.getFromGroup().getNAnimals(dlg.getPhase());
					sb.append("<li><b style='background:" + UIUtils.getHtmlColor(group.getFromGroup().getBlindedColor(user)) + "'>" + group.getBlindedName(user) + "</b>"
							+ (n>0? " <i>(n=" + n + ")</i>":"")
							+ "<br>");
					seen.add(group.getFromGroup());
				}
				if(!seen.contains(group)) {
					int n = group.getNAnimals(dlg.getPhase());
					sb.append("<li><b style='background:" + UIUtils.getHtmlColor(group.getBlindedColor(user)) + "'>" + group.getBlindedName(user) + "</b>"
							+ (n>0? " <i>(n=" + n + ")</i>":"")
							+ " from "+group.getFromGroup() + "<br>");
					seen.add(group);
				}
			} else {
				if(!seen.contains(group)) {
					int n = group.getNAnimals(dlg.getPhase());					
					sb.append("<li><b style='background:" + UIUtils.getHtmlColor(group.getBlindedColor(user)) + "'>" + group.getBlindedName(user) + "</b>"
							+ (n>0? " <i>(n=" + n + ")</i>":"")
							+ "<br>");
					seen.add(group);
				}
			}
									
		}
		
		groupsLabel.setText("<html>"+sb+"</html>");
		
	}
	
	@Override
	public void updateModel(boolean allowDialogs) throws Exception {
		if(dlg.getStudy()!=null) {
			dlg.getStudy().getMetadata().put("LICENSENO", licenseNoTextField.getText());
			dlg.getStudy().getMetadata().put("EXPERIMENTER", experimenterTextField.getText());
		}
		dlg.setBiotype(biotypeComboBox.getSelection());
	}
	
	@Override
	public void updateView() {
		resetButton.setEnabled(dlg.getPhase().getRandomization().getSamples().size()>0);
		
		if(dlg.getStudy()!=null) {
			String experimenter = dlg.getStudy().getMetadata().get("EXPERIMENTER");
			if(experimenter==null || experimenter.length()==0) experimenter = SpiritFrame.getUsername();
			experimenterTextField.setText(experimenter);
			licenseNoTextField.setText(dlg.getStudy().getMetadata().get("LICENSENO"));
		}
		
		
		biotypeComboBox.setSelection(dlg.getBiotype());
		biotypeComboBox.setEnabled(dlg.canChooseBiotype());
		
	}
	
	
}
