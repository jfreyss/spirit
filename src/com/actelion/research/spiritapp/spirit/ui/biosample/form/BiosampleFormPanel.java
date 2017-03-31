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

package com.actelion.research.spiritapp.spirit.ui.biosample.form;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleFinder;
import com.actelion.research.spiritapp.spirit.ui.lf.BiotypeComboBox;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

/**
 * BiosampleFormPanel is the class responsible for the form edition of one or a batch of biosamples of the same type
 * @author Joel Freyss
 *
 */
public class BiosampleFormPanel extends JPanel {

	public enum EditMode {
		READ, EDIT, NEW
	}
	public enum ViewMode {
		FORM, BATCH
	}

	private final BiosampleFormDlg dlg;
	private Biosample biosample = null;

	private BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());

	private JButton changeButton = new JButton("Select Existing Parent"); //only mode==edit
	private JButton createButton = new JButton("Reset"); //only mode==edit
	private JButton removeButton = new JButton("Unset"); //only for first and last
	private JRadioButton createOneButton = new JRadioButton("Create One", true); //only for leave-node
	private JRadioButton createBatchButton = new JRadioButton("Batch");  //only for leave-node

	private JPanel cardPanel = new JPanel(new GridLayout());
	private MetadataFormPanel formPanel  = new MetadataFormPanel(true, true);
	private BiosampleBatchPanel scannerPanel = new BiosampleBatchPanel();

	private Biotype biotype;
	private boolean highlightBiosample = false;

	private int push = 0;
	private EditMode editMode = EditMode.NEW;
	private ViewMode mode = ViewMode.FORM;


	/**
	 * Creates a form panel for selecting or editing biosamples in form or scanner mode
	 * @param dlg
	 */
	public BiosampleFormPanel(final BiosampleFormDlg dlg) {
		this.dlg = dlg;
		setLayout(new BorderLayout());
		setBackground(UIUtils.getDilutedColor(getBackground(), UIUtils.getColor(95, 165, 197), .85));

		ButtonGroup group = new ButtonGroup();
		group.add(createOneButton);
		group.add(createBatchButton);

		///////////////////////////////////
		//Layout components
		setOpaque(false);
		cardPanel.setOpaque(false);
		formPanel.setOpaque(false);
		scannerPanel.setOpaque(false);
		cardPanel.setMinimumSize(new Dimension(300, 150));
		cardPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0), BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY)));
		//Layout
		add(BorderLayout.NORTH, UIUtils.createHorizontalBox(BorderFactory.createEmptyBorder(1, 0, 5, 0), new JLabel("Biotype: "), biotypeComboBox, changeButton, createButton, removeButton, Box.createHorizontalStrut(30), createOneButton, Box.createHorizontalStrut(10), createBatchButton, Box.createHorizontalGlue()));
		add(BorderLayout.CENTER, cardPanel);
		add(BorderLayout.SOUTH, Box.createVerticalGlue());
		//Events
		biotypeComboBox.addTextChangeListener(e-> {

			if(push>0) return;
			if(biotype==biotypeComboBox.getSelection()) return;

			try {
				if(mode==ViewMode.FORM && !getBiosampleFromFormMode().isEmpty()) {
					throw new Exception("You cannot change the type if you already have some data");
				} else if(mode==ViewMode.BATCH && getBiosampleFromScannerMode().size()>1) {
					throw new Exception("You cannot change the type if you already have some data");
				} else {
					setBiotype(biotypeComboBox.getSelection());
				}
			} catch(Exception ex) {
				biotypeComboBox.setSelection(biotype);
				JExceptionDialog.showError(ex);
			}
		});

		changeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Biotype biotype = biosample==null? null: biosample.getBiotype();
				BiosampleFinder finder = new BiosampleFinder(dlg, "Select a parent biosample", null, biotype, null, biosample, false) {
					@Override
					public void onSelect(Biosample sel) {
						dispose();
						dlg.changeBiosample(BiosampleFormPanel.this, sel);
					}
				};
				finder.setVisible(true);
			}
		});
		createButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Biotype biotype = biosample==null? null: biosample.getBiotype();
				dlg.changeBiosample(BiosampleFormPanel.this, new Biosample(biotype));
			}
		});

		createOneButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setViewMode(ViewMode.FORM);
			}
		});
		createBatchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setViewMode(ViewMode.BATCH);
				scannerPanel.setBiotype(biotype);
			}
		});

		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dlg.eventRemove(BiosampleFormPanel.this);
			}
		});

		setViewMode(ViewMode.FORM);
	}

	public Biotype getBiotype() {
		return biotypeComboBox.getSelection();
	}

	public void setBiotype(Biotype biotype) {
		this.biotype = biotype;
		biotypeComboBox.setSelection(biotype);

		setViewMode(ViewMode.FORM);
		scannerPanel.setBiotype(biotype);

		biosample = new Biosample(biotype);
		initFormUI();
	}

	public void setEditMode(EditMode editMode) {
		this.editMode = editMode;
	}

	public void updateButtons() {
		BiosampleFormPanel previous = dlg.getPrevious(this);
		BiosampleFormPanel next = dlg.getNext(this);

		biotypeComboBox.setEnabled(editMode==EditMode.NEW);
		changeButton.setVisible(!highlightBiosample && editMode!=EditMode.READ && next!=null);
		createButton.setVisible(!highlightBiosample && editMode!=EditMode.READ && next!=null);
		createOneButton.setVisible(biotype!=null && !biotype.isAbstract() && !biotype.isHideContainer() && editMode==EditMode.NEW && next==null);
		createBatchButton.setVisible(biotype!=null && !biotype.isAbstract() && !biotype.isHideContainer() && editMode==EditMode.NEW && next==null);
		removeButton.setVisible(!highlightBiosample && ((previous==null && next!=null) || (previous!=null && next==null)));
		formPanel.setEditable(editMode!=EditMode.READ);
	}

	public void setViewMode(ViewMode mode) {
		cardPanel.removeAll();
		this.mode = mode;
		if(mode==ViewMode.FORM) {
			createOneButton.setSelected(true);
			cardPanel.add(formPanel);
		} else if(mode==ViewMode.BATCH) {
			createBatchButton.setSelected(true);
			cardPanel.add(scannerPanel);
		}
		dlg.updateButtons();
		dlg.validate();
		dlg.repaint();
	}

	public ViewMode getViewMode() {
		return mode;
	}

	public EditMode getEditMode() {
		return editMode;
	}

	public void setBiosample(Biosample b) {
		try {
			push++;
			setBiotype(b==null? null: b.getBiotype());
			this.biosample = b;
			setViewMode(ViewMode.FORM);
			setEditMode(biosample.getId()>0? (SpiritRights.canEdit(b, SpiritFrame.getUser())? EditMode.EDIT:  EditMode.READ): EditMode.NEW);
			initFormUI();
			//			updateComponent();
		} finally {
			push--;
		}
	}

	private void initFormUI() {
		formPanel.setBiosample(biosample);
	}

	public Biosample getBiosampleFromFormMode() {
		if(biosample==null) return null;

		//Update Model
		formPanel.updateModel();
		return biosample;
	}

	public List<Biosample> getBiosampleFromScannerMode() throws Exception {
		return scannerPanel.getBiosamples();
	}

	public void setHighlightBiosample(boolean highlightBiosample) {
		this.highlightBiosample = highlightBiosample;
	}
	public boolean isHighlightBiosample() {
		return highlightBiosample;
	}

	@Override
	public String toString() {
		return "[BiosampleFormPanel " + hashCode()+"]";
	}

}
