package Systeme;

import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import world.Carte;
import world.Position;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Systeme {
    private Carte carte;
    private List<AgentController> agents = new ArrayList<>();

    public Systeme(InputStream input, ContainerController cc) {
        this.carte = new Carte(input);
        initRobot(cc);
    }

    private void initRobot(ContainerController cc) {
        try {
            Position positionVaisseau = carte.getPositionVaisseau();

            AgentController robot = cc.createNewAgent(
                    "Robot1",
                    "Agent.RobotAgent",
                    new Object[]{ carte, positionVaisseau }
            );
            robot.start();

            AgentController robotsauveur = cc.createNewAgent(
                    "Robot2",
                    "Agent.RescueAgent",
                    new Object[]{ carte, positionVaisseau }
            );
            robotsauveur.start();

            AgentController robotseachear = cc.createNewAgent(
                    "Robot3",
                    "Agent.ResearcherAgent",
                    new Object[]{ carte, positionVaisseau }
            );
            robotseachear.start();

            agents.add(robot);
            agents.add(robotsauveur);
            agents.add(robotseachear);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Carte getCarte() { return carte; }
    public List<AgentController> getAgents() { return agents; }
}
