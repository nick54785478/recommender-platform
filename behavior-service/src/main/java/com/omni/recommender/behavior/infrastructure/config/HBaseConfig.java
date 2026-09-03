package com.omni.recommender.behavior.infrastructure.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@org.springframework.context.annotation.Configuration
public class HBaseConfig {

    @Value("${hbase.zookeeper.quorum}")
    private String zookeeperQuorum;

    @Value("${hbase.zookeeper.property.clientPort}")
    private String zookeeperClientPort;

    @Bean
    public Configuration hbaseConfiguration() {
        Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", zookeeperQuorum);
        config.set("hbase.zookeeper.property.clientPort", zookeeperClientPort);
        return config;
    }

    @Bean(destroyMethod = "close")
    public Connection hbaseConnection() throws IOException {
        Connection connection = ConnectionFactory.createConnection(hbaseConfiguration());
        
        // Auto-create table if not exists
        try (org.apache.hadoop.hbase.client.Admin admin = connection.getAdmin()) {
            org.apache.hadoop.hbase.TableName tableName = org.apache.hadoop.hbase.TableName.valueOf("user_behavior_history");
            if (!admin.tableExists(tableName)) {
                org.apache.hadoop.hbase.client.TableDescriptorBuilder tableDescriptorBuilder = 
                        org.apache.hadoop.hbase.client.TableDescriptorBuilder.newBuilder(tableName);
                org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder columnFamilyDescriptorBuilder = 
                        org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder.newBuilder(org.apache.hadoop.hbase.util.Bytes.toBytes("cf"));
                
                tableDescriptorBuilder.setColumnFamily(columnFamilyDescriptorBuilder.build());
                admin.createTable(tableDescriptorBuilder.build());
            }
        }
        
        return connection;
    }
}
