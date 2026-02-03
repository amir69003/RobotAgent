# Projet JADE + JavaFX

Ce projet combine une simulation **multi-agents** avec **JADE** ainsi qu’une **interface graphique JavaFX** permettant de visualiser la carte et le comportement des agents.

## Installation de JADE (si nécessaire)
Télécharger `jade.jar` puis l’installer dans le dépôt Maven local :
```bash
mvn install:install-file -Dfile=jade.jar -DgroupId=com.tilab -DartifactId=jade -Dversion=4.5.0 -Dpackaging=jar
```
## Lancer depuis robotAgent 
```bash
mvn clean javafx:run
```

