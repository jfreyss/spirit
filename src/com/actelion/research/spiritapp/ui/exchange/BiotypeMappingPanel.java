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

package com.actelion.research.spiritapp.ui.exchange;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.ui.util.lf.BiotypeComboBox;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping.EntityAction;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class BiotypeMappingPanel extends JPanel implements IMappingPanel {
	private Biotype biotype;

	private ImporterDlg dlg;
	//1st line
	private final MappingPanel biotypeMappingPanel;
	private final BiotypeComboBox biotypeComboBox;

	//
	private final JPanel centerPanel = new JPanel();
	private final List<MappingPanel> metadataMappingPanels = new ArrayList<>();
	private final List<JGenericComboBox<BiotypeMetadata>> metadataComboboxes = new ArrayList<>();
	private List<BiotypeMetadata> metadatas = new ArrayList<>();

	public BiotypeMappingPanel(ImporterDlg dlg, Biotype fromBiotype) {
		super(new BorderLayout());
		this.dlg = dlg;
		this.biotype = fromBiotype;

		ExchangeMapping mapping = dlg.getMapping();

		List<Biotype> biotypes = Biotype.filter(DAOBiotype.getBiotypes(), fromBiotype.getCategory());

		//Find metadata to be skipped
		if(dlg.getExchange()!=null && dlg.getExchange().getBiosamples().size()>0) {
			metadata: for (BiotypeMetadata m : biotype.getMetadata()) {
				assert m!=null: biotype + " has null metadata: " + biotype.getMetadata();
				for (Biosample b : dlg.getExchange().getBiosamples()) {
					if(b.getBiotype().equals(m.getBiotype()) && b.getMetadataValue(m)!=null && b.getMetadataValue(m).length()>0) {
						metadatas.add(m);
						continue metadata;
					}
				}
			}
		} else {
			metadatas.addAll(biotype.getMetadata());
		}

		//Init components
		biotypeComboBox = new BiotypeComboBox(biotypes, "Map to...");
		biotypeMappingPanel = new MappingPanel(biotypeComboBox);
		biotypeMappingPanel.addPropertyChangeListener(MappingPanel.PROPERTY_ACTION, evt -> {
			updateLayout();
		});

		biotypeComboBox.addTextChangeListener(e-> {
			Biotype toBiotype = biotypeComboBox.getSelection();
			for (int index = 0; index < metadatas.size(); index++) {
				BiotypeMetadata m = metadatas.get(index);
				JGenericComboBox<BiotypeMetadata> comboBox = null;
				if(toBiotype!=null) {
					comboBox = new JGenericComboBox<>(toBiotype.getMetadata(), "Map to...");
					comboBox.setSelectionString(m.getName());
				}
				metadataMappingPanels.get(index).setMappingComponent(comboBox);
				if(comboBox!=null && comboBox.getSelectedIndex()>0) {
					metadataMappingPanels.get(index).setMappingAction(EntityAction.MAP_REPLACE);
					metadataMappingPanels.get(index).setCreationEnabled(false);
				} else {
					metadataMappingPanels.get(index).setCreationEnabled(true);
				}
			}
		});

		for (int index = 0; index < metadatas.size(); index++) {
			metadataMappingPanels.add(new MappingPanel(null));
			metadataComboboxes.add(new JGenericComboBox<BiotypeMetadata>());
		}


		//Init Layout
		JPanel mapBiotypePanel = UIUtils.createHorizontalBox(biotypeMappingPanel, Box.createHorizontalGlue());
		mapBiotypePanel.setOpaque(false);
		add(BorderLayout.NORTH, mapBiotypePanel);
		JPanel panel = UIUtils.createHorizontalBox(centerPanel, Box.createGlue());
		panel.setOpaque(true);
		setOpaque(false);

		add(BorderLayout.CENTER, new JScrollPane(panel));


		//Preselection
		biotypeMappingPanel.setMappingAction(mapping.getBiotype2action().get(biotype.getName()));
		biotypeComboBox.setText(mapping.getBiotype2mappedBiotype().get(biotype.getName())==null?"":mapping.getBiotype2mappedBiotype().get(biotype.getName()).getName());
		if(biotypeComboBox.getSelection()!=null) {
			biotypeMappingPanel.setMappingAction(EntityAction.MAP_REPLACE);
			biotypeMappingPanel.setCreationEnabled(false);
		} else {
			biotypeMappingPanel.setCreationEnabled(true);
		}


		updateView();
	}

	private void updateLayout() {
		centerPanel.removeAll();
		List<JComponent> formComponents = new ArrayList<>();
		EntityAction action = biotypeMappingPanel.getMappingAction();
		if(action!=EntityAction.SKIP) {
			if(biotype.getSampleNameLabel()!=null) {
				formComponents.add(new JLabel("<html>MainField '<b>"+biotype.getSampleNameLabel()+"</b>':"));
				formComponents.add(new JLabel("MAP TO NAME"));
			}
			for (int index = 0; index < metadatas.size(); index++) {
				BiotypeMetadata m = metadatas.get(index);
				formComponents.add(new JLabel("<html>Metadata '<b>"+m.getName()+"</b>': "));
				formComponents.add(metadataMappingPanels.get(index));
				if(action==EntityAction.CREATE) {
					metadataMappingPanels.get(index).setMappingAction(EntityAction.CREATE);
					metadataMappingPanels.get(index).setMappingComponent(null);
				}
			}
		}

		centerPanel.add(UIUtils.createHorizontalBox(UIUtils.createTable(formComponents), Box.createHorizontalGlue()));

		centerPanel.revalidate();
	}

	@Override
	public void updateView() {
		ExchangeMapping mapping = dlg.getMapping();
		biotypeMappingPanel.setMappingAction(mapping.getBiotype2action().get(biotype.getName()));
		biotypeComboBox.setSelection(mapping.getBiotype2mappedBiotype().get(biotype.getName()));

		updateLayout();

	}

	@Override
	@SuppressWarnings("unchecked")
	public void updateMapping() {
		ExchangeMapping mapping = dlg.getMapping();
		mapping.getBiotype2action().put(biotype.getName(), biotypeMappingPanel.getMappingAction());
		mapping.getBiotype2mappedBiotype().put(biotype.getName(), biotypeComboBox.getSelection());

		for (int i = 0; i < metadatas.size(); i++) {
			BiotypeMetadata m = metadatas.get(i);
			MappingPanel mappingPanel = metadataMappingPanels.get(i);
			mapping.getBiotypeMetadata2action().put(new com.actelion.research.spiritcore.util.Pair<String, String>(biotype.getName(), m.getName()), mappingPanel.getMappingAction());

			if(mappingPanel.getMappingComponent()!=null && mappingPanel.getMappingComponent() instanceof JGenericComboBox) {
				JGenericComboBox<BiotypeMetadata> combobox = ((JGenericComboBox<BiotypeMetadata>) mappingPanel.getMappingComponent());
				if(combobox.getSelection()!=null) {
					mapping.getBiotypeMetadata2mappedBiotypeMetadata().put(new com.actelion.research.spiritcore.util.Pair<String, String>(biotype.getName(), m.getName()), combobox.getSelection());
				}
			}
		}
	}

}
