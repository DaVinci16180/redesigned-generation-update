package br.com.solarz.worker.util;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Deque;

public class ApiAverages {
    @Getter
    private final int usinasAmount;
    private final int N;
    private final Deque<Long> times;
    private final Deque<Boolean> errors;

    public ApiAverages(int usinasAmount) {
        this.usinasAmount = usinasAmount;
        N = (int) Math.round(usinasAmount * 0.2);
        times = new ArrayDeque<>(N);
        errors = new ArrayDeque<>(N);
    }

    public void register(long tempoMs, boolean error) {
        if (times.size() == N) {
            times.pollFirst();
            errors.pollFirst();
        }

        times.addLast(tempoMs);
        errors.addLast(error);
    }

    public double averageTime() {
        if (times.isEmpty())
            return 0.;

        long sum = times.stream().mapToLong(l -> l).sum();

        return sum / (double) times.size();
    }

    public double errorRate() {
        if (errors.isEmpty())
            return 0.;

        long errorAmount = errors
                .stream()
                .filter(e -> e)
                .count();

        return errorAmount / (double) errors.size();
    }

    public boolean isFull() {
        return times.size() == N;
    }
}
