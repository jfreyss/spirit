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

package com.actelion.research.spiritlib;

import java.io.Serializable;
import java.util.Set;

/**
 * ExchangePojo is used to represent a Spirit exchange file.
 *
 * Constraints:
 * - If results are linked to tests, those tests should be present
 * - If results are linked to biosamples, those biosamples should be present
 * - If results are linked to biosamples, those biosamples should be present
 * - If biosamples are linked to locations, those locations should be present
 * - If biosamples are linked to studies, those studies should be present
 * - If biosamples are linked to biosamples, those biosamples should be present
 * - If biosamples are present, their biotypes should be present
 * - If locations are present, their parent shoud be present
 * - If result are present, their tests should be present
 * - If studies contains samplings, their biotypes should be present
 *
 *
 *
 * @author freyssj
 *
 */
public class ExchangePojo implements Serializable {
	private String name;
	private String version;
	private Set<BiotypePojo> biotypes;
	private Set<TestPojo> tests;

	private Set<StudyPojo> studies;
	private Set<BiosamplePojo> biosamples;
	private Set<LocationPojo> locations;
	private Set<ResultPojo> results;

	public ExchangePojo() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Set<BiotypePojo> getBiotypes() {
		return biotypes;
	}

	public void setBiotypes(Set<BiotypePojo> biotypes) {
		this.biotypes = biotypes;
	}

	public Set<TestPojo> getTests() {
		return tests;
	}

	public void setTests(Set<TestPojo> tests) {
		this.tests = tests;
	}

	public Set<StudyPojo> getStudies() {
		return studies;
	}

	public void setStudies(Set<StudyPojo> studies) {
		this.studies = studies;
	}

	public Set<BiosamplePojo> getBiosamples() {
		return biosamples;
	}

	public void setBiosamples(Set<BiosamplePojo> biosamples) {
		this.biosamples = biosamples;
	}

	public Set<LocationPojo> getLocations() {
		return locations;
	}

	public void setLocations(Set<LocationPojo> locations) {
		this.locations = locations;
	}

	public Set<ResultPojo> getResults() {
		return results;
	}

	public void setResults(Set<ResultPojo> results) {
		this.results = results;
	}

	@Override
	public String toString() {
		return "biotypes: "+biotypes + ", biosamples: "+biosamples + ", tests: "+tests + ", results: "+results;
	}

}
