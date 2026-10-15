package com.velocitylens.velocity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads tasks from CSV as RFC 4180 writes it, so a sheet exported from Excel or Google
 * Sheets works as is: quoted fields, commas and line breaks inside quotes, doubled
 * quotes, CRLF line ends and a UTF-8 byte order mark. Columns are found by header name
 * in any order (name, impact, urgency, effort, confidence); confidence may be 0.8, 80
 * or 80%.
 */
public final class TaskCsv {
  private TaskCsv() {}

  public static List<Task> parse(String text) {
    List<List<String>> rows = rows(text);
    if (rows.isEmpty()) {
      throw new IllegalArgumentException("the CSV is empty");
    }
    List<String> header = rows.get(0);
    String[] want = {"name", "impact", "urgency", "effort", "confidence"};
    int[] col = new int[want.length];
    for (int k = 0; k < want.length; k++) {
      col[k] = -1;
      for (int i = 0; i < header.size(); i++) {
        if (header.get(i).strip().toLowerCase(Locale.ROOT).equals(want[k])) {
          col[k] = i;
        }
      }
      if (col[k] < 0) {
        throw new IllegalArgumentException("missing column \"" + want[k] + "\" in the header");
      }
    }
    List<Task> tasks = new ArrayList<>();
    for (int r = 1; r < rows.size(); r++) {
      List<String> row = rows.get(r);
      if (row.size() == 1 && row.get(0).isBlank()) {
        continue;
      }
      try {
        tasks.add(new Task(
            cell(row, col[0]),
            number(cell(row, col[1]), "impact"),
            number(cell(row, col[2]), "urgency"),
            number(cell(row, col[3]), "effort"),
            number(cell(row, col[4]).replace("%", ""), "confidence")));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("row " + (r + 1) + ": " + e.getMessage());
      }
    }
    return tasks;
  }

  private static String cell(List<String> row, int i) {
    return i < row.size() ? row.get(i).strip() : "";
  }

  private static double number(String s, String field) {
    if (s.isEmpty()) {
      throw new IllegalArgumentException(field + " is empty");
    }
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(field + " \"" + s + "\" is not a number");
    }
  }

  static List<List<String>> rows(String text) {
    if (text.startsWith("\uFEFF")) {
      text = text.substring(1);
    }
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (quoted) {
        if (ch == '"') {
          if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
            field.append('"');
            i++;
          } else {
            quoted = false;
          }
        } else {
          field.append(ch);
        }
      } else if (ch == '"' && field.length() == 0) {
        quoted = true;
      } else if (ch == ',') {
        row.add(field.toString());
        field.setLength(0);
      } else if (ch == '\n' || ch == '\r') {
        if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
          i++;
        }
        row.add(field.toString());
        field.setLength(0);
        rows.add(row);
        row = new ArrayList<>();
      } else {
        field.append(ch);
      }
    }
    if (quoted) {
      throw new IllegalArgumentException("a quoted field is never closed");
    }
    if (field.length() > 0 || !row.isEmpty()) {
      row.add(field.toString());
      rows.add(row);
    }
    return rows;
  }
}
