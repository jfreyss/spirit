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

import java.util.Collection;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent.EventType;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.result.TestComboBox;
import com.actelion.research.spiritapp.ui.util.ISpiritChangeObserver;
import com.actelion.research.spiritapp.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;
import com.actelion.research.util.ui.iconbutton.JIconButton;

public class TestOverviewDlg extends JEscapeDialog implements ISpiritChangeObserver {

	private TestComboBox testChoice = new TestComboBox(true);
	private TestDocumentPane testPane = new TestDocumentPane(DAOTest.getTests(true));

	public TestOverviewDlg() {
		super(UIUtils.getMainFrame(), "Admin - Tests", true);
		SpiritChangeListener.register(this);
		SpiritUser user = SpiritFrame.getUser();
		if(user==null || !SpiritRights.isSuperAdmin(user)) return;
		testChoice.setSelection(null);
		testPane.addHyperlinkListener(e-> {
			if(e.getEventType()!=EventType.ACTIVATED) return;
			if(e.getDescription().startsWith("test:")) {
				String param = e.getDescription().substring(5);
				Test bt = DAOTest.getTest(param);
				testChoice.setText(param);
				testPane.setSelection(bt);
			}
		});

		final JButton renameInputButton = new JButton("Rename Values");
		final JButton newTestButton = new JIconButton(IconType.NEW, "New Test");
		final JButton duplicateButton = new JIconButton(IconType.DUPLICATE, "Duplicate");
		final JButton editButton = new JIconButton(IconType.EDIT, "Edit");
		final JButton deleteButton = new JIconButton(IconType.DELETE, "Delete");

		renameInputButton.addActionListener(e-> {
			new TestRenameAttDlg(testChoice.getSelection());
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
			}
		});
		deleteButton.addActionListener(e-> {
			try {
				Test t = testChoice.getSelection();

				int res = JOptionPane.showConfirmDialog(TestOverviewDlg.this, "Are you sure you want to delete " + t + "?", "Delete Test", JOptionPane.YES_NO_OPTION);
				if(res!=JOptionPane.YES_OPTION) return;
				if(!Spirit.askReasonForChange()) return;
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
			}
		});

		testChoice.addTextChangeListener(e-> {
			testPane.setSelection(testChoice.getSelection());
			renameInputButton.setEnabled(testChoice.getSelection()!=null);
			editButton.setEnabled(testChoice.getSelection()!=null);
			duplicateButton.setEnabled(testChoice.getSelection()!=null);
			deleteButton.setEnabled(testChoice.getSelection()!=null);
		});

		renameInputButton.setEnabled(testChoice.getSelection()!=null);
		editButton.setEnabled(testChoice.getSelection()!=null);
		duplicateButton.setEnabled(testChoice.getSelection()!=null);
		deleteButton.setEnabled(testChoice.getSelection()!=null);

		setContentPane(UIUtils.createBox(UIUtils.createTitleBox(new JScrollPane(testPane)),
				UIUtils.createTitleBox(
						UIUtils.createVerticalBox(
								UIUtils.createHorizontalBox(testChoice, editButton, renameInputButton, duplicateButton, deleteButton, Box.createHorizontalGlue()),
								UIUtils.createHorizontalBox(newTestButton, Box.createHorizontalGlue())
								)),
				UIUtils.createHorizontalBox(Box.createHorizontalGlue(), new JButton(new CloseAction()))));

		testPane.setSelection(testChoice.getSelection());

		UIUtils.adaptSize(this, 1150, 700);
		setLocationRelativeTo(UIUtils.getMainFrame());
		setVisible(true);
	}

	@Override
	public <T> void actionModelChanged(SpiritChangeType action, Class<T> what, Collection<T> details) {
		testPane.setTests(DAOTest.getTests(true));
		if(what==Test.class && details.size()==1) {
			Test t = (Test)details.iterator().next();
			testChoice.setSelection(t);
			testPane.setSelection(t);
		}
	}
}
