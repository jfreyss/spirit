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
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.util.ui.JCustomTextField;

public class MonitorTextField extends JCustomTextField {

	private final Result result;
	private TestAttribute ta;
	private final int outputNo;
	private boolean required;
	
	public MonitorTextField(Result result, int outputNo, boolean required) {
		super();
		this.ta = result==null || result.getTest()==null || outputNo<0 || outputNo>=result.getTest().getOutputAttributes().size()? null: result.getTest().getOutputAttributes().get(outputNo);
		this.result = result;
		this.outputNo = outputNo;

		setType(ta==null || ta.getDataType()==DataType.NUMBER || ta.getDataType()==DataType.FORMULA? JCustomTextField.DOUBLE:  JCustomTextField.ALPHANUMERIC);
		setVisible(ta!=null);
		setRequired(required);
		
		//Be sure to refresh the text before setting the textchangelistener
		refreshText();

		
		if(ta!=null && ta.getDataType()==DataType.FORMULA) setForeground(Color.BLUE);
		setBorderColor(Color.GRAY);
		boolean hasSample = result!=null && result.getBiosample()!=null;
		setEnabled(hasSample);
		setWarningWhenEdited(hasSample);

		addTextChangeListener(new MonitorTextChangeListener(result, outputNo));
		addFocusListener(new AutoScrollFocusListener());

	}
	
	public void refreshText() {

		if(result!=null && result.getBiosample()!=null && ta!=null && result.getResultValue(ta)!=null) {
			setText(result.getResultValue(ta).getValue());			
			String tooltip = "<html><b>"+result.getBiosample().getTopParentInSameStudy().getSampleIdName() + "</b>:<br>" +
					(ta==null? "": ta.getName()) + 
					(result.getPhase()==null? "": " " + result.getPhase().toString()) + 
					(ta!=null && ta.getDataType()==DataType.FORMULA? "<br>=" + ta.getParameters(): ""); 
			
			if(result.getId()>0) {
				tooltip += "<br>Last value: "+ MonitoringCagePanel.formatTooltipText(result.getOutputResultValues().get(outputNo).getValue(), result.getUpdUser(), result.getUpdDate());
			} 
			setToolTipText(tooltip);
		}				
	}
	

	public boolean isRequired() {
		return required;
	}
	
	public void setRequired(boolean required) {
		this.required = required;
		setBackground(this.required? LF.BGCOLOR_REQUIRED: null);
	}

}
