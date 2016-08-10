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

package com.actelion.research.spiritapp.spirit.ui.exchange;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping;
import com.actelion.research.spiritcore.services.exchange.ExchangeMapping.MappingAction;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.UIUtils;

public class StudyMappingPanel extends JPanel implements IMappingPanel {
	
	private ImporterDlg dlg;
	
	private Study fromStudy;
	private Study mappedStudy;
	private JLabel commentLabel = new JLabel();
	private JRadioButton r1 = new JRadioButton("Ignore / Keep the existing study");
//	private JRadioButton r2 = new JRadioButton("Replace the existing study"); //Not supported yet
	private JRadioButton r3 = new JRadioButton("Create a copy");

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
	
	public void updateView() {
		ExchangeMapping mapping = dlg.getMapping();
		mappedStudy = mapping.getStudyId2mappedStudy().get(fromStudy.getStudyId());
		MappingAction action = mapping.getStudyId2action().get(fromStudy.getStudyId());
		commentLabel.setFont(FastFont.BOLD);
		if(mappedStudy==null) {
			commentLabel.setText("This study is new");
			r1.setText("Don't import");
			r3.setText("Import");
			setOpaque(false);
			
		} else {
			commentLabel.setText("This study matches the existing study: " + mappedStudy.getStudyId() + (mappedStudy.getIvv()==null?"": " ("+mappedStudy.getIvv()+") "));
			commentLabel.setForeground(Color.RED); 
			r1.setText("Keep the existing design: "+mappedStudy.getStudyId());
			r3.setText("Create a copy");
			setBackground(Color.PINK);
			setOpaque(true);
			
		}
		if(action==MappingAction.CREATE) {
			r3.setSelected(true);
		} else {
			r1.setSelected(true);
		}
	
	}
	
	public void updateMapping() {
		ExchangeMapping mapping = dlg.getMapping();
		MappingAction action = mappedStudy==null?  MappingAction.CREATE: r1.isSelected()? MappingAction.SKIP: r3.isSelected()? MappingAction.CREATE: null;
		mapping.getStudyId2mappedStudy().put(fromStudy.getStudyId(), mappedStudy);
		mapping.getStudyId2action().put(fromStudy.getStudyId(), action);
	}
	
}
