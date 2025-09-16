package app;

import model.*;
import database.*;
import log.*;
import command.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.io.*;
// import util.IDGenerator;

public class FoodDiaryApp {
    private static FoodDatabase basicFoodDatabase;
    private static FoodDatabase compositeFoodDatabase;
    private static DailyLogManager dailyLogManager;
    // Two separate stacks for log command undo functionality:
    // unsavedLogCommands holds commands that have not yet been saved.
    // savedLogCommands holds commands that have been saved and are available for undo.
    private static Deque<LogCommand> unsavedLogCommands = new ArrayDeque<>();
    private static Deque<LogCommand> savedLogCommands = new ArrayDeque<>();
    private static TreeSet<Integer> availableBasicIDs = new TreeSet<>();
    private static TreeSet<Integer> availableCompositeIDs = new TreeSet<>();
    private static int nextBasicID = 1;
    private static int nextCompositeID = 1;
    // Global variables for profile data
    private static Map<LocalDate, DailyProfile> profileMap = new HashMap<>();
    // Calculation method loaded from file header; default is Harris-Benedict.
    private static String calcMethodFromFile = "HARRIS_BENEDICT";

    public static void main(String[] args) {
        String basicFoodFile = "./data/simpleFoods.txt";
        String compositeFoodFile = "./data/complexFoods.txt";
        String dailyLogFile = "./data/dailylogs.txt";

        basicFoodDatabase = new FoodDatabase(basicFoodFile, 'B');
        compositeFoodDatabase = new FoodDatabase(compositeFoodFile, 'C');
        basicFoodDatabase.load();
        compositeFoodDatabase.load();
        updateFoodIdCounters();

        // Load unsaved logs into memory for modification purposes (writes are done to file only when saved)
        dailyLogManager = new DailyLogManager(dailyLogFile);
        dailyLogManager.load();
        loadProfiles();

        runCLI();

        basicFoodDatabase.save();
        compositeFoodDatabase.save();
        dailyLogManager.save();
        saveProfiles();
        System.out.println("Exiting Food Diary App. Changes have been saved.");
    }

    private static void updateFoodIdCounters() {
        for (Food food : basicFoodDatabase.getAllFoods()) {
            try {
                int num = Integer.parseInt(food.getId().substring(1));
                if (num >= nextBasicID) {
                    nextBasicID = num + 1;
                }
            } catch (NumberFormatException e) {
            }
        }
        for (Food food : compositeFoodDatabase.getAllFoods()) {
            try {
                int num = Integer.parseInt(food.getId().substring(1));
                if (num >= nextCompositeID) {
                    nextCompositeID = num + 1;
                }
            } catch (NumberFormatException e) {
            }
        }
    }

    public static Food getCommittedFoodById(String id) {
        Food f = basicFoodDatabase.getFood(id);
        if (f != null && f.isCommitted())
            return f;
        f = compositeFoodDatabase.getFood(id);
        if (f != null && f.isCommitted())
            return f;
        return null;
    }

    public static Food getFoodById(String id) {
        Food f = basicFoodDatabase.getFood(id);
        if (f == null) {
            f = compositeFoodDatabase.getFood(id);
        }
        return f;
    }

    public static Food getFoodByName(String name) {
        for (Food food : basicFoodDatabase.getAllFoods()) {
            if (food.getName().equalsIgnoreCase(name))
                return food;
        }
        for (Food food : compositeFoodDatabase.getAllFoods()) {
            if (food.getName().equalsIgnoreCase(name))
                return food;
        }
        return null;
    }

    private static String generateBasicFoodId() {
        int idNum;
        if (!availableBasicIDs.isEmpty()) {
            idNum = availableBasicIDs.first();
            availableBasicIDs.remove(idNum);
        } else {
            idNum = nextBasicID++;
        }
        return "B" + idNum;
    }

    private static String generateCompositeFoodId() {
        int idNum;
        if (!availableCompositeIDs.isEmpty()) {
            idNum = availableCompositeIDs.first();
            availableCompositeIDs.remove(idNum);
        } else {
            idNum = nextCompositeID++;
        }
        return "C" + idNum;
    }

    public static void recycleId(String id) {
        if (id.startsWith("B")) {
            try {
                int num = Integer.parseInt(id.substring(1));
                availableBasicIDs.add(num);
            } catch (NumberFormatException e) {
            }
        } else if (id.startsWith("C")) {
            try {
                int num = Integer.parseInt(id.substring(1));
                availableCompositeIDs.add(num);
            } catch (NumberFormatException e) {
            }
        }
    }

