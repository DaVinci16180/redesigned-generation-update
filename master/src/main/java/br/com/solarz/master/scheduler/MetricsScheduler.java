package br.com.solarz.master.scheduler;

import br.com.solarz.master.MasterApplication;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import model.Api;
import model.ApiScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import repository.ApiScoreRepository;
import repository.UsinaRepository;
import util.ApiAverages;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MetricsScheduler {

    Logger logger = LoggerFactory.getLogger(MetricsScheduler.class);
    private final RestClient client = RestClient.create();

    private final ApiScoreRepository apiScoreRepository;
    private final UsinaRepository usinaRepository;
    private final MeterRegistry meterRegistry;

    public static Instant start = null;
    private int checkpoint = 2; // minutos

    @Scheduled(cron = "*/5 * * * * *")
    public void processarAtualizacaoDeGeracaoFila() throws IOException {
        if (start == null)
            return;

        Instant now = Instant.now();
        if (Duration.between(start, now).toMinutes() >= checkpoint) {
            int updated = usinaRepository.countByUpdated(true);
            logger.info("Usinas atualizadas em {} minutos: {}", checkpoint, updated);

            FileOutputStream fos = new FileOutputStream("logs.txt", true);
            fos.write((ZonedDateTime.now() + " - Usinas atualizadas em " + checkpoint + " minutos: " + updated + "\n").getBytes());
            fos.close();

            updateAverages();
            checkpoint += 2;
        }
    }

    private void updateAverages() {
        Map<Long, ApiAverages> averages = new HashMap<>();

        for (String addr : MasterApplication.WORKERS_ADDR) {
            String response = client.get()
                    .uri("http://" + addr + ":8081/simulation/averages")
                    .retrieve()
                    .body(String.class);

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> map = objectMapper.readValue(response, new TypeReference<>(){});

                for (var entry : map.entrySet()) {
                    long apiId = Long.parseLong(entry.getKey());

                    Map<String, Object> avgs = (Map<String, Object>) entry.getValue();

                    int usinasAmount = (int) avgs.get("usinasAmount");
                    int N = (int) avgs.get("n");

                    List<Integer> timeList = (ArrayList<Integer>) avgs.get("times");
                    Deque<Long> times = new ArrayDeque<>();
                    times.addAll(timeList.stream().map(x -> (long) x).toList());

                    Deque<Boolean> errors = new ArrayDeque<>((ArrayList<Boolean>) avgs.get("errors"));

                    ApiAverages avg = new ApiAverages(usinasAmount, N, times, errors);

                    if (averages.containsKey(apiId))
                        averages.put(apiId, averages.get(apiId).add(avg));
                    else
                        averages.put(apiId, avg);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        List<ApiScore> scores = apiScoreRepository.findAll();

        for (ApiScore score : scores) {
            Api api = score.getApi();
            ApiAverages average = averages.get(api.getId());
            int notUpdated = usinaRepository.countByUpdatedAndCredencial_Api(false, api);

            score.setAverageTime(average.averageTime());
            score.setErrorRate(average.errorRate());
            score.setPending( notUpdated / (double) average.getUsinasAmount());

            apiScoreRepository.save(score);
        }
    }
}
