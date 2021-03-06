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

package com.actelion.research.spiritapp.ui.exchange;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.actelion.research.spiritapp.Spirit;
import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.IconType;

public class ExchangeActions {

	public static class Action_ExportExchange extends AbstractAction {
		private SpiritFrame spirit;
		public Action_ExportExchange(SpiritFrame spirit) {
			super("Export " + SpiritFrame.getApplicationName() + " Data...");
			this.spirit = spirit;
			putValue(AbstractAction.MNEMONIC_KEY, (int)('e'));
			putValue(AbstractAction.SMALL_ICON, IconType.EXCHANGE.getIcon());
			setEnabled(SpiritRights.isSuperAdmin(SpiritFrame.getUser()));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Exchange exchange = spirit.getExchange();
				new ExporterDlg(exchange);
			} catch (Exception ex) {
				JExceptionDialog.showError(ex);
			}
		}
	}


	public static class Action_ImportExchange extends AbstractAction {
		public Action_ImportExchange() {
			super("Import " + SpiritFrame.getApplicationName() + " Data...");
			putValue(AbstractAction.MNEMONIC_KEY, (int)('i'));
			putValue(AbstractAction.SMALL_ICON, IconType.EXCHANGE.getIcon());
			setEnabled(SpiritRights.isSuperAdmin(SpiritFrame.getUser()));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String directory = Spirit.getConfig().getProperty("import.directory", System.getProperty("user.home"));
			JFileChooser fileChooser = new JFileChooser(new File(directory));
			fileChooser.setDialogTitle("Select a Spirit file");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setFileFilter(new FileFilter() {
				@Override
				public String getDescription() {
					return "Spirit File";
				}
				@Override
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".spirit");
				}
			});
			int res = fileChooser.showOpenDialog(UIUtils.getMainFrame());
			if(res!=JFileChooser.APPROVE_OPTION) return;

			File file = fileChooser.getSelectedFile();
			if(file==null) return;
			Spirit.getConfig().setProperty("import.directory", file.getParent());
			new ImporterDlg(file);
		}
	}

}
