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

package com.actelion.research.spiritcore.business.location;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SortNatural;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.actelion.research.spiritcore.business.IEntity;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Container;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.business.location.LocationType.LocationCategory;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.util.CompareUtils;

/**
 * 
 *
 */
@Entity
@BatchSize(size=64)
@Audited
@Table(name="biolocation", uniqueConstraints= {@UniqueConstraint(columnNames= {"name", "parent_id"})})
@SequenceGenerator(name="biolocation_sequence", sequenceName="biolocation_sequence", allocationSize=1)
public class Location implements IEntity, Serializable, Comparable<Location>, Cloneable {


	public static final String SEPARATOR = "/";
	public static final String PREFIX = "LOC"; 
		
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="biolocation_sequence")
	private int id = 0;
		
	private String name = "";
	
	@Column(name="description", length=256)
	private String description = "";	
	
	@ManyToOne(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY, optional=true)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@OnDelete(action=OnDeleteAction.CASCADE)
	@JoinColumn(name="parent_id")
	private Location parent = null;
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="parent")	
	@SortNatural
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@BatchSize(size=4)
	private Set<Location> children = new TreeSet<>();

	@OneToMany(fetch=FetchType.LAZY, mappedBy="location")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)	
	@BatchSize(size=1)
	private Set<Biosample> biosamples = new LinkedHashSet<>();
		
	@Column(name="locationType", nullable=false)
	@Enumerated(EnumType.STRING)
	private LocationType locationType;
	
	@Column(nullable=false)
	private Privacy privacy = Privacy.INHERITED;
	
	@Column(length=10)
	@Enumerated(EnumType.STRING)
	private LocationLabeling labeling;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="department_id")
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)	
	private EmployeeGroup employeeGroup;
	
	
	/** Used for Labeling==ALPHA or NUMERICAL */
	@Column(name="ncols", nullable=false)
	private int cols = 0;
	
	/** Used for Labeling==ALPHA or NUMERICAL */
	@Column(name="nrows", nullable=false)
	private int rows = 0;
		
	private String updUser;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date updDate;
	
	private String creUser;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date creDate;
	
	
	private transient boolean wasUpdated = false;
	
	public Location() {}	
	
	public Location(Location parent, String name) {
		this.parent = parent;
		this.name = name;
		this.labeling = LocationLabeling.NONE;
	}
	
	public Location(String name) {
		this(null, name);
	}
	
	public Location(LocationType type) {
		setLocationType(type);
	}

	@Override
	public int getId() {
		return id;
	}
	
	public String getLocationId() {
		return id>=0?PREFIX + id:"";
	}
	
	public String getName() {
		return name==null?"":name;
	}
	public String getHierarchyFull() {
		StringBuilder res = new StringBuilder();
		Location loc = this;
		int depth = 0;
		while(loc!=null) {			
			res.insert(0, loc.getName() + (depth>0? Location.SEPARATOR:""));
			loc = loc.getParent();
			if(++depth>=10) break;
		}
		return res.toString();
	}
	
	public String getHierarchyMedium() {
		StringBuilder res = new StringBuilder();
		Location loc = this;
		int depth = 0;
		while(true) {			
			String name = loc.getName();
			if(name.indexOf("(")>4 && name.indexOf("(")<name.lastIndexOf(")")) name = (name.substring(0, name.indexOf("(")) + name.substring(name.lastIndexOf(")")+1)).trim();
			if(name.length()>10) name = name.substring(0, 5) + "..." + name.substring(name.length()-3);
			res.insert(0, name + (depth>0? Location.SEPARATOR:""));
			
			if(loc.getLocationType().getCategory()==LocationCategory.ADMIN) break;

			loc = loc.getParent();
			if(++depth>=2 || loc==null) break;
		}
		return res.toString();
	}
	
	/**
	 * Gets the hierarchy of parents: [Top , ..., Parent , this ] with a max of 10 locations
	 * @return
	 */
	public List<Location> getHierarchy() {
		LinkedList<Location> res = new LinkedList<>();
		Location l = this;
		int depth = 0;
		while(l!=null && (++depth<10)) {
			res.addFirst(l);
			l = l.getParent();
		}
		return res;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	public void setName(String name) {
		assert name==null || (name.indexOf('/')<0 && name.indexOf(':')<0);
		name = name==null? null: name.replace('/', '_').replace(':', '_');
		this.name = name;
	}


	public int getSize() {		
		return getCols()<=0 || getRows()<=0? -1: getCols()*getRows();
	}

	public int getCols() {
		return cols;
	}

	public void setCols(int cols) {
		this.cols = cols;
	}

	public void setLabeling(LocationLabeling labeling) {
		this.labeling = labeling;
	}
	public LocationLabeling getLabeling() {
		return labeling==null?LocationLabeling.NONE: labeling;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==this) return true;
		if(!(obj instanceof Location)) return false;		
		return getId() == ((Location)obj).getId();
	}
	
	@Override
	public int compareTo(Location o2) {
		if(o2==null) return -1;
		if(equals(o2)) return 0;
		
		if((getParent()==null && o2.getParent()==null) || (getParent()!=null && o2.getParent()!=null && getParent().getId()>0 && getParent().getId()==o2.getParent().getId())) {
			return CompareUtils.compare(getName(), o2.getName());			
		}
		return CompareUtils.compare(this.getHierarchyFull(), o2.getHierarchyFull());
	}
	
	public void setChildren(Set<Location> children) {
		this.children = children;
	}
	public Set<Location> getChildren() {
		return children;
	}
	
	public Location getChildByName(String name) {
		for (Location l : getChildren()) {
			if(l.getName().equals(name)) return l;
		}
		return null;
	}
	
	/**
	 * Return children recursively including this
	 * @param maxDepth
	 * @return an non empty set
	 */
	public Set<Location> getChildrenRec(int maxDepth) {
		Set<Location> childrenRec = new HashSet<>();
		childrenRec.add(this);
		if(maxDepth>0) {
			for (Location l : getChildren()) {
				childrenRec.addAll(l.getChildrenRec(maxDepth-1));
			}
		}
		return childrenRec;
	}
	
	/**
	 * Sets the parent (and make sure the dual relationship is correct)
	 * @param parent
	 */
	public void setParent(Location parent) {
		if(this.parent==parent) return;
		
		//update the double relationship
		if(this.parent!=null) {
			this.parent.getChildren().remove(this);
		}
		
		this.parent = parent;
		
		//update the double relationship
		if(parent!=null) {
			parent.getChildren().add(this);
		}
		
	}
	public Location getParent() {
		return parent;
	}

	public int parsePosition(String posString) throws Exception {
		if(getLabeling()==null) return -1;
		return getLabeling().getPos(this, posString);
	}
	
	
	@Override
	public String toString() {
		return getHierarchyFull();
	}
	public void setUpdUser(String updUser) {
		this.updUser = updUser;
		if(creUser==null) this.creUser = updUser;
	}
	public String getUpdUser() {
		return updUser;
	}
	public void setUpdDate(Date updDate) {
		this.updDate = updDate;
		if(creDate==null) this.creDate = updDate;
	}
	public Date getUpdDate() {
		return updDate;
	}
	public void setRows(int rows) {
		this.rows = rows;
	}
	public int getRows() {
		return rows;
	}

	
	/**
	 * @param privacy the privacy to set
	 */
	public void setPrivacy(Privacy privacy) {
		this.privacy = privacy;
	}
	/**
	 * @return the privacy
	 */
	public Privacy getPrivacy() {
		return privacy;
	}
	
	public Privacy getInheritedPrivacy() {
		Location l = this;
		int depth=0;
		while(l!=null && (++depth)<10) {
			if(l.getPrivacy()==null || l.getPrivacy()==Privacy.INHERITED) {
				l = l.getParent();
			} else {
				return l.getPrivacy();
			}
		}
		if(depth==10) {
			System.err.println("Cycle in "+this+":"+getHierarchy());
		}
		return Privacy.PUBLIC;
	}
	
	public EmployeeGroup getInheritedEmployeeGroup() {
		Location l = this;
		int depth=0;
		while(l!=null && (++depth)<10) {
			if(l.getPrivacy()==null || l.getPrivacy()==Privacy.INHERITED) {
				l = l.getParent();
			} else {
				return l.getEmployeeGroup();
			}
		}
		if(depth==10) {
			System.err.println("Cycle in "+this+":"+getHierarchy());
		}
		return null;
	}
	
	/**
	 * @param department the department to set
	 */
	public void setEmployeeGroup(EmployeeGroup employeeGroup) {
		this.employeeGroup = employeeGroup;
	}
	/**
	 * @return the department
	 */
	public EmployeeGroup getEmployeeGroup() {
		return employeeGroup;
	}

	/**
	 * @param category the category to set
	 */
	public void setLocationType(LocationType type) {
		this.locationType = type;
		if(type!=null && cols<=0 && rows<=0) {
			setLabeling(type.getPositionType());
			setCols(type.getDefaultCols());
			setRows(type.getDefaultRows());
		}
		
	}
	/**
	 * @return the category
	 */
	public LocationType getLocationType() {
		return locationType;
	}
	
	public List<Container> getContainers() {
		List<Container> containers = new ArrayList<>(Biosample.getContainers(biosamples, true));
		return containers;
	}
	
	
	/**
	 * Gets the biosamples from this location (not sorted)
	 * The location may be temporary (not persistent), if biosample.location == null
	 * @return
	 */
	public Set<Biosample> getBiosamples() {
		return biosamples;
	}
	
	public void setBiosamples(Set<Biosample> biosamples) {
		this.biosamples = biosamples;		
	}
	
	
	public Map<Integer, Container> getContainersMap() {
		
		Map<Integer, Container> res = new HashMap<>();
		if(getLabeling()==LocationLabeling.NONE) {
			List<Biosample> biosamples = new ArrayList<>(getBiosamples());
			Collections.sort(biosamples, new Comparator<Biosample>() {
				@Override
				public int compare(Biosample o1, Biosample o2) {
					return o1.getPos()-o2.getPos();
				}
			});
			List<Container> containers = Biosample.getContainers(biosamples, true);						
			int index = 0;
			for (Container c : containers) {
				res.put(index++, c);				
			}			
		} else if(getBiosamples()!=null) {
			for (Biosample b : getBiosamples()) {
				if(b.getPos()>=0) {
					res.put(b.getPos(), b.getContainer());
				} else {
					try {
						res.put(parsePosition(b.getScannedPosition()), b.getContainer());
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return res;
	}
	
	@Override
	public Location clone()  {
		try {
			Location res = (Location) super.clone();
			return res;
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	public String getCreUser() {
		return creUser;
	}

	public void setCreUser(String creUser) {
		this.creUser = creUser;
	}

	public Date getCreDate() {
		return creDate;
	}

	public void setCreDate(Date creDate) {
		this.creDate = creDate;
	}
	
	public String formatPosition(int pos) {
		return getLabeling().formatPosition(this, pos);
	}

	public String formatPosition(int row, int col) {
		return getLabeling().formatPosition(this, row, col);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	
	
	public int getOccupancy() {
		return getBiosamples().size();
	}
	

	/**
	 * To propagate the persist/merge location when a biosample is saved, one must set this flag, otherwise there will be no update 
	 * @param wasUpdated
	 */
	public void setWasUpdated(boolean wasUpdated) {
		this.wasUpdated = wasUpdated;
	}

	/**
	 * Should we propagate the persist/merge location when a biosample is saved
	 * @return
	 */
	public boolean wasUpdated() {
		return wasUpdated;
	}	

	public boolean isEmpty() {
		return getBiosamples().isEmpty();
	}



	/**
	 * Get the parent location if not null (no recursive calls)
	 * @param locations
	 * @return
	 */
	public static Set<Location> getParents(Collection<Location> locations) {
		Set<Location> res = new HashSet<>();
		if(locations==null) return res;
		for (Location loc : locations) {
			if(loc.getParent()!=null) {
				res.add(loc.getParent());			
			}
		}
		return res;
	}
		
	public static Set<Biosample> getBiosamples(Collection<Location> locations) {
		Set<Biosample> res = new HashSet<>();
		if(locations==null) return res;
		for (Location loc : locations) {
			res.addAll(loc.getBiosamples());
		}
		return res;
	}

	public static Location get(Collection<Location> locations, String name) {
		for (Location loc : locations) {
			if(loc.getName().equals(name)) return loc;
		}
		return null;
	}


	public int getCol(int pos) {
		return getLabeling().getCol(this, pos);
	}

	public int getRow(int pos) {
		return getLabeling().getRow(this, pos);
	}


	public Location duplicate()  {
		Location location = new Location();

		location.locationType = locationType;
		location.name = name;
		
		location.privacy = privacy;
		location.employeeGroup = employeeGroup;

		location.labeling = labeling;
		location.rows = rows;
		location.cols = cols;
		return location;
		
	}	
	
	
	public static List<Location> duplicate(List<Location> locations) {
		
		//Sort the location to have them in hierarchy already (parents come before their children)
		locations = new ArrayList<>(locations);
		Collections.sort(locations);
		
		//Duplicate each location
		List<Location> res = new ArrayList<>();
		IdentityHashMap<Location, Location> old2new = new IdentityHashMap<>();
		for(Location l: locations) {
			Location clone = l.duplicate();
			boolean changeName;
			if(l.getParent()!=null) {
				 if(old2new.get(l.getParent())!=null) {
					 changeName = false;
					 clone.setParent(old2new.get(l.getParent()));					 
				 } else {
					 changeName = true;
					 clone.setParent(l.getParent());
				 }
			} else {
				changeName = true;
			}
			if(changeName) {
				//Find a new possible Name
				String name = l.getName();
				if(name.indexOf(" (Copy ")>0) name = name.substring(0, name.indexOf(" (Copy "));
				int n = 1;
				while(DAOLocation.getLocation(l.getParent(), name + " (Copy "+n+")")!=null) n++;			
				clone.setName(name + " (Copy "+n+")");
			}
			
			res.add(clone);
			old2new.put(l, clone);
		}
		return res;
	}
}


