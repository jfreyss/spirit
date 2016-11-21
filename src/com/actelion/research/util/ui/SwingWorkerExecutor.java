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

package com.actelion.research.util.ui;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Simple class to maximize the number of Threads. 
 * This is particularly handy in combination of the ThreadLocal class to avoid too many threads and therefore too many object 
 * (for example thread-safe JPA connections)
 * 
 * @author jfreyss
 *
 */
public class SwingWorkerExecutor {

	private static LinkedBlockingQueue<Runnable> threads = new LinkedBlockingQueue<>();
	
	private static final ExecutorService service = 
			new ThreadPoolExecutor(1, 1, 1000, TimeUnit.MILLISECONDS, threads, new ThreadFactory() {	
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			threads.add(t);
			return t;
		}
	});
	
	public static boolean isFromPool(Runnable t) {
		return threads.contains(t);
	}
	
	public static void execute(Runnable runnable) {
		displayStatus();
		service.execute(runnable);
	}
	
	public static Collection<Runnable> getThreads() {
		return threads;
	}
	
	public static void displayStatus() {
		int count=0;
		for (Runnable thread : threads) {
			System.out.println("SwingWorkerExecutor-Thread-"+(++count)+"-"+thread);
		}
	}
	
}
