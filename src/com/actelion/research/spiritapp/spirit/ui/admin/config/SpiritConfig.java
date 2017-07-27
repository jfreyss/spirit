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

package com.actelion.research.spiritapp.spirit.ui.admin.config;

import com.actelion.research.spiritcore.util.Config;

public class SpiritConfig {
	private Config config = Config.getInstance(".spirit");
	
	public SpiritConfig() {
	}


	public Config getConfig() {
		return config;
	}

	public String getWeighingFile() {
		return config.getProperty("balance.mt", "");
	}
	public void setWeighingFile(String weighingFile) {
		config.setProperty("balance.mt", weighingFile);
	}


	public int getWeighingCol() {
		return config.getProperty("balance.mt.col", 2);		
	}
	public void setWeighingCol(int weighingCol) {
		config.setProperty("balance.mt.col", weighingCol);

	}


//	public String getRPath() {
//		return config.getProperty("r.path", "");
//	}
//	public void setRPath(String rFile) {
//		config.setProperty("r.path", rFile);
//	}
//	
//	public boolean isExpert() {
//		return config.getProperty("result.expert", Boolean.FALSE);
//	}
//	public void setExpert(boolean expert) {
//		config.setProperty("result.expert", expert);
//	}

	
	

}
