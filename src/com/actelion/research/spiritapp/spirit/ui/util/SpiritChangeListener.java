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

package com.actelion.research.spiritapp.spirit.ui.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.actelion.research.spiritapp.spirit.ui.SpiritFrame;
import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.spiritcore.services.dao.JPAUtil;

public class SpiritChangeListener {

	private static final List<ISpiritChangeObserver> observers = new ArrayList<>();


	public static void register(ISpiritChangeObserver observer) {
		observers.add(observer);
	}

	public final static<T extends IObject> void fireModelChanged(SpiritChangeType action) {
		fireModelChanged(action, null, (T) null);
	}

	/**
	 * Fires an update event, must be called after a dialog is disposed.
	 * @param action
	 * @param what
	 * @param detail
	 */
	public final static<T extends IObject> void fireModelChanged(SpiritChangeType action, Class<T> what, T detail) {
		SpiritChangeListener.fireModelChanged(action, what, Collections.singletonList(detail));
	}

	public final static<T extends IObject> void fireModelChanged(final SpiritChangeType action, final Class<T> what, final Collection<T> details) {
		try {
			//Clearing cache
			SpiritFrame.clearAll();

			//Firing Events on the non editable context
			for(int i=0; i<50 && JPAUtil.isEditableContext(); i++) {
				System.out.println("SpiritChangeListener.fireModelChanged() WAIT UNTIL CONTEXT IS NON EDITABLE "+i);
				try {Thread.sleep(50);} catch(Exception e) {break;}
			}
			for (ISpiritChangeObserver listener : new ArrayList<ISpiritChangeObserver>(observers)) {
				listener.actionModelChanged(action, what, details);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
