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

package com.actelion.research.spiritapp.spirit.ui.pivot;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Desktop;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleTable;
import com.actelion.research.spiritapp.spirit.ui.pivot.designer.PivotTemplateDlg;
import com.actelion.research.spiritapp.spirit.ui.result.ResultActions;
import com.actelion.research.spiritapp.spirit.ui.result.ResultTable;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils.ExportMode;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.spirit.ui.util.bgpane.JBGScrollPane;
import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.pivot.ColumnPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.CompactPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.FlatPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.InventoryPivotTemplate;
import com.actelion.research.spiritcore.business.pivot.PivotDataTable;
import com.actelion.research.spiritcore.business.pivot.PivotItemFactory;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate;
import com.actelion.research.spiritcore.business.pivot.datawarrior.DataWarriorExporter;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.CSVUtils;
import com.actelion.research.util.UsageLog;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.IExportable;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class PivotCardPanel extends JPanel {

	public static final String PROPERTY_PIVOT_CHANGED = "pivot_changed";
	private boolean pivotMode;
	private List<Result> results = new ArrayList<>();
	private Set<TestAttribute> skippedAttributes;

	// CardPanel
	private CardLayout cardLayout = new CardLayout();
	private JPanel cardPanel = new JPanel(cardLayout);
	private JComponent tableTab = null;
	private final PivotTable pivotTable = new PivotTable();
	private JPanel templatePanel = UIUtils.createHorizontalBox();

	// Views
	private final JPanel viewPanel;
	private PivotTemplate analysisPivotTemplate = new ColumnPivotTemplate();
	private PivotTemplate currentPivotTemplate = null;

	private final ButtonGroup buttonGroup = new ButtonGroup();
	private final PivotTemplateButton setupButton = new PivotTemplateButton("Customize", IconType.SETUP.getIcon().getImage());

	// Buttons
	private final JCheckBox pivotButton = new JCheckBox("Pivot Data");
	private final JButton statsButton = new JIconButton(IconType.STATS, "Analyze");
	private final JButton dwButton = new JIconButton(IconType.DATAWARRIOR, "DW");
	private final JButton csvButton = new JIconButton(IconType.CSV, "CSV");
	private final JButton excelButton = new JIconButton(IconType.EXCEL, "XLS");

	// sublist
	private ResultTable subResultTable;
	private BiosampleTable subBiosampleTable;
	private final BiosampleTabbedPane biosampleDetail;

	// Unique cutomize dialog
	private PivotTemplateDlg dlg;
	private PivotTemplate[] defaultTemplates;

	/**
	 * Create a panel for displaying pivoted resultsf
	 * 
	 * @param tableTab optional
	 * @param externalDetailPane
	 *            detailPanel to refresh when a biosample is selected, if null
	 *            the detailpanel will be created
	 */
	public PivotCardPanel(final JComponent tableTab, BiosampleTabbedPane externalDetailPane) {
		super(new BorderLayout());

		this.tableTab = tableTab;

		pivotButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setPivotMode(pivotButton.isSelected());

				if (!pivotButton.isSelected() && dlg != null) {
					dlg.dispose();
				}

				PivotCardPanel.this.firePropertyChange(PROPERTY_PIVOT_CHANGED, null, "");
			}
		});

		/////////////////////////////////
		// Bottom
		JBGScrollPane subTableSp;
		if (tableTab==null) {
			subResultTable = new ResultTable();
			subResultTable.getModel().showAllHideable(true);
			subTableSp = new JBGScrollPane(subResultTable, 1);
			ResultActions.attachPopup(subResultTable);
			ResultActions.attachPopup(subTableSp);

			subResultTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if (!biosampleDetail.isVisible())
						return;
					biosampleDetail.setBiosamples(Result.getBiosamples(subResultTable.getSelection()));
				}
			});
		} else {
			subBiosampleTable = new BiosampleTable();
			subTableSp = new JBGScrollPane(subBiosampleTable, 1);

			BiosampleActions.attachPopup(subBiosampleTable);
			BiosampleActions.attachPopup(subTableSp);

			subBiosampleTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if (!biosampleDetail.isVisible())
						return;
					biosampleDetail.setBiosamples(subBiosampleTable.getSelection());
				}
			});
		}

		JComponent subListPane;
		if (externalDetailPane == null) {
			biosampleDetail = new BiosampleTabbedPane();
			subListPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, subTableSp, biosampleDetail);
			((JSplitPane) subListPane).setDividerLocation((UIUtils.getMainFrame() == null ? 1600 : UIUtils.getMainFrame().getWidth()) - 600);
		} else {
			biosampleDetail = externalDetailPane;
			subListPane = subTableSp;
		}

		// CenterPane
		final JSplitPane centerPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JBGScrollPane(pivotTable, 1), subListPane);
		centerPane.setOneTouchExpandable(true);
		centerPane.setDividerLocation(10000);

		// pivotTable Listeners
		ListSelectionListener listener = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting())
					return;

				if (!biosampleDetail.isVisible())
					return;

				final List<Result> results = pivotTable.getSelectedResults();
				if (tableTab==null) {
					subResultTable.setRows(results);
				} else {
					List<Biosample> list = new ArrayList<Biosample>(Result.getBiosamples(results));
					Collections.sort(list);
					subBiosampleTable.setRows(list);
				}

				if (results.size() > 0 && centerPane.getDividerLocation() > getHeight() - 100) {
					centerPane.setDividerLocation(centerPane.getHeight() - 250);
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
			defaultTemplates = new PivotTemplate[] { new CompactPivotTemplate(), new ColumnPivotTemplate(), new FlatPivotTemplate() };
		} else {
			defaultTemplates = new PivotTemplate[] { new InventoryPivotTemplate() };
		}

		viewPanel = UIUtils.createTitleBoxSmall("Template", templatePanel);
		initTemplates();

		// TopPanel
		{
			statsButton.setFont(FastFont.SMALL);
			dwButton.setFont(FastFont.SMALL);
			csvButton.setFont(FastFont.SMALL);
			excelButton.setFont(FastFont.SMALL);
			
			statsButton.setToolTipText("Analyze the displayed results and suggest graphs");
			statsButton.addActionListener(e-> {
				assert pivotTable != null;
				assert pivotTable.getPivotDataTable() != null;
				new PivotAnalyzerDlg(pivotTable.getPivotDataTable().getResults(), pivotTable.getPivotDataTable().getSkippedAttributes(), analysisPivotTemplate);
			});
			dwButton.addActionListener(e -> exportToDw());
			csvButton.addActionListener(e-> exportToExcel(true));
			excelButton.addActionListener(e -> exportToExcel(false));
		}
		add(BorderLayout.NORTH, UIUtils.createHorizontalBox(
				(tableTab != null?pivotButton:null), 
				viewPanel, 
				Box.createHorizontalGlue(), 
				UIUtils.createTitleBoxSmall("Stats", UIUtils.createHorizontalBox(statsButton)),
				UIUtils.createTitleBoxSmall("Export", UIUtils.createHorizontalBox(dwButton, csvButton, excelButton))));
		add(BorderLayout.CENTER, cardPanel);

		// Add buttonlistener
		setupButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setupButton.setSelected(true);
				openEditTemplateDlg();
			}
		});

		setPivotMode(tableTab == null);
		pivot(false);

	}

	private void initTemplates() {
		// Init buttons
		templatePanel.removeAll();
		for (final PivotTemplate template : defaultTemplates) {
			final PivotTemplateButton button = new PivotTemplateButton(template);
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					firePropertyChange(PROPERTY_PIVOT_CHANGED, "", null);
					setCurrentPivotTemplate(button.getPivotTemplate());
					button.setSelected(true);
				}
			});

			templatePanel.add(button);
			buttonGroup.add(button);

			if (currentPivotTemplate == null) {
				setCurrentPivotTemplate(button.getPivotTemplate());
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
				if (DBAdapter.getAdapter().isInActelionDomain())
					UsageLog.logUsage("Spirit", Spirit.getUsername(), null, "DWExport", "");
				if (pivotTable.getPivotDataTable() == null || pivotTable.getPivotDataTable().getTemplate() == null)
					return;
				dwar = DataWarriorExporter.getDwar(pivotTable.getPivotDataTable()).toString();
			} else if (tableTab instanceof IExportable) {
				IExportable iexport = (IExportable) tableTab;
				dwar = MiscUtils.concatenate(iexport.getTabDelimitedTable(), true);
			} else {
				throw new Exception("Exporting to DW is not supported in this view");
			}

			if (dwar == null)
				throw new Exception("Exporting to DW is not supported in this view");

			File f = File.createTempFile("spirit_", ".dwar");
			FileWriter w = new FileWriter(f);
			com.actelion.research.util.IOUtils.redirect(new StringReader(dwar), w);
			w.close();

			Desktop.getDesktop().open(f);
		} catch (Exception e) {
			JExceptionDialog.showError(PivotCardPanel.this, e);
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

			new SwingWorkerExtended("Exporting", PivotCardPanel.this, true) {
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

				@Override
				protected void done() {
				}
			};

		} catch (Exception e) {
			JExceptionDialog.showError(PivotCardPanel.this, e);
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
		this.results = new ArrayList<Result>();
		pivot(false);
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
	public void setResults(List<Result> results, Set<TestAttribute> skippedAttributes, PivotTemplate template, boolean shouldOpenEditTemplateDlg) {

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

		this.skippedAttributes = skippedAttributes;

		// Update Data
		this.results = results;

		// Refresh
		pivot(shouldOpenEditTemplateDlg);

	}

	public List<Result> getResults() {
		return results;
	}

	private void pivot(final boolean shouldOpenEditTemplateDlg) {
		if (results == null || results.size() == 0) {
			pivotTable.setPivotDataTable(new PivotDataTable(results, null, currentPivotTemplate));
		} else {
			// Make it asynchronous, but don't specify a time, because we can
			// have more than 2 instances of this class (AnimalCare)
			new SwingWorkerExtended("Pivoting", PivotCardPanel.this, SwingWorkerExtended.FLAG_ASYNCHRONOUS) {
				private PivotDataTable pivotDataTable;

				@Override
				protected void doInBackground() throws Exception {
					if (currentPivotTemplate == null) {
						return;
					}

					// Find the items, which should be displayed
					currentPivotTemplate.init(results);
					currentPivotTemplate.removeBlindItems(results, Spirit.getUser());

					// Create the pivottable
					pivotDataTable = new PivotDataTable(results, skippedAttributes, currentPivotTemplate);
				}

				@Override
				protected void done() {
					results = JPAUtil.reattach(results);
					if (currentPivotTemplate == null) {
						SpiritContextListener.setStatus("No Results");
						return;
					}
					pivotTable.setPivotDataTable(pivotDataTable);
					SpiritContextListener.setStatus(results.size() + " Results - " + pivotDataTable.getPivotRows().size() + " rows, " + pivotDataTable.getPivotColumns().size() + " columns");
					if (shouldOpenEditTemplateDlg) {
						openEditTemplateDlg();
					}
				}
			};

		}

		statsButton.setEnabled(!pivotMode || (results != null && results.size() > 0));
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
				dlg = new PivotTemplateDlg((JFrame) top, currentPivotTemplate, PivotItemFactory.getPossibleItems(results, Spirit.getUser()), tableTab==null);
			} else if (top instanceof JDialog) {
				dlg = new PivotTemplateDlg((JDialog) top, currentPivotTemplate, PivotItemFactory.getPossibleItems(results, Spirit.getUser()), tableTab==null);
			}

			dlg.addPropertyChangeListener(PivotTemplateDlg.PROPERTY_UPDATED, new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					pivot(false);
				}
			});
			dlg.setVisible(true);
		} catch (Exception e) {
			JExceptionDialog.showError(e);
		}
	}

	public void setAnalysisPivotTemplate(PivotTemplate pivotTemplate) {
		this.analysisPivotTemplate = pivotTemplate;
	}

	public void setCurrentPivotTemplate(PivotTemplate pivotTemplate) {
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
		pivot(false);
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
