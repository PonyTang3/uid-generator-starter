package com.baidu.fsg.uid.worker.dao;

import com.baidu.fsg.uid.worker.entity.WorkerNodeEntity;

public interface WorkerNodeResposity {
	WorkerNodeEntity getWorkerNodeByHostPort(String host, String port);
	void addWorkerNode(WorkerNodeEntity entity);
}