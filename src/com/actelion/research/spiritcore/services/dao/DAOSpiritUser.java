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

package com.actelion.research.spiritcore.services.dao;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.services.SpiritUser;


/**
 * Proxy to the DBAdapter to load/authenticate user
 *
 * @author Joel Freyss
 */
public class DAOSpiritUser {


	/**
	 * Authenticates the username/password.
	 * <li>Does nothing if it is ok
	 * <li>Throws an exception if the authentification failed
	 * @param username
	 * @param password
	 * @throws Exception
	 */
	public static void authenticateUser(String username, char[] password) throws Exception {
		DBAdapter.getInstance().authenticateUser(username, password);
	}

	/**
	 * Loads the given user
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public static SpiritUser loadUser(String username) throws Exception {
		return DBAdapter.getInstance().loadUser(username);
	}


}
