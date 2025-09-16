package model;

import java.util.*;

public abstract class Food {
    protected String id; // e.g., B1, C1
    protected String name; // unique identifying name
    protected List<String> keywords; // keywords for search
    protected boolean committed; // true if saved to file (persistent)
    protected String extraInfo;

    public Food(String id, String name, List<String> keywords,String extraInfo) {
        this.id = id;
        this.name = name;
        // Trim keywords to remove extra spaces
        List<String> trimmed = new ArrayList<>();
        for (String k : keywords) {
            trimmed.add(k.trim());
        }
        this.keywords = trimmed;
        this.committed = false; // default for new items
        this.extraInfo = extraInfo;
    }

    public String getExtraInfo() {
        return extraInfo;
    }  

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public boolean isCommitted() {
        return committed;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

    // Returns calories per serving (can be extended later)
    public abstract double getCalories();
}