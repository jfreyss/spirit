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

package com.actelion.research.spiritapp.animalcare.ui.monitor;

import java.awt.Color;

import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.util.ui.JTextComboBox;

public class MonitorTextComboBox extends JTextComboBox {

	private boolean required;

	public MonitorTextComboBox(Result result, int valueNo, boolean required) {
		super();
		assert result!=null;
		assert result.getBiosample()!=null;
		
		setText(result.getOutputResultValues().get(valueNo).getValue());
		setWarningWhenEdited(true);
		setRequired(required);
		
		String tooltip = "<html><b><u>"+result.getBiosample().getTopParentInSameStudy().getSampleIdName()+"</u> "+(result.getPhase()==null? "": " - " +result.getPhase().toString()) + "</b>";
		setToolTipText(tooltip);
		if(result.getId()>0) setToolTipText(getToolTipText() + "<br>Last value: "+ MonitoringCagePanel.formatTooltipText(result.getOutputResultValues().get(valueNo).getValue(), result.getUpdUser(), result.getUpdDate()));
		addTextChangeListener(new MonitorTextChangeListener(result, valueNo));
		
		setBorderColor(Color.GRAY);

	}
	
	public boolean isRequired() {
		return required;
	}
	
	public void setRequired(boolean required) {
		this.required = required;
		setBackground(this.required? LF.BGCOLOR_REQUIRED: null);
	}
}
