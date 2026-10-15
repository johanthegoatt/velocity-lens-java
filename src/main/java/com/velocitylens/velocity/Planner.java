package com.velocitylens.velocity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Planner {
  public static final class RankedTask {
    public final Task task;
    public final double score;

    RankedTask(Task task, double score) {
      this.task = task;
      this.score = score;
    }
  }

  private Planner() {}

  public static double score(Task task) {
    return task.value() / task.chargedEffort();
  }

  public static List<RankedTask> rank(List<Task> tasks) {
    List<RankedTask> ranked = new ArrayList<>();
    for (Task task : tasks) {
      ranked.add(new RankedTask(task, score(task)));
    }
    ranked.sort(
        Comparator
            .comparingDouble((RankedTask rankedTask) -> rankedTask.score)
            .reversed()
            .thenComparing(rankedTask -> rankedTask.task.name));
    return ranked;
  }

  /** Above this many table cells the exact packer gives way to score order. */
  static final long MAX_CELLS = 2_000_000L;

  /**
   * The sprint with the most total value that fits the budget. Taking tasks in score
   * order (Dantzig's greedy rule) can leave value on the table: with a budget of 10, a
   * 6-effort task worth 7 has the best score, yet two 5-effort tasks worth 5.5 each are
   * worth 11 together. This solves the 0/1 knapsack exactly by dynamic programming over
   * hundredths of effort, rounding efforts up so the plan never runs over.
   */
  public static List<RankedTask> buildSprint(List<Task> tasks, double budget) {
    if (Double.isNaN(budget) || budget < 0.0d) {
      throw new IllegalArgumentException("budget must be zero or more");
    }
    List<RankedTask> ranked = rank(tasks);
    int n = ranked.size();
    int cap = (int) Math.floor(budget * 100.0d + 1e-9);
    if ((long) (n + 1) * (cap + 1) > MAX_CELLS) {
      return buildSprintByScore(tasks, budget);
    }
    int[] w = new int[n];
    double[] v = new double[n];
    for (int i = 0; i < n; i++) {
      w[i] = (int) Math.ceil(ranked.get(i).task.chargedEffort() * 100.0d - 1e-9);
      v[i] = ranked.get(i).task.value();
    }
    // best[i][c]: most value from the first i tasks within c hundredths of effort.
    double[][] best = new double[n + 1][cap + 1];
    for (int i = 1; i <= n; i++) {
      for (int c = 0; c <= cap; c++) {
        best[i][c] = best[i - 1][c];
        if (w[i - 1] <= c) {
          double with = best[i - 1][c - w[i - 1]] + v[i - 1];
          if (with > best[i][c] + 1e-9) {
            best[i][c] = with;
          }
        }
      }
    }
    List<RankedTask> selected = new ArrayList<>();
    int c = cap;
    for (int i = n; i >= 1; i--) {
      if (best[i][c] != best[i - 1][c]) {
        selected.add(ranked.get(i - 1));
        c -= w[i - 1];
      }
    }
    selected.sort(Comparator.comparingInt(ranked::indexOf));
    return selected;
  }

  /** Score order until the budget runs out. Kept for comparison and for very large inputs. */
  public static List<RankedTask> buildSprintByScore(List<Task> tasks, double budget) {
    List<RankedTask> ranked = rank(tasks);
    List<RankedTask> selected = new ArrayList<>();
    double used = 0.0d;

    for (RankedTask rankedTask : ranked) {
      if (used + rankedTask.task.chargedEffort() > budget) {
        continue;
      }
      selected.add(rankedTask);
      used += rankedTask.task.chargedEffort();
    }
    return selected;
  }

  public static double totalValue(List<RankedTask> sprint) {
    double sum = 0.0d;
    for (RankedTask r : sprint) {
      sum += r.task.value();
    }
    return sum;
  }

  public static double totalEffort(List<RankedTask> sprint) {
    double sum = 0.0d;
    for (RankedTask r : sprint) {
      sum += r.task.chargedEffort();
    }
    return sum;
  }
}


