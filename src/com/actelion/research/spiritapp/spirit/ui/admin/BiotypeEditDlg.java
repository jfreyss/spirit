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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerTypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritapp.spirit.ui.util.lf.BiotypeComboBox;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.biosample.AmountUnit;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.helper.ExpressionHelper;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CompareUtils;
import com.actelion.research.util.ui.JComboBoxBigPopup;
import com.actelion.research.util.ui.JCustomTextArea;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.TextChangeListener;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ValidationResult;

public class BiotypeEditDlg extends JSpiritEscapeDialog {


	private class MetadataPanel extends JPanel {
		private Collection<BiotypeMetadata> biotypeMetadatas;
		private List<MetadataRow> rows = new ArrayList<>();


		private class MetadataRow {
			private final BiotypeMetadata model;
			JButton addButton = new JButton("+");
			JButton delButton = new JButton("-");

			JCustomTextField nameTextField = new JCustomTextField(10, "");
			JButton paramButton = new JButton("View/Edit");
			JGenericComboBox<DataType> dataTypeComboBox = new JGenericComboBox<>(DataType.values(), false);
			JCheckBox requiredCheckBox = new JCheckBox("Req.");
			JCheckBox hideCheckBox = new JCheckBox("Sec.");
			final JLabel nCountDataLabel = new JLabel();
			int countData;

			public MetadataRow(final BiotypeMetadata t) {
				this.model = t;
				nameTextField.setText(t.getName());
				dataTypeComboBox.setSelection(t.getDataType());
				requiredCheckBox.setSelected(t.isRequired());
				hideCheckBox.setSelected(t.isSecundary());
				dataTypeComboBox.addItemListener(e-> {
					refresh();
				});

				delButton.setEnabled(false);
				new SwingWorkerExtended() {
					@Override
					protected void doInBackground() throws Exception {
						countData = DAOBiosample.countRelations(biotype, t);
					}
					@Override
					protected void done() {
						nCountDataLabel.setText(countData+" filled");
						delButton.setEnabled(countData==0);
					};
				};

				paramButton.addActionListener(e-> {
					eventClickParam();
					refresh();
				});

				addButton.addActionListener(e-> {
					final int index = rows.indexOf(MetadataRow.this);
					rows.add(index, new MetadataRow(new BiotypeMetadata()));
					update();
				});

				delButton.addActionListener(e-> {
					final int index = rows.indexOf(MetadataRow.this);
					rows.remove(index);
					update();
				});


				requiredCheckBox.setToolTipText("Required");
				hideCheckBox.setToolTipText("Hide in the table display");
			}

			private void eventClickParam() {
				if(dataTypeComboBox.getSelection()==DataType.BIOSAMPLE) {
					editParametersBiotype(model);
				} else if(dataTypeComboBox.getSelection()==DataType.FORMULA) {
					//For a formula attribute, the user can enter a formula
					editParametersFormula(model);
				} else {
					openEditParametersDlg(false, model, dataTypeComboBox.getSelection()!=DataType.AUTO);
				}
			}

			public void refresh() {
				paramButton.setToolTipText(null);
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
				} else if(dataTypeComboBox.getSelection().getParametersDescription()!=null) {
					paramButton.setText("Edit");
					paramButton.setVisible(true);
				} else {
					paramButton.setVisible(false);
				}

				metadataPanel.validate();
			}
			public BiotypeMetadata updateModel() {
				model.setName(nameTextField.getText());
				model.setDataType(dataTypeComboBox.getSelection());
				model.setRequired(requiredCheckBox.isSelected());
				model.setSecundary(hideCheckBox.isSelected());
				if(dataTypeComboBox.getSelection().getParametersDescription()==null) {
					model.setParametersArray(null);
				}
				return model;
			}
		}


		public MetadataPanel() {}

