package command;

import database.DailyLogManager;
import log.LogEntry;
import log.DailyLog;
import java.time.LocalDate;

public class AddLogEntryCommand extends LogCommand {
    private DailyLogManager logManager;
    private LocalDate date;
    private LogEntry newEntry;
    // Flag to indicate if we are updating an existing entry instead of adding new.
    private boolean isDuplicate = false;
    // Reference to the existing entry (if duplicate) and its original servings.
    private LogEntry existingEntry = null;
    private double originalServings = 0;

    public AddLogEntryCommand(DailyLogManager logManager, LocalDate date, LogEntry entry) {
        this.logManager = logManager;
        this.date = date;
        this.newEntry = entry;
    }

    @Override
    public void execute() {
        DailyLog log = logManager.getLog(date);
        // Check for duplicate entry by foodId.
        for (LogEntry entry : log.getEntries()) {
            if (entry.getFoodId().equalsIgnoreCase(newEntry.getFoodId())) {
                // Duplicate found: update the servings.
                isDuplicate = true;
                existingEntry = entry;
                originalServings = entry.getServings();
                entry.setServings(originalServings + newEntry.getServings());
                System.out.println("Updated log entry for " + date + ": " + entry.getFoodId() + " servings: " + originalServings + " -> " + entry.getServings());
                return;
            }
        }
        // If not a duplicate, add as new entry.
        log.addEntry(newEntry);
        System.out.println("Added log entry for " + date + ": " + newEntry.getFoodId() + ", servings: " + newEntry.getServings());
    }

    @Override
    public void undo() {
        DailyLog log = logManager.getLog(date);
        if (isDuplicate && existingEntry != null) {
            // Revert the update by restoring the original servings.
            existingEntry.setServings(originalServings);
            System.out.println("Undid update of log entry for " + date + ": " + existingEntry.getFoodId() + " servings restored to " + originalServings);
        } else {
            // Remove the newly added entry.
            if (log.getEntries().remove(newEntry)) {
                logManager.cleanupEmptyLog(date);
                System.out.println("Undid log entry addition for " + date + ": " + newEntry.getFoodId());
            } else {
                System.err.println("Undo failed: log entry not found.");
            }
        }
    }
}

