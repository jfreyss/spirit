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

package com.actelion.research.spiritapp.spirit.ui.biosample.edit;

import java.awt.BorderLayout;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.form.BiosampleFormDlg;
import com.actelion.research.spiritapp.spirit.ui.util.lf.BiotypeComboBox;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class EditBiosamplePanel extends JPanel {

	private int push = 0;
	private final EditBiosampleDlg dlg;
	private final EditBiosampleTable table = new EditBiosampleTable();

	//Top
	private final JButton formModeButton = new JButton("Switch to Form Mode");
	private JPanel formModeBox = UIUtils.createHorizontalBox(formModeButton, Box.createHorizontalGlue());
	private final BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());

	//Scan
	private JButton setLocationButton = new JButton(new SetLocationAction(table));

	/**
	 * Standard constructor
	 * @param biosampleBatchEditDlg
	 */
	public EditBiosamplePanel(EditBiosampleDlg biosampleBatchEditDlg) {
		this.dlg = biosampleBatchEditDlg;

		setBorder(BorderFactory.createEtchedBorder());
		setLayout(new BorderLayout());

		add(BorderLayout.NORTH,
				UIUtils.createVerticalBox(
						formModeBox,
						UIUtils.createTitleBox("Biotype", UIUtils.createHorizontalBox(new JLabel("Biotype: "), biotypeComboBox, Box.createHorizontalGlue(), setLocationButton))));
		add(BorderLayout.CENTER, new JScrollPane(table));

		biotypeComboBox.setMemorization(true);
		biotypeComboBox.addTextChangeListener(e-> {
			eventBiotypeChanged();
		});

		formModeButton.addActionListener(e-> {
			Biotype biotype = biotypeComboBox.getSelection();
			if(biotype==null) return;

			if(table.getRows().size()==0) {
				dlg.dispose();
				Biosample b = new Biosample(biotype);
				new BiosampleFormDlg(b);
			} else if(table.getRows().size()==1) {
				dlg.dispose();
				new BiosampleFormDlg(table.getRows().get(0));
			} else {
				JExceptionDialog.showError(EditBiosamplePanel.this, "You cannot switch to form mode if you have more than one biosample");
			}
		});
		table.getModel().addTableModelListener(e-> {
			formModeBox.setVisible(table.getRows().size()<=1);
		});
	}

	public EditBiosampleTable getTable() {
		return table;
	}


	private void eventBiotypeChanged() {
		if(push>0) return;
		push++;
		try {

			//Test if the list has metadata
			boolean hasData = false;
			for(Biosample b: table.getBiosamples()) {
				if(b==null || b.getBiotype()==null) continue;
				for(BiotypeMetadata m : b.getBiotype().getMetadata()) {
					String s = b.getMetadataValue(m);
					if(s!=null && s.length()>0) hasData = true;
				}
			}

			Biotype biotype = biotypeComboBox.getSelection();
			if(biotype!=null) {
				if(hasData) {
					if(!biotype.equals(table.getType())) {
						//Raise an exception if the biotype was changed
						JOptionPane.showMessageDialog(dlg, "You cannot change the type once you have entered some metadata", "Error", JOptionPane.ERROR_MESSAGE);
						biotypeComboBox.setSelection(table.getType());
					}
				} else {
					//Change the type
					List<Biosample> biosamples = table.getBiosamples();
					for (Biosample b : biosamples) {
						b.setBiotype(biotype);
					}
					Spirit.getConfig().setProperty("biosample.type", biotype==null?"": biotype.getName());
					table.setRows(biotype, biosamples);
				}
			} else {
				List<Biosample> biosamples = table.getBiosamples();
				for (Biosample b : biosamples) {
					if(b.getBiotype()==null) {
						biotypeComboBox.setSelection(table.getType());
						return;
					}
				}
			}

			setLocationButton.setVisible(biotype!=null && !biotype.isAbstract() && biotype.getCategory()!=BiotypeCategory.LIVING);

		} catch (Exception e) {
			JExceptionDialog.showError(dlg, e);
		} finally {
			push--;
		}
	}



	public void setRows(List<Biosample> biosamples) throws Exception {


		Set<Biotype> biotypes = Biosample.getBiotypes(biosamples);
		Biotype biotype = biotypes.size()==1? biotypes.iterator().next(): null;
		table.setRows(biotype, biosamples);
		push++;
		try {
			biotypeComboBox.setSelection(biotype);
			biotypeComboBox.setEnabled(biotypes.size()<=1);
		} finally {
			push--;
		}
		eventBiotypeChanged();
	}

	public Biotype getBiotype() {
		return biotypeComboBox.getSelection();
	}

	public void setForcedBiotype(Biotype biotype) {
		biotypeComboBox.setSelection(biotype);
		biotypeComboBox.setEnabled(biotype==null);
	}

}