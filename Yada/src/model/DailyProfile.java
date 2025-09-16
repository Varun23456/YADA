package model;

import java.time.LocalDate;

public class DailyProfile {
    private LocalDate date; // The effective date for the profile
    private LocalDate lastModified; // When the profile was last updated
    private String gender; // "M" or "F"
    private double height; // in cm
    private int age; // in years
    private double weight; // in kg
    private String activityLevel; // e.g., "sedentary", "lightly active", etc.

    public DailyProfile(LocalDate date, String gender, double height, int age, double weight, String activityLevel) {
        this.date = date;
        this.gender = gender;
        this.height = height;
        this.age = age;
        this.weight = weight;
        this.activityLevel = activityLevel;
        this.lastModified = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDate getLastModified() {
        return lastModified;
    }

    public String getGender() {
        return gender;
    }

    public double getHeight() {
        return height;
    }

    public int getAge() {
        return age;
    }

    public double getWeight() {
        return weight;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public void setLastModified(LocalDate lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "Gender: " + gender + ", Height: " + height + " cm, Age: " + age + ", Weight: " + weight + " kg, Activity Level: " + activityLevel;
    }
}
