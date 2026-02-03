package Agent;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import world.Carte;
import world.Position;
import Util.Util;

import java.util.*;

public class RescueAgent extends Agent {

    private Util util = new Util();

    // === Beliefs ===
    private Map<String, Object> beliefs = new LinkedHashMap<>();

    public static Map<String, Position> positions = new HashMap<>();

    public final int maxEnergy = 3000;
    private Carte carte;
    // === Désirs et intentions ===
    private List<Desire> desires = new ArrayList<>();
    private List<String> intentions = new ArrayList<>();

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " prêt (en veille au vaisseau).");

        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            carte = (Carte) args[0];
            Position vaisseau = (Position) args[1];
            beliefs.put("position", vaisseau);
            beliefs.put("positionVaisseau", vaisseau);
        }

        beliefs.put("energie", maxEnergy);
        beliefs.put("appelSOS", false);
        beliefs.put("robotPosition", null);
        beliefs.put("missionTerminee", true);

        // ✅ Boucle BDI toutes les 1 secondes
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
    private void updatePosition(Position newPos) {
        beliefs.put("position", newPos);
        positions.put(getLocalName(), newPos);
    }

    // === Beliefs ===
    private void updateBeliefs() {

        Position pos = (Position) beliefs.get("position");
        Position posVaisseau = (Position) beliefs.get("positionVaisseau");
        if (pos != null && pos.equals(posVaisseau)) {
            beliefs.put("energie", maxEnergy);
        }
        ACLMessage msg = receive();
        if (msg != null && msg.getContent().startsWith("SOS")) {
            System.out.println(getLocalName() + " ← [Talkie] : " + msg.getContent());
            try {
                String[] parts = msg.getContent().split(" ");
                int x = Integer.parseInt(parts[4].split("=")[1]);
                int y = Integer.parseInt(parts[5].split("=")[1]);
                beliefs.put("robotPosition", new Position(x, y));
                beliefs.put("appelSOS", true);
                beliefs.put("missionTerminee", false);
            } catch (Exception e) {
                System.out.println("Erreur de parsing du message SOS.");
            }
        }

    }

    // === Desires ===
    private void generateDesires() {
        desires.clear();

        Position pos = (Position) beliefs.get("position");
        Position vaisseau = (Position) beliefs.get("positionVaisseau");

        boolean appelSOS = (boolean) beliefs.getOrDefault("appelSOS", false);
        boolean missionTerminee = (boolean) beliefs.getOrDefault("missionTerminee", true);

        if (appelSOS && !missionTerminee) {
            desires.add(new Desire("aiderRobot", 100));
        }else if (!appelSOS && missionTerminee && !pos.equals(vaisseau)) {
            desires.add(new Desire("retournerVaisseau", 50));
        }else {
            desires.add(new Desire("attendreAppel", 10));
        }

        desires.sort((d1, d2) -> Integer.compare(d2.priority, d1.priority));
    }

    // === Intentions ===
    private void filterIntentions() {
        intentions.clear();

        if (!desires.isEmpty()) {
            Desire best = desires.get(0);
            if (best.name.equals("aiderRobot")) {
                beliefs.remove("cheminVaisseau");
            } else if (best.name.equals("retournerVaisseau")) {
                beliefs.remove("cheminRobot");
            }
            generateIntentions(best);
        }
    }
    private void generateIntentions(Desire desire) {
        switch (desire.name) {
            case "aiderRobot" -> {
                intentions.add("quitVaisseau");
                intentions.add("atteindre(robot)");
                intentions.add("avancer1case");
                intentions.add("rechargerRobot");
            }
            case "retournerVaisseau" -> {
                intentions.add("atteindre(vaisseau)");
                intentions.add("avancer1case");
            }
            case "attendreAppel" -> {
                // aucun plan actif
            }
        }
    }

    // === Exécution des plans ===
    private void executePlans() {
        if (intentions.isEmpty()) return;

        for (String i : intentions) {

            switch (i) {
                case "quitVaisseau" -> addBehaviour(new QuitVaisseauPlan());
                case "atteindre(robot)" -> {
                    if (!beliefs.containsKey("cheminRobot"))
                        addBehaviour(new CourtCheminPlan("cheminRobot"));
                }
                case "atteindre(vaisseau)" -> {
                    if (!beliefs.containsKey("cheminVaisseau"))
                        addBehaviour(new CourtCheminPlan("cheminVaisseau"));
                }
                case "avancer1case" -> addBehaviour(new Avancer1CasePlan());
                case "rechargerRobot" -> addBehaviour(new RechargerRobotPlan());
            }

        }
    }

    // === Satisfaction ===
    private void checkSatisfaction() {
        if (!desires.isEmpty()) {
            Desire d = desires.get(0);
            if (isSatisfied(d)) {
                System.out.println("Désir " + d.name + " comblé !");
            }
        }
    }

    private boolean isSatisfied(Desire desire) {
        return switch (desire.name) {
            case "aiderRobot" -> (boolean) beliefs.getOrDefault("missionTerminee", false);
            default -> false;
        };
    }

    // === Plans (comme ton RobotAgent) ===

    private class QuitVaisseauPlan extends OneShotBehaviour {
        public void action() {

            Position pos = (Position) beliefs.get("position");
            Position vaisseau = (Position) beliefs.get("positionVaisseau");

            if (pos != null && pos.equals(vaisseau)) {
                Position next = new Position(pos.x + 1, pos.y);
                if (next.x < carte.getWidth()) {
                    updatePosition(next);
                    System.out.println(getLocalName() + " quitte le vaisseau : " + next);
                }
            }
        }
    }

    private class CourtCheminPlan extends OneShotBehaviour {
        private String keyChemin;
        public CourtCheminPlan(String keyChemin) { this.keyChemin = keyChemin; }
        public void action() {
            if (Objects.equals(keyChemin, "cheminRobot")) calculerCheminVersRobot();
            if (Objects.equals(keyChemin, "cheminVaisseau")) calculerCheminVersVaisseau();
        }
    }

    private class Avancer1CasePlan extends OneShotBehaviour {
        public void action() {
            Integer energie = (Integer) beliefs.get("energie");
            if (energie <= 0) {
                System.out.println(getLocalName() + " est immobilisé (énergie = 0)");
                return;
            }

            String keyChemin = beliefs.containsKey("cheminRobot") ? "cheminRobot" : "cheminVaisseau";
            System.out.println(getLocalName() + " je prend le chemin " + keyChemin);
            List<Position> chemin = (List<Position>) beliefs.get(keyChemin);
            System.out.println(chemin.size());

            if (chemin != null && !chemin.isEmpty()) {
                Position prochaine = chemin.remove(0);
                //System.out.println(getLocalName() + " avancer1case : " + prochaine);
                updatePosition(prochaine);
                beliefs.put("energie", energie - 10);
                //System.out.println(getLocalName() + " avancer 1 carte : " + prochaine);
                System.out.println(getLocalName() + " avance vers " + prochaine +
                        " | Energie restante = " + energie);
            } else {
                beliefs.remove(keyChemin);
            }
        }
    }
    private class RechargerRobotPlan extends OneShotBehaviour {
        public void action() {
            Position pos = (Position) beliefs.get("position");
            Position posRobot = (Position) beliefs.get("robotPosition");
            boolean estAdjacent =
                    (Math.abs(pos.x - posRobot.x) == 1 && pos.y == posRobot.y) ||  // gauche / droite
                            (Math.abs(pos.y - posRobot.y) == 1 && pos.x == posRobot.x);    // haut / bas

            if(estAdjacent) {
                System.out.println(getLocalName() + " recharge le robot !");
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(new AID("Robot1", AID.ISLOCALNAME));
                msg.setContent("Recharge effectuée !");
                send(msg);

                beliefs.put("appelSOS", false);
                beliefs.put("missionTerminee", true);
                beliefs.put("robotPosition", null);
                beliefs.remove("cheminRobot");
            }

        }
    }
    private void calculerCheminVersVaisseau() {
        Position depart = (Position) beliefs.get("position");
        Position vaisseau = (Position) beliefs.get("positionVaisseau");
        if (depart == null || vaisseau == null) return;

        List<Position> chemin = util.calculerChemin(carte, depart, vaisseau);
        beliefs.put("cheminVaisseau", chemin);
    }
    private void calculerCheminVersRobot() {
        Position depart = (Position) beliefs.get("position");
        Position robotPos = (Position) beliefs.get("robotPosition"); // ✅ Bon nom
        if (depart == null || robotPos == null) return;

        // Trouver la meilleure position adjacente au robot
        Position targetAdj = util.findBestAdjacentPosition(carte, depart, robotPos);
        List<Position> chemin = util.calculerChemin(carte, depart, targetAdj);
        beliefs.put("cheminRobot", chemin); // ✅ Bonne clé
        System.out.println(getLocalName() + " 🗺️ Chemin calculé vers " + targetAdj);
    }



    // === Structure Desire ===
    private static class Desire {
        String name;
        int priority;

        Desire(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }
    }
}