    // Modified CLI for viewing logs.
    // This version always reads the saved logs from the dailylogs.txt file so that only committed logs are shown.
    private static void viewLogsCLI(Scanner scanner) {
        DailyLogManager savedLogManager = new DailyLogManager("./data/dailylogs.txt");
        savedLogManager.load();
        System.out.println("View logs options:");
        System.out.println("1. View all logs");
        System.out.println("2. View logs for a specific date");
        System.out.print("Enter option: ");
        String option = scanner.nextLine().trim();
        if (option.equals("1")) {
            System.out.println("Daily logs (from saved file):");
            boolean found = false;
            for (LocalDate d : savedLogManager.getAllLogs().keySet()) {
                DailyLog dl = savedLogManager.getLog(d);
                if (!dl.getEntries().isEmpty()) {
                    System.out.println("Date: " + d);
                    int count = 1;
                    //should modify endong |
                for (LogEntry le : dl.getEntries()) {
                    Food f = getFoodById(le.getFoodId());
                    String info = "";
                    if (f != null && f.getExtraInfo() != null && !f.getExtraInfo().isEmpty()) {
                        info = " | ExtraInfo: " + f.getExtraInfo();
                    }
                    if(info != null && info.length() > 2)
                    {
                        info = info.substring(0,info.length()-2);
                    }
                    System.out.printf("  %d: %s, servings: %.2f%s\n",
                        count, le.getFoodId(), le.getServings(), info);
                    count++;
                }
                    found = true;
                }
            }
            if (!found)
                System.out.println("No log entries found.");
        } else if (option.equals("2")) {
            System.out.print("Enter date (YYYY-MM-DD): ");
            LocalDate date = null;
            try {
                date = LocalDate.parse(scanner.nextLine().trim());
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format.");
                return;
            }
            DailyLog dl = savedLogManager.getAllLogs().get(date);
            if (dl == null || dl.getEntries().isEmpty()) {
                System.out.println("No log entries for " + date);
            } else {
                System.out.println("Log entries for " + date + ":");
                int count = 0;
                for (LogEntry le : dl.getEntries()) {
                    Food f = getFoodById(le.getFoodId());
                    String info = "";
                    if (f != null && f.getExtraInfo() != null && !f.getExtraInfo().isEmpty()) {
                        info = " | ExtraInfo: " + f.getExtraInfo();
                    }
                    if(info != null && info.length() > 2)
                    {
                        info = info.substring(0,info.length()-2);
                    }
                    System.out.printf("  %d: %s, servings: %.2f%s\n",
                        count, le.getFoodId(), le.getServings(), info);
                    count++;
                }
            }
        } else {
            System.out.println("Invalid option. Returning to main menu.");
        }
    }

    // Modified manageLogForDateCLI: always loads logs from the saved dailylogs.txt file.
    private static void manageLogForDateCLI(Scanner scanner) {
        System.out.print("Enter date to manage log (YYYY-MM-DD): ");
        LocalDate date;
        try {
            date = LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format. Returning to main menu.");
            return;
        }
        DailyLogManager savedLogManager = new DailyLogManager("./data/dailylogs.txt");
        savedLogManager.load();
        DailyLog log = savedLogManager.getLog(date);
        
        if (log.getEntries().isEmpty()) {
            System.out.println("No saved log entries for " + date);
            return;
        }
        
        System.out.println("Log entries for " + date + ":");
        List<Integer> validIndices = new ArrayList<>();
        for (int i = 0; i < log.getEntries().size(); i++) {
            if (getCommittedFoodById(log.getEntries().get(i).getFoodId()) != null) {
                validIndices.add(i);
            }
        }
        
        if (validIndices.isEmpty()) {
            System.out.println("No valid log entries for " + date);
            return;
        }
        
        for (int j = 0; j < validIndices.size(); j++) {
            int origIdx = validIndices.get(j);
            LogEntry le = log.getEntries().get(origIdx);
            Food f = getFoodById(le.getFoodId());
            String info = (f != null && f.getExtraInfo() != null && !f.getExtraInfo().isEmpty())
                        ? " | ExtraInfo: " + f.getExtraInfo()
                        : "";
            if(info != null && info.length() > 2)
            {
                info = info.substring(0,info.length()-2);
            }
            System.out.printf("%d: %s, servings: %.2f%s\n",
                j+1, le.getFoodId(), le.getServings(), info);
        }

        
        System.out.print("Would you like to update an entry? (y/n): ");
        String choice = scanner.nextLine().trim();
        if (choice.equalsIgnoreCase("y")) {
            System.out.print("Enter the 1-indexed position of the entry to update: ");
            int userIndex = Integer.parseInt(scanner.nextLine().trim());
            if (userIndex < 1 || userIndex > validIndices.size()) {
                System.out.println("Invalid index. Returning to main menu.");
                return;
            }
            int origIdx = validIndices.get(userIndex - 1);
            System.out.print("Enter new number of servings (enter 0 to delete the entry): ");
            double newServings = Double.parseDouble(scanner.nextLine().trim());
            
            // Note: Update on saved logs will be applied to the main dailyLogManager
            LogCommand updateCmd = new UpdateLogEntryCommand(dailyLogManager, date, origIdx, newServings);
            updateCmd.execute();
            unsavedLogCommands.addLast(updateCmd);
            System.out.println("Changes updated in memory. Use the 'Save Data' option from the main menu to commit these changes to dailylogs.txt.");
        }
    }
    
