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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.IObject;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.study.NamedSampling;
import com.actelion.research.spiritcore.business.study.Sampling;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.StringEncrypter;
import com.actelion.research.spiritcore.util.QueryTokenizer;

/**
 * JPAUtil class designed for Desktop applications.
 * We have to make sure persistence.xml is in managed mode.
 * Then we keep 1 connection always open for read-only.
 * An other connection is used for editing (needs a call to pusheditablecontext). This one is rollbacked and close after a call to pop.
 * This class is thread-safe
 *
 *
 * Rules to be enforced:
 * 1) A transaction must be open and close in the same method (or one must ensure that no 2 transactions run at the same time)
 * 2) A transaction can only be started after pusheditablecontext, each objet need to be then reload through JPAUtil.reload
 * 3) After each modification, a call to clear is needed to refresh the data
 *
 *
 *
 *
 * @author Joel Freyss
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class JPAUtil {

	public static enum JPAMode {
		READ,
		WRITE,
		REQUEST
	}

	private static class MyThreadLocal extends ThreadLocal<EntityManager> {

		public long lastTestQuery = 0;
		private List<EntityManager> all = Collections.synchronizedList(new ArrayList<EntityManager>());

		/**
		 * Not Thread-safe. Close all entityManagers
		 */
		public void close() {
			for(final EntityManager em : new ArrayList<>(getAll())) {
				LoggerFactory.getLogger(JPAUtil.class).debug("Close EM: "+em+" open="+em.isOpen());
				if(em!=null && em.isOpen()) {
					if(em.getTransaction().isActive()) em.getTransaction().rollback();
					em.close();
				}
			}
		}

		/**
		 * Clear the cache of all entityManagers associated to all threads.
		 * Make sure all related threads are stopped
		 */
		public void clear() {
			for(final EntityManager em : new ArrayList<>(getAll())) {
				if(em!=null && em.isOpen() ) {
					LoggerFactory.getLogger(JPAUtil.class).debug("Clear EM: "+em+" active="+em.getTransaction().isActive());
					if(em.getTransaction().isActive()) em.getTransaction().rollback();
					em.clear();
				}
			}
		}

		@Override
		public EntityManager get() {
			EntityManager em = super.get();

			//Check first if the connection is still alive, execute test query every 5s
			boolean connected = false;
			if(em!=null && em.isOpen()) {
				try {
					String testQuery =  DBAdapter.getAdapter().getTestQuery() ;
					if(testQuery!=null && testQuery.length()>0 && System.currentTimeMillis()-lastTestQuery>5000) {
						em.createNativeQuery(testQuery).getSingleResult();
						lastTestQuery = System.currentTimeMillis();
					}
					connected = true;
				} catch(Exception e) {
					connected = false;
				}
			}


			//If not alive, recreate a connection
			if(!connected) {
				if(em!=null) all.remove(em);
				em  = factory.createEntityManager();
				em.setFlushMode(FlushModeType.AUTO);
				LoggerFactory.getLogger(JPAUtil.class).debug("Create EntityManager");
				all.add(em);
				set(em);

				if(Thread.currentThread().getName().equals("main")) {
					LoggerFactory.getLogger(getClass()).warn("Spirit should not start a new JPA context on the main thread");
				}
			}
			return em;
		}

		public List<EntityManager> getAll() {
			return all;
		}

		@Override
		public void remove() {
			all.remove(get());
			super.remove();
		}
	}

	private static Map<Thread, EntityManager> thread2entityManager = Collections.synchronizedMap(new HashMap<Thread, EntityManager>());


	/**
	 * The currently logged in user (for auditing through Envers)
	 */
	private static Map<Thread, SpiritUser> thread2user = Collections.synchronizedMap(new HashMap<Thread, SpiritUser>());


	private static EntityManagerFactory factory;

	private static JPAMode jpaMode = JPAMode.READ;

	private static MyThreadLocal readEntityManager;
	private static MyThreadLocal writeEntityManager;

	private static Long timeDiff = null;
	private static Long lastSynchro = null;

	static {
		System.setProperty("org.jboss.logging.provider", "slf4j");

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					closeFactory();
				} catch(Throwable e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static void clear() {
		getManager().clear();
	}
	/**
	 * Clear all entity manager and Spirit Cache.
	 * Not Thread safe
	 */
	public static void clearAll() {
		try {
			LoggerFactory.getLogger(JPAUtil.class).debug("Clear Sessions");
			popEditableContext();
			if(readEntityManager!=null) {
				readEntityManager.clear();
			}
			if(writeEntityManager!=null) {
				writeEntityManager.clear();
			}
		} catch(Exception e) {
			LoggerFactory.getLogger(JPAUtil.class).warn("Could not clear managers: ", e);
		}
		LoggerFactory.getLogger(JPAUtil.class).debug("Clear Cache");
		Cache.removeAll();
	}

	public static void closeFactory() {
		try {
			LoggerFactory.getLogger(JPAUtil.class).debug("close sessions");
			if(readEntityManager!=null) {
				readEntityManager.close();
				readEntityManager = null;
			}
			if(writeEntityManager!=null) {
				writeEntityManager.close();
				writeEntityManager = null;
			}
		} catch(Exception e) {
			LoggerFactory.getLogger(JPAUtil.class).warn("Could not clear factory: ", e);
		}
		if(factory!=null) {
			LoggerFactory.getLogger(JPAUtil.class).debug("close factory");
			try {
				factory.close();
			} catch(Exception e) {
				LoggerFactory.getLogger(JPAUtil.class).warn("Could not close factory: ", e);
			}
			factory = null;
		}
		Cache.removeAll();
		SpiritProperties.clear();
	}

	public static void closeRequest() {
		assert jpaMode == JPAMode.REQUEST;
		Thread thread = Thread.currentThread();
		final EntityManager session = thread2entityManager.get(thread);
		if(session!=null && session.isOpen()) {
			session.close();
		}
		thread2entityManager.remove(thread);
	}


	public static void copyProperties(Biosample dest, Biosample src) {
		if (dest == null || src==null) return;
		dest.getAuxiliaryInfos().putAll(src.getAuxiliaryInfos());
		dest.setScannedPosition(src.getScannedPosition());
	}

	/**
	 * Create a new EntityManager - be ABSOLUTELY SURE to close it
	 * @return
	 */
	public static EntityManager createManager() {
		if(factory==null) {
			initialize();
		}

		return factory.createEntityManager();
	}

	public static Date getCurrentDateFromDatabase() {
		if(timeDiff==null || System.currentTimeMillis()-lastSynchro>5*3600*1000L) {//Resync every 5h
			Date now;
			EntityManager em = null;
			try {
				em = createManager();
				DBAdapter adapter = DBAdapter.getAdapter();

				assert em!=null;
				assert adapter!=null;
				now = (Date) em.createNativeQuery(adapter.getCurrentDateQuery()).getSingleResult();
			} catch(Throwable e) {
				e.printStackTrace();
				now = new Date();
			} finally {
				if(em!=null) em.close();
			}
			timeDiff = now.getTime() - new Date().getTime();
			lastSynchro = System.currentTimeMillis();
		}
		return new Date(System.currentTimeMillis()+timeDiff);

	}

	public static<T extends IObject> List<Integer> getIds(Collection<T> object) {
		List<Integer> res = new ArrayList<>();
		for (T o : object) {
			if(o!=null) res.add(o.getId());
		}
		return res;
	}

	public static JPAMode getJpaMode() {
		return jpaMode;
	}

	/**
	 * Get an entitymanager based on the current context (and thread):
	 * - read: returns the open EM, which is always open
	 * - write: returns the open EM, which is open/close together with the dialogs
	 * - request: returns the associated EM, which MUST be created first  with openRequest and which MUST be closed in a ServletFilter
	 * @return
	 */
	public static EntityManager getManager() {
		if(factory==null) {
			initialize();
		}
		EntityManager em;
		switch (jpaMode) {
		case READ:
			em = readEntityManager.get();
			//			((HibernateEntityManager)em).getSession().setDefaultReadOnly(true);
			return em;
		case WRITE:
			assert writeEntityManager!=null;
			em = writeEntityManager.get();
			//			((HibernateEntityManager)em).getSession().setDefaultReadOnly(false);
			return em;
		case REQUEST:
			em = thread2entityManager.get(Thread.currentThread());
			assert em!=null : "You must use this block: try {openRequest} finally {closeRequest}";
			return em;
		default:
			assert false : "Invalid mode: "+jpaMode;
		return readEntityManager.get();
		}
	}

	public static SpiritUser getSpiritUser() {
		if(jpaMode==JPAMode.REQUEST) {
			return thread2user.get(Thread.currentThread());
		} else {
			return thread2user.get(null);
		}
	}

	/**
	 * Inits the JPA factory
	 * @throws Exception
	 */
	protected static void initFactory() throws Exception {
		DBAdapter adapter = DBAdapter.getAdapter();
		if(factory!=null) closeFactory();
		adapter.preInit();
		initFactory(adapter, "");
		adapter.postInit();
		assert factory!=null;
	}


	/**
	 * Inits the JPA factory, but without calling preInit/postInit
	 * @param adapter
	 * @param mode
	 * @throws Exception
	 */
	public static void initFactory(DBAdapter adapter, String mode) throws Exception {
		LoggerFactory.getLogger(JPAUtil.class).debug("initFactory on "+adapter.getClass().getName()+" url="+adapter.getDBConnectionURL()+" mode="+mode);
		closeFactory();
		Map properties = new HashMap();
		properties.put("hibernate.dialect", adapter.getHibernateDialect());
		properties.put("hibernate.connection.driver_class", adapter.getDriverClass());
		properties.put("hibernate.connection.username", adapter.getDBUsername());
		properties.put("hibernate.connection.password", new String(new StringEncrypter("program from joel").decrypt(adapter.getDBPassword())));
		properties.put("hibernate.show_sql", "true".equalsIgnoreCase(System.getProperty("show_sql")));
		properties.put("hibernate.hbm2ddl.auto", mode);
		properties.put("hibernate.connection.url", adapter.getDBConnectionURL());
		properties.put("hibernate.default_schema", "spirit");

		LoggerFactory.getLogger(JPAUtil.class).debug("create factory");
		factory = Persistence.createEntityManagerFactory("spirit", properties);
		LoggerFactory.getLogger(JPAUtil.class).debug("factory created");

		if(jpaMode==JPAMode.REQUEST) {
			readEntityManager = null;
			writeEntityManager = null;
		} else {
			readEntityManager = new MyThreadLocal();
			writeEntityManager = jpaMode==JPAMode.WRITE? new MyThreadLocal(): null;
		}
	}

	private static void initialize() {
		LoggerFactory.getLogger(JPAUtil.class).debug("JPA Factory Initialized");
		assert factory == null;
		try {
			initFactory();
			getCurrentDateFromDatabase();
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Throwable ex2) {
			LoggerFactory.getLogger(JPAUtil.class).error("Spirit: Initial EntityManagerFactory creation failed.", ex2);
			throw new RuntimeException(ex2);
		}
	}


	public static boolean isEditableContext() {
		return jpaMode == JPAMode.WRITE || jpaMode == JPAMode.REQUEST;
	}

	/**
	 * Replace '?' by '?#'
	 * @param jpql
	 * @return
	 */
	protected static String makeQueryJPLCompatible(String jpql) {
		StringTokenizer st = new StringTokenizer(jpql, "?'", true);
		StringBuilder sb = new StringBuilder();
		boolean inBrace = false;
		int index = 0;
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if(token.equals("\'")) {
				inBrace = !inBrace;
				sb.append(token);
			} else if(token.equals("?") && !inBrace) {
				sb.append("?" + (++index));
			} else {
				sb.append(token);
			}
		}
		return sb.toString();
	}

	public static<T extends IObject> Map<Integer, T> mapIds(Collection<T> object) {
		Map<Integer, T> res = new HashMap<>();
		for (T o : object) {
			if(o!=null && o.getId()>0) res.put(o.getId(), o);
		}
		return res;
	}

	public static EntityManager openRequest() {
		if(factory==null) {
			initialize();
		}

		assert jpaMode == JPAMode.REQUEST;
		Thread thread = Thread.currentThread();
		EntityManager session = thread2entityManager.get(thread);
		if(session==null || !session.isOpen()) {
			session = factory.createEntityManager();
			session.setFlushMode(FlushModeType.AUTO);
			thread2entityManager.put(thread, session);
		}
		return session;
	}
	/**
	 * Pop and rollback the edit connection
	 */
	public static void popEditableContext() {
		if(jpaMode==JPAMode.WRITE) {
			jpaMode = JPAMode.READ;
		}
	}

	public static void pushEditableContext(SpiritUser user) {
		assert jpaMode!=JPAMode.REQUEST;

		if(readEntityManager==null) try {
			initFactory();
		} catch (Exception e) {
			e.printStackTrace();
		}

		jpaMode = JPAMode.WRITE;

		if(writeEntityManager!=null) {
			writeEntityManager.close();
		}
		setSpiritUser(user);
		writeEntityManager = new MyThreadLocal();
	}

	/**
	 * Check if the object belong to the current session (associated to the current thread).
	 * If the object is not in the session, the object is loaded from the DB and kept attached to the session.
	 *
	 * If the object is a biosample, reattach does not affect the scanned position or transient properties
	 *
	 */
	public static<T extends IObject> List<T> reattach(Collection<T> objects) {
		if(objects==null) return null;
		long s = System.currentTimeMillis();
		List<T> res = new ArrayList<>(objects);
		if(res.size()==0) return res;

		//Check which object are not attached to the session and push them to reload them
		EntityManager entityManager = getManager();
		List<Integer> toBeReloadedIds = new ArrayList<>();
		Map<Integer, Integer> id2index = new HashMap<>();
		for (int i = 0; i < res.size(); i++) {
			T o = res.get(i);
			if(o==null) {
				continue;
			} else if( o.getId()>0 && !entityManager.contains(o) ) {
				//The entity is in the DB but is not attached
				assert id2index.get(o.getId())==null: "Object " + o.getClass() + " id: " + o.getId()+" is present 2 times" ;
				toBeReloadedIds.add(o.getId());
				id2index.put(o.getId(), i);
			} else if(o.getId()<=0) {
				//The entity is new. Reload the dependancies if needed
				if(o instanceof Study) {
					for (NamedSampling a : ((Study)o).getNamedSamplings()) {
						for(Sampling ss: a.getAllSamplings()) {
							if(ss.getId()>0 && !entityManager.contains(ss)) {
								throw new RuntimeException(o+" has Sampling dependancies not in the session");
							}
							if(ss.getBiotype().getId()>0 && !entityManager.contains(ss.getBiotype())) {
								ss.setBiotype(JPAUtil.reattach(ss.getBiotype()));
							}
						}
					}
				} else if(o instanceof Biosample) {
					((Biosample) o).setBiotype(JPAUtil.reattach(((Biosample) o).getBiotype()));
					((Biosample) o).setParent(JPAUtil.reattach(((Biosample) o).getParent()));
					((Biosample) o).setInheritedStudy(JPAUtil.reattach(((Biosample) o).getInheritedStudy()));
					((Biosample) o).setInheritedPhase(JPAUtil.reattach(((Biosample) o).getInheritedPhase()));
					((Biosample) o).setInheritedGroup(JPAUtil.reattach(((Biosample) o).getInheritedGroup()));
				} else if(o instanceof Location) {
					((Location)o).setParent(JPAUtil.reattach(((Location)o).getParent()));
				} else if(o instanceof Result) {
					((Result)o).setBiosample(JPAUtil.reattach(((Result)o).getBiosample()));
					((Result)o).setPhase(JPAUtil.reattach(((Result)o).getPhase()));
				}
			} else {
				//The entity is already attached to the session
			}
		}

		Class<?> claz  = objects.iterator().next().getClass();
		if(!toBeReloadedIds.isEmpty()) {
			//Get the proper class to load
			if(claz.getName().contains("_$$_")) claz = claz.getSuperclass();

			//Reload detached objects
			String jpql = "select o from " + claz.getSimpleName() +" o where " + QueryTokenizer.expandForIn("o.id", toBeReloadedIds);
			List<T> reloaded = entityManager.createQuery(jpql).getResultList();
			for (T o : reloaded) {
				int index = id2index.get(o.getId());
				if(o instanceof Biosample) {
					copyProperties((Biosample)o, (Biosample)res.get(index));
				} else if(o instanceof Study) {
					DAOStudy.postLoad(Collections.singleton(((Study) o)));
				}
				res.set(index, o);
			}
		}

		LoggerFactory.getLogger(JPAUtil.class).debug("Reattach "+claz.getSimpleName()+": n="+toBeReloadedIds.size()+" done in "+(System.currentTimeMillis()-s)+"ms");
		return res;
	}

	public static boolean isValid(IObject object) {
		return Persistence.getPersistenceUtil().isLoaded(object);
	}

	/**
	 * Reattach the object to the current session (associated to the current thread)
	 * This function must be called in the constructor of each dialog
	 * @param object
	 * @return
	 */
	public static<T extends IObject> T reattach(T object) {
		if(object==null) return null;

		List<T> res = reattach(Collections.singletonList(object));
		if(res.size()==1) return res.get(0);
		return null;
	}
	public static void setRequestMode() {
		if(jpaMode==JPAMode.REQUEST) return;
		assert factory==null: "setRequestMode must be called before accessing the session";
		jpaMode = JPAMode.REQUEST;
	}

	public static void setSpiritUser(SpiritUser user) {
		if(jpaMode==JPAMode.REQUEST) {
			thread2user.put(Thread.currentThread(), user);
		} else {
			thread2user.put(null, user);
		}
	}

}