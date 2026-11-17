package com.alexh.capitalism.block;

import net.minecraft.util.StringRepresentable;

// What the Purification Table's glass tank shows.
public enum TankState implements StringRepresentable {
    EMPTY("empty"),
    DIRTY("dirty"),
    CLEAN("clean");

    private final String name;

    TankState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
