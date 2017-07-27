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

package com.actelion.research.spiritapp.spirit.ui.study.edit;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.swing.JOptionPane;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.LongTaskDlg;
import com.actelion.research.util.ui.UIUtils;

public class StudyDiscardDlg {


	public static void createDialogForDelete(Study s) throws Exception {
		
		final SpiritUser user = Spirit.askForAuthentication();		
		final Study study = JPAUtil.reattach(s);			
		
		final List<Biosample> biosamples = DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForStudyIds(s.getStudyIdAndInternalId()), user);
		final List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForStudyIds(s.getStudyIdAndInternalId()), user);

		int res = JOptionPane.showOptionDialog(UIUtils.getMainFrame(), "Are you sure you want to 'DEFINITELY' delete " + study + "\b (" + biosamples.size() + " samples, " + results.size() + " results)", "DELETE Study", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {"Delete", "Cancel"}, "Cancel");
		if(res!=0) return;
		


		new LongTaskDlg("Deleting Study") {				
			@Override
			public void longTask() throws Exception {
				JPAUtil.pushEditableContext(SpiritFrame.getUser());
				EntityTransaction txn = null;
				try {
					EntityManager session = JPAUtil.getManager();
					txn = session.getTransaction();
					txn.begin();

//					DAOResult.deleteResults(session, results, user);
//					DAOBiosample.deleteBiosamples(session, biosamples, user);
					DAOStudy.deleteStudies(session, Collections.singleton(study), true, user);
					
					txn.commit();					
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Study.class, study);
				} catch(Exception e) {
					e.printStackTrace();
					if(txn!=null) try {txn.rollback();} catch (Exception e2) {}
					throw e;
				} finally {
					JPAUtil.popEditableContext();			
				}
			}
		};
	}
	
}
