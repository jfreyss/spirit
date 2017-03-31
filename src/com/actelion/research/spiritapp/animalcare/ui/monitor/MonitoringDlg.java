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

package com.actelion.research.spiritapp.animalcare.ui.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.text.JTextComponent;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.ContainerComboBox;
import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.study.NamedTreatmentComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.component.BalanceDecorator;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.FoodWater;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOFoodWater;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class MonitoringDlg extends JEscapeDialog {

	public static final Container CAGE_NONE = new Container("NoCage");
	public static final NamedTreatment TREATMENT_NONE = new NamedTreatment("NoTreatment");

	private Map<NamedTreatment, String> treatment2Formulation = new LinkedHashMap<>();

	private Study study;
	private final Phase phase;
	private JSplitPane splitPane;
	private List<Result> allPreviousResults;

	private JLabel formulationLabel = new JLabel();
	private List<MonitoringAnimalPanel> animalPanels = new ArrayList<>();

	private BalanceDecorator balanceDecorator = new BalanceDecorator(this);

	private ContainerComboBox cageComboBox;
	private NamedTreatmentComboBox treatmentComboBox;
	private JCheckBox onlyRequiredCheckBox = new JCheckBox("Only show required", true);
	private JComboBox<String> sortComboBox1 = new JComboBox<>(new String[] { "by Cage", "by Group"});
	private JComboBox<String> sortComboBox2 = new JComboBox<>(new String[] { "by SampleName", "by SampleId"});

	private JPanel cagePanel = new JPanel(new GridBagLayout());
	private JPanel animalPanel = new JPanel(new GridBagLayout());
	private List<JTextComponent> requiredComponents = new ArrayList<>();
	private final String elb;

	public MonitoringDlg(Phase p) {
		super(UIUtils.getMainFrame(), "Live Monitoring");


		this.phase = p;
		this.study = JPAUtil.reattach(phase.getStudy());
		List<Biosample> animals = study.getTopAttachedBiosamples();
		this.elb = DAOResult.suggestElb(SpiritFrame.getUsername());

		// Reload results
		try {
			DAOResult.attachOrCreateStudyResultsToTops(study, animals, phase, elb);
		} catch (Exception e) {
			JExceptionDialog.showError(e);
			return;
		}

		// Filter Components
		ActionListener queryListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateFilters();
				updateView();
			}
		};
		cageComboBox = new ContainerComboBox();
		treatmentComboBox = new NamedTreatmentComboBox(getNamedTreatments(), "");
		treatmentComboBox.setEnabled(!SpiritRights.isBlindAll(study, SpiritFrame.getUser()));
		cageComboBox.addActionListener(queryListener);
		treatmentComboBox.addActionListener(queryListener);
		onlyRequiredCheckBox.addActionListener(queryListener);
		sortComboBox1.addActionListener(queryListener);
		sortComboBox1.setEnabled(!SpiritRights.isBlindAll(study, SpiritFrame.getUser()));
		sortComboBox2.addActionListener(queryListener);

		// Filter Panel
		formulationLabel.setPreferredSize(new Dimension(200, 50));
		JPanel filterPanel = UIUtils.createTitleBox("Filters for " + phase.getStudy().getStudyId() + " / " + phase.getShortName(),
				UIUtils.createHorizontalBox(
						UIUtils.createTable(
								new JLabel("Cage: "), UIUtils.createHorizontalBox(cageComboBox, Box.createHorizontalStrut(15), onlyRequiredCheckBox, Box.createHorizontalGlue()),
								new JLabel("Treatment: "), UIUtils.createHorizontalBox(treatmentComboBox, Box.createHorizontalStrut(15), new JLabel("Sort: "), sortComboBox1, new JLabel("and"), sortComboBox2)),
						Box.createHorizontalGlue(),
						new JScrollPane(formulationLabel)
						));

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(e-> {
			dispose();

		});

		// ContentPanel
		cagePanel.setBackground(Color.WHITE);
		cagePanel.setOpaque(true);
		JScrollPane sp1 = new JScrollPane(cagePanel);
		sp1.getVerticalScrollBar().setAutoscrolls(true);
		sp1.getVerticalScrollBar().setUnitIncrement(16);
		JPanel cageSp = new JPanel(new BorderLayout());
		cageSp.add(BorderLayout.CENTER, sp1);
		cageSp.setMinimumSize(new Dimension(0,0));

		animalPanel.setBackground(Color.WHITE);
		animalPanel.setOpaque(true);
		JScrollPane sp2 = new JScrollPane(animalPanel);
		sp2.getVerticalScrollBar().setUnitIncrement(16);
		JPanel animalSp = new JPanel(new BorderLayout());
		animalSp.add(BorderLayout.CENTER, sp2);
		animalSp.setMinimumSize(new Dimension(0,0));

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, cageSp, animalSp);
		splitPane.setDividerLocation(250);
		splitPane.setOneTouchExpandable(true);

		// Update view/filters
		updateFilters();
		updateView();

		setContentPane(UIUtils.createBox(
				splitPane,
				filterPanel,
				UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), balanceDecorator.getBalanceCheckBox(), Box.createHorizontalGlue(), closeButton)));
		UIUtils.adaptSize(this, 1100, 1200);
		setVisible(true);
	}

	public String getElb() {
		return elb;
	}

	private Set<NamedTreatment> getNamedTreatments() {
		Set<NamedTreatment> res = new LinkedHashSet<>();
		res.add(TREATMENT_NONE);
		for (StudyAction a : study.getStudyActions(phase)) {
			if (a.getNamedTreatment() != null)
				res.add(a.getNamedTreatment());
		}
		return res;
	}

	private void updateFilters() {
		NamedTreatment filterTreatment = treatmentComboBox.getSelection();

		Set<Container> cages = new TreeSet<>();
		for (Biosample animal : study.getTopAttachedBiosamples()) {
			StudyAction a = animal.getStudyAction(phase);
			if (filterTreatment != null) {
				if (filterTreatment.getId() <= 0 && a != null && a.getNamedTreatment() != null)
					continue;
				if (filterTreatment.getId() > 0 && (a == null || !filterTreatment.equals(a.getNamedTreatment())))
					continue;
			}

			if (animal.getContainer() == null)
				cages.add(CAGE_NONE);
			else
				cages.add(animal.getContainer());
		}

		cageComboBox.setValues(cages, true);
	}

	private void updateView() {
		// Read Filters
		NamedTreatment filterTreatment = treatmentComboBox.getSelection();
		Container filterCage = cageComboBox.getSelection();
		boolean onlyRequired = onlyRequiredCheckBox.isSelected();

		// Apply filters
		List<Biosample> filteredAnimals = new ArrayList<>();
		List<Container> filteredContainers = new ArrayList<>();
		for (Biosample animal : study.getTopAttachedBiosamples()) {
			StudyAction a = animal.getStudyAction(phase);
			if (filterTreatment != null) {
				if (filterTreatment.getId() <= 0 && a != null && a.getNamedTreatment() != null)
					continue;
				if (filterTreatment.getId() > 0 && (a == null || !filterTreatment.equals(a.getNamedTreatment())))
					continue;
			}
			if (filterCage != null && !filterCage.equals(animal.getContainer())) continue;


			// Is the animal already dead
			if (animal.isDeadAt(phase)) continue;

			// The animal has some measurements
			// Add the animal to our list
			if(!onlyRequired || (a!=null && (a.isMeasureWeight() || (a.getNamedTreatment()!=null && a.getNamedTreatment().isWeightDependant()) || a.getMeasurements().size()>0))) {
				filteredAnimals.add(animal);
			}

			// Add the cage if there is required FoodWater
			if (!onlyRequired || (a != null && (a.isMeasureFood() || a.isMeasureWater()))) {
				if (animal.getContainerId()!=null && animal.getContainer() != null && !filteredContainers.contains(animal.getContainer())) {
					filteredContainers.add(animal.getContainer());
				}
			}
		}

		// Sort cages by group/cageId
		Collections.sort(filteredContainers, new Comparator<Container>() {
			@Override
			public int compare(Container o1, Container o2) {
				int c = 0;
				if (sortComboBox1.getSelectedIndex() == 0) {
					c = CompareUtils.compare(o1.getContainerId(), o2.getContainerId());
					if (c != 0) return c;
					c = CompareUtils.compare(o1.getFirstBiosample()==null?"": o1.getFirstBiosample().getSampleIdName(), o2.getFirstBiosample()==null?"": o2.getFirstBiosample().getSampleIdName());
				} else {
					c = CompareUtils.compare(o1.getFirstGroup(), o2.getFirstGroup());
					if (c != 0) return c;
					c = CompareUtils.compare(o1.getContainerId(), o2.getContainerId());
				}
				return c;
			}
		});

		// Sort animals by group/cageId/animalNo/animalId
		Collections.sort(filteredAnimals, new Comparator<Biosample>() {
			@Override
			public int compare(Biosample o1, Biosample o2) {
				int c;
				if (sortComboBox1.getSelectedIndex() == 1) {
					c = CompareUtils.compare(o1.getInheritedGroup(), o2.getInheritedGroup());
					if (c != 0) return c;
				}

				c = CompareUtils.compare(o1.getContainerId(), o2.getContainerId());
				if (c != 0) return c;

				if (sortComboBox2.getSelectedIndex() == 0) {
					c = CompareUtils.compare(o1.getSampleName(), o2.getSampleName());
					if (c != 0) return c;
				}
				return CompareUtils.compare(o1.getSampleId(), o2.getSampleId());
			}
		});

		// Get FoodWater
		List<FoodWater> fws = DAOFoodWater.getFoodWater(study, null);
		List<FoodWater> currentfws = getOrCreateFoodWaterFor(phase);

		// Get previous results
		try {
			Set<Test> tests = new HashSet<>();
			tests.add(DAOTest.getTest(DAOTest.WEIGHING_TESTNAME));
			tests.add(DAOTest.getTest(DAOTest.OBSERVATION_TESTNAME));
			tests.addAll(Measurement.getTests(StudyAction.getMeasurements(study.getStudyActions())));


			allPreviousResults = new ArrayList<Result>();
			ResultQuery q = new ResultQuery();
			q.setTestIds(new HashSet<>(JPAUtil.getIds(tests)));
			q.setSid(study.getId());
			allPreviousResults.addAll(DAOResult.queryResults(q, null));
		} catch (Exception ex) {
			ex.printStackTrace(); // should not happen
		}


		// CagesInfo
		GridBagConstraints c = new GridBagConstraints();
		c.weighty = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;

		requiredComponents.clear();
		cagePanel.removeAll();
		for (Container cage : filteredContainers) {
			if(cage.getContainerId()==null) continue;
			FoodWater currentFw = FoodWater.extract(currentfws, cage.getContainerId(), phase);

			if (currentFw != null) {
				MonitoringCagePanel oneCagePanel = new MonitoringCagePanel(this, c.gridy, cage, phase, currentFw, fws, onlyRequired);
				requiredComponents.addAll(oneCagePanel.getRequiredComponents());
				c.weightx = 1;
				c.gridx = 1;
				cagePanel.add(oneCagePanel, c);
			}
			c.gridy++;
		}
		cagePanel.setMinimumSize(new Dimension(0,0));
		cagePanel.setMaximumSize(new Dimension(cagePanel.getPreferredSize().width,cagePanel.getPreferredSize().height));

		c.gridy = GridBagConstraints.REMAINDER;
		c.weighty = 1;
		cagePanel.add(Box.createVerticalGlue(), c);
		cagePanel.validate();

		// AnimalsInfo
		animalPanel.setLayout(new BorderLayout());
		animalPanel.removeAll();
		List<JPanel> cagePanels = new ArrayList<>();
		int i = 0;
		while(i < filteredAnimals.size()) {
			String cageId = filteredAnimals.get(i).getContainerId();
			List<JPanel> panels = new ArrayList<>();
			for(; i < filteredAnimals.size() && ((cageId==null &&  filteredAnimals.get(i).getContainerId()==null) || (cageId!=null && cageId.equals(filteredAnimals.get(i).getContainerId()))); i++) {
				MonitoringAnimalPanel oneAnimalPanel = new MonitoringAnimalPanel(this, i, filteredAnimals.get(i), phase, onlyRequired);
				requiredComponents.addAll(oneAnimalPanel.getRequiredComponents());
				animalPanels.add(oneAnimalPanel);
				panels.add(oneAnimalPanel);
			}
			JPanel cagePanel = UIUtils.createTitleBoxBigger(cageId==null?"": cageId, UIUtils.createVerticalBox(panels));
			cagePanel.setOpaque(true);
			cagePanel.setBackground(Color.WHITE);
			cagePanels.add(cagePanel);
		}

		animalPanel.add(BorderLayout.NORTH, UIUtils.createVerticalBox(cagePanels));
		animalPanel.validate();

		if (splitPane != null) {
			splitPane.setDividerLocation(Math.min(350, filteredContainers.size() * 110));
		}


		//Init required components
		JComponent previous = null;
		for(final JComponent comp: requiredComponents) {
			if(comp.isEnabled()) {
				if(previous!=null) {
					((JCustomTextField)previous).addActionListener(e-> {
						comp.requestFocusInWindow();
					});
				}
				previous = comp;
			}
		}

	}

	public String getFormulation(NamedTreatment nt) {
		return treatment2Formulation.get(nt);
	}

	public void setFormulation(NamedTreatment nt, String formulation) {
		if (nt == null)
			return;

		treatment2Formulation.put(nt, formulation);

		StringBuilder sb = new StringBuilder();
		sb.append("<html><span style='font-size:80%'>");
		for (NamedTreatment key : treatment2Formulation.keySet()) {
			if (treatment2Formulation.get(key) != null) {
				sb.append(key + " -> " + treatment2Formulation.get(key) + "<br>");
			}
		}
		formulationLabel.setText(sb.toString());

	}

	public List<Result> getAllPreviousResults() {
		return allPreviousResults;
	}

	public void updateFW(Container cage) {
		for (MonitoringAnimalPanel panel : animalPanels) {
			if (cage.equals(panel.getAnimal().getContainer())) {
				panel.updateFW();
			}
		}
	}

	public static List<FoodWater> getOrCreateFoodWaterFor(Phase phase) {
		List<FoodWater> fwsFromGivenPhase = DAOFoodWater.getFoodWater(phase.getStudy(), phase);
		List<FoodWater> res = new ArrayList<FoodWater>();
		for (Container cage: Biosample.getContainers(phase.getStudy().getTopAttachedBiosamples())) {

			//Create or retrieve a row for this cage/phase
			FoodWater sel = null;
			for (FoodWater fw : fwsFromGivenPhase) {
				if(fw.getContainerId().equals(cage.getContainerId()) && fw.getPhase().equals(phase)) {
					sel = fw;
					break;
				}
			}

			//If not existing, create it
			if(sel==null) {
				sel = new FoodWater();
				sel.setPhase(phase);
				sel.setContainerId(cage.getContainerId());
			}
			//Skip this cage if all animals are already dead
			if(cage.getBiosamples().size()<=0) continue;


			//Add the FoodWater
			res.add(sel);
		}
		return res;

	}

}
