package com.actelion.research.spirit.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPanel;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.pivot.analyzer.Analyzer;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.services.migration.MigrationScript;
import com.actelion.research.util.ui.SwingWorkerExtended;

public class SwingWorkerTest extends AbstractSpiritTest {


	@BeforeClass
	public static void init() throws Exception {
		initDemoExamples(user);
		SwingWorkerExtended.SHOW_EXCEPTION = false;
		SwingWorkerExtended.SHOW_EXCEPTION = false;
	}


	/**
	 * Test that there is only one active worker thread per component, and tbhat only the last one is executed
	 */
	@Test
	public void testThreadOverwrite() {
		JPanel panel = new JPanel();
		final long s = System.currentTimeMillis();
		List<Integer> finished = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			final int index = i;
			new SwingWorkerExtended(panel, SwingWorkerExtended.FLAG_ASYNCHRONOUS100MS) {
				@Override
				protected void doInBackground() throws Exception {
					Thread.sleep(200);
				}
				@Override
				protected void done() {
					//Nothing
				}
			}.afterDone(() -> {
				finished.add(index);				System.out.println("Thread-"+index+" completed in "+(System.currentTimeMillis()-s)+"ms");
			});
		}
		//Initially, nothing is fininshed
		Assert.assertEquals(0, finished.size());

		//After 500ms, the last thread should have been executed, and the others not
		try{Thread.sleep(1000);} catch (Exception e) {e.printStackTrace();}
		Assert.assertEquals(1, finished.size());
		Assert.assertEquals((Integer)9, finished.get(0));
	}


	/**
	 * Test that there is only one active worker per component in a real DB scenario
	 */
	@Test
	public void testThreadForAnalyzer() throws Exception {
		JPanel[] panels = new JPanel[]{new JPanel(), new JPanel(), new JPanel()};

		AtomicInteger n = new AtomicInteger();
		List<Study> studies = DAOStudy.getStudies();
		studies = studies.subList(0, 3);
		Assert.assertTrue(studies.size()>0);
		for (int i = 0; i < studies.size(); i++) {
			List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForStudyIds(studies.get(i).getStudyId()), user);
			for (int k = 0; k < 5; k++) {
				final int ii = i;
				final int kk = k;
				new SwingWorkerExtended(panels[i], SwingWorkerExtended.FLAG_ASYNCHRONOUS20MS) {
					@Override
					protected void doInBackground() throws Exception {
						List<Result> res = JPAUtil.reattach(results);
						new Analyzer(res, user);
					}
					@Override
					protected void done() {
						n.incrementAndGet();
					}
				}.afterDone(() -> {
					System.out.println("SwingWorkerTest.testThreadForAnalyzer() "+ii+"_"+kk);

				});
			}
		}

		//Wait for termination
		int timeout = 0;
		do {
			try{Thread.sleep(200);} catch (Exception e) {break;}
			System.out.println("SwingWorkerTest.testThreadForAnalyzer() Wait termination: "+n.get());
			if(timeout++>=250) throw new Exception("TimeOut");
		} while(n.get()<studies.size());
		try{Thread.sleep(200);} catch (Exception e) {e.printStackTrace();}
		Assert.assertEquals(3, studies.size());

	}


	/**
	 * Test that there is only one active worker and that the swingworker properly dispose connection, without creating DB locks
	 */
	@Test
	public void testLock() throws Exception {
		List<Study> studies = DAOStudy.getStudies();
		studies = studies.subList(0, 3);
		Assert.assertTrue(studies.size()>0);
		ExecutorService service = Executors.newSingleThreadExecutor();
		for (int i = 0; i < studies.size(); i++) {
			List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForStudyIds(studies.get(i).getStudyId()), user);

			Runnable r = new Runnable() {
				@Override
				public void run() {
					JPAUtil.reattach(results);
					try{Thread.sleep(1000);}catch (Exception e) {
						e.printStackTrace();
					}
				}
			};

			service.submit(r);
		}
		service.shutdown();
		service.awaitTermination(10, TimeUnit.SECONDS);

		//Test if DB is locked
		SpiritProperties.getInstance().setDBVersion("2.1");
		SpiritProperties.getInstance().saveValues();
		JPAUtil.closeFactory();
		MigrationScript.updateDB(DBAdapter.getInstance().getVendor(), null);
	}
}
