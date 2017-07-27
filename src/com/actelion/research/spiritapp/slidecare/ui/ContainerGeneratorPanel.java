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

package com.actelion.research.spiritapp.slidecare.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.BiosampleActions;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerActions;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerTable;
import com.actelion.research.spiritapp.spirit.ui.location.ContainerTableModel.ContainerTableModelType;
import com.actelion.research.spiritapp.spirit.ui.print.PrintingDlg;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.editor.ImageEditorPane;
import com.actelion.research.spiritapp.spirit.ui.util.lf.LF;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.slide.Template;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.helper.TemplateHelper;
import com.actelion.research.spiritcore.services.helper.TemplateHelper.BarcodeMemo;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class ContainerGeneratorPanel extends JPanel {

	private ContainerCreatorDlg dlg;
	
	private final BarcodeMemo barcodeMemo;
	private Template slideTemplate;
	
	private AnimalsTable animalsTable = new AnimalsTable();
	
	private ContainerTable containerTable = new ContainerTable(ContainerTableModelType.EXPANDED);
	private JLabel infoLabel = new JLabel();
	
	private List<Biosample> generated = new ArrayList<>(); 
	
	public ContainerGeneratorPanel(final ContainerCreatorDlg dlg) {
		super(new BorderLayout());
		
		this.dlg = dlg;
		this.barcodeMemo = new BarcodeMemo(dlg.getContainerTypeToCreate());
		
		JButton saveButton = new JIconButton(IconType.SAVE, "Save & Print Labels");
		saveButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
//					List<Container> allContainers = containerTable.getRows();
//					dlg.save(generated);
					if(generated==null || generated.size()==0) throw new Exception("There are no containers to save");

					DAOBiosample.persistBiosamples(generated, Spirit.askForAuthentication());
					
					JExceptionDialog.showInfo(dlg, Biosample.getContainers(generated).size() + " containers saved");
					new PrintingDlg(generated);

					
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, generated);
				} catch (Throwable ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});
		
		//JPanel animalSelection
		JPanel animalPanel = new JPanel(new BorderLayout());
		{
			JButton previewButton = new JButton("Add & Preview "+dlg.getContainerTypeToCreate().getName()+"s >>>");
			previewButton.addActionListener(new ActionListener() {			
				@Override
				public void actionPerformed(ActionEvent e) {
					generateContainers();
				}
			});
			animalPanel.add(BorderLayout.NORTH, new JLabel("Select "));
			animalPanel.add(BorderLayout.CENTER, new JScrollPane(animalsTable));
			animalPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), previewButton));
		}
		
		//ContainerPanel
		JButton deleteButton = new JButton("Clear");
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<Container> toDelete = containerTable.getSelection();
				if(toDelete.size()==0) {
					JExceptionDialog.showError("You must select some rows");
					return;
				}
				
				generated.clear();
				containerTable.clear();
				barcodeMemo.init();
			}
		});
	
		
		JPanel previewPanel = new JPanel(new BorderLayout());
		previewPanel.add(BorderLayout.NORTH, UIUtils.createHorizontalBox(deleteButton, Box.createHorizontalGlue()));
		previewPanel.add(BorderLayout.CENTER, new JScrollPane(containerTable));
		
		ContainerActions.attachPopup(containerTable);

		JPanel box = new JPanel(new BorderLayout());
		box.add(BorderLayout.CENTER, new JScrollPane(infoLabel));
		box.add(BorderLayout.EAST, UIUtils.createVerticalBox(Box.createVerticalGlue(), saveButton));
		box.setPreferredSize(new Dimension(200, 88));
		
		JSplitPane containerPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, previewPanel, box);
		containerPanel.setDividerLocation(Toolkit.getDefaultToolkit().getScreenSize().height-300);
		
		BiosampleActions.attachPopup(animalsTable);
		
		//MainPanel
		JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, animalPanel, containerPanel);
		sp.setDividerLocation(400);
		add(BorderLayout.CENTER, sp);
		generated.clear();
		containerTable.clear();
		
	}
	
	
	public void refresh() {
		Study study = dlg.getStudy();
		List<Biosample> rows = study==null? null: new ArrayList<Biosample>( study.getTopAttachedBiosamples());
		animalsTable.setRows(rows);
		generated.clear();
		containerTable.clear();
	}	
	
	public void setTemplate(Template slideTemplate) {
		this.slideTemplate = slideTemplate;
		generated.clear();
		containerTable.clear();
		barcodeMemo.init();
	}
	
	public void generateContainers() {
		
		
		Template tpl = dlg.getTemplate();
		
		
		//Prepare a list of samples that will used for the generation of slides
		//if we have a template for 1 animal: generations = [[animal1], [animal2], [animal3],...]
		//if we have a template for 2 animals: generations = [[animal1, animal2], [animal3, animal4], ...]
		int nAnimals = slideTemplate==null? 0: slideTemplate.getNAnimals();
		
		if(nAnimals<1) {
			JExceptionDialog.showInfo(this, "You must give a template");
			return;
		} else {
			List<Biosample> selectedSamples = JPAUtil.reattach(animalsTable.getSelection());			
			if(selectedSamples.size()==0) {
				infoLabel.setText("<html><span style='color:red;font-size:110%'>You must select at least one sample</span></html>");
				return;
			}
			infoLabel.setText("");
			
			//Cut the selection by number of animals
			List<List<Biosample>> groups = new ArrayList<>();
			for(int i=0; i<selectedSamples.size(); ) {
				List<Biosample> generation = new ArrayList<>();
				for (int j = 0; j < nAnimals; j++) {
					if(i<selectedSamples.size()) generation.add(selectedSamples.get(i));
					i++;
				}
				groups.add(generation);
			}
			
			
			
			try {
				//Generate the containers
				StringBuilder msgs = new StringBuilder();
				List<Biosample> res = TemplateHelper.applyTemplate(tpl, groups, barcodeMemo, SpiritFrame.getUsername(), msgs);
				
				//Add the container to the table
				generated.addAll(res);
				List<Container> previousRows = containerTable.getRows();
				previousRows.addAll(Biosample.getContainers(res));
				containerTable.setRows(previousRows);

				
				//Notify the user
				infoLabel.setText("<html>" + msgs + "</html>");
				JEditorPane editorPane = new ImageEditorPane();
				editorPane.setText("<html>" + msgs + "</html>");
				LF.initComp(editorPane);
				JScrollPane sp = new JScrollPane(editorPane);
				sp.setMaximumSize(new Dimension(600, 600));
				sp.setPreferredSize(new Dimension(600, 600));
				if(res.size()==0) {
					JOptionPane.showMessageDialog(this, sp, "No containers were created", JOptionPane.ERROR_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(this, sp, "Containers Created", JOptionPane.PLAIN_MESSAGE);
				}
			} catch (Exception e) {
				JExceptionDialog.showError(e);
			}
		}

	}
	
	

	public static class SlideInfo {
		String name;
		Set<String> animals = new TreeSet<String>();
		int total;
		int copies;
		@Override
		public String toString() {
			return name + ": "+animals.size()+" animals x "+copies+" copies -> "+total+" slides";
		}
	}	
}
