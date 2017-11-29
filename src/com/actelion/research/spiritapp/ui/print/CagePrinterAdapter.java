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

package com.actelion.research.spiritapp.ui.print;

import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.print.CagePrinterPDF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.UIUtils;

public class CagePrinterAdapter extends PrintAdapter {

	private JCheckBox printGroupTreatmentCheckBox = new JCheckBox("Print groups / treatments", false);
	private JCheckBox printTreatmentDescCheckBox = new JCheckBox("Print treatment's descriptions", false);
	private JCheckBox printBackgroundCheckBox = new JCheckBox("Use a white background (no group background colors)", false);

	public CagePrinterAdapter(final PrintingTab tab, final ContainerType containerType) {
		super(tab);
	}


	@Override
	public JPanel getConfigPanel() {
		return UIUtils.createVerticalBox(
				printGroupTreatmentCheckBox,
				printTreatmentDescCheckBox,
				printBackgroundCheckBox, Box.createVerticalGlue());
	}

	@Override
	public void eventSetRows(List<Container> containers) {
		Study s = Biosample.getStudy(Container.getBiosamples(containers));
		if(s!=null && s.isBlind()) {
			printGroupTreatmentCheckBox.setEnabled(false);
			printGroupTreatmentCheckBox.setSelected(false);
			printTreatmentDescCheckBox.setEnabled(false);
			printTreatmentDescCheckBox.setSelected(false);
		} else {
			printGroupTreatmentCheckBox.setEnabled(true);
			printGroupTreatmentCheckBox.setSelected(true);
			printTreatmentDescCheckBox.setEnabled(true);
			printTreatmentDescCheckBox.setSelected(Spirit.getConfig().getProperty("print.cage.fullTreatment", false));
		}
	}

	@Override
	public void print(List<Container> containers) throws Exception {
		Spirit.getConfig().setProperty("print.cage.fullTreatment", printTreatmentDescCheckBox.isSelected());
		CagePrinterPDF.printCages(containers, printGroupTreatmentCheckBox.isSelected(), printTreatmentDescCheckBox.isSelected(), printBackgroundCheckBox.isSelected());
	}



}
