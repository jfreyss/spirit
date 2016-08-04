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

package com.actelion.research.spiritapp.bioviewer.ui.batchaliquot;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import com.actelion.research.spiritapp.spirit.ui.icons.ImageFactory;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritapp.spirit.ui.lf.ScannerConfigurationComboBox;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.DefaultRackDepictorRenderer;
import com.actelion.research.spiritapp.spirit.ui.location.depictor.RackDepictor;
import com.actelion.research.spiritapp.spirit.ui.scanner.SpiritScanner;
import com.actelion.research.spiritcore.business.biosample.AmountUnit;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;
import com.actelion.research.util.ui.scanner.RackPos;
import com.itextpdf.text.Font;

class BatchAliquotRackPanel extends JPanel {

	private BatchAliquotDlg dlg;
	private int rackNo;
	
	private JCustomLabel infoLabel = new JCustomLabel("", Font.NORMAL, 12f);
	private RackDepictor plateDepictor = new RackDepictor();
	
//	private BiosampleOrRackTab rackTab = new BiosampleOrRackTab();
	
	private ScannerConfigurationComboBox scannerConfigComboBox = new ScannerConfigurationComboBox(true);
	
	
	private JToggleButton parentButton = new JToggleButton("Parent");
	private JToggleButton childButton = new JToggleButton("Child");
	
	private CardLayout cardLayout = new CardLayout();
	private JPanel cardPanel = new JPanel(cardLayout);

	private Sampling sampling = new Sampling();
	private JLabel sharedInfoLabel = new JLabel();
	private JButton metadataButton = new JButton("Edit Attributes"); 
		
	private JCustomTextField volumeTextField = new JCustomTextField(JCustomTextField.DOUBLE , "");
	private JCustomLabel unitLabel = new JCustomLabel("");
	private JCustomLabel maxLabel = new JCustomLabel(""); 
	
