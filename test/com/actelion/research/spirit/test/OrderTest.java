package com.actelion.research.spirit.test;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.actelion.research.spiritcore.business.order.Order;
import com.actelion.research.spiritcore.business.order.OrderContainer;
import com.actelion.research.spiritcore.business.order.OrderStatus;
import com.actelion.research.spiritcore.services.dao.DAOOrder;


public class OrderTest extends AbstractSpiritTest {

	@Test
	public void testOrders() throws Exception {
		//Test emptyness
		Assert.assertEquals(0, DAOOrder.getActiveOrders(7).size());


		//Add an order
		Order order = new Order();
		order.add(new OrderContainer("testContainer1"));
		order.add(new OrderContainer("testContainer2"));
		DAOOrder.persistOrders(Collections.singleton(order), user);

		int id = order.getId();
		Assert.assertTrue(id>0);
		Assert.assertEquals(1, DAOOrder.getActiveOrders(7).size());

		//Reload
		order = DAOOrder.getOrder(id);
		Assert.assertNotNull(order);
		Assert.assertEquals(OrderStatus.PLANNED, order.getStatus());
		Assert.assertEquals(2, order.getContainerMap().size());

		//Change status
		order.setStatus(OrderStatus.ACCEPTED);
		order.add(new OrderContainer("testContainer3"));
		DAOOrder.persistOrders(Collections.singleton(order), user);
		Assert.assertEquals(OrderStatus.ACCEPTED, order.getStatus());
		Assert.assertEquals(3, order.getContainerMap().size());


	}

}
