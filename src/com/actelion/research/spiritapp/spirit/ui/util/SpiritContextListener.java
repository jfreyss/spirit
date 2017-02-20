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
import java.util.List;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;

public class SpiritContextListener {
	
	private static final List<ISpiritContextObserver> observers = new ArrayList<ISpiritContextObserver>();
	
	
	public static void register(ISpiritContextObserver observer) {
		observers.add(observer);
	}

	public static void setStudy(Study study) {
		try {
			for (ISpiritContextObserver o : observers) {
				o.setStudy(study);
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void setBiosamples(List<Biosample> biosamples) {
		try {
			for (ISpiritContextObserver o : observers) {
				o.setBiosamples(biosamples);
			}		
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void setRack(Location rack) {
		try {
			for (ISpiritContextObserver o : observers) {
				o.setRack(rack);
			}		
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void setLocation(Location location, int pos) {
		try {
			for (ISpiritContextObserver o : observers) {
				o.setLocation(location, pos);
			}	
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void setResults(List<Result> results) {
		try {
			for (ISpiritContextObserver o : observers) {
				o.setResults(results);
			}	
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static void query(final BiosampleQuery q) {
		try {
			for (ISpiritContextObserver o : observers) {
				o.query(q);
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void query(final ResultQuery q) {
		try {
			for (ISpiritContextObserver o : observers) {
				o.query(q);
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void setStatus(String s) {
		try {
			for (ISpiritContextObserver o : observers) {
				o.setStatus(s);
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static void setUser(String s) {
		try {
			for (ISpiritContextObserver o : observers) {
				o.setUser(s);
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	
}
