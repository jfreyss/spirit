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

package com.actelion.research.spiritapp.ui.pivot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.HelpBinder;
import com.actelion.research.spiritapp.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.pivot.PivotDataTable;
import com.actelion.research.spiritcore.business.pivot.PivotItemFactory;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.spiritcore.business.pivot.analyzer.Analyzer;
import com.actelion.research.spiritcore.business.pivot.analyzer.Analyzer.Sort;
import com.actelion.research.spiritcore.business.pivot.analyzer.ColumnAnalyser;
import com.actelion.research.spiritcore.business.pivot.analyzer.ColumnAnalyser.Distribution;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorConfig;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorConfig.ChartType;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorExporter;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.UsageLog;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

public class PivotAnalyzerDlg extends JEscapeDialog {
	private Study study;
	private List<Result> results;
	//	private Set<TestAttribute> skippedAttributes;
	private ImageEditorPane editorPane = new ImageEditorPane();

	private PivotAnalyzerDlg() {
		super(UIUtils.getMainFrame(), "Analyze");

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		add(BorderLayout.CENTER, new JScrollPane(editorPane));
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), closeButton));
		setSize(1050, 740);
		setLocationRelativeTo(UIUtils.getMainFrame());

	}
	public PivotAnalyzerDlg(Study study) {
		this();

		this.study = study;
		analyze();
		setVisible(true);
	}
	public PivotAnalyzerDlg(List<Result> results) {
		this();

		this.results = results;
		//		this.skippedAttributes = skippedAttributes;
		analyze();
		setVisible(true);
	}

	public void analyze() {
		if(DBAdapter.getInstance().isInActelionDomain()) UsageLog.logUsage("Spirit", SpiritFrame.getUsername(), null, "Analyze", study==null?"": study.getStudyId());

		//Analyze in a thread
		new SwingWorkerExtended("Analyzing", editorPane) {

			private PivotDataTable statsTable;
			private Analyzer analyzer;
			private String report;

			@Override
			protected void doInBackground() throws Exception {

				if(results==null && study!=null) {
					ResultQuery q = new ResultQuery();
					q.setSid(study.getId());
					results = DAOResult.queryResults(q, SpiritFrame.getUser());
				}
				if(results==null) throw new Exception("There are no results");

				//Initializes the analyser
				analyzer = new Analyzer(results, SpiritFrame.getUser());

				//Creates the report
				try {
					report = analyzer.getReport();
				} catch(Exception e) {
					e.printStackTrace();
					report = "<div style='color:red'>Error: " + e.getMessage() + "</div>";
				}
			}

			@Override
			protected void done() {
				editorPane.addHyperlinkListener(new HyperlinkListener() {
					@Override
					public void hyperlinkUpdate(HyperlinkEvent e) {
						if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
							try {
								if(e.getDescription().equals("sort:name")) {
									analyzer.setSort(Sort.NAME);
								} else if(e.getDescription().equals("sort:groups")) {
									analyzer.setSort(Sort.GROUPS);
								} else if(e.getDescription().equals("sort:N")) {
									analyzer.setSort(Sort.N);
								} else if(e.getDescription().equals("sort:distrib")) {
									analyzer.setSort(Sort.DISTRIB);
								} else if(e.getDescription().equals("sort:K")) {
									analyzer.setSort(Sort.KW);
								} else if(e.getDescription().startsWith("graphs:") || e.getDescription().startsWith("pca:")) {
									boolean pca = e.getDescription().startsWith("pca:");
									List<Integer> ids;
									if(pca) {
										ids = MiscUtils.splitIntegers(e.getDescription().substring("pca:".length()));
										statsTable.getTemplate().setWhere(PivotItemFactory.STUDY_PHASE_DATE, Where.MERGE);
									} else {
										ids = MiscUtils.splitIntegers(e.getDescription().substring("graphs:".length()));
									}
									List<Result> results = statsTable.getResults();

									if(ids.size()==0) return;

									//Create DW Config
									ColumnAnalyser ca = analyzer.getColumnFromIndex(ids.get(0));
									DataWarriorConfig config = new DataWarriorConfig();
									config.setType(ChartType.BOXPLOT);
									config.setLogScale(ca.getDistribution()==Distribution.LOGNORMAL);
									//									config.setSkippedAttributes(statsTable.getSkippedAttributes());
									config.setCustomTemplate(statsTable.getTemplate());
									config.setXAxis(null); //automatic
									config.setExportAll(!pca);

									List<String> allViewNames = DataWarriorExporter.getViewNames(results, config, SpiritFrame.getUser());

									//Select views
									List<String> views = new ArrayList<>();
									for (int id : ids) {
										if(id>=0 && id<allViewNames.size()) views.add(allViewNames.get(id));
									}
									config.setViewNames(views);

									//Export to DW
									StringBuilder sb = DataWarriorExporter.getDwar(results, config, SpiritFrame.getUser());
									File f = File.createTempFile("spirit_", ".dwar");
									FileWriter w = new FileWriter(f);
									com.actelion.research.util.IOUtils.redirect(new StringReader(sb.toString()), w);
									w.close();

									Desktop.getDesktop().open(f);


									return;
								} else if(e.getDescription().startsWith("http")) {
									Desktop.getDesktop().browse(new URI(e.getDescription()));
								}
								setHtml(analyzer.getReport());
							} catch(Exception ex ) {
								JExceptionDialog.showError(ex);
							}
						}
					}
				});
				setHtml(report);
			}
		};
	}

	public void setHtml(String html) {
		createImages(html);
		editorPane.setText(html);
		editorPane.setCaretPosition(0);
	}


	private void createImages(String html) {
		Pattern pattern = Pattern.compile("<img src=['\"](.*?)['\"].*?>");
		Matcher matcher = pattern.matcher(html);
		while(matcher.find()) {
			final String url = matcher.group(1);
			if(url.startsWith("histo://") && editorPane.getImageCache().get(url)==null) {
				BufferedImage image = createHisto(url.substring("histo://".length()));
				editorPane.getImageCache().put(url, image);
			}
		}
	}

	private BufferedImage createHisto(String code) {
		int max = 5;

		BufferedImage img = new BufferedImage(4*code.length(), max*3, BufferedImage.TYPE_INT_RGB);
		Graphics g = img.getGraphics();
		g.setColor(UIUtils.getColor(240,240,240));
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.setColor(Color.DARK_GRAY);


		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);
			int n = c-'0';
			if(n<0 || n>=10) continue;

			g.fillRect(i*4, img.getHeight()-3*n, 4, 3*n);

		}
		g.dispose();
		return img;
	}
}
