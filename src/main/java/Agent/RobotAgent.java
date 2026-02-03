package Agent;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import world.Carte;
import world.EtatCase;
import world.Position;
import Util.Util;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

import Util.CarteExploree;
import java.util.*;

public class RobotAgent extends Agent {

    private Util util = new Util();

    // ====== Beliefs (croyances)
    private Map<String, Object> beliefs = new LinkedHashMap<>();

    private final int maxEnergie = 300;

    // Désirs et intentions
    private List<Desire> desires = new ArrayList<>();
    private List<String> intentions = new ArrayList<>();

    private final int STOCK_MAX = 5;

    private Carte carte;

    // ✅ Registre global des positions pour la GUI
    public static Map<String, Position> positions = new HashMap<>();



    @Override
    protected void setup() {
        System.out.println(getLocalName() + " prêt.");

        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            carte = (Carte) args[0];
            Position vaisseau = (Position) args[1];
            updatePosition(vaisseau);
            beliefs.put("positionVaisseau", vaisseau);
        }

        beliefs.put("pierre", 0);
        beliefs.put("energie", maxEnergie);
        beliefs.put("dontcheckreturn", false);

        beliefs.put("ciblePierre", null);
        beliefs.put("pierresEnAttente", new LinkedList<Position>());


        addBehaviour(new TickerBehaviour(this, (200)) {
            protected void onTick() {
                updateBeliefs();
                generateDesires();
                filterIntentions();
                executePlans();
                checkSatisfaction();
            }
        });
    }


    // ✅ méthode utilitaire pour MAJ position + registre global
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
        Position vaisseau = (Position) beliefs.get("positionVaisseau");
        int energie = (int) beliefs.get("energie");


        // ⚡ Recharge si retour au vaisseau
        if (pos != null && pos.equals(vaisseau) && energie < maxEnergie) {
            beliefs.put("energie", maxEnergie);
            boolean dontCheckReturn = Math.random() < 0.5;

            beliefs.put("dontcheckreturn", dontCheckReturn);
            if (dontCheckReturn)
                System.out.println(getLocalName() + " → ne vérifiera pas le chemin de retour.");
            else
                System.out.println(getLocalName() + " → vérifiera le chemin de retour.");

            // dépôt du stock
            int stock = (int) beliefs.getOrDefault("pierre", 0);
            if (stock > 0) {
                System.out.println(getLocalName() + " dépose " + stock + " pierres au vaisseau.");
                beliefs.put("pierre", 0);
            }
        }

        // 🔹 Réception de messages
        ACLMessage msg = receive();
        if (msg != null) {
            String contenu = msg.getContent();

            // 🔹 Message du ResearcherAgent : "Pierre détectée à x=.. y=.."
            if (contenu.startsWith("Pierre détectée")) {
                String[] parts = contenu.split(" ");
                int x = Integer.parseInt(parts[3].split("=")[1]);
                int y = Integer.parseInt(parts[4].split("=")[1]);

                Position cible = new Position(x, y);
                Queue<Position> file = (Queue<Position>) beliefs.get("pierresEnAttente");

                // Si la pierre n'est pas déjà en attente
                if (!file.contains(cible)) {
                    file.add(cible);
                    System.out.println(getLocalName() + " → ajoute " + cible + " à la file des pierres en attente.");
                }

                // Si aucune cible active, on prend la première
                if (beliefs.get("ciblePierre") == null) {
                    beliefs.put("ciblePierre", file.poll());
                }


                System.out.println(getLocalName() + " ← [Talkie] Nouvelle pierre détectée à " + cible);

                // ⚡ Si il était en retour et stock pas plein -> repart chercher
                int stock = (int) beliefs.getOrDefault("pierre", 0);
                if (stock < STOCK_MAX) {
                    beliefs.remove("cheminVaisseau");
                    System.out.println(getLocalName() + " interrompt son retour pour aller à " + cible);
                }
            }

            if (contenu.contains("Recharge effectuée")) {
                beliefs.put("energie", maxEnergie);
                System.out.println(getLocalName() + " ← [Talkie] : Recharge confirmée !");
            }
        }
    }

    // ====== Desires ======
    private void generateDesires() {
        desires.clear();

        Integer stockPierre = (Integer) beliefs.getOrDefault("pierre", 0);
        Integer energie = (Integer) beliefs.getOrDefault("energie", maxEnergie);
        Position pos = (Position) beliefs.get("position");
        Position vaisseau = (Position) beliefs.get("positionVaisseau");

        Position cible = (Position) beliefs.get("ciblePierre");

        if (energie <= 0) {
            //System.out.println(getLocalName() + " est complètement bloqué (énergie = 0)");
            appelerSecours();
            return;
        }
        // ⚡ Calcul du coût pour retourner
        int coutRetour = Integer.MAX_VALUE;
        if (carte != null && pos != null && vaisseau != null) {
            List<Position> cheminRetour = util.calculerChemin(carte, pos, vaisseau);
            if (cheminRetour != null) {
                coutRetour = cheminRetour.size() * 10; // 10 énergie par case
            }
        }

        boolean dontCheckReturn = (boolean) beliefs.get("dontcheckreturn");
        boolean retourAuto = (boolean) beliefs.getOrDefault("retourAuto", false);


        // Désir : remplir le stock
        if (stockPierre < STOCK_MAX && cible != null) {
            desires.add(new Desire("remplirStock", 70));

        }else if (retourAuto){
            desires.add(new Desire("retournerVaisseau", 100));
            if(pos != null && pos.equals(vaisseau)){
                beliefs.put("retourAuto", false);
            }
            //beliefs.put("retourAuto", false);
        }
        else{
            desires.add(new Desire("attendreAppel", 10));
        }

        if (stockPierre == STOCK_MAX || energie <= coutRetour + 10 && !dontCheckReturn) {
            System.out.println("Je fais un retour au vaisseau");
            desires.add(new Desire("retournerVaisseau", 100));
        }


        // Tri par priorité
        desires.sort((d1, d2) -> Integer.compare(d2.priority, d1.priority));
    }


    // ====== Intentions ======
    private void filterIntentions() {
        intentions.clear();

        if (!desires.isEmpty()) {
            Desire best = desires.get(0);
            if (best.name.equals("retournerVaisseau")) {
                beliefs.remove("cheminPierre");
            } else if (best.name.equals("remplirStock")) {
                beliefs.remove("cheminVaisseau");
            }

            generateIntentions(best);
        }
    }

    private void generateIntentions(Desire desire) {
        switch (desire.name) {
            case "remplirStock" -> {
                intentions.add("quitVaisseau");
                intentions.add("atteindre(pierre)");
                intentions.add("avancer1case");
                intentions.add("recupPierre");
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

    // ====== Satisfaction ======
    private void checkSatisfaction() {
        if (!desires.isEmpty()) {
            Desire d = desires.get(0);
            if (isSatisfied(d)) {
                System.out.println("Désir " + d.name + " comblé !");
            }
        }
    }

    private boolean isSatisfied(Desire desire) {
        switch (desire.name) {
            case "remplirStock":
                return ((Integer) beliefs.getOrDefault("pierre", 0)) == STOCK_MAX;
            case "retournerVaisseau":
                Position pos = (Position) beliefs.get("position");
                Position vaisseau = (Position) beliefs.get("positionVaisseau");
                return pos != null && pos.equals(vaisseau);
            default:
                return false;
        }
    }

    // ====== Exécution des Plans ======
    private void executePlans() {
        for (String i : intentions) {
            switch (i) {
                case "quitVaisseau" -> addBehaviour(new QuitVaisseauPlan());
                case "atteindre(pierre)" -> {
                    if (!beliefs.containsKey("cheminPierre")) {
                        addBehaviour(new CourtCheminPlan("cheminPierre"));
                    }
                }
                case "atteindre(vaisseau)" -> {
                    if (!beliefs.containsKey("cheminVaisseau")) {
                        addBehaviour(new CourtCheminPlan("cheminVaisseau"));
                    }
                }
                case "avancer1case" -> addBehaviour(new Avancer1CasePlan());
                case "recupPierre" -> addBehaviour(new RecupPierrePlan());
            }
        }
    }

    // ====== Plans ======
    private class QuitVaisseauPlan extends OneShotBehaviour {
        public void action() {
            Position pos = (Position) beliefs.get("position");
            Position vaisseau = (Position) beliefs.get("positionVaisseau");
            if (pos.equals(vaisseau)) {
                Position next = new Position(pos.x + 1, pos.y);
                updatePosition(next);
                System.out.println(getLocalName() + " quitte le vaisseau vers " + next);
            }
        }
    }

    private class CourtCheminPlan extends OneShotBehaviour {
        private String keyChemin;
        public CourtCheminPlan(String keyChemin) { this.keyChemin = keyChemin; }
        public void action() {
            if (Objects.equals(keyChemin, "cheminPierre")) calculerCheminVersPierre();
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

            String keyChemin = beliefs.containsKey("cheminPierre") ? "cheminPierre" : "cheminVaisseau";
            List<Position> chemin = (List<Position>) beliefs.get(keyChemin);

            if (chemin != null && !chemin.isEmpty()) {
                Position prochaine = chemin.remove(0);
                //System.out.println(getLocalName() + " avancer1case : " + prochaine);
                updatePosition(prochaine);
                beliefs.put("energie", energie - 10);
                System.out.println(getLocalName() + " avance vers " + prochaine +
                        " | Energie restante = " + energie);
            } else {
                beliefs.remove(keyChemin);
            }
        }
    }

    private class RecupPierrePlan extends OneShotBehaviour {
        public void action() {
            Integer stock = (Integer) beliefs.getOrDefault("pierre", 0);
            if (stock >= STOCK_MAX) return;

            Position pos = (Position) beliefs.get("position");

            if (pos != null && carte.getEtat(pos) == EtatCase.PIERRE) {
                beliefs.put("pierre", stock + 1);
                carte.removePierres(pos, 1);

                if(carte.getNbPierres(pos) == 0) {
                    Queue<Position> file = (Queue<Position>) beliefs.get("pierresEnAttente");
                    beliefs.remove("cheminPierre");
                    if (!file.isEmpty()) {
                        Position prochaine = file.poll();
                        beliefs.put("ciblePierre", prochaine);
                        System.out.println(getLocalName() + " → passe à la pierre suivante " + prochaine);
                    } else {
                        beliefs.put("ciblePierre", null);
                        beliefs.remove("cheminPierre");
                        beliefs.put("retourAuto", true);
                        System.out.println(getLocalName() + " → plus de pierre en attente, retour au vaisseau.");
                    }
                }
                System.out.println(getLocalName() + " récupère une pierre. Stock = " + (stock + 1));
            }
        }
    }

    // ====== Utils ======
    private void calculerCheminVersPierre() {
        Position depart = (Position) beliefs.get("position");
        Position cible = (Position) beliefs.get("ciblePierre");

        if (depart == null || cible == null) return;
        List<Position> chemin = util.calculerChemin(carte, depart, cible);

        if (chemin != null) {
            beliefs.put("cheminPierre", chemin);
            System.out.println(getLocalName() + " a tracé un chemin vers la pierre " + cible);
        }
    }

    private void calculerCheminVersVaisseau() {
        Position depart = (Position) beliefs.get("position");
        Position vaisseau = (Position) beliefs.get("positionVaisseau");
        if (depart == null || vaisseau == null) return;

        List<Position> chemin = util.calculerChemin(carte, depart, vaisseau);
        beliefs.put("cheminVaisseau", chemin);
    }

    // === Appel au secours ===
    private void appelerSecours() {
        Position pos = (Position) beliefs.get("position");
        if (pos == null) return;

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("Robot2", AID.ISLOCALNAME));
        msg.setContent("SOS besoin d'énergie à x=" + pos.x + " y=" + pos.y);
        send(msg);
        System.out.println(getLocalName() + " → [Talkie] : SecoursAgent, j'ai besoin d'aide à " + pos);
    }


    private static class Desire {
        String name;
        int priority;
        Desire(String name, int priority) { this.name = name; this.priority = priority; }
    }
}
