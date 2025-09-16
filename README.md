# Food Diary App

**YADA: Yet Another Diet Assistant** is a Java-based command-line application for tracking daily food intake, managing custom food items, and monitoring personalized calorie goals.

---

## Setup

1. **Unzip** and go into a FoodDairyApp directory.
   ```bash
   cd FoodDiaryApp
   ```

## Running the App

```bash
make
```

Upon launch, you’ll see the main menu:

```
==== Food Diary Main Menu ====
1. Add Food Item
2. Add Log Entry
3. Delete Log Entry
4. List Food Items
5. View Log Entries
6. Undo Last Command
7. Save Data
8. Exit
9. Help
10. Manage Log for a Specific Date
11. Update/Set Daily Profile
12. Change Calorie Calculation Method
13. View Calorie Summary for a Date
14. View Daily Profile
```

## Exercising Features

1. **Add Food Item** (Option 1)
   - Add new basic or composite foods.
   - For composites, combine existing items.

2. **Add Log Entry** (Option 2)
   - Log servings for today or specify a date.
   - Duplicate entries merge automatically.

3. **Manage & Delete Entries** (Options 3 & 10)
   - Delete or update entries for a specific date.

4. **List Foods** (Option 4)
   - Verify seeded items from `simpleFoods.txt`.

5. **View Log Entries** (Option 5)
   - See all logs or filter by date.

6. **Undo Last Command** (Option 6)
   - Revert the last saved log action (add/update/delete).

7. **Save Data** (Option 7)
   - Persist foods, logs, and profiles to files.

8. **Profiles & Targets** (Options 11–14)
   - Create/update daily profiles (gender, age, height, weight, activity).
   - Switch between Harris–Benedict and Mifflin–St Jeor (Option 12).
   - View calorie summary against target (Option 13).

9. **Exit** (Option 8)
   - Saves all data before quitting.

---

## File Formats

- **simpleFoods.txt** (`TYPE;ID;Name;Keywords;Calories;ExtraInfo`)
- **complexFoods.txt** (`TYPE;ID;Name;Keywords;Calories;Components;ExtraInfo`)
- **dailylogs.txt** (`Date: YYYY-MM-DD` then numbered entries)
- **profile.txt** (`CALC_METHOD:METHOD` then `date;gender;height;age;weight;activity`)

---

