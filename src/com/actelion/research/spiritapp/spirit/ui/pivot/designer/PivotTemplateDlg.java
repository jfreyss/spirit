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

package com.actelion.research.spiritapp.spirit.ui.pivot.designer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritcore.business.pivot.PivotItem;
import com.actelion.research.spiritcore.business.pivot.PivotItemFactory;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Aggregation;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Deviation;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class PivotTemplateDlg extends JEscapeDialog {

	public static final String PROPERTY_UPDATED = "updated";
	
	public static final Color ROW_COLOR = new Color(139,248,244);
	public static final Color COLUMN_COLOR = new Color(76,201,255);
	public static final Color CELL_COLOR = new Color(218,236,240);
	
	
	private ItemPanelControler itemControler = new ItemPanelControler() {
		@Override
		public void onDragEnd() {
			updateModel();
			PivotTemplateDlg.this.firePropertyChange(PROPERTY_UPDATED, null, "");
		}
	};

	private DropZonePanel mergeItems;
	private DropZonePanel columnItems;
	private DropZonePanel rowItems;
	private DropZonePanel cellItems;

	private JGenericComboBox<Aggregation> display1ComboBox = new JGenericComboBox<>(Aggregation.values(), false);
	private JGenericComboBox<Deviation> display2ComboBox = new JGenericComboBox<>(Deviation.values(), false);
	private ComputedComboBox computedComboBox = new ComputedComboBox();
	private JCheckBox showNCheckBox = new JCheckBox("Show N.Values");

	private final PivotTemplate pivotTemplate;
	private List<PivotItem> pivotItems;
	
	private JButton resetButton = new JButton(new ResetAction());
	
	
	public PivotTemplateDlg(final JDialog dlg, final PivotTemplate pivotTemplate, Set<PivotItem> set, boolean forResults) {
		super(dlg, "Edit Pivot Template", false);
		this.pivotTemplate = pivotTemplate;
		init(set, forResults);		
	}
	
	public PivotTemplateDlg(final JFrame frame, final PivotTemplate pivotTemplate, Set<PivotItem> set, boolean forResults) {
		super(frame, "Edit Pivot Template", false);
		this.pivotTemplate = pivotTemplate;
		init(set, forResults);
	}
	
	
	private void init(final Set<PivotItem> set, final boolean forResults) {
		
		this.pivotItems = new ArrayList<PivotItem>(set);
		this.itemControler.clear();
		Collections.sort(pivotItems);


		//itemControler
		mergeItems = new DropZonePanel("", itemControler, new Color(240,240,240));
		columnItems = new DropZonePanel("Columns", itemControler, UIUtils.getDilutedColor(Color.WHITE, COLUMN_COLOR));
		rowItems = new DropZonePanel("Rows",  itemControler, UIUtils.getDilutedColor(Color.WHITE, ROW_COLOR));
		cellItems = new DropZonePanel("Cells",  itemControler, CELL_COLOR);

		itemControler.addDropZone(Where.MERGE, mergeItems);
		itemControler.addDropZone(Where.ASROW, rowItems);
		itemControler.addDropZone(Where.ASCOL, columnItems);
		itemControler.addDropZone(Where.ASCELL, cellItems);

		
		
		//DisplayPanel
		JPanel displayPanel = UIUtils.createTable(
				new JLabel("Aggregation: "), UIUtils.createHorizontalBox(display1ComboBox, display2ComboBox),
				null, showNCheckBox,
				null, Box.createVerticalStrut(5),
				new JLabel("Calculation: "), computedComboBox);
		displayPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		
		//Put items in scrollpane
		JScrollPane mergeScrollPane = new JScrollPane(mergeItems, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JScrollPane rowScrollPane = new JScrollPane(rowItems, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JScrollPane columnScrollPane = new JScrollPane(columnItems, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JScrollPane cellScrollPane = new JScrollPane(cellItems, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		rowScrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		columnScrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		cellScrollPane.setBorder(null);
		
		mergeScrollPane.setPreferredSize(new Dimension(800, 200));
		rowScrollPane.setPreferredSize(new Dimension(400, 180));
		columnScrollPane.setPreferredSize(new Dimension(350, 180));
		cellScrollPane.setPreferredSize(new Dimension(400, 100));
		
		JPanel cellPanel = UIUtils.createBox(cellScrollPane, null, displayPanel, null, null);
		cellPanel.setOpaque(true);
		cellPanel.setBackground(CELL_COLOR);
		cellPanel.setBorder(BorderFactory.createMatteBorder(0,0,1,1, Color.BLACK));
		displayPanel.setVisible(forResults);
		
		//PivotTablePanel
		final JPanel pivotTablePanel = new JPanel(new GridBagLayout());
		pivotTablePanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		pivotTablePanel.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 0, 0, 0);
		c.anchor = GridBagConstraints.NORTHWEST;		
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 1; c.gridy = 0; c.gridwidth = 1; pivotTablePanel.add(columnScrollPane, c);
		c.gridx = 0; c.gridy = 1; c.gridwidth = 1; pivotTablePanel.add(rowScrollPane, c);
		c.gridx = 1; c.gridy = 1; c.gridwidth = 1; pivotTablePanel.add(cellPanel, c);
		

		//CenterPanel
		JButton okButton = new JButton("Close");
		JPanel centerPane = new JPanel(new BorderLayout());
		centerPane.setOpaque(true);
		centerPane.setBackground(Color.WHITE);
		centerPane.add(BorderLayout.NORTH, UIUtils.createTitleBox("Drag & Drop the items to the template below", mergeScrollPane));
		centerPane.add(BorderLayout.CENTER, pivotTablePanel);
		
		//ContentPane
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, centerPane);
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(resetButton, Box.createHorizontalStrut(15), Box.createHorizontalStrut(20), Box.createHorizontalGlue(), okButton));
		getRootPane().setDefaultButton(okButton);		
		
		//UpdateView
		updateView();
		
		//Events
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				eventOk();
			}
		});
		ItemListener processingListener = new ItemListener() {			
			@Override
			public void itemStateChanged(ItemEvent e) {
				updateModel();
				PivotTemplateDlg.this.firePropertyChange(PROPERTY_UPDATED, null, "");		
			}
		};
		display1ComboBox.addItemListener(processingListener);
		display2ComboBox.addItemListener(processingListener);
		computedComboBox.addItemListener(processingListener);
		showNCheckBox.addItemListener(processingListener);
		
		//Make Visible
		setSize(900,680);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
	}
	
	
	private void updateModel() {
		for (PivotItem item : pivotItems) {
			pivotTemplate.setWhere(item, itemControler.getPanelId(item));
		}

		pivotTemplate.setAggregation(display1ComboBox.getSelection());
		pivotTemplate.setDeviation(display2ComboBox.getSelection());
		pivotTemplate.setComputed(computedComboBox.getSelection());
		pivotTemplate.setShowN(showNCheckBox.isSelected());

		columnItems.updateView();
		rowItems.updateView();
		cellItems.updateView();
	}

	private void updateView() {
		for (PivotItem item : pivotItems) {
			itemControler.placeItem(item, pivotTemplate.getWhere(item));
		}

		itemControler.updateView();

		display1ComboBox.setSelection(pivotTemplate.getAggregation());
		display2ComboBox.setSelection(pivotTemplate.getDeviation());
		computedComboBox.setSelection(pivotTemplate.getComputed());
		showNCheckBox.setSelected(pivotTemplate.isShowN());
		
		

	}

	public void eventOk() {
		updateModel();
		PivotTemplateDlg.this.firePropertyChange(PROPERTY_UPDATED, null, ""); 
		PivotTemplateDlg.this.dispose();
	}
	
	private class ResetAction extends AbstractAction {
		public ResetAction() {
			super("Start with an empty template");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			pivotTemplate.clear();
			pivotTemplate.setWhere(PivotItemFactory.RESULT_OUTPUT, Where.ASCOL);
			updateView();
			PivotTemplateDlg.this.firePropertyChange(PROPERTY_UPDATED, null, ""); //Don't hang during refresh		
		}
	}
	
}