    // Modified deleteLogEntryCLI: displays only saved log entries from dailylogs.txt.
    private static void deleteLogEntryCLI(Scanner scanner) {
        System.out.print("Enter date (YYYY-MM-DD): ");
        LocalDate date;
        try {
            date = LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format. Returning to main menu.");
            return;
        }
        DailyLogManager savedLogManager = new DailyLogManager("./data/dailylogs.txt");
        savedLogManager.load();
        DailyLog log = savedLogManager.getLog(date);
        if (log == null) {
            System.out.println("No saved log entries for " + date);
            return;
        }
        
        List<Integer> validIndices = new ArrayList<>();
        // Loop through saved log entries only
        for (int i = 0; i < log.getEntries().size(); i++) {
            if (getCommittedFoodById(log.getEntries().get(i).getFoodId()) != null) {
                validIndices.add(i);
            }
        }
        if (validIndices.isEmpty()) {
            System.out.println("No valid log entries for " + date);
            return;
        }
        
        System.out.println("Valid log entries for " + date + ":");
        // Print using 1-indexing based on validIndices list
        // Example snippet in manageLogForDateCLI:
        for (int j = 0; j < validIndices.size(); j++) {
            int origIdx = validIndices.get(j);
            LogEntry le = log.getEntries().get(origIdx);
            Food f = getFoodById(le.getFoodId());
            String info = (f != null && f.getExtraInfo() != null && !f.getExtraInfo().isEmpty())
                        ? " | ExtraInfo: " + f.getExtraInfo()
                        : "";
            if(info != null && info.length() > 2)
            {
                info = info.substring(0,info.length()-2);
            }
            System.out.printf("%d: %s, servings: %.2f%s\n",
                j+1, le.getFoodId(), le.getServings(), info);
        }
        
        
        System.out.print("Enter the 1-indexed position to delete: ");
        try {
            int userIndex = Integer.parseInt(scanner.nextLine().trim());
            if (userIndex < 1 || userIndex > validIndices.size()) {
                System.out.println("Invalid index input. Returning to main menu.");
                return;
            }
            int originalIndex = validIndices.get(userIndex - 1);
            // Note: For deletion we use the main dailyLogManager
            LogCommand deleteCmd = new DeleteLogEntryCommand(dailyLogManager, date, originalIndex);
            deleteCmd.execute();
            unsavedLogCommands.addLast(deleteCmd);
        } catch (NumberFormatException e) {
            System.out.println("Invalid index input. Returning to main menu.");
        }
    }
    
