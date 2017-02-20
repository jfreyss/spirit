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

package com.actelion.research.spiritapp.bioviewer.ui.batchassign;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.bioviewer.ui.batchassign.BatchAssignDlg.RackPosTubeId;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleOrRackTab;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.DefaultRackDepictorRenderer;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.RackDepictor;
import com.actelion.research.spiritapp.spirit.ui.scanner.ScanRackAction;
import com.actelion.research.spiritapp.spirit.ui.scanner.ScanRackForDepictorAction;
import com.actelion.research.spiritapp.spirit.ui.scanner.SelectRackAction;
import com.actelion.research.spiritapp.spirit.ui.scanner.SpiritScanner;
import com.actelion.research.spiritapp.spirit.ui.scanner.SpiritScanner.Verification;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoFormat;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.location.Direction;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.scanner.ScannerConfiguration;
import com.itextpdf.text.Font;

class BatchAssignRackPanel extends JPanel {

	private BatchAssignDlg dlg;
	private int rackNo;
	
	private SpiritScanner model = new SpiritScanner();
	private BiosampleOrRackTab rackTab = new BiosampleOrRackTab();
	private JLabel sharedInfoLabel = new JCustomLabel("", FastFont.REGULAR);
	private JLabel rackIdLabel = new JLabel();
	private Location rack;
	
	public BatchAssignRackPanel(final BatchAssignDlg dlg, final int rackNo) {
		super(new BorderLayout());
		this.dlg = dlg;
		this.rackNo = rackNo;
		
		
		
		//TODO
		rackTab.getRackDepictor().setRackDepictorRenderer(new DefaultRackDepictorRenderer() {
			@Override
			public void paintWell(RackDepictor depictor, Graphics2D g, Location location, int pos, Container c, Rectangle r) {
				Biosample b = dlg.getBiosample(rackNo, location.getRow(pos), location.getCol(pos));
				Color fgColor = b!=null && (dlg.getError(b)!=null)? Color.RED: Color.GRAY;
				
				int h = Math.min(r.width, r.height)-4;
				if(c!=null) {
					g.setColor(fgColor);
					g.fillOval(r.x+r.width/2-h/2-2, r.y+r.height/2-h/2, h, h);
					
					g.setColor(Color.BLACK);
					g.drawOval(r.x+r.width/2-h/2-2, r.y+r.height/2-h/2, h, h);
				}
				if(b!=null) {
					g.setFont(FastFont.BOLD.deriveSize(Math.max(5, r.height/2)));
					g.setColor(BatchAssignDlg.getForeground(rackNo));
					int index = dlg.getBiosamples().indexOf(b);
					String s = "" + (index+1);
					g.drawString(s, r.x + (r.width - g.getFontMetrics().stringWidth(s))/2, r.y + r.height*2/3 );
				}
				
			}
		});
		/*
		rackTab.getRackDepictor().setToolTipSelecter(new PlateDepictor.ToolTipSelecter() {			
			@Override
			public String getToolTip(RackPos value, Plate plate, int row, int col) {
				return dlg.getError(dlg.getBiosample(rackNo, row, col));
			}
		});
		*/
		
		
		rackTab.addPropertyChangeListener(BiosampleOrRackTab.PROPERTY_SELECTION, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {				
				dlg.setTableSelection(rackTab.getSelection(Biosample.class));				
			}
		});
		

		final SelectRackAction setRackAction = new SelectRackAction(model) {			
			@Override
			protected void eventRackSelected(Location sel) throws Exception {
				rack = sel;
				refreshData();
			}
		};

		final ScanRackAction scanRackAction = new ScanRackForDepictorAction(model, rackTab, Verification.EMPTY_CONTAINERS, false) {
			@Override
			public ScannerConfiguration getScannerConfiguration() {
				//We must ask the user, so return null
				return null;
			}
			
			@Override
			public void postScan(Location sel) throws Exception {
				rack = sel;
				dlg.assignPositions();
			}
			
		};		
		
		JButton clearButton = new JIconButton(IconType.CLEAR, "");		
		clearButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				clear();
				dlg.assignPositions();
			}
		}); 
		
		
		add(BorderLayout.NORTH, UIUtils.createHorizontalBox(new JCustomLabel("Rack "+(rackNo+1), Font.BOLD, 16f), Box.createHorizontalGlue(), new JButton(scanRackAction), clearButton));
		add(BorderLayout.CENTER, rackTab);
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(new JButton(setRackAction), rackIdLabel, Box.createHorizontalGlue(), sharedInfoLabel));
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK), BorderFactory.createEmptyBorder(2, 2, 2, 2)));
		setPreferredSize(new Dimension(400,380));
		clear();	

	}
	
	public List<RackPosTubeId> getOrderedPositions(Direction dir) {

		Location rack = rackTab.getRackDepictor().getBioLocation();
		List<RackPosTubeId> res = new ArrayList<>();
		if(rack!=null) {
			Map<Integer, Container> map = rack.getContainersMap();
			if(dir==Direction.TOP_BOTTOM) {
				for (int x = 0; x < rack.getCols(); x++) {
					for (int y = 0; y < rack.getRows(); y++) {
						Container c = map.get(rack.getLabeling().getPos(rack, y, x));
						if(c!=null) res.add(new RackPosTubeId(rackNo, rack.formatPosition(y, x), c.getContainerId()));					
					}				
				}	
			} else if(dir==Direction.LEFT_RIGHT) {
				for (int y = 0; y < rack.getRows(); y++) {
					for (int x = 0; x < rack.getCols(); x++) {
						Container c = map.get(rack.getLabeling().getPos(rack, y, x));
						if(c!=null) res.add(new RackPosTubeId(rackNo, rack.formatPosition(y, x), c.getContainerId()));					
					}	
				} 			
			}
		}
		System.out.println("BatchAssignRackPanel.getOrderedPositions() "+res);
		return res;
	}
	
	public void clear() {
		rackTab.setRack(null);
	}
	
	public void setSelection(List<Biosample> biosamples) {
		rackTab.setSelectedBiosamples(biosamples);
	}
	
	public void setSelectedRackPos(Collection<Integer> poses) {
		rackTab.getRackDepictor().setSelectedPoses(poses);
	}

	
	public ContainerType getContainerType() {
		if(model.getScannerConfiguration()==null) return null;
		return ContainerType.get(model.getScannerConfiguration().getDefaultTubeType());
	}
	
	public void refreshData() {		
		//Read metadata of samples that should be moved
		List<Biosample> biosamples = dlg.getBiosamples(rackNo);
		EnumSet<InfoFormat> en = EnumSet.allOf(InfoFormat.class);
		en.remove(InfoFormat.LOCATION);
		
		String s = Biosample.getInfos(biosamples, en, InfoSize.COMPACT);
		if(s.length()>40) s = s.substring(0,40);
		sharedInfoLabel.setText(s);
		
		//Read location where samples will be moved
		rackIdLabel.setText(rack==null?"N/A": rack.getHierarchyFull());
	}

	public String getRackLabel() {
		return sharedInfoLabel.getText();
	}
	
	public Location getBiolocation() {
		return rack;
	}
	
}
