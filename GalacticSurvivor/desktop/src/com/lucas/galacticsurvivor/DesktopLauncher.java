package com.lucas.galacticsurvivor;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.lucas.galacticsurvivor.GalacticSurvivorGame;

// Please note that on macOS your application needs to be started with the -XstartOnFirstThread JVM argument
public class DesktopLauncher {
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(720, 1280);
		config.setForegroundFPS(60);
		config.setTitle("Galactic Survivor");
		new Lwjgl3Application(new GalacticSurvivorGame(), config);
	}
}
