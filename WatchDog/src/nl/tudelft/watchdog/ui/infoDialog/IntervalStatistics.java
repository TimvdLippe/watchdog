package nl.tudelft.watchdog.ui.infoDialog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.tudelft.watchdog.logic.document.DocumentType;
import nl.tudelft.watchdog.logic.interval.IntervalManagerBase;
import nl.tudelft.watchdog.logic.interval.IntervalPersister;
import nl.tudelft.watchdog.logic.interval.intervaltypes.IntervalBase;
import nl.tudelft.watchdog.logic.interval.intervaltypes.IntervalType;

import org.joda.time.DateTime;
import org.joda.time.Duration;

/** Gathers and calculates statistics on interval length. */
public class IntervalStatistics extends IntervalManagerBase {
	private IntervalPersister intervalPersister;

	private Duration eclipseOpen;
	private Duration userActive;
	private Duration userReading;
	private Duration userTyping;
	private Duration userProduction;
	private Duration userTesting;

	/** Constructor. */
	public IntervalStatistics(IntervalPersister intervalPersister) {
		this.intervalPersister = intervalPersister;

		intervals.addAll(intervalPersister.readIntervals());
		filterIntervals();
		calculateStatistics();
	}

	private void calculateStatistics() {
		eclipseOpen = aggregateDurations(getIntervalsOfType(IntervalType.ECLIPSE_OPEN));
		userActive = aggregateDurations(getIntervalsOfType(IntervalType.USER_ACTIVE));
		userReading = aggregateDurations(getIntervalsOfType(IntervalType.READING));
		userTyping = aggregateDurations(getIntervalsOfType(IntervalType.TYPING));
		userTesting = aggregateDurations(getEditorIntervalsOfDocType(DocumentType.TEST));
		userProduction = aggregateDurations(getEditorIntervalsOfDocType(DocumentType.PRODUCTION));
	}

	/** Filters out and removes intervals which are older than one hour. */
	private void filterIntervals() {
		ArrayList<IntervalBase> filteredIntervals = new ArrayList<IntervalBase>();
		ArrayList<IntervalBase> intervalsToRemove = new ArrayList<IntervalBase>();

		Date mostRecentDate = intervals.get(intervals.size() - 1).getEnd();
		DateTime thresholdDate = new DateTime(mostRecentDate);
		thresholdDate = thresholdDate.minusHours(1);

		for (IntervalBase interval : intervals) {
			if (interval.getEnd().before(thresholdDate.toDate())) {
				intervalsToRemove.add(interval);
			} else {
				filteredIntervals.add(interval);
			}
		}

		intervalPersister.removeIntervals(intervalsToRemove);
		intervals = filteredIntervals;
	}

	private Duration aggregateDurations(List<IntervalBase> intervals) {
		Duration aggregatedDuration = new Duration(0);
		for (IntervalBase interval : intervals) {
			aggregatedDuration = aggregatedDuration
					.plus(interval.getDuration());
		}
		return aggregatedDuration;
	}
}
