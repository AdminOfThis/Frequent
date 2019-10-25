package main;

import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import version.Version;

public class CheckVersion {

	public static void main(String[] args) {
		checkVersion(args);
	}

	public static boolean checkVersion(String[] args) {
		boolean success = false;
		try {
			if (args.length == 2) {
				String startPath = args[1];
				startPath = startPath.replaceAll("\"", "");
//				startPath = "dir";

				Version neededVersion = neededVersion(args);
				Version javaVersion = javaVersion();

				if (neededVersion.compareTo(javaVersion) <= 0) {
					System.out.println("Version check successfull, java version " + javaVersion.get() + " is newer than required version " + neededVersion.get());
					if (!startPath.isEmpty()) {
						System.out.println("Executing command: \"" + startPath + "\"");
						ProcessBuilder pb = new ProcessBuilder(startPath.split(" "));
						File workingDir = new File(".").getAbsoluteFile();
						pb.directory(workingDir);
						pb.start();
					}
					success = true;
				} else {
					String errorMessage = "The detected java version (" + javaVersion.get()
							+ ") was too old to run the application. Please install a Java runtime environment with version 1.8 or newer.";
					System.err.println(errorMessage);
					showError(errorMessage);
				}
			} else {
				String errorMessage = "Wrong number of arguments, please insert needed java version as first argument, and start command as the second";
				System.out.println(errorMessage);
				showError(errorMessage);

			}
		} catch (Exception e) {
			String errorMesage = "Unable to check for correct version: ";
			System.err.println(errorMesage);
			showError(errorMesage);
		} catch (Error e) {
			String errorMesage = "Unable to check for correct version: ";
			System.err.println(errorMesage);
			showError(errorMesage);
		}
		return success;
	}

	private static Version neededVersion(String[] args) throws Exception {
		String neededVersion = args[0];
		neededVersion = neededVersion.split("_")[0];
		return new Version(neededVersion);
	}

	private static Version javaVersion() throws Exception {
		String javaVersion = System.getProperty("java.version");
		javaVersion = javaVersion.split("_")[0];
		return new Version(javaVersion);
	}

	private static void showError(final String message) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				JOptionPane.showMessageDialog(null, message);
			}
		});
	}
}
