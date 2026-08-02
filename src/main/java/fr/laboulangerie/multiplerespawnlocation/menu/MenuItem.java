package fr.laboulangerie.multiplerespawnlocation.menu;

import org.bukkit.inventory.ItemStack;

/**
 * Représente un élément du menu de respawn avec son type et identifiant.
 */
public class MenuItem {

    public enum Type {
        BED,
        SPAWN,
        HAMLET,
        MULTISPAWN
    }

    private final ItemStack item;
    private final Type type;
    private final String identifier; // UUID pour bed, nom pour multispawn, null sinon

    public MenuItem(ItemStack item, Type type, String identifier) {
        this.item = item;
        this.type = type;
        this.identifier = identifier;
    }

    public MenuItem(ItemStack item, Type type) {
        this(item, type, null);
    }

    public ItemStack getItem() {
        return item;
    }

    public Type getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }
}
