package log;

public class LogEntry {
    private String foodId;
    private double servings;

    public LogEntry(String foodId, double servings) {
        this.foodId = foodId;
        this.servings = servings;
    }

    public String getFoodId() {
        return foodId;
    }

    public double getServings() {
        return servings;
    }

    public void setServings(double servings) {
        this.servings = servings;
    }
}