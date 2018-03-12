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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiosample.AmountOp;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class UpdateAmountDlg extends JSpiritEscapeDialog {

	private JComboBox<String> whatComboBox = new JComboBox<String>(new String[] { "", "Add", "Substract", "Set To"});
	private JCustomTextField doubleTextField = new JCustomTextField(CustomFieldType.DOUBLE, 5);

	public UpdateAmountDlg(final List<Biosample> mySamples) throws Exception {
		super(UIUtils.getMainFrame(), "Biosample - Update Amount", UpdateAmountDlg.class.getName());

		final List<Biosample> biosamples = JPAUtil.reattach(mySamples);

		Biotype type = null;
		for (Biosample b : biosamples) {
			if(type==null) type = b.getBiotype();
			else if(!type.equals(b.getBiotype())) throw new Exception("All samples must have the same type");
		}
		if(type==null) throw new Exception("No biotype");
		if(type.getAmountUnit()==null) throw new Exception("No amount");

		Box northPanel = Box.createVerticalBox();
		northPanel.setBorder(BorderFactory.createTitledBorder(""));
		northPanel.add(UIUtils.createHorizontalBox(new JCustomLabel("Update " + type.getAmountUnit().getName() + " of " + (biosamples.size()==1? biosamples.get(0).getSampleId(): biosamples.size()+" "+type.getName()), Font.ITALIC), Box.createHorizontalGlue()));
		northPanel.add(UIUtils.createHorizontalBox(whatComboBox, Box.createHorizontalStrut(10), doubleTextField, new JLabel(type.getAmountUnit().getUnit())));

		JButton okButton = new JIconButton(IconType.SAVE, "Update Amount");
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if(whatComboBox.getSelectedIndex()==0) throw new Exception("You must select Add or Substract");
					if(doubleTextField.getTextDouble()==null) throw new Exception("You must give a volume");
					if(doubleTextField.getTextDouble()<0) throw new Exception("You must give a positive volume");
					updateAmount(biosamples, whatComboBox.getSelectedIndex()==1? AmountOp.ADD: whatComboBox.getSelectedIndex()==2? AmountOp.SUBSTRACT: AmountOp.SET, doubleTextField.getTextDouble(), Spirit.askForAuthentication());
					dispose();
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, biosamples);
				} catch (Exception ex) {
					JExceptionDialog.showError(ex);
				}
			}
		});

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(BorderLayout.CENTER, northPanel);
		contentPane.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton));


		setContentPane(contentPane);
		pack();
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

	public static void updateAmount(List<Biosample> biosamples, AmountOp op, double amount, SpiritUser user) throws Exception {
		for (Biosample b : biosamples) {
			if (!SpiritRights.canEdit(b, user)) {
				throw new Exception("You are not allowed to update the amount");
			}
			if (op == AmountOp.SUBSTRACT && b.getAmount() != null && b.getAmount() < amount) {
				throw new Exception(b.getSampleId() + " has only " + b.getAmount() + " left");
			}
		}

		for (Biosample b : biosamples) {
			Double before = b.getAmount();
			if (op == AmountOp.ADD && before != null) {
				b.setAmount(before + amount);
			} else if (op == AmountOp.SUBSTRACT && before != null) {
				b.setAmount(Math.max(before - amount, 0));
			} else if (op == AmountOp.SET) {
				b.setAmount(amount);
			}
		}

		DAOBiosample.persistBiosamples(biosamples, user);
	}

}
