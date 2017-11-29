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

package com.actelion.research.spiritapp.report;


public class ReportParameter {
	
	public String label;
	public Object defaultValue = null;
	public Object[] values = null;
			
	public ReportParameter(String label, Object defaultValue) {
		this(label, defaultValue, null);
	}
	public ReportParameter(String label, Object defaultValue, Object[] values) {
		assert label!=null;
		assert defaultValue!=null;
		
		this.label = label;
		this.defaultValue = defaultValue;
		this.values = values;
	}

	public String getLabel() {return label;}
	public Object getDefaultValue() {return defaultValue;}
	public Object[] getValues() {return values;}
	
	
	@Override
	public int hashCode() {
		return label.hashCode();
	}

}
