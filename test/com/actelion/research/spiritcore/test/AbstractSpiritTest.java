package com.actelion.research.spiritcore.test;

import org.junit.BeforeClass;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.adapter.HSQLMemoryAdapter;
import com.actelion.research.spiritcore.adapter.SchemaCreator;
import com.actelion.research.spiritcore.business.Exchange;
import com.actelion.research.spiritcore.services.SpiritUser;

public abstract class AbstractSpiritTest {

	protected static SpiritUser user;

	@BeforeClass
	public static final void initDB() throws Exception {
		//Init user
		user = SpiritUser.getFakeAdmin();

		//Init DB
		DBAdapter.setAdapter(new HSQLMemoryAdapter());
	}

	public static Exchange initDemoExamples(SpiritUser user) throws Exception {
		//Clean previous examples
		SchemaCreator.clearExamples(user);
		return SchemaCreator.createExamples(user);
	}

}
