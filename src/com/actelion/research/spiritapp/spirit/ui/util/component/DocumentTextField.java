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

package com.actelion.research.spiritapp.spirit.ui.util.component;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.border.Border;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.util.IOUtils;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class DocumentTextField extends JCustomTextField {
	
	private JButton button = new JButton(".");
	private Document document;
	
	public DocumentTextField() {
		super(JCustomTextField.ALPHANUMERIC, 18);
		setTextWhenEmpty("Document");
		setLayout(null);		
		button.setBorder(null);
		button.setToolTipText("Upload a file");
		setFocusable(true);
		setEditable(false);
		
		button.setFont(FastFont.SMALLER);
		add(button);
		
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openFileBrowser();
			}
		});
		addKeyListener(new KeyAdapter() {		
			@Override
			public void keyTyped(KeyEvent e) {
				openFileBrowser();
			}
		});			
		addMouseListener(new MouseAdapter() {			
			@Override
			public void mouseClicked(MouseEvent e) {
				if(hasFocus() || e.getClickCount()>=2) {
					openFileBrowser();
				}
			}
		});
		
		super.setForeground(Color.BLUE);
	}
	
	public void openFileBrowser() {
		if(!hasFocus() && !button.hasFocus()) return;
		JCustomLabel currentLabel = new JCustomLabel(document==null?" ": document.getFileName(), Color.BLUE);
		currentLabel.setOpaque(true);
		currentLabel.setBackground(Color.WHITE);
		currentLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(document!=null) {
					open(document);
				}
			}
		});
		
		final JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(Spirit.getConfig().getProperty("document.uploadDir", new File(System.getProperty("user.home"))));
		JButton deleteExisting = new JIconButton(IconType.TRASH, "Remove Current");
		deleteExisting.setEnabled(document!=null);
		deleteExisting.addActionListener(new ActionListener() {					
			@Override
			public void actionPerformed(ActionEvent e) {
				setSelectedDocument(null);
				chooser.cancelSelection();						
			}
		});
		
		chooser.setApproveButtonText("Upload");
		chooser.setPreferredSize(new Dimension(800,640));
		chooser.setAccessory(UIUtils.createVerticalBox(new JCustomLabel("Current Document:", Font.BOLD), currentLabel, Box.createVerticalGlue(), deleteExisting));
		
		int res = chooser.showOpenDialog(button);
		if(res==JFileChooser.APPROVE_OPTION) {
			File f = chooser.getSelectedFile();
			
			Spirit.getConfig().setProperty("document.uploadDir", f);
			try {
				int maxKilo = SpiritProperties.getInstance().getValueInt(PropertyKey.FILE_SIZE) * 1000;
				if(f.length()>maxKilo*1000) throw new Exception("The file is too large: Max: "+maxKilo+"kb");
				Document document = new Document();
				document.setBytes(IOUtils.getBytes(f));
				document.setFileName(f.getName());
				document.setCreDate(new Date());
				document.setCreUser(Spirit.getUser()==null?"??":Spirit.getUser().getUsername());
				setSelectedDocument(document);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			} 
		}		
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
