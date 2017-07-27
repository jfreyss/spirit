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

	public static int revId;

	public LastChangeColumn() {
		super("Audit\nLast Change", String.class, 120);
		revId = -1;
	}

	public static void setRevId(int revId) {
		LastChangeColumn.revId = revId;
	}

	public static int getRevId() {
		return revId;
	}

	@Override
	public float getSortingKey() {return -1f;}

	@Override
	public String getValue(Biosample b) {
		if(SpiritRights.isBlind(b.getInheritedStudy(), Spirit.getUser())) return "Blind";
		StringBuilder sb = new StringBuilder();
		try {
			List<Revision> revisions = DAORevision.getRevisions(b, revId);
			for (int i = 0; i < revisions.size(); i++) {
				Revision rev = revisions.get(i);

				String diff;
				Biosample b1 = revisions.get(i).getBiosamples().get(0);
				if(i+1<revisions.size()) {
					Biosample b2 = revisions.get(i+1).getBiosamples().get(0);
					diff = b1.getDifference(b2);
				} else {
					Biosample b2 = revisions.get(0).getBiosamples().get(0);
					diff = b1.getDifference(b2);
					if(diff.length()==0) diff = "First version";
				}

				if(revId<0 && diff.length()==0) continue;
				sb.append("<B>" + FormatterUtils.formatDateTimeShort(rev.getDate()) + " - " + rev.getUser() + "\n");
				sb.append(diff.replace(";", "\n"));
				break;
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
