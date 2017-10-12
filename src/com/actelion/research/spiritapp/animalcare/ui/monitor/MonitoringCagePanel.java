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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerLabel;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerLabel.ContainerDisplayMode;
import com.actelion.research.spiritapp.spirit.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.FoodWater;
import com.actelion.research.spiritcore.business.biosample.FoodWater.Consumption;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.services.dao.DAOFoodWater;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.TextChangeListener;
import com.actelion.research.util.ui.UIUtils;

public class MonitoringCagePanel extends JPanel {

	private JCustomLabel foodIntervalLabel = new JCustomLabel("", FastFont.SMALL, Color.BLACK);
	private JCustomLabel waterIntervalLabel = new JCustomLabel("", FastFont.SMALL, Color.BLACK);
	private JCustomLabel foodConsumptionLabel = new JCustomLabel("", FastFont.SMALL, Color.BLUE);
	private JCustomLabel waterConsumptionLabel = new JCustomLabel("", FastFont.SMALL, Color.BLUE);
	private JCustomLabel foodFormulaLabel = new JCustomLabel("", FastFont.SMALLER, Color.BLACK);
	private JCustomLabel waterFormulaLabel = new JCustomLabel("", FastFont.SMALLER, Color.BLACK);

	private JCustomLabel nextFoodIntervalLabel = new JCustomLabel("", FastFont.SMALL, Color.BLACK);
	private JCustomLabel nextWaterIntervalLabel = new JCustomLabel("", FastFont.SMALL, Color.BLACK);
	private JCustomLabel nextFoodConsumptionLabel = new JCustomLabel("", FastFont.SMALL, Color.BLUE);
	private JCustomLabel nextWaterConsumptionLabel = new JCustomLabel("", FastFont.SMALL, Color.BLUE);
	private JCustomLabel nextFoodFormulaLabel = new JCustomLabel("", FastFont.SMALLER, Color.BLACK);
	private JCustomLabel nextWaterFormulaLabel = new JCustomLabel("", FastFont.SMALLER, Color.BLACK);


	private final Phase phase;
	private final List<JTextComponent> requiredComponents = new ArrayList<>();


