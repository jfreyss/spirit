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

package com.actelion.research.spiritlib.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain POJO representing a Biotype, used to import/export data
 *  
 * @author freyssj
 */
public class BiotypePojo implements Serializable {

	private int id = 0;
	private String name;
	private String sampleNameLabel;
	private String category;
	private String prefix;
	private String amountUnit;
	private String containerType;
	private String parentBiotype;
	private String description;
	private List<BiotypeMetadataPojo> metadata = new ArrayList<>();
	
	private boolean isAbstract;
	private boolean isHidden;
	private boolean isHideContainer;
	private boolean isHideSampleId;
	private boolean isNameAutocomplete;
	private boolean isNameRequired;

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSampleNameLabel() {
		return sampleNameLabel;
	}
	public void setSampleNameLabel(String sampleNameLabel) {
		this.sampleNameLabel = sampleNameLabel;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public String getAmountUnit() {
		return amountUnit;
	}
	public void setAmountUnit(String amountUnit) {
		this.amountUnit = amountUnit;
	}
	public String getContainerType() {
		return containerType;
	}
	public void setContainerType(String containerType) {
		this.containerType = containerType;
	}
	public String getParentBiotype() {
		return parentBiotype;
	}
	public void setParentBiotype(String parentBiotype) {
		this.parentBiotype = parentBiotype;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<BiotypeMetadataPojo> getMetadata() {
		return metadata;
	}
	public void setMetadata(List<BiotypeMetadataPojo> metadata) {
		this.metadata = metadata;
	}
	public boolean isAbstract() {
		return isAbstract;
	}
	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}
	public boolean isHidden() {
		return isHidden;
	}
	public void setHidden(boolean isHidden) {
		this.isHidden = isHidden;
	}
	public boolean isHideContainer() {
		return isHideContainer;
	}
	public void setHideContainer(boolean isHideContainer) {
		this.isHideContainer = isHideContainer;
	}
	public boolean isHideSampleId() {
		return isHideSampleId;
	}
	public void setHideSampleId(boolean isHideSampleId) {
		this.isHideSampleId = isHideSampleId;
	}
	public boolean isNameAutocomplete() {
		return isNameAutocomplete;
	}
	public void setNameAutocomplete(boolean isNameAutocomplete) {
		this.isNameAutocomplete = isNameAutocomplete;
	}
	public boolean isNameRequired() {
		return isNameRequired;
	}
	public void setNameRequired(boolean isNameRequired) {
		this.isNameRequired = isNameRequired;
	}
	
	@Override
	public String toString() {
		return "[BiotypePojo: " + name + "]";
	}
}
