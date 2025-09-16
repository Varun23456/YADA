package command;

import database.DailyLogManager;
import log.DailyLog;
import log.LogEntry;
import java.time.LocalDate;

public class DeleteLogEntryCommand extends LogCommand {
    private DailyLogManager logManager;
    private LocalDate date;
    private int index;
    private LogEntry removedEntry;

    public DeleteLogEntryCommand(DailyLogManager logManager, LocalDate date, int index) {
        this.logManager = logManager;
        this.date = date;
        this.index = index;
    }

    @Override
    public void execute() {
        try {
            removedEntry = logManager.deleteLogEntry(date, index);
            System.out.println("Deleted log entry at position " + (index + 1) + " for " + date);
            logManager.cleanupEmptyLog(date);
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Delete failed: " + e.getMessage());
        }
    }

    @Override
    public void undo() {
        DailyLog log = logManager.getLog(date);
        // If stored index is invalid, insert at the end.
        if (index < 0 || index > log.getEntries().size()) {
            log.addEntry(removedEntry);
            System.out.println("Undid deletion (appended) of log entry for " + date);
        } else {
            try {
                log.insertEntry(index, removedEntry);
                System.out.println("Undid deletion of log entry at position " + (index + 1) + " for " + date);
            } catch (IndexOutOfBoundsException e) {
                log.addEntry(removedEntry);
                System.out.println("Undid deletion (appended) of log entry for " + date);
            }
        }
    }
}