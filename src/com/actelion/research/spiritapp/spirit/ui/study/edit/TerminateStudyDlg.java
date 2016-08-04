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

package com.actelion.research.spiritapp.spirit.ui.study.edit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

@Deprecated
public class TerminateStudyDlg extends JSpiritEscapeDialog {
	
	private final Study study;
	private JCustomTextField commentsTextField = new JCustomTextField();
	private boolean isSuccess = false;
	
	public TerminateStudyDlg(Study study) {
		super(UIUtils.getMainFrame(), "Stop Study", TerminateStudyDlg.class.getName());
		this.study = study;
		
		if(!SpiritRights.canAdmin(study, Spirit.getUser())) {
			JExceptionDialog.showError("You are not allowed to edit this study");
			return;			
		}
		if(study.getState().equals("STOPPED")) {
			JExceptionDialog.showError("This study is already stopped");
			return;			
		}

		
		JButton okButton = new JIconButton(IconType.SAVE, "Stop Study");
		okButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					stopStudy();
					isSuccess = true;
					dispose();
				} catch(Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});
		
		setContentPane(UIUtils.createBox(
				UIUtils.createVerticalBox(BorderFactory.createEtchedBorder(), new JLabel("Comments:"), commentsTextField),
				UIUtils.createBox(BorderFactory.createEtchedBorder(), new JLabel("<html>"
						+ "This function will change the status of the study to <span style='color:red'>STOPPED</span>:<br>"
						+ "the study will still be accessible but it will be crossed</html>")),
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton)
				));
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}
	
	public boolean isSuccess() {
		return isSuccess;
	}
	
	private void stopStudy() throws Exception {
		study.setState("STOPPED");
		study.setNotes((study.getNotes()!=null && study.getNotes().length()>0? study.getNotes() + "\n": "") + "Stopped: "+commentsTextField.getText());
		DAOStudy.persistStudy(study, Spirit.getUser());
		SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, study);
	}
	
}
