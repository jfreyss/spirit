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

package com.actelion.research.spiritapp.ui.study;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;

import com.actelion.research.spiritapp.report.AbstractReport;
import com.actelion.research.spiritapp.report.MixedReport;
import com.actelion.research.spiritapp.report.ReportFactory;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class ReportDlg extends JEscapeDialog {

	private final Study s;
	private final ReportFactory reportFactory = ReportFactory.getInstance();
	private JList<AbstractReport> reportList;
	private final JPanel detailPanel = new JPanel(new BorderLayout());
	private final JButton excelButton = new JIconButton(IconType.EXCEL, "Excel Report");
	private final JButton pdfButton = new JIconButton(IconType.PDF, "PDF Report");
	private Map<AbstractReport, JPanel> rep2panel = new HashMap<>();

	public ReportDlg(Study s) {
		super(UIUtils.getMainFrame(), "Study Reports - " + (s==null?"":s.getStudyId()), true);
		if(s==null) throw new IllegalArgumentException("The study cannot be null");
		s = DAOStudy.getStudy(s.getId());
		this.s = s;

		if(!SpiritRights.canWork(s, SpiritFrame.getUser())) {
			JExceptionDialog.showError("You must have read rights on the study to view the reports");
			return;
		}
		if(SpiritRights.isBlind(s, SpiritFrame.getUser())) {
			JExceptionDialog.showError("Blind users cannot view the reports");
			return;
		}

		//mixedReportsButton
		excelButton.setEnabled(false);
		excelButton.addActionListener(e-> {
			try {
				createReports(false);
			} catch(Exception ex) {
				JExceptionDialog.showError(ReportDlg.this, ex);
			}
		});

		pdfButton.setEnabled(false);
		pdfButton.addActionListener(e-> {
			try {
				createReports(true);
			} catch(Exception ex) {
				JExceptionDialog.showError(ReportDlg.this, ex);
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
		reportList.addListSelectionListener(e-> {
			excelButton.setEnabled(reportList.getSelectedValuesList().size()>0);
			pdfButton.setEnabled(reportList.getSelectedValuesList().size()>0);
			refresh();
		});
		refresh();

		JSplitPane splitPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT,
				UIUtils.createTitleBox(UIUtils.createBox(
						new JScrollPane(reportList),
						new JInfoLabel("<html>Please select one or several reports from the list."))),
				detailPanel);
		splitPane.setDividerLocation(300);
		setContentPane(UIUtils.createBox(splitPane, null, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), excelButton, pdfButton)));

		UIUtils.adaptSize(this, 900, 700);
		setVisible(true);
	}

	private void refresh() {
		detailPanel.removeAll();
		if(reportList.getSelectedValuesList().size()==1) {
			AbstractReport rep = reportList.getSelectedValuesList().get(0);
			JPanel reportPanel = rep2panel.get(rep);
			if(reportPanel==null) {
				rep2panel.put(rep, reportPanel = ReportFactory.createReportPanel(rep, s));
			}
			detailPanel.add(reportPanel);
		} else {
		}
		detailPanel.validate();
		detailPanel.repaint();
	}

	private void createReports(boolean pdf) throws Exception {
		MixedReport rep = new MixedReport(reportList.getSelectedValuesList());
		rep.populateReport(s);
		if(pdf) {
			rep.exportPDF(null);
		} else {
			rep.export(null);
		}
	}

}
