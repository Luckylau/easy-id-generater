package lucky.id.generator.server.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lucky.id.generator.server.exception.IdGeneratorException;
import lucky.id.generator.server.util.NamingThreadFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static lucky.id.generator.server.enums.ZookeeperHolder.PATH_FOREVER;

/**
 * @Author luckylau
 * @Date 2023/5/28
 */
@Slf4j
public class SnowflakeZookeeperHolder {

    private final String ip;
    private final int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private final CuratorFramework curator;
    private final int threshold = 10;
    private String zkAddressNode;
    private long lastUpdateTime = 0L;

    public SnowflakeZookeeperHolder(String connectionString, String ip, int port) {
        this.ip = ip;
        this.port = port;
        curator = createWithOptions(connectionString, new RetryUntilElapsed(1000, 4), 10000, 6000);
        curator.start();
    }

    private String getListenAddress() {
        return ip + ":" + port;
    }

    public long getWorkerId() {
        return Long.parseLong(zkAddressNode.split("-")[1]);
    }

    /**
     * /usr/local/etc/zookeeper/zoo.cfg
     * To have launchd start zookeeper now and restart at login:
     * brew services start zookeeper
     * Or, if you don't want/need a background service you can just run:
     * zkServer start
     */
    public void init() {
        try {
            Stat stat = curator.checkExists().forPath(PATH_FOREVER);
            if (stat == null) {
                //不存在根节点,机器第一次启动,创建/snowflake/workId/forever/ip:port-0000000000
                zkAddressNode = createPersistentNode();
            } else {
                List<String> nodes = curator.getChildren().forPath(PATH_FOREVER);
                //检查该机器时间是否正确：abs(系统时间-sum(time)/nodeSize) < 10ms
                checkInitTimeStamp(nodes);
                //ip:port->0000000001
                Map<String, Integer> nodeMap = Maps.newHashMap();
                //ip:port->(ip:port-0000000001)
                Map<String, String> realNode = Maps.newHashMap();
                for (String key : nodes) {
                    String[] nodeKey = key.split("-");
                    realNode.put(nodeKey[0], key);
                    nodeMap.put(nodeKey[0], Integer.parseInt(nodeKey[1]));
                }
                Integer workerId = nodeMap.get(getListenAddress());
                if (workerId == null) {
                    //新建节点
                    zkAddressNode = createPersistentNode();
                } else {
                    zkAddressNode = PATH_FOREVER + "/" + realNode.get(getListenAddress());
                }
            }
            scheduledUploadData();
        } catch (Exception e) {
            log.error("start node error", e);
            throw new IdGeneratorException(e.getMessage());
        }
    }

    private void checkInitTimeStamp(List<String> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        long sumTimestamp = 0L;
        int nodeCount = 0;
        for (String node : nodes) {
            try {
                byte[] data = curator.getData().forPath(PATH_FOREVER + "/" + node + "/data");
                if (data == null) {
                    log.warn("get node: {} data error", node);
                    continue;
                }
                Endpoint endpoint = mapper.readValue(data, Endpoint.class);
                sumTimestamp += endpoint.getTimestamp();
                nodeCount += 1;
            } catch (Exception e) {
                log.warn("get node: {} data error", node);
            }
        }
        if (nodeCount == 0) {
            return;
        }
        long currentTimestamp = System.currentTimeMillis();
        long avgTimestamp = sumTimestamp / nodeCount;
        if (Math.abs(currentTimestamp - avgTimestamp) > threshold) {
            log.error("currentTimestamp: {} avgTimestamp: {}", currentTimestamp, avgTimestamp);
            throw new IdGeneratorException("init timestamp check error");
        }
    }

    private void scheduledUploadData() {
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1,
                NamingThreadFactory.create("schedule-upload-time"));
        scheduledExecutorService.scheduleWithFixedDelay(
                this::updateNewData, 1L, 3L, TimeUnit.SECONDS);
    }

    private void updateNewData() {
        try {
            if (System.currentTimeMillis() < lastUpdateTime) {
                return;
            }
            //更新临时节点下的值
            Stat stat = curator.checkExists().forPath(zkAddressNode + "/data");
            if (stat == null) {
                curator.create().withMode(CreateMode.EPHEMERAL).forPath(zkAddressNode + "/data", buildData().getBytes());
            } else {
                curator.setData().forPath(zkAddressNode + "/data", buildData().getBytes());
            }
            lastUpdateTime = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("update init data error path : {} is  error ", zkAddressNode, e);
        }
    }

    private CuratorFramework createWithOptions(String connectionString, RetryPolicy retryPolicy, int connectionTimeoutMs, int sessionTimeoutMs) {
        return CuratorFrameworkFactory.builder().connectString(connectionString)
                .retryPolicy(retryPolicy)
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .build();
    }

    /**
     * 创建持久顺序节点 ,并把节点数据放入 value
     *
     * @return String
     * @throws Exception
     */
    private String createPersistentNode() {
        try {
            return curator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(PATH_FOREVER + "/" + getListenAddress() + "-");
        } catch (Exception e) {
            log.error("create node error msg {} ", e.getMessage());
            throw new IdGeneratorException(e.getMessage());
        }
    }

    /**
     * 构建需要上传的数据
     *
     * @return
     */
    private String buildData() throws JsonProcessingException {
        Endpoint endpoint = new Endpoint(ip, port, System.currentTimeMillis());
        return mapper.writeValueAsString(endpoint);
    }

    @Data
    static class Endpoint {
        private String ip;
        private int port;
        private long timestamp;

        public Endpoint() {
        }

        public Endpoint(String ip, int port, long timestamp) {
            this.ip = ip;
            this.port = port;
            this.timestamp = timestamp;
        }
    }
}
