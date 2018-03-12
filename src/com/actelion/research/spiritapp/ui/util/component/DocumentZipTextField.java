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

package com.actelion.research.spiritapp.ui.util.component;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.Document.DocumentType;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.IOUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;

public class DocumentZipTextField extends JCustomTextField {

	private JButton button = new JButton(".");
	private Document document;

	public DocumentZipTextField() {
		super(CustomFieldType.ALPHANUMERIC, 18);
		setTextWhenEmpty("Documents");
		setEditable(false);
		button.setBorder(null);
		button.setToolTipText("Upload files");
		setFocusable(true);
		add(button);

		button.setFont(FastFont.SMALLER);

		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openDocumentList();
			}
		});
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				openDocumentList();
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(hasFocus() || e.getClickCount()>=2) {
					openDocumentList();
				}
			}
		});

		super.setForeground(Color.BLUE);
	}

	public void openDocumentList() {
		Window window = SwingUtilities.getWindowAncestor(this);
		JEscapeDialog dlg = new JEscapeDialog((window instanceof JDialog)? (JDialog) window: null, "Multiple Documents");
		ImageEditorPane editorPane = new ImageEditorPane();
		editorPane.setEditable(false);
		editorPane.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if(e.getEventType()!=HyperlinkEvent.EventType.ACTIVATED) return;
				try {
					if(e.getDescription().startsWith("add:")) {
						JFileChooser chooser = new JFileChooser();
						chooser.setCurrentDirectory(new File(Spirit.getConfig().getProperty("document.uploadDir", System.getProperty("user.home"))));
						chooser.setDialogTitle("Add Document");
						chooser.setPreferredSize(new Dimension(800, 600));
						int res = chooser.showOpenDialog(dlg);
						if(res!=JFileChooser.APPROVE_OPTION) return;

						File f = chooser.getSelectedFile();
						Spirit.getConfig().setProperty("study.wizard.path", f.getParent());

						int maxKilo = SpiritProperties.getInstance().getValueInt(PropertyKey.FILE_SIZE) * 1000;
						if(f.length()>maxKilo*1000) throw new Exception("The file is too large: Max: "+maxKilo+"kb");


						if(document==null) {
							document = new Document(DocumentType.ZIP);
						}
						document.addZipEntry(f);
						setSelectedDocument(document);
						updateDocPane(editorPane);

					} else if(e.getDescription().startsWith("view:")) {
						int index = Integer.parseInt(e.getDescription().substring(5));
						Document doc = document.getZipEntry(index);
						if(doc==null) return;
						DocumentTextField.open(doc);

					} else if(e.getDescription().startsWith("del:")) {
						int index = Integer.parseInt(e.getDescription().substring(4));
						document.removeZipEntry(index);
						setSelectedDocument(document);
						updateDocPane(editorPane);

					}
				} catch (Exception e2) {
					JExceptionDialog.showError(e2);
				}

			}
		});
		updateDocPane(editorPane);

		dlg.setContentPane(UIUtils.createBox(
				new JScrollPane(editorPane),
				null,
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(dlg.new CloseAction()))));
		UIUtils.adaptSize(dlg, 400, 200);
		dlg.setVisible(true);
	}

	private void updateDocPane(JEditorPane editorPane) {
		StringBuilder sb = new StringBuilder();
		sb.append("<table>");
		if(document!=null) {
			Document zip;
			for(int i=0; ; i++) {
				try {
					zip = document.getZipEntry(i);
					if(zip==null) break;
					sb.append("<tr><td>" + MiscUtils.removeHtml(zip.getFileName()) + "</td><td><a href='view:" + i + "'>View</a><td><a href='del:" + i + "'>Del</td></tr>");
				} catch(Exception e) {
					e.printStackTrace();
					sb.append("<tr><td>" + e.getMessage() + "</td></tr>");
				}

			}
		}
		sb.append("<tr><td width=80%><a href='add:'>Add Document</td></tr>");
		sb.append("</table>");
		editorPane.setText(sb.toString());
	}

	@Override
	public void setForeground(Color foreground) {
		//Not allowed
	}

	public static void open(Document document) {
		if(document==null) return;
		try {
			File f = new File(System.getProperty("java.io.tmpdir"), document.getFileName());
			if(f.exists()) {
				f.renameTo(new File(System.getProperty("java.io.tmpdir"), "del_"+System.currentTimeMillis()+"_"+document.getFileName()));
				f.deleteOnExit();
			}
			IOUtils.bytesToFile(document.getBytes(), f);
			f.deleteOnExit();
			Desktop.getDesktop().open(f);
		} catch (Exception ex) {
			JExceptionDialog.showError(ex);
		}
	}

	public void setSelectedDocument(Document document) {
		this.document = document;
		if(document==null) {
			setText("");
		} else {
			setText(document.getFileName());
		}
	}

	public Document getSelectedDocument() {
		return document;
	}

	@Override
	public void doLayout() {
		Dimension size = getSize();
		if(button.isVisible()) button.setBounds(size.width-18, 1, 18, size.height-2);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		button.setEnabled(enabled);
	}

	@Override
	public void setBorder(Border border) {
		if(border==null) return;
		super.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(0, 0, 0, 12)));
	}

}
