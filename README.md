# KISSSimulator

La librairie pour gérer les Joystick nécessite des drivers. Pour les inclure, il faut lancer la commande "./gradlew -q buildNativeLibDir" qui celon l'OS déposera les bons dans le répertoire "nativeLib".

Configurer les périphériques dans la méthode fr.griffon.KissSimulator.main

./gradlew run

Pour un projet sous IntelliJ, il faut ajouter dans la configuration de l'execution le paramètre VM options :-Djava.library.path=nativeLib/


WINDOWS :
En cas de pb avec la Taranis : http://liftoff-game.com/tutorials/Liftoff-Windows-10-Reinstall-Remote-USB-Driver.pdf