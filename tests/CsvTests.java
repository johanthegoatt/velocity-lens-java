import com.velocitylens.velocity.Task;
import com.velocitylens.velocity.TaskCsv;
import java.util.List;

final class CsvTests {
  private CsvTests() {}

  static void run() {
    testCsv();
    testCsvErrorsNameTheRow();
  }

  private static void testCsv() {
    String csv = "\uFEFFName,Effort,Impact,Urgency,Confidence\r\n"
        + "\"Fix login, mobile\",3,9,9,90%\r\n"
        + "\"Rewrite \"\"Pricing\"\" page\",4,7,5,0.8\r\n"
        + "\"Two\nlines\",1,1,1,50\r\n\r\n";
    List<Task> tasks = TaskCsv.parse(csv);
    assert tasks.size() == 3 : "expected 3 tasks, got " + tasks.size();
    assert tasks.get(0).name.equals("Fix login, mobile") : tasks.get(0).name;
    assert tasks.get(0).confidence == 0.9d : "90% should read as 0.9";
    assert tasks.get(0).effort == 3.0d : "columns are found by header name";
    assert tasks.get(1).name.equals("Rewrite \"Pricing\" page") : tasks.get(1).name;
    assert tasks.get(2).name.equals("Two\nlines");
  }

  private static void testCsvErrorsNameTheRow() {
    boolean threw = false;
    try {
      TaskCsv.parse("name,impact,urgency,effort,confidence\na,1,1,1,1\nb,1,x,1,1\n");
    } catch (IllegalArgumentException e) {
      threw = true;
      assert e.getMessage().startsWith("row 3: urgency") : e.getMessage();
    }
    assert threw : "expected a bad cell to be rejected";
    threw = false;
    try {
      TaskCsv.parse("name,impact,effort,confidence\n");
    } catch (IllegalArgumentException e) {
      threw = true;
      assert e.getMessage().contains("urgency") : e.getMessage();
    }
    assert threw : "expected a missing column to be rejected";
  }
}
