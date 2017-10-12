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

package com.actelion.research.spiritapp.spirit.ui.biosample.column;

import java.util.List;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAORevision.Revision;
import com.actelion.research.util.FormatterUtils;
import com.actelion.research.util.ui.exceltable.Column;

public class LastChangeColumn extends Column<Biosample, String> {

	public final int revId;

	public LastChangeColumn(int revId) {
		super("Audit Trail\nLast Change", String.class, 120);
		this.revId = revId;
	}

	@Override
	public String getToolTipText() {
		return revId>0?"At Revision "+revId:"Last Change";
	}

	@Override
	public float getSortingKey() {return -1f;}

	@Override
	public String getValue(Biosample b) {
		if(SpiritRights.isBlind(b.getInheritedStudy(), Spirit.getUser())) return "Blind";
		StringBuilder sb = new StringBuilder();
		try {
			//Load all revisions of the object
			List<Revision> revisions = DAORevision.getLastRevisions(b, revId, 2);
			if(revisions.size()==0) return "";
			String diff;
			Revision rev0 = revisions.get(0);
			if(revisions.size()<2) {
				diff = "First version";
			} else {
				Revision rev1 = revisions.get(1);
				if(revId>0 && rev0.getRevId()!=revId) return "Rev was "+rev0.getRevId()+" expected "+revId+ "(second is"+(revisions.size()<1?"":rev0.getRevId());

				//Find the first difference
				Biosample b1 = rev0.getBiosamples().get(0);
				Biosample b2 = rev1.getBiosamples().get(0);
				diff = b1.getDifference(b2);
			}

			if(revId>0 || diff.length()>0) {
				sb.append("<B>" + rev0.getRevId() + " " + FormatterUtils.formatDateTimeShort(rev0.getDate()) + " - " + rev0.getUser() + "\n");
				sb.append(diff.replace(";", "\n"));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return sb.toString();

	}

	@Override
	public boolean isMultiline() {
		return true;
	}

	@Override
	public boolean isHideable() {
		return true;
	}


}
