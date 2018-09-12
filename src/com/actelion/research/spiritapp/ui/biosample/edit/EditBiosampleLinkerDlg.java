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

package com.actelion.research.spiritapp.ui.biosample.edit;

import java.util.Collections;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.biosample.MetadataComponent;
import com.actelion.research.spiritapp.ui.biosample.MetadataComponentFactory;
import com.actelion.research.spiritapp.ui.biosample.SampleIdScanField;
import com.actelion.research.spiritapp.ui.location.ContainerTextField;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.FastFont;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class EditBiosampleLinkerDlg extends JSpiritEscapeDialog {

	private Biosample b;
	private BiosampleLinker linker;
	private JComponent newValueTextField;

	public EditBiosampleLinkerDlg(Biosample biosample, BiosampleLinker linker) {
		super(UIUtils.getMainFrame(), "Edit Sample - " + biosample.getSampleId(), EditBiosampleLinkerDlg.class.getName());
		assert biosample!=null;
		assert linker!=null;

		this.b = JPAUtil.reattach(biosample);
		this.linker = linker;

		JCustomTextField sampleIdTextField = new JCustomTextField();
		sampleIdTextField.setText(b.getSampleId());
		sampleIdTextField.setEnabled(false);

		JCustomTextField oldValueTextField = new JCustomTextField();
		oldValueTextField.setText(linker.getValue(b));
		oldValueTextField.setEnabled(false);


		if(linker.getBiotypeMetadata()!=null) {
			newValueTextField = MetadataComponentFactory.getComponentFor(linker.getBiotypeMetadata());
			((MetadataComponent)newValueTextField).updateView(b, linker.getBiotypeMetadata());
		} else {
			if(linker.getType()==LinkerType.CONTAINERID) {
				newValueTextField = new ContainerTextField();
			} else if(linker.getType()==LinkerType.SAMPLEID) {
				newValueTextField = new SampleIdScanField();
			} else if(linker.getType()==LinkerType.SAMPLENAME) {
				newValueTextField = new JCustomTextField();
			} else if(linker.getType()==LinkerType.COMMENTS) {
				newValueTextField = new JCustomTextField();
			} else {
				throw new IllegalArgumentException("Not implemented");
			}
			((JTextComponent) newValueTextField).setText(linker.getValue(b));

		}
		JButton okButton = new JIconButton(IconType.SAVE, "Update");
		okButton.addActionListener(e-> ok());

		JPanel contentPane = UIUtils.createTable(
				new JLabel("SampleId: "), sampleIdTextField,
				null, new JCustomLabel("Update " + linker.getLabelShort(), FastFont.BOLD),
				new JLabel("Old value: "), oldValueTextField,
				new JLabel("New value: "), newValueTextField);

		setContentPane(UIUtils.createBox(
				UIUtils.createTitleBox(UIUtils.addPadding(10, 10, contentPane)),
				null,
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton)));
		UIUtils.adaptSize(this, -1, -1);
		setVisible(true);

	}

	public void ok() {
		try {
			if(linker.getBiotypeMetadata()!=null) {
				((MetadataComponent)newValueTextField).updateModel(b, linker.getBiotypeMetadata());
			} else if(linker.getType()==LinkerType.CONTAINERID) {
				assert newValueTextField instanceof JTextComponent;
				linker.setValue(b, ((JTextComponent) newValueTextField).getText());
			}


			if(!Spirit.askReasonForChangeIfUpdated(Collections.singleton(b))) return;
			DAOBiosample.persistBiosamples(Collections.singleton(b), Spirit.getUser());

			SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Biosample.class, b);
		} catch (Exception e) {
			JExceptionDialog.showError(this, e);
		}
	}
}
