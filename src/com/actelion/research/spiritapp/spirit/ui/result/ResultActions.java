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

package com.actelion.research.spiritapp.spirit.ui.result;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.admin.AdminActions;
import com.actelion.research.spiritapp.spirit.ui.lf.UserIdComboBox;
import com.actelion.research.spiritapp.spirit.ui.pivot.PivotTable;
import com.actelion.research.spiritapp.spirit.ui.result.dialog.ResultDuplicatesDlg;
import com.actelion.research.spiritapp.spirit.ui.result.dialog.ResultHistoryDlg;
import com.actelion.research.spiritapp.spirit.ui.result.dialog.SetResultQualityDlg;
import com.actelion.research.spiritapp.spirit.ui.result.edit.EditResultDlg;
import com.actelion.research.spiritapp.spirit.ui.result.edit.ResultDiscardDlg;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.PopupAdapter;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;

public class ResultActions {

	public static class Action_New extends AbstractAction {
		private Test defaultTest;
		public Action_New() {
			super("New Results");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('n'));
			putValue(Action.SMALL_ICON, IconType.RESULT.getIcon());
		}
		public Action_New(Test defaultTest) {
			super("New Results");
			this.defaultTest = defaultTest;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('n'));
			putValue(Action.SMALL_ICON, IconType.RESULT.getIcon());
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			List<Result> results = new ArrayList<>();
			Result result = new Result(defaultTest);
			results.add(result);
			new EditResultDlg(true, results);
		}
	}


	public static class Action_Edit_ELB extends AbstractAction {
		private String elb = null;
		private final Result selectedResult;
		public Action_Edit_ELB(String elb, Result selectedResult) {
			super("Edit " + (elb==null? "ELB" : elb));
			putValue(AbstractAction.MNEMONIC_KEY, (int)('l'));
			putValue(Action.SMALL_ICON, IconType.EDIT.getIcon());
			setEnabled(elb!=null);
			this.elb = elb;
			this.selectedResult = selectedResult;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new EditResultDlg(elb, selectedResult);
		}
	}
	public static class Action_Edit_Results extends AbstractAction {
		private List<Result> results;

		public Action_Edit_Results() {
			super("Edit Selected Results");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('e'));
			putValue(Action.SMALL_ICON, IconType.EDIT.getIcon());
		}
		public Action_Edit_Results(List<Result> results) {
			this();

			this.results = results;

			boolean enabled = results.size()>0;
			for (Result result : results) {
				if(!SpiritRights.canEdit(result, SpiritFrame.getUser())) {
					enabled = false;
					break;
				}
			}
			setEnabled(enabled);
		}
		public List<Result> getResults() {
			return results;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(getResults()==null || getResults().size()==0) return;
			new EditResultDlg(getResults());
		}
	}

	public static class Action_Delete_Results extends AbstractAction {
		private final List<Result> results;

		public Action_Delete_Results(List<Result> results) {
			super("Delete Selected Results (Owner)");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			putValue(Action.SMALL_ICON, IconType.DELETE.getIcon());

			this.results = results;

			boolean enabled = results.size()>0;
			for (Result result : results) {
				if(!SpiritRights.canDelete(result, SpiritFrame.getUser())) {
					enabled = false;
					break;
				}
			}
			setEnabled(enabled);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				ResultDiscardDlg.createDialogForDelete(results);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}

	public static class Action_Find_Duplicate_Results extends AbstractAction {
		public Action_Find_Duplicate_Results() {
			super("Find Duplicated Results");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('d'));
			putValue(Action.SMALL_ICON, IconType.DUPLICATE.getIcon());
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			new ResultDuplicatesDlg();
		}
	}

	public static class Action_AssignTo extends AbstractAction {
		private List<Result> results;
		public Action_AssignTo(List<Result> results) {
			super("Change Ownership (owner)");
			this.results = results;

			putValue(AbstractAction.SMALL_ICON, IconType.ADMIN.getIcon());
			putValue(AbstractAction.MNEMONIC_KEY, (int)'o');

			boolean enabled = results.size()>0;
			for (Result result : results) {
				if(!SpiritRights.canDelete(result, SpiritFrame.getUser())) {
					enabled = false;
					break;
				}
			}
			setEnabled(enabled);
		}
		@Override
		public void actionPerformed(ActionEvent ev) {


			UserIdComboBox userIdComboBox = new UserIdComboBox();
			int res = JOptionPane.showOptionDialog(UIUtils.getMainFrame(),
					UIUtils.createVerticalBox(
							new JLabel("To whom would you like to assign those " + results.size() + " results?"),
							UIUtils.createHorizontalBox(userIdComboBox, Box.createHorizontalGlue())),
					"Change ownership",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					null,
					null);
			if(res!=JOptionPane.YES_OPTION) return;
			JPAUtil.pushEditableContext(SpiritFrame.getUser());
			try {
				String name = userIdComboBox.getText();
				if(name.length()==0) return;

				results = JPAUtil.reattach(results);
				SpiritUser admin = Spirit.askForAuthentication();
				SpiritUser u = DAOSpiritUser.loadUser(name);
				if(u==null) throw new Exception(name + " is an invalid user");
				res = JOptionPane.showConfirmDialog(null, "Are you sure to update the updUser to " + u.getUsername()+" and the department to "+u.getMainGroup()+"?", "Change Ownership", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(res!=JOptionPane.YES_OPTION) return;

				DAOResult.changeOwnership(results, u, admin);
				SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_UPDATED, Result.class, results);
			} catch (Exception e) {
				JExceptionDialog.showError(e);
			} finally {
				JPAUtil.popEditableContext();
			}

		}
	}

	//	public static class Action_DeleteResults extends AbstractAction {
	//		private final List<Result> results;
	//		public Action_DeleteResults(List<Result> results) {
	//			super("Delete results from experiment");
	//			this.results = results;
	//			putValue(AbstractAction.SMALL_ICON, IconType.DELETE.getIcon());
	//			for (Result result : results) {
	//				if(!SpiritRights.canDelete(result, Spirit.getUser())) {setEnabled(false); break;}
	//			}
	//		}
	//		@Override
	//		public void actionPerformed(ActionEvent e) {
	//			DeleteResultDlg.showDeleteDialog(results);
	//		}
	//	}


	public static class Action_SetQuality extends AbstractAction {
		private final List<Result> results;
		private Quality quality;
		public Action_SetQuality(List<Result> results, Quality quality) {
			super(quality.getName());
			this.results = results;
			this.quality = quality;
			putValue(AbstractAction.MNEMONIC_KEY, (int)(quality.getName().charAt(0)));
			for (Result result : results) {
				if(!SpiritRights.canEdit(result, SpiritFrame.getUser())) setEnabled(false);
			}
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new SetResultQualityDlg(results, quality);
		}
	}


	public static class Action_History extends AbstractAction {
		private final Result result;
		public Action_History(Result result) {
			super("View Change History");
			this.result = result;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('h'));
			putValue(Action.SMALL_ICON, IconType.HISTORY.getIcon());
			setEnabled(result!=null);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				List<Revision> revisions = DAORevision.getRevisions(result);
				new ResultHistoryDlg(revisions);
			} catch(Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}



	public static void attachPopup(final ResultTable table) {
		table.addMouseListener(new PopupAdapter(table) {
			@Override
			protected void showPopup(MouseEvent e) {
				ResultActions.createPopup(table.getSelection()).show(table, e.getX(), e.getY());
			}
		});
	}

	public static void attachPopup(final PivotTable table) {
		table.addMouseListener(new PopupAdapter(table) {
			@Override
			protected void showPopup(MouseEvent e) {
				List<Result> results = table.getSelectedResults();

				ResultActions.createPopup(results).show(table, e.getX(), e.getY());
			}
		});
	}

	public static void attachPopup(final JComponent comp) {
		comp.addMouseListener(new PopupAdapter() {
			@Override
			protected void showPopup(MouseEvent e) {
				ResultActions.createPopup(null).show(comp, e.getX(), e.getY());
			}
		});
	}

	public static JPopupMenu createPopup(List<Result> results) {
		JPopupMenu menu = new JPopupMenu();
		if(results!=null && results.size()>0) {
			menu.add(new JCustomLabel("   Results: " + (results.size()>1?" "+results.size()+" selected":""), Font.BOLD));
			menu.add(new JSeparator());

			String elb = null;
			for (Result result : results) {
				if(elb==null) {
					elb = result.getElb();
				} else if(!elb.equals(result.getElb())) {
					elb = null;
					break;
				}
			}

			JMenu newMenu = new JMenu("New");
			newMenu.setIcon(IconType.NEW.getIcon());
			newMenu.setMnemonic('n');
			menu.add(newMenu);
			newMenu.add(new Action_New());

			JMenu editMenu = new JMenu("Edit");
			editMenu.setIcon(IconType.EDIT.getIcon());
			editMenu.setMnemonic('e');
			menu.add(editMenu);
			editMenu.add(new Action_Edit_ELB(elb, results.get(0)));
			editMenu.add(new Action_Edit_Results(results));
			editMenu.add(new JSeparator());
			JMenu markMenu = new JMenu("Set Quality");
			markMenu.setIcon(IconType.QUALITY.getIcon());
			for (Quality quality : Quality.values()) {
				markMenu.add(new Action_SetQuality(results, quality));
			}
			editMenu.add(markMenu);

			menu.add(new JSeparator());

			JMenu systemMenu = new JMenu("Advanced");
			systemMenu.setIcon(IconType.ADMIN.getIcon());

			systemMenu.add(new Action_Delete_Results(results));
			systemMenu.add(new JSeparator());
			systemMenu.add(new Action_AssignTo(results));
			systemMenu.add(new JSeparator());
			systemMenu.add(new Action_History(results.size()==1? results.get(0): null));
			menu.add(systemMenu);

		}
		return menu;
	}

	public static void attachRevisionPopup(final ResultTable table) {
		table.addMouseListener(new PopupAdapter(table) {
			@Override
			protected void showPopup(MouseEvent e) {

				List<Result> objects = table.getSelection();
				JPopupMenu popupMenu = new JPopupMenu();
				String s = SpiritFrame.getUser()!=null && SpiritFrame.getUser().isSuperAdmin() && objects!=null && objects.size()==1? " (id:" + objects.get(0).getId()+")":"";
				popupMenu.add(new JCustomLabel("   Result Menu"+s, Font.BOLD));

				if(objects==null || objects.size()==0) {
				} else if(objects.size()==1) {
					popupMenu.add(new JMenuItem(new AdminActions.Action_Restore(objects)));
					//					popupMenu.add(new JMenuItem(new AdminActions.Action_Rollback(revision)));
					popupMenu.add(new JSeparator());
					popupMenu.add(new JMenuItem(new Action_History(objects.get(0))));
				} else { //batch
					popupMenu.add(new JMenuItem(new AdminActions.Action_Restore(objects)));

				}

				popupMenu.show(table, e.getX(), e.getY());
			}
		});
	}

}