	public BatchAliquotRackPanel(final BatchAliquotDlg dlg, final int rackNo) {
		super(new BorderLayout());
		this.dlg = dlg;
		this.rackNo = rackNo;
		setParent(rackNo==0);

		plateDepictor.setRackDepictorRenderer(new DefaultRackDepictorRenderer() {
			public Color getWellBackground(Location location, int pos, Container c) {
				String error = dlg.getError(rackNo, location.getLabeling().getRow(location, pos), location.getLabeling().getCol(location, pos));
				return error == null ? LF.COLOR_ERROR_BACKGROUND : Color.LIGHT_GRAY;
			}

			@Override
			public void paintWell(RackDepictor depictor, Graphics2D g, Location location, int pos, Container c, Rectangle r) {
				if (c == null)
					return;
				Image img = null;
				if (isParent()) {
					if (c.getBiosamples().size() == 1) {
						Biosample b = c.getBiosamples().iterator().next();
						img = ImageFactory.getImage(b, r.height * 3 / 4);
					}
				} else if (sampling != null && sampling.getBiotype() != null) {
					Biosample b = sampling.createCompatibleBiosample();
					img = ImageFactory.getImage(b, r.height * 3 / 4);
				}

				if (img != null)
					g.drawImage(img, r.x + r.height / 8, r.y + r.height / 2 - img.getHeight(dlg) / 2, dlg);
			}
		});
		
		
		//CardPanel
		cardPanel.add("child", UIUtils.createBox(new JScrollPane(sharedInfoLabel), null, null, metadataButton, null));
		cardPanel.add("parent", UIUtils.createHorizontalBox(new JLabel("Remove: "), volumeTextField, unitLabel, Box.createHorizontalStrut(15), maxLabel, Box.createHorizontalGlue()));
		cardPanel.setBorder(BorderFactory.createLoweredBevelBorder());
		parentButton.setFont(FastFont.BOLD);
		childButton.setFont(FastFont.BOLD);
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(childButton);
		buttonGroup.add(parentButton);
		parentButton.setSelected(isParent());
		childButton.setSelected(!isParent());
		childButton.setEnabled(rackNo==1);
		parentButton.setEnabled(rackNo==1);
		childButton.setBorder(BorderFactory.createEmptyBorder(10,6,10,6));
		parentButton.setBorder(BorderFactory.createEmptyBorder(10,6,10,6));
		
		
		childButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {				
				dlg.refresh();
			}
		});
		parentButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				dlg.refresh();
			}
		});
		
		
		JButton scanButton = new JButton("Scan");
		scanButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Location rack = new SpiritScanner().scan(scannerConfigComboBox.getSelection(), false, null);
					if(rack==null) return;
					
					setRack(rack);
					dlg.refresh();
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}
				
			}
		}); 
		JButton clearButton = new JIconButton(IconType.CLEAR, "");
		clearButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				clear();
				dlg.refresh();
			}
		}); 
		
		metadataButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				editAttributes();
				refresh();
			}
		});
		
		//SouthPanel
		cardPanel.setBackground(Color.WHITE);
		JPanel southPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c  = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0; c.gridx = 1; c.gridy=1; southPanel.add(parentButton, c);
		c.weightx = 0; c.gridx = 2; c.gridy=1; southPanel.add(childButton, c);		
		c.weightx = 1; c.gridx = 3; c.gridy=1; southPanel.add(cardPanel, c);
		
		add(BorderLayout.NORTH, UIUtils.createHorizontalBox(new JCustomLabel("Rack "+(rackNo+1), Font.BOLD, 16f), infoLabel, Box.createHorizontalGlue(), 
				Box.createHorizontalStrut(5),scannerConfigComboBox, scanButton, clearButton));
		add(BorderLayout.CENTER, plateDepictor);
		add(BorderLayout.SOUTH, southPanel);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK), BorderFactory.createEmptyBorder(2, 2, 2, 2)));
		
		clear();

	}
	
	public boolean isEmpty() {
		return plateDepictor.getBioLocation()==null || plateDepictor.getBioLocation().isEmpty();
	}
	public Location getPlate() {
		return plateDepictor.getBioLocation();
	}
	
	protected void editAttributes() {
		ContainerType containerType = getContainerType();
		sampling.setContainerType(containerType);
		
		if(sampling.getBiotype()==null) {
			BatchAliquotRackPanel rackParent = dlg.getRackPanels()[getRackParent()];
			List<Biosample> biosamples = Container.getBiosamples(rackParent.getContainers());
			Set<Biotype> biotypes = Biosample.getBiotypes(biosamples);
			if(biotypes.size()==1) {
				Biotype biotype = biotypes.iterator().next();
				sampling.setBiotype(biotype);
				for(BiotypeMetadata bm: biotype.getMetadata()) {
					Set<String> values = Biosample.getMetadata(bm.getName(), biosamples);
					System.out.println("RackPanel.editAttributes() "+bm.getName()+" > "+values);
					if(values.size()==1) {
						sampling.setMetadata(bm, values.iterator().next());
					}					
				}
			}
			
		}
		
		EditAttributesDlg dlg = new EditAttributesDlg(sampling);
		if(dlg.isSuccess()) {
//			containerTypeComboBox.setSelection(sampling.getContainerType());
			refreshSharedInfoLabel();
		}
	}
	
	protected void refreshSharedInfoLabel() {
		try {
			List<Biosample> biosamples = createCompatibleAliquots();
			String studyInfos = Biosample.getInfosStudy(biosamples);
			String metaInfos = Biosample.getInfosMetadata(biosamples);			
			sharedInfoLabel.setText("<html>"+studyInfos+"<br>"+metaInfos+"</html>");
		} catch(Exception e) {
			e.printStackTrace();
			sharedInfoLabel.setText("<html><span style='color:red'>"+e.getMessage()+"</span></html>");
		}
		

	}
	
	public boolean isParent() {
		return parentButton.isSelected();
	}
	public void setParent(boolean b) {
		if(b) {
			parentButton.setSelected(true);
		} else {
			childButton.setSelected(true);
		}
	}
	
	public int getRackParent() {
		if(rackNo==0 || rackNo==2) {
			return 0;
		} else if(rackNo==1 || rackNo==3) {
			boolean b = dlg.getRackPanels()[1].isParent();
			return b? 1: 0;
		}		
		return -1;
	}
	
	public void refresh() {
		
		if(isParent()) {
			cardLayout.show(cardPanel, "parent");
		} else {
			cardLayout.show(cardPanel, "child");
		}
		
		if(rackNo==0) {
			//Nothing
		} else if(rackNo==1) {
			boolean b = dlg.getRackPanels()[1].isParent();
			infoLabel.setText(b? "": " - from Rack 1");
		} else if(rackNo==2) {
			infoLabel.setText(" - from Rack 1");
		} else if(rackNo==3) {
			boolean b = dlg.getRackPanels()[1].isParent();
			infoLabel.setText(b? " - from Rack 2": " - from Rack 1");
		}
		
		
		parentButton.setForeground(!isEnabled()? Color.LIGHT_GRAY: isParent()? Color.BLUE: Color.BLACK);
		childButton.setForeground(!isEnabled()? Color.LIGHT_GRAY: isParent()? Color.BLACK: Color.BLUE);
		parentButton.setBackground( !isEnabled()? getBackground(): new Color(200,220,240));
		childButton.setBackground( !isEnabled()? getBackground(): new Color(200,225,255));
		
		metadataButton.setEnabled(!isParent());
		
		
		if(isParent()) {
			//Unit and Max volume
			boolean enableVolume = getContainers().size()>0;
			AmountUnit unit = null;
			double maxVolume = 1000000;
			for (Container c : getContainers()) {
				if(c.getBiosamples().size()==1) {
					Biosample b = c.getBiosamples().iterator().next();
					if(b.getBiotype()==null || b.getBiotype().getAmountUnit()==null) {
						enableVolume = false;
						break;
					} else {
						if(unit==null || unit==b.getBiotype().getAmountUnit()) {
							unit = b.getBiotype().getAmountUnit();
							maxVolume = Math.min(maxVolume, (b.getAmount()==null?0: b.getAmount()));
						} else {
							enableVolume = false;
							break;
						}
					}
				} else {
					enableVolume = false;
				}
			}
			
			if(enableVolume) {
				unitLabel.setText(unit==null?"":unit.getUnit());
				maxLabel.setText(" (Max. " + maxVolume + " "+(unit==null?"":unit.getUnit())+")");
				volumeTextField.setEnabled(true);
			} else {
				unitLabel.setText("");
				maxLabel.setText("");
				volumeTextField.setEnabled(false);
				
			}
			
			
			
		} else {//Child				
		}
		
		refreshSharedInfoLabel();
		repaint();
	}
	
	
	public void setRack(Location rack) {
		plateDepictor.setBiolocation(rack);
		dlg.refresh();
		
	}
	public Container getContainer(int row, int col) {
		return plateDepictor.getBioLocation().getContainersMap().get(plateDepictor.getBioLocation().getLabeling().getPos(plateDepictor.getBioLocation(), row, col));
	}	
	public Collection<Container> getContainers() {
		return plateDepictor.getBioLocation()==null? new ArrayList<Container>(): plateDepictor.getBioLocation().getContainers();
	}	

	public void clear() {
		setRack(null);
	}
	
	public String getRackLabel() {
		return MiscUtils.removeHtml(sharedInfoLabel.getText().replaceAll("<br>", "\n"));
	}
	
	public Sampling getSampling() {
		return sampling;
	}
	
	public ContainerType getContainerType() {
		return  ContainerType.get(scannerConfigComboBox.getSelection().getDefaultTubeType()); 
	}
	
	public Double getVolume() {
		return volumeTextField.getTextDouble();
	}
	
	protected List<Biosample> createCompatibleAliquots() throws Exception {		
		List<Biosample> res = new ArrayList<Biosample>();
		if(!isParent() && getPlate()!=null) {
			int rackParent = getRackParent();
			for (int y = 0; y < getPlate().getRows(); y++) {
				for (int x = 0; x < getPlate().getCols(); x++) {
					Container c = getContainer(y, x);
					
					if(c==null) continue;
					
					Container cParent = dlg.getContainer(rackParent, y, x);
					if(cParent==null) throw new Exception("The Container at Rack"+(rackParent+1)+": "+LocationLabeling.ALPHA.formatPosition(getPlate(), y, x)+" does not have a parent");
					if(cParent.getBiosamples().size()==0) throw new Exception("The Container at Rack"+(rackParent+1)+": "+RackPos.getPosition(y, x)+" is empty");
										
					//Work on a new container (to avoid touching the original one)
					Container clonedContainer = new Container(c.getContainerId());
					
					c.getBiosamples().clear();
					
					Biosample b = sampling.createCompatibleBiosample();
					b.setInheritedStudy(cParent.getStudy());
					b.setInheritedGroup(cParent.getFirstGroup());
					b.setInheritedPhase(cParent.getPhase());
					b.setParent(cParent.getBiosamples().iterator().next());
					b.setContainer(clonedContainer);
					b.setContainerType(c.getContainerType());
					
					res.add(b);
				}
			}
		}
		return res;
	}
}
