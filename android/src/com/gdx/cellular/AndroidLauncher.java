package com.gdx.cellular;

import android.os.Build;
import android.os.Bundle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.gdx.cellular.CellularAutomaton;

public class AndroidLauncher extends AndroidApplication {
	private CellularAutomaton automaton;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		automaton = new CellularAutomaton();
		initialize(automaton, config);

		// API 33+ routes Back through OnBackInvokedDispatcher. Keep those Android
		// classes isolated in a nested helper so API 26-32 never load them.
		if (Build.VERSION.SDK_INT >= 33) {
			Api33Back.register(this, this::dispatchBackToGame);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onBackPressed() {
		if (Build.VERSION.SDK_INT >= 33) {
			// The API 33+ dispatcher is the normal path. Retain this fallback
			// for hardware/OEM paths that still invoke Activity.onBackPressed.
			dispatchBackToGame();
			return;
		}
		dispatchBackToGame();
	}

	private void dispatchBackToGame() {
		if (Gdx.app == null || automaton == null) {
			finish();
			return;
		}
		Gdx.app.postRunnable(() -> {
			boolean handled = automaton.handleAndroidBack();
			if (!handled) {
				runOnUiThread(this::finish);
			}
		});
	}

	private static final class Api33Back {
		private Api33Back() {
		}

		static void register(AndroidLauncher launcher, Runnable action) {
			launcher.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
					android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
					action::run);
		}
	}
}