	public MonitoringCagePanel(final MonitoringDlg dlg, final int no, final Container container, final Phase phase, final FoodWater currentFw, final List<FoodWater> allFws, boolean onlyShowRequired) {
		super(new BorderLayout());

		assert container!=null;
		assert currentFw!=null;
		assert currentFw.getContainerId()!=null;
		assert currentFw.getContainerId().equals(container.getContainerId());
		assert currentFw.getPhase().equals(phase);

		int nAnimals;
		if(currentFw.getNAnimals()==null) {
			nAnimals = 0;
			for(Biosample b: container.getBiosamples()) {
				if(!b.isDeadAt(phase)) nAnimals++;
			}
		} else {
			nAnimals = currentFw.getNAnimals();
		}

		this.phase = phase;
		boolean foodRequired = isRequired(currentFw, container, false);
		boolean waterRequired = isRequired(currentFw, container, true);

		final FoodWater prevFood = currentFw.getPreviousFromList(allFws, false);
		final FoodWater prevWater = currentFw.getPreviousFromList(allFws, true);
		final Consumption prevFoodCons = currentFw.calculatePrevConsumptionFromList(allFws, false);
		final Consumption prevWaterCons = currentFw.calculatePrevConsumptionFromList(allFws, true);
		final Consumption nextFoodCons = currentFw.calculateNextConsumptionFromList(allFws, false);
		final Consumption nextWaterCons = currentFw.calculateNextConsumptionFromList(allFws, true);


		final JCustomTextField foodMeasureTextField  = new JCustomTextField(CustomFieldType.DOUBLE);
		foodMeasureTextField.addFocusListener(new AutoScrollFocusListener());
		foodMeasureTextField.setWarningWhenEdited(true);
		foodMeasureTextField.setTextDouble(currentFw.getFoodWeight());
		foodMeasureTextField.setToolTipText("<html>Last Value: "+ formatTooltipText(currentFw.getFoodWeight(), currentFw.getUpdUser(), currentFw.getUpdDate()));
		if(foodRequired && prevFood!=null) foodMeasureTextField.setBackground(LF.BGCOLOR_REQUIRED);
		if(foodRequired && prevFood!=null) requiredComponents.add(foodMeasureTextField);

		final JCustomTextField waterMeasureTextField  = new JCustomTextField(CustomFieldType.DOUBLE);
		waterMeasureTextField.addFocusListener(new AutoScrollFocusListener());
		waterMeasureTextField.setEnabled(prevWater!=null);
		waterMeasureTextField.setWarningWhenEdited(true);
		waterMeasureTextField.setTextDouble(currentFw.getWaterWeight());
		waterMeasureTextField.setToolTipText("<html>Last Value: "+ formatTooltipText(currentFw.getWaterWeight(), currentFw.getUpdUser(), currentFw.getUpdDate()));
		if(waterRequired && prevWater!=null) waterMeasureTextField.setBackground(LF.BGCOLOR_REQUIRED);
		if(waterRequired && prevWater!=null) requiredComponents.add(waterMeasureTextField);


		final JCustomTextField foodTareTextField  = new JCustomTextField(CustomFieldType.DOUBLE);
		foodTareTextField.addFocusListener(new AutoScrollFocusListener());
		foodTareTextField.setWarningWhenEdited(true);
		foodTareTextField.setTextDouble(currentFw.getFoodTare());
		foodTareTextField.setToolTipText("<html>Last Value: "+ formatTooltipText(currentFw.getFoodTare(), currentFw.getUpdUser(), currentFw.getUpdDate()));
		if(foodRequired) foodTareTextField.setBackground(LF.BGCOLOR_REQUIRED);
		if(foodRequired) requiredComponents.add(foodTareTextField);

		final JCustomTextField waterTareTextField  = new JCustomTextField(CustomFieldType.DOUBLE);
		waterTareTextField.addFocusListener(new AutoScrollFocusListener());
		waterTareTextField.setWarningWhenEdited(true);
		waterTareTextField.setTextDouble(currentFw.getWaterTare());
		waterTareTextField.setToolTipText("<html>Last Value: "+ formatTooltipText(currentFw.getWaterTare(), currentFw.getUpdUser(), currentFw.getUpdDate()));
		if(waterRequired) waterTareTextField.setBackground(LF.BGCOLOR_REQUIRED);
		if(waterRequired) requiredComponents.add(waterTareTextField);

		final JCustomTextField nAnimalsTextField = new JCustomTextField(CustomFieldType.INTEGER);
		nAnimalsTextField.addFocusListener(new AutoScrollFocusListener());
		nAnimalsTextField.setWarningWhenEdited(true);
		nAnimalsTextField.setTextInteger(nAnimals);
		nAnimalsTextField.setToolTipText("<html>Last Value: "+ formatTooltipText(currentFw.getWaterTare(), currentFw.getUpdUser(), currentFw.getUpdDate()));
		nAnimalsTextField.setTextWhenEmpty("Number of animals");

		TextChangeListener tl = new TextChangeListener() {

			@Override
			public void textChanged(JComponent src) {
				currentFw.setFoodWeight(foodMeasureTextField.getTextDouble());
				currentFw.setWaterWeight(waterMeasureTextField.getTextDouble());
				currentFw.setFoodTare(foodTareTextField.getTextDouble());
				currentFw.setWaterTare(waterTareTextField.getTextDouble());
				currentFw.setNAnimals(nAnimalsTextField.getTextInt());

				//if food is not empty:
				// - calculate the food consumption from the last entry to this one
				// - calculate the food consumption from this entry to the next one
				//if food is empty:
				// - delete this food consumption
				// - calculate the food consumption from the last entry to the next one

				Consumption newFoodCons = currentFw.calculatePrevConsumptionFromList(allFws, false);
				Consumption newWaterCons = currentFw.calculatePrevConsumptionFromList(allFws, true);
				Consumption newNextFoodCons = currentFw.calculateNextConsumptionFromList(allFws, false);
				Consumption newNextWaterCons = currentFw.calculateNextConsumptionFromList(allFws, true);

				Date now = JPAUtil.getCurrentDateFromDatabase();

				try {
					List<Result> results = new ArrayList<Result>();

					//Update results of current phase (based on previous tare)
					if(newFoodCons!=null) {
						DAOResult.attachOrCreateStudyResultsToTops(phase.getStudy(), container.getBiosamples(), newFoodCons.toPhase, dlg.getElb());
						for (Biosample animal : container.getBiosamples()) {
							Result r = animal.getAuxResult(DAOTest.getTest(DAOTest.FOODWATER_TESTNAME), newFoodCons.toPhase);
							r.getOutputResultValues().get(0).setValue(newFoodCons.value==null? null: ""+newFoodCons.value);
							r.setUpdDate(now);
							r.setUpdUser(SpiritFrame.getUser().getUsername());
							if(!results.contains(r)) results.add(r);
						}
					}
					if(newWaterCons!=null) {
						DAOResult.attachOrCreateStudyResultsToTops(phase.getStudy(), container.getBiosamples(), newWaterCons.toPhase, dlg.getElb());
						for (Biosample animal : container.getBiosamples()) {
							Result r = animal.getAuxResult(DAOTest.getTest(DAOTest.FOODWATER_TESTNAME), newWaterCons.toPhase);
							r.getOutputResultValues().get(1).setValue(newWaterCons.value==null? null: ""+newWaterCons.value);
							r.setUpdDate(now);
							r.setUpdUser(SpiritFrame.getUser().getUsername());
							if(!results.contains(r)) results.add(r);
						}
					}

					//Update results of next food phase (if needed, ie nextFood<>null)
					if(newNextFoodCons!=null) {
						DAOResult.attachOrCreateStudyResultsToTops(phase.getStudy(), container.getBiosamples(), newNextFoodCons.toPhase, dlg.getElb());
						for (Biosample animal : container.getBiosamples()) {
							Result r2 = animal.getAuxResult(DAOTest.getTest(DAOTest.FOODWATER_TESTNAME), newNextFoodCons.toPhase);
							String valFood = newNextFoodCons.value==null? null: ""+newNextFoodCons.value;
							r2.getOutputResultValues().get(0).setValue(valFood);
							r2.setUpdDate(now);
							r2.setUpdUser(SpiritFrame.getUser().getUsername());
							if(!results.contains(r2)) results.add(r2);
						}
					}

					//Update result of next water phase (if needed, ie nextWater<>null)
					if(newNextWaterCons!=null) {
						DAOResult.attachOrCreateStudyResultsToTops(phase.getStudy(), container.getBiosamples(), newNextWaterCons.toPhase, dlg.getElb());
						for (Biosample animal : container.getBiosamples()) {
							Result r2 = animal.getAuxResult(DAOTest.getTest(DAOTest.FOODWATER_TESTNAME), newNextWaterCons.toPhase);
							String valWater = newNextWaterCons.value==null? null: ""+newNextWaterCons.value;
							r2.getOutputResultValues().get(1).setValue(valWater);
							r2.setUpdDate(new Date());
							r2.setUpdUser(SpiritFrame.getUser().getUsername());
							if(!results.contains(r2)) results.add(r2);
						}
					}


					currentFw.setUpdUser(SpiritFrame.getUser().getUsername());
					currentFw.setUpdDate(now);

					updateLabels(newFoodCons, newWaterCons, newNextFoodCons, newNextWaterCons);

					JPAUtil.pushEditableContext(SpiritFrame.getUser());
					DAOResult.persistResults(results, SpiritFrame.getUser());
					DAOFoodWater.persistFoodWater(currentFw, SpiritFrame.getUser());
					if(src instanceof JCustomTextField) {
						((JCustomTextField)src).setBorderColor(Color.BLUE);
						src.setToolTipText((src.getToolTipText()==null?"<html>": src.getToolTipText()+"<br>") + "Updated: "+formatTooltipText(((JCustomTextField)src).getText(),  currentFw.getUpdUser(), currentFw.getUpdDate()));
					}
				} catch(Exception ex) {
					if(src instanceof JCustomTextField) ((JCustomTextField)src).setBorderColor(Color.RED);
					JExceptionDialog.showError(ex);
				} finally {
					JPAUtil.popEditableContext();
				}



				if(src==foodMeasureTextField || src==waterMeasureTextField) {
					//Update FW of animals
					dlg.updateFW(container);
				}

			}
		};
		foodMeasureTextField.setEnabled(prevFood!=null && prevFood.getFoodTare()!=null);
		waterMeasureTextField.setEnabled(prevWater!=null && prevWater.getWaterTare()!=null);
		foodMeasureTextField.addTextChangeListener(tl);
		waterMeasureTextField.addTextChangeListener(tl);
		foodTareTextField.addTextChangeListener(tl);
		waterTareTextField.addTextChangeListener(tl);
		nAnimalsTextField.addTextChangeListener(tl);

		JPanel centerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 0; c.weighty = 0;
		c.ipadx = 5; c.ipady = 1;
		c.fill = GridBagConstraints.HORIZONTAL;

		//Prepare headers
		c.weightx=1; c.gridx = 100; c.gridy = 0; centerPanel.add(Box.createHorizontalGlue(), c); c.weightx=0;

		c.gridx = 0; c.gridy = 0; centerPanel.add(UIUtils.createHorizontalBox(new JLabel("#"+(no+1)+". ")), c);
		c.gridx = 1; c.gridy = 0; centerPanel.add(new JCustomLabel("Old Tare ", FastFont.BOLD), c);
		c.gridx = 5; c.gridy = 0; centerPanel.add(new JCustomLabel("Measure ", FastFont.BOLD), c);
		c.gridx = 6; c.gridy = 0; centerPanel.add(new JCustomLabel("New Tare ", FastFont.BOLD), c);
		c.gridx = 8; c.gridy = 0; centerPanel.add(Box.createHorizontalStrut(10), c);
		c.gridx = 9; c.gridy = 0; centerPanel.add(new JCustomLabel("Calc. Cons. [/day/animal]", FastFont.BOLD), c);


		c.gridheight = 1;
		c.gridx = 10; c.gridy = 0; centerPanel.add(UIUtils.createHorizontalBox(new JCustomLabel("N=", FastFont.SMALLER), nAnimalsTextField, Box.createGlue()), c);
		c.gridheight = 1; c.weightx = 0;


		JCustomTextField foodOldTareLabel = new JCustomTextField(CustomFieldType.DOUBLE);
		foodOldTareLabel.setTextDouble(prevFood==null? null: prevFood.getFoodTare());
		foodOldTareLabel.setEnabled(false);
		foodOldTareLabel.setForeground(Color.BLUE);

		JCustomTextField waterOldTareLabel = new JCustomTextField(CustomFieldType.DOUBLE);
		waterOldTareLabel.setTextDouble(prevWater==null? null: prevWater.getWaterTare());
		waterOldTareLabel.setEnabled(false);
		waterOldTareLabel.setForeground(Color.BLUE);

		if(!onlyShowRequired || foodRequired) {

			c.gridx = 0; c.gridy = 2; centerPanel.add(new JCustomLabel("  Food [g]: ", FastFont.BOLD), c);
			c.gridx = 1; c.gridy = 2; centerPanel.add(UIUtils.createHorizontalBox(new JCustomLabel(prevFoodCons==null?"": prevFoodCons.fromPhase.getShortName()+": ", FastFont.SMALL), foodOldTareLabel), c);
			c.gridx = 5; c.gridy = 2; centerPanel.add(UIUtils.createHorizontalBox(new JCustomLabel(currentFw.getPhase().getShortName()+": ", FastFont.SMALL), foodMeasureTextField), c);
			c.gridx = 6; c.gridy = 2; centerPanel.add(UIUtils.createHorizontalBox(new JCustomLabel(currentFw.getPhase().getShortName()+": ", FastFont.SMALL), foodTareTextField), c);
			c.gridx = 9; c.gridy = 2; centerPanel.add(UIUtils.createVerticalBox(UIUtils.createHorizontalBox(foodIntervalLabel, foodConsumptionLabel), foodFormulaLabel), c);
			c.gridx = 10; c.gridy = 2; centerPanel.add(UIUtils.createVerticalBox(UIUtils.createHorizontalBox(nextFoodIntervalLabel, nextFoodConsumptionLabel), nextFoodFormulaLabel, Box.createHorizontalGlue()), c);
		}
		if(!onlyShowRequired || waterRequired) {
			c.gridx = 0; c.gridy = 3; centerPanel.add(new JCustomLabel("  Water [ml]: ", FastFont.BOLD), c);
			c.gridx = 1; c.gridy = 3; centerPanel.add(UIUtils.createHorizontalBox(new JCustomLabel(prevWaterCons==null?"": prevWaterCons.fromPhase.getShortName()+": ", FastFont.SMALL), waterOldTareLabel), c);
			c.gridx = 5; c.gridy = 3; centerPanel.add(UIUtils.createHorizontalBox(new JCustomLabel(currentFw.getPhase().getShortName()+": ", FastFont.SMALL), waterMeasureTextField), c);
			c.gridx = 6; c.gridy = 3; centerPanel.add(UIUtils.createHorizontalBox(new JCustomLabel(currentFw.getPhase().getShortName()+": ", FastFont.SMALL), waterTareTextField), c);
			c.gridx = 9; c.gridy = 3; centerPanel.add(UIUtils.createVerticalBox(UIUtils.createHorizontalBox(waterIntervalLabel, waterConsumptionLabel), waterFormulaLabel), c);
			c.gridx = 10; c.gridy = 3; centerPanel.add(UIUtils.createVerticalBox(UIUtils.createHorizontalBox(nextWaterIntervalLabel, nextWaterConsumptionLabel), nextWaterFormulaLabel, Box.createHorizontalGlue()), c);
		}
		c.weighty=1; c.gridx = 10; c.gridy = 4; centerPanel.add(new JPanel(), c);





		updateLabels(prevFoodCons, prevWaterCons, nextFoodCons, nextWaterCons);

		StringBuilder groupLabel = new StringBuilder();
		Set<Group> groups = container.getGroups();
		if(groups.size()>3) {
			groupLabel.append("###");
		} else {
			for (Group gr : groups) {
				groupLabel.append((groupLabel.length()>0? ",": "") + gr.getShortName());
			}
		}
		Group group = container.getGroup();

		StringBuilder sb = new StringBuilder();
		for(Biosample b: container.getBiosamples()) {
			boolean dead = b.isDeadAt(phase);
			sb.append((dead?"<span style='color:#CC8888'>":"") +  b.getSampleIdName() +(dead?"</span>":"") + "<br>");
		}
		JLabel content = new JLabel("<html>"+sb);
		content.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1), BorderFactory.createLoweredBevelBorder()));
		content.setFont(FastFont.SMALLER);
		content.setOpaque(true);
		content.setBackground(UIUtils.getDilutedColor(Color.LIGHT_GRAY, group==null? Color.LIGHT_GRAY: group.getBlindedColor(SpiritFrame.getUsername())));

		ContainerLabel containerLabel = new ContainerLabel(ContainerDisplayMode.CONTAINERID, container);
		JPanel westPanel = UIUtils.createVerticalBox(
				containerLabel,
				new JLabel(groupLabel.toString()),
				content,
				Box.createVerticalGlue());

		westPanel.setOpaque(true);
		westPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
		if(group!=null) westPanel.setBackground(group.getBlindedColor(SpiritFrame.getUsername()));

		add(BorderLayout.WEST, westPanel);
		add(BorderLayout.CENTER, centerPanel);

		setBorder(BorderFactory.createEtchedBorder());
	}

	private void updateLabels(Consumption prevFood, Consumption prevWater,  Consumption nextFood, Consumption nextWater) {

		//Current
		if(prevFood!=null && prevFood.toPhase!=null && prevFood.toPhase.equals(phase)) {
			foodIntervalLabel.setForeground(Color.BLACK);
			foodFormulaLabel.setForeground(Color.BLACK);
		} else {
			foodIntervalLabel.setForeground(Color.LIGHT_GRAY);
			foodFormulaLabel.setForeground(Color.LIGHT_GRAY);
		}
		foodIntervalLabel.setText(prevFood==null?"": prevFood.fromPhase.getShortName()+" -> "+prevFood.toPhase.getShortName()+": ");
		foodConsumptionLabel.setText(prevFood==null || prevFood.value==null?"": prevFood.value+"g  ");
		foodFormulaLabel.setText(prevFood==null || prevFood.formula==null? null: "("+prevFood.formula+")");


		if(prevWater!=null && prevWater.toPhase!=null && prevWater.toPhase.equals(phase)) {
			waterIntervalLabel.setForeground(Color.BLACK);
			waterFormulaLabel.setForeground(Color.BLACK);
		} else {
			waterIntervalLabel.setForeground(Color.LIGHT_GRAY);
			waterFormulaLabel.setForeground(Color.LIGHT_GRAY);
		}
		waterIntervalLabel.setText(prevWater==null?"": prevWater.fromPhase.getShortName()+" -> "+prevWater.toPhase.getShortName()+": ");
		waterConsumptionLabel.setText(prevWater==null || prevWater.value==null?"": prevWater.value+"ml  ");
		waterFormulaLabel.setText(prevWater==null || prevWater.formula==null? null: "("+prevWater.formula+")");

		//Next
		if(nextFood!=null && nextFood.fromPhase!=null && nextFood.fromPhase.equals(phase)) {
			nextFoodIntervalLabel.setForeground(Color.BLACK);
			nextFoodFormulaLabel.setForeground(Color.BLACK);
		} else {
			nextFoodIntervalLabel.setForeground(Color.LIGHT_GRAY);
			nextFoodFormulaLabel.setForeground(Color.LIGHT_GRAY);
		}
		nextFoodIntervalLabel.setText(nextFood==null?"": nextFood.fromPhase.getShortName()+" -> "+nextFood.toPhase.getShortName()+": ");
		nextFoodConsumptionLabel.setText(nextFood==null || nextFood.value==null?"": nextFood.value+"g  ");
		nextFoodFormulaLabel.setText(nextFood==null || nextFood.formula==null? null: "("+nextFood.formula+")");

		if(nextWater!=null && nextWater.fromPhase!=null && nextWater.fromPhase.equals(phase)) {
			nextWaterIntervalLabel.setForeground(Color.BLACK);
			nextWaterFormulaLabel.setForeground(Color.BLACK);
		} else {
			nextWaterIntervalLabel.setForeground(Color.LIGHT_GRAY);
			nextWaterFormulaLabel.setForeground(Color.LIGHT_GRAY);
		}
		nextWaterIntervalLabel.setText(nextWater==null?"": nextWater.fromPhase.getShortName()+" -> "+nextWater.toPhase.getShortName()+": ");
		nextWaterConsumptionLabel.setText(nextWater==null || nextWater.value==null?"": nextWater.value+"ml  ");
		nextWaterFormulaLabel.setText(nextWater==null || nextWater.formula==null? null: "("+nextWater.formula+")");

	}

	public static String formatTooltipText(Object val, String user, java.util.Date date) {
		return (val==null?"NA":val + " - "+user+" "+FormatterUtils.formatDateTime(date));
	}

	public List<JTextComponent> getRequiredComponents() {
		return requiredComponents;
	}

	public boolean isRequired(FoodWater fw, Container container, boolean water) {
		if(container==null || phase==null) return false; //Should not happen
		for(Biosample b: container.getBiosamples()) {
			if(b.getInheritedStudy()==null) continue;
			StudyAction a = b.getStudyAction(phase);
			if(a==null) continue;
			if(water && a.isMeasureWater()) return true;
			if(!water && a.isMeasureFood()) return true;
		}
		return false;
	}
}
