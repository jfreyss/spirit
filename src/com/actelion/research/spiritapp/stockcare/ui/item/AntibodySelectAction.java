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

package com.actelion.research.spiritapp.stockcare.ui.item;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.biosample.selector.SelectorDlg;
import com.actelion.research.spiritapp.stockcare.ui.FilterDlg;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;
import com.actelion.research.spiritcore.business.biosample.BiosampleLinker.LinkerType;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeMetadata;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class AntibodySelectAction extends AbstractAction {

	public AntibodySelectAction() {
		super("Help me select some antibodies");
	}
	@Override
	public void actionPerformed(ActionEvent e) {

		try {
			final Biotype antibodyType = DAOBiotype.getBiotype("Antibody");
			if(antibodyType==null) throw new Exception("Antibody does not exist");

			final BiotypeMetadata antibodyAggMetadata = antibodyType.getMetadata("Fluorophore/Label");
			if(antibodyAggMetadata==null) throw new Exception("Antibody.Fluorophore/Label does not exist");

			Biotype fluorophoreType = DAOBiotype.getBiotype("Fluorophore");
			if(fluorophoreType==null) throw new Exception("Fluorophore does not exist");
			final BiotypeMetadata mt1 = fluorophoreType.getMetadata("Instrument compatibility");
			if(mt1==null) throw new Exception("Fluorophore.Instrument compatibility does not exist");
			final BiotypeMetadata mt2 = antibodyType.getMetadata("Target Species");
			if(mt2==null) throw new Exception("Antibody.Target Species does not exist");
			//

			FilterDlg dlg = new FilterDlg("Please select the instrument and target species", new BiotypeMetadata[] {mt1, mt2}) {
				@Override
				public void onFilterChange() {
					new SwingWorkerExtended() {
						List<Biosample> pool;
						@Override
						protected void doInBackground() throws Exception {
							setInfo("");
							String[] res = getFilters();
							BiosampleQuery q = new BiosampleQuery();
							q.setBiotype(antibodyType);
							if(res[0].length()>0) q.getLinker2values().put(new BiosampleLinker(antibodyAggMetadata, mt1), "*"+res[0]+"*");
							if(res[1].length()>0) q.getLinker2values().put(new BiosampleLinker(antibodyType, mt2), res[1]);
							pool = DAOBiosample.queryBiosamples(q, SpiritFrame.getUser());
						}

						@Override
						protected void done() {
							setInfo(pool.size()+" "+antibodyType);
						}

					};

				}
			};
			if(!dlg.isSuccess()) return;
			String[] res = dlg.getFilters();


			BiosampleQuery q = new BiosampleQuery();
			q.setBiotype(antibodyType);
			if(res[0].length()>0) q.getLinker2values().put(new BiosampleLinker(antibodyAggMetadata, mt1), "*"+res[0]+"*");
			if(res[1].length()>0) q.getLinker2values().put(new BiosampleLinker(antibodyType, mt2), res[1]);
			List<Biosample> pool = DAOBiosample.queryBiosamples(q, SpiritFrame.getUser());

			BiosampleLinker querySel = new BiosampleLinker(LinkerType.SAMPLENAME, antibodyType);
			BiosampleLinker discrimSel = new BiosampleLinker(antibodyAggMetadata, fluorophoreType.getMetadata("Type"));
			//			BiosampleLinker discrimSel = new BiosampleLinker(antibodyAggMetadata, LinkerType.SAMPLENAME, fluorophoreType);
			BiosampleLinker displaySel = new BiosampleLinker(antibodyAggMetadata, LinkerType.SAMPLEID);

			new SelectorDlg(pool, querySel, discrimSel, displaySel);

		} catch(Exception ex) {
			JExceptionDialog.showError(ex);
		}
	}
}
