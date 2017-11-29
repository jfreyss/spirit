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

package com.actelion.research.spiritapp.ui.util.correction;

import java.util.ArrayList;
import java.util.List;

public class Correction<ATTRIBUTE, DATA> {

	private final String value;
	private final List<String> suggestedValues;
	private final String suggestedValue;
	private final ATTRIBUTE attribute;
	private final float score;
	private final List<DATA> affectedData = new ArrayList<DATA>();
	private final boolean mustBeChanged;
	
	public Correction(ATTRIBUTE attribute, String value, List<String> suggestedValues, String suggestedValue, float score, boolean mustBeChanged) {
		this.attribute = attribute; 
		this.value = value; 
		this.suggestedValues = suggestedValues;
		this.suggestedValue = suggestedValue;
		this.score = score;
		this.mustBeChanged = mustBeChanged;
	}
	
	public boolean isMustBeChanged() {
		return mustBeChanged;
	}
	
	public String getValue() {
		return value;
	}	
	public List<String> getSuggestedValues() {
		return suggestedValues;
	}
	public String getSuggestedValue() {
		return suggestedValue;
	}
	public ATTRIBUTE getAttribute() {
		return attribute;
	}
	public List<DATA> getAffectedData() {
		return affectedData;
	}
	public float getScore() {
		return score;
	}
	
}
