package database;

import model.Food;
import model.BasicFood;
import app.*;
import model.CompositeFood;
import java.util.*;
import java.io.*;

public class FoodDatabase {
    private Map<String, Food> foodMap = new HashMap<>();
    private final String filePath;
    private final char typeIndicator; // 'B' for basic, 'C' for composite

    public FoodDatabase(String filePath, char typeIndicator) {
        this.filePath = filePath;
        this.typeIndicator = typeIndicator;
    }

    public void addFood(Food food) {
        foodMap.put(food.getId(), food);
    }

    public void removeFood(String id) {
        Food food = foodMap.remove(id);
        if (food != null) {
            FoodDiaryApp.recycleId(id);
        }
    }

    public Food getFood(String id) {
        return foodMap.get(id);
    }

    public Collection<Food> getAllFoods() {
        return foodMap.values();
    }

    // Load from file; skip header lines (starting with "#" or "Date")
    public void load() {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Database file " + filePath + " not found. Starting with an empty database.");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.startsWith("Date") || line.trim().isEmpty()) {
                    continue;
                }
                int expectedParts = (typeIndicator == 'C' ? 7 : 6);
                String[] parts = line.split(";", expectedParts);
                if (parts.length < expectedParts) {
                    System.err.println("Skipping invalid line: " + line);
                    continue;
                }
                String type = parts[0];
                if (type.charAt(0) != typeIndicator) {
                    continue;
                }
                String id = parts[1];
                String name = parts[2];
                List<String> keywords = Arrays.asList(parts[3].split("\\|"));
                if (type.equals("B")) {
                    try {
                        double calories = Double.parseDouble(parts[4]);
                        String extraInfo = parts[5];
                        BasicFood bf = new BasicFood(id, name, keywords, calories,extraInfo);
                        bf.setCommitted(true);
                        foodMap.put(id, bf);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid calorie value for food " + id + " in line: " + line);
                    }
                } else if (type.equals("C")) {
                    try {
                        // double calories = Double.parseDouble(parts[4]);
                        String extraInfo = parts[6];
                        CompositeFood cf = new CompositeFood(id, name, keywords,extraInfo);
                        String componentsStr = parts[5];
                        String[] componentsArr = componentsStr.split("\\|");
                        for (String comp : componentsArr) {
                            String[] compParts = comp.split(":");
                            if (compParts.length != 2)
                                continue;
                            String compId = compParts[0];
                            try {
                                double servings = Double.parseDouble(compParts[1]);
                                Food compFood = FoodDiaryApp.getCommittedFoodById(compId);
                                if (compFood != null) {
                                    cf.addComponent(compFood, servings);
                                } else {
                                    System.err.println("Component food " + compId + " not found for composite " + id);
                                }
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid serving count for component " + comp + " in composite " + id);
                            }
                        }
                        cf.setCommitted(true);
                        cf.finalizeCalories();
                        foodMap.put(id, cf);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid calorie value for composite food " + id + " in line: " + line);
                    }
                } else {
                    System.err.println("Unknown food type: " + type);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + filePath + ": " + e.getMessage());
        }
    }

    // Save to file with header and sorted in ascending order by numeric ID.
    public void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            if (typeIndicator == 'B') {
                writer.write("# Basic Foods Database");
                writer.newLine();
                writer.write("TYPE;ID;Name;Keywords;Calories;ExtraInfo");
                writer.newLine();
            } else if (typeIndicator == 'C') {
                writer.write("# Composite Foods Database");
                writer.newLine();
                writer.write("TYPE;ID;Name;Keywords;Calories;Components;ExtraInfo");
                writer.newLine();
            }
            List<Food> foods = new ArrayList<>(foodMap.values());
            foods.sort((f1, f2) -> {
                int id1 = Integer.parseInt(f1.getId().substring(1));
                int id2 = Integer.parseInt(f2.getId().substring(1));
                return Integer.compare(id1, id2);
            });
            for (Food food : foods) {
                if (food instanceof BasicFood) {
                    BasicFood bf = (BasicFood) food;
                    String keywordsStr = String.join("|", bf.getKeywords());
                    writer.write("B;" + bf.getId() + ";" + bf.getName() + ";" + keywordsStr + ";" + bf.getCalories() + ";" + bf.getExtraInfo());
                    writer.newLine();
                    bf.setCommitted(true);
                } else if (food instanceof CompositeFood) {
                    CompositeFood cf = (CompositeFood) food;
                    String keywordsStr = String.join("|", cf.getKeywords());
                    StringBuilder compBuilder = new StringBuilder();
                    for (Map.Entry<Food, Double> entry : cf.getComponents().entrySet()) {
                        compBuilder.append(entry.getKey().getId())
                                .append(":")
                                .append(entry.getValue())
                                .append("|");
                    }
                    if (compBuilder.length() > 0)
                        compBuilder.setLength(compBuilder.length() - 1);
                    writer.write("C;" + cf.getId() + ";" + cf.getName() + ";" + keywordsStr + ";" + cf.getCalories()
                            + ";" + compBuilder.toString() + ";" + cf.getExtraInfo());  
                    writer.newLine();
                    cf.setCommitted(true);
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing file " + filePath + ": " + e.getMessage());
        }
    }
}
