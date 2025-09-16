package model;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CompositeFood extends Food {
    private Map<Food, Double> components;
    private double totalCalories; // stored total calories

    public CompositeFood(String id, String name, List<String> keywords,String extraInfo) {
        super(id, name, keywords,extraInfo);
        components = new HashMap<>();
        totalCalories = 0;
    }

    // Add a component with servings
    public void addComponent(Food food, double servings) {
        components.put(food, servings);
    }

    // Once all components are added, compute and store calories
    public void finalizeCalories() {
        double total = 0;
        for (Map.Entry<Food, Double> entry : components.entrySet()) {
            total += entry.getKey().getCalories() * entry.getValue();
        }
        totalCalories = total;
    }

    public Map<Food, Double> getComponents() {
        return components;
    }

    @Override
    public double getCalories() {
        return totalCalories;
    }
}