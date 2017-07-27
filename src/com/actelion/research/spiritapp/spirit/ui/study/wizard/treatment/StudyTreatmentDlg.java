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

package com.actelion.research.spiritapp.spirit.ui.study.wizard.treatment;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.spirit.ui.util.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.util.component.JColorChooserButton;
import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.NamedTreatment.TreatmentUnit;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;

public class StudyTreatmentDlg extends JEscapeDialog {

	private boolean addDlg;
	private Study study;
	private NamedTreatment namedTreatment;
	private JColorChooserButton colorChooser = new JColorChooserButton(false);
	private JCustomTextField nameTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 18);

	private JCustomTextField compoundTextField1 = new JCustomTextField(15, "", "");
	private JCustomTextField doseTextField1 = new JCustomTextField(JCustomTextField.DOUBLE, "", 5);
	private JGenericComboBox<TreatmentUnit> unitComboBox1 = new JGenericComboBox<>(TreatmentUnit.values(), "Unit");
	private JGenericComboBox<String> applicationComboBox1 = new JGenericComboBox<>(new String[] {"ip", "ic", "id", "it", "iv", "po", "sc", "food admix", "orthotopic"}, "Administration");

	private JCustomTextField compoundTextField2 = new JCustomTextField(15, "", "");
	private JCustomTextField doseTextField2 = new JCustomTextField(JCustomTextField.DOUBLE, "", 5);
	private JGenericComboBox<TreatmentUnit> unitComboBox2 = new JGenericComboBox<>(TreatmentUnit.values(), "Unit");
	private JGenericComboBox<String> applicationComboBox2 = new JGenericComboBox<>(new String[] {"ip", "ic", "id", "it", "iv", "po", "sc", "food admix", "orthotopic"}, "Administration");



	public StudyTreatmentDlg(final Study study, NamedTreatment namedTreatment) {
		super(UIUtils.getMainFrame(), "Study - Treatment", true);

		//Init components
		nameTextField.setMaxChars(30);
		compoundTextField1.setMaxChars(30);
		compoundTextField2.setMaxChars(30);
		doseTextField1.setMaxChars(10);
		doseTextField2.setMaxChars(10);

		unitComboBox1.setPreferredWidth(100);
		applicationComboBox1.setPreferredWidth(80);
		applicationComboBox1.setEditable(true);

		unitComboBox2.setPreferredWidth(100);
		applicationComboBox2.setPreferredWidth(80);
		applicationComboBox2.setEditable(true);


		JPanel centerPanel = UIUtils.createBox(
				UIUtils.createTitleBox("Treatment",
						UIUtils.createGrid(
								UIUtils.createTable(
										new JLabel("Name: "), nameTextField,
										new JLabel("Color: "), colorChooser),
								UIUtils.createVerticalBox(new JInfoLabel("<html>The name is always visible.<br>For blind studies, use a meaningless label: ie. Red/Blue"),
										Box.createHorizontalGlue()))),

				null,
				UIUtils.createTitleBox("Compounds",
						UIUtils.createVerticalBox(
								UIUtils.createGrid(
										UIUtils.createTable(
												new JLabel("Compound 1: "), compoundTextField1,
												new JLabel("Dose/Unit/App.: "), UIUtils.createHorizontalBox(doseTextField1, unitComboBox1, applicationComboBox1, Box.createHorizontalGlue())),
										UIUtils.createVerticalBox(
												new JInfoLabel("<html>Optional<br>Used for the dose calculation in the live monitoring.<br> Hidden for users in 'blind-details'"),
												Box.createHorizontalGlue())),
								new JSeparator(),
								UIUtils.createTable(
										new JLabel("Compound 2: "), compoundTextField2,
										new JLabel("Dose/Unit/App.: "), UIUtils.createHorizontalBox(doseTextField2, unitComboBox2, applicationComboBox2, Box.createHorizontalGlue())))));


		if(namedTreatment==null) {
			addDlg = true;
			namedTreatment = new NamedTreatment();

			if(study.getNamedTreatments().size()==0) {
				nameTextField.setText("Vehicle");
			}
			nameTextField.selectAll();
			colorChooser.setColor(Color.BLACK);
		} else {
			nameTextField.setText(namedTreatment.getName());
			colorChooser.setColorRgb(namedTreatment.getColorRgb());


			compoundTextField1.setText(namedTreatment.getCompoundName1());
			doseTextField1.setTextDouble(namedTreatment.getDose1());
			unitComboBox1.setSelection(namedTreatment.getUnit1());
			applicationComboBox1.setSelectedItem(namedTreatment.getApplication1());

			compoundTextField2.setText(namedTreatment.getCompoundName2());
			doseTextField2.setTextDouble(namedTreatment.getDose2());
			unitComboBox2.setSelection(namedTreatment.getUnit2());
			applicationComboBox2.setSelectedItem(namedTreatment.getApplication2());


		}
		this.study = study;
		this.namedTreatment = namedTreatment;

		JButton deleteButton = new JButton("Delete");
		deleteButton.setVisible(!addDlg);
		if(!addDlg) {
			deleteButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ev) {
					try {
						List<StudyAction> toDelete = new ArrayList<StudyAction>();
						for(StudyAction a: study.getStudyActions()) {
							if(StudyTreatmentDlg.this.namedTreatment.equals(a.getNamedTreatment())) {
								toDelete.add(a);
							}
						}
						if(toDelete.size()>0) {
							int res = JOptionPane.showConfirmDialog(StudyTreatmentDlg.this, "There are still some actions with this treatments. Are you sure you want to remove it?", "Remove Treatment", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if(res!=JOptionPane.YES_OPTION) return;

							for(StudyAction a: toDelete) {
								a.setNamedTreatment(null);
							}
						}

						StudyTreatmentDlg.this.namedTreatment.remove();
						dispose();
					} catch (Exception e) {
						JExceptionDialog.showError(e);
					}

				}
			});
		}

		JButton okButton = new JButton("OK");
		getRootPane().setDefaultButton(okButton);
		okButton.addActionListener(ev-> {
			try {
				NamedTreatment t = StudyTreatmentDlg.this.namedTreatment;

				//Check unicity
				String name = nameTextField.getText();
				if(name.length()==0) throw new Exception("The name cannot be empty");
				for (NamedTreatment nt : StudyTreatmentDlg.this.study.getNamedTreatments()) {
					if(nt!=t && nt.getName().equals(name)) {
						throw new Exception("The treatment's name must be unique");
					}
				}


				//Update the NamedTreatment
				t.setName(nameTextField.getText());
				t.setColorRgb(colorChooser.getColorRgb());

				if(doseTextField1.getTextDouble()!=null && unitComboBox1.getSelection()==null) throw new Exception("Unit1 is required");
				t.setCompoundName1(compoundTextField1.getText());
				t.setDose1(doseTextField1.getTextDouble());
				t.setUnit1(unitComboBox1.getSelection());
				t.setApplication1(applicationComboBox1.getSelection());

				if((applicationComboBox2.getSelection())!=null || unitComboBox2.getSelection()!=null || doseTextField2.getTextDouble()!=null) {
					if(compoundTextField1.getText().length()==0) throw new Exception("Compound1 is required when a second dose is given");
					if(compoundTextField2.getText().length()==0) throw new Exception("Compound2 is required when a second dose is given");
				}
				if(doseTextField2.getTextDouble()!=null && unitComboBox2.getSelection()==null) throw new Exception("Unit2 is required");
				t.setCompoundName2(compoundTextField2.getText());
				t.setDose2(doseTextField2.getTextDouble());
				t.setUnit2(unitComboBox2.getSelection());
				t.setApplication2(applicationComboBox2.getSelection());


				if(addDlg) {
					StudyTreatmentDlg.this.study.getNamedTreatments().add(t);
					t.setStudy(StudyTreatmentDlg.this.study);
				}



				dispose();
			} catch (Exception e) {
				JExceptionDialog.showError(e);
			}
		});

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, centerPanel);
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), deleteButton, okButton));

		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);


	}

}
