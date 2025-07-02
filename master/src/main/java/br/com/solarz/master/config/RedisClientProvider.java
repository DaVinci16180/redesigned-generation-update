package br.com.solarz.master.config;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisClientProvider {

    @Value("${DOCKER_ADDR}")
    private String DOCKER_ADDR;
    private RedissonClient client;

    public RedissonClient getClient() {
        if (client == null) {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://" + DOCKER_ADDR + ":6379"); // ajuste o IP conforme o ambiente
            client = Redisson.create(config);
        }

        return client;
    }
}
