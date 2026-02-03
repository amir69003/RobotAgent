package Util;

import world.Carte;
import world.EtatCase;
import world.Position;

import java.util.HashMap;
import java.util.Map;

public class CarteExploree {
    private static CarteExploree instance;
    private Map<Position, Boolean> zones;

    private CarteExploree() {
        zones = new HashMap<>();
    }

    public static synchronized CarteExploree getInstance() {
        if (instance == null) {
            instance = new CarteExploree();
        }
        return instance;
    }

    public void initialiser(Carte carte, Position vaisseau) {
        zones.clear();
        for (int row = 0; row < carte.getHeight(); row++) {
            for (int col = 0; col < carte.getWidth(); col++) {
                Position pos = new Position(col, row);
                if (carte.getEtat(pos) != EtatCase.OBSTACLE) {
                    zones.put(pos, false); // false = inexploré
                }
            }
        }
        zones.put(vaisseau, true); // Vaisseau exploré
    }

    public synchronized void marquerExplore(Position pos) {
        zones.put(pos, true);
    }

    public synchronized boolean estExplore(Position pos) {
        return zones.getOrDefault(pos, false);
    }

    public synchronized Map<Position, Boolean> getZones() {
        return new HashMap<>(zones);
    }

    public synchronized long getNombreExplorees() {
        return zones.values().stream().filter(b -> b).count();
    }

    public synchronized long getNombreTotal() {
        return zones.size();
    }
}