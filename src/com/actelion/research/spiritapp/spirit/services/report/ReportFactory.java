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

package com.actelion.research.spiritapp.spirit.services.report;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.services.report.AbstractReport.ReportCategory;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.UIUtils;


/**
 * Class used to retrieve 
 * @author freyssj
 *
 */
public class ReportFactory {

	public static AbstractReport[] REPORTS = new AbstractReport[] {
		new StudyDesignReport(),
		new StudyGroupAssignmentReport(),
		new SpecimenStatusReport(),
		
		new SpecimenWeighingToxicologyReport(),
		new SpecimenWeighingInVivoReport(),
		new SpecimenFoodWaterReport(),
		
		new SamplesToxicologyReport(),
		new SamplesInVivoReport(),
		new SamplesMeasurementReport(),		
		new SamplesLocationReport()
	};
	
	public static List<AbstractReport> get(ReportCategory cat){
		List<AbstractReport> res = new ArrayList<AbstractReport>();
		for (AbstractReport r : REPORTS) {
			if(r.getCategory()==cat) res.add(r);
		}
		return res;
	}
	
	public static JPanel createReportPanel(final AbstractReport report, final Study study, final JCheckBox checkBox) {
		
		JPanel extraPanel = report.getExtraParameterPanel(study);

		
		ReportParameter[] parameters = report.getReportParameters();
		List<Component> reportPanels = new ArrayList<Component>();
		
		//Create the Panel for the different options
		for (int j = 0; j < parameters.length; j++) {
			final ReportParameter parameter = parameters[j];
			if(parameter.getDefaultValue().getClass()==Boolean.class) {
				//Boolean parameters are converted to a JCheckbox
				final JCheckBox cb = new JCheckBox(parameter.getLabel(), (Boolean) parameter.getDefaultValue());
				cb.addActionListener(new ActionListener() {						
					@Override
					public void actionPerformed(ActionEvent e) {
						report.setParameter(parameter, cb.isSelected());
					}
				});
				reportPanels.add(UIUtils.createHorizontalBox(cb, Box.createHorizontalGlue()));
			} else if(parameter.getValues()!=null && parameter.getValues().length>0) {
				//Object[] parameters are converted to a JComboBox
				final JComboBox<?> comboBox = new JComboBox<Object>(parameter.getValues());
				comboBox.setSelectedItem(parameter.getDefaultValue());
				comboBox.addActionListener(new ActionListener() {						
					@Override
					public void actionPerformed(ActionEvent e) {
						report.setParameter(parameter, comboBox.getSelectedItem());
					}
				});
				reportPanels.add(UIUtils.createHorizontalBox(comboBox, Box.createHorizontalGlue()));
			} else {
				reportPanels.add(new JLabel("invalid parameter: "+parameter));				
			}
		} 
		
		//Add the custom parameters
		if(extraPanel!=null) {
			JScrollPane sp = new JScrollPane(extraPanel);			
			sp.setPreferredSize(new Dimension(Math.min(extraPanel.getPreferredSize().width+10, 208), Math.min(extraPanel.getPreferredSize().height+10, 200)));
			reportPanels.add(sp);				
		}
				
		//Add the description
		final JEditorPane editorPane = new JEditorPane("text/html", report.getDescription()==null?"":report.getDescription());
		editorPane.setEditable(false);
		editorPane.setCaretPosition(0);
		editorPane.setOpaque(false);
		editorPane.setVisible(report.getDescription()!=null);
		
		//Create the ReportPanel (open only if selected)
		JPanel panel = UIUtils.createBox(editorPane, UIUtils.createHorizontalTitlePanel(checkBox, report.getName(), checkBox.isSelected()? UIUtils.getColor(114, 160, 193): Color.LIGHT_GRAY));
		if(checkBox.isSelected()) {
			panel = UIUtils.createBox(BorderFactory.createEtchedBorder(), reportPanels.size()==0? null: UIUtils.createVerticalBox(BorderFactory.createEmptyBorder(2, 1, 1, 10), UIUtils.createVerticalBox(reportPanels.toArray(new Component[0]))), panel);		
		} else{
			panel = UIUtils.createBox(BorderFactory.createEtchedBorder(), panel);
		}
		
		
		return panel;
	}
	
}
