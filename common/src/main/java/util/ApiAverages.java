package util;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Deque;

@Getter
public class ApiAverages {
    private final int usinasAmount;
    private final int N;
    private final Deque<Long> times;
    private final Deque<Boolean> errors;

    public ApiAverages(int usinasAmount) {
        this.usinasAmount = usinasAmount;
        N = Math.max((int) Math.round(usinasAmount * 0.1), 20); // 10% do total de usinas, com um mínimo de 20 usinas
        times = new ArrayDeque<>(N);
        errors = new ArrayDeque<>(N);
    }

    public ApiAverages(int usinasAmount, int N, Deque<Long> times, Deque<Boolean> errors) {
        this.usinasAmount = usinasAmount;
        this.N = N;
        this.times = times;
        this.errors = errors;
    }

    public void register(long tempoMs, boolean error) {
        if (times.size() == N)
            times.pollFirst();

        if (errors.size() == N)
            errors.pollFirst();

        if (!error)
            times.addLast(tempoMs);
        errors.addLast(error);
    }

    public double averageTime() {
        if (times.size() < N)
            return 0.;

        long sum = times.stream().reduce(Long::sum).get();

        return sum / (double) times.size();
    }

    public double errorRate() {
        if (errors.size() < N)
            return 0.;

        long errorAmount = errors
                .stream()
                .filter(e -> e)
                .count();

        return errorAmount / (double) errors.size();
    }

    public ApiAverages add(ApiAverages other) {
        this.times.addAll(other.times);
        this.errors.addAll(other.errors);
        return this;
    }
}
