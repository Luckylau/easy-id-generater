package lucky.id.generator.controller;

import lucky.id.generator.service.SnowflakeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author luckylau
 * @Date 2023/7/15
 */
@RestController
@RequestMapping("/id/")
public class IdController {
    @Autowired
    private SnowflakeService snowflakeService;

    @RequestMapping(value = "/snowflake")
    public Long getSnowflakeId() {
        return snowflakeService.getId();
    }

    @RequestMapping(value = "/snowflake/batch/{range}")
    public Long batchGetSnowflakeId(@PathVariable("range") Integer range) {
        return snowflakeService.getIds(range);
    }


}
