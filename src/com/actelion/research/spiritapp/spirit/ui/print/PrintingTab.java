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

package com.actelion.research.spiritapp.spirit.ui.print;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.actelion.research.spiritapp.spirit.ui.location.ContainerTable;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerTableModel.ContainerTableModelType;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.itextpdf.text.Font;


public class PrintingTab extends JPanel {

	private final PrintingDlg dlg;
	private final ContainerTable containerTable = new ContainerTable(ContainerTableModelType.PRINT);

	private JComponent containerPanel, printerPanel, previewPanel;
	private PrintAdapter printAdapter;
	private JRadioButton printAllButton = new JRadioButton("Print all", true);
	private JRadioButton printSelectionButton = new JRadioButton("Print");
	private JLabel printLabel = new JCustomLabel("", Font.ITALIC);

	private SwingWorkerExtended previewThread;


	public PrintingTab(final PrintingDlg dlg, final ContainerType containerType) {
		super(new BorderLayout());
		this.dlg = dlg;

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(printAllButton);
		buttonGroup.add(printSelectionButton);

		if(containerType==ContainerType.SLIDE) {
			printAdapter = new SlidePrinterAdapter(this, containerType);
		} else if(containerType==ContainerType.K7) {
			printAdapter = new CassettePrinterAdapter(this, containerType);
		} else if(containerType==ContainerType.CAGE) {
			printAdapter = new CagePrinterAdapter(this, containerType);
		} else {
			printAdapter = new BrotherPrinterAdapter(this, containerType);
		}

		//Are preview Panel enabled?
		boolean hasPreview;
		try {
			JComponent panel = printAdapter.getPreviewPanelForList(null);
			if(panel!=null) {
				hasPreview = true;
			} else {
				panel = printAdapter.getPreviewPanel(null);
				if(panel!=null) {
					hasPreview = true;
				} else {
					previewPanel = null;
					hasPreview = false;
				}
			}


		} catch (Exception e) {
			e.printStackTrace();
			previewPanel = null;
			hasPreview = false;
		}


		containerTable.getSelectionModel().addListSelectionListener(e-> {
			if(e.getValueIsAdjusting()) return;
			updatePrintLabel();
		});

		containerPanel = new JPanel(new BorderLayout());
		containerPanel.add(BorderLayout.CENTER, new JScrollPane(containerTable));

		printerPanel = printAdapter.getConfigPanel();
		previewPanel = new JPanel(new BorderLayout());


		JButton printButton = new JIconButton(IconType.PRINT, "Print");
		printButton.addActionListener(e-> {
			try {
				printLabel.setForeground(Color.BLUE);
				printLabel.setText("Printing...");

				List<Container> sel;
				if(printAllButton.isSelected()) {
					sel = containerTable.getRows();
				} else {
					sel = containerTable.getSelection();
				}

				printAdapter.print(sel);
				printLabel.setText("Sent to printer successfully");
				printLabel.setForeground(Color.GREEN);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
				printLabel.setForeground(Color.RED);
				printLabel.setText("Error: "+ex.getMessage());
			}
		});


		//PrintPanel
		JPanel printPanel = new JPanel(new BorderLayout());
		if(hasPreview) {
			printPanel = UIUtils.createGrid(UIUtils.createTitleBox("Settings", printerPanel), UIUtils.createTitleBox("Preview", previewPanel));
		} else {
			printPanel = UIUtils.createTitleBox("Settings", printerPanel);
		}

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, printPanel, containerPanel);
		splitPane.setDividerLocation(250);
		add(BorderLayout.CENTER, splitPane);
		add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), printLabel, Box.createHorizontalStrut(15), printAllButton, printSelectionButton, Box.createHorizontalStrut(15), printButton));



		updatePrintLabel();


		//Preview
		if(hasPreview) {
			containerTable.getSelectionModel().addListSelectionListener(e-> {
				makePreview();
			});
			SwingUtilities.invokeLater(()-> makePreview());
		}
	}

	public PrintingDlg getDialog() {
		return dlg;
	}

	public void setRows(List<Container> containers) {
		containerTable.setRows(containers);
		if(containers.size()>0) containerTable.setSelection(Collections.singleton(containers.get(0)));
		printAdapter.eventSetRows(containers);

	}

	private void updatePrintLabel() {
		printLabel.setText("");
		printLabel.setForeground(Color.BLACK);
		if(containerTable.getSelection().size()==0) {
			printSelectionButton.setEnabled(false);
			printSelectionButton.setText("Print selection    ");
			printAllButton.setSelected(true);

		} else {
			printSelectionButton.setEnabled(true);
			printSelectionButton.setText("Print selection ("+containerTable.getSelection().size()+")");
		}
	}

	public void fireConfigChanged() {
		makePreview();
	}

	public void makePreview() {
		if(previewThread!=null) previewThread.cancel();

		if(previewPanel==null) return;
		previewPanel.setBackground(Color.LIGHT_GRAY);

		final Container container = containerTable.getSelection().size()>0? containerTable.getSelection().get(0): containerTable.getModel().getRows().size()>0? containerTable.getModel().getRows().get(0): null;

		previewThread = new SwingWorkerExtended("Preview", previewPanel, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
			private JComponent prev;

			@Override
			protected void doInBackground() throws Exception {
				prev = printAdapter.getPreviewPanelForList(containerTable.getRows());
				if(prev==null) {
					prev = printAdapter.getPreviewPanel(container);
				}
			}

			@Override
			protected void done() {
				previewPanel.setBackground(null);
				previewPanel.removeAll();
				previewPanel.add(BorderLayout.CENTER, new JScrollPane(prev));
				PrintingTab.this.validate();
			}
		};
	}




}
