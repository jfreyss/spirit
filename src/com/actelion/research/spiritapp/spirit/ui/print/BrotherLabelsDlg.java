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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.SheetCollate;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.services.print.DirectPrinter;
import com.actelion.research.spiritapp.spirit.services.print.LocationPrintable;
import com.actelion.research.spiritapp.spirit.services.print.PrintLabel;
import com.actelion.research.spiritapp.spirit.services.print.PrintableOfContainers;
import com.actelion.research.spiritapp.spirit.services.print.SpiritPrinter;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class BrotherLabelsDlg extends JEscapeDialog {

	private final String MEDIA = "Rack";
	public static final int PREVIEW_WIDTH = 1000;
	public static final int PREVIEW_HEIGHT = 200;
	

	
	private PrintLabelEditTable labelTable = new PrintLabelEditTable();
	
	private JGenericComboBox<PrintService> printerComboBox;
	private JGenericComboBox<Media> mediaComboBox = new JGenericComboBox<Media>();
	private JPanel configPanel;
	private JComponent previewPanel = new JPanel(new BorderLayout());
	
	private JButton printButton = new JIconButton(IconType.PRINT, "Print");
	private JLabel printLabel = new JCustomLabel("", Font.ITALIC);
	private JComboBox<String> marginCombobox = new JComboBox<String>(new String[] { "Add top margin (PTouch config: 36mm paper)", "No top margin (PTouch config: 12mm paper)"});

	
	public BrotherLabelsDlg() {
		this(new ArrayList<PrintLabel>());
	}
	
	public BrotherLabelsDlg(List<PrintLabel> labels) {
		super(UIUtils.getMainFrame(), "Print Dialog");
		
//		barcodeTextField.setEnabled(false);
		labelTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting()) return;
				updatePrintLabel();
				fireConfigChanged();
			}
		});

		labelTable.getModel().addTableModelListener(new TableModelListener() {			
			@Override
			public void tableChanged(TableModelEvent e) {
				fireConfigChanged();
			}
		});
		
		if(labels==null || labels.size()==0) {
			labels = new ArrayList<PrintLabel>();
			for(int i=0; i<10; i++) {
				labels.add(new PrintLabel());
			}
		}
		labelTable.setRows(labels);
		
		//configPanel
		configPanel = new JPanel(new GridBagLayout());
		{
			configPanel.setBorder(BorderFactory.createEtchedBorder());
			PrintService[] services = SpiritPrinter.getBrotherPrintServices();
			printerComboBox = new JGenericComboBox<PrintService>(services, true);
			
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			c.insets = new Insets(1, 2, 1, 2);
			c.weightx = 0; c.gridx = 0; c.gridy = 0; configPanel.add(new JLabel("Brother Printer: "), c); 
			c.weightx = 0; c.gridx = 1; c.gridy = 0; configPanel.add(printerComboBox, c);		 
			c.weightx = 0; c.gridx = 0; c.gridy = 1; configPanel.add(new JLabel("Media: "), c); 
			c.weightx = 0; c.gridx = 1; c.gridy = 1; configPanel.add(mediaComboBox, c);		
			c.weightx = 0; c.gridx = 1; c.gridy = 2; configPanel.add(marginCombobox, c);		
			
			c.weighty = 1; 
			c.weightx = 1; c.gridx = 2; c.gridy = 4; configPanel.add(new JLabel(), c);
			
			c.gridwidth = 2; c.weighty = 0; 
			c.weightx = 0; c.gridx = 0; c.gridy = 6; configPanel.add(new JCustomLabel("You need a 'Brother' printer with a media '" + MEDIA + "'", Font.ITALIC), c);
					
			c.gridwidth = 1; c.weighty = 0;
			c.weightx = 1; c.gridx = 3; c.gridy = 10; configPanel.add(Box.createVerticalStrut(10), c);
			
			c.gridwidth=2; c.weighty = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
//			c.weightx = 1; c.gridx = 0; c.gridy = 11; configPanel.add(new JScrollPane(textArea), c);
			
			printerComboBox.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent ev) {
					List<Media> media = SpiritPrinter.loadMedias(printerComboBox.getSelection(), MEDIA);
					mediaComboBox.setValues(media, false);
					
					for (Media m : media) {
						if(m.toString().equalsIgnoreCase(MEDIA)) {
							mediaComboBox.setSelection(m);
						}
					}
				}
			});
			mediaComboBox.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent ev) {
					fireConfigChanged();
				}
			});
			marginCombobox.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent ev) {
					Spirit.getConfig().setProperty("print.brother.margin", marginCombobox.getSelectedIndex());
					if(marginCombobox.getSelectedIndex()==0) {
						PrintableOfContainers.setTopMargin(13);
					} else {
						PrintableOfContainers.setTopMargin(2);
					}
					
					fireConfigChanged();
				}
			});
			marginCombobox.setSelectedIndex(Spirit.getConfig().getProperty("print.brother.margin", 0));
			
			if(services.length>0) {
				printerComboBox.setSelection(services[0]);
				printerComboBox.getActionListeners()[0].actionPerformed(null);			
			}
		}

		//previewPanel		
		{
			previewPanel.setPreferredSize(new Dimension(600, 350));
		}
		
		printButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					printLabel.setForeground(Color.BLUE);
					print();
					printLabel.setText("Sent to printer successfully");
					printLabel.setForeground(Color.GREEN);
				} catch(Exception ex) {
					JExceptionDialog.showError(BrotherLabelsDlg.this, ex);
					printLabel.setForeground(Color.RED);
					printLabel.setText("Error: "+ex.getMessage());
				}
				
				
			}
		});

		
		//ContentPane
		JPanel centerPane = new JPanel(new BorderLayout());
		centerPane.add(BorderLayout.NORTH, configPanel);
		centerPane.add(BorderLayout.CENTER, UIUtils.createTitleBox("Preview", previewPanel));
		centerPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), printLabel, printButton));
		
		JSplitPane contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(labelTable), centerPane);
		contentPane.setDividerLocation(200);		
		setContentPane(contentPane);		
		
		setSize(900, 600);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
		
	}
	
	private SwingWorkerExtended previewThread = null;
	private void fireConfigChanged() {
		previewPanel.removeAll();
		synchronized (this) {
			
			
			if(previewThread!=null) previewThread.cancel();
			
			if(previewPanel==null) return;
			
			previewPanel.setBackground(Color.lightGray);
			
			previewThread = new SwingWorkerExtended() {
				private JComponent prev;
				@Override
				protected void doInBackground() throws Exception {
					try {Thread.sleep(60);}catch (Exception e) {return;}
					if(isCancelled()) return;
					prev = getPreview();
				}
				@Override
				protected void done() {
					prev.setMinimumSize(new Dimension(500, 400));
					previewPanel.setBackground(null);
					previewPanel.removeAll();
					previewPanel.add(BorderLayout.CENTER, new JScrollPane(prev));
					previewPanel.validate();
				}
			};
		}
	}
	
	private JComponent getPreview() {
		PrintService ps = printerComboBox.getSelection();
		if(ps==null) return new JLabel("Please select a printer"); 
			
		Media media = mediaComboBox.getSelection();
		if(media==null) return new JLabel("Please select a media"); 
			
		List<PrintLabel> sel = labelTable.getSelection();
		if(sel.size()==0) sel = labelTable.getRows();
		
		LocationPrintable printable = new LocationPrintable(sel);
		PrinterJob job = PrinterJob.getPrinterJob();
		try {
			PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
			attr.add(media);
			attr.add(OrientationRequested.LANDSCAPE);				
			attr.add(SheetCollate.UNCOLLATED);
			attr.add(Fidelity.FIDELITY_TRUE);
			attr.add(Chromaticity.MONOCHROME);
	
			
			job.setPrintable(printable);
			job.setPrintService(ps);
			PageFormat pf = job.getPageFormat(attr);
	
			
			
			BufferedImage img = new BufferedImage((int)pf.getWidth()*3+10, (int)pf.getHeight()*3+10, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = (Graphics2D) img.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, img.getWidth(), img.getHeight());
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, img.getWidth()-1, img.getHeight()-1);
			g.translate(3, 3); //border
			g.scale(3, 3);
			printable.print(g, pf, 0);
			g.dispose();
				
			int w = PREVIEW_WIDTH;
			int h = img.getHeight()*w/img.getWidth();
			if(h>PREVIEW_HEIGHT) {
				h = PREVIEW_HEIGHT;
				w = h*img.getWidth()/img.getHeight();
			}
			
			JLabel lbl = new JLabel();
			lbl.setMinimumSize(new Dimension(w, h));
			lbl.setPreferredSize(new Dimension(w, h));

			BufferedImage scale = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			g = (Graphics2D) scale.getGraphics();
			g.drawImage(img, 0, 0, scale.getWidth(), scale.getHeight(), lbl);
			g.dispose();
			
			lbl.setIcon(new ImageIcon(scale));
			return lbl;
		} catch (Exception e) {
			e.printStackTrace();
			return new JLabel(e.getMessage()); 
		}
	}

	
	public void print() throws Exception {
		List<PrintLabel> labels = new ArrayList<PrintLabel>();
		for(PrintLabel lbl: labelTable.getSelectedRows().length<=1? labelTable.getRows(): labelTable.getSelection()) {
			if(lbl.getBarcodeId()!=null && lbl.getBarcodeId().length()>0) labels.add(lbl);
		}
		PrintService ps = printerComboBox.getSelection();
		Media media = mediaComboBox.getSelection();
		
		DirectPrinter printer = new DirectPrinter();
		LocationPrintable printable = new LocationPrintable(labels);
		printer.print(ps, media, printable);
	}
	
	
	private void updatePrintLabel() {
		printLabel.setForeground(Color.BLACK);
		if(labelTable.getSelection().size()<=1) {
			printLabel.setText("Print all "+labelTable.getRows().size()+" labels");
		} else {
			printLabel.setText("Print selected "+labelTable.getSelection().size()+" labels");			
		}
	}

	
	public static void main(String[] args) {
		
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			UIManager.put("nimbusSelectionBackground", new Color(163, 197, 221));
		} catch (Exception e) {
			e.printStackTrace();
		}

		
//		List<PrintLabel> labels = new ArrayList<PrintLabel>();
//		labels.add(new PrintLabel("S-00001 Geoffroy Heart"));
//		labels.add(new PrintLabel("Barcode", "S-00001 Geoffroy Heart and Geoffroy Lung and Geoffroy brain"));
//		labels.add(new PrintLabel("S-00001\nGeoffroy Heart\nGeoffroy Lung\nGeoffroy brain"));
		new BrotherLabelsDlg();
	}
}
