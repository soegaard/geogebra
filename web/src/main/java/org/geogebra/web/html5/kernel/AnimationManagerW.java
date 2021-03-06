package org.geogebra.web.html5.kernel;

import org.geogebra.common.kernel.AnimationManager;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.TimerListener;
import org.geogebra.web.html5.gawt.Timer;

public class AnimationManagerW extends AnimationManager implements
        HasTimerAction {
	private Timer timer;

	public AnimationManagerW(Kernel kernel2) {
		super(kernel2);
		timer = new Timer(1000 / MAX_ANIMATION_FRAME_RATE, this);
	}

	@Override
	public boolean isRunning() {
		return timer.isRunning();
	}

	@Override
	protected void setTimerDelay(int i) {
		timer.setDelay(i);
	}

	@Override
	protected void stopTimer() {
		timer.stop();
		for (TimerListener tl : listener) {
			tl.onTimerStopped();
		}
	}

	@Override
	protected void startTimer() {
		timer.start();
		for (TimerListener tl : listener) {
			tl.onTimerStarted();
		}
	}

	@Override
	public void actionPerformed() {
		sliderStep();
	}

}
