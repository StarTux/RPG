package com.winthier.rpg;

final class Msg {
    private Msg() { }

    public static String capitalize(String inp) {
        if (inp.isEmpty()) return inp;
        return inp.substring(0, 1).toUpperCase() + inp.substring(1).toLowerCase();
    }
}
