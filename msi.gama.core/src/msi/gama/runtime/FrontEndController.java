package msi.gama.runtime;

import java.util.concurrent.ArrayBlockingQueue;
import msi.gama.common.util.GuiUtils;
import msi.gama.kernel.experiment.IExperimentSpecies;
import msi.gama.kernel.model.IModel;
import msi.gama.outputs.OutputSynchronizer;
import msi.gama.runtime.exceptions.GamaRuntimeException;

public class FrontEndController implements Runnable {

	public final static String PAUSED = "STOPPED";
	public final static String RUNNING = "RUNNING";
	public final static String NOTREADY = "NOTREADY";
	public final static String NONE = "NONE";
	public static final int _INIT = 0;
	public static final int _START = 1;
	public static final int _STEP = 2;
	public static final int _PAUSE = 3;
	public static final int _STOP = 4;
	public static final int _CLOSE = 5;
	public static final int _RELOAD = 6;
	public static final int _NEXT = 7;

	public ISimulationStateProvider state = null;
	public volatile IExperimentSpecies experiment = null;
	protected volatile ArrayBlockingQueue<Integer> commands;
	public volatile Thread commandThread;
	protected volatile boolean running = true;
	public final FrontEndScheduler scheduler;

	public FrontEndController(FrontEndScheduler scheduler) {
		this.scheduler = scheduler;
		commands = new ArrayBlockingQueue(10);
		if ( GuiUtils.isInHeadLessMode() ) {
			commandThread = null;
		} else {
			commandThread = new Thread(this, "Front end controller");
			commandThread.start();
		}
	}

	@Override
	public void run() {
		while (running) {
			try {
				Integer i = commands.take();
				if ( i == null ) { throw new InterruptedException("Internal error. Please retry"); }
				processUserCommand(i);
			} catch (InterruptedException e) {}
		}
	}

	public void offer(int command) {
		if ( commandThread == null || !commandThread.isAlive() ) {
			processUserCommand(command);
		} else {
			commands.offer(command);
		}
	}

	protected void processUserCommand(final int command) {
		switch (command) {
			case _INIT:
				// GuiUtils.debug("FrontEndController.processUserCommand _INIT");
				updateSimulationState(NOTREADY);
				try {
					OutputSynchronizer.waitForViewsToBeClosed();
					experiment.schedule();
				} catch (GamaRuntimeException e) {
					closeExperiment(e);
				}
				// catch (Exception e) {
				// closeExperiment(new GamaRuntimeException(e));
				// }
				finally {
					updateSimulationState();
				}
				break;
			case _START:
				// GuiUtils.debug("FrontEndController.processUserCommand _START");
				try {
					scheduler.start();
				} catch (GamaRuntimeException e) {
					closeExperiment(e);
				} finally {
					updateSimulationState(RUNNING);
				}
				break;
			case _PAUSE:
				// GuiUtils.debug("FrontEndController.processUserCommand _PAUSE");
				updateSimulationState(PAUSED);
				scheduler.pause();
				break;
			case _STEP:
				// GuiUtils.debug("FrontEndController.processUserCommand _STEP");
				updateSimulationState(PAUSED);
				scheduler.stepByStep();
				break;
			case _RELOAD:
				// GuiUtils.debug("FrontEndController.processUserCommand _RELOAD");
				updateSimulationState(NOTREADY);
				try {
					boolean wasRunning = !scheduler.paused;
					scheduler.pause();
					GuiUtils.waitStatus("Reloading...");
					experiment.reload();
					if ( wasRunning ) {
						processUserCommand(_START);
					} else {
						GuiUtils.informStatus("Experiment reloaded");
					}
				} catch (GamaRuntimeException e) {
					closeExperiment(e);
				} catch (Exception e) {
					closeExperiment(GamaRuntimeException.create(e));
				} finally {
					updateSimulationState();
				}
				break;
		}
	}

	public void userPause() {
		// TODO Should maybe be done directly (so as to pause immediately)
		offer(_PAUSE);
	}

	public void userStep() {
		if ( experiment == null ) { return; }
		offer(_STEP);
	}

	public void userInit() {
		offer(_INIT);
	}

	public void userInterrupt() {
		if ( experiment != null ) {
			closeExperiment(GamaRuntimeException.warning("Interrupted by user"));
		}
	}

	public void userReload() {
		// TODO Should maybe be done directly (so as to pause immediately)

		if ( experiment == null ) { return; }
		offer(_RELOAD);
	}

	public void userStart() {
		offer(_START);
	}

	public void closeExperiment() {
		if ( experiment != null ) {
			try {
				updateSimulationState(NOTREADY);
				GuiUtils.closeDialogs();
				experiment.close();
				experiment = null;
			} finally {
				updateSimulationState(NONE);
			}
		}
	}

	public void startPause() {
		if ( experiment == null ) {
			return;
		} else if ( scheduler.paused ) {
			userStart();
		} else {
			userPause();
		}
	}

	public void closeExperiment(GamaRuntimeException e) {
		GuiUtils.errorStatus(e.getMessage());
		// GuiUtils.runtimeError(e);
		closeExperiment();
	}

	/**
	 * 
	 * Simulation state related utilities for Eclipse GUI
	 * 
	 */

	public String getFrontmostSimulationState() {
		if ( experiment == null ) {
			return NONE;
		} else if ( experiment.isLoading() ) {
			return NOTREADY;
		} else if ( scheduler.paused ) { return PAUSED; }
		return RUNNING;
	}

	public void updateSimulationState(final String forcedState) {
		if ( state != null ) {
			GuiUtils.run(new Runnable() {

				@Override
				public void run() {
					state.updateStateTo(forcedState);
				}
			});
		}
	}

	public void updateSimulationState() {
		updateSimulationState(getFrontmostSimulationState());
	}

	public void newExperiment(final String id, final IModel model) {
		final IExperimentSpecies newExperiment = model.getExperiment(id);
		if ( newExperiment == null ) { return; }
		GuiUtils.openSimulationPerspective();
		if ( newExperiment == experiment && experiment != null ) {
			userReload();
			return;
		}
		if ( experiment != null ) {
			IModel m = experiment.getModel();
			if ( !m.getFilePath().equals(model.getFilePath()) ) {
				if ( !verifyClose() ) { return; }
				closeExperiment();
				m.dispose();
			} else if ( !id.equals(experiment.getName()) ) {
				if ( !verifyClose() ) { return; }
				closeExperiment();
			} else {
				if ( !verifyClose() ) { return; }
				closeExperiment();
			}
		}
		experiment = newExperiment;
		experiment.open();
		userInit();
	}

	private boolean verifyClose() {
		if ( experiment == null ) { return true; }
		// TODO boolean wasRunning = !scheduler.paused;
		scheduler.pause();
		return GuiUtils.confirmClose(experiment);
	}

	public void shutdown() {
		scheduler.dispose();
		running = false;
	}

}