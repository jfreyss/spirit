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

package com.actelion.research.spiritapp.ui.biosample.form;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.spiritapp.ui.biosample.edit.EditBiosampleTable;
import com.actelion.research.spiritapp.ui.biosample.edit.SetLocationAction;
import com.actelion.research.spiritapp.ui.util.scanner.ScanRackForTableAction;
import com.actelion.research.spiritapp.ui.util.scanner.SelectRackAction;
import com.actelion.research.spiritapp.ui.util.scanner.SpiritScanner;
import com.actelion.research.spiritapp.ui.util.scanner.SpiritScanner.Verification;
import com.actelion.research.spiritcore.business.biosample.BarcodeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class BiosampleBatchPanel extends JPanel {

	private Biosample parent;
	private Biotype biotype;
	
	private final EditBiosampleTable table = new EditBiosampleTable();
	private MetadataFormPanel formPanel;
	
	private JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 0, 999, 1));
	private JButton setLocationButton = new JButton(new SetLocationAction(table));
	private SpiritScanner model = new SpiritScanner(Verification.EMPTY_CONTAINERS);
	private JButton scanButton = new JButton(new ScanRackForTableAction(model, table) {
		
		@Override
		public Location scan() throws Exception {
			if(!Biosample.isEmpty(table.getRows())) {
				int res = JOptionPane.showConfirmDialog(BiosampleBatchPanel.this, "Do you want to clear the table?", "Scan", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
				if(res==JOptionPane.YES_OPTION) {
					table.clear();
				}
			}
			
			return super.scan();
		}
		
		@Override
		public void postScan(Location rack) throws Exception {
			setLocationButton.setEnabled(table.getRows().size()>0);
			for(Biosample b: table.getRows()) {
				b.setParent(b);
				formPanel.updateModel();				
			}			

			new SelectRackAction(scanner) {				
				@Override
				protected void eventRackSelected(Location rack) throws Exception {
					//Convert the scanned position to the real position
					for (Biosample b : table.getRows()) {
						b.setLocation(rack);
						if(b.getContainer()!=null) b.setPos(rack==null? -1: rack.parsePosition(b.getContainer().getScannedPosition()));
					}
					table.repaint();					
				}
			}.showDlgForRackCreation();
			
			spinner.setValue(table.getRows().size());
		}
	});
	
	/**
	 * 
	 * @param dlg
	 * @param biotype
	 */
	public BiosampleBatchPanel() {
		setLayout(new BorderLayout());
		
		//itemsPanel
		formPanel = new MetadataFormPanel(false, false) {
			@Override
			public void eventTextChanged() {
				try {
					updateSamples();
				} catch(Exception e) {
					JExceptionDialog.showError(e);					
				}
			}
		};
		
		setBiotype(null);
		initUI();
	}
	
	private void initUI() {
		//table
		try {
			table.getModel().setCompactView(true);
			table.setRows(biotype, null);
		} catch(Exception e) {
			JExceptionDialog.showError(e);
		}		
		table.setCanAddRow(false);

		JPanel tablePanel = new JPanel(new BorderLayout());
		tablePanel.setOpaque(false);
		tablePanel.add(BorderLayout.CENTER, new JScrollPane(table));
		tablePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		//actionPanel
		setLocationButton.setEnabled(false);
		JPanel actionPanel = UIUtils.createHorizontalBox(
				Box.createHorizontalGlue(),
				UIUtils.createTitleBoxSmall(null, 
						UIUtils.createVerticalBox(
							UIUtils.createHorizontalBox(new JLabel("Number: "), spinner),
							setLocationButton,
							Box.createVerticalGlue())),
				Box.createHorizontalStrut(2),
				new JLabel(" or "),
				Box.createHorizontalStrut(2),
				UIUtils.createTitleBoxSmall(null, 
						UIUtils.createVerticalBox(
							new JCustomLabel("Scan", FastFont.BOLD),
							Box.createVerticalGlue(),
							scanButton						
						))
		);
		scanButton.setEnabled(biotype!=null && !biotype.isAbstract());
		
		removeAll();
		add(BorderLayout.WEST, UIUtils.createVerticalBox(BorderFactory.createEmptyBorder(5, 5, 5, 5), 
					formPanel, 
					Box.createVerticalStrut(10),
					actionPanel,
					Box.createVerticalGlue()));
		add(BorderLayout.CENTER, tablePanel);
		validate();
		
		//actions
		spinner.addChangeListener(new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				try {
					updateSamples();
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});

	}
	
	public void setBiotype(Biotype biotype) {
		this.biotype = biotype;
		formPanel.setBiosample(new Biosample(biotype));
		initUI();
		try {
			updateSamples();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void updateSamples() throws Exception {
		List<Biosample> res = new ArrayList<>(table.getRows());
		int n = (Integer) spinner.getValue();

		//Add rows if needed
		for (int i=0; i<table.getRows().size(); i++) {
			res.get(i).setBiotype(biotype);
			formPanel.updateModel(res.get(i));
			res.get(i).setParent(parent);
		}
		for (int i=table.getRows().size(); i<n; i++) {
			Biosample b = new Biosample(biotype);
			formPanel.updateModel(b);
			b.setParent(parent);
			res.add(b);
		}
		//Remove rows if needed
		for (int i=res.size()-1; i>=n; i--) {
			res.remove(i);
		}
		
		table.setRows(biotype, res);
		setLocationButton.setEnabled(table.getRows().size()>0);
	}
	

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		formPanel.setEnabled(enabled);
		spinner.setEnabled(enabled);
		table.setEnabled(enabled);
		scanButton.setEnabled(enabled && biotype.getContainerType()!=null && biotype.getContainerType().getBarcodeType()==BarcodeType.MATRIX);
	}
	
	public List<Biosample> getBiosamples() {
		if(!isEnabled()) return new ArrayList<Biosample>();
		List<Biosample> res = table.getBiosamples();
		for (Biosample b : res) {
			b.setParent(parent);
		}
		return res;
	}
}
