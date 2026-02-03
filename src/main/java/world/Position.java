package world;

import java.util.ArrayList;
import java.util.List;

public class Position {
    public int x;
    public final int y;
    public Position(int x, int y) { this.x = x; this.y = y; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position p = (Position) o;
        return this.x == p.x && this.y == p.y;
    }

    public List<Position> voisins() {
        List<Position> voisins = new ArrayList<>();
        voisins.add(new Position(x + 1, y));
        voisins.add(new Position(x - 1, y));
        voisins.add(new Position(x, y + 1));
        voisins.add(new Position(x, y - 1));
        return voisins;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(x);
        result = 31 * result + Integer.hashCode(y);
        return result;
    }
}
