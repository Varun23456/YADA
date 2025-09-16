package command;

import database.DailyLogManager;
import log.DailyLog;
import log.LogEntry;
import java.time.LocalDate;

public class UpdateLogEntryCommand extends LogCommand {
    private DailyLogManager logManager;
    private LocalDate date;
    private int index;
    private double newServings;
    private double oldServings;
    private boolean deleted = false;
    private LogEntry removedEntry;
    // Store reference to the target entry if update is applied
    private LogEntry targetEntry;

    public UpdateLogEntryCommand(DailyLogManager logManager, LocalDate date, int index, double newServings) {
        this.logManager = logManager;
        this.date = date;
        this.index = index;
        this.newServings = newServings;
    }

    @Override
    public void execute() {
        DailyLog log = logManager.getLog(date);
        if (index < 0 || index >= log.getEntries().size()) {
            System.err.println("Invalid log entry index: " + (index + 1));
            return;
        }
        targetEntry = log.getEntries().get(index);
        oldServings = targetEntry.getServings();
        if (newServings == 0) {
            removedEntry = logManager.deleteLogEntry(date, index);
            deleted = true;
            System.out.println("Deleted log entry at position " + (index + 1) + " for " + date + " (servings set to 0)");
        } else {
            targetEntry.setServings(newServings);
            deleted = false;
            System.out.println("Updated log entry at position " + (index + 1) + " for " + date + " from " + oldServings + " to " + newServings);
        }
    }

    @Override
    public void undo() {
        DailyLog log = logManager.getLog(date);
        if (deleted) {
            // Try to insert removed entry at stored index; if invalid, append at end.
            if (index < 0 || index > log.getEntries().size()) {
                log.addEntry(removedEntry);
                System.out.println("Undid deletion (appended) of log entry at position " + (index + 1) + " for " + date);
            } else {
                try {
                    log.insertEntry(index, removedEntry);
                    System.out.println("Undid deletion of log entry at position " + (index + 1) + " for " + date);
                } catch (IndexOutOfBoundsException e) {
                    log.addEntry(removedEntry);
                    System.out.println("Undid deletion (appended) of log entry for " + date);
                }
            }
        } else {
            // Restore old servings using the stored targetEntry reference.
            if (targetEntry != null) {
                targetEntry.setServings(oldServings);
                System.out.println("Undid update of log entry at position " + (index + 1) + " for " + date + " back to " + oldServings);
            } else {
                System.err.println("Undo failed: target log entry not found.");
            }
        }
    }
}