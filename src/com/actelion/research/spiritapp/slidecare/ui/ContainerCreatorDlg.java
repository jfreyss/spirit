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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.slide.Template;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JCustomTabbedPane;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.exceltable.JSplitPaneWithZeroSizeDivider;

public class ContainerCreatorDlg extends JSpiritEscapeDialog {

	public static int MAX_SAMPLES = 6;

	private JTabbedPane tabbedPane = new JCustomTabbedPane();
	private ContainerType containerTypeToCreate;
	private Study study;
	private JTabbedPane samplePoolTabs = new JCustomTabbedPane();
	private List<OrganTab> organPanels = new ArrayList<>();
	/** 1st Tab to create the template */
	private TemplatePreviewPanel templatePreviewPanel;
	/** 2nd Tab to generate the containers */
	private ContainerGeneratorPanel containerGeneratorPanel;


	public ContainerCreatorDlg(final Study myStudy, ContainerType containerTypeToCreate) {
		super(UIUtils.getMainFrame(), "SlideCare - Create " + containerTypeToCreate.getName(), ContainerCreatorDlg.class.getName());
		this.study = DAOStudy.getStudy(myStudy.getId());
		this.containerTypeToCreate = containerTypeToCreate;

		//OrganPanel
		for (int i=0; i<MAX_SAMPLES; i++) {
			OrganTab organPanel = new OrganTab(this, i, containerTypeToCreate==ContainerType.SLIDE);
			organPanels.add(organPanel);
			samplePoolTabs.add("Sample #"+(i+1), new JScrollPane(organPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		}

		//Find the starting index
		BiosampleQuery q = new BiosampleQuery();
		q.setSids(java.util.Collections.singleton(study.getId()));
		q.setContainerType(containerTypeToCreate);
		int maxBlocNo = 0;
		try {
			for (Biosample b: DAOBiosample.queryBiosamples(q, null)) {
				if(b.getBlocNo()!=null && b.getBlocNo()>maxBlocNo) maxBlocNo = b.getBlocNo();
			}
		} catch(Exception e) {
			//Should not happen
			e.printStackTrace();
		}

		//NewContainerPreview
		templatePreviewPanel = new TemplatePreviewPanel(containerTypeToCreate, maxBlocNo+1);
		templatePreviewPanel.addPropertyChangeListener(TemplatePreviewOnePanel.PROPERTY_UPDATED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				containerGeneratorPanel.setTemplate(templatePreviewPanel.getTemplate());
			}
		});


		JButton nextButton = new JButton("Next >>>");
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tabbedPane.setSelectedIndex(1);
			}
		});

		JPanel centerFirstPane = new JPanel(new BorderLayout());
		centerFirstPane.add(BorderLayout.CENTER, new JScrollPane(templatePreviewPanel));
		centerFirstPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), nextButton));

		JSplitPane firstPane = new JSplitPaneWithZeroSizeDivider(JSplitPane.VERTICAL_SPLIT, samplePoolTabs, centerFirstPane);
		firstPane.setDividerLocation(250);

		//SlideGenerator
		containerGeneratorPanel = new ContainerGeneratorPanel(this);


		tabbedPane.add("<html><br><b>1. Create the template<br><br>", firstPane);
		tabbedPane.add("<html><br><b>2. Create the "+containerTypeToCreate.getName()+"s<br><br>", containerGeneratorPanel);


		templatePreviewPanel.addPropertyChangeListener(TemplatePreviewOnePanel.PROPERTY_UPDATED, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				enableTabs();
			}
		});

		//Update UI later
		refreshStudy();
		enableTabs();
		containerGeneratorPanel.refresh();


		setContentPane(tabbedPane);

		UIUtils.adaptSize(this, 1550, 1150);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);

	}

	public ContainerType getContainerTypeToCreate() {
		return containerTypeToCreate;
	}

	public void enableTabs() {
		int n = templatePreviewPanel.getTemplate().getNAnimals();
		for(int i=0; i<samplePoolTabs.getTabCount(); i++) {
			samplePoolTabs.setEnabledAt(i, i<n+1);
		}
	}

	public Study getStudy() {
		return study;
	}


	private void refreshStudy() {

		new SwingWorkerExtended("Refreshing", samplePoolTabs) {
			@Override
			protected void doInBackground() throws Exception {
				study = DAOStudy.getStudy(study.getId());

				//Load all samples (for faster loading)
				//				DAOStudy.fullLoad(study);
			}

			@Override
			protected void done() {
				for (OrganTab sp : organPanels) {
					sp.refresh();
				}
				containerGeneratorPanel.refresh();
			}
		};

	}

	public Template getTemplate() {
		return templatePreviewPanel.getTemplate();
	}
}
