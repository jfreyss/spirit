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

import java.awt.GridBagLayout;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.print.PrintService;
import javax.print.attribute.standard.Media;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.print.PrintTemplate;
import com.actelion.research.spiritapp.print.SpiritPrinter;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.component.JFileBrowser;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoFormat;
import com.actelion.research.spiritcore.business.biosample.Biosample.InfoSize;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.IOUtils;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;

public class CassettePrinterAdapter extends PrintAdapter {

	private JPanel configPanel;
	private JCheckBox printStudyIdCheckBox = new JCheckBox("Print StudyId", false);
	private JCheckBox printBlocNoCheckBox = new JCheckBox("Print BlocNo", false);

	private JRadioButton cassetteMateRadioButton = new JRadioButton("Print to SlideMate", true);
	private JRadioButton ptouchRadioButton = new JRadioButton("Print to PTouch Printer");

	private JFileBrowser fileBrowser = new JFileBrowser();
	private JRadioButton defaultRadioButton = new JRadioButton("Auto", true);
	private JRadioButton multipleAnimalsRadioButton = new JRadioButton("Multiple Animals", false);
	private JRadioButton oneAnimalRadioButton = new JRadioButton("One Animal ", false);

	private JGenericComboBox<PrintService> printerComboBox;
	private JGenericComboBox<Media> mediaComboBox = new JGenericComboBox<>();

