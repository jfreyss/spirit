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

package com.actelion.research.spiritapp.spirit.ui.biosample;

import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.study.Study;

public class SampleIdBrowser extends SampleIdScanField {

	private final BiosampleQuery query = new BiosampleQuery();;
	private JButton button = new JButton(".");

	public SampleIdBrowser() {
		setLayout(null);
		button.setBorder(null);
		button.setToolTipText("Find Sample");
		add(button);

		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPopup();
			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				showPopup();
			}
		});
	}

	public SampleIdBrowser(Biotype biotype) {
		this();
		setBiotype(biotype);
	}

	public void setStudy(Study study) {
		query.setStudyIds(study==null?"": study.getStudyId());
	}

	public void setBiotype(Biotype biotype) {
		query.setBiotype(biotype);
		setTextWhenEmpty(biotype==null?"": biotype.getName());
	}

	protected void showPopup() {

		if(!isShowing()) return;
		final Point p = SampleIdBrowser.this.getLocationOnScreen();

		//Create the BiosampleFinder
		final BiosampleFinder frame = new BiosampleFinder((JDialog) getTopLevelAncestor(), "Biosample Finder", null, query, null, getBiosample(), true) {
			@Override
			public void onSelect(Biosample sel) {
				setBiosample(sel); dispose();
			}
		};

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				frame.setUndecorated(true);

				int x = p.x;
				int y = p.y+getBounds().height;
				if(y+frame.getHeight()>Toolkit.getDefaultToolkit().getScreenSize().height) {
					x = p.x+getBounds().width;
					y = Toolkit.getDefaultToolkit().getScreenSize().height - frame.getHeight();
				}
				if(x+frame.getWidth()>Toolkit.getDefaultToolkit().getScreenSize().width) {
					x = Toolkit.getDefaultToolkit().getScreenSize().width - frame.getWidth();
				}
				frame.setLocation(x, y);
				frame.setVisible(true);
			}
		});
	}


	@Override
	public void doLayout() {
		if(button.isVisible()) button.setBounds(getWidth()-14, 1, 14, getHeight()-2);
	}



	@Override
	public void setBorder(Border border) {
		if(border==null) return;
		super.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(0, 0, 0, 8)));
	}

	@Override
	public void setEnabled(boolean enabled) {
		button.setEnabled(enabled);
		super.setEnabled(enabled);
	}

}
