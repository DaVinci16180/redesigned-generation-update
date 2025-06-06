package br.com.solarz.worker.config;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedisClientProvider {

    private static RedissonClient client;

    public static RedissonClient getClient() {
        if (client == null) {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://127.0.0.1:6379"); // ajuste o IP conforme o ambiente
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
