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

package com.actelion.research.spiritapp.spirit.ui.biosample.edit;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.help.HelpBinder;
import com.actelion.research.spiritapp.spirit.ui.lf.BiotypeComboBox;
import com.actelion.research.spiritapp.spirit.ui.study.sampling.NamedSamplingComboBox;
import com.actelion.research.spiritapp.spirit.ui.study.sampling.NamedSamplingDlg;
import com.actelion.research.spiritapp.spirit.ui.study.sampling.NamedSamplingEditorPane;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAONamedSampling;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.helper.BiosampleCreationHelper;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JInfoLabel;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class CreateChildrenDlg extends JEscapeDialog {
	
	private JToggleButton radio1 = new JToggleButton("Manual creation");
	private JToggleButton radio2 = new JToggleButton("Use a sampling's template");
	
	//
	private final JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
	private final BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());
	
	//
	private final NamedSamplingComboBox namedSamplingComboBox = new NamedSamplingComboBox();
	
	private final List<Biosample> parents;
	private List<Biosample> children;
	
	public CreateChildrenDlg(List<Biosample> parents) {		
		super(UIUtils.getMainFrame(), "Add Children", true);
		
		this.parents = parents;
		
		final Set<Biotype> types = Biosample.getBiotypes(parents);
		final Biotype type = types.size()==1? types.iterator().next(): null;
		
		//TopPanel
		JPanel topPanel = UIUtils.createTitleBox("", 
				UIUtils.createVerticalBox(
						new JInfoLabel("How do you want to create the children of " + (parents.size()==1? parents.iterator().next().getSampleId(): " these "+parents.size() + " biosamples")),
						UIUtils.createGrid(radio1, radio2)));
		
		
		final JPanel centerPanel = new JPanel(new CardLayout());		
		centerPanel.add(new JPanel(), "empty");
		
		//Create Aliquots by type
		{
			centerPanel.add(UIUtils.createTitleBox("Manual Creation", UIUtils.createTable(
					new JLabel("Biotype:"), biotypeComboBox,
					new JLabel("Number:"), spinner)), "panel1");

			if(type!=null) {
				Biotype uniqueChild = null;
				for (Biotype b : DAOBiotype.getBiotypes()) {
					if(type.equals(b.getParent())) {
						if(uniqueChild==null) uniqueChild = b;
						else {uniqueChild=null; break;}
					}
				}
				biotypeComboBox.setSelection(uniqueChild!=null? uniqueChild: type);
			}
		}
		
		//Use a sampling's template
		{
			final JButton createTemplateButton = new JIconButton(IconType.NEW, "New Template");
			final JButton deleteTemplateButton = new JIconButton(IconType.DELETE, "Delete");
			final JButton editTemplateButton = new JIconButton(IconType.EDIT, "Edit");
			

			final NamedSamplingEditorPane previewTemplate = new NamedSamplingEditorPane();
			centerPanel.add(UIUtils.createTitleBox("Use a sampling's template",
					UIUtils.createBox(new JScrollPane(previewTemplate),
						UIUtils.createTable(2, 
								new JLabel("Template:"), UIUtils.createHorizontalBox(namedSamplingComboBox, Box.createHorizontalGlue()),
								null, UIUtils.createHorizontalBox(deleteTemplateButton, editTemplateButton, createTemplateButton, Box.createHorizontalGlue()))
						)), "panel2");
			
			refreshSamplings();
			
			deleteTemplateButton.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					NamedSampling sel = namedSamplingComboBox.getSelection();
					if(sel==null || sel.getStudy()!=null || !SpiritRights.canEdit(sel, Spirit.getUser())) {
						JExceptionDialog.showError(CreateChildrenDlg.this, "You must select an editable template");
						return;
					} 
					int res = JOptionPane.showOptionDialog(CreateChildrenDlg.this, "Are you sure you want to delete "+sel+"?", "Delete Template", JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {"Delete", "Cancel"}, "Cancel");
					if(res==0) {
						try {
							JPAUtil.pushEditableContext(Spirit.getUser());
							DAONamedSampling.deleteNamedSampling(sel, Spirit.getUser());
						} catch(Exception ex) {
							JExceptionDialog.showError(ex);
						} finally {
							JPAUtil.popEditableContext();
						}
						refreshSamplings();
					}
					
				}
			});
			
			createTemplateButton.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					NamedSamplingDlg dlg = new NamedSamplingDlg(null, new NamedSampling(), null);
					if(dlg.getSavedNamedSampling()!=null) {
						refreshSamplings();
						namedSamplingComboBox.setSelection(dlg.getSavedNamedSampling());
					}
				}
			});
			editTemplateButton.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					NamedSampling sel = namedSamplingComboBox.getSelection();
					if(sel==null || sel.getStudy()!=null || !SpiritRights.canEdit(sel, Spirit.getUser())) {
						JExceptionDialog.showError(CreateChildrenDlg.this, "You must select an editable template");
						return;
					} 
					new NamedSamplingDlg(sel.getStudy(), sel, null);
					refreshSamplings();
				}
			});
			
			
			deleteTemplateButton.setEnabled(false);
			editTemplateButton.setEnabled(false);
			namedSamplingComboBox.addActionListener(new ActionListener() {				
				@Override
				public void actionPerformed(ActionEvent e) {
					NamedSampling sel = namedSamplingComboBox.getSelection();
					previewTemplate.setNamedSampling(sel);
					
					boolean enable = sel!=null && sel.getStudy()==null && SpiritRights.canEdit(sel, Spirit.getUser());
					deleteTemplateButton.setEnabled(enable);
					editTemplateButton.setEnabled(enable);
					editTemplateButton.setToolTipText(enable? null: "Edition is only possible if you are the creator of the template, and it is not associated to a study");
					editTemplateButton.setToolTipText(enable? null: "Deletion is only possible if you are the creator of the template, and it is not associated to a study");
				}
			});
			
		}
		
		
		
		//Buttons
		final JButton okButton = new JButton("Create Biosamples...");
		okButton.setEnabled(false);
		
		ButtonGroup group = new ButtonGroup();
		group.add(radio1);
		group.add(radio2);
		//Events
		radio1.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				((CardLayout)centerPanel.getLayout()).show(centerPanel, "panel1");
				okButton.setEnabled(true);
			}
		});
		radio2.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				((CardLayout)centerPanel.getLayout()).show(centerPanel, "panel2");
				okButton.setEnabled(true);
			}
		});
		okButton.addActionListener(new ActionListener() {					
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if(radio1.isSelected()) createTemplate1();
					else if(radio2.isSelected()) createTemplate2();
					else throw new Exception("You must select one of the radio button");
					dispose();
				} catch (Exception ex) {
					JExceptionDialog.showError(ex);
				}
			} 
		});


		//Contentpane
		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.NORTH, topPanel);
		contentPane.add(BorderLayout.CENTER, centerPanel);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(HelpBinder.createHelpButton(), Box.createHorizontalGlue(), okButton));
		setContentPane(contentPane);
		UIUtils.adaptSize(this, 600, 400);
		setLocationRelativeTo(UIUtils.getMainFrame());
		
		
	}
	
	private void refreshSamplings() {
		
		Study study = Biosample.getStudy(parents);
		List<NamedSampling> samplings = DAONamedSampling.getNamedSamplings(Spirit.getUser()==null?"": Spirit.getUser().getUsername(), study);			
		namedSamplingComboBox.setValues(samplings, true);
	}

	public List<Biosample> getChildren(){
		return children;
	}
	
	
	public void createTemplate1() {				
		List<Biosample> res = new ArrayList<Biosample>();
		for (Biosample parent : parents) {
			parent = JPAUtil.reattach(parent);
			for (int i = 0; i < (Integer) spinner.getValue(); i++) {
				Biosample b = new Biosample();
				b.setBiotype(biotypeComboBox.getSelection());			
				b.setInheritedStudy(parent.getInheritedStudy());
				b.setInheritedGroup(parent.getInheritedGroup());
				b.setInheritedPhase(parent.getInheritedPhase());
				b.setTopParent(parent.getTopParent());
				b.setParent(parent);
				res.add(b);											
			}
		}
		children = res;
	}

	public void createTemplate2() throws Exception {				
		NamedSampling sampling = namedSamplingComboBox.getSelection();
		if(sampling==null) throw new Exception("You must select a sampling");
		children = BiosampleCreationHelper.processTemplateOutsideStudy(sampling, JPAUtil.reattach(parents), true);
	}

}
