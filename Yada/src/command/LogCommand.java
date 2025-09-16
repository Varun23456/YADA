package command;

public abstract class LogCommand implements Command {
    protected boolean saved = false;

    public void markSaved() {
        saved = true;
    }

    public boolean isSaved() {
        return saved;
    }
}