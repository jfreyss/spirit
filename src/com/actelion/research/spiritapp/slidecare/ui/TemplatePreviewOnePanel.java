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

package com.actelion.research.spiritapp.slidecare.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.slide.ContainerTemplate;
import com.actelion.research.spiritcore.business.slide.SampleDescriptor;
import com.actelion.research.spiritcore.business.slide.ContainerTemplate.Duplicate;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.UIUtils;
/**
 * Class used to preview a containerTemplate
 * @author J
 *
 */
public class TemplatePreviewOnePanel extends JPanel implements DropTargetListener {

	public static final String PROPERTY_UPDATED = "updated";
	
	private final List<JSpinner> copiesSpinners = new ArrayList<>();
	private final List<JTextComboBox> stainingComboboxes = new ArrayList<>();
	private final List<JTextComboBox> sectionNoComboboxes = new ArrayList<>();
	

	private List<OrganSamplePanel> samplingItems = new ArrayList<OrganSamplePanel>();
	private int showLine = -1;
	
	private JTextComboBox blocNoComboBox = new JTextComboBox(false) {
		@Override
		public Collection<String> getChoices() {
			List<String> res = new ArrayList<String>();
			for (int i = 1; i <= 50; i++) {
				res.add(""+i);
			}
			return res;
		}
	};
	

	
	private JPanel dropZone = new JPanel() {
		@Override
		protected void paintComponent(Graphics gr) {
			Graphics2D g = (Graphics2D) gr;

			g.setBackground(showLine>=0?new Color(255,255,245): Color.WHITE);
			g.clearRect(0, 0, getWidth(), getHeight());

			g.setColor(Color.BLACK);
			g.drawRect(0, 0, getWidth()-1, getHeight()-1);
			
			super.paintComponent(g);
			
			int y = 2 + showLine * (OrganSamplePanel.HEIGHT+4);
			g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, 0, 20f, new float[] {4,3}, 1));
			g.drawLine(6, y, getWidth()-6, y);
			g.setStroke(new BasicStroke(1.5f));
			g.drawLine(6, y, 2, y-2);
			g.drawLine(6, y, 2, y+2);
			g.drawLine(getWidth()-6, y, getWidth()-2, y-2);
			g.drawLine(getWidth()-6, y, getWidth()-2, y+2);
			g.setStroke(new BasicStroke());
			
		}
	};
	
	public TemplatePreviewOnePanel(TemplatePreviewPanel templatePreview, ContainerTemplate slide) {
		super(new GridBagLayout());
		
		
		blocNoComboBox.setText(""+slide.getBlocNo());
		blocNoComboBox.setColumns(4);
		
		setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		dropZone.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		dropZone.setPreferredSize(new Dimension(OrganSamplePanel.WIDTH+2, (OrganSamplePanel.HEIGHT+4)*ContainerCreatorDlg.MAX_SAMPLES+34));
		dropZone.setOpaque(false);
		dropZone.setDropTarget(new DropTarget(this, this));
				
		for (SampleDescriptor sample : slide.getSampleDescriptors()) {
			samplingItems.add(new OrganSamplePanel(sample, this));				
		}

		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0; c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;

		
		c.gridy = 0; c.weighty = 0; add(UIUtils.createHorizontalBox(new JCustomLabel(templatePreview.getType2Create().getName() + (templatePreview.getType2Create().getBlocNoPrefix()!=null?" - " + templatePreview.getType2Create().getBlocNoPrefix(): "") + " : ", Font.BOLD), blocNoComboBox, Box.createHorizontalGlue()), c);
		c.gridy = 1; c.weighty = 1; add(dropZone, c);
		
		//create components for number, staining, sectionNo
		if(templatePreview.getType2Create()==ContainerType.SLIDE) {
			List<Duplicate> duplicates = slide.getDuplicates();
			for (int i = 0; i < 4; i++) {
				JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(i==0?1: 0, 0, 50, 1));
				JTextComboBox stainingCombobox = new JTextComboBox(true) {
					@Override
					public Collection<String> getChoices() {
						try {
							Biotype biotype = DAOBiotype.getBiotype("Organ");
							if(biotype==null) throw new Exception("Organ is not a valid biotype");
							BiotypeMetadata mt = biotype.getMetadata("Staining");
							if(mt==null) throw new Exception("Staining is not a valid biotype");
							return DAOBiotype.getAutoCompletionFields(mt, null);
						} catch (Exception e) {
							return new ArrayList<String>();
						}
					}
				};
				JTextComboBox sectionNoCombobox = new JTextComboBox(true) {
					@Override
					public Collection<String> getChoices() {
						List<String> res = new ArrayList<String>();
						for (int j = 1; j < 10; j++) {
							res.add(""+j);
						}
						return res;
					}
				};
				stainingCombobox.setColumns(8);
				stainingCombobox.setTextWhenEmpty("Staining");
				
				sectionNoCombobox.setColumns(7);
				sectionNoCombobox.setTextWhenEmpty("Sect.No");
				
	
				copiesSpinners.add(copiesSpinner);
				stainingComboboxes.add(stainingCombobox);
				sectionNoComboboxes.add(sectionNoCombobox);
			
				copiesSpinner.addChangeListener(new ChangeListener() {			
					@Override
					public void stateChanged(ChangeEvent e) {
						firePropertyChange(PROPERTY_UPDATED, null, this);
					}
				});
				stainingCombobox.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent e) {
						firePropertyChange(PROPERTY_UPDATED, null, this);
					}
				});
				sectionNoCombobox.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent e) {
						firePropertyChange(PROPERTY_UPDATED, null, this);
					}
				});
				
				if(i<duplicates.size()) {
					copiesSpinner.setValue(duplicates.get(i).getNDuplicates());
					stainingCombobox.setText(duplicates.get(i).getStaining());
					sectionNoCombobox.setText(duplicates.get(i).getSectionNo());
				}
				c.gridy = i+2; c.weighty = 0; add(UIUtils.createHorizontalBox(copiesSpinner, new JLabel("x"), stainingCombobox, sectionNoCombobox), c);
			}
		}
		
		
		updateView();

		

		

		

	}
	
	
	
	
	public void updateView() {
		dropZone.removeAll();
		dropZone.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 1, 2, 1);
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;

		for (OrganSamplePanel s : samplingItems) {
			dropZone.add(s, c);
		}
		c.weighty = 1;
		dropZone.add(Box.createGlue(), c);
		
		dropZone.validate();
		dropZone.repaint();
		
		
	}
	

	public void addSampling(int index, OrganSamplePanel item) {
		if(index>=samplingItems.size()) {
			if(samplingItems.size()<=ContainerCreatorDlg.MAX_SAMPLES) {
				samplingItems.add(item);
			}
		} else {
			samplingItems.add(index, item);
		}
		updateView();
	}
	
	public void removeSampling(OrganSamplePanel item) {
		for (Iterator<OrganSamplePanel> iterator = samplingItems.iterator(); iterator.hasNext();) {
			OrganSamplePanel i = iterator.next();
			if(i==item) iterator.remove();
			
		}
		updateView();
		firePropertyChange(PROPERTY_UPDATED, null, this);
	}
	
	private int getIndex(Point pt) {
		if(pt.y<2) return -1;
		return Math.min(samplingItems.size(), (pt.y - 2) / (OrganSamplePanel.HEIGHT+4));
	}
	
	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		
		if(dtde.isDataFlavorSupported(OrganSamplePanel.SAMPLING_FLAVOR)) {
			dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
		} else {
			dtde.rejectDrag();
		}
	}
	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		showLine = getIndex(dtde.getLocation());
		repaint();
		
		if(dtde.isDataFlavorSupported(OrganSamplePanel.SAMPLING_FLAVOR)) {
			dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
		} else {
			dtde.rejectDrag();
		}
		
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
		
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		showLine = -1;
		repaint();		
	}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		
		try {
			
			if(showLine>=0 && dtde.getTransferable().isDataFlavorSupported(OrganSamplePanel.SAMPLING_FLAVOR)) {
	
				Object obj = dtde.getTransferable().getTransferData(OrganSamplePanel.SAMPLING_FLAVOR);
				OrganSamplePanel s = (OrganSamplePanel) obj;
				
				
				//Find the position
				dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

				OrganSamplePanel ns = new OrganSamplePanel(s.getSample(), this);
				addSampling(showLine, ns);				

				dtde.dropComplete(true);
			} else {
				dtde.rejectDrop();
				dtde.dropComplete(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			dtde.dropComplete(false);
		}
		showLine = -1;
		repaint();
		
		firePropertyChange(PROPERTY_UPDATED, null, this);		
	}
	
	public ContainerTemplate getContainerTemplate() {
		ContainerTemplate containerTemplate = new ContainerTemplate();
		
		//Set the BlocNo
		if(blocNoComboBox.getText().length()>0) {
			try {
				containerTemplate.setBlocNo(Integer.parseInt(blocNoComboBox.getText()));
			} catch(Exception e) {
				containerTemplate.setBlocNo(1);
			}
			
		}
		
		//Add the duplicates
		List<Duplicate> duplicates = new ArrayList<Duplicate>();
		for (int i = 0; i < copiesSpinners.size(); i++) {
			Duplicate d = new Duplicate();
			int n = (Integer)copiesSpinners.get(i).getValue();
			if(n<=0) continue;
			d.setNDuplicates(n);
			d.setStaining(stainingComboboxes.get(i).getText());
			d.setSectionNo(sectionNoComboboxes.get(i).getText());
			duplicates.add(d);
		}
		
		containerTemplate.setDuplicates(duplicates);
		
		//Add the items
		for (OrganSamplePanel samplingItem : samplingItems) {
			SampleDescriptor s = samplingItem.getSample().clone();
			containerTemplate.getSampleDescriptors().add(s);
		}
		return containerTemplate;
	}
	
}
