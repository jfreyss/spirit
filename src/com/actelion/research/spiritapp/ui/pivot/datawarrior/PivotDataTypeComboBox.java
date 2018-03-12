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

package com.actelion.research.spiritapp.ui.pivot.datawarrior;

import java.util.Collection;

import com.actelion.research.spiritcore.business.pivot.datawarrior.PivotDataType;
import com.actelion.research.util.ui.JGenericComboBox;

public class PivotDataTypeComboBox extends JGenericComboBox<PivotDataType> {
	
	public PivotDataTypeComboBox() {
		super(PivotDataType.values(), true);
		setPreferredWidth(100);
	}
	
	public PivotDataTypeComboBox(Collection<PivotDataType> values) {
		super(values, true);
		setPreferredWidth(100);
	}

	
//	@Override
//	public Component processCellRenderer(JLabel comp, DataType value, int index) {
//		switch (value) {
//		case GROUP:
//			comp.setBackground(PivotItemClassifier.STUDY_GROUP.getBgcolor());
//			break;
//		case PHASE:
//			comp.setBackground(PivotItemClassifier.STUDY_PHASE.getBgcolor());
//			break;
//		case INPUT:
//			comp.setBackground(PivotItemClassifier.BIOSAMPLE.getBgcolor());
//			break;
//		case VALUE:
//			comp.setBackground(PivotItemClassifier.RESULT.getBgcolor());
//			break;
//		}
//		comp.setOpaque(true);
//		comp.setForeground(UIUtils.getInverseColor(getBackground()));
//		return comp;
//		
//	}

}
