package nl.tudelft.watchdog.eclipse.ui;

import java.awt.Color;
import java.awt.Paint;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.gantt.GanttCategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.util.Rotation;

import nl.tudelft.watchdog.core.logic.interval.IntervalStatisticsBase.StatisticsTimePeriod;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.DebugInterval;
import nl.tudelft.watchdog.core.ui.util.DebugEventVisualizationUtils;
import nl.tudelft.watchdog.core.util.WatchDogGlobals;
import nl.tudelft.watchdog.eclipse.logic.InitializationManager;
import nl.tudelft.watchdog.eclipse.logic.event.EventStatistics;
import nl.tudelft.watchdog.eclipse.logic.interval.IntervalStatistics;
import nl.tudelft.watchdog.eclipse.logic.ui.listeners.WatchDogViewListener;
import nl.tudelft.watchdog.eclipse.ui.util.UIUtils;

/** A view displaying all the statistics that WatchDog has gathered. */
public class WatchDogView extends ViewPart {
	private static final float FOREGROUND_TRANSPARENCY = 0.8f;

	/** The Id of the view. */
	public static final String ID = "WatchDog.view";

	private IntervalStatistics intervalStatistics;
	private EventStatistics eventStatistics;

	private Composite parent;

	private double eclipseOpen;
	private double userActive;
	private double userReading;
	private double userTyping;
	private double userProduction;
	private double userTest;
	private double userActiveRest;
	private double perspectiveDebug;
	private double perspectiveJava;
	private double perspectiveOther;
	private double averageTestDurationMinutes;
	private double averageTestDurationSeconds;

	private int junitRunsCount;
	private int junitFailuresCount;
	private int junitSuccessCount;

	private int debuggingSessionCount;
	private double totalDebuggingTime;
	private double debuggingTimePercentage;
	private double averageDebuggingTime;

	private StatisticsTimePeriod selectedTimePeriod = StatisticsTimePeriod.HOUR_1;

	private DebugInterval selectedDebugInterval;
	private List<DebugInterval> latestDebugIntervals;
	private static final int NUMBER_OF_INTERVALS_TO_SHOW = 10;

	private ScrolledComposite scrolledComposite;
	private Composite oneColumn;
	private Composite intervalSelection;

	private IPartService partService;

	private WatchDogViewListener watchDogViewListener;

	/** Updates the view by completely repainting it. */
	public void update() {
		scrolledComposite.dispose();
		createPartControl(parent);
		parent.update();
		parent.layout();
	}

