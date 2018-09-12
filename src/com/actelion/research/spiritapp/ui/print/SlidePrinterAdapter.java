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
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.study.Group;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.IOUtils;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;

public class SlidePrinterAdapter extends PrintAdapter {

	private JPanel configPanel;
	private JCheckBox printInternalIdCheckBox = new JCheckBox("Print InternalId instead of studyId", true);
	private JCheckBox printBlocNoCheckBox = new JCheckBox("Print BlocNo", true);
	private JCheckBox printAllParticipantIds = new JCheckBox("Print All Participants", true);

	private JRadioButton slideMateRadioButton = new JRadioButton("Print to SlideMate", true);
	private JRadioButton ptouchRadioButton = new JRadioButton("Print to PTouch Printer");
	private JRadioButton tsvRadioButton = new JRadioButton("Export to Comma Separated");

	private JFileBrowser slideMateBrowser = new JFileBrowser();

	private JFileBrowser tsvBrowser = new JFileBrowser();

	private JGenericComboBox<PrintService> printerComboBox;
	private JGenericComboBox<Media> mediaComboBox = new JGenericComboBox<>();

	public SlidePrinterAdapter(final PrintingTab tab, final ContainerType containerType) {
		super(tab);

		slideMateBrowser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		slideMateBrowser.setFile(Spirit.getConfig().getProperty("printer.slidemate.path", "\\\\idorsia.com\\org\\Res_shared_folders\\LABUSER\\PrintMateCache(BOURQUG)"));

		tsvBrowser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		tsvBrowser.setFile(Spirit.getConfig().getProperty("printer.slidecsv.path", "\\\\idorsia.com\\org\\Res_shared_folders\\LABUSER\\PrintMateCache(BOURQUG)\\labels.csv"));

		PrintService[] services = SpiritPrinter.getPrintServices();
		printerComboBox = new JGenericComboBox<PrintService>(services, true);
		for(PrintService service: services) {
			if(service.getName().contains("AW-S") || service.getName().contains("Brother")){
				printerComboBox.setSelection(service);
				break;
			}
		}


		JPanel ptouchPanel = UIUtils.createTable(
				new JLabel("Brother Printer: "), printerComboBox,
				new JLabel("Media: "), mediaComboBox);


		configPanel = UIUtils.createVerticalBox(

				UIUtils.createTable(3, printInternalIdCheckBox, printBlocNoCheckBox, printAllParticipantIds),

				Box.createVerticalStrut(15),

				UIUtils.createHorizontalBox(slideMateRadioButton, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(Box.createHorizontalStrut(20), new JLabel("Path to the slideMade printing file: "), slideMateBrowser, Box.createHorizontalGlue()),

				Box.createVerticalStrut(15),

				UIUtils.createHorizontalBox(tsvRadioButton, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(Box.createHorizontalStrut(20), new JLabel("Path to the csv file: "), tsvBrowser, Box.createHorizontalGlue()),

				Box.createVerticalStrut(15),

				UIUtils.createHorizontalBox(ptouchRadioButton, new JInfoLabel("You need a 'Brother' printer with a media '" + containerType.getBrotherFormat()+ "'"), Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(Box.createHorizontalStrut(20), ptouchPanel, Box.createHorizontalGlue()),
				Box.createVerticalGlue()
				);


		ButtonGroup gr = new ButtonGroup();
		gr.add(tsvRadioButton);
		gr.add(ptouchRadioButton);
		gr.add(slideMateRadioButton);


		ptouchRadioButton.setSelected(Spirit.getConfig().getProperty("printer.slide.type", "").equals("ptouch")) ;
		tsvRadioButton.setSelected(Spirit.getConfig().getProperty("printer.slide.type", "").equals("tsv")) ;


		ActionListener refreshActionListener = e-> refreshButtons();
		ptouchRadioButton.addActionListener(refreshActionListener);
		slideMateRadioButton.addActionListener(refreshActionListener);
		tsvRadioButton.addActionListener(refreshActionListener);
		printInternalIdCheckBox.addActionListener(refreshActionListener);
		printBlocNoCheckBox.addActionListener(refreshActionListener);
		printAllParticipantIds.addActionListener(refreshActionListener);


		printerComboBox.addActionListener(e-> {
			List<Media> media = SpiritPrinter.loadMedias(printerComboBox.getSelection(), containerType.getName());
			mediaComboBox.setValues(media, false);
			String sel = null;
			if(sel==null) {
				//Chose Media with appropriate media format
				for (Media m : media) {
					if(m.toString().equalsIgnoreCase(containerType.getBrotherFormat().getMedia())) {
						sel = m.toString();
						break;
					}
				}
			}
			if(sel==null) {
				//Chose Media with appropriate containerType
				for (Media m : media) {
					if(m.toString().equalsIgnoreCase(containerType.getName())) {
						sel = m.toString();
						break;
					}
				}
			}
			mediaComboBox.setSelectionString(sel);
			fireConfigChanged();
		});
		mediaComboBox.addActionListener(e-> {
			fireConfigChanged();
		});

		if(services.length>0) {
			printerComboBox.getActionListeners()[0].actionPerformed(null);
		}


		refreshButtons();



	}

	private void refreshButtons() {
		slideMateBrowser.setEnabled(slideMateRadioButton.isSelected());
		tsvBrowser.setEnabled(tsvRadioButton.isSelected());
		printerComboBox.setEnabled(ptouchRadioButton.isSelected());
		mediaComboBox.setEnabled(ptouchRadioButton.isSelected());
		fireConfigChanged();

		Spirit.getConfig().setProperty("printer.slide.type", ptouchRadioButton.isSelected()? "ptouch": tsvRadioButton.isSelected()?"tsv": "");

	}

	@Override
	public JComponent getConfigPanel() {
		return configPanel;
	}

	@Override
	public JComponent getPreviewPanelForList(List<Container> containers) {
		if(slideMateRadioButton.isSelected()) {
			JTextArea textarea = new JTextArea();
			textarea.setText(getSlideMatePrint(containers));
			textarea.setEditable(false);
			return textarea;
		} else if(tsvRadioButton.isSelected()) {
			JTextArea textarea = new JTextArea();
			textarea.setText(getTsvPrint(containers));
			textarea.setEditable(false);
			return textarea;
		} else {
			return null;
		}
	}

	@Override
	public JComponent getPreviewPanel(Container container) {
		if(slideMateRadioButton.isSelected()) {
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
		tpl.setBarcodePosition(-1);
		tpl.setOverlapPosition(0);
		tpl.setShowInternalIdFirst(printInternalIdCheckBox.isSelected());
		tpl.setShowBlocNo(printBlocNoCheckBox.isSelected());
		tpl.setShowAllParticipants(printAllParticipantIds.isSelected());
		return tpl;
	}



	@Override
	public void print(List<Container> containers) throws Exception {


		if(containers==null || containers.size()==0) throw new Exception("No containers");

		if(slideMateRadioButton.isSelected()) {

			Spirit.getConfig().setProperty("printer.slidemate.path", slideMateBrowser.getFile());

			File destDir = new File(slideMateBrowser.getFile());
			if(!destDir.exists() || !destDir.isDirectory()) throw new Exception("The directory "+destDir+" does not exist or is not a directory");

			File archiveDir = new File(destDir, "Archive");
			if(!archiveDir.exists()) archiveDir.mkdirs();

			String fileName = SpiritFrame.getUser()+"-" + new SimpleDateFormat("yyyyMMDD-HHmmss").format(new Date())+ ".txt";

			String content = getSlideMatePrint(containers).toString();
			IOUtils.stringToFile(content, new File(destDir, fileName));
			if(archiveDir.exists())IOUtils.stringToFile(content, new File(archiveDir, fileName));

		} else if(tsvRadioButton.isSelected()) {

			if(tsvBrowser.getFile().trim().length()==0) throw new Exception("You must select a file");
			Spirit.getConfig().setProperty("printer.tsv.path", tsvBrowser.getFile());
			File destFile = new File(tsvBrowser.getFile());

			File archiveDir = new File(destFile.getParentFile(), "Archive");
			if(!archiveDir.exists()) archiveDir.mkdirs();

			String fileName = SpiritFrame.getUser() + "-" + new SimpleDateFormat("yyyyMMDD-HHmmss").format(new Date())+ ".txt";

			String content = getSlideMatePrint(containers).toString();
			IOUtils.stringToFile(content, destFile);
			if(archiveDir.exists()) IOUtils.stringToFile(content, new File(archiveDir, fileName));

		} else {
			PrintService ps = printerComboBox.getSelection();
			Media media = mediaComboBox.getSelection();
			PrintAdapter.print(containers, ps, media, getModel());
		}

	}

	private String getSlideMatePrint(List<Container> containers) {
		return getPrint(containers, true);
	}

	private String getTsvPrint(List<Container> containers) {
		return getPrint(containers, false);
	}

	private String getPrint(List<Container> containers, boolean slideMate) {

		if(containers==null) return "";

		StringBuilder sb = new StringBuilder();
		for (Container c : containers) {


			Set<Biosample> biosamples = c.getBiosamples();
			if(biosamples.size()==0) return "\r\n";

			Study study = Biosample.getStudy(biosamples);
			String studyId = study==null? "": printInternalIdCheckBox.isSelected()? study.getLocalId(): study.getStudyIdAndInternalId();

			Group group = Biosample.getGroup(biosamples);


			//10 items
			String staining = c.getMetadata(BiotypeMetadata.STAINING);
			String sectionNo = c.getMetadata(BiotypeMetadata.SECTIONNO);
			String name = Biosample.getInfos(c.getBiosamples(), EnumSet.of(InfoFormat.SAMPLENAME, InfoFormat.PARENT_SAMPLENAME), InfoSize.ONELINE);

			if((printBlocNoCheckBox.isSelected() || name.length()==0) && c.getBlocNo()!=null) name = "Bl."+c.getBlocNo();

			if(name.length()>25) name = name.substring(0, 25);

			//Note: For the slide printer, empty lines are not allowed, they should have at least a dot.
			//Only one template is used: 10 lines
			List<Biosample> tops = new ArrayList<Biosample>();
			for (Biosample b : c.getBiosamples()) {
				if(!tops.contains(b.getTopParentInSameStudy())) {
					tops.add(b.getTopParentInSameStudy());
				}
			}

			if(slideMate) {
				if(studyId.length()==0) studyId = ".";
				if(staining.length()==0) staining = ".";
				if(sectionNo.length()==0) sectionNo = ".";
				if(name.length()==0) name = ".";

				sb.append("$" + studyId); 									//1 -StudyId
				sb.append("$" + c.getContainerOrBiosampleId());				//2 -Barcode
				sb.append("$" + (group==null?".": group.getName()));		//3 -Group
				sb.append("$" + (tops.size()<1?".": tops.get(0).getSampleIdName()).replace('$', ' ') );
				sb.append("$" + (tops.size()<2?".": tops.get(1).getSampleIdName()).replace('$', ' ') );
				sb.append("$" + (tops.size()<3?".": tops.get(2).getSampleIdName()).replace('$', ' ') );
				sb.append("$" + (tops.size()<4?".": tops.get(3).getSampleIdName()).replace('$', ' ') );

				sb.append("$" + name);	 		//8
				sb.append("$" + staining); 		//9
				sb.append("$" + sectionNo); 	//10 -SectionNo
				sb.append("\r\n");

			} else {
				final char TAB = ',';
				//TSV
				sb.append(studyId);		 													//1 -StudyId
				sb.append(TAB + c.getContainerOrBiosampleId().replace(TAB, ' '));			//2 -Barcode
				sb.append(TAB + (group==null?"": group.getName()).replace(TAB, ' '));		//3 -Group
				sb.append(TAB + (tops.size()!=1?"": tops.get(0).getSampleId() + (tops.get(0).getSampleName()!=null && tops.get(0).getSampleName().length()>0? " [" + tops.get(0).getSampleName() + "]":"")).replace(TAB, ' '));
				sb.append(TAB + name.replace(TAB, ' '));
				sb.append(TAB + staining.replace(TAB, ' '));
				sb.append(TAB + sectionNo.replace(TAB, ' '));
				sb.append("\r\n");
			}
		}
		String s = sb.toString();

		if(slideMate) {
			//Replace all empty lines with '.' (bug from the slideprinter)
			s = s.replaceAll("\\$\\$", "\\$.\\$").replaceAll("\\$\\$", "\\$.\\$");
		}

		return s;
	}



}
