package world;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class Carte {

    private final int w;
    private final int h;

    // On crée une classe interne pour stocker l'état + le nombre de pierres
    public static class CaseInfo {
        private EtatCase etat;
        private int nbPierres;

        public CaseInfo(EtatCase etat) {
            this.etat = etat;
            this.nbPierres = (etat == EtatCase.PIERRE) ? 1 : 0;
        }

        public EtatCase getEtat() { return etat; }
        public void setEtat(EtatCase etat) { this.etat = etat; }

        public int getNbPierres() { return nbPierres; }
        public void setNbPierres(int nbPierres) { this.nbPierres = nbPierres; }

        public void addPierre(int n) { this.nbPierres += n; }
        public void removePierre(int n) { this.nbPierres = Math.max(0, this.nbPierres - n); }
    }

    private Map<Position, CaseInfo> map;

    // ✅ Constructeur classique avec chemin de fichier
    public Carte(String filename) {
        int[] size = loadCarte(new FileLoader(filename));
        this.w = size[0];
        this.h = size[1];
    }

    // ✅ Nouveau constructeur avec InputStream
    public Carte(InputStream input) {
        int[] size = loadCarte(new StreamLoader(input));
        this.w = size[0];
        this.h = size[1];
    }

    // ✅ Méthode factorisée : lit la carte via un "Reader" générique
    private int[] loadCarte(LineReader reader) {
        map = new LinkedHashMap<>();
        int rows = 0;
        int cols = 0;

        try {
            String line;
            int row = 0;
            while ((line = reader.readLine()) != null) {
                cols = line.length();
                for (int col = 0; col < line.length(); col++) {
                    char c = line.charAt(col);
                    EtatCase etat;
                    switch (c) {
                        case 'P': etat = EtatCase.PIERRE; break;
                        case 'V': etat = EtatCase.VAISSEAU; break;
                        case 'X': etat = EtatCase.OBSTACLE; break;
                        case '.': etat = EtatCase.VIDE; break;
                        default:  etat = EtatCase.VIDE; break;
                    }
                    map.put(new Position(col, row), new CaseInfo(etat));
                }
                row++;
            }
            rows = row;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new int[]{cols, rows};
    }

    // ====== Interface + Implémentations internes ======
    private interface LineReader {
        String readLine() throws IOException;
    }

    private static class FileLoader implements LineReader {
        private final BufferedReader br;
        public FileLoader(String filename) throws RuntimeException {
            try {
                this.br = new BufferedReader(new FileReader(filename));
            } catch (IOException e) {
                throw new RuntimeException("Impossible de charger la carte : " + filename, e);
            }
        }
        public String readLine() throws IOException { return br.readLine(); }
    }

    private static class StreamLoader implements LineReader {
        private final BufferedReader br;
        public StreamLoader(InputStream input) {
            this.br = new BufferedReader(new InputStreamReader(input));
        }
        public String readLine() throws IOException { return br.readLine(); }
    }

    // ====== Getters & Méthodes ======

    public int getWidth() { return w; }
    public int getHeight() { return h; }

    public boolean estAccessible(Position pos) {
        EtatCase etat = getEtat(pos);
        return etat != EtatCase.OBSTACLE && !(pos.x >= w && pos.y >= h && pos.x < 0 && pos.y < 0);
    }

    public CaseInfo getCaseInfo(Position pos) {
        return map.getOrDefault(pos, new CaseInfo(EtatCase.VIDE));
    }

    public EtatCase getEtat(Position pos) {
        return getCaseInfo(pos).getEtat();
    }

    public int getNbPierres(Position pos) {
        return getCaseInfo(pos).getNbPierres();
    }

    public Position getPositionVaisseau() {
        for (Map.Entry<Position, CaseInfo> entry : map.entrySet()) {
            if (entry.getValue().getEtat() == EtatCase.VAISSEAU) {
                return entry.getKey();
            }
        }
        return null; // si pas trouvé
    }

    public void setEtat(Position pos, EtatCase etat) {
        map.put(pos, new CaseInfo(etat));
    }

    public void addPierres(Position pos, int n) {
        CaseInfo ci = map.get(pos);
        if (ci != null) {
            ci.addPierre(n);
        }
    }

    public void removePierres(Position pos, int n) {
        CaseInfo ci = map.get(pos);
        if (ci != null) {
            ci.removePierre(n);
            if (ci.getNbPierres() <= 0) {
                ci.setEtat(EtatCase.VIDE);
            }
        }
    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < h && col >= 0 && col < w;
    }
}
