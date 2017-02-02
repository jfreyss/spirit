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

package com.actelion.research.spiritapp.spirit.services.report;

import java.util.List;

public class MixedReport extends AbstractReport {

	private List<AbstractReport> reports;
	
	public MixedReport(List<AbstractReport> reports) {
		super(null, "Reports", "Mix of several reports");
		this.reports = reports;
	}
	
	
	@Override
	protected void populateWorkBook() throws Exception {
		StringBuilder sb = new StringBuilder();
		for (AbstractReport rep : reports) {
			try {
				rep.initFromReport(this);
				rep.populateWorkBook();
			} catch(Exception e) {
				e.printStackTrace();
				sb.append(e);
			}
		}
		if(sb.length()>0) {
			throw new Exception(sb.toString());
		}
	}

}
