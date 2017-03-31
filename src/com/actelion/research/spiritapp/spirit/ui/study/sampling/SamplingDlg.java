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

package com.actelion.research.spiritapp.spirit.ui.study.sampling;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import com.actelion.research.spiritapp.spirit.ui.biosample.MetadataComponent;
import com.actelion.research.spiritapp.spirit.ui.biosample.MetadataComponentFactory;
import com.actelion.research.spiritapp.spirit.ui.container.ContainerTypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.lf.BiotypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.study.wizard.ExtraMeasurementDlg;
import com.actelion.research.spiritapp.spirit.ui.util.correction.Correction;
import com.actelion.research.spiritapp.spirit.ui.util.correction.CorrectionDlg;
import com.actelion.research.spiritapp.spirit.ui.util.correction.CorrectionMap;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.study.Measurement;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.UIUtils;

/**
 * The SamplingDlg is responsible for editing a Sampling, within a study or not
 * @author Joel Freyss
 *
 */
public class SamplingDlg extends JEscapeDialog {

	private BiotypeComboBox typeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());
	private final Sampling sampling;
	private JPanel contentPanel = new JPanel(new GridBagLayout());
	private boolean success = false;

	private List<JComponent> components = new ArrayList<>();
	private JTextComponent nameTextField;
	private JCustomTextField amountTextField = new JCustomTextField(JCustomTextField.DOUBLE);
	private JCustomTextField commentsTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 30);

	private final JCheckBox weightCheckBox = new JCheckBox("Weight");
	private final JCheckBox lengthCheckBox = new JCheckBox("Length");
	private final JCheckBox commentCheckBox = new JCheckBox("Observations");
	private final ContainerTypeComboBox containerTypeComboBox = new ContainerTypeComboBox();
	private final BlocNoComboBox containerIndexComboBox = new BlocNoComboBox(false);

	private final JPanel measurementPanel = new JPanel();
	private final JLabel warningLabel1 = new JCustomLabel(" ", Color.RED);
	private final JLabel warningLabel2 = new JCustomLabel(" ", Color.RED);

	public SamplingDlg(final NamedSamplingDlg dlg, final Study study, final Sampling sampling, boolean addActions) {
		super(dlg, "Edit Sampling", true);
		this.sampling = sampling;

		typeComboBox.addTextChangeListener(e-> {
			refresh();
		});
		containerTypeComboBox.addPropertyChangeListener(JTextComboBox.PROPERTY_TEXTCHANGED, e-> {
			ContainerType type = containerTypeComboBox.getSelection();
			containerIndexComboBox.setVisible(type!=null && type.isMultiple());
			containerIndexComboBox.setSelection(1);

			if(type==ContainerType.K7 || type==ContainerType.SLIDE) {
				warningLabel1.setText("Cassette and slides should only be created in SlideCare");
			} else {
				warningLabel1.setText("");
			}
		});
		containerIndexComboBox.setVisible(sampling.getContainerType()!=null && sampling.getContainerType().isMultiple());


		//If samples are already generated and are in a named container, the container cannot be anymore changed
		if(sampling.getSamples().size()>0) {
			typeComboBox.setEnabled(false);
		}
		typeComboBox.setSelection(sampling.getBiotype());

		//Extra measurement
		measurementPanel.setOpaque(false);

		//Center Panel
		JPanel centerPanel = new JPanel(new BorderLayout(3,3));
		centerPanel.add(BorderLayout.NORTH, UIUtils.createHorizontalBox(BorderFactory.createEmptyBorder(8, 8, 8, 8), new JLabel("BioType: "), typeComboBox, Box.createHorizontalGlue()));
		centerPanel.add(BorderLayout.CENTER, UIUtils.createTitleBox("Metadata", contentPanel));
		centerPanel.add(BorderLayout.SOUTH, UIUtils.createVerticalBox(
				!addActions? null: UIUtils.createTitleBox("Storage", UIUtils.createHorizontalBox(new JLabel("Container: "), containerTypeComboBox, containerIndexComboBox, Box.createHorizontalGlue())),
						!addActions? null: UIUtils.createTitleBox("Measurements", UIUtils.createHorizontalBox(
								UIUtils.createVerticalBox(weightCheckBox, lengthCheckBox, commentCheckBox, Box.createVerticalGlue()),
								Box.createHorizontalStrut(20),
								UIUtils.createVerticalBox(measurementPanel, Box.createVerticalGlue()),
								Box.createHorizontalGlue())),
								UIUtils.createHorizontalBox(Box.createHorizontalStrut(5), warningLabel1),
								UIUtils.createHorizontalBox(Box.createHorizontalStrut(5), warningLabel2)
				));

		refresh();

		//Buttons
		JButton okButton = new JButton("Ok");
		getRootPane().setDefaultButton(okButton);
		okButton.addActionListener(ev-> {
			try {

				if(validateModel()) return;

				Sampling fromSampling = sampling.clone();
				fromSampling.setId(sampling.getId());
				fromSampling.setSamples(sampling.getSamples());

				updateModel();

				if(dlg!=null) {
					dlg.synchronizeSamples(study, fromSampling, sampling);
				}

				dispose();
				success = true;
			} catch (Exception e) {
				JExceptionDialog.showError(e);
			}
		});

		//Update View
		containerTypeComboBox.setSelection(sampling.getContainerType());
		containerIndexComboBox.setSelection(sampling.getBlocNo());
		weightCheckBox.setSelected(sampling.isWeighingRequired());
		lengthCheckBox.setSelected(sampling.isLengthRequired());
		commentCheckBox.setSelected(sampling.isCommentsRequired());
		amountTextField.setTextDouble(sampling.getAmount());
		commentsTextField.setMaxChars(255);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, centerPanel);
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));

		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

	private void refresh() {
		refreshMeasurementPanel();
		refreshMetadataPanel();
		getContentPane().validate();
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
	}

	private void refreshMeasurementPanel() {
		measurementPanel.removeAll();
		List<JComponent> comps = new ArrayList<>();

		for(final Measurement m: sampling.getMeasurements()) {
			JButton removeButton = new JButton("Del.");
			removeButton.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
			removeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					List<Measurement> list = new ArrayList<>(sampling.getMeasurements());
					list.remove(m);
					sampling.setMeasurements(list);
					refresh();
				}
			});
			comps.add(new JLabel(m.getDescription()));
			comps.add(removeButton);

		}

		JButton addMeasurementButton = new JButton("Add Measurement");
		addMeasurementButton.addActionListener(e-> {
			ExtraMeasurementDlg dlg = new ExtraMeasurementDlg();
			Measurement m = dlg.getExtraMeasurement();
			if(m!=null) {
				List<Measurement> list = new ArrayList<>(sampling.getMeasurements());
				list.add(m);
				sampling.setMeasurements(list);
				refresh();
			}
		});
		comps.add(addMeasurementButton);
		comps.add(null);


		measurementPanel.add(UIUtils.createTable(comps));
	}

	public void refreshMetadataPanel() {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		Biotype type = typeComboBox.getSelection();
		components.clear();
		contentPanel.removeAll();
		if(type!=null) {

			//Name
			if(type.getSampleNameLabel()!=null) {
				c.gridy++;
				c.gridx = 0; contentPanel.add(new JLabel(type.getSampleNameLabel()  + (type.isNameRequired()?"*":"") + ": "), c);

				if(type.isNameAutocomplete()) {
					nameTextField = new JTextComboBox(new ArrayList<String>( DAOBiotype.getAutoCompletionFieldsForName(type, null)));
					((JTextComboBox)nameTextField).setColumns(20);
				} else {
					nameTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 20);
				}
				nameTextField.setText(sampling.getSampleName());

				c.gridx = 1; contentPanel.add(nameTextField, c);
			}

			//Metadata
			for (BiotypeMetadata m : type.getMetadata()) {
				c.gridy++;
				c.gridx = 0; contentPanel.add(new JLabel(m.getName()  + (m.isRequired()?"*":"") + ": "), c);
				JComponent comp = MetadataComponentFactory.getComponentFor(m);
				if(comp instanceof MetadataComponent) {
					((MetadataComponent) comp).setData(sampling.getMetadata(m));
				}
				components.add(comp);
				c.gridx = 1; contentPanel.add(comp, c);
			}

			//Amount
			if(type.getAmountUnit()!=null) {
				c.gridy++;
				c.gridx = 0; contentPanel.add(new JLabel(type.getAmountUnit().getName() + ": "), c);
				c.gridx = 1; contentPanel.add(UIUtils.createHorizontalBox(amountTextField, new JLabel(type.getAmountUnit().getUnit())), c);
			}
			//Comments
			c.gridy++;
			c.gridx = 0; contentPanel.add(new JLabel("Comments: "), c);
			c.gridx = 1; contentPanel.add(commentsTextField, c);
			commentsTextField.setText(sampling.getComments());

			c.gridy++;
			c.weighty = 1;
			c.weightx = 1;
			c.gridx = 1; contentPanel.add(new JLabel(""), c);
		}
	}

	/**
	 *
	 * @return true if sth changed in the validation process
	 * @throws Exception
	 */
	public boolean validateModel() throws Exception {
		Biotype type = typeComboBox.getSelection();
		if(type==null) throw new Exception("You must select a type");
		if(containerIndexComboBox.isVisible() && containerIndexComboBox.getSelection()==null) {
			throw new Exception("You must specify a container index");
		}

		//Validate name
		if(type.getSampleNameLabel()!=null) {
			String data = nameTextField.getText();
			if(type.isNameRequired() && data.length()==0) throw new Exception(type.getSampleNameLabel()+" is required");

			if(data.length()>0 && type.isNameAutocomplete()) {


				Set<String> possibleValues = new TreeSet<String>(DAOBiotype.getAutoCompletionFieldsForName(type, null));
				if(!possibleValues.contains(data)) {
					CorrectionMap<Biotype, JTextComponent> correctionMap1 = new CorrectionMap<Biotype, JTextComponent>();

					Correction<Biotype, JTextComponent> correction = correctionMap1.addCorrection(type, data, new ArrayList<String>( possibleValues), false);
					correction.getAffectedData().add(nameTextField);

					CorrectionDlg<Biotype, JTextComponent> dlg = new CorrectionDlg<Biotype, JTextComponent>(this, correctionMap1) {
						@Override
						public String getSuperCategory(Biotype att) {
							return att.getName();
						}
						@Override
						protected String getName(Biotype att) {
							return att.getSampleNameLabel();
						}
						@Override
						protected void performCorrection(Correction<Biotype, JTextComponent> correction, String newValue) {
							for (JTextComponent t : correction.getAffectedData()) {
								t.setText(newValue);
							}
						}
					};

					if(dlg.getReturnCode()!=CorrectionDlg.OK) return true;
				}
			}
		}


		//Validate Metadata
		int i = 0;
		for (BiotypeMetadata m : type.getMetadata()) {
			if(i>components.size()) break;
			if((components.get(i) instanceof MetadataComponent)) {
				String data = ((MetadataComponent) components.get(i)).getData();
				if(m.isRequired() && data.length()==0) throw new Exception(m.getName()+" is required");
			}
			i++;
		}
		return false;
	}

	public Sampling updateModel() throws Exception {
		Biotype type = typeComboBox.getSelection();

		sampling.setBiotype(type);
		if(type!=null) {

			//Update name
			if(type.getSampleNameLabel()!=null) {
				String data = nameTextField.getText();
				if(type.isNameRequired() && data.length()==0) throw new Exception(type.getSampleNameLabel()+" is required");
				sampling.setSampleName(data);
			}
			//Update Metadata
			Map<BiotypeMetadata, String> metadataMap = new HashMap<>();
			int i = 0;
			for (BiotypeMetadata m : type.getMetadata()) {
				if(i>components.size()) break;
				if((components.get(i) instanceof MetadataComponent)) {
					String data = ((MetadataComponent) components.get(i)).getData();
					metadataMap.put(m, data);

					if(m.isRequired() && data.length()==0) throw new Exception(m.getName()+" is required");
				}
				i++;
			}
			sampling.setMetadataMap(metadataMap);

			sampling.setAmount(amountTextField.getTextDouble());

			sampling.setComments(commentsTextField.getText());

			sampling.setContainerType(containerTypeComboBox.getSelection());
			if(containerIndexComboBox.isVisible()) {
				sampling.setBlocNo(containerIndexComboBox.getSelection());
			} else {
				sampling.setBlocNo(null);
			}
			sampling.setWeighingRequired(weightCheckBox.isSelected());
			sampling.setLengthRequired(lengthCheckBox.isSelected());
			sampling.setCommentsRequired(commentCheckBox.isSelected());


		} else {
			sampling.setMetadataMap(new HashMap<BiotypeMetadata, String>());
		}
		return sampling;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isSuccess() {
		return success;
	}

}
