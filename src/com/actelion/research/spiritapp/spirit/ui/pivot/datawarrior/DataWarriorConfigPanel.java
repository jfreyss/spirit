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

package com.actelion.research.spiritapp.spirit.ui.pivot.datawarrior;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritapp.spirit.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.pivot.Computed;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorConfig;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorConfig.ChartType;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorExporter;
import com.actelion.research.spiritcore.business.pivot.datawarrior.PivotDataType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

public class DataWarriorConfigPanel extends JPanel {

	private DataWarriorConfig model = new DataWarriorConfig();

	private final List<Result> results;
	private final Set<TestAttribute> skippedAttributes;
	private final Set<PivotDataType> pivotDataTypes;

	private final PivotDataTypeComboBox xAxisComboBox;
	private final JGenericComboBox<String> yAxisComboBox;
	private final JCheckBox logScaleCheckbox = new JCheckBox("Log Scale");

	private final JGenericComboBox<ChartType> chartTypeComboBox = new JGenericComboBox<ChartType>(ChartType.values(), false);

	private final JPanel itemPanel = new JPanel(new BorderLayout());
	private final JEditorPane editorPane = new ImageEditorPane();

	public DataWarriorConfigPanel(List<Result> results, Set<TestAttribute> skippedAttributes) {
		super(new GridLayout(1,1));
		this.results = results;
		this.skippedAttributes = skippedAttributes;

		//Init comboboxes
		pivotDataTypes = PivotDataType.getValues(results, SpiritFrame.getUser());
		xAxisComboBox = new PivotDataTypeComboBox(pivotDataTypes);

		List<String> computedValues = new ArrayList<String>();
		computedValues.add("VALUE");
		for (Computed computed : Computed.values()) {
			if(computed==Computed.NONE) continue;
			computedValues.add(computed.getName());
		}

		yAxisComboBox = new JGenericComboBox<String>(computedValues, false);
		yAxisComboBox.setPreferredWidth(100);

		//Graph panel
		JPanel panel1 = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(1, 1, 1, 1);

		c.gridwidth = 1;
		c.gridy++;
		c.gridx = 1; panel1.add(new JLabel("Graph Type: "), c);
		c.gridx = 2; panel1.add(chartTypeComboBox, c);

		c.gridy++;
		c.gridx = 1; panel1.add(new JLabel("X Axis: "), c);
		c.gridx = 2; panel1.add(xAxisComboBox, c);

		c.gridy++;
		c.gridx = 1; panel1.add(new JLabel("Y Axis: "), c);
		c.gridx = 2; panel1.add(UIUtils.createHorizontalBox(yAxisComboBox, logScaleCheckbox), c);


		LF.initComp(editorPane);
		editorPane.setEditable(false);
		JScrollPane sp = new JScrollPane(editorPane);
		sp.setPreferredSize(new Dimension(200, 120));


		c.gridy++;
		c.gridwidth=5; c.fill = GridBagConstraints.BOTH;
		c.gridx = 1; c.weightx = 1; panel1.add(itemPanel, c);

		c.gridy++;
		c.gridwidth=5; c.fill = GridBagConstraints.BOTH;
		c.gridx = 1; c.weightx = c.weighty = 1; panel1.add(sp, c);


		xAxisComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.setXAxis(xAxisComboBox.getSelection());
				updateView();
			}
		});


		add(panel1);
		setBorder(BorderFactory.createLoweredBevelBorder());
	}

	/**
	 * Update itempanel
	 */
	private void updateView() {
		itemPanel.removeAll();
		JPanel itemPanel2 = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(1, 0, 1, 0);

		if(model.getCustomTemplate()==null) {
			c.gridy = 0; c.anchor = GridBagConstraints.WEST;
			c.gridwidth = 10;
			c.gridx = 0; itemPanel2.add(new JCustomLabel("How do you want to display the following data: ", FastFont.BOLD), c);
			c.gridwidth = 1;

			for(final PivotDataType p: pivotDataTypes) {
				if(p==model.getXAxis()) continue;
				c.gridy++;
				c.gridx = 0; itemPanel2.add(new JCustomLabel(p.toString()+": "), c);

				JToggleButton mergeButton = createToggleButton("Don't show");
				if(model.getMerge().contains(p)) mergeButton.setSelected(true);
				mergeButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.addMerge(p);
						updateView();
					}
				});
				c.gridx++; itemPanel2.add(mergeButton, c);

				JToggleButton newGraphButton = createToggleButton("New Graph");
				if(!model.isSet(p)) newGraphButton.setSelected(true);
				newGraphButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.unset(p);
						updateView();
					}
				});
				c.gridx++; itemPanel2.add(newGraphButton, c);

				c.gridx++; itemPanel2.add(Box.createHorizontalStrut(12), c);

				JToggleButton separateButton = createToggleButton("Separate");
				if(model.getSeparate()==p) separateButton.setSelected(true);
				separateButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.setSeparate(p);
						updateView();
					}
				});
				c.gridx++; itemPanel2.add(separateButton, c);

				JToggleButton splitButton = createToggleButton("SplitView");
				if(model.getSplit()==p) splitButton.setSelected(true);
				splitButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.setSplit(p);
						updateView();
					}
				});
				c.gridx++; itemPanel2.add(splitButton, c);
			}
		} else {
			//			c.gridy = 0; c.anchor = GridBagConstraints.WEST;
			//			c.gridwidth = 10;
			//			c.gridx = 0; itemPanel2.add(new JLabel("<html><span style='color:red'><b>Expert Mode</b><br>The Data will be exported exactly as displayed</html>"), c);
		}

		if(model.getXAxis()!=null) c.gridy++; c.gridx = 0; c.weighty = 0; itemPanel2.add(Box.createVerticalStrut(20), c);
		c.gridy++; c.gridx = 0; c.weighty = 1; itemPanel2.add(Box.createVerticalGlue(), c);
		itemPanel.add(BorderLayout.NORTH, new JSeparator());
		itemPanel.add(BorderLayout.WEST, itemPanel2);
		itemPanel.revalidate();


		editorPane.setText("<html><div style='color:#AAAAAA'>Analyzing...");
		new SwingWorkerExtended("Analyzing", editorPane) {

			List<String> views;
			Exception exception = null;

			@Override
			protected void doInBackground() throws Exception {
				try {
					views = DataWarriorExporter.getViewNames(results, getDataWarriorModel(), SpiritFrame.getUser());
				} catch(Exception ex) {
					ex.printStackTrace();

					exception = ex;
				}
			}

			@Override
			protected void done() {
				//Preview produced graphs
				if(exception==null) {
					StringBuilder sb = new StringBuilder();
					sb.append("<html><div style='font-size:9px'>");
					sb.append("<b>Those settings gives " + views.size() + " graph" + (views.size()>0?"s":"") + ":</b><br> ");
					if(views.size()>DataWarriorExporter.MAX_VIEWS) {
						sb.append("<span style='color:red'>Only the first "+DataWarriorExporter.MAX_VIEWS+" will be displayed</span><br>");
					}
					for (int i = 0; i < views.size(); i++) {
						if(i>0) sb.append("<br>");
						sb.append(" - "+ views.get(i).replaceAll("<.>", "").replace("\n", " "));
					}
					editorPane.setText(sb.toString());
				} else {
					editorPane.setText("<html><div style='color:#AA0000'>"+exception);
				}
				editorPane.setCaretPosition(0);
			}
		};


	}

	private JToggleButton createToggleButton(String name) {
		JToggleButton button = new JToggleButton(name);
		button.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
		button.setBackground(Color.LIGHT_GRAY);
		button.setForeground(Color.BLACK);
		return button;
	}

	private int push = 0;
	public void setDataWarriorModel(DataWarriorConfig model) {
		assert model!=null;
		this.model = model;

		try {
			push++;
			xAxisComboBox.setSelection(model.getXAxis());
			chartTypeComboBox.setSelection(model.getType());
			yAxisComboBox.setSelection(model.getComputed()==null || model.getComputed()==Computed.NONE? "VALUE": model.getComputed().getName());
			logScaleCheckbox.setSelected(model.isLogScale());
		} finally {
			push--;
		}
		updateView();
	}

	private void updateModel() {
		if(push>0) return;
		model.setType(chartTypeComboBox.getSelection());
		model.setXAxis(xAxisComboBox.getSelection());
		model.setComputed(Computed.getValue(yAxisComboBox.getSelection()));
		model.setLogScale(logScaleCheckbox.isSelected());
		model.setSkippedAttributes(skippedAttributes);
	}

	public DataWarriorConfig getDataWarriorModel() {
		updateModel();
		return model;
	}



}
