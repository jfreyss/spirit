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

package com.actelion.research.spiritapp.ui.biosample.form;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.biosample.MetadataComponent;
import com.actelion.research.spiritapp.ui.biosample.MetadataComponentFactory;
import com.actelion.research.spiritapp.ui.biosample.SampleIdGenerateField;
import com.actelion.research.spiritapp.ui.location.ContainerTextField;
import com.actelion.research.spiritapp.ui.location.ContainerTypeComboBox;
import com.actelion.research.spiritapp.ui.location.LocationPosTextField;
import com.actelion.research.spiritapp.ui.study.GroupLabel;
import com.actelion.research.spiritcore.business.biosample.BarcodeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.TextChangeListener;

/**
 * Panel used to represent the edition of a biosample as a form.
 *
 * @author Joel Freyss
 *
 */
public class MetadataFormPanel extends JPanel {

	private boolean showContainerLocationSample;
	private final boolean multicolumns;
	private Biosample biosample;


	private final GroupLabel groupLabel = new GroupLabel();
	private final JCustomTextField elbTextField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 10);

	private final SampleIdGenerateField<Biosample> sampleIdTextField = new SampleIdGenerateField<>();
	private final ContainerTypeComboBox containerTypeComboBox = new ContainerTypeComboBox();
	private final ContainerTextField containerIdTextField = new ContainerTextField();
	private final JCustomTextField amountTextField = new JCustomTextField(CustomFieldType.DOUBLE, 5);
	private final LocationPosTextField locationTextField = new LocationPosTextField();
	private JCustomTextField nameTextField = null;
	private final List<JComponent> components = new ArrayList<>();
	private final JCustomTextField commentsTextField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 18);

	private boolean editable = true;

	private TextChangeListener listener = src -> eventTextChanged();

	public MetadataFormPanel(boolean showContainerLocationSample, boolean multicolumns) {
		this(showContainerLocationSample, multicolumns, null);
	}

	public MetadataFormPanel(boolean showContainerLocationSample, boolean multicolumns, Biosample biosample) {
		super(new GridBagLayout());
		this.showContainerLocationSample = showContainerLocationSample;
		this.multicolumns = multicolumns;
		setOpaque(false);

		//Add TextChangeListener
		containerTypeComboBox.addTextChangeListener(e-> {
			eventTextChanged();
			containerIdTextField.setEnabled(containerTypeComboBox.getSelection()!=null && containerTypeComboBox.getSelection().getBarcodeType()!=BarcodeType.NOBARCODE);
		});
		locationTextField.addTextChangeListener(listener);
		containerIdTextField.addTextChangeListener(listener);
		amountTextField.addTextChangeListener(listener);
		commentsTextField.addTextChangeListener(listener);

		setBiosample(biosample);
	}

	public void setEditable(boolean editable) {
		if(this.editable==editable) return;
		this.editable = editable;
		initUI();
	}

	public boolean isEditable() {
		return editable;
	}

	/**
	 * updateView is called by this function
	 */
	private void initUI() {
		boolean refresh = getComponentCount()>0;
		if(refresh) {removeAll(); components.clear();}

		if(biosample!=null && biosample.getBiotype()!=null) {
			final Biotype biotype = biosample.getBiotype();
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			c.gridy = 0;
			c.ipady = -1;
			c.insets = new Insets(0, 0, 0, 0);
			int n = 0;

			containerTypeComboBox.setEnabled(false);
			//ELB/Study
			c.gridy = 0;
			c.gridx = 0; c.gridwidth=2; c.fill=GridBagConstraints.BOTH; add(groupLabel, c);
			c.gridwidth=1;

			//Container
			if(!biotype.isAbstract()) {
				c.fill=GridBagConstraints.NONE;
				if(showContainerLocationSample) {
					if(!biotype.isHideContainer()) {
						//ContainerType
						c.gridy = 5;
						c.gridx = 0; add(new JLabel("ContainerType: "), c);
						c.gridx = 1; add(containerTypeComboBox, c);
						if(biotype.getContainerType()!=null) {
							containerTypeComboBox.setSelection(biotype.getContainerType());
							containerTypeComboBox.setEnabled(false);
						} else {
							containerTypeComboBox.setEnabled(editable);
						}

						//ContainerId
						if((biotype.getContainerType()==null || biotype.getContainerType().getBarcodeType()==BarcodeType.MATRIX) && !biotype.isHideContainer()) {
							c.gridy = 6;
							c.gridx = 0; add(new JLabel("ContainerId: "), c);
							c.gridx = 1; add(containerIdTextField, c);
							containerIdTextField.setEnabled(editable && containerTypeComboBox.getSelection()!=null && containerTypeComboBox.getSelection().getBarcodeType()!=BarcodeType.NOBARCODE);
						}
					}
				}

				//Amount
				if(biotype.getAmountUnit()!=null) {
					c.gridy = multicolumns? 5: 7;
					c.gridx = multicolumns? 3: 0; add(new JLabel(biotype.getAmountUnit().getNameUnit()+": "), c);
					c.gridx = multicolumns? 4: 1; add(amountTextField, c);
					amountTextField.setEnabled(editable);
				}

				if(showContainerLocationSample) {
					//Location
					c.gridy = multicolumns? 6: 8;
					c.gridx = multicolumns? 3: 0; add(new JLabel("Location: "), c);
					c.fill=GridBagConstraints.HORIZONTAL;
					c.gridx = multicolumns? 4: 1; add(locationTextField, c);
					locationTextField.setEnabled(editable);
				}

			}

			//Separator
			c.weightx = 1;
			c.gridy = 9; c.gridwidth = multicolumns?99:2; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(5, 0, 5, 0); ; c.ipady = 2;
			c.gridx = 0; add(new JSeparator(JSeparator.HORIZONTAL), c);
			c.weightx = 0; c.gridwidth=1; c.insets = new Insets(0, 0, 0, 0); ; c.ipady = 0;

			//Spacer for proper alignment
			c.gridy = 10; c.gridx = 1; add(Box.createHorizontalStrut(240), c);
			c.gridy = 11;
			c.fill=GridBagConstraints.NONE;
			if(showContainerLocationSample) {
				//SampleId
				c.gridx = 0; add(new JCustomLabel("SampleId: ", FastFont.BOLD), c); //Name
				c.gridx = 1; add(sampleIdTextField, c);
				sampleIdTextField.setEnabled(editable && !biotype.isHideSampleId());

				c.gridy++;
			}

			int offsetY = 12;
			int offsetX = 0;
			c.gridy = offsetY;

			//Name
			if(biotype.getSampleNameLabel()!=null) {
				String text = nameTextField==null?"": nameTextField.getText();
				if(biotype.isNameAutocomplete()) {
					nameTextField = new JTextComboBox() {
						@Override
						public Collection<String> getChoices() {
							return DAOBiotype.getAutoCompletionFieldsForName(biotype, null);
						}
					};
				} else {
					nameTextField = new JCustomTextField();
				}
				nameTextField.setText(text);
				nameTextField.addTextChangeListener(listener);

				c.gridx = 0; add(new JCustomLabel(biotype.getSampleNameLabel() + ": "), c); //Name
				c.fill=GridBagConstraints.HORIZONTAL;
				c.gridx = 1; add(nameTextField, c);
				c.gridy++;
				nameTextField.setEnabled(editable);
			}

			//Metadata
			for(BiotypeMetadata bm: biotype.getMetadata()) { //Metadata
				if(multicolumns && biotype.getMetadata().size()>=2 && (n++)==(biotype.getMetadata().size()+1)/2) {
					c.gridy = offsetY;
					c.gridx = offsetX+2;
					offsetX+=3;
					add(Box.createHorizontalStrut(15), c);
				}

				JComponent comp = MetadataComponentFactory.getComponentFor(bm);
				if(comp instanceof MetadataComponent) {
					((MetadataComponent) comp).addTextChangeListener(listener);
				}
				components.add(comp);

				c.fill=GridBagConstraints.NONE; c.gridx = offsetX; add(new JLabel(bm.getName()+": "), c);
				c.fill=GridBagConstraints.HORIZONTAL; c.gridx = offsetX+1; add(comp, c);
				c.gridy++;
				comp.setEnabled(editable);

			}

			//Comments
			c.fill=GridBagConstraints.NONE; c.gridx = offsetX; add(new JLabel("Comments: "), c);
			c.fill=GridBagConstraints.HORIZONTAL; c.gridx = offsetX+1; add(commentsTextField, c);
			c.gridy++;
			commentsTextField.setEnabled(editable);

			//Spacer
			c.weightx = 1;
			c.weighty = 1;
			c.gridx = 20; add(Box.createHorizontalGlue(), c);

		}

		updateView();

		super.validate();
		repaint();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if(nameTextField!=null) nameTextField.setEnabled(enabled);
		for (JComponent c : components) {
			c.setEnabled(enabled);
		}
		commentsTextField.setEnabled(enabled);
		amountTextField.setEnabled(enabled);
	}

	/**
	 * This function calls initUI
	 * @param biosample
	 */
	public void setBiosample(Biosample biosample) {
		this.biosample = biosample;
		initUI();

	}

	public void updateModel() {
		updateModel(biosample);
	}
	/**
	 * Updates Model (can be different of the encapsulated biosample
	 * @param b
	 */
	public void updateModel(Biosample b) {
		if(b==null) return;
		Biotype biotype = b.getBiotype();
		if(biotype==null) return;
		assert b.getBiotype().equals(biotype);

		b.setElb(elbTextField.getText());
		if(!biotype.isAbstract()) {

			if(containerTypeComboBox.isVisible() && containerTypeComboBox.isEnabled()) b.setContainerType(containerTypeComboBox.getSelection());
			else b.setContainerType(biotype.getContainerType());
			if(containerIdTextField.isEnabled()) b.setContainerId(containerIdTextField.getText());
			if(amountTextField.isShowing() && biotype.getAmountUnit()!=null) {
				b.setAmount(amountTextField.getTextDouble());
			}
			try {
				if(locationTextField.isShowing()) locationTextField.updateBiosample();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		if(!biotype.isHideSampleId()) {
			b.setSampleId(sampleIdTextField.getText());
		}
		if(biotype.getSampleNameLabel()!=null) {
			b.setSampleName(nameTextField==null? null: nameTextField.getText());
		}
		int n = 0;
		for(BiotypeMetadata bm: biotype.getMetadata()) {
			JComponent c = components.get(n++);
			if(c instanceof MetadataComponent) {
				((MetadataComponent)c).updateModel(b, bm);
			}
		}
		b.setComments(commentsTextField.getText());
		if(amountTextField!=null) b.setAmount(amountTextField.getTextDouble());

	}

	/**
	 * Updates View
	 * @param b
	 */
	public void updateView() {
		Biosample b = biosample;
		if(b==null || b.getBiotype()==null) return;
		Biotype biotype = b.getBiotype();


		if(b.getAttachedStudy()!=null || (b.getParent()!=null && b.getInheritedPhase()!=null && !b.getInheritedPhase().equals(b.getParent().getInheritedPhase()))) {
			groupLabel.setVisible(true);
			groupLabel.setBorder(BorderFactory.createLoweredSoftBevelBorder());
			groupLabel.setText(b.getInheritedStudy()==null?"":
				b.getInheritedStudy().getStudyId() + (b.getInheritedGroup()==null?"": " " + b.getInheritedGroupString(SpiritFrame.getUsername()) + " " + b.getInheritedPhaseString()), b.getInheritedGroup());
		} else {
			groupLabel.setVisible(false);
			groupLabel.setText("");
		}
		elbTextField.setText(b.getElb());

		if(!biotype.isAbstract()) {
			if(containerTypeComboBox.isEnabled()) containerTypeComboBox.setSelection(b.getContainerType());
			if(containerIdTextField.isEnabled()) containerIdTextField.setText(b.getContainerId());
			if(biotype.getAmountUnit()!=null) {
				amountTextField.setTextDouble(b.getAmount());
			}
			locationTextField.setBiosample(b);
		}

		sampleIdTextField.putCachedSampleId(b, biotype.getPrefix(), b.getId()<=0? null: b.getSampleId());
		sampleIdTextField.setText(b.getSampleId());

		if(biotype.getSampleNameLabel()!=null) {
			nameTextField.setText(b.getSampleName());
		}

		int n = 0;
		for(BiotypeMetadata bm: biotype.getMetadata()) {
			JComponent c = components.get(n++);
			if(c instanceof MetadataComponent) {
				((MetadataComponent)c).updateView(b, bm);
			}
		}
		commentsTextField.setText(b.getComments());
	}

	public List<JComponent> getMetadataComponents() {
		return components;
	}

	/**
	 * Can be overidden
	 */
	public void eventTextChanged() {

	}
}