    private static void undoLastCommand() {
        if (savedLogCommands.isEmpty()) {
            System.out.println("No saved commands to undo.");
            return;
        }
        LogCommand last = savedLogCommands.pop();
        last.undo();
    }
    private static void runCLI() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {
            printMainMenu();
            System.out.print("Enter option number: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty())
                continue;
            try {
                int option = Integer.parseInt(input);
                switch (option) {
                    case 1:
                        addFoodCLI(scanner);
                        break;
                    case 2:
                        addLogEntryCLI(scanner);
                        break;
                    case 3:
                        deleteLogEntryCLI(scanner);
                        break;
                    case 4:
                        listFoodsCLI();
                        break;
                    case 5:
                        viewLogsCLI(scanner);
                        break;
                    case 6:
                        undoLastCommand();
                        break;
                    case 7:
                        // Save data option: save all databases and logs,
                        // then move unsaved log commands to the saved stack in FIFO order.
                        basicFoodDatabase.save();
                        compositeFoodDatabase.save();
                        dailyLogManager.save();
                        saveProfiles();
                        while (!unsavedLogCommands.isEmpty()) {
                            LogCommand cmd = unsavedLogCommands.pollFirst();
                            cmd.markSaved();
                            savedLogCommands.push(cmd);
                        }
                        System.out.println("Databases and logs saved.");
                        break;
                    case 8:
                        running = false;
                        break;
                    case 9:
                        printHelp();
                        break;
                    case 10:
                        manageLogForDateCLI(scanner);
                        break;
                    case 11:
                        updateDailyProfileCLI(scanner);
                        break;
                    case 12:
                        changeCalcMethodCLI(scanner);
                        break;
                    case 13:
                        viewCalorieSummaryCLI(scanner);
                        break;
                    case 14:
                        viewDailyProfileCLI(scanner);
                        break;
                    default:
                        System.out.println("Invalid option number. Please try again.");
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
        scanner.close();
    }

    private static void printMainMenu() {
        System.out.println("\n==== Food Diary Main Menu ====");
        System.out.println("1. Add Food Item");
        System.out.println("2. Add Log Entry");
        System.out.println("3. Delete Log Entry");
        System.out.println("4. List Food Items");
        System.out.println("5. View Log Entries");
        System.out.println("6. Undo Last Command");
        System.out.println("7. Save Data");
        System.out.println("8. Exit");
        System.out.println("9. Help");
        System.out.println("10. Manage Log for a Specific Date");
        System.out.println("11. Update/Set Daily Profile");
        System.out.println("12. Change Calorie Calculation Method");
        System.out.println("13. View Calorie Summary for a Date");
        System.out.println("14. View Daily Profile");
    }

    private static void printHelp() {
        System.out.println("This Food Diary App supports the following features:");
        System.out.println(" - Adding food items (basic/simple or composite/complex).");
        System.out.println(" - Adding daily log entries (the current date is used automatically).");
        System.out.println("   * When adding a log entry for the same food on the same date, the servings are summed rather than duplicating the entry.");
        System.out.println(" - Deleting log entries and managing logs for a specific date (only saved logs are shown).");
        System.out.println(" - All log entries are shown with 1-indexing.");
        System.out.println(" - Undoing the last log command (only for saved commands).");
        System.out.println(" - Saving data to files (foods and logs are stored in sorted, readable formats).");
        System.out.println(" - Updating/setting daily user profile, changing calorie calculation method, and viewing summaries.");
    }

    private static void listFoodsCLI() {
        System.out.println("Listing all committed foods:");
        System.out.println("Basic Foods:");
        for (Food food : basicFoodDatabase.getAllFoods()) {
            if (food.isCommitted()) {
                System.out.printf("ID: %s | Name: %s | Keywords: %s | Calories: %.2f",
                    food.getId(),
                    food.getName(),
                    String.join(",", food.getKeywords()),
                    food.getCalories());
                if (food.getExtraInfo() != null && !food.getExtraInfo().isEmpty()) {
                    System.out.printf(" | ExtraInfo: %s", food.getExtraInfo());
                }
                System.out.println();
            }
        }
        System.out.println("Composite Foods:");
        for (Food food : compositeFoodDatabase.getAllFoods()) {
            if (food.isCommitted()) {
                System.out.printf("ID: %s | Name: %s | Keywords: %s | Calories: %.2f",
                    food.getId(),
                    food.getName(),
                    String.join(",", food.getKeywords()),
                    food.getCalories());
                if (food.getExtraInfo() != null && !food.getExtraInfo().isEmpty()) {
                    System.out.printf(" | ExtraInfo: %s", food.getExtraInfo());
                }
                System.out.println();
            }
        }
    }
    

    private static void addFoodCLI(Scanner scanner) {
        System.out.println("Select type of food to add:");
        System.out.println("1. Basic Food");
        System.out.println("2. Composite Food");
        System.out.print("Enter option: ");
        String choice = scanner.nextLine().trim();
        if (choice.equals("1")) {
            System.out.print("Enter food name: ");
            String name = scanner.nextLine().trim();
            if (getFoodByName(name) != null) {
                System.out.println("Error: A food with that name already exists.");
                return;
            }
            String id = generateBasicFoodId();
            System.out.println("Assigned Food ID: " + id);
            System.out.print("Enter keywords (separated by |): ");
            String keywordsStr = scanner.nextLine().trim();
            List<String> keywords = Arrays.asList(keywordsStr.split("\\|"));
            System.out.print("Enter calories per serving: ");
            double calories = Double.parseDouble(scanner.nextLine().trim());
            // --------- EXTRA INFO BLOCK ---------
            String extraInfo = "";
            while(true) {
            System.out.print("Do you want to add extra info/notes? (y/n): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
               
                    String entry = "";

                    System.out.print("  Field name : ");
                    String field = scanner.nextLine().trim();
                    if (field.isEmpty()) {
                        System.out.println("    Field name cannot be empty. Try again.");
                        continue;
                    }

                    System.out.print("  Value: ");
                    String value = scanner.nextLine().trim();
                    if (value.isEmpty()) {
                        System.out.println("    Value cannot be empty. Entry discarded.");
                        continue;
                    }

                    System.out.print("  Unit: ");
                    String unit = scanner.nextLine().trim();
                    if (unit.isEmpty()) {
                        System.out.println("    Unit cannot be empty. Entry discarded.");
                        continue;
                    }

                    // Build "field=value:unit"
                    entry = field + "=" + value + ":" + unit + " | ";    
                
                // Join with '|'
                extraInfo += entry;
            }
            else{
                int extraInfoLength = extraInfo.length();
                if (extraInfoLength > 1) {
                    // Remove the last '|'
                    extraInfo = extraInfo.substring(0, extraInfoLength - 2);
                }
                break;
            }
        }

            // ------------------------------------

            BasicFood bf = new BasicFood(id, name, keywords, calories,extraInfo);
            Command addFoodCmd = new AddFoodCommand(basicFoodDatabase, bf);
            addFoodCmd.execute();
        } else if (choice.equals("2")) {
            System.out.print("Enter food name: ");
            String name = scanner.nextLine().trim();
            if (getFoodByName(name) != null) {
                System.out.println("Error: A food with that name already exists.");
                return;
            }
            String id = generateCompositeFoodId();
            System.out.println("Assigned Food ID: " + id);
        
            System.out.print("Enter keywords (separated by |): ");
            List<String> keywords = Arrays.asList(scanner.nextLine().trim().split("\\|"));
        
            // Temporarily construct with empty extraInfo; we'll set it after aggregation
            CompositeFood cf = new CompositeFood(id, name, keywords, "");
        
            System.out.println("Available Foods for Components (committed items only):");
            listFoodsCLI();
        
            System.out.println("Enter components for the composite food. Type the component food ID and servings. When finished, type 'done'.");
            while (true) {
                System.out.print("Enter component food ID (or 'done'): ");
                String compId = scanner.nextLine().trim();
                if (compId.equalsIgnoreCase("done")) break;
        
                Food compFood = getCommittedFoodById(compId);
                if (compFood == null) {
                    System.out.println("Component food not found or not yet saved: " + compId);
                    continue;
                }
                System.out.print("Enter servings for component: ");
                double servings = Double.parseDouble(scanner.nextLine().trim());
                cf.addComponent(compFood, servings);
            }
        
            // Compute total calories
            cf.finalizeCalories();
        
            // --- AGGREGATE EXTRA INFO ---
            // Map of "field|unit" -> totalValue
            Map<String, Double> agg = new HashMap<>();
            for (Map.Entry<Food, Double> entry : cf.getComponents().entrySet()) {
                Food comp = entry.getKey();
                double servings = entry.getValue();
        
                String info = comp.getExtraInfo();
                if (info == null || info.isEmpty()) continue;
        
                // each compInfo is "f1=v1:u1|f2=v2:u2"
                for (String part : info.split("\\|")) {
                    String[] fv = part.split("=");
                    if (fv.length != 2) continue;
                    String field = fv[0].trim();
                    String[] vu = fv[1].split(":");
                    if (vu.length != 2) continue;
                    try {
                        double val = Double.parseDouble(vu[0].trim()) * servings;
                        String unit = vu[1].trim();
                        String key = field + "|" + unit;
                        agg.put(key, agg.getOrDefault(key, 0.0) + val);
                    } catch (NumberFormatException e) {
                        // skip invalid numbers
                    }
                }
            }
        
            // Build the composite extraInfo string
            StringBuilder extraInfoBuilder = new StringBuilder();
            for (Map.Entry<String, Double> e : agg.entrySet()) {
                String[] fu = e.getKey().split("\\|");
                String field = fu[0], unit = fu[1];
                // strip any trailing .0
                String value = String.valueOf(e.getValue()).replaceAll("\\.0+$", "");
                if (extraInfoBuilder.length() > 0) extraInfoBuilder.append("|");
                extraInfoBuilder.append(field)
                                .append("=")
                                .append(value)
                                .append(":")
                                .append(unit);
            }
            String compositeExtraInfo = extraInfoBuilder.toString();
        
            // Now set it on the composite and save
            cf.setExtraInfo(compositeExtraInfo);
            Command addFoodCmd = new AddFoodCommand(compositeFoodDatabase, cf);
            addFoodCmd.execute();
        } else {
            System.out.println("Invalid selection. Returning to main menu.");
        }
    }

    private static void addLogEntryCLI(Scanner scanner) {
        LocalDate date;
        System.out.print("Enter date for log entry (YYYY-MM-DD) or press Enter for today: ");
        String dateInput = scanner.nextLine().trim();
        if (dateInput.isEmpty()) {
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(dateInput);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Using today's date.");
                date = LocalDate.now();
            }
        }
        Food selectedFood = null;
        System.out.println("Select food entry method:");
        System.out.println("1. Choose from full list");
        System.out.println("2. Search by keywords");
        System.out.print("Enter option: ");
        String method = scanner.nextLine().trim();
        if (method.equals("1")) {
            listFoodsCLI();
            System.out.print("Enter food ID from the above list: ");
            String foodId = scanner.nextLine().trim();
            selectedFood = getCommittedFoodById(foodId);
        } else if (method.equals("2")) {
            listFoodsCLI();
            System.out.print("Enter keywords (separated by space): ");
            String keywordLine = scanner.nextLine().trim();
            String[] searchKeywords = keywordLine.split("\\s+");
            System.out.println("Match options:");
            System.out.println("1. Match ANY keyword");
            System.out.println("2. Match ALL keywords");
            System.out.print("Enter option: ");
            String matchOption = scanner.nextLine().trim();
            List<Food> matchedFoods = searchFoodsByKeywords(searchKeywords, matchOption.equals("2"));
            if (matchedFoods.isEmpty()) {
                System.out.println("No foods matched the given keywords.");
                return;
            } else {
                System.out.println("Matched foods:");
                for (Food f : matchedFoods) {
                    System.out.println("ID: " + f.getId() + " | Name: " + f.getName() + " | Keywords: " + f.getKeywords() + " | Calories: " + f.getCalories() + " | ExtraInfo: " + f.getExtraInfo());
                }
                System.out.print("Enter food ID from the matched list: ");
                String foodId = scanner.nextLine().trim();
                selectedFood = getCommittedFoodById(foodId);
            }
        } else {
            System.out.println("Invalid selection method. Returning to main menu.");
            return;
        }
        if (selectedFood == null) {
            System.out.println("Food not found in database (must be saved).");
            return;
        }
        System.out.print("Enter number of servings: ");
        double servings = Double.parseDouble(scanner.nextLine().trim());
        
        // Check if an entry for the same food already exists on the given date.
        DailyLog log = dailyLogManager.getLog(date);
        int duplicateIndex = -1;
        for (int i = 0; i < log.getEntries().size(); i++) {
            if (log.getEntries().get(i).getFoodId().equals(selectedFood.getId())) {
                duplicateIndex = i;
                break;
            }
        }
        
        if (duplicateIndex != -1) {
            // Duplicate found: update the existing log entry's servings.
            LogEntry existingEntry = log.getEntries().get(duplicateIndex);
            double updatedServings = existingEntry.getServings() + servings;
            LogCommand updateCmd = new UpdateLogEntryCommand(dailyLogManager, date, duplicateIndex, updatedServings);
            updateCmd.execute();
            unsavedLogCommands.addLast(updateCmd);
            System.out.println("Duplicate entry found. Updated servings from " + existingEntry.getServings() + " to " + updatedServings);
        } else {
            // No duplicate: add a new log entry.
            LogEntry entry = new LogEntry(selectedFood.getId(), servings);
            LogCommand addLogCmd = new AddLogEntryCommand(dailyLogManager, date, entry);
            addLogCmd.execute();
            unsavedLogCommands.addLast(addLogCmd);
        }
    }

    private static List<Food> searchFoodsByKeywords(String[] keywords, boolean matchAll) {
        List<Food> results = new ArrayList<>();
        for (Food f : basicFoodDatabase.getAllFoods()) {
            if (f.isCommitted() && matchesKeywords(f, keywords, matchAll))
                results.add(f);
        }
        for (Food f : compositeFoodDatabase.getAllFoods()) {
            if (f.isCommitted() && matchesKeywords(f, keywords, matchAll))
                results.add(f);
        }
        return results;
    }

    private static boolean matchesKeywords(Food food, String[] keywords, boolean matchAll) {
        List<String> foodKeywords = food.getKeywords();
        if (matchAll) {
            for (String key : keywords) {
                boolean found = false;
                String trimmedKey = key.trim();
                for (String fKey : foodKeywords) {
                    if (fKey.equalsIgnoreCase(trimmedKey)) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return false;
            }
            return true;
        } else {
            for (String key : keywords) {
                String trimmedKey = key.trim();
                for (String fKey : foodKeywords) {
                    if (fKey.equalsIgnoreCase(trimmedKey))
                        return true;
                }
            }
            return false;
        }
    }

    // Load profiles and the calculation method from profile.txt (called once at startup)
    private static void loadProfiles() {
        profileMap.clear();
        File file = new File("./data/profile.txt");
        if (!file.exists()) {
            System.out.println("Profile file not found. Starting with no saved profiles and default calculation method.");
            calcMethodFromFile = "HARRIS_BENEDICT";
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean headerRead = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                if (!headerRead && line.startsWith("CALC_METHOD:")) {
                    calcMethodFromFile = line.substring("CALC_METHOD:".length()).trim();
                    headerRead = true;
                } else {
                    // Expected format: date;gender;height;age;weight;activityLevel
                    String[] parts = line.split(";");
                    if (parts.length < 6)
                        continue;
                    LocalDate date = LocalDate.parse(parts[0].trim());
                    String gender = parts[1].trim();
                    double height = Double.parseDouble(parts[2].trim());
                    int age = Integer.parseInt(parts[3].trim());
                    double weight = Double.parseDouble(parts[4].trim());
                    String activityLevel = parts[5].trim();
                    DailyProfile profile = new DailyProfile(date, gender, height, age, weight, activityLevel);
                    profileMap.put(date, profile);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading profiles: " + e.getMessage());
        }
    }

    // Save the global profileMap and calcMethodFromFile to profile.txt
    private static void saveProfiles() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./data/profile.txt"))) {
            // Write header for calculation method
            writer.write("CALC_METHOD:" + calcMethodFromFile);
            writer.newLine();
            writer.write("# Date;Gender;Height;Age;Weight;ActivityLevel");
            writer.newLine();
            List<LocalDate> dates = new ArrayList<>(profileMap.keySet());
            Collections.sort(dates);
            for (LocalDate d : dates) {
                DailyProfile p = profileMap.get(d);
                writer.write(d.toString() + ";" + p.getGender() + ";" + p.getHeight() + ";" + p.getAge() + ";" + p.getWeight() + ";" + p.getActivityLevel());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving profiles: " + e.getMessage());
        }
    }
    

    private static void updateDailyProfileCLI(Scanner scanner) {
        // Do not reload profiles here; we use the global cache loaded at startup.
        System.out.print("Enter date for profile update (YYYY-MM-DD) or press ENTER for today: ");
        String dateInput = scanner.nextLine().trim();
        LocalDate date = dateInput.isEmpty() ? LocalDate.now() : null;
        if (date == null) {
            try {
                date = LocalDate.parse(dateInput);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format.");
                return;
            }
        }
        
        DailyProfile profile = profileMap.get(date);
        if (profile == null) {
            // Try to get the most recent profile before this date
            profile = getMostRecentProfile(date);
            if (profile != null) {
                // Create a new profile for this date, copying previous details
                profile = new DailyProfile(date, profile.getGender(), profile.getHeight(), profile.getAge(), profile.getWeight(), profile.getActivityLevel());
            }
        }
        if (profile == null) {
            // No previous data; use defaults (empty gender, zeros, etc.)
            profile = new DailyProfile(date, "", 0, 0, 0, "");
        }
        
        System.out.println("Current profile for " + date + ": " + profile);
        System.out.print("Enter gender (M/F) [current: " + profile.getGender() + "]: ");
        String gender = scanner.nextLine().trim();
        if (!gender.isEmpty())
            profile.setGender(gender.toUpperCase());
        System.out.print("Enter height in cm [current: " + profile.getHeight() + "]: ");
        String heightStr = scanner.nextLine().trim();
        if (!heightStr.isEmpty())
            profile.setHeight(Double.parseDouble(heightStr));
        System.out.print("Enter age in years [current: " + profile.getAge() + "]: ");
        String ageStr = scanner.nextLine().trim();
        if (!ageStr.isEmpty())
            profile.setAge(Integer.parseInt(ageStr));
        System.out.print("Enter weight in kg [current: " + profile.getWeight() + "]: ");
        String weightStr = scanner.nextLine().trim();
        if (!weightStr.isEmpty())
            profile.setWeight(Double.parseDouble(weightStr));
        System.out.print("Enter activity level (sedentary, lightly active, moderately active, very active, extra active) [current: " + profile.getActivityLevel() + "]: ");
        String actLevel = scanner.nextLine().trim();
        if (!actLevel.isEmpty())
            profile.setActivityLevel(actLevel.toLowerCase());
        
        // Update the global cache.
        profileMap.put(date, profile);
        System.out.println("Profile updated for " + date + ": " + profile);
        System.out.println("Note: These changes will be written to profile.txt only when you choose 'Save Data'.");
    }    
    
    // Reads the calculation method from profile.txt header (if present)
    private static String fetchCalcMethodFromFile() {
        File file = new File("./data/profile.txt");
        if (!file.exists()) {
            return "HARRIS_BENEDICT"; // default if file not found
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("CALC_METHOD:")) {
                    return line.substring("CALC_METHOD:".length()).trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching calculation method: " + e.getMessage());
        }
        return "HARRIS_BENEDICT";
    }

    // Reads all profiles from profile.txt into a temporary map (without updating global cache)
    private static Map<LocalDate, DailyProfile> fetchProfilesFromFile() {
        Map<LocalDate, DailyProfile> tempProfiles = new HashMap<>();
        File file = new File("./data/profile.txt");
        if (!file.exists()) {
            return tempProfiles;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean headerRead = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                if (!headerRead && line.startsWith("CALC_METHOD:")) {
                    headerRead = true;
                    continue;
                }
                // Expected format: date;gender;height;age;weight;activityLevel
                String[] parts = line.split(";");
                if (parts.length < 6)
                    continue;
                LocalDate date = LocalDate.parse(parts[0].trim());
                String gender = parts[1].trim();
                double height = Double.parseDouble(parts[2].trim());
                int age = Integer.parseInt(parts[3].trim());
                double weight = Double.parseDouble(parts[4].trim());
                String activityLevel = parts[5].trim();
                DailyProfile profile = new DailyProfile(date, gender, height, age, weight, activityLevel);
                tempProfiles.put(date, profile);
            }
        } catch (Exception e) {
            System.err.println("Error fetching profiles from file: " + e.getMessage());
        }
        return tempProfiles;
    }


    private static DailyProfile getMostRecentProfile(LocalDate date) {
        DailyProfile recent = null;
        for (LocalDate d : profileMap.keySet()) {
            if (d.isBefore(date)) {
                if (recent == null || d.isAfter(recent.getDate())) {
                    recent = profileMap.get(d);
                }
            }
        }
        return recent;
    }
    

    private static void changeCalcMethodCLI(Scanner scanner) {
        System.out.println("Current calculation method: " + calcMethodFromFile);
        System.out.println("Select new Calorie Calculation Method:");
        System.out.println("1. Harris-Benedict");
        System.out.println("2. Mifflin-St Jeor");
        System.out.print("Enter option (or press ENTER to keep current): ");
        String choice = scanner.nextLine().trim();
        if (choice.isEmpty()) {
            System.out.println("No change made. Calculation method remains: " + calcMethodFromFile);
            return;
        }
        if (choice.equals("1")) {
            calcMethodFromFile = "HARRIS_BENEDICT";
            System.out.println("Calorie calculation method set to Harris-Benedict.");
        } else if (choice.equals("2")) {
            calcMethodFromFile = "MIFFLIN_ST_JEOR";
            System.out.println("Calorie calculation method set to Mifflin-St Jeor.");
        } else {
            System.out.println("Invalid selection. Calculation method unchanged.");
        }
        System.out.println("Note: This change will be saved to profile.txt only when you choose 'Save Data'.");
    }
    

    private static double calculateTargetCalories(DailyProfile profile, String calcMethod) {
        double bmr = 0;
        if (calcMethod.equalsIgnoreCase("HARRIS_BENEDICT")) {
            if (profile.getGender().equalsIgnoreCase("M")) {
                bmr = 66.47 + (13.75 * profile.getWeight()) + (5.003 * profile.getHeight()) - (6.755 * profile.getAge());
            } else {
                bmr = 655.1 + (9.563 * profile.getWeight()) + (1.850 * profile.getHeight()) - (4.676 * profile.getAge());
            }
        } else if (calcMethod.equalsIgnoreCase("MIFFLIN_ST_JEOR")) {
            if (profile.getGender().equalsIgnoreCase("M")) {
                bmr = (10 * profile.getWeight()) + (6.25 * profile.getHeight()) - (5 * profile.getAge()) + 5;
            } else {
                bmr = (10 * profile.getWeight()) + (6.25 * profile.getHeight()) - (5 * profile.getAge()) - 161;
            }
        }
        double activityFactor = 1.2;
        switch (profile.getActivityLevel().toLowerCase()) {
            case "lightly active":
                activityFactor = 1.375;
                break;
            case "moderately active":
                activityFactor = 1.55;
                break;
            case "very active":
                activityFactor = 1.725;
                break;
            case "extra active":
                activityFactor = 1.9;
                break;
            default:
                activityFactor = 1.2;
                break;
        }
        return bmr * activityFactor;
    }
     

    private static void viewDailyProfileCLI(Scanner scanner) {
        // Use the temporary fetch function so that only saved data is shown.
        Map<LocalDate, DailyProfile> tempProfiles = fetchProfilesFromFile();
        System.out.print("Enter date to view profile (YYYY-MM-DD) or press ENTER for today: ");
        String dateInput = scanner.nextLine().trim();
        LocalDate date = dateInput.isEmpty() ? LocalDate.now() : null;
        if (date == null) {
            try {
                date = LocalDate.parse(dateInput);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format.");
                return;
            }
        }
        DailyProfile profile = tempProfiles.get(date);
        if (profile == null) {
            System.out.println("No saved profile found for " + date);
        } else {
            System.out.println("Saved profile for " + date + ": " + profile);
        }
    }    
        

    private static void viewCalorieSummaryCLI(Scanner scanner) {
        // Use the temporary fetch function to get only saved profile data.
        Map<LocalDate, DailyProfile> tempProfiles = fetchProfilesFromFile();
        String fileCalcMethod = fetchCalcMethodFromFile();
        
        System.out.print("Enter date for calorie summary (YYYY-MM-DD) or press ENTER for today: ");
        String dateInput = scanner.nextLine().trim();
        LocalDate date = dateInput.isEmpty() ? LocalDate.now() : null;
        if (date == null) {
            try {
                date = LocalDate.parse(dateInput);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format.");
                return;
            }
        }
        DailyProfile profile = tempProfiles.get(date);
        if (profile == null) {
            System.out.println("No saved profile available for " + date + ". Please update and save your profile.");
            return;
        }
        
        // Use the in-memory dailyLogManager for logs.
        DailyLog log = dailyLogManager.getLog(date);
        double totalConsumed = 0;
        for (LogEntry entry : log.getEntries()) {
            Food food = getFoodById(entry.getFoodId());
            if (food != null) {
                totalConsumed += food.getCalories() * entry.getServings();
            }
        }
        double target = calculateTargetCalories(profile, fileCalcMethod);
        double difference = totalConsumed - target;
        System.out.println("Calorie Summary for " + date + ":");
        System.out.println(" - Total Calories Consumed: " + totalConsumed);
        System.out.println(" - Target Calorie Intake (" + fileCalcMethod + "): " + target);
        if (difference < 0) {
            System.out.println(" - You are under your target by " + (-difference) + " calories.");
        } else if (difference > 0) {
            System.out.println(" - You exceeded your target by " + difference + " calories.");
        } else {
            System.out.println(" - You met your target exactly.");
        }
    }    
    
}

