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

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent.EventType;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.result.TestChoice;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class TestOverviewDlg extends JEscapeDialog {

	private TestChoice testChoice;
	private TestDocumentPane testPane = new TestDocumentPane();

	public TestOverviewDlg() {
		super(UIUtils.getMainFrame(), "Admin - Tests", true);
		SpiritUser user = SpiritFrame.getUser();
		if(user==null || !SpiritRights.isSuperAdmin(user)) return;

		testPane.addHyperlinkListener(e-> {
			if(e.getEventType()!=EventType.ACTIVATED) return;
			if(e.getDescription().startsWith("test:")) {
				String param = e.getDescription().substring(5);
				Test bt = DAOTest.getTest(param);
				testChoice.setText(param);
				testPane.setSelection(bt);
			}
		});

		testChoice = new TestChoice();
		final JButton renameInputButton = new JButton("Rename Values");
		final JButton moveButton = new JButton("Move Values");
		final JButton newTestButton = new JIconButton(IconType.NEW, "New Test");
		final JButton duplicateButton = new JIconButton(IconType.DUPLICATE, "Duplicate");
		final JButton editButton = new JIconButton(IconType.EDIT, "Edit");
		final JButton deleteButton = new JIconButton(IconType.DELETE, "Delete");



		renameInputButton.addActionListener(e-> {
			new TestRenameAttDlg(testChoice.getSelection());
		});

		moveButton.addActionListener(e-> {
			new TestMoveDlg(testChoice.getSelection());
		});

		newTestButton.addActionListener(e-> {
			Test t = new Test();
			t.setCategory(testChoice.getSelection()==null?null:testChoice.getSelection().getCategory());
			new TestEditDlg(t);
			testChoice.setSelection(t);
		});
		editButton.addActionListener(e-> {
			Test t = testChoice.getSelection();
			if(t!=null) {
				new TestEditDlg(t);
				testChoice.reset();
			}
		});
		deleteButton.addActionListener(e-> {
			try {
				Test t = testChoice.getSelection();
				int res = JOptionPane.showConfirmDialog(TestOverviewDlg.this, "Are you sure you want to delete " + t + "?", "Delete Test", JOptionPane.YES_NO_OPTION);
				if(res!=JOptionPane.YES_OPTION) return;
				DAOTest.removeTest(t, Spirit.askForAuthentication());
				testChoice.reset();
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Test.class, t);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		});

		duplicateButton.addActionListener(e-> {
			Test t = testChoice.getSelection();
			if(t!=null) {
				new TestEditDlg(t.duplicate());
				testChoice.reset();
			}
		});
		testChoice.addActionListener(e-> {
			testPane.setSelection(testChoice.getSelection());
			renameInputButton.setEnabled(testChoice.getSelection()!=null);
			editButton.setEnabled(testChoice.getSelection()!=null);
			moveButton.setEnabled(testChoice.getSelection()!=null);
			duplicateButton.setEnabled(testChoice.getSelection()!=null);
			deleteButton.setEnabled(testChoice.getSelection()!=null);
		});

		renameInputButton.setEnabled(testChoice.getSelection()!=null);
		editButton.setEnabled(testChoice.getSelection()!=null);
		moveButton.setEnabled(testChoice.getSelection()!=null);
		duplicateButton.setEnabled(testChoice.getSelection()!=null);
		deleteButton.setEnabled(testChoice.getSelection()!=null);

		setContentPane(UIUtils.createBox(UIUtils.createTitleBox("Test Details", new JScrollPane(testPane)),
				UIUtils.createTitleBox("",
						UIUtils.createVerticalBox(
								UIUtils.createHorizontalBox(testChoice, editButton, duplicateButton, deleteButton, Box.createHorizontalGlue()),
								UIUtils.createHorizontalBox(newTestButton, Box.createHorizontalGlue()),
								UIUtils.createHorizontalBox(renameInputButton, moveButton, Box.createHorizontalGlue()))),
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new CloseAction()))));

		testPane.setSelection(testChoice.getSelection());

		UIUtils.adaptSize(this, 950, 700);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}
}
