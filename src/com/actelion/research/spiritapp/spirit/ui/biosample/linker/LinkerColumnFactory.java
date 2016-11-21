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

package com.actelion.research.spiritapp.spirit.ui.biosample.linker;

import com.actelion.research.spiritcore.business.biosample.BiosampleLinker;

public class LinkerColumnFactory {

	/**
	 * Factory
	 * @param linker
	 * @return
	 */
	public static AbstractLinkerColumn<?> create(BiosampleLinker linker) {
		switch(linker.getType()) {
		case SAMPLEID:
			return new SampleIdColumn(linker, true, true);
		case SAMPLENAME:
			return new SampleNameColumn(linker);
		case METADATA:
			switch(linker.getBiotypeMetadata().getDataType()) {
				case D_FILE: return new DocumentColumn(linker);
				case BIOSAMPLE: return new LinkedBiosampleColumn(linker);
				default: return new MetadataColumn(linker);
			}
		case COMMENTS:
			return new CommentsColumn(linker); 
		default: throw new IllegalArgumentException("Not implemented");
		}
	}

}
