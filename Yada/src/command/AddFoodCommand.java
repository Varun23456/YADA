package command;

import database.FoodDatabase;
import model.Food;

public class AddFoodCommand implements Command {
    private FoodDatabase database;
    private Food food;

    public AddFoodCommand(FoodDatabase database, Food food) {
        this.database = database;
        this.food = food;
    }

    @Override
    public void execute() {
        database.addFood(food);
        System.out.println("Added food: " + food.getName() + " (ID: " + food.getId() + ")");
    }

    @Override
    public void undo() {
        database.removeFood(food.getId());
        System.out.println("Undid add food: " + food.getName() + " (ID: " + food.getId() + ")");
    }
}