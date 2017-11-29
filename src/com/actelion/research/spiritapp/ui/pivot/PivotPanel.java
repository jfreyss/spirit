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

package com.actelion.research.spiritapp.ui.pivot;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Window;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.ui.pivot.designer.PivotTemplateDlg;
import com.actelion.research.spiritapp.ui.result.ResultActions;
import com.actelion.research.spiritapp.ui.result.ResultTable;
import com.actelion.research.spiritapp.ui.util.POIUtils;
import com.actelion.research.spiritapp.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.ui.util.POIUtils.ExportMode;
import com.actelion.research.spiritapp.ui.util.lf.JBGScrollPane;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.pivot.CompactPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.InventoryPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PerInputPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotDataTable;
import com.actelion.research.spiritcore.business.pivot.PivotItemFactory;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.pivot.analyzer.Analyzer;
import com.actelion.research.spiritcore.business.pivot.analyzer.ColumnAnalyser.Distribution;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorConfig;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorConfig.ChartType;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorExporter;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.util.IOUtils;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CSVUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.IExportable;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class PivotPanel extends JPanel {

	public static final String PROPERTY_PIVOT_CHANGED = "pivot_changed";
	private boolean pivotMode;
	private List<Result> results = new ArrayList<>();

	// CardPanel
	private CardLayout cardLayout = new CardLayout();
	private JPanel cardPanel = new JPanel(cardLayout);
	private JComponent tableTab = null;
	private final PivotTable pivotTable = new PivotTable();
	private JPanel templatePanel = UIUtils.createHorizontalBox();

	// Views
	private final JPanel viewPanel;
	private PivotTemplate currentPivotTemplate = null;

	private final ButtonGroup buttonGroup = new ButtonGroup();
	private final PivotTemplateButton setupButton = new PivotTemplateButton("Customize", IconType.SETUP.getIcon().getImage());

	// Buttons
	private final JCheckBox pivotButton = new JCheckBox("Pivot Data");
	//	private final JButton reportButton = new JIconButton(IconType.STATS, "Report");
	private final JButton dwButton = new JIconButton(IconType.DATAWARRIOR, "");
	private final JButton csvButton = new JIconButton(IconType.CSV, "");
	private final JButton excelButton = new JIconButton(IconType.EXCEL, "");

	// sublist
	private ResultTable subResultTable;
	private BiosampleTable subBiosampleTable;
	private BiosampleTabbedPane biosampleDetail;

	// Unique cutomize dialog
	private PivotTemplateDlg dlg;
	private PivotTemplate[] defaultTemplates;

	/**
	 * Create a panel for displaying pivoted results
	 *
	 * @param tableTab optional
	 * @param externalDetailPane
	 *            detailPanel to refresh when a biosample is selected, if null
	 *            the detailpanel will be created
	 */
	public PivotPanel(final JComponent tableTab, BiosampleTabbedPane externalDetailPane) {
		super(new BorderLayout());

		this.tableTab = tableTab;

		pivotButton.addActionListener(e-> {
			setPivotMode(pivotButton.isSelected());

			if (!pivotButton.isSelected() && dlg != null) {
				dlg.dispose();
			}

			PivotPanel.this.firePropertyChange(PROPERTY_PIVOT_CHANGED, null, "");
		});
		final JSplitPane centerPane;
		//		if(allDataMode) {
		/////////////////////////////////
		// Bottom
		JBGScrollPane subTableSp;
		if (tableTab==null) {
			subResultTable = new ResultTable();
			subResultTable.getModel().showAllHideable(true);
			subTableSp = new JBGScrollPane(subResultTable, 1);
			ResultActions.attachPopup(subResultTable);
			ResultActions.attachPopup(subTableSp);

			subResultTable.getSelectionModel().addListSelectionListener(e-> {
				if (!biosampleDetail.isVisible()) return;
				biosampleDetail.setBiosamples(Result.getBiosamples(subResultTable.getSelection()));
			});
		} else {
			subBiosampleTable = new BiosampleTable();
			subTableSp = new JBGScrollPane(subBiosampleTable, 1);

			BiosampleActions.attachPopup(subBiosampleTable);
			BiosampleActions.attachPopup(subTableSp);

			subBiosampleTable.getSelectionModel().addListSelectionListener(e-> {
				if (!biosampleDetail.isVisible()) return;
				biosampleDetail.setBiosamples(subBiosampleTable.getSelection());
			});
		}
		assert subBiosampleTable!=null || subResultTable!=null;

		JComponent subListPane;
		if (externalDetailPane == null) {
			biosampleDetail = new BiosampleTabbedPane();
			subListPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.HORIZONTAL_SPLIT, subTableSp, biosampleDetail);
			((JSplitPane) subListPane).setDividerLocation((UIUtils.getMainFrame() == null ? 1600 : UIUtils.getMainFrame().getWidth()) - 600);
		} else {
			biosampleDetail = externalDetailPane;
			subListPane = subTableSp;
		}

		// CenterPane
		centerPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.VERTICAL_SPLIT, new JScrollPane(pivotTable), subListPane);
		centerPane.setDividerLocation(10000);

		// pivotTable Listeners
		ListSelectionListener listener = e -> {
			if (e.getValueIsAdjusting() || biosampleDetail==null || !biosampleDetail.isVisible()) return;
			final List<Result> results = pivotTable.getSelectedResults();
			if (tableTab==null) {
				subResultTable.setRows(results);
			} else {
				List<Biosample> list = new ArrayList<>(Result.getBiosamples(results));
				Collections.sort(list);
				subBiosampleTable.setRows(list);
			}

			if (results.size()==0) {
				centerPane.setDividerLocation(1.0);
			} else  if (results.size() > 0 && centerPane.getDividerLocation()>centerPane.getHeight()-205) {
				int tableHeight = (subBiosampleTable==null? subResultTable.getPreferredSize().height: subBiosampleTable.getPreferredSize().height);
				centerPane.setDividerLocation(centerPane.getHeight() - Math.min(200, 45 + tableHeight));
			}

			// Check selected biosamples
			final Set<Biosample> biosamples = new HashSet<>();
			for (Result r : results) {
				if (r.getBiosample() != null) {
					biosamples.add(r.getBiosample());
				}
			}
			biosampleDetail.setBiosamples(biosamples);

			Collection<Biosample> list = pivotTable.getHighlightedSamples();
			if (list.size() > 0) {
				biosampleDetail.setBiosamples(list);
			}
		};
		pivotTable.getSelectionModel().addListSelectionListener(listener);
		pivotTable.getColumnModel().getSelectionModel().addListSelectionListener(listener);

		// Init cardPanel
		if (tableTab != null) {
			cardPanel.add("table", tableTab);
		}
		cardPanel.add("pivot", centerPane);

		// init templates
		if (tableTab==null) {
			defaultTemplates = new PivotTemplate[] { new CompactPivotTemplate(), /*new PerGroupPivotTemplate(),*/ new PerInputPivotTemplate() };
		} else {
			defaultTemplates = new PivotTemplate[] { new InventoryPivotTemplate() };
		}

		viewPanel = UIUtils.createTitleBoxSmall("Format", templatePanel);
		initTemplates();

		// TopPanel
		{
			dwButton.setFont(FastFont.SMALL);
			csvButton.setFont(FastFont.SMALL);
			excelButton.setFont(FastFont.SMALL);
			dwButton.addActionListener(e -> exportToDw());
			csvButton.addActionListener(e-> exportToExcel(true));
			excelButton.addActionListener(e -> exportToExcel(false));
		}
		add(BorderLayout.NORTH, UIUtils.createHorizontalBox(
				(tableTab != null?pivotButton:null),
				viewPanel,
				Box.createHorizontalGlue(),
				UIUtils.createTitleBoxSmall("Export", UIUtils.createHorizontalBox(dwButton, csvButton, excelButton))));
		add(BorderLayout.CENTER, cardPanel);

		// Add buttonlistener
		setupButton.addActionListener(e-> {
			setupButton.setSelected(true);
			openEditTemplateDlg();
		});

		setPivotMode(tableTab == null);
		refresh();

	}

	private void initTemplates() {
		// Init buttons
		templatePanel.removeAll();
		for (final PivotTemplate template : defaultTemplates) {
			final PivotTemplateButton button = new PivotTemplateButton(template);
			button.addActionListener(e-> {
				firePropertyChange(PROPERTY_PIVOT_CHANGED, "", null);
				setPivotTemplate(button.getPivotTemplate());
				button.setSelected(true);
			});

			templatePanel.add(button);
			buttonGroup.add(button);

			if (currentPivotTemplate == null) {
				setPivotTemplate(button.getPivotTemplate());
				button.setSelected(true);
			}

		}
		buttonGroup.add(setupButton);

		// Add custom Button
		templatePanel.add(Box.createHorizontalStrut(2));
		templatePanel.add(setupButton);
		templatePanel.invalidate();

	}

	/**
	 * Reinterpret the data and export a pivoted view to DataWarrior
	 */
	public void exportToDw() {
		try {
			String dwar = null;
			if (isPivotMode()) {

				PivotTemplate tpl = new PerInputPivotTemplate();
				tpl.init(results);

				Analyzer analyzer = new Analyzer(results, SpiritFrame.getUser());
				DataWarriorConfig config = new DataWarriorConfig();
				config.setType(ChartType.BOXPLOT);
				config.setLogScale(analyzer.getColumnFromIndex(0).getDistribution()==Distribution.LOGNORMAL);
				config.setCustomTemplate(tpl);

				//Export to DW
				StringBuilder sb = DataWarriorExporter.getDwar(results, config, SpiritFrame.getUser());
				File f = File.createTempFile("spirit_", ".dwar");
				FileWriter w = new FileWriter(f);
				IOUtils.redirect(new StringReader(sb.toString()), w);
				w.close();

				Desktop.getDesktop().open(f);
				return;
			} else if (tableTab instanceof IExportable) {
				IExportable iexport = (IExportable) tableTab;
				dwar = MiscUtils.concatenate(iexport.getTabDelimitedTable(), true);
			} else {
				throw new Exception("Exporting to DW is not supported in this view");
			}

			if (dwar == null) {
				throw new Exception("Exporting to DW is not supported in this view");
			}

			File f = File.createTempFile("spirit_", ".dwar");
			try(FileWriter w = new FileWriter(f)) {
				IOUtils.redirect(new StringReader(dwar), w);
				Desktop.getDesktop().open(f);
			}
		} catch (Exception e) {
			JExceptionDialog.showError(PivotPanel.this, e);
		}
	}

	/**
	 * Export the view to Excel
	 */
	public void exportToExcel(final boolean csv) {
		try {
			final IExportable iexport;
			if (isPivotMode()) {
				iexport = pivotTable;
			} else if (tableTab instanceof IExportable) {
				iexport = (IExportable) tableTab;
			} else {
				throw new Exception("Exporting to CSV/Excel is not supported in this view");
			}

			new SwingWorkerExtended("Exporting", PivotPanel.this) {
				String[][] table;

				@Override
				protected void doInBackground() throws Exception {
					table = iexport.getTabDelimitedTable();
					if (table == null)
						throw new Exception("Exporting to Excel is not supported in this view");

					if (csv) {
						CSVUtils.exportToCsv(table);
					} else {
						POIUtils.exportToExcel(table, ExportMode.HEADERS_TOP);
					}
				}
			};

		} catch (Exception e) {
			JExceptionDialog.showError(PivotPanel.this, e);
		}
	}

	public void setPivotMode(boolean pivotMode) {
		this.pivotMode = pivotMode;
		cardLayout.show(cardPanel, pivotMode ? "pivot" : "table");
		pivotButton.setSelected(pivotMode);
		viewPanel.setVisible(pivotMode);
	}

	public boolean isPivotMode() {
		return pivotMode;
	}

	public JComponent getTableTab() {
		return tableTab;
	}

	public PivotTable getPivotTable() {
		return pivotTable;
	}

	public void clear() {
		this.results = new ArrayList<>();
		refresh();
	}

	public void setResults(List<Result> results) {
		setResults(results, null);
	}

	/**
	 * Update the CardPanel with the given results
	 *
	 * @param results
	 * @param template
	 *            (null to use the current template)
	 * @param shouldOpenEditTemplateDlg
	 *            (true to force the opening of the template dialog)
	 */
	public void setResults(List<Result> results, PivotTemplate template) {

		// Update Result View
		if (template != null) {
			setupButton.setSelected(true);
			Enumeration<AbstractButton> buttons = buttonGroup.getElements();
			while (buttons.hasMoreElements()) {
				PivotTemplateButton button = (PivotTemplateButton) buttons.nextElement();
				if (button.getTitle().equals(template.getName())) {
					button.setSelected(true);
					break;
				}
			}

			this.currentPivotTemplate = template;
		}

		// Update Data
		this.results = results;
		refresh();
	}

	public List<Result> getResults() {
		return results;
	}

	private void refresh() {
		if (results == null || results.size() == 0) {
			pivotTable.setPivotDataTable(new PivotDataTable(results, currentPivotTemplate));
		} else {
			// Make it asynchronous, but don't specify a time, because we can
			// have more than 2 instances of this class (AnimalCare)
			new SwingWorkerExtended("Pivoting", PivotPanel.this, SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
				private PivotDataTable pivotDataTable;

				@Override
				protected void doInBackground() throws Exception {
					if (currentPivotTemplate == null) return;
					currentPivotTemplate.init(results);
					currentPivotTemplate.removeBlindItems(results, SpiritFrame.getUser());
					pivotDataTable = new PivotDataTable(results, currentPivotTemplate);
				}

				@Override
				protected void done() {
					if (currentPivotTemplate == null) {
						SpiritContextListener.setStatus("No Results");
						return;
					}
					pivotTable.setPivotDataTable(pivotDataTable);
					SpiritContextListener.setStatus(results.size() + " Results - " + pivotDataTable.getPivotRows().size() + " rows, " + pivotDataTable.getPivotColumns().size() + " columns");
				}
			};

		}

		setupButton.setEnabled(!pivotMode || (results != null && results.size() > 0));
		excelButton.setEnabled(!pivotMode || (results != null && results.size() > 0));
		csvButton.setEnabled(!pivotMode || (results != null && results.size() > 0));
		dwButton.setEnabled(!pivotMode || (results != null && results.size() > 0));

	}

	/**
	 * openEditTemplateDlg
	 *
	 * @param view
	 */
	public void openEditTemplateDlg() {
		if (dlg != null) {
			dlg.dispose();
		}
		try {
			currentPivotTemplate = currentPivotTemplate.clone();
			Window top = SwingUtilities.getWindowAncestor(this);
			if (top instanceof JFrame) {
				dlg = new PivotTemplateDlg((JFrame) top, currentPivotTemplate, PivotItemFactory.getPossibleItems(results, SpiritFrame.getUser()), tableTab==null);
			} else if (top instanceof JDialog) {
				dlg = new PivotTemplateDlg((JDialog) top, currentPivotTemplate, PivotItemFactory.getPossibleItems(results, SpiritFrame.getUser()), tableTab==null);
			}

			dlg.addPropertyChangeListener(PivotTemplateDlg.PROPERTY_UPDATED, e-> {
				refresh();
			});
			dlg.setVisible(true);
		} catch (Exception e) {
			JExceptionDialog.showError(e);
		}
	}

	public void setPivotTemplate(PivotTemplate pivotTemplate) {
		PivotTemplate newTemplate = pivotTemplate;
		newTemplate.copyDisplaySettings(this.currentPivotTemplate);
		this.currentPivotTemplate = newTemplate;

		// Select appropriate button
		setupButton.setSelected(true);
		for (int i = 0; i < defaultTemplates.length; i++) {
			if (defaultTemplates[i].equals(currentPivotTemplate)) {
				Enumeration<AbstractButton> en = buttonGroup.getElements();
				for (int j = 0; j < i && en.hasMoreElements(); j++)
					en.nextElement();
				if (en.hasMoreElements())
					en.nextElement().setSelected(true);
			}
		}
		refresh();
	}

	public PivotTemplate getCurrentPivotTemplate() {
		return currentPivotTemplate;
	}

	public PivotTemplate[] getDefaultTemplates() {
		return defaultTemplates;
	}

	public void setDefaultTemplates(PivotTemplate[] defaultTemplates) {
		this.defaultTemplates = defaultTemplates;
		this.currentPivotTemplate = null;
		initTemplates();
	}

}
