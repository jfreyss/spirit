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

package com.actelion.research.spiritapp.ui.util.scanner;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.location.LocationBrowser;
import com.actelion.research.spiritapp.ui.location.LocationBrowser.LocationBrowserFilter;
import com.actelion.research.spiritapp.ui.util.component.JHeaderLabel;
import com.actelion.research.spiritapp.ui.util.component.ScannerConfigurationComboBox;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.BiosampleQuery;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.biosample.ContainerType;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.util.Config;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JCustomTextField.CustomFieldType;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.scanner.FluidxScanner;
import com.actelion.research.util.ui.scanner.Plate;
import com.actelion.research.util.ui.scanner.RackPos;
import com.actelion.research.util.ui.scanner.ScannerConfiguration;
import com.actelion.research.util.ui.scanner.ScannerFactory;

/**
 * Util class to scan racks.
 *
 *
 *
 *
 * @author freyssj
 *
 */
public class SpiritScannerHelper {

	public enum Verification {
		NONE,

		/** Checks that containers are empty */
		EMPTY_CONTAINERS,

		/** Checks that containers are existing */
		EXISTING_CONTAINER;
	}

	private static ScannerConfiguration defaultScannerConfiguration = ScannerConfiguration.SCANNER_CONFIGURATION_MATRIX_1_0PP;
	private Verification verification = Verification.NONE;
	private boolean askRackIdLocation;

	/**
	 * Return the ScannerConfiguration, which was selected by the user
	 */
	private ScannerConfiguration scannerConfiguration;

	/**
	 * Creates a new SpiritScannerHelper, ready to scan
	 */
	public SpiritScannerHelper() {}

	public static void setDefaultScannerConfiguration(ScannerConfiguration defaultScannerConfiguration) {
		Config.getInstance(".spirit").setProperty("scannerConfig", defaultScannerConfiguration.name());
		SpiritScannerHelper.defaultScannerConfiguration = defaultScannerConfiguration;
	}

	public static ScannerConfiguration getDefaultScannerConfiguration() {
		String config = Config.getInstance(".spirit").getProperty("scannerConfig", defaultScannerConfiguration.name());
		try {
			return ScannerConfiguration.valueOf(config);
		} catch(Exception e) {
			return defaultScannerConfiguration;
		}

	}

	public void setVerification(Verification verification) {
		this.verification = verification;
	}

	public Verification getVerification() {
		return verification;
	}

	public void setScannerConfiguration(ScannerConfiguration scannerConfiguration) {
		this.scannerConfiguration = scannerConfiguration;
	}
	public ScannerConfiguration getScannerConfiguration() {
		return scannerConfiguration;
	}
	public void setAskRackIdLocation(boolean askRackIdLocation) {
		this.askRackIdLocation = askRackIdLocation;
	}
	public boolean isAskRackIdLocation() {
		return askRackIdLocation;
	}



