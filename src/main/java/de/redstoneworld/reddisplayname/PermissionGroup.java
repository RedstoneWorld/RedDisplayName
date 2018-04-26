package de.redstoneworld.reddisplayname;

/**
 * Created by Max on 20.03.2017.
 */
public class PermissionGroup {
    private final String name;
    private final String weight;

    public PermissionGroup(String name, String weight) {
        this.name = name;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public String getWeight() {
        return weight;
    }
}
