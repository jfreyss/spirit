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

package com.actelion.research.spiritapp.spirit.ui.study;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.services.report.AbstractReport;
import com.actelion.research.spiritapp.spirit.services.report.MixedReport;
import com.actelion.research.spiritapp.spirit.services.report.ReportFactory;
import com.actelion.research.spiritapp.spirit.services.report.SpecimenFoodWaterReport;
import com.actelion.research.spiritapp.spirit.services.report.AbstractReport.ReportCategory;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class ReportDlg extends JEscapeDialog {
	
	private final Study s;
	private final Map<AbstractReport, JCheckBox> report2checkboxes = new LinkedHashMap<AbstractReport, JCheckBox>();
	private final JPanel centerPanel = new JPanel(new GridLayout(1,1));
	private final JButton createReportsButton = new JIconButton(IconType.EXCEL, "Create Report");

	public ReportDlg(Study s) {		
		super(UIUtils.getMainFrame(), "Reports - " + s.getStudyId(), true);
		s = DAOStudy.getStudy(s.getId());
		this.s = s;
		
		if(!SpiritRights.canExpert(s, Spirit.getUser())) {
			JExceptionDialog.showError("You must have read rights on the study to view the reports");
			return;
		}
		if(SpiritRights.isBlind(s, Spirit.getUser())) {
			JExceptionDialog.showError("Blind users cannot view the reports");
			return;
		}

		//mixedReportsButton
		createReportsButton.setEnabled(false);
		createReportsButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					createReports();
				} catch(Exception ex) {
					JExceptionDialog.showError(ReportDlg.this, ex);
				}
			}
		});
		
		//create checkboxes
		for(ReportCategory cat: ReportCategory.values()) {
			for(final AbstractReport report: ReportFactory.get(cat)) {
				final JCheckBox cb = new JCheckBox();
				cb.addActionListener(new ActionListener() {					
					@Override
					public void actionPerformed(ActionEvent e) {
						refresh();
						cb.requestFocusInWindow();
						for (JCheckBox c : report2checkboxes.values()) {
							if(c.isSelected()) {
								createReportsButton.setEnabled(true);
								return;
							}
						}
						createReportsButton.setEnabled(false);
					}
				});
				report2checkboxes.put(report, cb);
			}
		}
		
		
		refresh();
		

		//Layout
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, centerPanel);		
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), createReportsButton));
		setContentPane(contentPane);
		
		setSize(900, 700);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}
	
	private void refresh() {
		//mainPanel
		List<Component> comps = new ArrayList<Component>();
		for(ReportCategory cat: ReportCategory.values()) {
			List<JPanel> panels = new ArrayList<JPanel>();
			for(final AbstractReport report: ReportFactory.get(cat)) {
				JCheckBox cb = report2checkboxes.get(report);
				panels.add(ReportFactory.createReportPanel(report, s, cb));
			}
			comps.add(UIUtils.createBox(BorderFactory.createRaisedBevelBorder(), 
					UIUtils.createVerticalBox(panels.toArray(new JPanel[0])), 
					null, null, UIUtils.createVerticalTitlePanel(cat.toString()),null));

		}
		centerPanel.removeAll();
		centerPanel.add(new JScrollPane(
				UIUtils.createBox(Box.createVerticalGlue(), UIUtils.createVerticalBox(comps.toArray(new Component[0])), null),
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
				);
		centerPanel.validate();
		
		
	}

	private void createReports() throws Exception {
		List<AbstractReport> reports = new ArrayList<AbstractReport>();
		for(AbstractReport report: report2checkboxes.keySet()) {
			if(!report2checkboxes.get(report).isSelected()) continue;
			reports.add(report);
		}
		MixedReport rep = new MixedReport(reports);
		rep.populateReport(s);
		rep.export(null);
	}
	
	
	public static void main(String[] args) throws Exception {
		Spirit.initUI();
		Spirit.setUser(DAOSpiritUser.loadUser("freyssj"));
		Study s = DAOStudy.getStudyByStudyId("S-00515");
//		new ReportDlg(s);
		
		
		List<AbstractReport> reports = new ArrayList<AbstractReport>();
		reports.add(new SpecimenFoodWaterReport());

		MixedReport rep = new MixedReport(reports);
		rep.populateReport(s);
		rep.export(null);

	}
}
