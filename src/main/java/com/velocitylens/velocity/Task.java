package com.velocitylens.velocity;

public final class Task {
  public final String name;
  public final double impact;
  public final double urgency;
  public final double effort;
  public final double confidence;

  /**
   * Confidence can be a fraction (0.8) or a percent (80), the way RICE writes it.
   * NaN, infinite and negative numbers are rejected with the field named, so a bad
   * row fails loudly instead of sorting to the top or the bottom of the plan.
   */
  public Task(String name, double impact, double urgency, double effort, double confidence) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("task name is empty");
    }
    this.name = name.strip();
    this.impact = check(this.name, "impact", impact);
    this.urgency = check(this.name, "urgency", urgency);
    this.effort = check(this.name, "effort", effort);
    double c = check(this.name, "confidence", confidence);
    if (c > 1.0d) {
      if (c > 100.0d) {
        throw new IllegalArgumentException(this.name + ": confidence " + c + " is above 100%");
      }
      c = c / 100.0d;
    }
    this.confidence = c;
  }

  private static double check(String name, String field, double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException(name + ": " + field + " is not a number");
    }
    if (value < 0.0d) {
      throw new IllegalArgumentException(name + ": " + field + " is negative (" + value + ")");
    }
    return value;
  }

  /** Effort the plan charges for: at least a quarter of a unit, so tiny tasks aren't free. */
  public double chargedEffort() {
    return Math.max(effort, 0.25d);
  }

  /** What the task is worth before dividing by effort. */
  public double value() {
    return ((impact * 1.7d) + (urgency * 1.3d)) * confidence;
  }
}
