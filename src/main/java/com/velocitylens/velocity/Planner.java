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

  public static List<RankedTask> buildSprint(List<Task> tasks, double budget) {
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
}


