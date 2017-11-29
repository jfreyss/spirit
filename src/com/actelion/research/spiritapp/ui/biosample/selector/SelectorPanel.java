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

package com.actelion.research.spiritapp.ui.biosample.selector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.actelion.research.spiritapp.ui.biosample.SampleIdLabel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.util.ui.JTextComboBox;

public class SelectorPanel extends JPanel {

	private SelectorDlg dlg;
	private final JTextComboBox queryComboBox;
	private final DefaultListModel<Biosample> listModel = new DefaultListModel<Biosample>();
	private final JList<Biosample> discriminatorList = new JList<Biosample>(listModel);
	private final SampleIdLabel label = new SampleIdLabel();

	public SelectorPanel(final SelectorDlg dlg) {
		super(new GridBagLayout());
		this.dlg = dlg;

		//queryComboBox
		queryComboBox = new JTextComboBox(dlg.getQueryValues());
		queryComboBox.setTextWhenEmpty(dlg.getQueryLinker()==null?"": dlg.getQueryLinker().toString());
		queryComboBox.addTextChangeListener(e-> {
			updateList();
			dlg.selectionQueryChanged();
		});
		queryComboBox.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				boolean canBeChosen = dlg.isCompatibleWithSelected((String)value);
				setForeground(canBeChosen?Color.BLACK: Color.LIGHT_GRAY);
				return this;
			}
		});


		//discriminatorList
		discriminatorList.addListSelectionListener(e-> {
			dlg.selectionBiosampleChanged();
		});
		discriminatorList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				label.setBiosample((Biosample)value);
				label.setOpaque(true);
				label.setBackground(getBackground());
				label.setForeground(getForeground());
				label.setBorder(getBorder());


				boolean gray = dlg.isGray(SelectorPanel.this, (Biosample)value);
				label.setAlpha(gray? .75f: 0f);
				return label;
			}
		});
		discriminatorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		updateView();



		//Layout component
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.fill = GridBagConstraints.BOTH; c.weightx = 1;
		c.gridy = 0; c.weighty = 0; add(queryComboBox, c);
		c.gridy = 1; c.weighty = 1; add(new JScrollPane(discriminatorList), c);


		setPreferredSize(new Dimension(150, 300));
		setBorder(BorderFactory.createEtchedBorder());
	}

	private void updateList() {
		String sel = queryComboBox.getText();
		listModel.clear();
		listModel.addElement(null);
		for(Biosample b: dlg.getBiosamples(sel)) {
			listModel.addElement(b);
		}
		discriminatorList.setSelectedIndex(0);
	}

	public void updateView() {
		label.setExtraDisplay(dlg.getDisplayLinker(), false);
		discriminatorList.updateUI();
	}
	public String getQuery() {
		return queryComboBox.getText();
	}

	public Biosample getSelection() {
		return discriminatorList.getSelectedValue();
	}
}
