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

package com.actelion.research.spiritapp.spirit.ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.JTextComboBox;
import com.actelion.research.util.ui.TextChangeListener;
import com.actelion.research.util.ui.UIUtils;

public class BiotypeRenameMetadataDlg extends JSpiritEscapeDialog {

	private final Biotype biotype;
	private final boolean editNames;
	private final BiotypeMetadata btMetadata;

	private JTextComboBox valuesComboBox = new JTextComboBox();
	private JTextField newValueTextField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 10);
	private JButton renameButton = new JButton(new Action_Rename());
	private JLabel infoLabel = new JLabel();

	public BiotypeRenameMetadataDlg(Biotype type, boolean editNames, BiotypeMetadata btMetadata) {

		super(UIUtils.getMainFrame(), "Admin - Biotype - Rename", BiotypeRenameMetadataDlg.class.getName());
		this.biotype = JPAUtil.reattach(type);
		this.editNames = editNames;
		this.btMetadata = biotype.getMetadata(btMetadata.getName());

		if(biotype==null) throw new IllegalArgumentException("Please select a biotype");

		Box line1 = Box.createHorizontalBox();
		line1.add(new JLabel("Attribute: "));
		line1.add(valuesComboBox);
		line1.add(infoLabel);
		infoLabel.setForeground(Color.GRAY);
		line1.add(Box.createHorizontalGlue());


		Box line3 = Box.createHorizontalBox();
		line3.add(new JLabel("Rename To: "));
		line3.add(newValueTextField);
		line3.add(renameButton);
		line3.add(Box.createHorizontalGlue());

		Box centerPane = Box.createVerticalBox();
		centerPane.setBorder(BorderFactory.createEtchedBorder());
		centerPane.add(line1);
		centerPane.add(Box.createVerticalGlue());
		centerPane.add(line3);



		repopulateValues();

		valuesComboBox.addTextChangeListener(new TextChangeListener() {
			@Override
			public void textChanged(javax.swing.JComponent src) {
				String value = valuesComboBox.getText();
				newValueTextField.setText(value);
				renameButton.setEnabled(value!=null);
			}
		});

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, centerPane);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new Action_Close())));
		setContentPane(contentPane);
		setSize(600, 195);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

	private void repopulateValues() {
		Set<String> values;
		if(editNames) {
			values = DAOBiotype.getAutoCompletionFieldsForName(biotype, null);
			infoLabel.setText("");
		} else {
			values = DAOBiotype.getAutoCompletionFields(btMetadata, null);
			infoLabel.setText("id:"+btMetadata.getId());
		}
		valuesComboBox.setChoices(values);
	}

	private class Action_Rename extends AbstractAction {
		public Action_Rename() {
			super("Rename");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				SpiritUser user = Spirit.askForAuthentication();
				String value = valuesComboBox.getText();
				String newValue = newValueTextField.getText();

				if(newValue.length()==0) throw new Exception("Please enter a new value");

				int res;
				if(editNames) {
					res = DAOBiotype.renameNames(biotype, value, newValue, user);
				} else {
					res = DAOBiotype.renameMetadata(btMetadata, value, newValue, user);
				}
				JOptionPane.showMessageDialog(UIUtils.getMainFrame(), res + " values renamed", "Success", JOptionPane.INFORMATION_MESSAGE);
				repopulateValues();
				valuesComboBox.setText(newValue);

			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}

	private class Action_Close extends AbstractAction {
		public Action_Close() {
			super("Close");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	}
}
