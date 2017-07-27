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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.actelion.research.spiritapp.animalcare.ui.monitor.MonitoringHelper.MonitoringStats;
import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.pivot.graph.GraphPanelWithResults;
import com.actelion.research.spiritapp.spirit.ui.study.StudyActions;
import com.actelion.research.spiritapp.spirit.ui.util.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.FoodWater;
import com.actelion.research.spiritcore.business.pivot.MonitorPivotTemplate;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOFoodWater;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

public class MonitoringOverviewDlg extends JEscapeDialog implements ISpiritChangeObserver {

	private final Study s;

	private final JPanel contentPanel = new JPanel(new BorderLayout());

	public MonitoringOverviewDlg(Study s) throws Exception {
		super(UIUtils.getMainFrame(), "Live Monitoring - " +(s==null?"": s.getStudyId()) );
		if (s == null) throw new Exception("You must select a study");
		this.s = s;

		//		SpiritChangeListener.register(this);
		setContentPane(contentPanel);
		UIUtils.adaptSize(this, 1500, 1200);
		recreateUIInThread();
		setVisible(true);
	}

	public static List<Result> extractResults(Collection<Biosample> biosamples, Phase phase) {
		List<Result> res = new ArrayList<>();
		for (Biosample b : biosamples) {
			res.addAll(b.getAuxResults((Test) null, phase));
		}
		Collections.sort(res);
		return res;
	}

	private void recreateUIInThread()  {
		System.out.println("MonitoringOverviewDlg.recreateUIInThread()");

		new SwingWorkerExtended("Loading", contentPanel, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {

			private List<Biosample> animals;
			private List<Result> results;
			private Map<Test, List<Result>> mapResults;
			private List<Test> tests;
			private List<FoodWater> allFws;
			private Date now = JPAUtil.getCurrentDateFromDatabase();
			private Study study;

			@Override
			protected void doInBackground() throws Exception {
				System.out.println("MonitoringOverviewDlg.recreateUIInThread().new SwingWorkerExtended() {...}.doInBackground()");
				// Load animals
				study = JPAUtil.reattach(s);
				animals = study.getTopAttachedBiosamples();

				//Load Results (except foodwater which is loaded separately)
				DAOResult.attachOrCreateStudyResultsToTops(study, animals, null, null);
				results = extractResults(animals, null);
				mapResults = Result.mapTest(results);
				tests = new ArrayList<>();
				for (Test t : mapResults.keySet()) {
					if(t.getName().equals(DAOTest.FOODWATER_TESTNAME)) continue;
					tests.add(t);
				}
				Collections.sort(tests);

				//Load FWs
				allFws = DAOFoodWater.getFoodWater(study, null);
			}

			@Override
			protected void done() {
				try {

					/////////////////////////////////////////////////////////////////////////////////////////////////////////////
					// DisplayPane
					// Loop through each phase and display the actions
					List<JComponent> comps = new ArrayList<>();
					JButton nowButton = null;
					Set<Phase> phasesWithGroupAssignments =  study.getPhasesWithGroupAssignments();
					for (final Phase phase : study.getPhases()) {


						MonitoringStats fwStats = MonitoringHelper.calculateDoneRequiredFW(Biosample.getContainers(animals), phase, allFws);
						Map<Test, MonitoringStats> stats = new LinkedHashMap<>();
						for (Test test : tests) {
							stats.put(test, MonitoringHelper.calculateDoneRequiredTest(animals, phase, test, mapResults.get(test)));
						}

						//Is everything done?
						List<MonitoringStats> allStats = new ArrayList<>();
						allStats.add(fwStats);
						allStats.addAll(stats.values());

						//Create button
						JButton liveButton = new JButton("<html><span style='font-size:90%;font-weight:bold;'>" + phase.getShortName() + "</span><br>"
								+ "<span style='font-size:80%'>" + (phase.getAbsoluteDate() == null ? "" :FormatterUtils.formatDate(phase.getAbsoluteDate())) + "</span><br>");
						if (Phase.isSameDay(phase.getAbsoluteDate(), now)) liveButton.setBackground(LF.BGCOLOR_TODAY);
						if (phase.getAbsoluteDate() != null && phase.getAbsoluteDate().before(now)) nowButton = liveButton;

						liveButton.setIcon(MonitoringStats.getIconType(allStats).getIcon());
						liveButton.setHorizontalAlignment(SwingConstants.LEFT);
						liveButton.setPreferredSize(new Dimension(92, 44));
						liveButton.addActionListener(e-> {
							new MonitoringDlg(phase);
							Spirit.clearAll();
							recreateUIInThread();
						});

						StringBuilder statsBuilder = new StringBuilder();
						if(fwStats.nMade>0 || fwStats.nRequired>0) {
							statsBuilder.append(fwStats.getText("FoodWater")+"<br>");
						}
						for (Test test : new TreeSet<>(stats.keySet())) {
							MonitoringStats s = stats.get(test);
							if(s.nMade>0 || s.nRequired>0) {
								statsBuilder.append(s.getText(test.getName())+"<br>");
							}
						}

						if(phasesWithGroupAssignments.contains(phase)) {
							JButton b = new JButton(new StudyActions.Action_GroupAssignment(phase));
							b.setText("<html>Group<br>Assign.");
							b.setIcon(null);
							comps.add(b);
						} else {
							comps.add(null);
						}

						comps.add(liveButton);
						comps.add(new JLabel("<html><span style='font-size:90%;white-space:nowrap'>"+phase.getDescription().replace(" + ", " +<br>")));
						comps.add(new JLabel("<html><div style='font-size:90%'>"+statsBuilder));
					}

					// ContentPane
					JPanel displayPane = UIUtils.createTable(4, 2, 0, comps);
					final JScrollPane displaySp = new JScrollPane(displayPane);
					displaySp.setPreferredSize(new Dimension(400, 500));

					GraphPanelWithResults graphPanel = new GraphPanelWithResults();
					graphPanel.setPivotTemplate(new MonitorPivotTemplate());

					JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, displaySp, graphPanel);
					splitPane.setDividerLocation(380);
					contentPanel.removeAll();
					contentPanel.add(BorderLayout.CENTER, splitPane);
					contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), new JButton(new CloseAction())));
					contentPanel.validate();
					contentPanel.repaint();

					final JComponent comp = nowButton;
					SwingUtilities.invokeLater(()-> {
						//Scroll to current date
						if (comp != null) {
							Point pt = SwingUtilities.convertPoint(comp, 0, 0, displaySp);
							displaySp.getVerticalScrollBar().setValue(pt.y-displaySp.getHeight()/2);
						}
						graphPanel.setResults(results);
					});
				} catch(Exception e) {
					JExceptionDialog.showError(e);
				}
			}

		};
	}

	@Override
	public <T> void actionModelChanged(SpiritChangeType action, Class<T> what, Collection<T> details) {
		if(!isVisible()) return;
		recreateUIInThread();
	}
}
