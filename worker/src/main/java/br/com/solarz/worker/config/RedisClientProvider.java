package br.com.solarz.worker.config;
import br.com.solarz.worker.WorkerApplication;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;

public class RedisClientProvider {

    @Value("${MASTER_ADDR}")
    private static String MASTER_ADDR;
    private static RedissonClient client;

    public static RedissonClient getClient() {
        if (client == null) {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://" + MASTER_ADDR + ":6379"); // ajuste o IP conforme o ambiente
            client = Redisson.create(config);
        }

        return client;
    }

    public static void shutdown() {
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }
}
