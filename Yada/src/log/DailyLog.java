package log;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public class DailyLog {
    private LocalDate date;
    private List<LogEntry> entries;

    public DailyLog(LocalDate date) {
        this.date = date;
        this.entries = new ArrayList<>();
    }

    public LocalDate getDate() {
        return date;
    }

    public List<LogEntry> getEntries() {
        return entries;
    }

    public void addEntry(LogEntry entry) {
        entries.add(entry);
    }

    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
        } else {
            throw new IndexOutOfBoundsException("Invalid log entry index: " + index);
        }
    }

    public void insertEntry(int index, LogEntry entry) {
        if (index < 0 || index > entries.size())
            throw new IndexOutOfBoundsException("Invalid index to insert: " + index);
        entries.add(index, entry);
    }
}