	/**
	 * Opens a dialog to ask which type of tubes
	 * @return null if no configuration is selected, otherwise a new location containing the scanned tubes
	 * @throws Exception
	 */
	private Pair<ScannerConfiguration, Location> askForScannerConfAndLocation(boolean askLocation, Location defaultRackLocation) throws Exception {
		//find compatible scanner configuration for biosamples

		//Ask for type of rack to scan
		ScannerConfigurationComboBox scannerConfComboBox = new ScannerConfigurationComboBox();
		JCustomTextField rackNameTextField = new JCustomTextField(CustomFieldType.ALPHANUMERIC, 25);
		LocationBrowser locationBrowser = new LocationBrowser(LocationBrowserFilter.CONTAINER);
		if(askLocation && defaultRackLocation!=null) rackNameTextField.setText(defaultRackLocation.getName());
		if(askLocation && defaultRackLocation!=null) locationBrowser.setBioLocation(defaultRackLocation.getParent());

		JScrollPane locationSp = new JScrollPane(locationBrowser);

		JPanel contentPanel = UIUtils.createBox(
				UIUtils.createTable(3,
						new JLabel("Rack Type: "), scannerConfComboBox,
						(askLocation? new JLabel("RackId: "): null), (askLocation? rackNameTextField: null),
						(askLocation? new JLabel("Location: "): null), (askLocation? locationSp: null)),
				new JHeaderLabel("Scan Rack"));

		//TODO add status

		locationSp.setPreferredSize(new Dimension(280, 70));


		int res = JOptionPane.showOptionDialog(UIUtils.getMainFrame(), contentPanel, "Scan", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		ScannerConfiguration selectedConf = scannerConfComboBox.getSelection();
		if(res!=JOptionPane.YES_OPTION || selectedConf==null) return null;


		//Check scanner installation
		if(selectedConf!=ScannerConfiguration.SCANNER_CONFIGURATION_OPENFILE) {
			//Check Scanner is installed
			if(!FluidxScanner.isInstalled()) throw new Exception("Could not find the Fluidx Scanner");
		}

		//Memo Contiguration
		setDefaultScannerConfiguration(selectedConf);

		if(rackNameTextField.getText().trim().length()==0) {
			defaultRackLocation = null;
		} else {
			defaultRackLocation = new Location(LocationType.RACK);
			defaultRackLocation.setRows(selectedConf.getRows());
			defaultRackLocation.setCols(selectedConf.getCols());
			defaultRackLocation.setName(rackNameTextField.getText().trim());
			defaultRackLocation.setLabeling(LocationLabeling.ALPHA);
		}

		//Scan
		return new Pair<ScannerConfiguration, Location>(selectedConf, defaultRackLocation);
	}

	/**
	 * Scan based on a containerType,
	 * ie find or request the appropriate scannerconfiguration
	 * @param containerType
	 * @return
	 * @throws Exception
	 */
	public Location scan(ContainerType containerType) throws Exception {
		if(containerType==null) return scan();
		Set<ScannerConfiguration> res = new HashSet<>();
		for(ScannerConfiguration config: ScannerConfiguration.values()) {
			if(containerType==ContainerType.get(config.getDefaultTubeType())) res.add(config);
		}
		if(res.size()>1) {
			ScannerConfiguration[] options = res.toArray(new ScannerConfiguration[0]);
			int opt = JOptionPane.showOptionDialog(null, "Please select a scanner Configuration", "Scan", 0, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			if(opt>=0) scannerConfiguration = options[opt];
		} else  if(res.size()>0) {
			scannerConfiguration = res.iterator().next();
		} else {
			throw new Exception("There is no scanner definition for "+containerType);
		}

		return scan();
	}

	/**
	 * Scan a XTR96 Plate with Matrix Containers and load those Containers (or associate an unknown Containers)
	 * @return
	 * @throws Exception
	 */
	public Location scan() throws Exception {
		Location rackLocation = null;

		if(scannerConfiguration==null) {
			Pair<ScannerConfiguration, Location> res = askForScannerConfAndLocation(askRackIdLocation, rackLocation);
			if(res==null) return null;
			scannerConfiguration = res.getFirst();
			if(askRackIdLocation) rackLocation = res.getSecond();
		}

		String tubeType = scannerConfiguration.getDefaultTubeType();
		ContainerType containerType = tubeType==null? ContainerType.UNKNOWN: ContainerType.get(tubeType);

		List<Biosample> biosamples;
		int rows, cols;
		if(scannerConfiguration==ScannerConfiguration.SCANNER_CONFIGURATION_OPENFILE) {
			//SCAN FROM PASTE

			JTextArea ta = new JTextArea(12, 30);
			boolean done = false;
			do {
				ScannerConfigurationComboBox scannerConfComboBox = new ScannerConfigurationComboBox();
				int res = JOptionPane.showOptionDialog(null,
						UIUtils.createBox(
								new JScrollPane(ta),
								UIUtils.createVerticalBox(
										new JLabel("Enter the list of position<TAB>containerId: "),
										scannerConfComboBox)),
						"Copy Paste the tubes", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[] {"Cancel", "OK"}, "OK");
				if(res!=1) return null;
				biosamples = new ArrayList<>();
				rows = scannerConfComboBox.getSelection().getRows();
				cols = scannerConfComboBox.getSelection().getCols();
				rackLocation = new Location(LocationType.RACK);
				rackLocation.setRows(rows);
				rackLocation.setCols(cols);
				rackLocation.setLabeling(LocationLabeling.ALPHA);

				try {
					List<String> containerIds = new ArrayList<>();
					Map<String, String> pos2id = new HashMap<>();
					for(String line: ta.getText().split("\n")) {
						String[] split = line.split("\t");
						if(split.length==0) {
							continue;
						} else if(split.length<2) {
							throw new Exception("The line "+line+" is not well formatted");
						}
						String pos = split[0].trim();
						String id = split[1].trim();
						containerIds.add(id);
						if(pos2id.get(pos)!=null) throw new Exception("The position "+pos+" is duplicated");
						pos2id.put(pos, id);
					}
					Map<String, List<Biosample>> map = Biosample.mapContainerId(DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForContainerIds(containerIds), null));
					for (String pos : pos2id.keySet()) {
						String tubeId = pos2id.get(pos);

						//Parse
						LocationLabeling.ALPHA.getPos(rackLocation, pos);

						List<Biosample> l = map.get(tubeId);
						if(l==null) {
							Biosample b = new Biosample();
							b.setContainerType(containerType);
							b.setContainerId(tubeId);
							b.setScannedPosition(pos);
							biosamples.add(b);

						} else {
							for (Biosample b : l) {
								b.setScannedPosition(pos);
								b.setLocation(null);
								biosamples.add(b);
							}
						}
					}
					done = true;
				} catch(Exception e) {
					JExceptionDialog.showError(e);
				}
			} while(!done);


		} else {

			Plate plate;
			FluidxScanner scanner = ScannerFactory.getScanner(SpiritFrame.getApplicationName());
			plate = scanner.scanPlate(scannerConfiguration);
			if (plate.getTubes().size() == 0) {
				return null;
			}
			//Verify NOREAD
			for(int i=0; i<plate.getTubes().size(); i++) {
				if(RackPos.NOREAD.equals(plate.getTubes().get(i).getTubeId())) {
					throw new Exception("The tube at "+plate.getTubes().get(i).getPosition()+" cannot be read");
				}
			}

			//Load existing containers
			Map<String, List<Biosample>> map = Biosample.mapContainerId(DAOBiosample.queryBiosamples(BiosampleQuery.createQueryForContainerIds(RackPos.getTubeIds(plate.getTubes())), null));

			//Gets containers

			biosamples = new ArrayList<>();
			for (RackPos t : plate.getTubes()) {
				String tubeId = t.getTubeId();
				String pos = t.getPosition();

				List<Biosample> l = map.get(tubeId);
				if(l==null) {
					//The tube is not known: create a
					Biosample b = new Biosample();
					b.setContainerType(containerType);
					b.setContainerId(tubeId);
					b.setScannedPosition(pos);
					biosamples.add(b);

				} else {
					for (Biosample b : l) {
						b.setScannedPosition(pos);
						biosamples.add(b);
					}
				}
			}

			rows = scannerConfiguration.getRows();
			cols = scannerConfiguration.getCols();
		}

		//Verify the containers
		if(verification==Verification.EMPTY_CONTAINERS) {
			for(Container container: Biosample.getContainers(biosamples)) {
				if(!container.isEmpty()) {
					throw new Exception("The container " + container.getContainerId() + " at " + container.getScannedPosition()
					+ " should be empty but it contains:\n "
					+ MiscUtils.flatten(Biosample.getSampleIds(container.getBiosamples())) + "\n"
					+ container.getPrintLabel());
				}
			}
		} else if(verification==Verification.EXISTING_CONTAINER) {
			for(Container container: Biosample.getContainers(biosamples)) {
				if(container.isEmpty()) throw new Exception("The container "+container+" does not exist");
				if(container.getContainerType()!=containerType) throw new Exception("The container "+container+" at " + container.getScannedPosition() + " is registered as a " +container.getContainerType());
			}
		}

		if(rackLocation!=null) {
			//Load matching rack?
			Location rack = DAOLocation.getLocation(null, rackLocation.getName());
			if(rack!=null) {
				if(!rack.isEmpty() || rack.getParent()!=null) throw new Exception(rackLocation.getName()+" already exists and cannot be reused");
				if(rack.getLocationType()!=LocationType.RACK) throw new Exception(rackLocation.getName()+" is a "+rack.getLocationType()+" (expected: "+LocationType.RACK+")");
				rackLocation = rack;
			}
		}
		if(rackLocation!=null) {
			for (Biosample b : biosamples) {
				b.setLocPos(rackLocation, rackLocation.parsePosition(b.getScannedPosition()));
			}
		} else {
			rackLocation = new Location(LocationType.RACK);
			rackLocation.setRows(rows);
			rackLocation.setCols(cols);
			rackLocation.setLabeling(LocationLabeling.ALPHA);
			rackLocation.setBiosamples(new HashSet<>(biosamples));
		}

		return rackLocation;
	}


}
