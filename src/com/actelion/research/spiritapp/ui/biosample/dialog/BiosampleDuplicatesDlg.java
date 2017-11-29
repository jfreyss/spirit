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

package com.actelion.research.spiritapp.ui.biosample.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.SpiritContextListener;
import com.actelion.research.spiritapp.ui.util.lf.BiotypeComboBox;
import com.actelion.research.spiritapp.ui.util.lf.StudyComboBox;
import com.actelion.research.spiritcore.business.RightLevel;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiosample.BiosampleDuplicateMethod;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class BiosampleDuplicatesDlg extends JEscapeDialog {

	private StudyComboBox studyComboBox = new StudyComboBox(RightLevel.READ);
	private BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes());
	private JGenericComboBox<BiosampleDuplicateMethod> methodComboBox = new JGenericComboBox<>(BiosampleDuplicateMethod.values(), false);


	public BiosampleDuplicatesDlg() {
		super(UIUtils.getMainFrame(), "Find Duplicates Biosamples", true);


		studyComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Study study = studyComboBox.getText().length()==0? null: DAOStudy.getStudyByStudyId(studyComboBox.getText());
				biotypeComboBox.setValues(study==null? DAOBiotype.getBiotypes(): DAOStudy.getBiotypes(study));
			}
		});

		//Buttons
		JButton queryButton = new JIconButton(IconType.SEARCH, "Query");
		queryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				new SwingWorkerExtended("Duplicates", getContentPane()) {
					List<Biosample> biosamples;
					@Override
					protected void doInBackground() throws Exception {
						Study study = DAOStudy.getStudyByStudyId(studyComboBox.getText());
						if(study==null || !SpiritRights.canAdmin(study, SpiritFrame.getUser())) throw new Exception("You must select a study");

						biosamples = DAOBiosample.findDuplicates(study, biotypeComboBox.getSelection(), methodComboBox.getSelection(), SpiritFrame.getUser());
					}
					@Override
					protected void done() {
						JOptionPane.showMessageDialog(BiosampleDuplicatesDlg.this, "The query returned " + biosamples.size() + " results", "Results", JOptionPane.INFORMATION_MESSAGE);
						SpiritContextListener.setBiosamples(biosamples);
						dispose();
					}
				};
			}
		});


		//contentPanel
		//contentPanel
		setContentPane(UIUtils.createBox(
				UIUtils.createTitleBox(
						UIUtils.createVerticalBox(
								new JLabel("<html><i>This function will query all duplicate biosamples (same top, name, container, sampling)"),
								UIUtils.createTable(
										new JLabel("Study: "), studyComboBox,
										new JLabel("Biotype: "), biotypeComboBox),
								methodComboBox
								)),
				null,
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), queryButton)));

		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);

	}

}
