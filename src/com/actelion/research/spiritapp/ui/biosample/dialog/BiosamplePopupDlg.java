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

package com.actelion.research.spiritapp.ui.biosample.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.actelion.research.spiritapp.ui.biosample.BiosampleTabbedPane;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.util.ui.UIUtils;

public class BiosamplePopupDlg extends JDialog {

	private static BiosamplePopupDlg instance;

	private Collection<Biosample> objects = new ArrayList<>();
	private Dimension prefDim = null;
	private Dimension setDim = null;
	private int maxCols;

	private BiosamplePopupDlg() {
		super(UIUtils.getMainFrame(), "Biosamples", false);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				super.windowClosed(e);
				instance = null;
			}
		});

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if(maxCols<=0) return;

				double incX = getSize().getWidth() / setDim.getWidth();
				double incY = getSize().getHeight() / setDim.getHeight();

				prefDim = new Dimension((int)(prefDim.getWidth() * incX), (int)(prefDim.getHeight()*incY)) ;
				refresh();
			}
		});

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
		setAlwaysOnTop(true);
	}

	public void addObjects(Collection<Biosample> biosamples) {
		if(this.objects!=null) {
			this.objects.removeAll(biosamples);
			this.objects.addAll(biosamples);
		}
		refresh();
	}

	private void refresh() {
		if(objects.size()==0) return;

		SwingUtilities.invokeLater(()->{
			JPanel content = new JPanel();
			content.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
			content.setBackground(Color.WHITE);

			for (Object object : objects) {
				BiosampleTabbedPane pane = new BiosampleTabbedPane();
				if(prefDim==null) prefDim = pane.getPreferredSize();

				pane.setPreferredSize(prefDim);
				pane.setMinimumSize(prefDim);
				pane.setBorder(BorderFactory.createEmptyBorder());
				if(object instanceof Container) {
					pane.setBiosamples(((Container)object).getBiosamples());
				} else if(object instanceof Biosample) {
					pane.setBiosamples(Collections.singletonList((Biosample)object));
				}
				content.add(pane);
			}

			JScrollPane contentPane = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			setContentPane(contentPane);

			int prefWidth = prefDim.width+25;
			int prefHeight = prefDim.height+50;
			int maxWidth = Toolkit.getDefaultToolkit().getScreenSize().width-40;
			maxCols = maxWidth / prefWidth;
			validate();
			if(objects.size()>maxCols) {
				setDim = new Dimension(prefWidth*maxCols, prefHeight+30);
			} else {
				setDim = new Dimension(prefWidth*objects.size(), prefHeight);
			}
			setSize(setDim);
		});
	}

	public static BiosamplePopupDlg showBiosamples(Collection<Biosample> biosamples) {
		if(instance==null) {
			instance = new BiosamplePopupDlg();
		}
		instance.addObjects(biosamples);
		return instance;
	}



}
