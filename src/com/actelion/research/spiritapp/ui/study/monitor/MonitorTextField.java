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

package com.actelion.research.spiritapp.ui.study.monitor;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.ui.biosample.MetadataComponentFactory;
import com.actelion.research.spiritapp.ui.biosample.MetadataComponentFactory.AutoCompleteComponent;
import com.actelion.research.spiritapp.ui.util.component.LF;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.util.ui.JCustomTextField;

public class MonitorTextField extends JPanel {

	private final Result result;
	private JComponent editor;
	private TestAttribute ta;
	private final int outputNo;
	private boolean required;

	public MonitorTextField(Result result, int outputNo, boolean required) {
		super(new GridLayout());
		this.ta = result==null || result.getTest()==null || outputNo<0 || outputNo>=result.getTest().getOutputAttributes().size()? null: result.getTest().getOutputAttributes().get(outputNo);
		this.result = result;
		this.outputNo = outputNo;

		if(result==null) {
			//Empty Panel
			return;
		}

		if(ta==null) {
			editor = new JLabel("Invalid Att");
			editor.setToolTipText(""+result+" / "+outputNo);
		} else {
			editor = MetadataComponentFactory.getComponentFor(ta.getDataType(), ta.getParameters());
			if(editor instanceof AutoCompleteComponent) {
				((AutoCompleteComponent) editor).setChoices(DAOTest.getAutoCompletionFields(ta));
			}
			if(editor instanceof JCustomTextField) {
				boolean hasData = result!=null && result.getBiosample()!=null;
				if(ta!=null && ta.getDataType()==DataType.FORMULA) {
					editor.setForeground(Color.BLUE);
					((JCustomTextField)editor).setBorderColor(Color.LIGHT_GRAY);
				} else {
					((JCustomTextField)editor).setBorderColor(Color.GRAY);
					((JCustomTextField)editor).setEnabled(hasData);
					((JCustomTextField)editor).setWarningWhenEdited(hasData);
					((JCustomTextField)editor).addFocusListener(new AutoScrollFocusListener());
				}
			} else {
				editor.setEnabled(false);
			}
			setRequired(required);
		}

		//Be sure to refresh the text before setting the textchangelistener
		refreshText();

		if(editor instanceof JCustomTextField) {
			((JCustomTextField)editor).addTextChangeListener(new MonitorTextChangeListener(result, outputNo));
		}
		add(editor);
	}

	@Override
	public boolean requestFocusInWindow() {
		if(editor instanceof JCustomTextField) {
			return ((JCustomTextField)editor).requestFocusInWindow();
		} else {
			return super.requestFocusInWindow();
		}
	}

	@Override
	public synchronized void addFocusListener(FocusListener l) {
		if(editor instanceof JCustomTextField) {
			((JCustomTextField)editor).addFocusListener(l);
		} else {
			super.addFocusListener(l);
		}
	}

	public void refreshText() {
		if(result!=null && result.getBiosample()!=null && ta!=null && result.getResultValue(ta)!=null) {
			if(editor instanceof JCustomTextField) {
				((JCustomTextField)editor).setText(result.getResultValue(ta).getValue());
			}

			String tooltip = "<html><b>"+result.getBiosample().getTopParentInSameStudy().getSampleIdName() + "</b>:<br>" +
					(ta==null? "": ta.getName()) +
					(result.getPhase()==null? "": " " + result.getPhase().toString()) +
					(ta!=null && ta.getDataType()==DataType.FORMULA? "<br>=" + ta.getParameters(): "");

			if(result.getId()>0) {
				tooltip += "<br>Last value: "+ MonitoringCagePanel.formatTooltipText(result.getOutputResultValues().get(outputNo).getValue(), result.getUpdUser(), result.getUpdDate());
			}
			editor.setToolTipText(tooltip);
		}
	}

	public void addActionListener(ActionListener al) {
		if(editor instanceof JCustomTextField) {
			((JCustomTextField)editor).addActionListener(al);
		}
	}

	public boolean isRequired() {
		return required;
	}

	public String getText() {
		if(editor instanceof JCustomTextField) {
			return ((JCustomTextField)editor).getText();
		} else {
			assert false;
			return null;
		}
	}

	public Double getTextDouble() {
		if(editor instanceof JCustomTextField) {
			return ((JCustomTextField)editor).getTextDouble();
		} else {
			assert false;
			return null;
		}
	}

	public void setRequired(boolean required) {
		this.required = required;
		editor.setBackground(this.required? LF.BGCOLOR_REQUIRED: null);
	}

}
