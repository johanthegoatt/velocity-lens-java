package com.velocitylens.velocity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Main {
  private static final String USAGE =
      "Usage: java -cp out com.velocitylens.velocity.Main [tasks.csv] [--budget N] [--greedy]\n"
          + "  tasks.csv  columns: name, impact, urgency, effort, confidence (any order)\n"
          + "  --budget   effort you have this sprint (default 10)\n"
          + "  --greedy   take tasks in score order instead of packing for the most value";

  public static void main(String[] args) {
    String file = null;
    double budget = 10.0d;
    boolean greedy = false;
    try {
      for (int i = 0; i < args.length; i++) {
        switch (args[i]) {
          case "--budget":
            budget = Double.parseDouble(args[++i]);
            break;
          case "--greedy":
            greedy = true;
            break;
          case "-h":
          case "--help":
            System.out.println(USAGE);
            return;
          default:
            file = args[i];
        }
      }
    } catch (RuntimeException e) {
      System.err.println("Could not read the options.\n" + USAGE);
      System.exit(2);
      return;
    }

    try {
      List<Task> tasks = file == null ? sample() : TaskCsv.parse(Files.readString(Path.of(file), StandardCharsets.UTF_8));
      List<Planner.RankedTask> sprint = greedy ? Planner.buildSprintByScore(tasks, budget) : Planner.buildSprint(tasks, budget);
      print(tasks, sprint, budget, greedy);
    } catch (IOException e) {
      System.err.println("Could not open " + file + ": " + e.getMessage());
      System.exit(1);
    } catch (IllegalArgumentException e) {
      System.err.println((file == null ? "" : file + ", ") + e.getMessage());
      System.exit(1);
    }
  }

  static List<Task> sample() {
    return List.of(
        new Task("Portfolio refresh", 9.2d, 8.0d, 4.0d, 0.9d),
        new Task("Telemetry cleanup", 8.0d, 6.8d, 3.0d, 0.85d),
        new Task("Minor UI copy pass", 5.0d, 4.5d, 2.0d, 0.95d));
  }

  static void print(List<Task> tasks, List<Planner.RankedTask> sprint, double budget, boolean greedy) {
    System.out.printf("Velocity Lens sprint batch (budget %.2f, %s):%n", budget, greedy ? "score order" : "most value");
    for (Planner.RankedTask item : sprint) {
      System.out.printf("- %s (effort=%.2f, value=%.2f, score=%.3f)%n",
          item.task.name, item.task.chargedEffort(), item.task.value(), item.score);
    }
    System.out.printf("Value %.2f, effort %.2f of %.2f.%n",
        Planner.totalValue(sprint), Planner.totalEffort(sprint), budget);
    for (Planner.RankedTask item : Planner.rank(tasks)) {
      if (sprint.stream().noneMatch(s -> s.task == item.task)) {
        String why = item.task.chargedEffort() > budget ? "bigger than the whole budget" : "the rest is worth more together";
        if (greedy && item.task.chargedEffort() <= budget) {
          why = "did not fit after higher scores";
        }
        System.out.printf("  left out: %s (%s)%n", item.task.name, why);
      }
    }
  }
}