	public CassettePrinterAdapter(final PrintingTab tab, final ContainerType containerType) {
		super(tab);

		//RadioButtons
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(defaultRadioButton);
		buttonGroup.add(multipleAnimalsRadioButton);
		buttonGroup.add(oneAnimalRadioButton);

		//Ptouch
		PrintService[] services = SpiritPrinter.getPrintServices();
		printerComboBox = new JGenericComboBox<PrintService>(services, true);
		for(PrintService service: services) {
			if(service.getName().toLowerCase().contains("brother")){
				printerComboBox.setSelection(service);
				break;
			}
		}

		JPanel ptouchPanel = UIUtils.createTable(
				new JLabel("Brother Printer: "), printerComboBox,
				new JLabel("Media: "), mediaComboBox);

		ptouchRadioButton.addActionListener(e-> fireConfigChanged());
		cassetteMateRadioButton.addActionListener(e-> fireConfigChanged());
		defaultRadioButton.addActionListener(e-> fireConfigChanged());
		multipleAnimalsRadioButton.addActionListener(e-> fireConfigChanged());
		oneAnimalRadioButton.addActionListener(e-> fireConfigChanged());
		printStudyIdCheckBox.addActionListener(e-> fireConfigChanged());
		printBlocNoCheckBox.addActionListener(e-> fireConfigChanged());

		//FileBrowser
		configPanel = new JPanel(new GridBagLayout());
		fileBrowser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileBrowser.setFile(Spirit.getConfig().getProperty("printer.cassette", "\\\\idorsia.com\\org\\Res_shared_folders\\LABUSER\\PrintMateCache(BOURQUG)"));

		//configPanel
		configPanel = UIUtils.createVerticalBox(
				UIUtils.createHorizontalBox(printStudyIdCheckBox, printBlocNoCheckBox, Box.createHorizontalGlue()),

				Box.createVerticalStrut(15),

				UIUtils.createHorizontalBox(cassetteMateRadioButton, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(Box.createHorizontalStrut(20), new JLabel("Path to the cassette printing directory: "), Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(Box.createHorizontalStrut(20), fileBrowser, Box.createHorizontalGlue()),
				Box.createVerticalStrut(5),
				UIUtils.createHorizontalBox(Box.createHorizontalStrut(20), new JLabel("Label type: "), Box.createHorizontalStrut(10), defaultRadioButton, Box.createHorizontalStrut(10), oneAnimalRadioButton, Box.createHorizontalStrut(10), multipleAnimalsRadioButton, Box.createHorizontalGlue()),

				Box.createVerticalStrut(15),

				UIUtils.createHorizontalBox(ptouchRadioButton, new JInfoLabel("You need a 'Brother' printer with a media '" + containerType.getBrotherFormat()+ "'"), Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(Box.createHorizontalStrut(20), ptouchPanel, Box.createHorizontalGlue()),
				Box.createVerticalGlue());

		ButtonGroup gr = new ButtonGroup();
		gr.add(ptouchRadioButton);
		gr.add(cassetteMateRadioButton);


		ptouchRadioButton.setSelected(Spirit.getConfig().getProperty("printer.cassette.type", "").equals("ptouch")) ;

		printerComboBox.addActionListener(e -> {
			List<Media> media = SpiritPrinter.loadMedias(printerComboBox.getSelection(), containerType.getName());
			mediaComboBox.setValues(media, false);

			for (Media m : media) {
				if(m.toString().equalsIgnoreCase(containerType.getName()) || m.toString().equalsIgnoreCase(containerType.getBrotherFormat().getMedia())) {
					mediaComboBox.setSelection(m);
					break;
				}
			}
			fireConfigChanged();
		});
		mediaComboBox.addActionListener(e -> fireConfigChanged());

		if(services.length>0) {
			printerComboBox.getActionListeners()[0].actionPerformed(null);
		}

	}

	@Override
	public JPanel getConfigPanel() {
		return configPanel;
	}

	@Override
	public JComponent getPreviewPanelForList(List<Container> containers) {
		if(cassetteMateRadioButton.isSelected()) {
			JTextArea textarea = new JTextArea();
			textarea.setText(getPrint(containers));
			textarea.setEditable(false);
			return textarea;
		} else {
			return null;
		}
	}

	@Override
	public JComponent getPreviewPanel(Container container) {
		if(cassetteMateRadioButton.isSelected()) {
			return null;
		} else {
			PrintService ps = printerComboBox.getSelection();
			Media media = mediaComboBox.getSelection();
			return PrintAdapter.getPreviewPanel(container, ps, media, getModel());
		}
	}

	private PrintTemplate getModel() {
		PrintTemplate tpl = new PrintTemplate();
		tpl.setPerLine(true);
		tpl.setOverlapPosition(0);
		return tpl;
	}

	private String getPrint(List<Container> containers) {
		if(containers==null) return "";
		StringBuilder sb = new StringBuilder();
		for (Container c : containers) {
			Set<Biosample> biosamples = c.getBiosamples();
			if(biosamples.size()==0) return "\r\n";

			List<Biosample> tops = new ArrayList<>();
			for (Biosample b : c.getBiosamples()) {
				tops.add(b.getTopParent());
			}

			Study study = Biosample.getStudy(biosamples);
			String studyId = study==null? " ": !printStudyIdCheckBox.isSelected()? study.getLocalId(): study.getStudyIdAndInternalId();

			//7 items
			boolean multipleMode;
			if(oneAnimalRadioButton.isSelected()){
				multipleMode = false;
			} else if(multipleAnimalsRadioButton.isSelected()) {
				multipleMode = true;
			} else {
				multipleMode = c.getBiosamples().size()>1;
			}


			if(multipleMode) {
				String metaLabel = Biosample.getInfos(c.getBiosamples(), EnumSet.of(InfoFormat.SAMPLENAME), InfoSize.ONELINE);
				if(metaLabel.length()>25) metaLabel = metaLabel.substring(0, 25);
				if(metaLabel.length()==0 && c.getBlocNo()!=null) metaLabel = "Bl." + c.getBlocNo();

				sb.append("$" + studyId);
				sb.append("$" + c.getContainerOrBiosampleId());
				sb.append("$" + (metaLabel.length()==0?" ":metaLabel));
				sb.append("$" + (tops.size()<1?" ": tops.get(0).getSampleNameOrId()));
				sb.append("$" + (tops.size()<2?" ": tops.get(1).getSampleNameOrId()));
				sb.append("$" + (tops.size()<3?" ": tops.get(2).getSampleNameOrId()));
				sb.append("$" + (tops.size()<4?" ": tops.get(3).getSampleNameOrId()));
				sb.append("\r\n");
			} else {
				String metaLabel = Biosample.getInfos(c.getBiosamples(), EnumSet.of(InfoFormat.SAMPLENAME), InfoSize.ONELINE);
				if(metaLabel.length()>25) metaLabel = metaLabel.substring(0, 25);
				if(printBlocNoCheckBox.isSelected() || metaLabel.length()==0) metaLabel = "Bl." + c.getBlocNo();

				sb.append("$" + studyId);
				sb.append("$" + c.getContainerOrBiosampleId());
				sb.append("$" + (metaLabel.length()==0?" ":metaLabel));
				sb.append("$" + (tops.size()<1?" ": tops.get(0).getSampleIdName() + " " + (tops.get(0).getInheritedGroup()==null?"": "Gr."+tops.get(0).getInheritedGroup().getShortName())));
				sb.append("$");
				sb.append("\r\n");
			}
		}
		return sb.toString();
	}

	@Override
	public void print(List<Container> containers) throws Exception {
		if(containers==null || containers.size()==0) throw new Exception("No containers");
		if(cassetteMateRadioButton.isSelected()) {
			if(fileBrowser.getFile().length()==0) throw new Exception("The directory for printing is not filled");

			File destDir = new File(fileBrowser.getFile());
			if(!destDir.exists() || !destDir.isDirectory()) throw new Exception("The directory "+destDir+" does not exist or is not a directory");

			Spirit.getConfig().setProperty("printer.cassette", fileBrowser.getFile());

			File archiveDir = new File(destDir, "Archive");
			if(!archiveDir.exists()) archiveDir.mkdirs();

			String fileName = SpiritFrame.getUser()+"-" + new SimpleDateFormat("yyyyMMDD-HHmmss").format(new Date())+ ".txt";

			String content = getPrint(containers).toString();
			IOUtils.stringToFile(content, new File(destDir, fileName));
			if(archiveDir.exists())IOUtils.stringToFile(content, new File(archiveDir, fileName));
		} else {
			PrintService ps = printerComboBox.getSelection();
			Media media = mediaComboBox.getSelection();
			PrintAdapter.print(containers, ps, media, getModel());
		}

	}
}
