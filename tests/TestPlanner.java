import com.velocitylens.velocity.Planner;
import com.velocitylens.velocity.Task;
import java.util.List;

public final class TestPlanner {
  public static void main(String[] args) {
    testRankOrder();
    testSprintBudget();
    testRejectsBadNumbers();
    testConfidencePercent();
    testTinyTasksAreNotFree();
    System.out.println("Velocity Lens Java tests passed.");
  }

  private static void testRankOrder() {
    List<Task> tasks = List.of(
        new Task("low", 4.0d, 4.0d, 5.0d, 0.8d),
        new Task("high", 9.0d, 8.5d, 3.0d, 0.9d));

    List<Planner.RankedTask> ranked = Planner.rank(tasks);
    assert "high".equals(ranked.get(0).task.name) : "expected high to rank first";
  }

  private static void testSprintBudget() {
    List<Task> tasks = List.of(
        new Task("a", 8.0d, 8.0d, 4.0d, 0.8d),
        new Task("b", 7.0d, 7.0d, 4.0d, 0.85d),
        new Task("c", 6.0d, 6.0d, 3.0d, 0.9d));

    List<Planner.RankedTask> sprint = Planner.buildSprint(tasks, 7.0d);
    double used = 0.0d;
    for (Planner.RankedTask item : sprint) {
      used += item.task.effort;
    }
    assert used <= 7.0d : "sprint used more effort than budget";
  }

  private static void testRejectsBadNumbers() {
    double[][] bad = {
        {Double.NaN, 1, 1, 0.5}, {1, -2, 1, 0.5}, {1, 1, Double.POSITIVE_INFINITY, 0.5}, {1, 1, 1, 150}
    };
    for (double[] b : bad) {
      boolean threw = false;
      try {
        new Task("bad", b[0], b[1], b[2], b[3]);
      } catch (IllegalArgumentException e) {
        threw = true;
        assert e.getMessage().startsWith("bad:") : "error should name the task: " + e.getMessage();
      }
      assert threw : "expected a bad row to be rejected";
    }
  }

  private static void testConfidencePercent() {
    Task a = new Task("a", 5, 5, 2, 80);
    Task b = new Task("b", 5, 5, 2, 0.8);
    assert a.confidence == 0.8d : "80 should read as 80%";
    assert Planner.score(a) == Planner.score(b) : "80 and 0.8 should score the same";
  }

  private static void testTinyTasksAreNotFree() {
    List<Task> tasks = List.of(
        new Task("t1", 1, 1, 0, 1), new Task("t2", 1, 1, 0, 1),
        new Task("t3", 1, 1, 0, 1), new Task("t4", 1, 1, 0, 1),
        new Task("t5", 1, 1, 0, 1));
    List<Planner.RankedTask> sprint = Planner.buildSprint(tasks, 1.0d);
    assert sprint.size() == 4 : "five zero-effort tasks at 0.25 each fit four in 1.0, got " + sprint.size();
  }
}
