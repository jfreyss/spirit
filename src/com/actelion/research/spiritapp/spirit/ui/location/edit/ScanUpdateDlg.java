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

package com.actelion.research.spiritapp.spirit.ui.location.edit;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleOrRackTab;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritapp.spirit.ui.util.scanner.ScanRackForBiosampleOrRackTabAction;
import com.actelion.research.spiritapp.spirit.ui.util.scanner.SpiritScanner;
import com.actelion.research.spiritapp.spirit.ui.util.scanner.SpiritScanner.Verification;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.scanner.ScannerConfiguration;

/**
 * Not used for the moment
 * @author freyssj
 *
 */
@Deprecated
public class ScanUpdateDlg extends JSpiritEscapeDialog {
	
	private final Location rack;
	private SpiritScanner scanner = new SpiritScanner();
	private BiosampleOrRackTab originalRackTab = new BiosampleOrRackTab();
	private BiosampleOrRackTab currentRackTab = new BiosampleOrRackTab();
	
	
	public ScanUpdateDlg(Location rackLocation) {
		super(UIUtils.getMainFrame(), "Scan & Update", ScanUpdateDlg.class.getName());
		assert rackLocation!=null;
		
		//Reload the rack in its current context
		this.rack = JPAUtil.reattach(rackLocation);

		//Buttons
		JButton scanButton = new JButton(new ScanRackForBiosampleOrRackTabAction(scanner, currentRackTab) {			
			@Override
			public ScannerConfiguration getScannerConfiguration() {
				//Find compatible ScannerConfiguratoin
				Set<ContainerType> types = Biosample.getContainerTypes(rack.getBiosamples());
				if(types.size()!=1) return null;
				ContainerType type = types.iterator().next();
				List<ScannerConfiguration> res = new ArrayList<>();
				for (ScannerConfiguration c : ScannerConfiguration.valuesForBiosamples()) {
					if(c.getCols()!=rack.getCols()) continue;
					if(c.getRows()!=rack.getRows()) continue;
					if(!type.getName().equals(c.getDefaultTubeType())) continue;					
					
					res.add(c);
				}
				
				return res.size()==1? res.get(0): null;
			}
		});
	
//TODO uncomment
//		//Depictor
//		currentRackTab.getRackDepictor().setBackgroundColorSelecter(new SpiritPlateDepictor.IColorSelecter() {			
//			@Override
//			public String getName() {
//				return "Unmatched";
//			}
//			
//			@Override
//			public Color getColor(RackPos value, Plate plate, int row, int col) {
//				if(currentRackTab.getRack()==null || originalRackTab.getRack()==null) return null;
//				
//				Container c1 = originalRackTab.getRack().getScannedContainer(row, col);
//				Container c2 = currentRackTab.getRack().getScannedContainer(row, col);
//				if(CompareUtils.compare(c1, c2)!=0) return Color.RED;
//				if(c2!=null) return Color.GREEN;
//				return null;
//			}
//		});
		

		//CenterPanel
		JPanel centerPanel = new JPanel(new GridLayout(1, 2));		
		centerPanel.add(UIUtils.createBox(BorderFactory.createTitledBorder("Before Update"), originalRackTab, UIUtils.createHorizontalBox(), null, null, null));
		centerPanel.add(UIUtils.createBox(BorderFactory.createTitledBorder("After Update"), currentRackTab, UIUtils.createHorizontalBox(Box.createHorizontalGlue()), null, null, null));
		
		
		//ContentPanel
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.CENTER, centerPanel);
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(),scanButton));
		setContentPane(contentPanel);
		
		//Init
		originalRackTab.setRack(rack);
		
		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}
	
}
