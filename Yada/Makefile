# Root directory
SRC_DIR = src
OUT_DIR = out

# Find all Java source files
SOURCES = $(shell find $(SRC_DIR) -name "*.java")

# Main class with main() method
MAIN_CLASS = app.FoodDiaryApp

# Default target
all: compile run

# Compile all Java source files
compile:
	mkdir -p $(OUT_DIR)
	javac -d $(OUT_DIR) $(SOURCES)

# Run the main class
run:
	java -cp $(OUT_DIR) $(MAIN_CLASS)

# Remove compiled files
clean:
	rm -rf $(OUT_DIR)

.PHONY: all compile run clean