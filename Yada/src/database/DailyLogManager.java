package database;

import log.DailyLog;
import log.LogEntry;
import app.*;
import model.*; // Ensure this matches the actual package of the Food class
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;

public class DailyLogManager {
    private Map<LocalDate, DailyLog> logs;
    private final String filePath;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public DailyLogManager(String filePath) {
        this.filePath = filePath;
        logs = new HashMap<>();
    }

    public DailyLog getLog(LocalDate date) {
        return logs.computeIfAbsent(date, d -> new DailyLog(d));
    }

    public void addLogEntry(LocalDate date, LogEntry entry) {
        getLog(date).addEntry(entry);
    }

    public LogEntry deleteLogEntry(LocalDate date, int entryIndex) {
        DailyLog log = getLog(date);
        if (entryIndex < 0 || entryIndex >= log.getEntries().size()) {
            throw new IndexOutOfBoundsException("Invalid log entry index: " + entryIndex);
        }
        LogEntry removed = log.getEntries().get(entryIndex);
        log.removeEntry(entryIndex);
        return removed;
    }

    public void cleanupEmptyLog(LocalDate date) {
        DailyLog log = logs.get(date);
        if (log != null && log.getEntries().isEmpty()) {
            logs.remove(date);
        }
    }

    public void load() {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Daily log file " + filePath + " not found. Starting with empty logs.");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            LocalDate currentDate = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                if (line.startsWith("Date:")) {
                    String dateStr = line.substring(5).trim();
                    try {
                        currentDate = LocalDate.parse(dateStr, dtf);
                    } catch (Exception e) {
                        System.err.println("Invalid date format in log file: " + line);
                        currentDate = null;
                    }
                } else if (currentDate != null) {
                    try {
                        String[] parts = line.split(",");
                        if (parts.length < 3)
                            continue;
                        String foodIdPart = parts[0].trim();
                        String servingsPart = parts[2].trim();
                        int idx = foodIdPart.indexOf("FoodID:");
                        if (idx == -1)
                            continue;
                        String foodId = foodIdPart.substring(idx + 7).trim();
                        int sIdx = servingsPart.indexOf("Servings:");
                        if (sIdx == -1)
                            continue;
                        String servingsStr = servingsPart.substring(sIdx + 9).trim();
                        double servings = Double.parseDouble(servingsStr);
                        DailyLog log = logs.computeIfAbsent(currentDate, d -> new DailyLog(d));
                        log.addEntry(new LogEntry(foodId, servings));
                    } catch (Exception e) {
                        System.err.println("Error parsing log entry: " + line + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading daily log file: " + e.getMessage());
        }
    }

    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("# Daily Log Database");
            writer.newLine();
            List<LocalDate> dates = new ArrayList<>(logs.keySet());
            dates.sort(Comparator.naturalOrder());
            for (LocalDate date : dates) {
                DailyLog log = logs.get(date);
                List<LogEntry> filtered = new ArrayList<>();
                for (LogEntry entry : log.getEntries()) {
                    if (FoodDiaryApp.getCommittedFoodById(entry.getFoodId()) != null) {
                        filtered.add(entry);
                    }
                }
                if (filtered.isEmpty())
                    continue;
                writer.write("Date: " + date.format(dtf));
                writer.newLine();
                int count = 1;
                for (LogEntry entry : filtered) {
                    Food food = FoodDiaryApp.getFoodById(entry.getFoodId());
                    String foodName = (food != null) ? food.getName() : "Unknown";
                    writer.write(count + ") FoodID: " + entry.getFoodId() + ", Name: " + foodName + ", Servings: " + entry.getServings());
                    writer.newLine();
                    count++;
                }
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing daily log file: " + e.getMessage());
        }
    }

    public Map<LocalDate, DailyLog> getAllLogs() {
        return logs;
    }
}
