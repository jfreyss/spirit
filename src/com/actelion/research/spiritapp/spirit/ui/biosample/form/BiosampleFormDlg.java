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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.edit.EditBiosampleDlg;
import com.actelion.research.spiritapp.spirit.ui.biosample.form.BiosampleFormPanel.EditMode;
import com.actelion.research.spiritapp.spirit.ui.biosample.form.BiosampleFormPanel.ViewMode;
import com.actelion.research.spiritapp.spirit.ui.util.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.services.dao.DAOBarcode;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class BiosampleFormDlg extends JSpiritEscapeDialog {

	private LinkedList<BiosampleFormPanel> panels = new LinkedList<>();
	private Biosample toEdit;
	private JPanel centerPane = new JPanel(new GridLayout());
	private JScrollPane scrollPane = new JScrollPane(centerPane);

	//	private JButton selectParentButton = new JButton("Select Existing Parent");
	private JButton createParentButton = new JButton("Add Parent");
	private JButton childButton = new JButton("Add Child");

	private JButton saveButton = new JIconButton(IconType.SAVE, "Save");

	/**
	 * Generic construction
	 * @param title
	 * @param biotypes
	 * @param parentSelectionForCreation
	 * @param toEdit
	 */
	public BiosampleFormDlg(Biosample mySample) {
		super(UIUtils.getMainFrame(), "Biosample Form Editor", BiosampleFormDlg.class.getName());


		//Make sure to reattach the sample
		this.toEdit = JPAUtil.reattach(mySample);
		assert toEdit!=null && toEdit.getBiotype()!=null;

		//init actions
		createParentButton.addActionListener(e-> {
			Biotype biotype = panels.size()==0 || panels.get(0).getBiotype()==null? null: panels.get(0).getBiotype().getParent();
			BiosampleFormPanel singlePanel = new BiosampleFormPanel(BiosampleFormDlg.this);
			panels.addFirst(singlePanel);
			singlePanel.setBiosample(new Biosample(biotype));
			initUI();
		});

		childButton.addActionListener(e-> {
			Biotype biotype = panels.size()==0 || panels.get(panels.size()-1).getBiotype()==null || panels.get(panels.size()-1).getBiotype().getChildren().size()==0? null: panels.get(panels.size()-1).getBiotype().getChildren().iterator().next();
			BiosampleFormPanel singlePanel = new BiosampleFormPanel(BiosampleFormDlg.this);
			panels.addLast(singlePanel);
			singlePanel.setBiosample(new Biosample(biotype));
			initUI();
			scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
		});



		//Create the single panels going through each parent and init the UI
		changeBiosample(null, toEdit);


		//Batch Mode (only for batch creation)
		JButton batchButton = new JButton("Switch to Batch/Expert Mode");

		//ContentPane
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.NORTH, UIUtils.createHorizontalBox(BorderFactory.createRaisedSoftBevelBorder(), batchButton, Box.createHorizontalGlue()));
		contentPane.add(BorderLayout.CENTER, scrollPane);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), saveButton));
		setContentPane(contentPane);


		//Events:
		batchButton.addActionListener(e-> {
			try {
				Biotype biotype = BiosampleFormDlg.this.toEdit.getBiotype();
				List<Biosample> biosamples = getBiosamplesToSave();
				if(biotype!=null) biosamples = Biosample.filter(biosamples, biotype);
				if(biosamples.size()==0) {
					Biosample b = new Biosample();
					b.setBiotype(biotype);
					biosamples.add(b);
				}
				dispose();

				EditBiosampleDlg dlg = EditBiosampleDlg.createDialogForEditInTransactionMode(biosamples);
				dlg.setVisible(true);

			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});

		saveButton.addActionListener(e-> {
			try {
				eventSave();
			} catch(Exception ex) {
				JExceptionDialog.showError(BiosampleFormDlg.this, ex);
				repaint();
			}
		});


		//setVisible
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		if(this.toEdit!=null && this.toEdit.getBiotype()!=null && this.toEdit.getBiotype().getChildren().isEmpty() && this.toEdit.getBiotype().getParent()==null) {
			pack();
			setSize(Math.min(dim.width-100, 1100), getHeight());
		} else {
			setSize(Math.min(dim.width-100, 1100), Math.min(dim.height-100, 1000));
		}
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}


	private void initUI() {

		boolean refresh = centerPane.getComponentCount()>0;
		if(refresh) centerPane.removeAll();

		List<Component> comps = new ArrayList<>();

		comps.add(UIUtils.createHorizontalBox(createParentButton, Box.createHorizontalGlue()));
		comps.add(initUIRec(0));
		comps.add(Box.createVerticalGlue());
		centerPane.add(UIUtils.createVerticalBox(comps));

		for (BiosampleFormPanel panel : panels) {
			panel.updateButtons();
		}

		if(refresh) {validate(); repaint();}

	}

	private JComponent initUIRec(int index) {
		if(index>=panels.size()) return UIUtils.createBox(
				Box.createVerticalGlue(),
				UIUtils.createHorizontalBox(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY), childButton, Box.createHorizontalGlue()));

		BiosampleFormPanel panel = panels.get(index);

		Color color = UIUtils.getColor(183,204,223);
		if(panel.isHighlightBiosample()) {
			color = UIUtils.getDilutedColor(getBackground(), color);
		}

		return UIUtils.createTitleBox("",
				UIUtils.createVerticalBox(BorderFactory.createEmptyBorder(3, 10, 3, 10), panel, initUIRec(index+1)),
				FastFont.BIGGER,
				Color.BLACK,
				color,
				10);

	}

	private List<Biosample> getBiosamplesToSave() throws Exception {
		List<Biosample> toSave = new ArrayList<>();
		Biosample parent = null;
		for (int i = 0; i < panels.size(); i++) {
			if(panels.get(i).getViewMode()==ViewMode.FORM) {

				Biosample b = panels.get(i).getBiosampleFromFormMode();
				if(panels.get(i).getEditMode()!=EditMode.READ && b!=null) {
					b.setParent(parent);
					toSave.add(b);
				}
				parent = b;

			} else if(panels.get(i).getViewMode()==ViewMode.BATCH) {
				assert i==panels.size()-1;
				for (Biosample biosample : panels.get(i).getBiosampleFromScannerMode()) {
					biosample.setParent(parent);
					toSave.add(biosample);
				}

			}
		}
		return toSave;
	}

	private void eventSave() throws Exception {

		List<Biosample> toSave = getBiosamplesToSave();
		if(toSave.size()==0) throw new Exception("You didn't not enter anything");

		//Verify linking
		for (Biosample b : toSave) {
			if(b.getBiotype()==null) {
				throw new Exception("The biotype cannot be empty");
			}
			if(b.getParent()!=null && b.getParent().getId()<=0 && !toSave.contains(b.getParent())) {
				throw new Exception("The "+b.getBiotype().getParent()+" cannot be empty");
			}
		}

		//Generate sampleIds
		for (Biosample b : toSave) {
			if(b.getSampleId()==null || b.getSampleId().length()==0) {
				if(b.getBiotype().getPrefix()==null || b.getBiotype().getPrefix().length()==0) throw new Exception("You must enter the SampleId");
				String nextId = DAOBarcode.getNextId(b.getBiotype());
				b.setSampleId(nextId);
			}
		}

		//Validation
		Spirit.askForAuthentication();
		toSave = EditBiosampleDlg.validate(this, toSave, null, false, true);
		if(toSave==null) {
			return;
		}

		//save
		boolean isNew = false;
		for (Biosample b : toSave) {
			if(b.getId()<=0) isNew = true;
		}
		DAOBiosample.persistBiosamples(toSave, Spirit.askForAuthentication());
		dispose();

		//Fire events
		SpiritChangeListener.fireModelChanged(isNew? SpiritChangeType.MODEL_ADDED: SpiritChangeType.MODEL_UPDATED, Biosample.class, toSave);
	}

	public BiosampleFormPanel getNext(BiosampleFormPanel panel) {
		int index = panels.indexOf(panel);
		return index>=0 && index+1<panels.size()? panels.get(index+1): null;
	}

	public BiosampleFormPanel getPrevious(BiosampleFormPanel panel) {
		int index = panels.indexOf(panel);
		return index>0 && index<panels.size()? panels.get(index-1): null;
	}

	public void eventRemove(BiosampleFormPanel panel) {
		panels.remove(panel);
		initUI();
	}

	public void updateButtons() {
		if(panels.isEmpty()) return;
		BiosampleFormPanel panel = panels.getLast();
		childButton.setVisible(panel.getViewMode()==ViewMode.FORM);
	}

	public void changeBiosample(BiosampleFormPanel panel, Biosample biosample) {
		if(panel!=null) {
			int index = panels.indexOf(panel);
			for (int i = 0; i <= index; i++) {
				panels.removeFirst();
			}
		}
		Biosample current = biosample;
		int count = 0;
		while(current!=null && count++<6) {
			BiosampleFormPanel singlePanel = new BiosampleFormPanel(BiosampleFormDlg.this);
			singlePanel.setBiosample(current);
			singlePanel.setHighlightBiosample(current.equals(toEdit));
			panels.addFirst(singlePanel);

			current = current.getParent();
		}

		initUI();

	}

	public Biosample getToEdit() {
		return toEdit;
	}
}
