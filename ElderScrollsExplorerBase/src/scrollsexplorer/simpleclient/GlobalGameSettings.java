package scrollsexplorer.simpleclient;

import tools.WeakListenerList;

public class GlobalGameSettings {
	private static WeakListenerList<UpdateListener>	updateListeners			= new WeakListenerList<UpdateListener>();

	private static boolean							autoLoadLastGameConfig	= true;
	private static boolean							autoLoadLastCell		= true;
	private static boolean							isFreeFly				= false;
	private static boolean							enablePhysics			= true;
	private static boolean							slowPhysics				= false;

	public static interface UpdateListener {
		public void gameSettingsUpdated();
	}

	public static void addUpdateListener(UpdateListener updateListener) {
		GlobalGameSettings.updateListeners.add(updateListener);
	}

	public static void removeUpdateListener(UpdateListener updateListener) {
		GlobalGameSettings.updateListeners.remove(updateListener);
	}

	private static void fireUpdate() {
		for (UpdateListener updateListener : updateListeners) {
			updateListener.gameSettingsUpdated();
		}

	}

	public static void setAutoLoadLastGameConfig(boolean autoLoadLastGameConfig) {
		System.out.println("GlobalGameSettings.autoLoadLastGameConfig: " + autoLoadLastGameConfig);
		GlobalGameSettings.autoLoadLastGameConfig = autoLoadLastGameConfig;
		fireUpdate();
	}

	public static boolean isAutoLoadLastGameConfig() {
		return autoLoadLastGameConfig;
	}

	public static void setAutoLoadLastCell(boolean autoLoadLastCell) {
		System.out.println("GlobalGameSettings.autoLoadLastCell: " + autoLoadLastCell);
		GlobalGameSettings.autoLoadLastCell = autoLoadLastCell;
		fireUpdate();
	}

	public static boolean isAutoLoadLastCell() {
		return autoLoadLastCell;
	}

	public static void setIsFreeFly(boolean isFreeFly) {
		System.out.println("GlobalGameSettings.isFreeFly: " + isFreeFly);
		GlobalGameSettings.isFreeFly = isFreeFly;
		fireUpdate();
	}

	public static boolean isFreeFly() {
		return isFreeFly;
	}

	public static void setEnablePhysics(boolean enablePhysics) {
		System.out.println("GlobalGameSettings.enablePhysics: " + enablePhysics);
		GlobalGameSettings.enablePhysics = enablePhysics;
		fireUpdate();
	}

	public static boolean isEnablePhysics() {
		return enablePhysics;
	}

	public static void setSlowPhysics(boolean slowPhysics) {
		System.out.println("GlobalGameSettings.slowPhysics: " + slowPhysics);
		GlobalGameSettings.slowPhysics = slowPhysics;
		fireUpdate();
	}

	public static boolean isSlowPhysics() {
		return slowPhysics;
	}

}
