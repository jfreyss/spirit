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

package com.actelion.research.spiritapp.slidecare.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleComboBox;
import com.actelion.research.spiritapp.spirit.ui.lf.BiotypeComboBox;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.slide.SampleDescriptor;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.UIUtils;

public class OrganTab extends JPanel {

	private final ContainerCreatorDlg dlg;
	private final BiosampleComboBox animalCombobox = new BiosampleComboBox();
	private final JPanel poolPanel = new JPanel();
	private int animalNo;
	private BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes()); 
	private JCheckBox onlyInCassetteCheckBox = new JCheckBox("Must be in a cassette");
	
	public OrganTab(ContainerCreatorDlg dlg, int animalNo, boolean filterOnlyInCassette) {
		super(new BorderLayout());
		this.dlg = dlg;
		this.animalNo = animalNo;
		
		biotypeComboBox.setSelectionString("Organ");
		onlyInCassetteCheckBox.setSelected(filterOnlyInCassette);
		
		JPanel filterPanel = UIUtils.createHorizontalBox(
				new JLabel(" Check samples of: "), 
				animalCombobox, 
				new JLabel("   Biotype:"),
				biotypeComboBox, 
				onlyInCassetteCheckBox,
				Box.createHorizontalGlue());
		
		poolPanel.setPreferredSize(new Dimension(600, 200));
		poolPanel.setBackground(Color.WHITE);
		poolPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		add(BorderLayout.NORTH, filterPanel);
		add(BorderLayout.CENTER, poolPanel);
		animalCombobox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshItems();
			}
		});
		biotypeComboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshItems();
			}
		});
		onlyInCassetteCheckBox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				refreshItems();		
			}
		});
		
	}
	
	public void refresh() {
		List<Biosample> animals = dlg.getStudy().getTopAttachedBiosamples();
		animalCombobox.setValues(animals, "Select One...");
		if(animals.size()>0) {
			animalCombobox.setSelection(animals.iterator().next());
		}
		refreshItems();
	}
	
	public void refreshItems() {
		Biosample animal = animalCombobox.getSelection();
		

		TreeSet<Biotype> biotypes = new TreeSet<Biotype>();
				
		List<SampleDescriptor> containerSamples = new ArrayList<SampleDescriptor>();
		if(animal!=null) {
			containerSamples = getPossibleContainerSamples(animalNo, animal);
			Collections.sort(containerSamples);
			for (SampleDescriptor s : containerSamples) {
				biotypes.add(s.getBiotype());
			}
		}
		biotypeComboBox.setValues(biotypes);
		
		

		//create a map: classifier->list of ContainerSample
		Map<String, List<SampleDescriptor>> map = new TreeMap<>();
		for (SampleDescriptor s : containerSamples) {
			if(biotypeComboBox.getSelection()!=null && !biotypeComboBox.getSelection().equals(s.getBiotype())) continue;
			if(onlyInCassetteCheckBox.isSelected() && s.getContainerType()!=ContainerType.K7) continue;
			String key = (s.getBlocNo()==null? 0: s.getBlocNo()) + "_" + s.getContainerType() + "_"+s.getBiotype();
			List<SampleDescriptor> l = map.get(key);
			if(l==null) {
				map.put(key, l = new ArrayList<>());
			}
			l.add(s);
		}
		
		//Position items
		poolPanel.removeAll();
		poolPanel.setLayout(null);
		int maxY = 0;
		int x = 4;
		for (List<SampleDescriptor> list: map.values()) {
			int y = 4;
			for (SampleDescriptor slideSample : list) {
				OrganSamplePanel s = new OrganSamplePanel(slideSample, null);
				s.setBounds(new Rectangle(x, y, OrganSamplePanel.WIDTH, OrganSamplePanel.HEIGHT));
				poolPanel.add(s);
				y+=OrganSamplePanel.HEIGHT + 1;
				maxY = Math.max(maxY, y);
			}			
			x+=OrganSamplePanel.WIDTH+10;
		}
		poolPanel.setPreferredSize(new Dimension(x, maxY));
		
		validate();
		repaint();
		
		
	}
	private static List<SampleDescriptor> getPossibleContainerSamples(int animalNo, Biosample b) {
		List<SampleDescriptor> res = new ArrayList<SampleDescriptor>();
		getPossibleContainerSamplesRec(animalNo, b, res);
		return res;
	}
	private static void getPossibleContainerSamplesRec(int animalNo, Biosample b, List<SampleDescriptor> res) {
		//Create a compatible SlideSample
		SampleDescriptor s = new SampleDescriptor(animalNo, b);
		
		//Find if is already present 
		SampleDescriptor found = null;
		for (SampleDescriptor sampling: res) {
			if(sampling.equals(s)) {
				found = sampling;
			}
		}
		
		if(found==null && b.getTopParent()!=b && b.getContainerType()!=ContainerType.SLIDE) {
			res.add(s);													
		}
		
		//Recursion
		for (Biosample child : b.getChildren()) {
			getPossibleContainerSamplesRec(animalNo, child, res);
		}
	}
	
	
}
