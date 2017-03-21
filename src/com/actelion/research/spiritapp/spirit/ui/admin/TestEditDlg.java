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

package com.actelion.research.spiritapp.spirit.ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.lf.BiotypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.result.TestAttribute.OutputType;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.helper.ExpressionHelper;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JCustomTextArea;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ValidationResult;

public class TestEditDlg extends JSpiritEscapeDialog {

	private final Test test;
	private JTextField testNameTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 14);
	private JGenericComboBox<String> testCategoryComboBox;

	private JPanel inputPanel = new JPanel(new GridBagLayout());
	private JPanel outputPanel = new JPanel(new GridBagLayout());
	private JPanel infoPanel = new JPanel(new GridBagLayout());
	private List<AttributeRow> inputRows = new ArrayList<>();
	private List<AttributeRow> outputRows = new ArrayList<>();
	private List<AttributeRow> infoRows = new ArrayList<>();

	class AttributeRow {
		TestAttribute model;
		JTextField name;
		JGenericComboBox<DataType> dataTypeComboBox;
		JCheckBox required;
		JButton paramButton = new JButton("View/Edit");
		int index;

		public AttributeRow(final TestAttribute att){
			this.model = att;
			name = new JCustomTextField(JCustomTextField.ALPHANUMERIC, att.getName(), 12);
			dataTypeComboBox = new JGenericComboBox<DataType>(DataType.values(), true);
			required = new JCheckBox("Req.", att.isRequired());
			index = att.getIndex();

			dataTypeComboBox.setSelection(att.getDataType());

			dataTypeComboBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					refresh();
				}
			});
			refresh();

			paramButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					//For a biosample attribute, the user can select a biotype
					if(dataTypeComboBox.getSelection()==DataType.BIOSAMPLE) {
						editParametersBiotype(att);
					} else if(dataTypeComboBox.getSelection()==DataType.FORMULA) {
						//For a formula attribute, the user can enter a formula
						editParametersFormula(att);
					} else if(dataTypeComboBox.getSelection()==DataType.AUTO || dataTypeComboBox.getSelection()==DataType.LIST) {
						//For a Auto or list attribute, the user can enter a list of choices
						editParametersChoice(att, dataTypeComboBox.getSelection());
					}
					refresh();
				}
			});
		}

		public void refresh() {
			if(dataTypeComboBox.getSelection()==DataType.AUTO) {
				paramButton.setText("View Entered");
				paramButton.setVisible(true);
			} else if(dataTypeComboBox.getSelection()==DataType.MULTI || dataTypeComboBox.getSelection()==DataType.LIST) {
				paramButton.setText("Edit Choices ("+model.getParametersArray().length+")");
				paramButton.setVisible(true);
			} else if(dataTypeComboBox.getSelection()==DataType.BIOSAMPLE) {
				paramButton.setText(model.getParameters()==null || model.getParameters().length()==0? "Select": model.getParameters());
				paramButton.setVisible(true);
			} else if(dataTypeComboBox.getSelection()==DataType.FORMULA) {
				paramButton.setText(model.getParameters()==null || model.getParameters().length()==0? "Edit": model.getParameters().substring(0, Math.min(model.getParameters().length(), 15)));
				paramButton.setToolTipText(model.getParameters());
				paramButton.setVisible(true);

			} else if(dataTypeComboBox.getSelection()!=null && dataTypeComboBox.getSelection().getParametersDescription()!=null) {
				paramButton.setText("Edit");
				paramButton.setVisible(true);
			} else {
				paramButton.setVisible(false);
			}
		}

		public TestAttribute updateAtt(OutputType outputType) {
			model.setOutputType(outputType);
			model.setName(name.getText());
			model.setDataType(dataTypeComboBox.getSelection());
			model.setRequired(required.isSelected());
			return model;
		}

		public void remove() {
			if(model.getTest()!=null) {
				model.getTest().getAttributes().remove(model);
				model.setTest(null);
			}
		}
	}

	public TestEditDlg(Test t) {
		super(UIUtils.getMainFrame(), "Admin - Tests", TestEditDlg.class.getName());
		this.test = JPAUtil.reattach(t);
		testCategoryComboBox = new JGenericComboBox<String>(Test.getTestCategories(DAOTest.getTests()), true);
		testCategoryComboBox.setEditable(true);

		testNameTextField.setText(test.getName());
		testCategoryComboBox.setSelection(test.getCategory());

		//Test Panel
		JPanel testPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(1,1,1,1);
		c.gridx = 0; c.gridy = 0; testPanel.add(new JLabel("Category: "), c);
		c.gridx = 0; c.gridy = 2; testPanel.add(new JLabel("TestName: "), c);

		c.weightx = 0;
		c.gridx = 1; c.gridy = 0; testPanel.add(testCategoryComboBox, c);
		c.gridx = 1; c.gridy = 2; testPanel.add(testNameTextField, c);

		c.weightx = 1;
		c.gridx = 2; c.gridy = 0; testPanel.add(new JInfoLabel("Ex: Physiology, Genetics, ..."), c);
		c.gridx = 2; c.gridy = 2; testPanel.add(new JInfoLabel("Unique"), c);



		inputRows.clear();
		for (TestAttribute att : test.getAttributes()) {
			switch(att.getOutputType()){
			case INPUT: inputRows.add(new AttributeRow(att)); break;
			case OUTPUT: outputRows.add(new AttributeRow(att)); break;
			case INFO: infoRows.add(new AttributeRow(att)); break;
			}
		}

		//Input Panel
		JScrollPane inputScrollPane = new JScrollPane(inputPanel);
		inputScrollPane.setPreferredSize(new Dimension(500, 190));

		//Output Panel
		JScrollPane outputScrollPane = new JScrollPane(outputPanel);
		outputScrollPane.setPreferredSize(new Dimension(500, 200));

		//Info Panel
		JScrollPane infoScrollPane = new JScrollPane(infoPanel);
		outputScrollPane.setPreferredSize(new Dimension(500, 180));

		for (OutputType outputType : OutputType.values()) {
			refreshPanel(outputType);
		}

		JPanel centerPanel = UIUtils.createVerticalBox(
				UIUtils.createTitleBox("Test Name", testPanel),
				UIUtils.createTitleBox("Input Attributes", UIUtils.createBox(inputScrollPane, new JInfoLabel("Input attributes are optional and describe the test conditions (biomarker, method, ...)"))),
				UIUtils.createTitleBox("Output Attributes", UIUtils.createBox(outputScrollPane, new JInfoLabel("You must have at least one output attribute"))),
				UIUtils.createTitleBox("Info Attributes", UIUtils.createBox(infoScrollPane, new JInfoLabel("Info attributes are optional and are used to give extra information about a result (comments, raw value, ...)"))));

		JButton okButton = new JIconButton(IconType.SAVE, "Save");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					eventOk();
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(TestEditDlg.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

			}
		});

		JPanel content = new JPanel(new BorderLayout());
		content.add(BorderLayout.CENTER, centerPanel);
		content.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));
		setContentPane(content);

		UIUtils.adaptSize(this, 820, 770);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);

	}

	public void updateModel() {
		test.setName(testNameTextField.getText());
		test.setCategory(testCategoryComboBox.getSelection());
		test.getAttributes().clear();
		for (AttributeRow row : inputRows) {
			test.getAttributes().add(row.updateAtt(OutputType.INPUT));
		}
		for (AttributeRow row : outputRows) {
			test.getAttributes().add(row.updateAtt(OutputType.OUTPUT));
		}
		for (AttributeRow row : infoRows) {
			test.getAttributes().add(row.updateAtt(OutputType.INFO));
		}
	}

	public void refreshPanel(final OutputType outputType) {
		final JPanel panel;
		final List<AttributeRow> rows;
		switch (outputType) {
		case INPUT:
			panel = inputPanel;
			rows = inputRows;
			break;
		case OUTPUT:
			panel = outputPanel;
			rows = outputRows;
			break;
		case INFO:
			panel = infoPanel;
			rows = infoRows;
			break;
		default:
			return;
		}

		panel.removeAll();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.insets = new Insets(0, 3, 1, 3);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 0;
		c.insets = new Insets(0, 0, 0, 10);
		c.gridx = 0; c.gridy = 0; panel.add(new JLabel("#"), c);
		c.insets = new Insets(0, 0, 0, 95);
		c.gridx = 1; c.gridy = 0; panel.add(new JLabel("Name [unit]"), c);
		c.insets = new Insets(0, 0, 0, 95);
		c.gridx = 2; c.gridy = 0; panel.add(new JLabel("Datatype"), c);
		c.insets = new Insets(0, 0, 0, 30);
		c.gridx = 5; c.gridy = 0; panel.add(new JLabel("Required?"), c);
		c.insets = new Insets(0,0,0,0);
		c.insets = new Insets(0,0,0,10);
		c.gridx = 7; c.gridy = 0; panel.add(new JLabel(" "), c);

		c.weightx=1; c.gridx = 10; c.gridy = 0; panel.add(new JLabel(" "), c); c.weightx=0;
		c.insets = new Insets(0,0,0,0);
		for (int i = 0; i < rows.size(); i++) {
			final AttributeRow row = rows.get(i);
			final int index = i;
			JButton addButton = new JButton("+");
			addButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					TestAttribute ta = new TestAttribute();
					ta.setTest(test);
					rows.add(index, new AttributeRow(ta));
					updateModel();
					refreshPanel(outputType);
				}
			});
			JButton delButton = new JButton("-");
			delButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int n = DAOTest.countRelations(row.model);
					if(n>0) {
						JExceptionDialog.showError("You must first delete the "+n+" results with a "+row.model.getName());
						return;
					}
					AttributeRow r = rows.remove(index);
					r.remove();
					updateModel();
					refreshPanel(outputType);
				}
			});
			final JComboBox<String> moveButton = new JComboBox<String>(new String[] {"Move", "Up", "Down", "To Input", "To Output", "To Info"});
			moveButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if(moveButton.getSelectedIndex()==1 && index>0) {
						rows.add(index-1, rows.remove(index));
					} else if(moveButton.getSelectedIndex()==2 && index+1<rows.size()) {
						rows.add(index, rows.remove(index+1));
					} else if(moveButton.getSelectedIndex()==3 && outputType!=OutputType.INPUT) {
						inputRows.add(rows.remove(index));
						refreshPanel(OutputType.INPUT);
					} else if(moveButton.getSelectedIndex()==4 && outputType!=OutputType.OUTPUT) {
						outputRows.add(rows.remove(index));
						refreshPanel(OutputType.OUTPUT);
					} else if(moveButton.getSelectedIndex()==5 && outputType!=OutputType.INFO) {
						infoRows.add(rows.remove(index));
						refreshPanel(OutputType.INFO);
					}

					moveButton.setSelectedIndex(0);
					refreshPanel(outputType);
				}
			});
			c.gridx = 0; c.gridy = i+1; panel.add(new JLabel((i+1)+"."), c);
			c.gridx = 1; c.gridy = i+1; panel.add(row.name, c);
			c.gridx = 2; c.gridy = i+1; panel.add(row.dataTypeComboBox, c);
			c.gridx = 3; c.gridy = i+1; panel.add(row.paramButton, c);
			c.gridx = 5; c.gridy = i+1; panel.add(row.required, c);
			c.gridx = 6; c.gridy = i+1; panel.add(addButton, c);
			c.gridx = 7; c.gridy = i+1; panel.add(delButton, c);
			c.gridx = 8; c.gridy = i+1; panel.add(moveButton, c);
		}
		JButton addButton = new JButton("+");
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rows.add(new AttributeRow(new TestAttribute()));
				updateModel();
				refreshPanel(outputType);
			}
		});
		c.gridx = 6; c.gridy = rows.size()+1; panel.add(addButton, c);

		c.weighty = 1;
		c.gridx = 10; c.gridy = rows.size()+1; panel.add(new JLabel(" "), c);
		panel.updateUI();
	}

	public void eventOk() {
		boolean create = test.getId()<=0;
		try {
			if(testCategoryComboBox.getSelection()==null) throw new Exception("Category is required");
			if(!Test.getTestCategories(DAOTest.getTests()).contains(testCategoryComboBox.getSelection())) {
				int res = JOptionPane.showConfirmDialog(this, "The category "+testCategoryComboBox.getSelection()+" is new.\n Are you sure you want to create it?", "New category", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(res!=JOptionPane.YES_OPTION) return;

			}

			if(testNameTextField.getText().trim().length()==0) throw new Exception("Test Name is required");

			SpiritUser user = Spirit.askForAuthentication();

			updateModel();

			DAOTest.persistTests(Collections.singleton(test), user);
			JOptionPane.showMessageDialog(this, "Test '" + test + "' " + (create? "created": "updated"), "Success", JOptionPane.INFORMATION_MESSAGE);
			dispose();
			SpiritChangeListener.fireModelChanged(create? SpiritChangeType.MODEL_ADDED: SpiritChangeType.MODEL_UPDATED, Test.class, test);
		} catch (Exception e) {
			JExceptionDialog.showError(e);
		}
	}

	private void editParametersBiotype(TestAttribute att) {
		BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());
		biotypeComboBox.setSelectionString(att.getParameters());
		int res = JOptionPane.showOptionDialog(TestEditDlg.this, biotypeComboBox, "Select Linked Biotype", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if(res==JOptionPane.YES_OPTION) {
			att.setParameters(biotypeComboBox.getSelectionString());
		}
	}

	private void editParametersFormula(TestAttribute att) {
		String formula = att.getParameters();

		final JCustomTextArea paramTextArea = new JCustomTextArea(2, 15);
		final JLabel okLabel = new JLabel("");
		final JPanel contentPane = new JPanel(new BorderLayout());

		contentPane.add(BorderLayout.NORTH,
				new JLabel("<html><b>Enter the formula to be calculated:</b><ul>"
						+ "<li> Variables 'I1', 'I2' reference the 1st, 2nd input (numeric only)"
						+ "<li> Variables 'O1', 'O2' reference the 1st, 2nd output (numeric only)<br>"
						+ "<li> ^ for exponentation, % for modulo"
						+ "<li> sqrt(), log(), exp(), abs() can be used"
						+ "<li> round(expression, n) to round to n decimals<br>"
						+ "</ul>Example:<br>"
						+ "round(sqrt(O1*O2), 2) to calculate a geometric average"));
		contentPane.add(BorderLayout.CENTER, new JScrollPane(paramTextArea));
		contentPane.add(BorderLayout.SOUTH, okLabel);
		contentPane.add(new JScrollPane(paramTextArea));
		contentPane.setPreferredSize(new Dimension(500, 300));


		paramTextArea.setText(att.getParameters());
		paramTextArea.setEditable(true);
		paramTextArea.setVisible(true);
		paramTextArea.setBackground(Color.WHITE);


		CaretListener l = new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				try {
					validateFormula(paramTextArea.getText());
					okLabel.setForeground(Color.GREEN);
					okLabel.setText("Valid");
				} catch(Exception ex2) {
					okLabel.setForeground(Color.RED);
					String s = ex2.getMessage();
					okLabel.setText("Error: "+(s.length()>30? s.substring(0, 30): s));
				}
			}
		};
		paramTextArea.addCaretListener(l);
		l.caretUpdate(null);


		while(true) {

			int res = JOptionPane.showOptionDialog(TestEditDlg.this, contentPane, "Parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
			if(res==JOptionPane.YES_OPTION) {
				formula = paramTextArea.getText();
				try {
					validateFormula(formula);
					att.setParameters(formula);
					return;
				} catch(Exception e) {
					JExceptionDialog.showError("The formula is not correct: "+e);
				}
			} else {
				return;
			}
		}
	}

	private void validateFormula(String expr) throws Exception {
		Set<String> variables = new HashSet<>();
		for (int i = 0; i < inputRows.size(); i++) {
			if(inputRows.get(i).dataTypeComboBox.getSelection()==DataType.NUMBER) variables.add("I"+(i+1));
		}
		for (int i = 0; i < outputRows.size(); i++) {
			if(outputRows.get(i).dataTypeComboBox.getSelection()==DataType.NUMBER) variables.add("O"+(i+1));
		}
		//Evaluate expression
		Expression e = ExpressionHelper.createExpressionBuilder(expr).variables(variables).build();
		ValidationResult res = e.validate(false);
		if(!res.isValid()) throw new Exception(MiscUtils.flatten(res.getErrors(), ", "));
	}

	private void editParametersChoice(TestAttribute att, DataType dataType) {
		final JCustomTextArea paramTextArea = new JCustomTextArea(2, 15);
		final JLabel nLabel = new JLabel();
		final JButton sortButton = new JButton("Sort");

		final JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.NORTH, new JLabel(dataType.getParametersDescription()));
		contentPane.add(BorderLayout.CENTER, new JScrollPane(paramTextArea));
		contentPane.add(BorderLayout.EAST, UIUtils.createVerticalBox(nLabel, sortButton, Box.createVerticalGlue()));
		contentPane.add(new JScrollPane(paramTextArea));
		contentPane.setPreferredSize(new Dimension(500, 300));


		paramTextArea.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				String[] items = MiscUtils.split(paramTextArea.getText());
				nLabel.setText(items.length+" items");
			}
		});
		sortButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String[] items = MiscUtils.split(paramTextArea.getText());
				Arrays.sort(items, CompareUtils.STRING_COMPARATOR);
				paramTextArea.setText(MiscUtils.unsplit(items, "\n"));
			}
		});

		if(dataType==DataType.AUTO) {
			paramTextArea.setBackground(Color.LIGHT_GRAY);
			Set<String> items = DAOTest.getAutoCompletionFields(att);
			paramTextArea.setText(MiscUtils.unsplit(items.toArray(new String[0]), "\n"));
			paramTextArea.setEditable(false);
			paramTextArea.setVisible(true);
		} else {
			String[] params = att.getParametersArray();
			paramTextArea.setText(MiscUtils.unsplit(params, "\n"));
			paramTextArea.setEditable(true);
			paramTextArea.setVisible(true);
			paramTextArea.setBackground(Color.WHITE);
		}



		int res = JOptionPane.showOptionDialog(TestEditDlg.this, contentPane, "Parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if(res==JOptionPane.YES_OPTION) {
			att.setParametersArray(MiscUtils.split(paramTextArea.getText()));
		}
	}


}
