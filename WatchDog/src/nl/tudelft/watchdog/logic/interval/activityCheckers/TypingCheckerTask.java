package nl.tudelft.watchdog.logic.interval.activityCheckers;

import java.util.TimerTask;

import nl.tudelft.watchdog.logic.eclipseuireader.events.DocumentActivateEvent;
import nl.tudelft.watchdog.logic.eclipseuireader.events.DocumentNotifier;
import nl.tudelft.watchdog.logic.exceptions.ContentReaderException;
import nl.tudelft.watchdog.logic.exceptions.EditorClosedPrematurelyException;
import nl.tudelft.watchdog.logic.logging.WDLogger;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/** A task for checking whether the user is typing. */
public class TypingCheckerTask extends TimerTask {

	/** An update checker. */
	private IUpdateChecker checker;

	/** Callback. */
	private OnInactiveCallBack callback;

	/** The editor. */
	private ITextEditor editor;

	/** The workbenchPart this editor belongs to. */
	private IWorkbenchPart workbenchPart;

	/** Constructor. */
	public TypingCheckerTask(IWorkbenchPart part, OnInactiveCallBack callback) {
		this.editor = (ITextEditor) part;
		this.workbenchPart = part;
		this.checker = new EditorContentChangedChecker(editor);
		this.callback = callback;
	}

	@Override
	public void run() {
		try {
			if (!checker.hasChanged()) {
				// not an active document any longer, as no changes had been
				// made.
				// (1) stop the timer
				cancel();
				// (2) listen to changes in open, inactive documents
				createListenerForReactivation();
				callback.onInactive();
			}
		} catch (EditorClosedPrematurelyException e) {
			// this can happen when eclipse is closed while the document is
			// still active
			WDLogger.logInfo("Editor closed prematurely");
		} catch (ContentReaderException e) {
			// this can happen when a file is moved inside the workspace
			WDLogger.logInfo("Unavailable doc provider");
		}
	}

	/** Adds listeners to document such that it is ready to be called again. */
	public void createListenerForReactivation() {
		IDocumentProvider documentProvider = editor.getDocumentProvider();
		final IDocument document = documentProvider.getDocument(editor
				.getEditorInput());

		final IDocumentListener docListener = new IDocumentListener() {

			@Override
			public void documentChanged(DocumentEvent event) {
				// listen to this event just once, notify that the document is
				// activated, then remove this listener
				DocumentNotifier
						.fireDocumentStartEditingEvent(new DocumentActivateEvent(
								workbenchPart));
				document.removeDocumentListener(this);
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}
		};

		document.addDocumentListener(docListener);
	}
}