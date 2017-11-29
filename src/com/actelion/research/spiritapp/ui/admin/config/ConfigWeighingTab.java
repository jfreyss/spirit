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

package com.actelion.research.spiritapp.ui.admin.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.actelion.research.spiritapp.ui.util.component.JFileBrowser;

public class ConfigWeighingTab extends JPanel {
	
	private JFileBrowser mettlerFileBrowser = new JFileBrowser();
	private JSpinner mettlerSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
	
	public ConfigWeighingTab() {
		
		SpiritConfig config = new SpiritConfig();
		String mtFile = config.getWeighingFile();
		int col = config.getWeighingCol();
		
		mettlerFileBrowser.setFile(mtFile);
		mettlerFileBrowser.setExtension(".txt");		
		mettlerSpinner.setValue(col+1);
		
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		
		c.weightx = 0; c.gridx=0; c.gridy=0; add(new JLabel("Path to the MT weighing balance: "), c);
		c.weightx = 1; c.gridx=1; c.gridy=0; add(mettlerFileBrowser, c);		
		
		c.weightx = 0; c.gridx=0; c.gridy=1; add(new JLabel("ColumnNo (with weight[g]): "), c);		
		c.weightx = 1; c.gridx=1; c.gridy=1; add(mettlerSpinner, c);

		
		c.weightx = 1; c.weighty = 1; c.gridx=1; c.gridy=100; add(Box.createGlue(), c);
		
	}
	
	public void eventOk() throws Exception {
		SpiritConfig config = new SpiritConfig();
		config.setWeighingFile(mettlerFileBrowser.getFile());
		config.setWeighingCol(((Integer)mettlerSpinner.getValue())-1);
	}
}
