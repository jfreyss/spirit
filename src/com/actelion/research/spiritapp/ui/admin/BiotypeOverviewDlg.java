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

package com.actelion.research.spiritapp.ui.admin;


import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent.EventType;

import com.actelion.research.spiritapp.ui.util.component.BiotypeComboBox;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class BiotypeOverviewDlg extends JEscapeDialog {

	private BiotypeComboBox biotypeComboBox = new BiotypeComboBox(DAOBiotype.getBiotypes(true));

	private final BiotypeDocumentPane biotypePane = new BiotypeDocumentPane();

	public BiotypeOverviewDlg() {
		super(UIUtils.getMainFrame(), "Admin - Biotype", true);

		biotypePane.addHyperlinkListener(e-> {
			if(e.getEventType()!=EventType.ACTIVATED) return;
			if(e.getDescription().startsWith("type:")) {
				String param = e.getDescription().substring(5);
				Biotype bt = DAOBiotype.getBiotype(param);
				biotypeComboBox.setSelection(bt);
				biotypePane.setSelection(bt);
			}
		});

		final JButton addButton = new JIconButton(IconType.NEW, "New Biotype");
		final JButton editButton = new JIconButton(IconType.EDIT, "Edit");
		//		final JButton deleteButton = new JIconButton(IconType.DELETE, "Delete");
		JButton closeButton = new JButton("Close");


		//Help
		biotypePane.setSelection(null);
		JScrollPane sp = new JScrollPane(biotypePane);
		sp.setPreferredSize(new Dimension(440, 250));

		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(BorderLayout.NORTH, UIUtils.createTitleBox("Biotypes", UIUtils.createVerticalBox(
				UIUtils.createHorizontalBox(new JLabel("Biotype: "), biotypeComboBox, editButton, Box.createHorizontalGlue()),
				UIUtils.createHorizontalBox(addButton, Box.createHorizontalGlue()))));
		contentPanel.add(BorderLayout.CENTER, sp);
		contentPanel.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), closeButton));
		setContentPane(contentPanel);


		biotypeComboBox.addTextChangeListener(e->{
			biotypePane.setSelection(biotypeComboBox.getSelection());
			editButton.setEnabled(biotypeComboBox.getSelection()!=null);
			//			deleteButton.setEnabled(biotypeComboBox.getSelection()!=null);
		});


		editButton.addActionListener(e-> {
			if(biotypeComboBox.getSelection()!=null) {
				new BiotypeEditDlg(biotypeComboBox.getSelection());
				biotypeComboBox.setValues(DAOBiotype.getBiotypes(true));
				biotypeComboBox.setSelection(biotypeComboBox.getSelection());
			}
		});
		//		deleteButton.addActionListener(e-> {
		//			Biotype biotype = biotypeComboBox.getSelection();
		//			if(biotype!=null) {
		//				int r = JOptionPane.showConfirmDialog(BiotypeOverviewDlg.this, "Are you sure you want to delete " + biotype +"?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		//				if(r!=JOptionPane.YES_OPTION) return;
		//				try {
		//					if(biotype==null || biotype.getId()<=0) throw new Exception("Nothing to delete!?");
		//					if(!Spirit.askReasonForChange()) return;
		//					DAOBiotype.deleteBiotype(biotype, Spirit.askForAuthentication());
		//					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Biotype.class, biotype);
		//					biotypeComboBox.setValues(DAOBiotype.getBiotypes(true));
		//					biotypePane.setSelection(null);
		//					JOptionPane.showMessageDialog(BiotypeOverviewDlg.this, biotype.getName() + " Deleted", "Success", JOptionPane.INFORMATION_MESSAGE);
		//				} catch (Exception e2) {
		//					JExceptionDialog.showError(e2);
		//				}
		//			}
		//		});
		addButton.addActionListener(e-> {
			Biotype type = new Biotype();
			new BiotypeEditDlg(type);
			biotypeComboBox.setValues(DAOBiotype.getBiotypes(true));
			biotypeComboBox.setSelection(type);
			editButton.setEnabled(biotypeComboBox.getSelection()!=null);
		});
		closeButton.addActionListener(e-> {
			dispose();
		});
		editButton.setEnabled(biotypeComboBox.getSelection()!=null);
		//		deleteButton.setEnabled(biotypeComboBox.getSelection()!=null);

		UIUtils.adaptSize(this, 1100, 800);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

}
