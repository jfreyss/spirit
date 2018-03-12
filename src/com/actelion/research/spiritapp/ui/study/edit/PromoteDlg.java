/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.ui.study.edit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JGenericComboBox;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class PromoteDlg extends JSpiritEscapeDialog {
	private final Study study;

	public PromoteDlg(Study myStudy) {
		super(UIUtils.getMainFrame(), "Study - Set Status", PromoteDlg.class.getName());
		this.study = JPAUtil.reattach(myStudy);

		List<String> nextStates = Arrays.asList(SpiritProperties.getInstance().getValues(PropertyKey.STUDY_STATES));
		final JGenericComboBox<String> stateComboBox = new JGenericComboBox<>(nextStates, true);
		stateComboBox.setEnabled(nextStates.size()>0);
		JButton okButton = new JIconButton(IconType.SAVE, "Set Status");
		okButton.addActionListener(e-> {
			try {
				promote(study, stateComboBox.getSelection(), SpiritFrame.getUser());
				dispose();
			} catch(Exception e2) {
				JExceptionDialog.showError(PromoteDlg.this, e2);
			}
		});
		getRootPane().setDefaultButton(okButton);


		setContentPane(UIUtils.createBox(
				UIUtils.createTitleBox("Set the status of " + myStudy.getStudyId(),
						UIUtils.createVerticalBox(
								Box.createVerticalStrut(10),
								UIUtils.createHorizontalBox(Box.createHorizontalStrut(30), new JLabel("New Status: "), stateComboBox, Box.createHorizontalGlue(), Box.createHorizontalStrut(30)),
								Box.createVerticalStrut(10))),
				null,
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new CloseAction("Cancel")), okButton)));
		UIUtils.adaptSize(this, -1, -1);
		setVisible(true);
	}

	private static void promote(Study study, String state, SpiritUser user) throws Exception {
		if(state==null) throw new Exception("You must select a state");
		if(state.equals(study.getState())) throw new Exception("You must select a state");
		study.setState(state);
		DAOStudy.persistStudies(Collections.singleton(study), user);
		SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Study.class, study);
	}
}
