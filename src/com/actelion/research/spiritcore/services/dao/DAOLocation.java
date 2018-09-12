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

package com.actelion.research.spiritcore.services.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.LocPos;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationLabeling;
import com.actelion.research.spiritcore.business.location.LocationQuery;
import com.actelion.research.spiritcore.business.location.LocationType;
import com.actelion.research.spiritcore.business.location.LocationType.LocationCategory;
import com.actelion.research.spiritcore.business.location.Privacy;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.util.QueryTokenizer;

/**
 * DAO functions linked to locations
 *
 * @author Joel Freyss
 */
public class DAOLocation {

	private static Logger logger = LoggerFactory.getLogger(DAOLocation.class);

	public static void persistLocations(Collection<Location> locations, SpiritUser user) throws Exception {
		if(locations==null || locations.size()==0) return;
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			persistLocations(session, locations, user);
			txn.commit();
			txn = null;
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}
	}

	public static void persistLocations(EntityManager session, Collection<Location> locations, SpiritUser user) throws Exception {

		if(locations==null || locations.size()==0) return;
		logger.info("persist "+locations.size()+" locations");


		// Test that nobody else modified the location
		Map<Integer, Integer> id2rows = new HashMap<>();
		Map<Integer, Integer> id2cols = new HashMap<>();
		Map<Integer, Location> id2location = JPAUtil.mapIds(locations);
		if(id2location.size()>0) {
			List<Object[]> lastUpdates = session.createQuery("select l.id, l.rows, l.cols, l.updDate, l.updUser from Location l where " + QueryTokenizer.expandForIn("l.id", id2location.keySet())).getResultList();
			for (Object[] lastUpdate : lastUpdates) {
				Location l = id2location.get(lastUpdate[0]);
				Integer rows = (Integer) lastUpdate[1];
				Integer cols = (Integer) lastUpdate[2];
				Date lastDate = (Date) lastUpdate[3];
				String lastUser = (String) lastUpdate[4];

				if (l == null) {
					throw new Exception("The location " + l + " has just been deleted. Please refresh your data");
				}
				if (l.getUpdDate() != null && lastDate != null) {
					int diffSeconds = (int) ((lastDate.getTime() - l.getUpdDate().getTime()) / 1000L);
					if (diffSeconds > 0)
						throw new Exception("The location " + l + " has just been updated by " + lastUser + " [" + diffSeconds + "seconds ago].\nYou cannot overwrite those changes unless you reopen the newest version.");
				}
				id2rows.put(l.getId(), rows);
				id2cols.put(l.getId(), cols);
			}
		}


		IdentityHashMap<Location, Location> map = new IdentityHashMap<>();

		Date now = JPAUtil.getCurrentDateFromDatabase();

		List<Location> sorted = new ArrayList<>(locations);
		Collections.sort(sorted);



		for (Location location : sorted) {

			if(location.getId()<=0 && (location.getName()==null || location.getName().length()==0) && location.getParent()==null) continue;

			LoggerFactory.getLogger(DAOLocation.class).debug("persist " + location.getHierarchyFull() + "(" + location.getId() + ") - parent " + (location.getParent()==null?"NA": location.getParent().getHierarchyFull() + "(" + location.getParent().getId() + ")"));

			//Name is required
			location.setName(location.getName()==null? "": location.getName().trim());
			if(location.getName().length()==0) throw new Exception("The location's name is required");

			//Name is unique for each parent
			List<Location> sieblings;
			if(location.getParent()==null) {
				sieblings = session.createQuery("from Location l where l.parent is null").getResultList();
			} else {
				sieblings = new ArrayList<>(location.getParent().getChildren());
			}
			for (Location siebling : sieblings) {
				if(siebling!=location && siebling.getId()!=location.getId() && location.getName().equals(siebling.getName())) {
					throw new Exception("The location's name is not unique: " + location.getHierarchyFull());
				}
			}

			location.setDescription(location.getDescription()==null? "": location.getDescription().trim());

			if(location.getLocationType()==null) throw new Exception("You must specify the type for "+location);

			//Department is required for private or protected locations
			if(location.getPrivacy()==null) {
				throw new Exception("The privacy is required for " + location);
			} else if((location.getPrivacy()==Privacy.PRIVATE || location.getPrivacy()==Privacy.PROTECTED)) {
				if(location.getEmployeeGroup()==null) throw new Exception("You must specify the department for " + location + "(" + location.getPrivacy() + ")");
			}

			//Check positions
			if(location.getLabeling()!=LocationLabeling.NONE) {
				if(location.getRows()<=0)  throw new Exception("You must specify the rows for " + location);
				if(location.getCols()<=0)  throw new Exception("You must specify the cols for " + location);
			}

			//Check cycles
			int depth = 0;
			Location c = location;
			Set<Location> seen = new HashSet<>();
			while(c!=null && (++depth)<30) {
				if(seen.contains(c)) throw new Exception("You cannot create cycles");
				seen.add(c);
				c = c.getParent();
			}

			if(location.getParent()!=null) {
				for (Location l : location.getParent().getChildren()) {
					if(!l.equals(location) && l.getName().equals(location.getName())) {
						throw new Exception("The location " + location.getName() + " must be unique under " + location.getParent());
					}
				}
			} else {
				for (Location l : getLocationRoots()) {
					if(!l.equals(location) && l.getName().equals(location.getName())) {
						throw new Exception("The location " + location.getName() + " must be unique");
					}
				}
			}

			if (location.getParent() != null && location.getParent().getId() <= 0 && !locations.contains(location.getParent())) {
				throw new Exception("The parent of " + location + " id:" + location.getId() + " ->" + location.getParent() + " id:"+(location.getParent().getId())+ " is invalid, 2nd parent:"+location.getParent().getParent());
			}

			//Update the parent if needed
			if(location.getParent()!=null && location.getParent().getId()<=0 && !session.contains(location.getParent())) {
				Location parent = map.get(location.getParent());
				if(parent==null) throw new Exception("Could not persist "+location+" without saving its parent first: "+location.getParent());
				location.setParent(parent);
			}

			if(location.getPrivacy()==null) {
				location.setPrivacy(Privacy.INHERITED);
			}

			//Persist/Merge the location
			int oldId = location.getId();
			location.setUpdUser(user.getUsername());
			location.setUpdDate(now);

			LoggerFactory.getLogger(DAOLocation.class).debug("persist "+location.getName() + "("+location.getId()+") - parent "+(location.getParent()==null?"NA": location.getParent() + "("+location.getParent().getId()+")"));

			if(location.getId()<=0) {
				location.setCreDate(location.getUpdDate());
				location.setCreUser(location.getUpdUser());
				session.persist(location);
				map.put(location, location);
			} else if(!session.contains(location)) {
				Location mergedLocation = session.merge(location);
				map.put(location, mergedLocation);
			} else {
				map.put(location, location);
			}

			//Update all samples in the updated location, and their children
			for (Biosample b : map.get(location).getBiosamples()) {
				b.setUpdUser(user.getUsername());
				b.setUpdDate(now);
			}


			//Update the biosample position?
			if(oldId>0) {
				int oldCols = id2cols.get(oldId)==null?-1: id2cols.get(oldId);
				int oldRows = id2rows.get(oldId)==null?-1: id2rows.get(oldId);

				Location oldLocation = session.find(Location.class, oldId);
				if(oldLocation!=null && (location.getCols()!=oldCols || location.getRows()!=oldRows)) {
					if(location.getCols()<=0 || location.getRows()<=0) {  //The new location does not have a size
						//Reset new positions to -1
						for (Biosample b : new ArrayList<>(oldLocation.getBiosamples())) {
							if(b.getPos()!=-1) {
								b.setPos(-1);
								b.setUpdDate(location.getUpdDate());
								b.setUpdUser(location.getUpdUser());
							}
						}
					} else if(oldCols>0 && oldRows>0) { //The new location has a size, and the old also
						//Validate new size
						for (Biosample b : new ArrayList<Biosample>(oldLocation.getBiosamples())) {
							int oldCol = b.getPos()%oldCols;
							int oldRow = b.getPos()/oldCols;
							if(oldCol>=location.getCols()) throw new Exception("The cols of "+location+" cannot be less than "+(oldCol+1));
							if(oldRow>=location.getRows()) throw new Exception("The rows of "+location+" cannot be less than "+(oldRow+1));

							int newPos = oldRow*location.getCols()+oldCol;
							if(b.getPos()!=newPos) {
								b.setPos(newPos);
								b.setUpdDate(location.getUpdDate());
								b.setUpdUser(location.getUpdUser());
							}
						}
					} else if(location.getBiosamples().size()<=location.getSize()) { //The new location has a size, and the old didn't
						int count = 0;
						for (Biosample b : new ArrayList<Biosample>(oldLocation.getBiosamples())) {
							b.setPos(count++);
							b.setUpdDate(location.getUpdDate());
							b.setUpdUser(location.getUpdUser());
						}
					} else {
						throw new Exception("You cannot set the size of "+location+" to less than "+location.getSize());
					}
				}
			}

		}
		Cache.getInstance().remove("locationRoots");
		Cache.getInstance().remove("locations");
	}


	public static Location getLocation(int id) {
		EntityManager session = JPAUtil.getManager();
		List<Location> list = session.createQuery("from Location l where l.id = "+id).getResultList();
		return list.size()==1? list.get(0): null;
	}

	public static Location getLocation(Location parent, String name) {
		EntityManager session = JPAUtil.getManager();
		if(parent==null || parent.getId()<=0) {
			List<Location> list = session.createQuery("from Location l where l.name = ?1 and l.parent is null")
					.setParameter(1, name)
					.getResultList();
			return list.size()==1? list.get(0): null;
		} else {
			List<Location> list = session.createQuery("from Location l where l.name = ?1 and l.parent = ?2")
					.setParameter(1, name)
					.setParameter(2, parent)
					.getResultList();
			return list.size()==1? list.get(0): null;
		}
	}

	/**
	 * Returns locations without any parents
	 * @return
	 */
	public static List<Location> getLocationRoots() {
		List<Location> res = null;
		StringBuilder sb = new StringBuilder();
		for (LocationType t : LocationType.getPossibleRoots()) {
			sb.append((sb.length()>0?",":"") + "'" + t.name() + "'");
		}

		EntityManager session = JPAUtil.getManager();
		Query query = session.createQuery("from Location location where location.parent is null and location.locationType in (" + sb + ")");
		res = query.getResultList();
		Collections.sort(res);

		return res;
	}


	public static List<Location> getLocationRoots(SpiritUser user) {
		List<Location> res = new ArrayList<>();
		for(Location l: getLocationRoots()) {
			if(SpiritRights.canRead(l, user)) {
				res.add(l);
			}
		}
		return res;

	}


	/**
	 * Query the location table from the given criteria
	 * @param q
	 * @param user (can be null to return all, otherwise returns all location with read access)
	 * @return
	 * @throws Exception
	 */
	public static List<Location> queryLocation(LocationQuery q, SpiritUser user) throws Exception {

		EntityManager session = JPAUtil.getManager();
		String jpql = "from Location location where 1 = 1";

		List<Object> parameters = new ArrayList<>();

		if(q.getStudyId()!=null && q.getStudyId().length()>0) {
			jpql += " and exists(select b from Biosample b Where b.inheritedStudy.studyId = ? and b.location = location)";
			parameters.add(q.getStudyId());
		}

		if(q.getEmployeeGroup()!=null) {
			jpql += " and location.employeeGroup = ?";
			parameters.add(q.getEmployeeGroup());
		}

		if(q.getName()!=null && q.getName().length()>0) {

			boolean searchId = false;
			if(q.getName().startsWith(Location.PREFIX)) {
				try {
					int id = Integer.parseInt(q.getName().substring(Location.PREFIX.length()));
					jpql += " and location.id = " + id;
					searchId = true;
				} catch(Exception e) {
					//Not an id
				}
			}
			if(!searchId) {
				jpql += " and (" + QueryTokenizer.expandQuery("lower(location.name) like ? ", q.getName().toLowerCase(), true, true) + ")";
			}
		}
		if(q.getLocationType()!=null) {
			jpql += " and location.locationType = ?";
			parameters.add(q.getLocationType());
		}

		if(q.getBiotype()!=null) {
			jpql += " and exists(select b from Biosample b where b.biotype = ? and b.location = location)";
			parameters.add(q.getBiotype());
		}

		if(q.getOnlyOccupied()!=null) {
			if(q.getOnlyOccupied()) {
				jpql += " and exists(select b from Biosample b where b.location = location)";
			} else {
				jpql += " and not exists(select b from Biosample b where b.location = location) and not exists(select l2 from Location l2 where l2.parent.id = location.id)";
			}
		}


		jpql = JPAUtil.makeQueryJPLCompatible(jpql);
		Query query = session.createQuery(jpql);
		for (int i = 0; i < parameters.size(); i++) {
			query.setParameter(i+1, parameters.get(i));
		}

		List<Location> locations = query.getResultList();
		List<Location> res = new ArrayList<>();
		for (Location location : locations) {
			if(user!=null && !SpiritRights.canRead(location, user)) continue;
			res.add(location);

		}
		Collections.sort(res);

		//
		//Remove moveable location if needed (and keep the first non-moveable parent)
		if(q.isFilterAdminLocation()) {
			List<Location> filtered = new ArrayList<>();
			for (Location l : res) {
				Location l2 = l;
				while(l2!=null && l2.getLocationType().getCategory()==LocationCategory.MOVEABLE) {
					l2 = l2.getParent();
				}
				if(l2!=null && !filtered.contains(l2)) {
					filtered.add(l2);
				}
			}
			res = filtered;
		}

		return res;
	}

	/**
	 * Delete locations after checking rights
	 * @param locations
	 * @param user
	 * @throws Exception
	 */
	public static void deleteLocations(Collection<Location> locations, SpiritUser user) throws Exception {
		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();
			deleteLocations(session, locations, user);
			txn.commit();

		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}
	}

	/**
	 * Delete locations in the same transaction
	 * After checking rights
	 * @param session
	 * @param locations
	 * @param user
	 * @throws Exception
	 */
	public static void deleteLocations(EntityManager session, Collection<Location> locations, SpiritUser user) throws Exception {

		if(user==null) throw new Exception("There is no user");
		for (Location location : locations) {
			if(location.getId()<=0) throw new Exception("the location "+location.getName()+" is not persistant");
			if(location.getBiosamples().size()>0) throw new Exception("You must first checkout all the biosamples in "+location.getName());
			if(!SpiritRights.canDelete(location, user)) throw new Exception("You are not allowed to edit "+location);
			if(location.getChildren()!=null && !locations.containsAll(location.getChildren())) throw new Exception(location+" has children");
		}

		Date now = JPAUtil.getCurrentDateFromDatabase();
		for (Location location : locations) {

			location.setUpdUser(user.getUsername());
			location.setUpdDate(now);

			if(!session.contains(location)) {
				location = session.merge(location);
			}
			session.remove(location);
		}

		Cache.getInstance().remove("locationRoots");
	}




	/**
	 * Find compatible location and returns null ifnone is found
	 * If a user is given, only the location readable by the user are queried
	 *
	 * @param mediumLocation
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static Location getCompatibleLocation(String mediumLocation, SpiritUser user) {
		Set<Location> res;
		try {
			res = queryLocations(mediumLocation, user);
		} catch (Exception e) {
			return null;
		}

		if(res.size()>1) {
			return null;
			//			StringBuilder sb = new StringBuilder();
			//			if(res.size()<5) {
			//				for (Location l : res) {
			//					sb.append("\n "+l.getHierarchyFull());
			//				}
			//			}
			//
			//			throw new Exception(res.size() + " locations found for '"+mediumLocation+"': "+sb.toString());
		} else if(res.size()==0) {
			//			throw new Exception("Unknown location: "+mediumLocation);
			return null;
		} else { //possibles.size==1
			try {
				return res.iterator().next();
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		}
	}

	/**
	 * Fing the location which ends with the given input: ex: "FreezerA/Rack1", there is no need to enter the building.
	 * If user is null, rights are not checked. If a user is given, only the locations readable by this user are returned.
	 *
	 * @param mediumLocation
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static Set<Location> queryLocations(String mediumLocation, SpiritUser user) throws Exception {

		//Is the location fully given?
		String[] split = mediumLocation.split(Location.SEPARATOR, -1);
		Location current = null;
		for (int i = 0; i < split.length; i++) {
			if(current==null) {
				current = Location.get(getLocationRoots(), split[i]);
			} else {
				current = Location.get(current.getChildren(), split[i]);
			}
			if(current==null) break;

			if(i==split.length-1) return Collections.singleton(current);
		}


		//Otherwise we search compatible location by querying the last given location
		String last = mediumLocation.trim();
		if(last.lastIndexOf(Location.SEPARATOR.trim())>=0) {
			last = last.substring(last.lastIndexOf(Location.SEPARATOR.trim())+Location.SEPARATOR.trim().length()).trim();
		}
		Set<Location> possibles = new HashSet<>();
		LocationQuery q = new LocationQuery();
		q.setName(last);
		List<Location> locations = queryLocation(q, user);

		String curatedLoc2 = mediumLocation.replaceAll("\\s*"+Location.SEPARATOR+"\\s*", Location.SEPARATOR).trim();
		for (Location location : locations) {
			String curatedLoc1 = location.getHierarchyFull();
			if(!(Location.SEPARATOR + curatedLoc1).endsWith(Location.SEPARATOR + curatedLoc2)) continue;
			possibles.add(location);
		}
		return possibles;
	}

	public static LocPos getCompatibleLocationPos(String value, SpiritUser user) throws Exception {
		if(value==null) return null;
		//Parse location:pos
		int index = value.lastIndexOf(':');
		String fullLocation;
		String pos;
		if(index>=0) {
			fullLocation = value.substring(0, index).trim();
			pos = value.substring(index+1);
			if(fullLocation.length()==0) throw new Exception("Invalid location format: "+value);
		} else {
			fullLocation = value.trim();
			pos = "";
			if(fullLocation.length()==0 || fullLocation.equals("N/A")) { //No location
				return null;
			}
		}

		Location location = getCompatibleLocation(fullLocation, user);

		return location==null? null: new LocPos(location, location.parsePosition(pos));


	}



	public static void moveLocations(List<Location> locations, Location parent, SpiritUser user) throws Exception {
		//test for cycles
		if(parent!=null) {
			List<Location> hierarchy = parent.getHierarchy();
			for (Location l : locations) {
				if(hierarchy.contains(l)) throw new Exception("This would create a cycle from " + l + " to " + parent);
			}
		}

		EntityManager session = JPAUtil.getManager();
		EntityTransaction txn = null;
		try {
			txn = session.getTransaction();
			txn.begin();


			//Update the locations
			Date now = JPAUtil.getCurrentDateFromDatabase();
			for (Location l : locations) {
				l.setParent(parent);
				l.setUpdUser(user.getUsername());
				l.setUpdDate(now);
				session.merge(l);
			}

			txn.commit();
			Cache.getInstance().remove("locationRoots");
			Cache.getInstance().remove("locations");
		} finally {
			if(txn!=null && txn.isActive()) try{txn.rollback();}catch (Exception e) {e.printStackTrace();}
		}

	}



	public static void updateLocation(Biosample b, String locationPart, SpiritUser user) throws Exception {
		assert b!=null;
		if(locationPart!=null && locationPart.length()>0) {
			LocPos loc = DAOLocation.getCompatibleLocationPos(locationPart, user);
			if(loc==null) {
				loc = new LocPos(new Location(locationPart));
			}
			if(!loc.getLocation().equals(b.getLocation()) || loc.getPos()!=b.getPos()) {
				b.setLocPos(loc.getLocation(), loc.getPos());
			}
		} else {
			b.setLocPos(null, -1);
		}
	}


}
