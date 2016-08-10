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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.util.component.AutosaveDecorator;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Randomization;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class RandomizationDlg extends JSpiritEscapeDialog {

	private Biotype biotype;
	private boolean canChooseBiotype;
	private final Study study;
	private final Phase phase;
	private final List<Group> groups;
	
	private JTabbedPane wizardPane = new JCustomTabbedPane(JTabbedPane.LEFT);
	private ConfigTab configTab;
	private DataTab weighingTab;
	private GroupTab groupTab;
	private CageTab cageTab;
	private SummaryTab summaryTab;
	
	/**
	 * Create a new RandomizationDlg for the given phase
	 * @param p - can be null if the randomization does not need to be saved
	 */
	public RandomizationDlg(Phase p) {
		super(UIUtils.getMainFrame(), "Group Assignment - "+(p==null?"": p.getStudy().getStudyId() + " / " + p.getShortName()), RandomizationDlg.class.getName());
		if(p==null) 		{
			throw new IllegalArgumentException("you need to select a phase");
		}
		
		this.study = DAOStudy.getStudy(p.getStudy().getId());
		this.groups = new ArrayList<>(study.getGroups());
		this.phase = study.getPhase(p.getId());
		
		//Load the samples
		DAOStudy.loadBiosamplesFromStudyRandomization(phase.getRandomization());
		
		
		//find the biotype of the (possible) attached sample, and check if this biotype can be changed
		if(getStudy()!=null && phase.getRandomization().getSamples().size()>0) {
			canChooseBiotype = true;
			for(Biosample b: AttachedBiosample.getBiosamples(phase.getRandomization().getSamples())) {
				if(b.getBiotype()==null) continue;
				if(b.getMetadataAsString().length()>0) canChooseBiotype = false;
				if(biotype==null) {
					biotype = b.getBiotype();
				} else if(!biotype.equals(b.getBiotype())) {
					biotype = null;
					canChooseBiotype = false;
					break;
				}
			}
		} else {
			biotype = DAOBiotype.getBiotype(Biotype.ANIMAL);
			canChooseBiotype = true;
		}
		
		
		//Buttons
		JButton saveButton = new JIconButton(IconType.SAVE, "Save (without doing the assignment)");
		saveButton.setToolTipText("Save without finalization. Specimen are not assigned to the study");
		saveButton.setEnabled(study!=null);		
		saveButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					saveForLater();
					dispose();
				} catch (Exception ex) {
					JExceptionDialog.showError(RandomizationDlg.this, ex);
				}
				
			}
		});
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.CENTER, wizardPane);
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalStrut(50), autosaveDecorator.getAutosaveCheckBox(), saveButton, Box.createHorizontalGlue()));
		
		

		configTab = new ConfigTab(this);
		weighingTab = new DataTab(this);
		groupTab = new GroupTab(this);
		cageTab = new CageTab(this);
		summaryTab = new SummaryTab(this);
		
		
		wizardPane.setFont(FastFont.BOLD);
		wizardPane.add("<html><br>Parameters<br><br></html>", configTab);
		wizardPane.add("<html><br>Randomization<br>Data<br><br></html>", weighingTab);
		wizardPane.add("<html><br>Group<br>Assignment<br><br></html>", groupTab);
		wizardPane.add("<html><br>Cage<br>Assignment<br><br></html>", cageTab);
		wizardPane.add("<html><br>Saving<br><br></html>", summaryTab);
		
		WizardPanel.configureEvents(wizardPane, new WizardPanel[] {configTab, weighingTab, groupTab, cageTab, summaryTab});
		
		
		setContentPane(contentPanel);
		
   		UIUtils.adaptSize(this, 1200, 1000);
		setVisible(true);
	}
	
	
	
	protected void saveForLater() throws Exception {
		WizardPanel.updateModel(wizardPane, false);
		
		DAOStudy.persistStudies(Collections.singleton(study), Spirit.getUser());
	}
	
	
	final AutosaveDecorator autosaveDecorator = new AutosaveDecorator(this) {			
		@Override
		public void autosave() throws Exception {
			if(mustAskForExit()) {
				saveForLater();
			}
			setMustAskForExit(false);
		}
	};
	
	
	public Phase getPhase() {
		return phase;
	}
	public Study getStudy() {
		return study;
	}
	public List<Group> getGroups(){
		return groups;
	}
	
	public Randomization getRandomization() {
		return phase.getRandomization();
	}
		
	public Biotype getBiotype() {
		return biotype;
	}
	
	public void setBiotype(Biotype biotype) {
		this.biotype =  biotype;
	}
	
	
	public static Map<Integer, List<AttachedBiosample>> splitByGroup(List<AttachedBiosample> list) {
		Map<Integer, List<AttachedBiosample>> id2list = new HashMap<Integer, List<AttachedBiosample>>();
		for (AttachedBiosample rndSample : list) {
			Integer key = rndSample.getGroup()==null? -1: (int) rndSample.getGroup().getId();
			List<AttachedBiosample> l = id2list.get(key);
			if(l==null) {
				l = new ArrayList<AttachedBiosample>();
				id2list.put(key, l);
			}
			l.add(rndSample);
		}
		
		
		return id2list;
	}
	
	public static void main(String[] args) throws Exception {
		Spirit.initUI();
		Spirit.setUser(DAOSpiritUser.loadUser("freyssj"));
		Study s = DAOStudy.getStudyByStudyId("S-00479");
		new RandomizationDlg(s.getPhase("d-1"));
	}
	public boolean canChooseBiotype() {
		return canChooseBiotype;
	}
	
}
