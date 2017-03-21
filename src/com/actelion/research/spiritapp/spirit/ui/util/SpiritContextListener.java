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

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;

public class SpiritContextListener {
	
	private static final List<ISpiritContextObserver> observers = new ArrayList<>();
	
	
	public static void register(ISpiritContextObserver observer) {
		observers.add(observer);
	}

	public static void setStudy(Study study) {
		LoggerFactory.getLogger(SpiritContextListener.class).debug("Set Study " +study);
		try {
			for (ISpiritContextObserver o : observers) {
				o.setStudy(study);
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void setBiosamples(List<Biosample> biosamples) {
		LoggerFactory.getLogger(SpiritContextListener.class).debug("Set Biosample " + (biosamples==null? "NULL": "n=" + biosamples.size()));
		try {
			for (ISpiritContextObserver o : observers) {
				o.setBiosamples(biosamples);
			}		
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void setRack(Location rack) {
		LoggerFactory.getLogger(SpiritContextListener.class).debug("Set Rack " + rack);
		try {
			for (ISpiritContextObserver o : observers) {
				o.setRack(rack);
			}		
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void setLocation(Location location, int pos) {
		LoggerFactory.getLogger(SpiritContextListener.class).debug("Set Location " + location + (pos>=0? " / " + pos:""));
		try {
			for (ISpiritContextObserver o : observers) {
				o.setLocation(location, pos);
			}	
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void setResults(List<Result> results) {
		LoggerFactory.getLogger(SpiritContextListener.class).debug("Set Result " + (results==null? "NULL": "n=" + results.size()));
		try {
			for (ISpiritContextObserver o : observers) {
				o.setResults(results);
			}	
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static void query(final BiosampleQuery q) {
		LoggerFactory.getLogger(SpiritContextListener.class).debug("Set BiosampleQuery " + (q==null?"": q.getSuggestedQueryName()));
		try {
			for (ISpiritContextObserver o : observers) {
				o.query(q);
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void query(final ResultQuery q, int selectGraph) {
		LoggerFactory.getLogger(SpiritContextListener.class).debug("Set ResultQuery");
		try {
			for (ISpiritContextObserver o : observers) {
				o.query(q, selectGraph);
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public static void setStatus(String s) {
		LoggerFactory.getLogger(SpiritContextListener.class).debug("Set Status "+s);
		try {
			for (ISpiritContextObserver o : observers) {
				o.setStatus(s);
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static void setUser(String s) {
		LoggerFactory.getLogger(SpiritContextListener.class).debug("Set User "+s);
		try {
			for (ISpiritContextObserver o : observers) {
				o.setUser(s);
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
	
}
