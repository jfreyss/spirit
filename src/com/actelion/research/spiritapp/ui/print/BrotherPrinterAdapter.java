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

package com.actelion.research.spiritapp.ui.print;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.print.PrintService;
import javax.print.attribute.standard.Media;
import javax.swing.Box;
import javax.swing.ButtonGroup;
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

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.print.PrintTemplate;
import com.actelion.research.spiritapp.print.SpiritPrinter;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.editor.EditorPaneDlg;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoFormat;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.util.IOUtils;
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
	private JCheckBox showParentCheckbox = new JCheckBox("Show Parent", model.isShowParent());
	private JCheckBox showMetadataCheckbox = new JCheckBox("Show Metadata", model.isShowMetadata());
	private JCheckBox showCommentsCheckbox = new JCheckBox("Show Amount/Comments", model.isShowComments());

	public BrotherPrinterAdapter(final PrintingTab tab, final ContainerType containerType) {
		super(tab);
		PrintService[] services = SpiritPrinter.getBrotherPrintServices();
		printerComboBox = new JGenericComboBox<>(services, true);
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
					if (m.toString().equalsIgnoreCase(containerType.getMedia())) {
						sel = m.toString();
						break media;
					}
				}
			}

			// Select media one with same containerType
			if (sel == null) {
				media: for (Media m : medias) {
					if (m.toString().equalsIgnoreCase(containerType.getName()) || m.toString().equalsIgnoreCase(containerType.getShortName())) {
						sel = m.toString();
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
						break media;
					}
				}
			}

			mediaComboBox.setSelectionString(sel);
		});
		mediaComboBox.addActionListener(e -> {
			reloadPreferencesForMedia();
			fireConfigChanged();
		});

		if (services.length > 0) {
			printerComboBox.setSelection(services[0]);
			printerComboBox.getActionListeners()[0].actionPerformed(null);
		}

	}

	private void reloadPreferencesForMedia() {
		String mediaName = mediaComboBox.getSelection() == null ? "" : mediaComboBox.getSelection().toString();
		if (Spirit.getConfig().getProperty("print.brother.overlap_" + mediaName, 99) == 0) {
			overlapNoCheckBox.setSelected(true);
		} else if (Spirit.getConfig().getProperty("print.brother.overlap_" + mediaName, 99) == 1) {
			overlapLeftCheckBox.setSelected(true);
		} else if (Spirit.getConfig().getProperty("print.brother.overlap_" + mediaName, 99) == -1) {
			overlapRightCheckBox.setSelected(true);
		} else {
			try {
				int width = Integer.parseInt(mediaName.substring(mediaName.indexOf('x') + 1, mediaName.indexOf('_')));
				if (width < 30) {
					overlapNoCheckBox.setSelected(true);
				} else {
					overlapLeftCheckBox.setSelected(true);
				}
			} catch (Exception e) {
				//ok
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
		return PrintAdapter.getPreviewPanel(container, ps, media, getModel());
	}

	public PrintTemplate getModel() {
		PrintTemplate tpl = new PrintTemplate();
		tpl.setOverlapPosition(overlapLeftCheckBox.isSelected() ? 1 : overlapRightCheckBox.isSelected() ? -1 : 0);
		tpl.setShowParent(showParentCheckbox.isSelected());
		tpl.setShowComments(showCommentsCheckbox.isSelected());
		tpl.setShowMetadata(showMetadataCheckbox.isSelected());
		return tpl;
	}

	static String[] getPrintableLines(Container container, PrintTemplate model) {
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

		List<Biosample> tops = new ArrayList<>(Biosample.getTopParentsInSameStudy(container.getBiosamples()));
		List<String> toPrint = new ArrayList<>();
		toPrint.add(container.getContainerId());
		toPrint.add(container.getStudy() == null ? "" : model.isShowInternalIdFirst()? container.getStudy().getLocalIdOrStudyId(): container.getStudy().getStudyId());
		if(model.isShowAllParticipants()) {
			for (Biosample b : tops) {
				toPrint.add(b.getSampleIdName());
			}
		} else {
			toPrint.add(tops.size() != 1 ? "" : tops.get(0).getSampleIdName());
		}
		toPrint.add(container.getGroup() == null ? "" : container.getGroup().getBlindedName(SpiritFrame.getUsername()));
		toPrint.add(container.getPhase() == null ? "" : container.getPhase().getAbsoluteDateAndName());
		toPrint.add(types);
		toPrint.add(staining);
		toPrint.add(sectionNo);

		return toPrint.toArray(new String[toPrint.size()]);
	}

	@Override
	public void print(List<Container> containers) throws Exception {
		PrintService ps = printerComboBox.getSelection();
		Media media = mediaComboBox.getSelection();

		PrintAdapter.print(containers, ps, media, getModel());
	}
}
