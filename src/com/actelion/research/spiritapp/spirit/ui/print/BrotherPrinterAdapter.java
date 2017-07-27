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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.services.print.DirectPrinter;
import com.actelion.research.spiritapp.spirit.services.print.PrintTemplate;
import com.actelion.research.spiritapp.spirit.services.print.PrintableOfContainers;
import com.actelion.research.spiritapp.spirit.services.print.PrintableOfLines;
import com.actelion.research.spiritapp.spirit.services.print.SpiritPrinter;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.editor.EditorPaneDlg;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoFormat;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.util.IOUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class BrotherPrinterAdapter extends PrintAdapter {

	private JPanel configPanel;
	private JGenericComboBox<PrintService> printerComboBox;
	private JGenericComboBox<Media> mediaComboBox = new JGenericComboBox<>();
	private JComboBox<String> marginCombobox = new JComboBox<>(new String[] { "PTouch Configured with 36mm tape", "PTouch Configured with 12mm paper" });

	private PrintTemplate model = new PrintTemplate();
	private JRadioButton overlapNoCheckBox = new JRadioButton("No Overlap", model.getOverlapPosition() == 0);
	private JRadioButton overlapLeftCheckBox = new JRadioButton("Left Overlap", model.getOverlapPosition() == 1);
	private JRadioButton overlapRightCheckBox = new JRadioButton("Right Overlap", model.getOverlapPosition() == -1);
	// private JCheckBox showBarcode = new JCheckBox("Show SampleId/Barcode",
	// model.isShowBarcode());
	private JCheckBox showParentCheckbox = new JCheckBox("Show Parent", model.isShowParent());
	private JCheckBox showMetadataCheckbox = new JCheckBox("Show Metadata", model.isShowMetadata());
	private JCheckBox showCommentsCheckbox = new JCheckBox("Show Amount/Comments", model.isShowComments());

	public BrotherPrinterAdapter(final PrintingTab tab, final ContainerType containerType) {
		super(tab);
		PrintService[] services = SpiritPrinter.getBrotherPrintServices();
		printerComboBox = new JGenericComboBox<PrintService>(services, true);
		printerComboBox.setPreferredWidth(200);

		ButtonGroup group = new ButtonGroup();
		group.add(overlapNoCheckBox);
		group.add(overlapLeftCheckBox);
		group.add(overlapRightCheckBox);

		// Update the preview when the template is changed
		ActionListener listener = evt -> {
			savePreferencesForMedia();
			fireConfigChanged();
		};
		overlapNoCheckBox.addActionListener(listener);
		overlapLeftCheckBox.addActionListener(listener);
		overlapRightCheckBox.addActionListener(listener);
		showParentCheckbox.addActionListener(listener);
		showMetadataCheckbox.addActionListener(listener);
		showCommentsCheckbox.addActionListener(listener);

		// Help button
		JButton helpButton = new JIconButton(IconType.HELP, "Howto set media");
		helpButton.addActionListener(e -> {
			String help = "To print most labels in Spirit, you must install a PTouch printer and connect it to this computer. It is advised to use a 12mm tape. Once it is installed, make sure you follow those instructions:"
					+ "<ul>" + "<li> The printer must be called *brother* or *-BR*, to be recognized by Spirit"
					+ "<li> In the printer settings->preferences, make sure you select the good paper size: 12mm"
					+ "<li> In the printer settings->preferences, click on the Advanced tab, and click Label Format Settings. On the next dialog, click 'Import Format List' to import the media used by Spirit and import this <a href='download'>file</a> (click on the hyperlink to download it)"
					+ "</ul>";

			EditorPaneDlg dlg = new EditorPaneDlg(tab.getDialog(), "Brother Printer Help", help);
			dlg.getEditorPane().addHyperlinkListener(new HyperlinkListener() {
				@Override
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if (e.getEventType() == EventType.ACTIVATED) {
						JFileChooser chooser = new JFileChooser();
						chooser.setDialogTitle("Select where to save the Spirit Label File");
						chooser.setSelectedFile(new File("SpiritLabels.org"));
						int res = chooser.showSaveDialog(tab.getDialog());
						if (res == JOptionPane.YES_OPTION) {

							FileOutputStream os = null;
							File f = chooser.getSelectedFile();
							try (InputStream is = getClass().getResourceAsStream("Spirit Labels.org")) {
								os = new FileOutputStream(f);
								IOUtils.redirect(is, os);
								os.close();
								JExceptionDialog.showInfo(tab.getDialog(), "File Saved under " + f);
							} catch (Exception e2) {
								JExceptionDialog.showError(tab.getDialog(), e2);
							}
						}
					}
				}
			});
			dlg.setSize(540, 400);
			dlg.setVisible(true);
		});

		// ConfigPanel
		configPanel = UIUtils.createVerticalBox(UIUtils.createTable(new JLabel("Brother Printer: "), UIUtils.createHorizontalBox(printerComboBox, helpButton),
				new JLabel("Media: "), mediaComboBox, null, marginCombobox, Box.createVerticalStrut(20), null, new JLabel("Display: "),
				UIUtils.createVerticalBox(UIUtils.createHorizontalBox(overlapNoCheckBox, overlapLeftCheckBox, overlapRightCheckBox), showParentCheckbox, showMetadataCheckbox,
						showCommentsCheckbox)));

		// Events
		printerComboBox.addActionListener(e -> {
			List<Media> medias = SpiritPrinter.loadMedias(printerComboBox.getSelection(), containerType.getName());
			mediaComboBox.setValues(medias);

			String sel = null;
			// Select media one with same media name
			if (sel == null) {
				media: for (Media m : medias) {
					System.out.println("BrotherPrinterAdapter.BrotherPrinterAdapter() " + m + " " + containerType.getMedia());
					if (m.toString().equalsIgnoreCase(containerType.getMedia())) {
						sel = m.toString();
						System.out.println("BrotherPrinterAdapter.BrotherPrinterAdapter() SELECT0 " + sel);
						break media;
					}
				}
			}

			// Select media one with same containerType
			if (sel == null) {
				media: for (Media m : medias) {
					if (m.toString().equalsIgnoreCase(containerType.getName()) || m.toString().equalsIgnoreCase(containerType.getShortName())) {
						sel = m.toString();
						System.out.println("BrotherPrinterAdapter.BrotherPrinterAdapter() SELECT1 " + sel);
						break media;
					}
				}
			}

			// Select media one with same prefix
			if (sel == null) {
				String pref = containerType.getMedia().indexOf("x") < 0 ? "" : containerType.getMedia().substring(0, containerType.getMedia().indexOf("x"));
				media: for (Media m : medias) {
					if (m.toString().startsWith(pref)) {
						sel = m.toString();
						System.out.println("BrotherPrinterAdapter.BrotherPrinterAdapter() SELECT2 " + sel);
						break media;
					}
				}
			}

			System.out.println("BrotherPrinterAdapter.BrotherPrinterAdapter() >>>" + sel);
			mediaComboBox.setSelectionString(sel);
		});
		mediaComboBox.addActionListener(e -> {
			reloadPreferencesForMedia();
			fireConfigChanged();
		});
		marginCombobox.addActionListener(e -> {
			Spirit.getConfig().setProperty("print.brother.margin", marginCombobox.getSelectedIndex());
			if (marginCombobox.getSelectedIndex() == 0) {
				PrintableOfContainers.setTopMargin(13);
			} else {
				PrintableOfContainers.setTopMargin(2);
			}

			fireConfigChanged();
		});
		marginCombobox.setSelectedIndex(Spirit.getConfig().getProperty("print.brother.margin", 0));

		if (services.length > 0) {
			printerComboBox.setSelection(services[0]);
			printerComboBox.getActionListeners()[0].actionPerformed(null);
		}

	}

	private void reloadPreferencesForMedia() {
		String mediaName = mediaComboBox.getSelection() == null ? "" : mediaComboBox.getSelection().toString();
		System.out.println("BrotherPrinterAdapter.reloadPreferencesForMedia() =" + Spirit.getConfig().getProperty("print.brother.overlap_" + mediaName, -1));
		if (Spirit.getConfig().getProperty("print.brother.overlap_" + mediaName, 99) == 0) {
			overlapNoCheckBox.setSelected(true);
		} else if (Spirit.getConfig().getProperty("print.brother.overlap_" + mediaName, 99) == 1) {
			overlapLeftCheckBox.setSelected(true);
		} else if (Spirit.getConfig().getProperty("print.brother.overlap_" + mediaName, 99) == -1) {
			overlapRightCheckBox.setSelected(true);
		} else {
			try {
				System.out.println("BrotherPrinterAdapter.reloadPreferencesForMedia() " + mediaName);
				int width = Integer.parseInt(mediaName.substring(mediaName.indexOf('x') + 1, mediaName.indexOf('_')));
				System.out.println("BrotherPrinterAdapter.reloadPreferencesForMedia() >" + width);
				if (width < 30)
					overlapNoCheckBox.setSelected(true);
				else
					overlapLeftCheckBox.setSelected(true);
			} catch (Exception e) {
			}
		}
		showParentCheckbox.setSelected(Spirit.getConfig().getProperty("print.brother.parent_" + mediaName, true));
		showMetadataCheckbox.setSelected(Spirit.getConfig().getProperty("print.brother.metadata_" + mediaName, true));
		showCommentsCheckbox.setSelected(Spirit.getConfig().getProperty("print.brother.comments_" + mediaName, true));
	}

	private void savePreferencesForMedia() {
		String mediaName = mediaComboBox.getSelection() == null ? "" : mediaComboBox.getSelection().toString();
		Spirit.getConfig().setProperty("print.brother.overlap_" + mediaName, overlapLeftCheckBox.isSelected() ? 1 : overlapRightCheckBox.isSelected() ? -1 : 0);
		Spirit.getConfig().setProperty("print.brother.parent_" + mediaName, showParentCheckbox.isSelected());
		Spirit.getConfig().setProperty("print.brother.metadata_" + mediaName, showMetadataCheckbox.isSelected());
		Spirit.getConfig().setProperty("print.brother.comments_" + mediaName, showCommentsCheckbox.isSelected());
	}

	@Override
	public JPanel getConfigPanel() {
		return configPanel;
	}

	@Override
	public JComponent getPreviewPanel(Container container) {
		PrintService ps = printerComboBox.getSelection();
		Media media = mediaComboBox.getSelection();
		return getPreviewPanel(container, ps, media, getModel());
	}

	public PrintTemplate getModel() {
		PrintTemplate tpl = new PrintTemplate();
		// tpl.setShowBarcode(showBarcode.isSelected());
		tpl.setOverlapPosition(overlapLeftCheckBox.isSelected() ? 1 : overlapRightCheckBox.isSelected() ? -1 : 0);
		tpl.setShowParent(showParentCheckbox.isSelected());
		tpl.setShowComments(showCommentsCheckbox.isSelected());
		tpl.setShowMetadata(showMetadataCheckbox.isSelected());
		return tpl;
	}

	private static String[] getPrintableLines(Container container, PrintTemplate model) {
		String types;
		String staining;
		String sectionNo;
		types = Biosample.getInfos(container.getBiosamples(), EnumSet.of(InfoFormat.SAMPLENAME, InfoFormat.PARENT_SAMPLENAME), InfoSize.ONELINE);
		if (container.getContainerType() == ContainerType.SLIDE) {
			if(model.isShowBlocNo()) types = "Bl." + container.getBlocNo();
			staining = container.getMetadata(BiotypeMetadata.STAINING);
			sectionNo = container.getMetadata(BiotypeMetadata.SECTIONNO);
		} else {
			staining = Biosample.getInfos(container.getBiosamples(), EnumSet.of(InfoFormat.METATADATA), InfoSize.ONELINE);
			sectionNo = Biosample.getInfos(container.getBiosamples(), EnumSet.of(InfoFormat.COMMENTS), InfoSize.ONELINE);
		}

		List<Biosample> tops = new ArrayList<>();
		for (Biosample b : container.getBiosamples()) {
			if (!tops.contains(b.getTopParentInSameStudy())) {
				tops.add(b.getTopParentInSameStudy());
			}
		}

		return new String[] { container.getContainerId(), container.getStudy() == null ? "" : model.isShowInternalIdFirst()? container.getStudy().getLocalIdOrStudyId(): container.getStudy().getStudyId(),
				tops.size() != 1 ? ""
						: tops.get(0).getSampleId()
						+ (tops.get(0).getSampleName() != null && tops.get(0).getSampleName().length() > 0 ? " [" + tops.get(0).getSampleName() + "]" : ""),
						container.getGroup() == null ? "" : container.getGroup().getBlindedName(SpiritFrame.getUsername()),
								container.getPhase() == null ? "" : container.getPhase().getAbsoluteDateAndName(), types, staining, sectionNo

		};
	}

	public static JComponent getPreviewPanel(Container container, PrintService ps, Media media, PrintTemplate model) {

		if (container == null)
			return new JPanel();

		Printable printable;
		if(model.isPerLine()){
			PrintableOfLines p = new PrintableOfLines(Collections.singletonList(getPrintableLines(container, model)));
			printable = p;
		} else {
			PrintableOfContainers p = new PrintableOfContainers(Collections.singletonList(container));
			p.setPrintTemplate(model);
			printable = p;
		}
		// if(model.isShowBarcode()){
		// } else {
		// PrintableOfLines p = new
		// PrintableOfLines(Collections.singletonList(getPrintableLines(container,
		// model)));
		// printable = p;
		// }
		try {

			if (ps == null)
				throw new Exception("You must select a 'Brother' printer");
			PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
			if (media != null)
				attr.add(media);

			PrinterJob job = PrinterJob.getPrinterJob();
			job.setPrintable(printable);
			job.setPrintService(ps);
			PageFormat pf = job.getPageFormat(attr);

			BufferedImage img = new BufferedImage((int) pf.getWidth() * 3 + 10, (int) pf.getHeight() * 3 + 10, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = (Graphics2D) img.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, img.getWidth(), img.getHeight());
			g.setColor(Color.BLACK);
			g.drawRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);
			g.translate(3, 3); // border
			g.scale(3, 3);
			printable.print(g, pf, 0);
			g.dispose();

			int w = PREVIEW_WIDTH;
			int h = img.getHeight() * w / img.getWidth();
			if (h > PREVIEW_HEIGHT) {
				h = PREVIEW_HEIGHT;
				w = h * img.getWidth() / img.getHeight();
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

	@Override
	public void print(List<Container> containers) throws Exception {
		PrintService ps = printerComboBox.getSelection();
		Media media = mediaComboBox.getSelection();

		print(containers, ps, media, getModel());
	}

	public static void print(List<Container> containers, PrintService ps, Media media, PrintTemplate model) throws Exception {

		if (containers == null || containers.size() == 0)
			throw new Exception("You must select some containers to print");

		List<Container> toPrint = new ArrayList<>();
		for (Container b : containers) {
			if (b != null) {
				toPrint.add(b);
			}
		}

		
		Printable printable;
		if(model.isPerLine()){
			List<String[]> items = new ArrayList<>();
			for (Container c : containers) {
				items.add(getPrintableLines(c, model));
			}
			PrintableOfLines p = new PrintableOfLines(items);
			printable = p;
		} else {
			PrintableOfContainers p = new PrintableOfContainers(containers);
			p.setPrintTemplate(model);
			printable = p;

		}
		
		DirectPrinter printer = new DirectPrinter();
		printer.print(ps, media, printable);

	}
}
