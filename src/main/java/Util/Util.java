package Util;

import world.Carte;
import world.EtatCase;
import world.Position;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Util {
    public List<Position> calculerChemin(Carte carte, Position depart, Position cible) {
        Map<Position, Position> precedent = new LinkedHashMap<>();
        Queue<Position> queue = new LinkedList<>();
        Set<Position> visites = new HashSet<>();

        queue.add(depart);
        visites.add(depart);

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (!queue.isEmpty()) {
            Position current = queue.poll();
            if (current.equals(cible)) break;

            for (int i = 0; i < 4; i++) {
                Position voisin = new Position(current.x + dx[i], current.y + dy[i]);
                if (carte.getEtat(voisin) != EtatCase.OBSTACLE && !visites.contains(voisin) &&
                        voisin.x >= 0 && voisin.x < carte.getWidth() &&
                        voisin.y >= 0 && voisin.y < carte.getHeight()) {
                    queue.add(voisin);
                    visites.add(voisin);
                    precedent.put(voisin, current);
                }
            }
        }

        // Reconstruction du chemin
        List<Position> chemin = new ArrayList<>();
        Position step = cible;
        while (step != null && precedent.containsKey(step)) {
            chemin.add(0, step);
            step = precedent.get(step);
        }
        if (!chemin.isEmpty() && !chemin.get(0).equals(depart)) {
            chemin.add(0, depart);
        }
        return chemin;
    }
    public Position findBestAdjacentPosition(Carte carte, Position currentPos, Position robotPos) {

        Position[] adjacents = {
                new Position(robotPos.x + 1, robotPos.y),
                new Position(robotPos.x - 1, robotPos.y),
                new Position(robotPos.x, robotPos.y + 1),
                new Position(robotPos.x, robotPos.y - 1)
        };

        Position best = null;
        int minDist = Integer.MAX_VALUE;

        for (Position adj : adjacents) {
            if (adj.x >= 0 && adj.x < carte.getWidth() &&
                    adj.y >= 0 && adj.y < carte.getHeight()) {
                int dist = Math.abs(currentPos.x - adj.x) + Math.abs(currentPos.y - adj.y);
                if (dist < minDist) {
                    minDist = dist;
                    best = adj;
                }
            }
        }

        return best != null ? best : robotPos;
    }

    // java
    public boolean existeZoneInexploree(Carte carte, Position depart) {
        if (carte == null || depart == null) return false;

        CarteExploree carteExploree = CarteExploree.getInstance();

        // Parcourt toutes les cases explorées
        for (Map.Entry<Position, Boolean> entry : carteExploree.getZones().entrySet()) {
            Position pos = entry.getKey();

            if (carteExploree.estExplore(pos)) {
                // Cherche un voisin inexploré et accessible
                for (Position voisin : pos.voisins()) {
                    if (!carteExploree.estExplore(voisin) && carte.estAccessible(voisin)) {
                        List<Position> chemin = calculerChemin(carte, depart, voisin);
                        if (chemin != null && !chemin.isEmpty()) {
                            return true; // ✅ Il reste au moins une zone accessible
                        }
                    }
                }
            }
        }

        return false; // ❌ Plus rien à explorer
    }
}
