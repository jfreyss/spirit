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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.print.PrintLabel;
import com.actelion.research.spiritapp.ui.biosample.edit.EditBiosampleDlg;
import com.actelion.research.spiritapp.ui.print.BrotherLabelsDlg;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class BatchAliquotDlg extends JEscapeDialog {

	private BatchAliquotRackPanel[] rackPanels = new BatchAliquotRackPanel[] {
			new BatchAliquotRackPanel(this, 0),
			new BatchAliquotRackPanel(this, 1),
			new BatchAliquotRackPanel(this, 2),
			new BatchAliquotRackPanel(this, 3)};

	public BatchAliquotDlg() {
		super(UIUtils.getMainFrame(), "Batch Aliquot Creation");

		//RackPanels
		JPanel centerPane = new JPanel(new GridLayout(2, 2));
		for (BatchAliquotRackPanel rackPanel : rackPanels) {
			centerPane.add(rackPanel);
		}
		refresh();

		//PRINT Button
		JButton printButton = new JIconButton(IconType.PRINT, "Print Rack Labels");
		printButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					List<PrintLabel> labels = new ArrayList<PrintLabel>();
					for (int i = 0; i < rackPanels.length; i++) {
						String name = rackPanels[i].getRackLabel();
						if(name.length()>0) {
							labels.add(new PrintLabel(name));
						}
					}
					new BrotherLabelsDlg(labels);

				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}

			}
		});

		//SAVE Button
		JButton okButton = new JIconButton(IconType.SAVE, "Create");
		okButton.addActionListener(e-> {
			try {
				eventCreateAliquots();
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});

		//ContentPane
		setContentPane(UIUtils.createBox(centerPane, null, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), printButton, okButton)));


		UIUtils.adaptSize(this, 1400, 900);
	}

	public void refresh() {
		if(rackPanels==null) return;
		for (int i = 0; i < rackPanels.length; i++) {
			rackPanels[i].refresh();
		}
	}

	public String getError(int rackNo, int row, int col) {
		if(rackNo<0 || rackNo>=rackPanels.length) return null;
		BatchAliquotRackPanel rp = rackPanels[rackNo];
		if(rp.isEmpty()) return null;
		if(rp.isParent()) {
			Container c = rp.getContainer(row, col);
			if(c==null) return null;
			if(c.getBiosamples().size()<=0) return "the tube is empty";
			if(c.getBiosamples().size()>1) return "the tube contains more than 1 sample";
		} else {
			BatchAliquotRackPanel rpParent = rackPanels[rp.getRackParent()];
			Container cParent = rpParent.getContainer(row, col);
			Container c = rp.getContainer(row, col);

			if(c==null && cParent==null) return null;
			else if(c==null && cParent!=null) return "a tube is missing";
			else if(c!=null && cParent==null) return "the tube should not be here";
			else if(c!=null && c.getBiosamples().size()>0) return "the tube is already used ("+c.getBlocDescription()+")";

		}
		return null;
	}

	public BatchAliquotRackPanel[] getRackPanels() {
		return rackPanels;
	}

	protected Container getContainer(int rackNo, int row, int col) {
		if(rackNo<0 || rackNo>=rackPanels.length) return null;
		BatchAliquotRackPanel rp = rackPanels[rackNo];
		return rp.getContainer(row, col);

	}

	private void eventCreateAliquots() throws Exception {

		//Generate Samples to be saved
		List<Biosample> aliquotsToSave = new ArrayList<Biosample>();
		List<Biosample> parentsToSave = new ArrayList<Biosample>();

		for (int i = 0; i < rackPanels.length; i++) {
			if(rackPanels[i].isEmpty()) continue;

			//Check errors
			Location rack = rackPanels[i].getPlate();
			for (int y = 0; y < rack.getRows(); y++) {
				for (int x = 0; x < rack.getCols(); x++) {
					String error = getError(i, y, x);
					if(error!=null) throw new Exception("Error: Rack"+(i+1)+": "+LocationLabeling.ALPHA.formatPosition(rack, y, x)+": "+error);
				}
			}

			if(rackPanels[i].isParent()) {
				//parent Rack
				Double vol = rackPanels[i].getVolume();
				if(vol!=null) {
					for(Container tube: rack.getContainers()) {
						if(tube.getAmount()!=null && tube.getAmount().getQuantity()!=null) {
							tube.setAmount(Math.max(0, tube.getAmount().getQuantity()-vol));
							parentsToSave.addAll(tube.getBiosamples());
						}
					}
				}
			} else {
				//Aliquot Rack
				//Check the sampling
				Sampling sampling = rackPanels[i].getSampling();
				if(sampling.getBiotype()==null) throw new Exception("The attributes are not specified for the Rack"+(i+1));

				//Create compatible aliquots
				aliquotsToSave.addAll(rackPanels[i].createCompatibleAliquots());

			}
		}


		EditBiosampleDlg dlg = EditBiosampleDlg.createDialogForEditInTransactionMode(aliquotsToSave);
		dlg.setVisible(true);
		if(dlg.getSaved().size()>0) {
			//			DAOBiosample.persistBiosamples(parentsToSave, Spirit.getUser());
			JExceptionDialog.showInfo(UIUtils.getMainFrame(), dlg.getSaved().size() + " aliquot created and volumes updated");
		}


	}


}
