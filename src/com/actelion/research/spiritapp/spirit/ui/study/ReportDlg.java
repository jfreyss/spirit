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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.services.report.AbstractReport;
import com.actelion.research.spiritapp.spirit.services.report.MixedReport;
import com.actelion.research.spiritapp.spirit.services.report.ReportFactory;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class ReportDlg extends JEscapeDialog {
	
	private final Study s;
	private final ReportFactory reportFactory = ReportFactory.getInstance();
	private JList<AbstractReport> reportList;
	private final JPanel detailPanel = new JPanel(new BorderLayout());
	private final JButton createReportsButton = new JIconButton(IconType.EXCEL, "Create Report");

	public ReportDlg(Study s) {		
		super(UIUtils.getMainFrame(), "Reports - " + (s==null?"":s.getStudyId()), true);
		if(s==null) throw new IllegalArgumentException("The study cannot be null");
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
		
		
		//Layout
		reportList = new JList<>(new Vector<AbstractReport>(reportFactory.getReports()));
		reportList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {				
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				AbstractReport rep = (AbstractReport) value;
				setText((index+1)+". "+rep.getCategory().getName() + " - " + rep.getName());
				return this;
			}
		});
		reportList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		reportList.addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				createReportsButton.setEnabled(reportList.getSelectedValuesList().size()>0);
				refresh();
			}
		});
		refresh();
		
		JPanel contentPane = new JPanel(new BorderLayout());
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
				UIUtils.createTitleBox("Reports", UIUtils.createBox(
						new JScrollPane(reportList),
						new JInfoLabel("<html>Please select one or several reports from the list.<br>(Reports are configurable)"))),
				detailPanel);
		splitPane.setDividerLocation(300);
		contentPane.add(BorderLayout.CENTER, splitPane);		
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), createReportsButton));
		setContentPane(contentPane);
		
		setSize(900, 700);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}
	
	private void refresh() {
		detailPanel.removeAll();
		if(reportList.getSelectedValuesList().size()==1) {
			AbstractReport rep = reportList.getSelectedValuesList().get(0);
			detailPanel.add(ReportFactory.createReportPanel(rep, s));
		} else {
		}
		detailPanel.validate();
		
	}

	private void createReports() throws Exception {		
		MixedReport rep = new MixedReport(reportList.getSelectedValuesList());
		rep.populateReport(s);
		rep.export(null);
	}
	
}
