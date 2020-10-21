package com.baidu.fsg.uid.worker.dao;

import com.baidu.fsg.uid.worker.entity.WorkerNodeEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * 
 * 
 * 使用jdbctemplate替代原来的mybatis
 * @author wj596（https://github.com/wj596）
 *
 */
public class WorkerNodeDAO implements WorkerNodeResposity {
	
	private static final String	GET_WORKER_NODE_BY_HOST_PORT_SQL ="SELECT ID,HOST_NAME,PORT,TYPE,LAUNCH_DATE,MODIFIED,CREATEDFROMWORKER_NODEWHEREHOST_NAME = ? AND PORT = ?";
	private static final String	ADD_WORKER_NODE_SQL ="INSERT INTO WORKER_NODE "
			+ "(HOST_NAME,PORT,TYPE,LAUNCH_DATE,MODIFIED,CREATED)VALUES ("
			+ "?,?,?,?,NOW(),NOW())";
	
	private JdbcTemplate jdbcTemplate;

    public WorkerNodeDAO(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
     * Get {@link WorkerNodeEntity} by node host
     * 
     * @param host
     * @param port
     * @return
     */
	public WorkerNodeEntity getWorkerNodeByHostPort(String host, String port) {
    	return this.jdbcTemplate.queryForObject(GET_WORKER_NODE_BY_HOST_PORT_SQL, new String[]{host, port}, new RowMapper<WorkerNodeEntity>(){

			@Override
			public WorkerNodeEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
				WorkerNodeEntity entity = new WorkerNodeEntity();  
				entity.setId(rs.getLong("ID"));  
				entity.setHostName(rs.getString("HOST_NAME"));
				entity.setPort(rs.getString("PORT"));
				entity.setType(rs.getInt("TYPE"));
				entity.setLaunchDateDate(rs.getDate("LAUNCH_DATE"));
				entity.setModified(rs.getTimestamp("MODIFIED"));
				entity.setCreated(rs.getTime("CREATED"));
				return entity;  
			}
    		
    	});
    }

    /**
     * Add {@link WorkerNodeEntity}
     * 
     * @param workerNodeEntity
     */
	public void addWorkerNode(WorkerNodeEntity entity) {
    	this.jdbcTemplate.update(ADD_WORKER_NODE_SQL, entity.getHostName(),entity.getPort(),entity.getType(),entity.getLaunchDate());
    }

}