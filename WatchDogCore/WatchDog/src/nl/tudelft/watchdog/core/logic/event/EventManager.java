package nl.tudelft.watchdog.core.logic.event;

import nl.tudelft.watchdog.core.logic.event.eventtypes.EventBase;
import nl.tudelft.watchdog.core.util.WatchDogLogger;

/**
 * Base class for managing events generated by the IDE. Contains functionality
 * for adding events.
 */
public class EventManager {

	/**
	 * The session seed, a random number generated on each instantiation of the
	 * {@link IntervalManager } to be able to tell running Eclipse instances
	 * apart. So, the session seed used by the EventManager equals the one from
	 * the IntervalManager to be able to connect events and intervals of the
	 * same session.
	 */
	private String sessionSeed;

	private EventPersisterBase eventsToTransferPersister;
	private EventPersisterBase eventsStatisticsPersister;

	/** Constructor. */
	public EventManager(EventPersisterBase eventsToTransferPersister,
			EventPersisterBase eventsStatisticsPersister) {
		this.eventsToTransferPersister = eventsToTransferPersister;
		this.eventsStatisticsPersister = eventsStatisticsPersister;
	}

	/** Sets the session seed used by this EventManager. */
	public void setSessionSeed(String sessionSeed) {
		this.sessionSeed = sessionSeed;
	}

	/**
	 * Saves the supplied event to persistent storage. New events must use this
	 * method to be registered properly.
	 */
	public void addEvent(EventBase event) {
		if (event != null) {
			event.setSessionSeed(sessionSeed);
			eventsToTransferPersister.saveItem(event);
			eventsStatisticsPersister.saveItem(event);
			WatchDogLogger.getInstance().logInfo("Created event " + event + " " + event.getType());
		}

	}
}
