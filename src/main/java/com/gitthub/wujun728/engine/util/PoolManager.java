package com.gitthub.wujun728.engine.util;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.gitthub.wujun728.engine.common.ApiDataSource;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PoolManager {

    private static Lock lock = new ReentrantLock();

    private static Lock deleteLock = new ReentrantLock();

  //所有数据源的连接池存在map里
    static ConcurrentHashMap<String, DruidDataSource> map = new ConcurrentHashMap<>();

    public static DruidDataSource getJdbcConnectionPool(ApiDataSource ds) {
        if (map.containsKey(ds.getId())) {
            return map.get(ds.getId());
        } else {
            lock.lock();
            try {
                log.info(Thread.currentThread().getName() + "获取锁");
                if (!map.containsKey(ds.getId())) {
                    DruidDataSource druidDataSource = new DruidDataSource();
                    druidDataSource.setName(ds.getName());
                    druidDataSource.setUrl(ds.getUrl());
                    druidDataSource.setUsername(ds.getUsername());
                    druidDataSource.setPassword(ds.getPassword());
                    druidDataSource.setDriverClassName(ds.getDriver());
                    druidDataSource.setConnectionErrorRetryAttempts(3);       //失败后重连次数
                    druidDataSource.setBreakAfterAcquireFailure(true);
                    map.put(ds.getId(), druidDataSource);
                    log.info("创建Druid连接池成功：{}", ds.getId());
                }
                return map.get(ds.getId());
            } catch (Exception e) {
                return null;
            } finally {
                lock.unlock();
            }
        }
    }

    //删除数据库连接池
    public static void removeJdbcConnectionPool(String id) {
        deleteLock.lock();
        try {
            DruidDataSource druidDataSource = map.get(id);
            if (druidDataSource != null) {
                druidDataSource.close();
                map.remove(id);
            }
        } catch (Exception e) {
            log.error(e.toString());
        } finally {
            deleteLock.unlock();
        }

    }

    public static DruidPooledConnection getPooledConnection(ApiDataSource ds) throws SQLException {
        DruidDataSource pool = PoolManager.getJdbcConnectionPool(ds);
        DruidPooledConnection connection = pool.getConnection();
//        log.info("获取连接成功");
        return connection;
    }
}
