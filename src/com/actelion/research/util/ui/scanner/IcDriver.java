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

import java.util.List;
import java.util.StringJoiner;

import com.actelion.research.util.ui.LongTaskDlg;

/**
 * This class executes commands that are sent to the Intellicode driver
 * The
 */
public class IcDriver extends FluidxScanner {
	// commands essentially have a start/middle & end. The sequence is..
	// 1) ack.  Command is prefixed with ack: to acknowledge the command. E.g, ack:list acknowldges the list command
	// 2) msg: During processing,  messages are recieved with this prefix
	// 3) warning: During processing,  warning messages are recieved with this prefix
	// 4) success: Command is prefixed with sucess: when the command was successful. E.g, success:list for a successful list command
	// 5) fail: Command is prefixed with fail: when the command failed. E.g, fail:list for a failed list command

	private String reader = "Perception";
	private String profile = "Test_96";
	private String exporter = "Text Exporter";
	private static IcDriver instance = null;
	private boolean deviceReady = false;
	
	private IcDriver() {
		
	}
	
	public static IcDriver getInstance() {
		if (instance == null) {
			instance = new IcDriver();
		}
		
		return instance;
	}
	
	@Override
	public Plate scanPlate(ScannerConfiguration config) throws Exception {

		if("true".equals(System.getProperty("simulateScanner"))) {
			return new Plate(config.getRows(), config.getCols(), getTestTubes());
		}

		try {
			StringJoiner joiner = new StringJoiner("\n");
			IcChannel icChannel = new IcChannel();

			if (config == ScannerConfiguration.SCANNER_CONFIGURATION_FLUIX_1_4) {
				profile = reader.toLowerCase() + "_96_1.4";
			} else if (config == ScannerConfiguration.SCANNER_CONFIGURATION_FLUIX_1_4) {
				profile = reader.toLowerCase() + "_96_0.5";
			} else if (config == ScannerConfiguration.SCANNER_CONFIGURATION_FLUIDX_96
					|| config == ScannerConfiguration.SCANNER_CONFIGURATION_96) {
				profile = reader.toLowerCase() + "_96";
			} else if (config == ScannerConfiguration.SCANNER_CONFIGURATION_FLUIDX_48
					|| config == ScannerConfiguration.SCANNER_CONFIGURATION_48) {
				profile = reader.toLowerCase() + "_48";
			} else if (config == ScannerConfiguration.SCANNER_CONFIGURATION_RACK24
					|| config == ScannerConfiguration.SCANNER_CONFIGURATION_FLUIDX_24) {
				profile = reader.toLowerCase() + "_24";
			}

			new LongTaskDlg("Scanning ") {
				List<String> response;

				@Override
				public void longTask() throws Exception {
					if ( !deviceReady ) {
						icChannel.send("Intellicode.Instrument.use(" + reader + ")");
						if (icChannel.getCmdState() != IcChannel.CmdState.SUCCESS)
							throw new Exception("Cannot set instrument : " + reader);
						icChannel.send("Intellicode.Instrument.Profile.load(" + profile + ".xtprof)");
						if (icChannel.getCmdState() != IcChannel.CmdState.SUCCESS)
							throw new Exception("Cannot load profile : " + profile);
						deviceReady = true;
					}
					icChannel.send("Intellicode.Instrument.Profile.scan");
					if (icChannel.getCmdState() != IcChannel.CmdState.SUCCESS)
						throw new Exception("Cannot scan plate");
					icChannel.send("Intellicode.Instrument.Profile.Exporter.getResults(" + exporter + ")");
					if (icChannel.getCmdState() != IcChannel.CmdState.SUCCESS)
						throw new Exception("Cannot get results");
					response = icChannel.getResponse();
					for (String s : response) {
						joiner.add(s);
					}
				}
			};

			return new Plate(config.getRows(), config.getCols(), parseResults(joiner.toString()));
		} catch (Exception e) {
			deviceReady = false;
			e.printStackTrace();
		}

		return null;
	}

	public static void main(String[] args) {
		try {
			IcChannel icChannel = new IcChannel();

			//icChannel.send("Intellicode.Instrument.Profile.list");
			icChannel.send("Intellicode.Instrument.use(Perception)");
			icChannel.send("Intellicode.Instrument.Profile.load(Perception_96.xtprof)");
			icChannel.send("Intellicode.Instrument.Profile.scan");
			icChannel.send("Intellicode.Instrument.Profile.Exporter.getResults(Text Exporter)");
			List<String> response = icChannel.getResponse();
			for (String s : response) {
				System.out.println(s);
			}
			//System.out.println(icChannel.getResponse().toString());

			System.out.println("Exiting.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
