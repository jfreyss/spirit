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

package com.actelion.research.spiritapp.spirit.services;


public class AdminTools {

//	public static void fixLocation() throws Exception {
//		SpiritUser user = DBSpiritUser.loadUser("freyssj");
//		List<Location> locations = DAOLocation.getLocations();
//		for (Location location : locations) {
//			Set<Integer> pos = new HashSet<Integer>();
//			List<Biosample> bs = new ArrayList<Biosample>();
//			for (Biosample b : location.getBiosamples()) {
//				if(pos.contains(b.getPos())) {
//					bs.add(b);
//				}
//				pos.add(b.getPos());
//			}
//			
//			if(bs.size()>0) {
//				System.out.println(location+" -> Need to relocate "+bs.size()+ " samples");
//				int index = 0;
//				for (Biosample biosample : bs) {
//					while(pos.contains(index)) index++;					
//					biosample.setLocPos(new LocPos(biosample.getLocation(), index++));
//					System.out.println(location+" -> set "+biosample.getSampleId()+" to "+index);
//				}
//			}
//			DAOBiosample.persistBiosamples(bs, user);
//		}
//		
//	}
//	
//	public static void main(String[] args) throws Exception  {
//		fixLocation();
//	}
}
