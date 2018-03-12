/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.ui.util.component;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.actelion.research.spiritapp.ui.util.formtree.FormTree;
import com.actelion.research.spiritapp.ui.util.formtree.Strategy;
import com.actelion.research.spiritapp.ui.util.icons.ImageFactory;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

/**
 * Wrapper to a BioTypeComboBox
 * @author freyssj
 *
 */
public class BiotypeToggleNode extends BiotypeNode {

	private JPanel panel;
	private List<JToggleButton> buttons = new ArrayList<>();
	private final List<Biotype> biotypes;

	public BiotypeToggleNode(FormTree tree, final List<Biotype> biotypes, final Strategy<Biotype> strategy) {
		super(tree, biotypes, strategy);
		this.biotypes = biotypes;


		ActionListener al = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(getStrategy()!=null) {
					getStrategy().onFocus();
					getStrategy().onAction();
				}
			}
		};

		ButtonGroup group = new ButtonGroup();
		JToggleButton button = new JToggleButton("All", null);
		button.setFont(FastFont.MEDIUM);
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.addActionListener(al);
		button.setVisible(biotypes.size()>1);
		group.add(button);
		buttons.add(button);

		for(Biotype biotype: biotypes) {
			button = new JToggleButton(biotype.getName(), new ImageIcon(ImageFactory.getImage(biotype, 28)));
			button.setFont(FastFont.SMALL);
			button.setHorizontalAlignment(SwingConstants.LEFT);
			button.setHorizontalTextPosition(SwingConstants.CENTER);
			button.setVerticalTextPosition(SwingConstants.BOTTOM);
			button.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
			button.addActionListener(al);
			group.add(button);
			buttons.add(button);
		}

		panel = UIUtils.createHorizontalBox(buttons.toArray(new JToggleButton[0]));
		panel.setBorder(BorderFactory.createEmptyBorder(2,0,2,0));
		panel.setBackground(Color.WHITE);
		panel.setOpaque(false);

	}

	@Override
	protected void updateModel() {
		assert strategy!=null;
		strategy.setModel(getSelection());

	}

	@Override
	protected void updateView() {
		assert strategy!=null;
		Biotype model = strategy.getModel();
		if(model==null) {
			buttons.get(0).setSelected(true);
		} else {
			for (int i = 1; i < buttons.size(); i++) {
				if(biotypes.get(i-1).equals(model)) buttons.get(i).setSelected(true);
			}
		}
	}

	@Override
	public Biotype getSelection() {
		for (int i = 1; i < buttons.size(); i++) {
			if(buttons.get(i).isSelected()) return biotypes.get(i-1);
		}
		return null;
	}

	@Override
	public JComponent getComponent() {
		return panel;
	}

	@Override
	public JComponent getFocusable() {
		return panel;
	}

	@Override
	protected boolean isFilled() {
		return false;
	}


	@Override
	public void setEnabled(boolean enabled) {
		for (JToggleButton button : buttons) {
			button.setEnabled(enabled);
		}
	}



}
