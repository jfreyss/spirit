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

package com.actelion.research.spiritapp.ui.exchange;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.component.JFileBrowser;
import com.actelion.research.spiritapp.ui.util.component.StudyComboBox;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.location.LocationQuery;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.business.study.StudyQuery;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.services.exchange.Exporter;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

public class ExporterDlg extends JEscapeDialog {

	private Exchange exchange;
	private final ExchangePanel panel = new ExchangePanel();
	private final Exchange currentView;
	private JFileBrowser fileBrowser = new JFileBrowser(null, "exporter.file", false);
	private StudyComboBox studyComboBox = new StudyComboBox();
	private JRadioButton exportCurrentViewRadioButton = new JRadioButton("Export all data from the current view");
	private JRadioButton exportStudyRadioButton = new JRadioButton("Export all entities from the following studies: ");
	private JRadioButton exportAdminRadioButton = new JRadioButton("Export all biotypes/tests (not hidden)");
	private JRadioButton exportAllRadioButton = new JRadioButton("Export the complete database");

	public ExporterDlg(Exchange currentView) {
		super(UIUtils.getMainFrame(), "Export Data", true);

		studyComboBox = new StudyComboBox(RightLevel.ADMIN);

		this.currentView = currentView;
		this.exchange = currentView;
		JButton okButton = new JButton("Export");
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					export();
					dispose();
					JExceptionDialog.showInfo(ExporterDlg.this, fileBrowser.getFile()+" saved ");
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});
		fileBrowser.setExtension(".spirit");

		ActionListener al = e-> {
			eventButtonClicked();
		};
		exportCurrentViewRadioButton.addActionListener(al);
		exportStudyRadioButton.addActionListener(al);
		exportAdminRadioButton.addActionListener(al);
		exportAllRadioButton.addActionListener(al);
		studyComboBox.addTextChangeListener(src-> {
			eventButtonClicked();
		});

		ButtonGroup group = new ButtonGroup();
		group.add(exportCurrentViewRadioButton);
		group.add(exportStudyRadioButton);
		group.add(exportAdminRadioButton);
		group.add(exportAllRadioButton);


		exportCurrentViewRadioButton.setEnabled(currentView!=null);
		if(currentView!=null) {
			exportCurrentViewRadioButton.setSelected(true);
		} else {
			exportStudyRadioButton.setSelected(true);
		}
		exportAdminRadioButton.setEnabled(SpiritFrame.getUser().isSuperAdmin());

		setContentPane(UIUtils.createBox(
				panel,
				UIUtils.createTitleBox("Query",
						UIUtils.createVerticalBox(
								UIUtils.createHorizontalBox(exportCurrentViewRadioButton, Box.createHorizontalStrut(26), Box.createHorizontalGlue()),
								UIUtils.createHorizontalBox(exportStudyRadioButton, studyComboBox, Box.createHorizontalStrut(26), Box.createHorizontalGlue()),
								UIUtils.createHorizontalBox(exportAdminRadioButton, Box.createHorizontalStrut(26), Box.createHorizontalGlue()),
								UIUtils.createHorizontalBox(exportAllRadioButton, Box.createHorizontalStrut(26), Box.createHorizontalGlue()))),
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JLabel("Destination File: "), fileBrowser, okButton)));
		UIUtils.adaptSize(this, 1000, 750);
		eventButtonClicked();
		setVisible(true);
	}

	private void eventButtonClicked() {
		new SwingWorkerExtended("Loading", getContentPane()) {
			private Exchange tmpExchange = null;
			@Override
			protected void doInBackground() throws Exception {
				SpiritUser user = SpiritFrame.getUser();
				String parent = new File(fileBrowser.getFile()).getParent();
				if(parent==null) parent = System.getProperty("user.home");
				studyComboBox.setEnabled(exportStudyRadioButton.isSelected());
				studyComboBox.setMultipleChoices(true);
				if(exportCurrentViewRadioButton.isSelected()) {
					if(currentView.getStudies()!=null && currentView.getStudies().size()==1) {
						fileBrowser.setFile(new File(parent, currentView.getStudies().iterator().next().getStudyId()+".spirit").getAbsolutePath());
					}
					this.tmpExchange = currentView;
				} else if(exportStudyRadioButton.isSelected()) {
					this.tmpExchange = new Exchange();
					if(studyComboBox.getText().trim().length()>0) {
						StudyQuery q = new StudyQuery();
						q.setStudyIds(studyComboBox.getText());
						tmpExchange.addStudies(DAOStudy.queryStudies(q, user));

						BiosampleQuery q2 = BiosampleQuery.createQueryForStudyIds(MiscUtils.flatten(Study.mapStudyId(tmpExchange.getStudies()).keySet(), " "));
						tmpExchange.addBiosamples(DAOBiosample.queryBiosamples(q2, user));

						ResultQuery q3 = ResultQuery.createQueryForStudyIds(MiscUtils.flatten(Study.mapStudyId(tmpExchange.getStudies()).keySet(), " "));
						tmpExchange.addResults(DAOResult.queryResults(q3, user));
					}
					fileBrowser.setFile(new File(parent, studyComboBox.getText()+".spirit").getAbsolutePath());

				} else if(exportAdminRadioButton.isSelected()) {
					fileBrowser.setFile(new File(parent, System.currentTimeMillis()+".spirit").getAbsolutePath());
					this.tmpExchange = new Exchange();
					tmpExchange.addBiotypes(DAOBiotype.getBiotypes());
					if(SpiritProperties.getInstance().isChecked(PropertyKey.TAB_RESULT)) {
						tmpExchange.addTests(DAOTest.getTests());
					}
				} else if(exportAllRadioButton.isSelected()) {
					fileBrowser.setFile(new File(parent, System.currentTimeMillis()+".spirit").getAbsolutePath());
					this.tmpExchange = new Exchange();
					List<Study> studies = DAOStudy.queryStudies(new StudyQuery(), user);
					if(studies.size()>10) throw new Exception("The number of studies to be exported is limited to 10");
					tmpExchange.addStudies(studies);
					tmpExchange.addBiosamples(DAOBiosample.queryBiosamples(new BiosampleQuery(), user));
					tmpExchange.addLocations(DAOLocation.queryLocation(new LocationQuery(), user));
					tmpExchange.addResults(DAOResult.queryResults(new ResultQuery(), user));
					tmpExchange.addBiotypes(DAOBiotype.getBiotypes());
					tmpExchange.addTests(DAOTest.getTests());
				}

			}
			@Override
			protected void done() {
				exchange = tmpExchange;
				panel.setExchange(tmpExchange);
			}
		};

	}


	private void export() throws Exception {
		//Check non emptyness
		if(exchange.isEmpty()) throw new Exception("The exchange file is empty");

		if(fileBrowser.getFile().length()==0) throw new Exception("You must enter a file");
		Writer writer = new BufferedWriter(new FileWriter(fileBrowser.getFile()));
		Exporter.write(exchange, writer);
		writer.close();
	}
}
