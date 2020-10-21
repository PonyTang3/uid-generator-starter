package com.baidu.fsg.uid.worker.dao;

import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baidu.fsg.uid.worker.entity.WorkerNodeEntity;

public class WorkerNodeZK implements WorkerNodeResposity{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerNodeZK.class);

	private final DistributedAtomicLong distAtomicLong;
	
	public WorkerNodeZK(DistributedAtomicLong distAtomicLong) {
		this.distAtomicLong = distAtomicLong;
	}

	@Override
	public WorkerNodeEntity getWorkerNodeByHostPort(String host, String port) {
		return null;
	}

	@Override
	public void addWorkerNode(WorkerNodeEntity entity) {
		try {
			AtomicValue<Long> sequence = distAtomicLong.increment();
			if (sequence.succeeded()) {
                Long seq = sequence.postValue();
                entity.setId(seq);
                LOGGER.info("WorkerNode id:{},hostName:{},port:{},type:{}",entity.getId(),entity.getHostName(),entity.getPort(),entity.getType());
            }else {
            	throw new RuntimeException("DistributedAtomicLong Error");
            }
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
