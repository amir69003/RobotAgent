package org.example;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import javafx.application.Application;
import Systeme.Systeme;

import java.io.InputStream;


public class Main {
    public static Systeme GLOBAL_SYSTEME;

    public static void main(String[] args) {
        try {
            // Lancer JADE
            Runtime rt = Runtime.instance();
            Profile p = new ProfileImpl();
            ContainerController cc = rt.createMainContainer(p);

            // Charger la carte et initialiser le système avec JADE
            InputStream input = Main.class.getClassLoader().getResourceAsStream("carte.txt");
            GLOBAL_SYSTEME = new Systeme(input, cc);


            // Lancer la GUI
            Application.launch(gui.MapGUI.class, args);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
