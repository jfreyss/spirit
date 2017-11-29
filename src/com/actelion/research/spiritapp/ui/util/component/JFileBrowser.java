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

package com.actelion.research.spiritapp.ui.util.component;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import com.actelion.research.spiritapp.Spirit;

public class JFileBrowser extends JPanel {
	
	private JTextField file = new JTextField(20);
	private JButton button = new JButton(".");
	private String extension = null;
	private int fileSelectionMode = JFileChooser.FILES_ONLY;
	
	public JFileBrowser() {
		this(null, null, true);
	}
	public JFileBrowser(final String title, final String propertyName, final boolean openDlg) {
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0; c.weightx = 1; add(file, c);
		c.gridx = 1; c.weightx = 0; add(button, c);		
		
		if(propertyName!=null) {
			File f = new File(Spirit.getConfig().getProperty(propertyName, "."));
			if(!openDlg) f = f.getParentFile();
			if(f!=null) setFile(f.getAbsolutePath());					
		}
		
		
		button.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				if(title!=null) chooser.setDialogTitle(title);
				chooser.setSelectedFile(new File(getFile()));
				chooser.setFileSelectionMode(fileSelectionMode);
				if(extension!=null) {
					chooser.setFileFilter(new FileFilter() {					
						@Override
						public String getDescription() {
							return extension;
						}
						
						@Override
						public boolean accept(File f) {
							if(f==null) return false;
							return f.isDirectory() || (f.isFile() && f.getName().toLowerCase().endsWith(extension.toLowerCase()));
						}
					});
				}
				int res;
				if(openDlg) {
					res = chooser.showOpenDialog(getParent());
				} else {
					res = chooser.showSaveDialog(getParent());
				}
				if(res!=JFileChooser.APPROVE_OPTION) return;
				String fileName= chooser.getSelectedFile().getAbsolutePath();
				if(!openDlg && extension!=null && !fileName.endsWith(extension)) {
					fileName = fileName + (extension.startsWith(".")? "": ".") + extension;
				}
				if(propertyName!=null) Spirit.getConfig().setProperty(propertyName, fileName);
				file.setText(fileName);
				for(ActionListener al: file.getActionListeners()) {
					al.actionPerformed(e);
				}
			}
		});
	}
	
	public void addActionListener(ActionListener al) {
		file.addActionListener(al);
	}
	
	/**
	 * Use JFileChooser.FILES_ONLY, DIRECTORIES_ONLY, ...
	 * @param fileSelectionMode
	 */
	public void setFileSelectionMode(int fileSelectionMode) {
		this.fileSelectionMode = fileSelectionMode;
	}
	
	public String getFile() {
		return file.getText();
	}
	
	public void setFile(String txt) {
		file.setText(txt);
	}

	public String getExtension() {
		return extension;
	}

	
	public void setExtension(String extension) {
		this.extension = extension;
	}
	
	
}
