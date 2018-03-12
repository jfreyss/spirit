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

package com.actelion.research.util.ui.scanner;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used to configure the different plates, which can be scanned.
 * Each configuration contains a name, the XTR/FluidX registry's name, the last scanned position, and if the tubes can contain biosample and/or biosamples
 *
 * @author freyssj
 *
 */
public enum ScannerConfiguration {

	SCANNER_CONFIGURATION_MATRIX_PP("8x12 Matrix 0.5PP", "Custom 2", "H12", "Matrix 0.5PP", true, true),
	SCANNER_CONFIGURATION_MATRIX_1_0PP("8x12 Matrix 1.0PP", "Custom 2", "H12", "Matrix 1.0PP", true, true),
	SCANNER_CONFIGURATION_MATRIX_GLASS("8x12 Matrix Glass", "Custom 4", "H12", null, true, false),
	SCANNER_CONFIGURATION_FLUIX_0_5("8x12 FluidX 0.5PP", "FluidX", "H12", "FluidX 0.5PP", false, true),
	SCANNER_CONFIGURATION_FLUIX_1_4("8x12 FluidX 1.4PP", "FluidX", "H12", "FluidX 1.4PP", false, true),
	SCANNER_CONFIGURATION_CRYO_120("12x10 Cryotube", "Cryotube 120", "L10", "Cryotube", false, true),
	SCANNER_CONFIGURATION_CRYO_24("6x4 Cryotube", "Custom 5", "D06", "Cryotube", false, true),
	SCANNER_CONFIGURATION_RACK24("4x6 Rack 24", "Custom 3", "D06", null, true, false),

	SCANNER_CONFIGURATION_OPENFILE("Paste From a list", null, null, null, false, false);


	public final String name;
	public final String regEditConfig;
	public final String last;
	public final String defaultTubeType;
	private final boolean allowCompounds;
	private final boolean allowBiosamples;

	private ScannerConfiguration(String name, String regEditConfig, String last, String defaultTubeType, boolean allowCompounds, boolean allowBiosamples) {
		this.name = name;
		this.regEditConfig = regEditConfig;
		this.last = last;
		this.defaultTubeType = defaultTubeType;
		this.allowCompounds = allowCompounds;
		this.allowBiosamples = allowBiosamples;
	}

	public static ScannerConfiguration[] valuesForBiosamples() {
		List<ScannerConfiguration> res = new ArrayList<ScannerConfiguration>();
		for(ScannerConfiguration config: values()) {
			if(config.allowBiosamples) res.add(config);
		}
		return res.toArray(new ScannerConfiguration[res.size()]);
	}

	public static ScannerConfiguration[] valuesForCompounds() {
		List<ScannerConfiguration> res = new ArrayList<ScannerConfiguration>();
		for(ScannerConfiguration config: values()) {
			if(config.allowCompounds) res.add(config);
		}
		return res.toArray(new ScannerConfiguration[res.size()]);
	}

	public String getDefaultTubeType() {
		return defaultTubeType;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	public int getRows() {
		try {
			return last.charAt(0) - 'A' + 1;
		} catch(Exception e) {
			System.err.println("getRows on ScannerConfiguration."+this+" returned "+e);
			return 8;
		}
	}

	public int getCols() {
		try {
			return Integer.parseInt(last.substring(1));
		} catch(Exception e) {
			System.err.println("getCols on ScannerConfiguration."+this+" returned "+e);
			return 12;
		}
	}

	public boolean isAllowBiosamples() {
		return allowBiosamples;
	}
	public boolean isAllowCompounds() {
		return allowCompounds;
	}



}