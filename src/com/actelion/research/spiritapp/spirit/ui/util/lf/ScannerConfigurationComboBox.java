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

package com.actelion.research.spiritapp.spirit.ui.util.lf;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.scanner.ScannerConfiguration;

public class ScannerConfigurationComboBox extends JGenericComboBox<ScannerConfiguration> {
	public ScannerConfigurationComboBox(boolean allowOpeningFile) {
		
		List<ScannerConfiguration> list = new ArrayList<ScannerConfiguration>();
		for (ScannerConfiguration sc : ScannerConfiguration.valuesForBiosamples()) {
			list.add(sc);
		}
		
		if(allowOpeningFile) {
			list.add(ScannerConfiguration.SCANNER_CONFIGURATION_OPENFILE);
		}		
		setValues(list, false);
	}

}
