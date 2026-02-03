package gui;

import Agent.RobotAgent;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import world.Carte;
import world.EtatCase;
import world.Position;
import Systeme.Systeme;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import Util.CarteExploree;


public class MapGUI extends Application {

    private final int cellSize = 40;
    private final Systeme systeme = org.example.Main.GLOBAL_SYSTEME;

    @Override
    public void start(Stage stage) {
        printResourceDebug();
        Carte carte = systeme.getCarte();
        GridPane grid = new GridPane();

        // === fonction de rendu ===
        Runnable render = () -> {
            grid.getChildren().clear();
            for (int row = 0; row < carte.getHeight(); row++) {
                for (int col = 0; col < carte.getWidth(); col++) {
                    Rectangle cell = new Rectangle(cellSize, cellSize);
                    cell.setFill(Color.LIGHTGREEN); // fond par défaut

                    EtatCase etat = carte.getEtat(new Position(col, row));

                    // Image pour le terrain
                    ImageView imageView = null;
                    switch (etat) {

                        case OBSTACLE -> imageView = createImageView("../image/Obstacle.png");
                        case PIERRE ->{
                            if(!CarteExploree.getInstance().estExplore(new Position(col, row)))
                                imageView = createImageView("../image/Pierreins.png");
                            else
                                imageView = createImageView("../image/Pierre.png");
                        }
                        case VAISSEAU -> imageView = createImageView("../image/Vaisseau.png");
                        case VIDE -> {
                            if(!CarteExploree.getInstance().estExplore(new Position(col, row)))
                                imageView = createImageView("../image/Solins.png");
                            else
                                imageView = createImageView("../image/Sol.png");
                        }
                    }

                    // Superposer l'image du robot si présent
                    for (Position robotPos : Agent.RobotAgent.positions.values()) {
                        if (robotPos != null && robotPos.x == col && robotPos.y == row && etat != EtatCase.VAISSEAU) {
                            imageView = createImageView("../image/Robot.png");
                        }
                    }

                    // Superposer l'image du rescue si présent
                    for (Position rescuePos : Agent.RescueAgent.positions.values()) {
                        if (rescuePos != null && rescuePos.x == col && rescuePos.y == row && etat != EtatCase.VAISSEAU) {
                            imageView = createImageView("../image/AgentRescue.png");
                        }
                    }

                    for (Position researcherPos : Agent.ResearcherAgent.positions.values()) {
                        if (researcherPos != null && researcherPos.x == col && researcherPos.y == row && etat != EtatCase.VAISSEAU) {
                            imageView = createImageView("../image/Rescue.png");
                        }
                    }

                    cell.setStroke(Color.BLACK);
                    grid.add(cell, col, row);

                    if (imageView != null) {
                        grid.add(imageView, col, row);
                    }
                }
            }
        };

        // === première fois ===
        render.run();

        // === rafraîchissement toutes les 500ms ===
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> render.run()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Scene scene = new Scene(grid, carte.getWidth() * cellSize, carte.getHeight() * cellSize);
        stage.setTitle("Carte de la planète");
        stage.setScene(scene);
        stage.show();
    }
    private ImageView createImageView(String path) {
        try {
            Image image = new Image(getClass().getResourceAsStream(path));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(cellSize);  // ✅ Adapte à la largeur de la case
            imageView.setFitHeight(cellSize); // ✅ Adapte à la hauteur de la case
            imageView.setPreserveRatio(true); // ✅ Garde les proportions
            return imageView;
        } catch (Exception e) {
            System.err.println("Erreur de chargement de l'image : " + path);
            return null;
        }
    }

    // java
    private void printResourceDebug() {
        try {
            System.out.println("user.dir = " + System.getProperty("user.dir"));
            System.out.println("java.class.path = " + System.getProperty("java.class.path"));
            System.out.println("Class resource root = " + MapGUI.class.getResource("/"));

            System.out.println("MapGUI.getResource(\"../PierreNonIns.png\") = "
                    + MapGUI.class.getResource("../PierreNonIns.png"));
            System.out.println("MapGUI.getResource(\"/PierreNonIns.png\") = "
                    + MapGUI.class.getResource("/PierreNonIns.png"));
            System.out.println("MapGUI.getResource(\"/images/PierreNonIns.png\") = "
                    + MapGUI.class.getResource("/images/PierreNonIns.png"));

            System.out.println("ClassLoader.getResource(\"images/PierreNonIns.png\") = "
                    + Thread.currentThread().getContextClassLoader().getResource("images/PierreNonIns.png"));

            java.io.InputStream is = MapGUI.class.getResourceAsStream("/images/PierreNonIns.png");
            System.out.println("getResourceAsStream('/images/PierreNonIns.png') != null? " + (is != null));
            if (is != null) is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) {
        launch();
    }
}
