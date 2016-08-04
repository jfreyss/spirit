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

package com.actelion.research.spiritapp.spirit.ui.exchange;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleList;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleMetadataPanel;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping.MappingAction;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.UIUtils;
import com.itextpdf.text.Font;

public class BiosampleMappingPanel extends JPanel implements IMappingPanel {

	private ImporterDlg dlg;
	private final Biotype inputBiotype;
	private JRadioButton r1 = new JRadioButton("Keep the existing one");
	private JRadioButton r2 = new JRadioButton("Replace the sample");
	private JRadioButton r3 = new JRadioButton("Create a copy");
	private final BiosampleList l = new BiosampleList();

	public BiosampleMappingPanel(ImporterDlg dlg, Biotype inputBiotype, List<Biosample> inputBiosamples) { 
		super(new GridLayout());
	
		this.dlg = dlg;
		this.inputBiotype = inputBiotype;
		
		setMinimumSize(new Dimension(200, 200));
		
		ButtonGroup group = new ButtonGroup();
		group.add(r1); 
		group.add(r2); 
		group.add(r3);
		
		r1.setSelected(true);
		r1.setToolTipText("The Biosample from the imported file will be ignored, and possible links will be made to the existing ones");
		r2.setToolTipText("The Biosample from the imported file will be replaced by the one in this file");
		r3.setToolTipText("The Biosample from the imported file will be renamed by appending .1 / .2 to the sampleId");
		
		
		JPanel existingPanel = UIUtils.createVerticalBox(BorderFactory.createEtchedBorder(),
						new JCustomLabel("What do you want to do for the biosamples with existing sampleIds? ", Font.BOLD),
						UIUtils.createHorizontalBox(BorderFactory.createEmptyBorder(2, 2, 2, 2), r1, r2, r3, Box.createVerticalGlue()));
		existingPanel.setOpaque(true);
		existingPanel.setBackground(LF.COLOR_ERROR_BACKGROUND);
				
		final BiosampleMetadataPanel metadataPanel = new BiosampleMetadataPanel();
		if(inputBiosamples!=null) {
			Collections.sort(inputBiosamples);
			final Map<String, Biosample> existing = DAOBiosample.getBiosamplesBySampleIds(Biosample.getSampleIds(inputBiosamples));
			l.setBiosamples(inputBiosamples);
			l.setCellRenderer(new DefaultListCellRenderer() {
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					if(existing.get(((Biosample)value).getSampleId())!=null) setBackground(LF.COLOR_ERROR_BACKGROUND);
					return this;
				}
			});
			l.addListSelectionListener(new ListSelectionListener() {						
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if(e.getValueIsAdjusting()) return;
					metadataPanel.setBiosample(l.getSelectedValue());
				}
			});
			if(inputBiosamples.size()>0) {
				l.setSelectedIndex(0);
			}
			
			
			existingPanel.setVisible(existing.size()>0);			
			add(UIUtils.createBox(
					new JScrollPane(metadataPanel), 
					new JLabel(l.getModel().getSize()+" "+inputBiotype.getName() + " (" + existing.size()+" overlapping sampleIds)"),
					existingPanel,
					new JScrollPane(l), 
					null));
		}  else {
			existingPanel.setVisible(false);
		}
		updateView();
	}
	
	public void updateView() {
		ExchangeMapping mapping = dlg.getMapping();
		MappingAction action = mapping.getBiotype2existingBiosampleAction().get(inputBiotype.getName());		
		if(action==MappingAction.IGNORE_LINK) r1.setSelected(true);
		if(action==MappingAction.MAP_REPLACE) r2.setSelected(true);
		if(action==MappingAction.CREATE_COPY) r3.setSelected(true);		
	}
	
	public void updateMapping() {
		ExchangeMapping mapping = dlg.getMapping();
		MappingAction action = r1.isSelected()? MappingAction.IGNORE_LINK: r2.isSelected()? MappingAction.MAP_REPLACE: r3.isSelected()? MappingAction.CREATE_COPY: MappingAction.IGNORE_LINK;
		mapping.getBiotype2existingBiosampleAction().put(inputBiotype.getName(), action);
	}

}