		public void update() {
			removeAll();
			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();

			c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.insets = new Insets(0, 0, 0, 5);
			c.gridx = 0; c.gridy = 0; add(new JLabel("#"), c);
			c.insets = new Insets(0, 5, 0, 50);
			c.gridx = 1; c.gridy = 0; add(new JLabel("Name"), c);
			c.gridx = 2; c.gridy = 0; add(new JLabel("Datatype"), c);
			c.gridx = 3; c.gridy = 0; add(new JLabel(""), c);
			c.insets = new Insets(0, 0, 0, 1);

			int index = 0;
			for (; index<rows.size(); index++) {
				final MetadataRow row = rows.get(index);
				java.util.Vector<String> moveToVector = new java.util.Vector<String>();
				moveToVector.add("Move>>>");
				for(int i=1; i<=rows.size(); i++) moveToVector.add("to #"+i);
				moveToVector.add("Convert to Main Metadata");

				final JComboBox<String> moveTo = new JComboBoxBigPopup<String>(70, moveToVector);
				final JLabel nLabel = new JLabel((index+1)+".");
				nLabel.setOpaque(true);
				nLabel.setBackground(Color.LIGHT_GRAY);
				c.insets = new Insets(0, 0, 2, 0);
				c.fill = GridBagConstraints.BOTH;
				c.gridx = 0; c.gridy = 3*index+2; c.gridheight = 3; add(nLabel, c);
				c.fill = GridBagConstraints.HORIZONTAL;
				c.insets = new Insets(0, 2, 0, 0);
				c.gridx = 1; c.gridy = 3*index+2; c.gridheight = 1; add(row.nameTextField, c);
				c.gridx = 2; c.gridy = 3*index+2; c.gridheight = 1; add(row.dataTypeComboBox, c);
				c.gridx = 3; c.gridy = 3*index+2; c.gridheight = 1; add(row.paramButton, c);
				c.gridx = 5; c.gridy = 3*index+2; c.gridheight = 1; add(row.requiredCheckBox, c);
				c.gridx = 6; c.gridy = 3*index+2; c.gridheight = 1; add(row.hideCheckBox, c);
				c.gridx = 7; c.gridy = 3*index+2; c.gridheight = 1; add(row.addButton, c);
				c.gridx = 8; c.gridy = 3*index+2; c.gridheight = 1; add(row.delButton, c);
				c.gridx = 9; c.gridy = 3*index+2; c.gridwidth = 1; c.gridheight = 1; add(moveTo, c);
				c.gridx = 10; c.gridy = 3*index+2; c.gridwidth = 1; c.gridheight = 1; add(row.nCountDataLabel, c);


				final int indexCopy = index;
				moveTo.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						int idx = moveTo.getSelectedIndex();
						moveTo.hidePopup();
						if(idx>=1 && idx<=rows.size()) {
							rows.add(idx-1, rows.remove(indexCopy));
							update();
						} else if(idx>rows.size()) {
							setMetadataAsName(row.model);
						}
					}
				});
			}

			JButton addButton = new JButton("+");
			addButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					rows.add(new MetadataRow(new BiotypeMetadata()));
					update();
				}
			});
			int line = 3*index + 2;
			c.gridx = 7; c.gridy = line; add(addButton, c);
			c.weightx = c.weighty = 1;
			c.gridx = 15; c.gridy = line+1; add(new JLabel(" "), c);
			int n = DAOBiosample.countRelations(biotype);
			deleteButton.setEnabled(n==0);
			deleteButton.setToolTipText(n+" entities");
			refresh();
			updateUI();
			pack();
		}

		public void refresh() {
			for (MetadataRow r : rows) r.refresh();
		}

		public void setMetadataTypes(Collection<BiotypeMetadata> metadataTypes){
			this.biotypeMetadatas = metadataTypes;
			rows = new ArrayList<MetadataRow>();
			if(metadataTypes!=null) {
				for (BiotypeMetadata m : metadataTypes) {
					rows.add(new MetadataRow(m));
				}
			}
			update();
		}

		public void updateModel() {
			biotypeMetadatas.clear();
			int index = 0;
			for (MetadataRow row : rows) {
				BiotypeMetadata m = row.updateModel();
				m.setIndex(++index);
				biotypeMetadatas.add(m);
			}
		}
	}

	private final Biotype biotype;
	private final BiotypeComboBox parentComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());
	private final JCustomTextField biotypeNameTextField = new JCustomTextField(12, "", "");
	private final JCheckBox nameCheckBox = new JCheckBox("", true);
	private final JCustomTextField nameLabelTextField = new JCustomTextField(12, "", "No Main Metadata");
	private final JCustomTextField prefixTextField = new JCustomTextField(7, "", "PREFIX");
	private final JLabel prefixExampleLabel = new JLabel();
	private final JGenericComboBox<AmountUnit> amountUnitComboBox = new JGenericComboBox<>(AmountUnit.values(), true);
	private final JGenericComboBox<BiotypeCategory> categoryComboBox = new JGenericComboBox<>(BiotypeCategory.values(), true);
	private final MetadataPanel metadataPanel = new MetadataPanel();
	private final JCheckBox abstractCheckbox = new JCheckBox("Abstract Biotype");
	private final JCheckBox editSampleIdCheckbox = new JCheckBox("The sampleId is set by the user");
	private final JCheckBox hideContainerCheckbox = new JCheckBox("No ContainerId");
	private final JCheckBox hiddenCheckbox = new JCheckBox("Hidden");
	private final ContainerTypeComboBox containerTypeComboBox = new ContainerTypeComboBox();
	private final JCheckBox nameAutocompleteCheckbox = new JCheckBox("Autocomplete");
	private final JCheckBox nameRequiredCheckbox = new JCheckBox("Required");
	private final JCheckBox nameUniqueCheckbox = new JCheckBox("Unique");
	private final JButton viewNamesButton = new JButton("View Entered");
	private final JButton setNameAsMetadataButton = new JButton("Convert to Metadata");

	private JButton deleteButton = new JIconButton(IconType.DELETE, "Delete");
	private JButton saveButton = new JIconButton(IconType.SAVE, "Save");

	public BiotypeEditDlg(Biotype type) {
		super(UIUtils.getMainFrame(), "Admin - Biotype - " + type.getName(), BiotypeEditDlg.class.getName());
		this.biotype = JPAUtil.reattach(type);

		TextChangeListener tl = src -> {prefixExampleLabel.setText(DAOBarcode.getExample(prefixTextField.getText()));};
		prefixTextField.addTextChangeListener(tl);

		//ContentPanel
		parentComboBox.setEnabled(parentComboBox.getValues().size()>0);
		JPanel contentPanel = UIUtils.createTitleBox("Biotype",
				UIUtils.createVerticalBox(
						UIUtils.createTable(3,5,0,
								new JLabel("Name: "), biotypeNameTextField, new JInfoLabel(" Unique"),
								new JLabel("Category: "), categoryComboBox, null,
								new JLabel("Inherited from: "), parentComboBox, new JInfoLabel(" Only to be used if the samples inherits all the properties of its parent (ex: cells from cellline)"),
								new JLabel("Prefix: "), UIUtils.createHorizontalBox(prefixTextField, prefixExampleLabel), new JInfoLabel(" Prefix for the automatic generation of ids (possible patterns are {YYYY}, {YY}, {MM}, {DD})")),
						UIUtils.createTable(2,5,0,
								null, UIUtils.createHorizontalBox(editSampleIdCheckbox, new JInfoLabel(" Check this box if the user can define the sampleId (ex: patientId, animalId)")),
								null, hiddenCheckbox))
				);


		JPanel containerPanelPanel = UIUtils.createTitleBox("Container",
				UIUtils.createHorizontalBox(new int[]{3,7},
						UIUtils.createVerticalBox(abstractCheckbox, hideContainerCheckbox),
						UIUtils.createTable(3,
								new JLabel("ContainerType: "), containerTypeComboBox, new JInfoLabel("Optional if the user can choose the type"),
								new JLabel("AmountUnit: "), amountUnitComboBox, new JInfoLabel("Optional if there is no amount"))));


		prefixTextField.setToolTipText("prefix for automatic generation");
		prefixTextField.setMaxChars(16);
		biotypeNameTextField.setMaxChars(20);
		containerTypeComboBox.setTextWhenEmpty("No fixed container");
		amountUnitComboBox.setTextWhenEmpty("No saved amount");

		///////////////////////////////
		//Name Panel
		nameLabelTextField.setToolTipText("The Main Field is used to discriminate the samples but it is not unique ");
		nameCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		viewNamesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openEditParametersDlg(true, null, false);
			}
		});
		setNameAsMetadataButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setNameAsMetadata();
			}
		});

		//NamePanel
		JPanel namePanel = UIUtils.createTitleBox("Main Metadata",
				UIUtils.createVerticalBox(
						new JInfoLabel("The main metadata should be sufficient to describe the sample (not unique, displayed with the SampleId)"),
						UIUtils.createHorizontalBox(nameCheckBox, new JLabel("Name: "), nameLabelTextField, Box.createHorizontalStrut(20), nameAutocompleteCheckbox, nameUniqueCheckbox, nameRequiredCheckbox, Box.createHorizontalStrut(40), viewNamesButton, setNameAsMetadataButton, Box.createHorizontalGlue())));


		//MetadataPanel
		JScrollPane sp = new JScrollPane(metadataPanel);
		sp.setPreferredSize(new Dimension(900, 500));


		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.NORTH, UIUtils.createVerticalBox(contentPanel, containerPanelPanel, namePanel));
		panel.add(BorderLayout.CENTER, UIUtils.createTitleBox("Other Metadata", sp));
		panel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(deleteButton, Box.createHorizontalGlue(), saveButton));
		setContentPane(panel);
		//UIUtils.enable(content, false);

		abstractCheckbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				int r = JOptionPane.showConfirmDialog(BiotypeEditDlg.this, "Are you sure you want to delete this type?", "Question", JOptionPane.YES_NO_OPTION);
				if(r!=JOptionPane.YES_OPTION) return;
				try {
					if(biotype==null || biotype.getId()<=0) throw new Exception("Nothing to delete!?");
					DAOBiotype.deleteBiotype(biotype, Spirit.askForAuthentication());
					dispose();
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Biotype.class, biotype);
					JOptionPane.showMessageDialog(BiotypeEditDlg.this, BiotypeEditDlg.this.biotype.getName() + " Deleted", "Success", JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception e) {
					JExceptionDialog.showError(e);
				}
			}
		});
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					eventSave();
				} catch (Exception e) {
					JExceptionDialog.showError(e);

				}
			}
		});

		contentPanel.setVisible(true);
		biotypeNameTextField.setText(biotype.getName());
		parentComboBox.setSelection(biotype.getParent());
		prefixTextField.setText(biotype.getPrefix());
		editSampleIdCheckbox.setSelected(!biotype.isHideSampleId());
		hideContainerCheckbox.setSelected(biotype.isHideContainer());
		nameLabelTextField.setText(biotype.getSampleNameLabel());
		nameCheckBox.setSelected(biotype.getSampleNameLabel()!=null);
		abstractCheckbox.setSelected(biotype.isAbstract());
		hiddenCheckbox.setSelected(biotype.isHidden());
		categoryComboBox.setSelection(biotype.getCategory());
		metadataPanel.setMetadataTypes(biotype.getMetadata());
		amountUnitComboBox.setSelection(biotype.getAmountUnit());
		containerTypeComboBox.setSelection(biotype.getContainerType());
		nameAutocompleteCheckbox.setSelected(biotype.isNameAutocomplete());
		nameRequiredCheckbox.setSelected(biotype.isNameRequired());
		nameUniqueCheckbox.setSelected(biotype.isNameUnique());

		refresh();
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

	private void refresh() {
		if(abstractCheckbox.isSelected()) {
			containerTypeComboBox.setSelection(null);
			containerTypeComboBox.setEnabled(false);

			amountUnitComboBox.setSelection(null);
			amountUnitComboBox.setEnabled(false);

			hideContainerCheckbox.setSelected(false);
			hideContainerCheckbox.setEnabled(false);
		} else {
			containerTypeComboBox.setEnabled(true);
			amountUnitComboBox.setEnabled(true);
			hideContainerCheckbox.setEnabled(true);
		}

		if(!nameCheckBox.isSelected()) nameLabelTextField.setText("");
		else if(nameLabelTextField.getText().length()==0) nameLabelTextField.setText("Name");

		nameLabelTextField.setEnabled(nameCheckBox.isSelected());
		nameAutocompleteCheckbox.setEnabled(nameCheckBox.isSelected());
		nameRequiredCheckbox.setEnabled(nameCheckBox.isSelected());
		nameUniqueCheckbox.setEnabled(nameCheckBox.isSelected());
		viewNamesButton.setEnabled(nameCheckBox.isSelected());
		setNameAsMetadataButton.setEnabled(nameCheckBox.isSelected());

	}

	private void updateModel() throws Exception {
		if(biotype==null) throw new Exception("Nothing to save!?");
		if(categoryComboBox.getSelection()==null) throw new Exception("The category is required");
		if(biotypeNameTextField.getText().length()==0) throw new Exception("The name is required");

		biotype.setName(biotypeNameTextField.getText());
		biotype.setParent(parentComboBox.getSelection());
		biotype.setPrefix(prefixTextField.getText());
		biotype.setHideContainer(hideContainerCheckbox.isSelected());
		biotype.setHideSampleId(!editSampleIdCheckbox.isSelected());
		biotype.setSampleNameLabel(nameLabelTextField.getText());
		biotype.setAbstract(abstractCheckbox.isSelected());
		biotype.setHidden(hiddenCheckbox.isSelected());
		biotype.setCategory(categoryComboBox.getSelection());
		biotype.setAmountUnit(amountUnitComboBox.getSelection());
		biotype.setContainerType(containerTypeComboBox.getSelection());
		biotype.setNameAutocomplete(nameAutocompleteCheckbox.isSelected());
		biotype.setNameUnique(nameUniqueCheckbox.isSelected());
		biotype.setNameRequired(nameRequiredCheckbox.isSelected());
		metadataPanel.updateModel();
	}

	private void eventSave() throws Exception {
		boolean add = biotype.getId()<=0;
		updateModel();

		DAOBiotype.persistBiotype(biotype, Spirit.askForAuthentication());
		SpiritChangeListener.fireModelChanged(add? SpiritChangeType.MODEL_ADDED: SpiritChangeType.MODEL_UPDATED, Biotype.class, biotype);

		dispose();

	}


	private void openEditParametersDlg(final boolean editNames, final BiotypeMetadata btMetadata, boolean editable) {
		final JLabel nLabel = new JLabel();

		//TextArea
		final JCustomTextArea paramTextArea = new JCustomTextArea(2, 15);
		paramTextArea.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(CaretEvent e) {
				String[] items = MiscUtils.split(paramTextArea.getText());
				nLabel.setText(items.length+" items");
			}
		});


		//Sort Button
		final JButton sortButton = new JButton("Sort");
		sortButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String[] items = MiscUtils.split(paramTextArea.getText());
				Arrays.sort(items, CompareUtils.STRING_COMPARATOR);
				paramTextArea.setText(MiscUtils.unsplit(items, "\n"));
			}
		});

		//Rename Button
		final JButton renameButton = new JButton("Rename Value");
		renameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new BiotypeRenameMetadataDlg(biotype, editNames, btMetadata);
			}
		});

		//ContentPane
		final JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.NORTH, new JLabel(editNames?"List of options": btMetadata.getDataType().getParametersDescription()));
		contentPane.add(BorderLayout.CENTER, new JScrollPane(paramTextArea));
		contentPane.add(BorderLayout.EAST, UIUtils.createVerticalBox(nLabel, sortButton, renameButton, Box.createVerticalGlue()));
		contentPane.add(new JScrollPane(paramTextArea));
		contentPane.setPreferredSize(new Dimension(500, 300));







		//Init dialog

		if(editNames) {
			sortButton.setEnabled(false);
			paramTextArea.setBackground(Color.LIGHT_GRAY);
			Set<String> items = DAOBiotype.getAutoCompletionFieldsForName(biotype, null);
			paramTextArea.setText(MiscUtils.unsplit(items.toArray(new String[0]), "\n"));
			paramTextArea.setEditable(false);
			paramTextArea.setVisible(true);
			JOptionPane.showMessageDialog(BiotypeEditDlg.this, contentPane, "Parameters",  JOptionPane.QUESTION_MESSAGE);
		} else if(!editable) {
			//View Mode
			sortButton.setEnabled(false);
			paramTextArea.setBackground(Color.LIGHT_GRAY);
			Set<String> items = DAOBiotype.getAutoCompletionFields(btMetadata, null);
			paramTextArea.setText(MiscUtils.unsplit(items.toArray(new String[0]), "\n"));
			paramTextArea.setEditable(false);
			paramTextArea.setVisible(true);
			JOptionPane.showMessageDialog(BiotypeEditDlg.this, contentPane, "Parameters",  JOptionPane.QUESTION_MESSAGE);
		} else {
			//Edit Mode
			String[] params = btMetadata.getParametersArray();
			paramTextArea.setText(MiscUtils.unsplit(params, "\n"));
			paramTextArea.setEditable(true);
			paramTextArea.setVisible(true);
			paramTextArea.setBackground(Color.WHITE);
			int res = JOptionPane.showOptionDialog(BiotypeEditDlg.this, contentPane, "Parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
			if(res==JOptionPane.YES_OPTION) {
				btMetadata.setParametersArray(MiscUtils.split(paramTextArea.getText()));
			}
		}

	}


	public void setNameAsMetadata() {
		try {
			SpiritUser user = SpiritFrame.getUser();

			updateModel();
			if(biotype.getSampleNameLabel()==null) throw new Exception(biotype+" has no Main Metadata");
			if(biotype.getId()<=0) throw new Exception(biotype+" was just created!??");

			int res = JOptionPane.showConfirmDialog(this, "Are you sure to set the Main Metadata as one of the metadata?\n (Note: this change will be done immediately, without any extra confirmation)", "Edit Biotype", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(res!=JOptionPane.YES_OPTION) return;
			DAOBiotype.moveNameToMetadata(biotype, user);

			SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biotype.class, biotype);
			dispose();

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					new BiotypeEditDlg(DAOBiotype.getBiotype(biotype.getName()));
				}
			});
		} catch(Exception ex) {
			JExceptionDialog.showError(ex);
		}
	}

	public void setMetadataAsName(BiotypeMetadata bm) {
		try {
			SpiritUser user = SpiritFrame.getUser();

			updateModel();
			if(biotype.getSampleNameLabel()!=null) throw new Exception(biotype+" has already a Main Field");
			if(bm.getId()<=0) throw new Exception("You can only convert this metadata if the biotype is saved");

			int res = JOptionPane.showConfirmDialog(this, "Are you sure to set "+bm.getName()+" as the Main Metadata?\n (Note: this change will be done immediately, without any extra confirmation) ", "Edit Biotype", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if(res!=JOptionPane.YES_OPTION) return;
			DAOBiotype.moveMetadataToName(bm, user);

			SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biotype.class, biotype);
			dispose();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					new BiotypeEditDlg(DAOBiotype.getBiotype(biotype.getName()));
				}
			});
		} catch(Exception ex) {
			JExceptionDialog.showError(ex);
		}

	}


	private void editParametersBiotype(BiotypeMetadata model) {
		//Biosample -> select biotype
		BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());
		biotypeComboBox.setSelectionString(model.getParameters());
		int res = JOptionPane.showOptionDialog(BiotypeEditDlg.this, biotypeComboBox, "Select Linked Biotype", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if(res==JOptionPane.YES_OPTION) {
			model.setParameters(biotypeComboBox.getSelectionString());
		}
	}


	private void editParametersFormula(BiotypeMetadata model) {
		String formula = model.getParameters();

		final JCustomTextArea paramTextArea = new JCustomTextArea(2, 15);
		final JLabel okLabel = new JLabel("");
		final JPanel contentPane = new JPanel(new BorderLayout());

		contentPane.add(BorderLayout.NORTH,
				new JLabel("<html><b>Enter the formula to be calculated:</b><ul>"
						+ "<li> Variables 'M1', 'M2', .. reference the 1st, 2nd attributes (if numeric)"
						+ "<li> ^ for exponentation, % for modulo"
						+ "<li> sqrt(), log(), exp(), abs() can be used"
						+ "<li> round(expression, n) to round to n decimals<br>"
						+ "</ul>Example:<br>"
						+ "round(sqrt(M1*M2), 2) to calculate a geometric average"));
		contentPane.add(BorderLayout.CENTER, new JScrollPane(paramTextArea));
		contentPane.add(BorderLayout.SOUTH, okLabel);
		contentPane.add(new JScrollPane(paramTextArea));
		contentPane.setPreferredSize(new Dimension(500, 300));


		paramTextArea.setText(model.getParameters());
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

			int res = JOptionPane.showOptionDialog(BiotypeEditDlg.this, contentPane, "Parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
			if(res==JOptionPane.YES_OPTION) {
				formula = paramTextArea.getText();
				try {
					validateFormula(formula);
					model.setParameters(formula);
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
		for (int i = 0; i < metadataPanel.rows.size(); i++) {
			if(metadataPanel.rows.get(i).dataTypeComboBox.getSelection()==DataType.NUMBER) variables.add("M"+(i+1));
		}
		//Evaluate expression
		Expression e = ExpressionHelper.createExpressionBuilder(expr).variables(variables).build();
		ValidationResult res = e.validate(false);
		if(!res.isValid()) throw new Exception(MiscUtils.flatten(res.getErrors(), ", "));
	}

}
