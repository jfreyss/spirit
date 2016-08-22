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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritapp.spirit.services.report.AbstractReport.ReportCategory;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.UIUtils;


/**
 * Class used to retrieve 
 * @author freyssj
 *
 */
public class ReportFactory {

	private static ReportFactory instance = null;
	private List<AbstractReport> reports = new ArrayList<>();
	
	public ReportFactory() {
		reports.add(new StudyDesignReport());
		reports.add(new StudyGroupAssignmentReport());
		reports.add(new SamplesLocationReport());
		reports.add(new SamplesMeasurementReport());
		try {
			reports.addAll(getAbstractReports("com.actelion.research.spiritapp.spirit.services.report.custom"));
		} catch(Exception e) {
			LoggerFactory.getLogger(getClass()).error("Could not retrieve custom reports", e);
		}		
		Collections.sort(reports, new Comparator<AbstractReport>() {
			@Override
			public int compare(AbstractReport o1, AbstractReport o2) {
				return o1.getCategory().compareTo(o2.getCategory());
			}
		});
	}
	
	public static ReportFactory getInstance() {
		if(instance==null) {
			instance = new ReportFactory();
		}
		return instance;
	}
	
	public List<AbstractReport> getReports() {
		return reports;
	}
	
	public List<AbstractReport> getReports(ReportCategory cat) {
		List<AbstractReport> res = new ArrayList<>();
		for (AbstractReport r : reports) {
			if(r.getCategory()==cat) res.add(r);
		}
		return res;
	}
	
	public static JPanel createReportPanel(final AbstractReport report, final Study study) {
		
		JPanel extraPanel = report.getExtraParameterPanel(study);

		
		ReportParameter[] parameters = report.getReportParameters();
		List<Component> reportPanels = new ArrayList<>();
		
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
			reportPanels.add(extraPanel);				
		}
		reportPanels.add(Box.createVerticalGlue());
				
		//Add the description
		final JEditorPane editorPane = new JEditorPane("text/html", report.getDescription()==null?"":report.getDescription());
		editorPane.setEditable(false);
		editorPane.setCaretPosition(0);
		editorPane.setOpaque(false);
		editorPane.setVisible(report.getDescription()!=null);
		JScrollPane sp = new JScrollPane(editorPane);
		sp.setPreferredSize(new Dimension(500, 170));
		//Create the ReportPanel (open only if selected)
		return UIUtils.createBox(
					new JScrollPane(UIUtils.createVerticalBox(reportPanels)), 
					UIUtils.createVerticalBox(
							new JCustomLabel(report.getCategory().getName() + " - " + report.getName(), FastFont.BIGGER),
							sp));		
	}


	private static List<AbstractReport> getAbstractReports(String packageName) throws Exception {
	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	    assert classLoader != null;
	    String path = packageName.replace('.', '/');
	    Enumeration<URL> resources = classLoader.getResources(path);
	    List<File> dirs = new ArrayList<>();
	    while (resources.hasMoreElements()) {
	        URL resource = resources.nextElement();
	        dirs.add(new File(resource.getFile()));
	    }
	    List<AbstractReport> res = new ArrayList<>();
	    for (File directory : dirs) {
	        for(Class<?> claz: findClasses(directory, packageName)) {
	        	if(AbstractReport.class.isAssignableFrom(claz)) {
	        		res.add((AbstractReport) claz.newInstance());
	        	}
	        }
	    }
	    LoggerFactory.getLogger(ReportFactory.class).info("Loaded "+res.size()+" custom reports.");
	    return res;
	}
	
	private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
	    List<Class<?>> classes = new ArrayList<Class<?>>();
	    if (!directory.exists()) {
	        return classes;
	    }
	    File[] files = directory.listFiles();
	    for (File file : files) {
	        if (file.isDirectory()) {
	            assert !file.getName().contains(".");
	            classes.addAll(findClasses(file, packageName + "." + file.getName()));
	        } else if (file.getName().endsWith(".class")) {
	            classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
	        }
	    }
	    return classes;
	}
	
}
