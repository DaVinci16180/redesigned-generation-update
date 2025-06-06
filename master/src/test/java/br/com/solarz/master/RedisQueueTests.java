package br.com.solarz.master;

import br.com.solarz.master.service.QueueBuilderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RedisQueueTests {

    @Autowired
    private QueueBuilderService queueBuilderService;

    @Test
    public void setupQueue() {
        queueBuilderService.setupQueues();
        queueBuilderService.buildQueues();
    }
}
