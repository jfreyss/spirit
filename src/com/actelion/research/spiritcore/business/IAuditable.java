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

package com.actelion.research.spiritcore.business;

import java.io.Serializable;

import com.actelion.research.spiritcore.business.audit.DifferenceList;
import com.actelion.research.spiritcore.business.property.SpiritProperty;

public interface IAuditable  {

	/**
	 * Returns a DifferenceList containing the differences between 2 samples (usually 2 different versions).
	 * The result is an empty list if there are no differences or if b is null
	 * @param previous
	 * @return
	 */
	public DifferenceList getDifferenceList(IAuditable previous);

	public default int getSid() {return 0;}

	default public Serializable getSerializableId() {
		Serializable id;
		if(this instanceof IObject) {
			id = ((IObject)this).getId();
		} else if (this instanceof SpiritProperty) {
			id = ((SpiritProperty)this).getId();
		} else {
			System.err.println(this + " is not monitored");
			assert false;
			return null;
		}
		return  id;
	}

}
