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

package com.actelion.research.spiritapp.ui.audit;

import java.awt.Color;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;

import com.actelion.research.spiritcore.business.audit.DifferenceItem.ChangeType;
import com.actelion.research.spiritcore.business.audit.RevisionItem;
import com.actelion.research.util.ui.exceltable.ExtendTable;

public class RevisionItemTable extends ExtendTable<RevisionItem> {

	public RevisionItemTable() {
		super(new RevisionItemTableModel());
		setBorderStrategy(BorderStrategy.WHEN_DIFFERENT_VALUE);
	}

	@Override
	public RevisionItemTableModel getModel() {
		return (RevisionItemTableModel) super.getModel();
	}

	@Override
	protected void postProcess(RevisionItem row, int rowNo, Object value, JComponent c) {
		if(row.getChangeType()==ChangeType.DEL) {
			c.setForeground(new Color(170, 0, 0));
		} else if(row.getChangeType()==ChangeType.ADD) {
			c.setForeground(new Color(0, 80, 0));
		} else {
			c.setForeground(new Color(150, 100, 0));
		}
	}

	@Override
	protected boolean shouldMerge(int viewCol, int row1, int row2) {
		RevisionItem rev1 = getRows().get(row1);
		RevisionItem rev2 = getRows().get(row2);
		return rev1.getRevId() == rev2.getRevId();
	}

	@Override
	public void setRows(List<RevisionItem> rows) {
		Set<String> entityTypes = RevisionItem.getEntityTypes(rows);
		getModel().initColumns(entityTypes.size()>1);
		super.setRows(rows);

	}

}