	@Override
	public void createPartControl(Composite parent) {
		partService = (IPartService) getSite().getService(IPartService.class);
		watchDogViewListener = new WatchDogViewListener(this);
		partService.addPartListener(watchDogViewListener);

		this.parent = parent;

		scrolledComposite = new ScrolledComposite(parent,
				SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setLayout(new FillLayout());
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = scrolledComposite.getClientArea();
				scrolledComposite.setMinSize(
						oneColumn.computeSize(r.width, SWT.DEFAULT));
			}
		});

		oneColumn = UIUtils.createGridedComposite(scrolledComposite, 1);
		scrolledComposite.setContent(oneColumn);

		if (!WatchDogGlobals.isActive) {
			createInactiveViewContent();
		} else {
			calculateTimes();
			latestDebugIntervals = intervalStatistics
					.getLatestDebugIntervals(NUMBER_OF_INTERVALS_TO_SHOW);
			if (selectedDebugIntervalShouldBeReset()) {
				selectedDebugInterval = !latestDebugIntervals.isEmpty()
						? latestDebugIntervals.get(0) : null;
			}
			createActiveView();
		}

		// Always create refresh link, even when statistics are not shown
		createRefreshLink();
	}

	/**
	 * @return true if and only if one of the following two conditions hold:
	 * 
	 *         1. No debug interval has been selected yet; or 2. A debug
	 *         interval has been selected before, but it is no longer part of
	 *         the latest debug intervals.
	 */
	private boolean selectedDebugIntervalShouldBeReset() {
		return (selectedDebugInterval == null
				|| !latestDebugIntervals.contains(selectedDebugInterval));
	}

	private void createInactiveViewContent() {
		UIUtils.createBoldLabel("WatchDog is not active in this workspace! \n",
				oneColumn);
		UIUtils.createLabel(
				"Therefore we cannot show you any cool test statistics. \nTo get them, click the WatchDog icon and enable WatchDog.",
				oneColumn);
		intervalSelection = UIUtils.createZeroMarginGridedComposite(oneColumn,
				3);
	}

	private void createActiveView() {
		// General section.
		UIUtils.createTitleLabel("General", oneColumn);
		Composite generalSectionContainer = UIUtils
				.createGridedComposite(oneColumn, 2);
		generalSectionContainer
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createSWTChart(generalSectionContainer,
				createBarChart(createDevelopmentBarDataset(),
						"Your Development Activity", "", "minutes"));
		createSWTChart(generalSectionContainer, createPieChart(
				createDevelopmentPieDataset(), "Your Development Activity"));
		createSWTChart(generalSectionContainer,
				createPieChart(createPerspectiveViewPieDataset(),
						"Your Perspective Activity"));

		// Testing section.
		UIUtils.createTitleLabel("Testing", oneColumn);
		Composite testingSectionContainer = UIUtils
				.createGridedComposite(oneColumn, 2);
		testingSectionContainer
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createSWTChart(testingSectionContainer,
				createBarChart(createProductionVSTestBarDataset(),
						"Your Production vs. Test Activity", "", "minutes"));
		createSWTChart(testingSectionContainer,
				createPieChart(createProductionVSTestPieDataset(),
						"Your Production vs. Test Activity"));
		createSWTChart(testingSectionContainer,
				createStackedBarChart(createJunitExecutionBarDataset(),
						"Your Test Run Activity", "", ""));

		// Debugging section.
		if (selectedDebugInterval != null) {
			UIUtils.createTitleLabel("Debugging", oneColumn);
			Composite debugSectionContainer = UIUtils
					.createGridedComposite(oneColumn, 2);
			debugSectionContainer.setLayoutData(
					new GridData(SWT.FILL, SWT.FILL, true, true));
			createSWTChart(debugSectionContainer, createDebugEventGanttChart());
			createDebugStatisticsLabels(
					UIUtils.createGridedComposite(debugSectionContainer, 1));
		}

		// Controls.
		createShowingStatisticsLine();
		createTimeSpanSelectionList();
		UIUtils.createStartDebugSurveyLink(oneColumn);
	}

	private void createDebugStatisticsLabels(Composite container) {
		UIUtils.createLabel(
				"Number of debugging intervals in the selected period: "
						+ debuggingSessionCount,
				container);
		UIUtils.createLabel(
				String.format(
						"Time spent in debugger: %.2f minutes (%.2f%% of active IDE time)",
						totalDebuggingTime, debuggingTimePercentage),
				container);
		UIUtils.createLabel(
				String.format("Average debugging session length: %.2f seconds",
						60 * averageDebuggingTime),
				container);
		createDebugIntervalSelectionList(container);
	}

	private JFreeChart createDebugEventGanttChart() {
		eventStatistics = new EventStatistics(
				InitializationManager.getInstance().getDebugEventManager(),
				selectedDebugInterval);
		GanttCategoryDataset dataset = eventStatistics
				.createDebugEventGanttChartDataset();

		JFreeChart chart = ChartFactory.createGanttChart(
				"Debug Events During Selected Debug Interval", "Event", "Time",
				dataset, false, true, false);

		// Scale the chart based on the selected debug interval.
		CategoryPlot plot = chart.getCategoryPlot();
		ValueAxis axis = plot.getRangeAxis();
		axis.setRangeWithMargins(
				selectedDebugInterval.getStart().getTime()
						- EventStatistics.PRE_SESSION_TIME_TO_INCLUDE,
				selectedDebugInterval.getEnd().getTime());

		// Give each event type a different color.
		plot.setRenderer(new WatchDogGanttRenderer());
		return chart;
	}

	private class WatchDogGanttRenderer extends GanttRenderer {

		private static final long serialVersionUID = 1L;

		public WatchDogGanttRenderer() {
			super();
			this.setShadowVisible(false);
		}

		public Paint getItemPaint(int row, int column) {
			return DebugEventVisualizationUtils.getColorForNumber(column);
		}
	}

	private void createShowingStatisticsLine() {
		Composite lineComposite = UIUtils
				.createZeroMarginGridedComposite(oneColumn, 3);
		UIUtils.createLabel("Showing statistics from "
				+ intervalStatistics.oldestDate + " to "
				+ intervalStatistics.mostRecentDate + " ("
				+ intervalStatistics.getNumberOfIntervals() + " intervals).",
				lineComposite);

		UIUtils.createLabel("Not enough statistics for you?", lineComposite);
		UIUtils.createOpenReportLink(lineComposite);
	}

	private void createTimeSpanSelectionList() {
		intervalSelection = UIUtils.createZeroMarginGridedComposite(oneColumn,
				3);
		UIUtils.createLabel("Show statistics of the past ", intervalSelection);
		UIUtils.createComboList(intervalSelection, new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Combo widget = (Combo) e.getSource();
				selectedTimePeriod = StatisticsTimePeriod.values()[widget
						.getSelectionIndex()];
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}

		}, StatisticsTimePeriod.names(), selectedTimePeriod.ordinal());
	}

	private void createDebugIntervalSelectionList(Composite parent) {
		Composite debugIntervalSelection = UIUtils
				.createZeroMarginGridedComposite(parent, 3);
		UIUtils.createLabel("Show debug events for debug interval ",
				debugIntervalSelection);
		UIUtils.createComboList(debugIntervalSelection,
				new SelectionListener() {

					@Override
					public void widgetSelected(SelectionEvent e) {
						Combo widget = (Combo) e.getSource();
						selectedDebugInterval = latestDebugIntervals
								.get(widget.getSelectionIndex());
						update();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
					}

				},
				DebugEventVisualizationUtils
						.getDebugIntervalStrings(latestDebugIntervals),
				latestDebugIntervals.indexOf(selectedDebugInterval));
	}

	private void createRefreshLink() {
		UIUtils.createLinkedLabel(intervalSelection, new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedDebugInterval = null;
				update();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		}, "Refresh.", "");
	}

	private void calculateTimes() {
		intervalStatistics = new IntervalStatistics(
				InitializationManager.getInstance().getIntervalManager(),
				selectedTimePeriod);

		eclipseOpen = intervalStatistics
				.getPreciseTime(intervalStatistics.ideOpen);
		userActive = intervalStatistics
				.getPreciseTime(intervalStatistics.userActive);
		userReading = intervalStatistics
				.getPreciseTime(intervalStatistics.userReading);
		userTyping = intervalStatistics
				.getPreciseTime(intervalStatistics.userTyping);
		userProduction = intervalStatistics
				.getPreciseTime(intervalStatistics.userProduction);
		userTest = intervalStatistics
				.getPreciseTime(intervalStatistics.userTest);
		userActiveRest = userActive - userReading - userTyping;
		perspectiveDebug = intervalStatistics
				.getPreciseTime(intervalStatistics.perspectiveDebug);
		perspectiveJava = intervalStatistics
				.getPreciseTime(intervalStatistics.perspectiveJava);
		perspectiveOther = intervalStatistics
				.getPreciseTime(intervalStatistics.perspectiveOther);
		averageTestDurationMinutes = intervalStatistics.averageTestDuration;
		averageTestDurationSeconds = averageTestDurationMinutes * 60;

		junitRunsCount = intervalStatistics.junitRunsCount;
		junitSuccessCount = intervalStatistics.junitSuccessfulRunsCount;
		junitFailuresCount = intervalStatistics.junitFailedRunsCount;

		debuggingSessionCount = intervalStatistics.debuggingSessionCount;
		totalDebuggingTime = intervalStatistics
				.getPreciseTime(intervalStatistics.totalDebuggingDuration);
		debuggingTimePercentage = totalDebuggingTime / userActive;
		averageDebuggingTime = intervalStatistics
				.getPreciseTime(intervalStatistics.averageDebuggingDuration);
	}

	private void createSWTChart(Composite container, JFreeChart chart) {
		ChartComposite chartComposite = new ChartComposite(container, SWT.NONE,
				chart, true);
		chartComposite.setLayoutData(new GridData(
				(int) Math.floor(parent.getSize().x / 2.0) - 20, 400));
		Rectangle bounds = chartComposite.getBounds();
		bounds.height = bounds.width;
		chartComposite.setBounds(bounds);
	}

	private DefaultCategoryDataset createDevelopmentBarDataset() {
		DefaultCategoryDataset result = new DefaultCategoryDataset();
		result.setValue(userReading, "1", "Reading");
		result.setValue(userTyping, "1", "Writing");
		result.setValue(userActive, "1", "User Active");
		result.setValue(eclipseOpen, "1", "Eclipse Open");
		return result;
	}

	private PieDataset createDevelopmentPieDataset() {
		double divisor = userReading + userTyping + userActiveRest;
		DefaultPieDataset result = new DefaultPieDataset();
		result.setValue("Reading" + printPercent(userReading, divisor),
				userReading);
		result.setValue("Writing" + printPercent(userTyping, divisor),
				userTyping);
		result.setValue(
				"Other activities" + printPercent(userActiveRest, divisor),
				userActiveRest);
		return result;
	}

	private DefaultCategoryDataset createProductionVSTestBarDataset() {
		DefaultCategoryDataset result = new DefaultCategoryDataset();
		result.setValue(userProduction, "1", "Production Code");
		result.setValue(userTest, "1", "Test Code");
		return result;
	}

	private PieDataset createProductionVSTestPieDataset() {
		double divisor = userProduction + userTest;
		DefaultPieDataset result = new DefaultPieDataset();
		result.setValue(
				"Production Code" + printPercent(userProduction, divisor),
				userProduction);
		result.setValue("Test Code" + printPercent(userTest, divisor),
				userTest);
		return result;
	}

	private JFreeChart createPieChart(final PieDataset dataset, String title) {
		JFreeChart chart = ChartFactory.createPieChart3D(title, dataset, true,
				true, false);
		PiePlot3D plot = (PiePlot3D) chart.getPlot();
		plot.setDirection(Rotation.CLOCKWISE);
		plot.setForegroundAlpha(FOREGROUND_TRANSPARENCY);
		return chart;
	}

	private JFreeChart createBarChart(final DefaultCategoryDataset dataset,
			String title, String xAxisName, String yAxisName) {
		JFreeChart chart = ChartFactory.createBarChart3D(title, xAxisName,
				yAxisName, dataset);
		setLegendInvisible(chart);
		return chart;
	}

	private void setLegendInvisible(JFreeChart chart) {
		LegendTitle legend = chart.getLegend();
		if (legend != null) {
			legend.setVisible(false);
		}
	}

	private JFreeChart createStackedBarChart(CategoryDataset dataset,
			String title, String xAxisName, String yAxisName) {
		JFreeChart chart = ChartFactory.createStackedBarChart3D(title,
				xAxisName, yAxisName, dataset);
		setLegendInvisible(chart);

		CategoryPlot plot = chart.getCategoryPlot();
		CategoryItemRenderer renderer = plot.getRenderer();
		renderer.setSeriesPaint(0, makeColorTransparent(Color.green));
		renderer.setSeriesPaint(1, makeColorTransparent(Color.red));
		renderer.setSeriesPaint(2, makeColorTransparent(Color.blue));
		return chart;
	}

	/**
	 * Takes a color and modifies its alpha channel to give it (roughly) the
	 * same transparency level that other JFreeCharts have per default, or that
	 * can be set using {@link Plot#setForegroundAlpha(float)}. Workaround for
	 * StackedBarCharts, where the above mentioned does not work.
	 */
	private Color makeColorTransparent(Color color) {
		int adjustedTransparency = (int) Math
				.round(FOREGROUND_TRANSPARENCY * 0.6 * 255);
		return new Color(color.getRed(), color.getGreen(), color.getBlue(),
				adjustedTransparency);
	}

	private PieDataset createPerspectiveViewPieDataset() {
		double divisor = perspectiveDebug + perspectiveJava + perspectiveOther;
		DefaultPieDataset result = new DefaultPieDataset();
		result.setValue("Java" + printPercent(perspectiveJava, divisor),
				perspectiveJava);
		result.setValue("Debug" + printPercent(perspectiveDebug, divisor),
				perspectiveDebug);
		result.setValue("Other" + printPercent(perspectiveOther, divisor),
				perspectiveOther);
		return result;
	}

	private CategoryDataset createJunitExecutionBarDataset() {
		double differenceSeconds = Math
				.abs(averageTestDurationSeconds - junitRunsCount);
		double differenceMinutes = Math
				.abs(averageTestDurationMinutes - junitRunsCount);

		String testDurationTitle = "Test Run Duration";
		double testDuration;
		if (differenceSeconds < differenceMinutes) {
			testDuration = averageTestDurationSeconds;
			testDurationTitle += " (in seconds)";
		} else {
			testDuration = averageTestDurationMinutes;
			testDurationTitle += " (in minutes)";
		}

		String[] rows = new String[] { "Successful", "Failed", "Both" };
		String[] columns = new String[] { "Test Runs", testDurationTitle };
		double[][] data = new double[][] { { junitSuccessCount, 0 },
				{ junitFailuresCount, 0 }, { 0, testDuration } };
		CategoryDataset dataSet = DatasetUtilities.createCategoryDataset(rows,
				columns, data);

		return dataSet;
	}

	private String printPercent(double dividend, double divisor) {
		if (divisor == 0) {
			return " (--)";
		}
		return " (" + String.format("%.1f", dividend * 100 / divisor) + "%)";
	}

	@Override
	public void setFocus() {
		parent.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		partService.removePartListener(watchDogViewListener);
	}
}
