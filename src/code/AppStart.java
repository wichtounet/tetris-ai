package code;

import static code.ProjectConstants.formatStackTrace;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/*
 * Class that starts the app!
 */
public class AppStart {
    /*
     * Errors go to console if true, otherwise go to GUI logger.
     */
    public static final boolean REPORT_TO_CONSOLE = true;

    public static void main(String... args) {
        System.setErr(System.out);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable t) {
        }

        //Better exception catching.
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        if (REPORT_TO_CONSOLE) {
                            e.printStackTrace();
                        } else {
                            JOptionPane.showMessageDialog(null,
                                    "Java version: " + System.getProperty("java.version")
                                    + "\nOperating System: " + System.getProperty("os.name")
                                    + "\nFatal exception in thread: " + t.getName()
                                    + "\nException: " + e.getClass().getName()
                                    + "\nReason given: " + e.getMessage()
                                    + "\n\n" + formatStackTrace(e.getStackTrace()));
                        }

                        System.exit(1);
                    }
                });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GameWindow();
            }
        });
    }
}
