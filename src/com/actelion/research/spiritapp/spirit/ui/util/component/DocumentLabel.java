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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import com.actelion.research.spiritcore.business.Document;
import com.actelion.research.spiritcore.util.IOUtils;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.exceltable.JLabelNoRepaint;
import com.itextpdf.text.Font;

public class DocumentLabel extends JLabelNoRepaint {
	
	private Document document;
	
	public DocumentLabel() {
		super();
		setLayout(null);		
		setBorder(null);
		setFocusable(true);
		setOpaque(true);

		
		addMouseListener(new MouseAdapter() {			
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount()>=1 && document!=null) {
					open(document);
				}
			}
		});		
		
		super.setForeground(Color.BLUE);
		super.setFont(getFont().deriveFont(Font.UNDERLINE));
	}
	
	@Override
	public void setForeground(Color foreground) {
		//Not allowed
	}
	
	public static void open(Document document) {
		if(document==null) return;
		try {
			File f = new File(System.getProperty("java.io.tmpdir"), document.getFileName());
			IOUtils.bytesToFile(document.getBytes(), f);
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
		repaint(); //needed but should be done automatically??
	}
	
	public Document getSelectedDocument() {
		return document;
	}
	

}
