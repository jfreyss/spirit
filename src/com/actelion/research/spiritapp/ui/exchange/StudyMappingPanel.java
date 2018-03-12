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

package com.actelion.research.spiritapp.ui.exchange;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping.EntityAction;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

public class StudyMappingPanel extends JPanel implements IMappingPanel {

	private ImporterDlg dlg;

	private Study fromStudy;
	private Study mappedStudy;
	private JLabel commentLabel = new JLabel();
	private JRadioButton r1 = new JRadioButton("Ignore");
	private JRadioButton r3 = new JRadioButton("Import / Create a copy (if existing)");

	public StudyMappingPanel(ImporterDlg dlg, Study fromStudy) {
		super(new BorderLayout());
		this.dlg = dlg;
		this.fromStudy = fromStudy;



		ButtonGroup group = new ButtonGroup();
		group.add(r1);
		group.add(r3);

		add(BorderLayout.CENTER,
				UIUtils.createVerticalBox(
						UIUtils.createHorizontalBox(commentLabel, Box.createHorizontalGlue()),
						r1,
						r3,
						Box.createHorizontalGlue()));
		updateView();


	}

	@Override
	public void updateView() {
		ExchangeMapping mapping = dlg.getMapping();
		mappedStudy = mapping.getStudyId2mappedStudy().get(fromStudy.getStudyId());
		EntityAction action = mapping.getStudyId2action().get(fromStudy.getStudyId());
		commentLabel.setFont(FastFont.BOLD);
		if(mappedStudy==null || mappedStudy.getId()<=0) {
			commentLabel.setText("This study is new");
		} else {
			commentLabel.setText("This study matches the existing study: " + mappedStudy.getStudyId() + (mappedStudy.getLocalId()==null?"": " ("+mappedStudy.getLocalId()+") "));
			commentLabel.setForeground(Color.RED);
			r3.setSelected(true);
		}

		if(action==EntityAction.CREATE) {
			r3.setSelected(true);
		} else {
			r1.setSelected(true);
		}
	}

	@Override
	public void updateMapping() {
		ExchangeMapping mapping = dlg.getMapping();
		EntityAction action = mappedStudy==null?  EntityAction.CREATE: r1.isSelected()? EntityAction.SKIP: r3.isSelected()? EntityAction.CREATE: null;
		mapping.getStudyId2mappedStudy().put(fromStudy.getStudyId(), mappedStudy);
		mapping.getStudyId2action().put(fromStudy.getStudyId(), action);
	}

}
