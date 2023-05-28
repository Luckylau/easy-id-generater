package lucky.id.generator.generator;

/**
 * @Author luckylau
 * @Date 2023/5/27
 */
public interface IdGenerator {
    /**
     * get next id
     *
     * @return Long
     */
    Long getId();

    /**
     * batch get next id
     *
     * @param range
     * @return Long
     */
    Long getIds(int range);
}
