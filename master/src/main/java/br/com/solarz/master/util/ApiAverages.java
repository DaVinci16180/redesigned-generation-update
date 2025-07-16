package br.com.solarz.master.util;

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
        N = usinasAmount;
        times = new ArrayDeque<>(N);
        errors = new ArrayDeque<>(N);
    }

    public ApiAverages(int usinasAmount, int N, Deque<Long> times, Deque<Boolean> errors) {
        this.usinasAmount = usinasAmount;
        this.N = usinasAmount;
        this.times = times;
        this.errors = errors;
    }

    public double averageTime() {
        if (times.isEmpty())
            return 0.;

        long sum = times.stream().reduce(Long::sum).get();

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

    public ApiAverages sum(ApiAverages other) {
        this.times.addAll(other.times);
        this.errors.addAll(other.errors);
        return this;
    }
}
