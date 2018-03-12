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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOExchange;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping.EntityAction;
import com.actelion.research.spiritcore.services.exchange.Importer;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;

public class ImporterDlg extends JEscapeDialog {

	private Exchange exchange;

	private ExchangeMapping mapping;

	private List<IMappingPanel> mappingPanels = new ArrayList<>();
	private List<StudyMappingPanel> studyMappingPanels = new ArrayList<>();
	private List<BiotypeMappingPanel> biotypeMappingPanels = new ArrayList<>();
	private List<BiosampleMappingPanel> biosampleMappingPanels = new ArrayList<>();

	private LocationMappingPanel locationMappingPanel;

	private List<TestMappingPanel> testMappingPanels = new ArrayList<>();
	private List<ResultMappingPanel> resultMappingPanels = new ArrayList<>();


	public ImporterDlg(File file) {
		super(UIUtils.getMainFrame(), "Import Data", true);
		JTabbedPane tabbedPane = new JCustomTabbedPane();

		//Read exchange file
		try(FileReader reader = new FileReader(file)) {
			exchange = Importer.read(reader);
		} catch (Exception e) {
			JExceptionDialog.showError(e);
			return;
		}

		//Prepare panel for batch action
		JRadioButton ignoreAdminButton = new JRadioButton("Ignore / don't import", false);
		JRadioButton createAdminButton = new JRadioButton("Create / Import", true);

		JRadioButton keepExistingButton = new JRadioButton("Keep the existing entity / ignore", true);
		JRadioButton replaceExistingButton = new JRadioButton("Replace", false);
		JRadioButton cloneExistingButton = new JRadioButton("Create a copy of the entity", false);

		ButtonGroup buttonGroup1 = new ButtonGroup();
		buttonGroup1.add(ignoreAdminButton);
		buttonGroup1.add(createAdminButton);

		ButtonGroup buttonGroup2 = new ButtonGroup();
		buttonGroup2.add(keepExistingButton);
		buttonGroup2.add(replaceExistingButton);
		buttonGroup2.add(cloneExistingButton);
		JPanel batchPanel = UIUtils.createGrid(
				UIUtils.createVerticalBox(new JCustomLabel("What do you want to do for the new configuration (biotypes/tests)?", FastFont.BOLD), ignoreAdminButton, createAdminButton),
				UIUtils.createVerticalBox(new JCustomLabel("What do you want to do for the existing entities (studies/biosamples/locations/results)?", FastFont.BOLD), keepExistingButton, replaceExistingButton, cloneExistingButton));

		ActionListener al = e-> {
			new SwingWorkerExtended(tabbedPane, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
				@Override
				public void doInBackground() {
					mapping.initializeMappingFromDb(
							ignoreAdminButton.isSelected()? EntityAction.SKIP: EntityAction.CREATE,
									keepExistingButton.isSelected()? EntityAction.SKIP: replaceExistingButton.isSelected()? EntityAction.MAP_REPLACE: EntityAction.CREATE);
				}
				@Override
				public void done() {
					updateView();
				}
			};
		};

		ignoreAdminButton.setSelected(true);
		keepExistingButton.setSelected(true);
		ignoreAdminButton.addActionListener(al);
		createAdminButton.addActionListener(al);
		keepExistingButton.addActionListener(al);
		replaceExistingButton.addActionListener(al);
		cloneExistingButton.addActionListener(al);

		setContentPane(
				UIUtils.createBox(
						UIUtils.createTitleBox("Entities to be imported", tabbedPane),
						UIUtils.createTitleBox("Import Default Action ", batchPanel),
						UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new ImportAction()))
						));
		UIUtils.adaptSize(this, 1200, 1000);
		setLocationRelativeTo(UIUtils.getMainFrame());

		new SwingWorkerExtended(this, SwingWorkerExtended.FLAG_ASYNCHRONOUS) {
			@Override
			protected void doInBackground() throws Exception {
				mapping = new ExchangeMapping(exchange, EntityAction.SKIP, EntityAction.SKIP);
				System.out.println("ImporterDlg.ImporterDlg() Inital Mapping= "+mapping.getStudyId2mappedStudy());

			}

			@Override
			protected void done() {
				System.out.println("ImporterDlg.ImporterDlg() mapping.hasNewTypes= "+mapping.hasNewTypes());
				if(!mapping.hasNewTypes()) {
					ignoreAdminButton.setEnabled(false);
					createAdminButton.setEnabled(false);
				}

				if(!mapping.hasExistingEntities()) {
					keepExistingButton.setEnabled(false);
					replaceExistingButton.setEnabled(false);
					cloneExistingButton.setEnabled(false);
					replaceExistingButton.setSelected(true);
				}

				//Prepare panels for studies
				if(exchange.getStudies()!=null && exchange.getStudies().size()>0) {
					List<Component> studyPanels = new ArrayList<>();

					for (final Study study : exchange.getStudies()) {
						StudyMappingPanel studyMappingPanel = new StudyMappingPanel(ImporterDlg.this, study);
						studyMappingPanels.add(studyMappingPanel);
						studyPanels.add(UIUtils.createTitleBox(study.getStudyIdAndInternalId(), studyMappingPanel));
					}
					studyPanels.add(Box.createVerticalGlue());
					tabbedPane.add("Studies", new JScrollPane(UIUtils.createVerticalBox(studyPanels)));
				}

				//Prepare panels for biotypes/biosamples
				if(exchange.getBiotypes()!=null && exchange.getBiotypes().size()>0) {
					List<Component> biotypePanels = new ArrayList<>();
					for (final Biotype biotype : exchange.getBiotypes()) {
						//Init BiotypeMappingPanel
						BiotypeMappingPanel biotypeMappingPanel = new BiotypeMappingPanel(ImporterDlg.this, biotype);
						biotypeMappingPanels.add(biotypeMappingPanel);

						//Display biosamples to be imported (information purpose only)
						BiosampleMappingPanel biosampleMappingPanel = new BiosampleMappingPanel(ImporterDlg.this, biotype, Biosample.filter(exchange.getBiosamples(), biotype));
						biosampleMappingPanel.setPreferredSize(biosampleMappingPanel.getMinimumSize());
						biosampleMappingPanels.add(biosampleMappingPanel);

						//Layout the BiotypePanel
						JSplitPane splitPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, biotypeMappingPanel, biosampleMappingPanel);
						splitPane.setDividerLocation(560);
						biotypePanels.add(UIUtils.createTitleBox("Biotype: " + biotype.getName(), splitPane));
					}
					biotypePanels.add(Box.createVerticalGlue());
					tabbedPane.add("Biotypes / Biosamples", new JScrollPane(UIUtils.createVerticalBox(biotypePanels)));

				}

				//Prepare panel for location
				if(exchange.getLocations()!=null && exchange.getLocations().size()>0) {
					locationMappingPanel = new LocationMappingPanel(ImporterDlg.this, exchange.getLocations());
					tabbedPane.add("Locations", new JScrollPane(UIUtils.createTitleBox("Location", locationMappingPanel)));
				}

				//Prepare panels for tests/results
				if(exchange.getTests()!=null && exchange.getTests().size()>0) {
					List<Component> testsPanels = new ArrayList<>();
					for (final Test test : exchange.getTests()) {
						//Init BiotypeMappingPanel
						TestMappingPanel testMappingPanel = new TestMappingPanel(ImporterDlg.this, test);
						testMappingPanels.add(testMappingPanel);

						//Display biosamples to be imported (information purpose only)
						ResultMappingPanel resultMappingPanel = new ResultMappingPanel(ImporterDlg.this, test, Result.filter(exchange.getResults(), test));
						resultMappingPanel.setPreferredSize(resultMappingPanel.getMinimumSize());
						resultMappingPanels.add(resultMappingPanel);

						//Layout the BiotypePanel
						JSplitPane splitPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, testMappingPanel, resultMappingPanel);
						splitPane.setDividerLocation(560);
						testsPanels.add(UIUtils.createTitleBox("Test: " + test.getFullName(), splitPane));
					}
					testsPanels.add(Box.createVerticalGlue());

					tabbedPane.add("Test / Results", new JScrollPane(UIUtils.createVerticalBox(testsPanels)));

				}

				if(tabbedPane.getTabCount()==0) {
					JExceptionDialog.showError(null, "The file is empty");
					return;
				}

				mappingPanels.addAll(studyMappingPanels);
				mappingPanels.addAll(biotypeMappingPanels);
				mappingPanels.addAll(biosampleMappingPanels);
				if(locationMappingPanel!=null) mappingPanels.add(locationMappingPanel);
				mappingPanels.addAll(testMappingPanels);
				mappingPanels.addAll(resultMappingPanels);


			}
		};

		setVisible(true);

	}


	public Exchange getExchange() {
		return exchange;
	}

	public void updateView() {
		for(IMappingPanel panel: mappingPanels) {
			panel.updateView();
		}
	}

	public void updateMapping() {
		for(IMappingPanel panel: mappingPanels) {
			panel.updateMapping();
		}
	}

	public class ImportAction extends AbstractAction {
		public ImportAction() {
			super("Import");
		}
		@Override
		public void actionPerformed(ActionEvent ev) {

			new SwingWorkerExtended("Importing", getContentPane(), SwingWorkerExtended.FLAG_ASYNCHRONOUS) {
				@Override
				protected void doInBackground() throws Exception {

					//Update the Mapping object
					updateMapping();

					//Save the mapped objects
					SpiritUser user = SpiritFrame.getUser();
					try {
						JPAUtil.pushEditableContext(SpiritFrame.getUser());

						DAOExchange.persist(mapping, user);

						SpiritChangeListener.fireModelChanged(SpiritChangeType.LOGIN);
						dispose();
					} finally {
						JPAUtil.popEditableContext();
					}
				}
			};

		}
	}

	public ExchangeMapping getMapping() {
		return mapping;
	}

}
