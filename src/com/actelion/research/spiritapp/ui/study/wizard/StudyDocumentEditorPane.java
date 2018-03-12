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

package com.actelion.research.spiritapp.ui.study.wizard;

import java.awt.Desktop;
import java.awt.Dimension;
import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.Document.DocumentType;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.util.IOUtils;
import com.actelion.research.util.StringUtils;
import com.actelion.research.util.ui.JExceptionDialog;

/**
 * The StudyDocumentEditorPane is used to edit/preview the list of documents attached to a study
 * @author Joel Freyss
 *
 */
public class StudyDocumentEditorPane extends ImageEditorPane {

	public static String PROPERTY_DOCUMENTCHANGED = "documentChanged";

	private Study study;

	public StudyDocumentEditorPane() {
		//Document panels
		setEditable(false);
		addHyperlinkListener(e-> {
			if(e.getEventType()!=HyperlinkEvent.EventType.ACTIVATED) return;

			try {
				if(e.getDescription().startsWith("add:")) {
					DocumentType docType = DocumentType.valueOf(e.getDescription().substring(4));
					JFileChooser chooser = new JFileChooser();
					chooser.setCurrentDirectory(new File(Spirit.getConfig().getProperty("study.wizard.path", System.getProperty("user.home"))));
					chooser.setDialogTitle("Add a "+docType);
					chooser.setPreferredSize(new Dimension(800, 600));
					int res = chooser.showOpenDialog(StudyDocumentEditorPane.this);
					if(res!=JFileChooser.APPROVE_OPTION) return;

					File f = chooser.getSelectedFile();
					Spirit.getConfig().setProperty("study.wizard.path", f.getParent());

					if(f.length()>10*1024*1024L) throw new Exception("The file cannot be larger than 10Mo");

					Document document = new Document(f);
					document.setType(docType);
					document.setCreUser(SpiritFrame.getUser().getUsername());

					firePropertyChange(PROPERTY_DOCUMENTCHANGED, null, document);
					study.getDocuments().add(document);
					refresh();

				} else if(e.getDescription().startsWith("view:")) {
					String filename =  StringUtils.unconvertForUrl(e.getDescription().substring(5));
					Document doc = Document.mapFilenames(study.getDocuments()).get(filename);
					if(doc==null) return;

					//Save the doc in tmp dir
					File f = new File(System.getProperty("java.io.tmpdir"), doc.getFileName());
					f.deleteOnExit();
					IOUtils.bytesToFile(doc.getBytes(), f);
					//Execute on windows platform
					Desktop.getDesktop().open(f);

				} else if(e.getDescription().startsWith("del:")) {
					String filename =  StringUtils.unconvertForUrl(e.getDescription().substring(4));
					Document document = Document.mapFilenames(study.getDocuments()).get(filename);
					if(document==null) return;

					int res = JOptionPane.showConfirmDialog(StudyDocumentEditorPane.this, "Are you sure you want to delete " + document + "?", "Delete Document", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					if(res!=JOptionPane.YES_OPTION) return;

					firePropertyChange(PROPERTY_DOCUMENTCHANGED, null, document);

					study.getDocuments().remove(document);
					refresh();
				}
			} catch (Exception e2) {
				JExceptionDialog.showError(e2);
			}
		});
	}

	public void setStudy(Study study) {
		this.study = study;
		refresh();
	}

	public void refresh() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><div style='white-space:nowrap'>");
		if(study!=null) {
			Map<DocumentType, List<Document>> docs = Document.mapDocumentTypes(study.getDocuments());
			for (DocumentType docType : EnumSet.of(DocumentType.CONSENT_FORM, DocumentType.DESIGN, DocumentType.PRESENTATION, DocumentType.OTHER)) {
				sb.append("<div style='margin-top:5px'><b>"+docType+"</b>:<br>");
				if(docs.get(docType)!=null) {
					for (Document d : docs.get(docType)) {
						sb.append("-" + (d.getFileName().length()>18?d.getFileName().substring(0, 8) + "..." + d.getFileName().substring(d.getFileName().length()-9): d.getFileName()));
						sb.append(" <a href='view:" + StringUtils.convertForUrl(d.getFileName()) + "'>View</a>");
						sb.append(" <a href='del:" + StringUtils.convertForUrl(d.getFileName()) + "'>Del</a>");
						sb.append(" <br>");
					}
				}
				sb.append(" <a href='add:"+docType.name() +"'>Add</a></div>");
			}
		}
		sb.append("</div></html>");
		setText(sb.toString());

	}

}
