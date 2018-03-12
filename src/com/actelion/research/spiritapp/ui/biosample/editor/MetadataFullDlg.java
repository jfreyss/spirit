/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.ui.biosample.editor;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.text.JTextComponent;

import com.actelion.research.spiritapp.ui.biosample.MetadataComponent;
import com.actelion.research.spiritapp.ui.biosample.MetadataComponentFactory;
import com.actelion.research.spiritapp.ui.biosample.SampleIdGenerateField;
import com.actelion.research.spiritapp.ui.biosample.SampleIdScanField;
import com.actelion.research.spiritapp.ui.biosample.edit.EditBiosampleTable;
import com.actelion.research.spiritapp.ui.util.component.BiotypeComboBox;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.UIUtils;

public class MetadataFullDlg extends JEscapeDialog {

	private final Biosample biosample;
	private final BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());
	private final SampleIdGenerateField<Biosample> sampleIdField = new SampleIdGenerateField<>();
	private final SampleIdScanField parentField = new SampleIdScanField();
	private JTextComponent  nameTextField;

	private final JPanel contentPane = new JPanel(new BorderLayout());
	private final JPanel content = new JPanel(new GridBagLayout());
	private boolean success = false;

	public MetadataFullDlg(EditBiosampleTable table, JComponent locationRelativeTo, Biosample biosample) {
		super(UIUtils.getMainFrame(), "Biosample - Edit Metadata", true);
		this.biosample = biosample;

		parentField.setExtraBiosamplesPool(table.getRows());

		JButton okButton = new JButton("OK");
		getRootPane().setDefaultButton(okButton);
		okButton.addActionListener(ev -> {
			try {
				updateModel();
				dispose();
				success = true;
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(MetadataFullDlg.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		});

		contentPane.add(BorderLayout.CENTER, content);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));
		setContentPane(contentPane);

		biotypeComboBox.setSelection(biosample.getBiotype());
		sampleIdField.setText(biosample.getSampleId());
		parentField.setBiosample(biosample.getParent());


		biotypeComboBox.addTextChangeListener(e-> {
			updateView();
		});



		updateView();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(locationRelativeTo);
		setVisible(true);
	}


	private List<JComponent> comps = new ArrayList<JComponent>();

	public void updateView() {


		final Biotype type = biotypeComboBox.getSelection();
		biosample.setBiotype(type);

		comps.clear();
		content.removeAll();
		JCustomTextField lbl = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 10);
		lbl.setText(biosample.getSampleId());
		lbl.setEnabled(false);


		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;

		c.gridy++;
		c.weightx = 0; c.gridx = 0; content.add(new JLabel("Biotype: "), c);
		c.weightx = 1; c.gridx = 1; content.add(UIUtils.createHorizontalBox(biotypeComboBox, Box.createHorizontalGlue()), c);

		c.gridy++;
		c.weightx = 0; c.gridx = 0; content.add(new JLabel("SampleId: "), c);
		c.weightx = 1; c.gridx = 1; content.add(sampleIdField, c);
		sampleIdField.putCachedSampleId(biosample, type==null? null: type.getPrefix(), biosample.getId()<=0? null:  biosample.getSampleId());
		sampleIdField.setText(biosample.getSampleId());


		//Separator
		c.gridy++;  content.add(Box.createVerticalStrut(5), c);
		c.gridy++; c.gridwidth=2; c.fill = GridBagConstraints.HORIZONTAL; c.gridx = 0; content.add(new JSeparator(), c); c.gridwidth=1; c.fill = GridBagConstraints.NONE;
		c.gridy++; content.add(Box.createVerticalStrut(5), c);

		c.gridy++;
		c.weightx = 0; c.gridx = 0; content.add(new JLabel("ParentId: "), c);
		c.weightx = 1; c.gridx = 1; content.add(parentField, c);

		//Separator
		c.gridy++;  content.add(Box.createVerticalStrut(5), c);
		c.gridy++; c.gridwidth=2; c.fill = GridBagConstraints.HORIZONTAL; c.gridx = 0; content.add(new JSeparator(), c); c.gridwidth=1; c.fill = GridBagConstraints.NONE;
		c.gridy++; content.add(Box.createVerticalStrut(5), c);


		if(type!=null) {
			c.weighty = 0;

			//Name
			if(type.getSampleNameLabel()!=null) {
				if(type.isNameAutocomplete()) {
					nameTextField = new JTextComboBox(true) {
						@Override
						public java.util.Collection<String> getChoices() {
							return DAOBiotype.getAutoCompletionFieldsForName(type, null);
						}
					};

				} else {
					nameTextField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 20);
				}
				nameTextField.setText(biosample.getSampleName());

				c.gridy++;
				c.weightx = 0; c.gridx = 0; content.add(new JLabel(type.getSampleNameLabel()+": "), c);
				c.weightx = 1; c.gridx = 1; content.add(nameTextField, c);
			}

			//Metadata
			for (BiotypeMetadata m : type.getMetadata()) {
				JComponent comp = MetadataComponentFactory.getComponentFor(m);
				if(comp instanceof MetadataComponent) {
					((MetadataComponent) comp).updateView(biosample, m);

				}
				comps.add(comp);

				c.gridy++;
				c.weightx = 0; c.gridx = 0; content.add(new JLabel(m.getName() + (m.isRequired()?"*":"") + ": "), c);
				c.weightx = 1; c.gridx = 1; content.add(comp, c);
			}

			c.gridy++;
			c.gridy++; c.gridwidth=2; c.fill = GridBagConstraints.HORIZONTAL; c.gridx = 0; content.add(new JSeparator(), c); c.gridwidth=1; c.fill = GridBagConstraints.NONE;

		}

		//Filler
		c.gridy++;
		c.weighty = 1; c.weightx = 1; c.gridx = 1; content.add(Box.createGlue(), c);

		contentPane.validate();
		pack();
		repaint();

	}

	public void updateModel() throws Exception {
		final Biotype type = biotypeComboBox.getSelection();
		if(type!=null) {
			biosample.setBiotype(type);
			biosample.setSampleId(sampleIdField.getText());

			if(parentField.getBiosample()!=null && parentField.getBiosample().getId()<=0) {
				throw new Exception("The Parent "+parentField.getBiosample()+" does not exist");
			}
			biosample.setParent(parentField.getBiosample());




			biosample.setSampleName(nameTextField.getText());
			int i = 0;
			for (BiotypeMetadata m : type.getMetadata()) {
				if(i>=comps.size()) break;
				((MetadataComponent) comps.get(i)).updateModel(biosample, m);
				i++;
			}
		}
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isSuccess() {
		return success;
	}

}
