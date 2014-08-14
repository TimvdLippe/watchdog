package nl.tudelft.watchdog.logic.ui;

import nl.tudelft.watchdog.logic.interval.IntervalManager;
import nl.tudelft.watchdog.logic.interval.intervaltypes.EditorIntervalBase;
import nl.tudelft.watchdog.logic.interval.intervaltypes.IntervalBase;
import nl.tudelft.watchdog.logic.interval.intervaltypes.IntervalType;
import nl.tudelft.watchdog.logic.interval.intervaltypes.JUnitInterval;
import nl.tudelft.watchdog.logic.interval.intervaltypes.PerspectiveInterval;
import nl.tudelft.watchdog.logic.interval.intervaltypes.PerspectiveInterval.Perspective;
import nl.tudelft.watchdog.logic.interval.intervaltypes.ReadingInterval;
import nl.tudelft.watchdog.logic.logging.WatchDogLogger;
import nl.tudelft.watchdog.logic.ui.WatchDogEvent.EventType;

import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Manager for {@link EditorEvent}s. Links such events to actions in the
 * IntervalManager, i.e. manages the creation and deletion of intervals based on
 * the incoming events. This class therefore contains the logic of when and how
 * new intervals are created, and how WatchDog reacts to incoming events
 * generated by its listeners.
 */
public class EventManager {

	private static final int USER_ACTIVITY_TIMEOUT = 16000;

	/** The {@link IntervalManager} this observer is working with. */
	private IntervalManager intervalManager;

	private InactivityNotifier userInactivityNotifier;

	private InactivityNotifier editorInactivityNotifier;

	/** Constructor. */
	public EventManager(IntervalManager intervalManager) {
		this.intervalManager = intervalManager;
		userInactivityNotifier = new InactivityNotifier(this,
				USER_ACTIVITY_TIMEOUT, EventType.USER_INACTIVITY);
		editorInactivityNotifier = new InactivityNotifier(this,
				USER_ACTIVITY_TIMEOUT, EventType.EDITOR_INACTIVITY);
	}

	/** Introduces the supplied editorEvent */
	public void update(WatchDogEvent event) {

		IntervalBase interval;
		switch (event.getType()) {
		case START_ECLIPSE:
			intervalManager.addInterval(new IntervalBase(
					IntervalType.ECLIPSE_OPEN));
			userInactivityNotifier.trigger();
			break;

		case END_ECLIPSE:
			intervalManager.closeAllIntervals();
			break;

		case ACTIVE_WINDOW:
			intervalManager.addInterval(new IntervalBase(
					IntervalType.ECLIPSE_ACTIVE));
			userInactivityNotifier.trigger();
			break;

		case END_WINDOW:
			interval = intervalManager
					.getIntervalOfType(IntervalType.ECLIPSE_ACTIVE);
			intervalManager.closeInterval(interval);
			userInactivityNotifier.cancelTimer();
			break;

		case START_JAVA_PERSPECTIVE:
			createNewPerspectiveInterval(Perspective.JAVA);
			userInactivityNotifier.trigger();
			break;

		case START_DEBUG_PERSPECTIVE:
			createNewPerspectiveInterval(Perspective.DEBUG);
			userInactivityNotifier.trigger();
			break;

		case START_UNKNOWN_PERSPECTIVE:
			createNewPerspectiveInterval(Perspective.OTHER);
			userInactivityNotifier.trigger();
			break;

		case JUNIT:
			JUnitInterval junitInterval = (JUnitInterval) event.getSource();
			intervalManager.addInterval(junitInterval);
			break;

		case ACTIVITY:
			userInactivityNotifier.trigger();
			interval = intervalManager
					.getIntervalOfType(IntervalType.USER_ACTIVE);
			if (interval == null) {
				intervalManager.addInterval(new IntervalBase(
						IntervalType.USER_ACTIVE));
			}
			break;

		case USER_INACTIVITY:
			interval = intervalManager
					.getIntervalOfType(IntervalType.USER_ACTIVE);
			intervalManager.closeInterval(interval);
			editorInactivityNotifier.cancelTimer();
			break;

		case EDIT:
			ITextEditor editor = (ITextEditor) event.getSource();
			// something like this:
			// WatchDogUtils.getEditorContent(editor))

			// editorInterval = intervalManager.getEditorInterval();
			// if (editorInterval.getActivityType() == IntervalType.READING
			// || editorInterval.getEditor() != editor.getEditorSite()) {
			// // intervalManager.closeInterval(editorInterval1);
			// }
			break;

		case PAINT:
		case CARET_MOVED:
		case ACTIVE_FOCUS:
			editor = (ITextEditor) event.getSource();
			EditorIntervalBase editorInterval = intervalManager
					.getEditorInterval();
			if (editorInterval == null) {
				intervalManager.addAndSetEditorInterval(new ReadingInterval(
						editor));
			}
			editorInactivityNotifier.trigger();
			break;

		case END_FOCUS:
			editorInactivityNotifier.cancelTimer();
			intervalManager.closeInterval(intervalManager.getEditorInterval());
			break;

		case EDITOR_INACTIVITY:
			intervalManager.closeInterval(intervalManager.getEditorInterval());
			break;

		default:
			break;
		}

		WatchDogLogger.getInstance().logInfo("Event " + event.getType() + " ");
	}

	/** Creates a new perspective Interval of the given type. */
	private void createNewPerspectiveInterval(
			PerspectiveInterval.Perspective perspecitveType) {
		PerspectiveInterval perspectiveInterval = (PerspectiveInterval) intervalManager
				.getIntervalOfType(IntervalType.PERSPECTIVE);
		if (perspectiveInterval != null
				&& perspectiveInterval.getPerspectiveType() == perspecitveType) {
			// abort if such an interval is already open.
			return;
		}
		intervalManager.closeInterval(perspectiveInterval);
		intervalManager.addInterval(new PerspectiveInterval(perspecitveType));
	}

}