package nl.tudelft.watchdog.exceptions;

@SuppressWarnings("serial")
public class FileSavingFailedException extends Exception {

	public FileSavingFailedException() {
		super();
	}

	public FileSavingFailedException(String message) {
		super(message);
	}

	public FileSavingFailedException(Throwable e) {
		super(e);
	}
}