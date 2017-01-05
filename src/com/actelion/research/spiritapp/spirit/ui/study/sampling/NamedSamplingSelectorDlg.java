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

package com.actelion.research.spiritapp.spirit.ui.study.sampling;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.lf.LF;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.services.dao.DAONamedSampling;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.UIUtils;

public class NamedSamplingSelectorDlg extends JEscapeDialog {
	
	private NamedSamplingComboBox namedSamplingComboBox = new NamedSamplingComboBox();
	JButton reuseTemplateButton = new JButton("Import");
	JButton newTemplateButton = new JButton("New Template");

	private NamedSampling namedSampling;
	private boolean success = false;
	
	public NamedSamplingSelectorDlg() {
		super(UIUtils.getMainFrame(), "Import sampling");
	
		//CenterPane
		namedSamplingComboBox.setValues(DAONamedSampling.getNamedSamplings(Spirit.getUser(), null), true);
		
		final JEditorPane editorPane = new JEditorPane("text/html", "");
		LF.initComp(editorPane);
		editorPane.setEditable(false);
		
		final JScrollPane scrollPane = new JScrollPane(editorPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(300, 300));		
		
		//Actions
		namedSamplingComboBox.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				namedSampling = namedSamplingComboBox.getSelection();
				reuseTemplateButton.setEnabled(namedSampling!=null);
				if(namedSampling==null) {
					editorPane.setText("");
				} else {
					editorPane.setText("<b>" + MiscUtils.removeHtml(namedSampling.getName()) + "</b><br>" + namedSampling.getHtmlBySampling());
					editorPane.setCaretPosition(0);
				}
			}
		});
		reuseTemplateButton.setEnabled(false);
		
		reuseTemplateButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(namedSamplingComboBox.getSelection()==null) return;
				success = true;
				namedSampling = namedSamplingComboBox.getSelection().duplicate();				
				dispose();
			}
		});
		
		newTemplateButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				success = true;
				namedSampling = new NamedSampling("");
				dispose();
			}
		});
		
		
		//ContentPane
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, 
				UIUtils.createGrid(
						UIUtils.createTitleBox("Import an existing Template", 
								UIUtils.createBox(scrollPane, UIUtils.createHorizontalBox(namedSamplingComboBox, reuseTemplateButton, Box.createHorizontalGlue()))),
						UIUtils.createTitleBox("New Template", 
								UIUtils.createCenterPanel(newTemplateButton, false))));

		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

	
	public boolean isSuccess() {
		return success;
	}
	
	public NamedSampling getNamedSampling() {
		return namedSampling;
	}
}
