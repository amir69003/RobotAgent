package Agent;


import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import world.Carte;
import world.EtatCase;
import world.Position;
import Util.Util;

import Util.CarteExploree;
import java.util.*;

public class ResearcherAgent extends Agent {

    private Util util = new Util();

    // ====== Beliefs (croyances) ======
    private Map<String, Object> beliefs = new LinkedHashMap<>();

    // Désirs et intentions
    private List<Desire> desires = new ArrayList<>();
    private List<String> intentions = new ArrayList<>();
    private final int maxEnergy = 1000;

    // Zones déjà explorées
    private Set<Position> exploredZones = new HashSet<>();
    private Carte carte;
    // ✅ Registre global des positions pour la GUI
    public static Map<String, Position> positions = new HashMap<>();

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " (Scout) prêt.");

        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            carte = (Carte) args[0];
            Position vaisseau = (Position) args[1];
            updatePosition(vaisseau);
            beliefs.put("positionVaisseau", vaisseau);
            CarteExploree.getInstance().initialiser(carte, vaisseau);


        }

        beliefs.put("energie", maxEnergy);
        beliefs.put("pierresTrouvees", new ArrayList<Position>());

        // Boucle BDI toutes les 1s
        addBehaviour(new TickerBehaviour(this, 200) {
            protected void onTick() {
                updateBeliefs();
                generateDesires();
                filterIntentions();
                executePlans();
                checkSatisfaction();
            }
        });
    }

    // ✅ MAJ position + registre global
    private void updatePosition(Position newPos) {
        beliefs.put("position", newPos);
        positions.put(getLocalName(), newPos);
        if(!CarteExploree.getInstance().estExplore(newPos)) {
            CarteExploree.getInstance().marquerExplore(newPos);
        }

    }

    public Position getPos() {
        return (Position) beliefs.get("position");
    }

    // ====== Beliefs ======
    private void updateBeliefs() {
        Position pos = (Position) beliefs.get("position");
        Position posVaisseau = (Position) beliefs.get("positionVaisseau");

        // Recharge au vaisseau
        if (pos != null && pos.equals(posVaisseau)) {
            beliefs.put("energie", maxEnergy);
            System.out.println(getLocalName() + " recharge son énergie au vaisseau.");
        }

        // Réception de messages (recharge d'urgence)
        ACLMessage msg = receive();
        if (msg != null && msg.getContent().contains("Recharge effectuée")) {
            beliefs.put("energie", maxEnergy);
            System.out.println(getLocalName() + " ← [Talkie] : Recharge confirmée !");
        }
    }

    // ====== Desires ======
    private void generateDesires() {
        desires.clear();

        Integer energie = (Integer) beliefs.getOrDefault("energie", 100);
        Position pos = (Position) beliefs.get("position");
        Position vaisseau = (Position) beliefs.get("positionVaisseau");

        // ⚠️ Énergie critique
        if (energie <= 0) {
            System.out.println(getLocalName() + " est bloqué (énergie = 0)");
            appelerSecours();
            return;
        }

        // Calcul coût retour
        int coutRetour = Integer.MAX_VALUE;
        if (carte != null && pos != null && vaisseau != null) {
            List<Position> cheminRetour = util.calculerChemin(carte, pos, vaisseau);
            if (cheminRetour != null) {
                coutRetour = cheminRetour.size() * 10;
            }
        }
        boolean aDesZonesInexplorees = util.existeZoneInexploree(carte, pos);

        // Priorité : retour si énergie faible
        if (energie <= coutRetour + 20) {
            desires.add(new Desire("retournerVaisseau", 100));
        } else if (!aDesZonesInexplorees) {
            desires.add(new Desire("retournerVaisseau", 90));
            System.out.println(getLocalName() + " : Carte entièrement explorée → retour au vaisseau");
        } else {
            desires.add(new Desire("explorerCarte", 80));
        }

        desires.sort((d1, d2) -> Integer.compare(d2.priority, d1.priority));
    }

    // ====== Intentions ======
    private void filterIntentions() {
        intentions.clear();

        if (!desires.isEmpty()) {
            Desire best = desires.get(0);
            if (best.name.equals("retournerVaisseau")) {
                beliefs.remove("cheminExploration");
            } else if (best.name.equals("explorerCarte")) {
                beliefs.remove("cheminVaisseau");
            }
            generateIntentions(best);
        }
    }

    private void generateIntentions(Desire desire) {
        switch (desire.name) {
            case "explorerCarte" -> {
                intentions.add("choisirZoneInexploree");
                intentions.add("avancer1case");
                intentions.add("scannerPierre");
            }
            case "retournerVaisseau" -> {
                intentions.add("atteindre(vaisseau)");
                intentions.add("avancer1case");
                intentions.add("scannerPierre");
            }
        }
    }

    // ====== Satisfaction ======
    private void checkSatisfaction() {
        if (!desires.isEmpty()) {
            Desire d = desires.get(0);
            if (isSatisfied(d)) {
                System.out.println(getLocalName() + " : Désir " + d.name + " comblé !");
            }
        }
    }

    private boolean isSatisfied(Desire desire) {
        Position pos = (Position) beliefs.get("position");
        Position vaisseau = (Position) beliefs.get("positionVaisseau");

        return switch (desire.name) {
            case "retournerVaisseau" -> pos != null && pos.equals(vaisseau);
            case "explorerCarte" -> false; // Exploration infinie
            default -> false;
        };
    }

    // ====== Exécution des Plans ======
    private void executePlans() {
        for (String i : intentions) {
            switch (i) {
                case "choisirZoneInexploree" -> {
                    if (!beliefs.containsKey("cheminExploration")) {
                        addBehaviour(new ChoisirZonePlan());
                    }
                }
                case "atteindre(vaisseau)" -> {
                    if (!beliefs.containsKey("cheminVaisseau")) {
                        addBehaviour(new CourtCheminVaisseauPlan());
                    }
                }
                case "avancer1case" -> addBehaviour(new Avancer1CasePlan());
                case "scannerPierre" -> addBehaviour(new ScannerPierrePlan());
            }
        }
    }

    // ====== Plans ======
    private class ChoisirZonePlan extends OneShotBehaviour {
        public void action() {
            Position depart = (Position) beliefs.get("position");
            if (depart == null) return;

            Position cible = trouverZoneInexploree(carte, depart);
            if (cible != null) {
                List<Position> chemin = util.calculerChemin(carte, depart, cible);
                beliefs.put("cheminExploration", chemin);
            }
        }
    }

    private class CourtCheminVaisseauPlan extends OneShotBehaviour {
        public void action() {
            Position depart = (Position) beliefs.get("position");
            Position vaisseau = (Position) beliefs.get("positionVaisseau");
            if (depart == null || vaisseau == null) return;

            List<Position> chemin = util.calculerChemin(carte, depart, vaisseau);
            beliefs.put("cheminVaisseau", chemin);
        }
    }

    private class Avancer1CasePlan extends OneShotBehaviour {
        public void action() {
            Integer energie = (Integer) beliefs.get("energie");
            if (energie <= 0) return;

            String keyChemin = beliefs.containsKey("cheminExploration") ? "cheminExploration" : "cheminVaisseau";
            List<Position> chemin = (List<Position>) beliefs.get(keyChemin);

            if (chemin != null && !chemin.isEmpty()) {
                Position prochaine = chemin.remove(0);
                updatePosition(prochaine);

                beliefs.put("energie", energie - 10);
                System.out.println(getLocalName() + " avance vers " + prochaine + " | Énergie = " + (energie - 10));
            } else {
                beliefs.remove(keyChemin);
            }
        }
    }

    private class ScannerPierrePlan extends OneShotBehaviour {
        public void action() {
            Position pos = (Position) beliefs.get("position");
            if (pos != null && carte.getEtat(pos) == EtatCase.PIERRE) {
                signalerPierre(pos);
            }
        }
    }

    // ====== Utils ======
    // Dans trouverZoneInexploree
    private Position trouverZoneInexploree(Carte carte, Position depart) {
        CarteExploree carteExploree = CarteExploree.getInstance();
        Position meilleureCible = null;
        double meilleurScore = Double.NEGATIVE_INFINITY;

        // Parcourt toutes les cases connues pour trouver les "frontières"
        for (Map.Entry<Position, Boolean> entry : carteExploree.getZones().entrySet()) {
            Position pos = entry.getKey();

            // Si la case est explorée, on regarde ses voisins
            if (carteExploree.estExplore(pos)) {
                // Chercher les voisins inexplorés de cette case
                for (Position voisin : pos.voisins()) {
                    // ✅ On cible directement le voisin inexploré
                    if (!carteExploree.estExplore(voisin) && carte.estAccessible(voisin)) {
                        // Calcule le chemin vers ce voisin inexploré
                        List<Position> chemin = util.calculerChemin(carte, depart, voisin);

                        if (chemin != null && !chemin.isEmpty()) {
                            double distance = chemin.size();
                            // Favorise les cases proches
                            double score = 100.0 - distance;

                            if (score > meilleurScore) {
                                meilleurScore = score;
                                meilleureCible = voisin; // ✅ On retourne le voisin inexploré
                            }
                        }
                    }
                }
            }
        }

        if (meilleureCible != null) {
            System.out.println(getLocalName() + " a choisi une zone frontière " + meilleureCible
                    + " (score curiosité = " + meilleurScore + ")");
        } else {
            System.out.println(getLocalName() + " n'a trouvé aucune zone inexplorée atteignable !");
        }

        return meilleureCible;
    }

    private void signalerPierre(Position pos) {
        List<Position> pierresTrouvees = (List<Position>) beliefs.get("pierresTrouvees");

        // 🔹 1. Vérifie si une pierre très proche a déjà été signalée
        for (Position p : pierresTrouvees) {
            if (pierresTrouvees.contains(pos)) {
                return; // Déjà signalée
            }
        }

        // 🔹 2. Si nouvelle pierre, on la garde et on la signale
        pierresTrouvees.add(pos);

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("Robot1", AID.ISLOCALNAME));
        msg.setContent("Pierre détectée à x=" + pos.x + " y=" + pos.y);
        send(msg);

        // 🔹 3. On la marque comme explorée pour éviter de revenir dessus
        CarteExploree.getInstance().marquerExplore(pos);

        System.out.println(getLocalName() + " → [Signal] Pierre trouvée à " + pos);
    }

    private void appelerSecours() {
        Position pos = (Position) beliefs.get("position");
        if (pos == null) return;

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("Robot2", AID.ISLOCALNAME));
        msg.setContent("SOS besoin d'énergie à x=" + pos.x + " y=" + pos.y);
        send(msg);

        System.out.println(getLocalName() + " → [Talkie] SOS à " + pos);
    }

    private static class Desire {
        String name;
        int priority;
        Desire(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }
    }
}
