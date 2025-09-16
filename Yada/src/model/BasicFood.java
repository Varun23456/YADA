package model;

import java.util.List;

public class BasicFood extends Food {
    private double calories;

    public BasicFood(String id, String name, List<String> keywords, double calories,String extraInfo) {
        super(id, name, keywords,extraInfo);
        this.calories = calories;
    }

    @Override
    public double getCalories() {
        return calories;
    }
}