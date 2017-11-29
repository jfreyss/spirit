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

package com.actelion.research.spiritapp.ui.biosample.batchaliquot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.ui.biosample.MetadataComponent;
import com.actelion.research.spiritapp.ui.biosample.MetadataComponentFactory;
import com.actelion.research.spiritapp.ui.location.ContainerTypeComboBox;
import com.actelion.research.spiritapp.ui.util.lf.BiotypeComboBox;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class EditAttributesDlg extends JEscapeDialog {

	private final Sampling d;
	private BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());
	private JPanel contentPanel = new JPanel(new GridBagLayout());
	private boolean success = false;

	private List<JComponent> components = new ArrayList<>();
	private JCustomTextField amountTextField = new JCustomTextField(CustomFieldType.DOUBLE);
	private JCustomTextField commentsTextField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 30);
	private ContainerTypeComboBox containerTypeComboBox = new ContainerTypeComboBox(ContainerType.valuesOfRackable());


	public EditAttributesDlg(final Sampling sampling) {
		super(UIUtils.getMainFrame(), "Edit Metadata", true);
		this.d = sampling;

		biotypeComboBox.setMemorization(true);
		biotypeComboBox.addTextChangeListener(e-> {
			refresh();
		});
		containerTypeComboBox.setTextWhenEmpty("");

		//TopPanel
		JPanel topPanel = UIUtils.createHorizontalBox(new JLabel("Sample Type: "), biotypeComboBox, Box.createHorizontalGlue());

		//containerPanel
		JPanel containerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(1, 1, 1, 1);
		c.weightx = 0; c.gridx = 0; c.gridy=0; containerPanel.add(new JLabel("Container: "), c);

		c.weightx = 1; c.gridx = 1; c.gridy=0; containerPanel.add(UIUtils.createHorizontalBox(containerTypeComboBox/*, new JLabel("Bloc:"), containerIndexComboBox*/), c);


		//Center Panel
		topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
		contentPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
		containerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));

		JPanel centerPanel = new JPanel(new BorderLayout(3,3));
		centerPanel.add(BorderLayout.NORTH, topPanel);
		centerPanel.add(BorderLayout.CENTER, contentPanel);
		centerPanel.add(BorderLayout.SOUTH, containerPanel);

		biotypeComboBox.setSelection(d.getBiotype());
		refresh();

		//Buttons
		JButton okButton = new JButton("Ok");
		getRootPane().setDefaultButton(okButton);
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {

					//					if(containerIndexComboBox.isVisible() && containerIndexComboBox.getSelection()==null) {
					//						throw new Exception("You must specify a container index");
					//					}
					//
					//					Check that there are no samples coming from this sampling
					//					if(sampling.getSamples().size()>0) {
					//
					//						int res = NamedSamplingDlg.createOptionDialog(EditAttributesDlg.this, "There are "+sampling.getSamples().size()+" samples already saved. Would you like to update the metadata and the comments?", new ArrayList<Biosample>(sampling.getSamples()));
					//						if(res!=JOptionPane.YES_OPTION) return;
					//
					//						//Update Model
					//						updateModel();
					//
					//						//Update the samples
					//						for (Biosample b : sampling.getSamples()) {
					//							if(!b.getBiotype().equals( sampling.getBiotype())) throw new Exception("The biotype cannot be changed");
					//							sampling.updateMetadata(b);
					//							if(containerTypeComboBox.isEnabled()) b.setContainer(new Container(sampling.getContainerType()));
					//						}
					//
					//						JOptionPane.showMessageDialog(EditAttributesDlg.this, "The samples are updated, you can still cancel by closing the window without saving", "Samples deleted", JOptionPane.INFORMATION_MESSAGE);
					//					} else {
					//Update Model
					updateModel();
					//					}

					dispose();
					success = true;
				} catch (Exception e) {
					JExceptionDialog.showError(e);
				}

			}
		});

		//Update View
		containerTypeComboBox.setSelection(d.getContainerType());
		amountTextField.setTextDouble(d.getAmount());
		commentsTextField.setMaxChars(255);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, centerPanel);
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));

		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}


	public void refresh() {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		Biotype type = biotypeComboBox.getSelection();
		components.clear();
		contentPanel.removeAll();
		if(type!=null) {

			//Metadata
			for (BiotypeMetadata m : type.getMetadata()) {
				c.gridy++;
				c.gridx = 0; contentPanel.add(new JLabel(m.getName()  + (m.isRequired()?"*":"") + ": "), c);
				JComponent comp = MetadataComponentFactory.getComponentFor(m);
				if(comp instanceof MetadataComponent) {
					((MetadataComponent) comp).setData(d.getMetadata(m));
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
			commentsTextField.setText(d.getComments());



			c.gridy++;
			c.weighty = 1;
			c.weightx = 1;
			c.gridx = 1; contentPanel.add(new JLabel(""), c);
		}
		contentPanel.updateUI();
		pack();
	}

	public Sampling updateModel() {
		Biotype type = biotypeComboBox.getSelection();

		d.setBiotype(type);
		if(type!=null) {

			//Update Metadata
			int i = 0;
			Map<BiotypeMetadata, String> metadataMap = new HashMap<>();
			for (BiotypeMetadata m : type.getMetadata()) {
				if(i>components.size()) break;
				if(!(components.get(i) instanceof MetadataComponent)) continue;
				String data = ((MetadataComponent) components.get(i)).getData();
				metadataMap.put(m, data);
				i++;
			}
			d.setMetadataMap(metadataMap);

			d.setAmount(amountTextField.getTextDouble());

			d.setComments(commentsTextField.getText());

			d.setContainerType(containerTypeComboBox.getSelection());
			//			if(containerIndexComboBox.isVisible()) {
			//				d.setBlocNumber(containerIndexComboBox.getSelection());
			//			} else {
			//				d.setBlocNumber(null);
			//			}
			//			d.setWeighingRequired(weightCheckBox.isSelected());
			//			d.setLengthRequired(lengthCheckBox.isSelected());
			//			d.setCommentsRequired(commentCheckBox.isSelected());


		} else {
			d.setMetadataMap(new HashMap<BiotypeMetadata, String>());
		}
		return d;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isSuccess() {
		return success;
	}

}
