package lucky.id.generator.server.enums;

/**
 * @Author luckylau
 * @Date 2023/5/28
 */
public interface ZookeeperHolder {
    String PARENT_PATH = "/snowflake";
    String PATH_FOREVER = PARENT_PATH + "/workId/forever";
}
