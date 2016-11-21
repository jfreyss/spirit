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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.study.AttachedBiosample;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class CageTab extends WizardPanel {

	private final RandomizationDlg dlg;
	private final JCheckBox reuseCagesCheckbox = new JCheckBox("Reuse the previous CageIds", true);
	private final List<GroupPanel> groupPanels = new ArrayList<>();
	private final List<Group> groups = new ArrayList<>();
	private final JPanel centerPanel = new JPanel();
	private final List<JToggleButton> nButtons = new ArrayList<>();
	public static final int PANEL_WIDTH = 350;


	public CageTab(final RandomizationDlg dlg) {
		super(new BorderLayout());
		this.dlg = dlg;
		
		
		//North Panel
		JPanel selectCagesPanel = new JPanel(new GridLayout(1, 9, 2,0));
		selectCagesPanel.setOpaque(false);
		ButtonGroup buttonGroup = new ButtonGroup();
		for (int i = 1; i <= 9; i++) {
			JToggleButton b = new JToggleButton("  " + i + "  ");
			selectCagesPanel.add(b);
			buttonGroup.add(b);
			nButtons.add(b);
			final int maxAnimals = i;
			b.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						reassignCages(maxAnimals, reuseCagesCheckbox.isEnabled() && reuseCagesCheckbox.isSelected());
						refreshTables();
					} catch (Exception ex) {
						ex.printStackTrace();
						JExceptionDialog.showError(CageTab.this, "Could not assign "+maxAnimals+" per cage");
					}

				}
			});
		}
		

		JPanel northPanel = UIUtils.createTitleBox("", UIUtils.createVerticalBox(
				UIUtils.createHorizontalBox(reuseCagesCheckbox, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(new JLabel("Maximal number per cage: "), selectCagesPanel, Box.createHorizontalGlue())));
		
		

		//CenterPanel
		add(BorderLayout.NORTH, northPanel);
		add(BorderLayout.CENTER, centerPanel);
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(/*dlg.createSaveButton(),*/ Box.createHorizontalGlue(), getNextButton()));
	}
	
	private void reassignCages(int maxAnimalsPerCage, boolean reuseCages) throws Exception {
		
		if(maxAnimalsPerCage<1) return;

		dlg.setMustAskForExit(true);
		
		List<Integer> groupIds = new ArrayList<Integer>();
		for (Group g : groups) groupIds.add((int)g.getId());
		groupIds.add(-1);
		
		List<AttachedBiosample> samples = dlg.getRandomization().getSamples();
		Map<Integer, List<AttachedBiosample>> splits = RandomizationDlg.splitByGroup(samples);
		
		//Extract Biosamples
		List<Biosample> toBeModified = new ArrayList<Biosample>();
		for (AttachedBiosample s : samples) if(s.getBiosample()!=null) toBeModified.add(s.getBiosample());
		
		//Get former cageNames
		Set<String> reusableCageNames = new HashSet<String>();
		Set<String> nonreusableCageNames = new HashSet<String>();
		for(Biosample b: dlg.getStudy().getTopAttachedBiosamples()) {
			if(b.getContainerId()==null || b.getContainerId()=="") continue;
			if(toBeModified.contains(b)) {
				if(reuseCages) {
					reusableCageNames.add(b.getContainerId());
				} else {
					nonreusableCageNames.add(b.getContainerId());
				}
			} else {
				nonreusableCageNames.add(b.getContainerId());
			}
		}
		
		
		List<String> cageNamesPool = new ArrayList<String>(); 
		cageNamesPool.addAll(reusableCageNames);		
		cageNamesPool.removeAll(nonreusableCageNames);

		//Create new cageNames
		{
			int cageNo = 1;
			while(cageNamesPool.size()<samples.size()*2+10) {
				String cageName = Container.suggestNameForCage(dlg.getStudy(), cageNo++);
				if(nonreusableCageNames.contains(cageName)) continue;
				if(!cageNamesPool.contains(cageName)) cageNamesPool.add(cageName);
			}
		}
		//Filter out cages used in other studies
		BiosampleQuery q = new BiosampleQuery();
		q.setContainerIds(MiscUtils.flatten(cageNamesPool));
		List<Biosample> occupied = DAOBiosample.queryBiosamples(q, null);
		for (Biosample b : occupied) {
			if(b.getInheritedStudy()==null || !b.getInheritedStudy().equals(dlg.getStudy())) {
				cageNamesPool.remove(b.getContainerId());
			}
		}
		
		
		
		//Sort by cageNo
		Collections.sort(cageNamesPool, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return CompareUtils.compare(o1, o2);
			}
		});


		int cageNo = 0;
		
		for(Integer groupId: groupIds) {
			List<AttachedBiosample> list = splits.get(groupId);
			if(list==null) continue;
			Collections.sort(list);
			int nCages = (list.size() + maxAnimalsPerCage - 1) / maxAnimalsPerCage;
			int index = 0;
			for (int i = 0; i < nCages; i++) {
				int nInCage = (list.size()-index + nCages-i - 1) / (nCages-i);
				//Assign the animals of this group
				for (int j = 0; j < nInCage && index<list.size(); j++) {
					String cageName = cageNamesPool.get(cageNo%cageNamesPool.size());					
					list.get(index).setContainerId(cageName);
					index++;
				}
				cageNo++;
			}
		}
		
	}
	
	
	@Override
	public void updateModel(boolean allowDialogs) throws Exception {
		//Nothing		
	}
		
	@Override
	public void updateView() {
		//Create groupPanels
		setVisible(dlg.getStudy()!=null && (dlg.getBiotype()==null || !dlg.getBiotype().isHideContainer()));
		centerPanel.removeAll();
		groupPanels.clear();
		groups.clear();
		
		boolean canReuseCages = false;
		for (Group gr : dlg.getGroups()) {
			if(!dlg.getPhase().equals(gr.getFromPhase())) continue;
			if(gr.getFromGroup()!=null) {
				canReuseCages = true; 
				if(!groups.contains(gr.getFromGroup())) groups.add(gr.getFromGroup());
			}
			groups.add(gr);
		}

		reuseCagesCheckbox.setEnabled(canReuseCages);
		if(!reuseCagesCheckbox.isEnabled()) reuseCagesCheckbox.setSelected(false);
		
		JPanel nonReservePanel = new JPanel();
		for (Group gr : groups) {
			GroupPanel grPanel = new GroupPanel(dlg, gr, true, true);
			groupPanels.add(grPanel);
			nonReservePanel.add(grPanel);
			
		}
		
		int cols = Math.max(1, (centerPanel.getWidth()-PANEL_WIDTH)/PANEL_WIDTH);	
		while(nonReservePanel.getComponentCount()<cols) nonReservePanel.add(new JPanel());
		nonReservePanel.setLayout(new GridLayout(0, cols));

		
		//Reserve
		GroupPanel grPanel = new GroupPanel(dlg, null, true, false);			
		JPanel reservePanel = grPanel;
		groupPanels.add(grPanel);

		centerPanel.removeAll();
		centerPanel.setLayout(new BorderLayout());
		JScrollPane sp = new JScrollPane(nonReservePanel);
		sp.setAutoscrolls(true);
		nonReservePanel.setAutoscrolls(true);
		centerPanel.add(BorderLayout.CENTER, sp);
		centerPanel.add(BorderLayout.EAST, reservePanel);
		
		
		
		refreshTables();
		
		validate();
		
		
	
	}
	
	public void refreshTables() {
		for (GroupPanel groupPanel : groupPanels) {
			Group g = groupPanel.getGroup();
			List<AttachedBiosample> rows = new ArrayList<AttachedBiosample>();
			for (AttachedBiosample s : dlg.getRandomization().getSamples()) {
				if(CompareUtils.compare(g, s.getGroup())==0) {
					rows.add(s);
				}
			}
			Collections.sort(rows);
			groupPanel.setRows(dlg.getRandomization(), rows);			
		}
	}

}
