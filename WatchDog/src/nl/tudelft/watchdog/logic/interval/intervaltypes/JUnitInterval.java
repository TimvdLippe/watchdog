package nl.tudelft.watchdog.logic.interval.intervaltypes;

import java.util.Date;

import org.eclipse.jdt.junit.model.ITestElement;

import com.google.gson.annotations.SerializedName;

/**
 * Data object containing information on JUnit test runs. Contains a
 * {@link JUnitExecution} object, which is a tree data structure. Hence, every
 * {@link JUnitExecution} can contain any number of child {@link JUnitExecution}
 * s.
 */
public class JUnitInterval extends IntervalBase {

	/** Class version. */
	private static final long serialVersionUID = 1L;

	@SerializedName("je")
	private JUnitExecution testExecution;

	/**
	 * Constructor. JUnit intervals are by definition closed (or non-existent
	 * yet), as they report on finished JUnit executions.
	 */
	public JUnitInterval(ITestElement test) {
		super(IntervalType.JUNIT);
		isClosed = true;
		double duration = test.getElapsedTimeInSeconds();

		if (!Double.isNaN(duration)) {
			setEndTime(new Date());
			setStartTime(new Date(new Date().getTime()
					- roundElapsedTime(duration)));
		}
		testExecution = new JUnitExecution(test, null);
	}

	private long roundElapsedTime(double duration) {
		return Math.round(duration * 1000);
	}
}