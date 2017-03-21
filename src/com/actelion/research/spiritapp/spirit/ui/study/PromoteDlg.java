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

package com.actelion.research.spiritapp.spirit.ui.study;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.helper.WorkflowHelper;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;

public class PromoteDlg extends JSpiritEscapeDialog {
	private final Study study;
	
	public PromoteDlg(Study myStudy) {
		super(UIUtils.getMainFrame(), "Study - Promote", PromoteDlg.class.getName());
		this.study = JPAUtil.reattach(myStudy);
		
		List<String> nextStates = WorkflowHelper.getNextStates(study, SpiritFrame.getUser());
		if(study.getState()!=null) nextStates.remove(study.getState());
		final JGenericComboBox<String> stateComboBox = new JGenericComboBox<>(nextStates, true);
		stateComboBox.setEnabled(nextStates.size()>0);
		JButton okButton = new JButton("Promote");
		okButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					promote(study, stateComboBox.getSelection(), SpiritFrame.getUser());
					dispose();
				} catch(Exception e2) {
					JExceptionDialog.showError(PromoteDlg.this, e2);
				}
				
			}
		});
		getRootPane().setDefaultButton(okButton);
		
		
		setContentPane(UIUtils.createBox(
				UIUtils.createVerticalBox(
						new JLabel("To which state do you want to promote the study?"),
						UIUtils.createHorizontalBox(Box.createHorizontalStrut(30), new JCustomLabel(study.getState()==null?"": study.getState() + " to: ", Font.BOLD), stateComboBox, Box.createHorizontalGlue())),
				null, 				
				UIUtils.createVerticalBox(
						UIUtils.createTitleBox("Possible states", new JLabel(WorkflowHelper.getWorkflowDescription(study.getState()))), 
						UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new CloseAction("Cancel")), okButton))));
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}
	
	private static void promote(Study study, String state, SpiritUser user) throws Exception {
		if(state==null) throw new Exception("You must select a state");
		study.setState(state);
		DAOStudy.persistStudies(Collections.singleton(study), user);
		SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, study);
	}
}
