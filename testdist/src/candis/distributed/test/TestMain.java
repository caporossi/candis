package candis.distributed.test;

import candis.distributed.SchedulerStillRuningException;
import candis.server.DroidManager;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestMain{

	private static final Logger LOGGER = Logger.getLogger(TestMain.class.getName());


	public static void runCDBTest(String cdb, int threads) throws Exception {
		LOGGER.log(Level.INFO, "CDB file {0}", cdb);
		JobDistributionIOTestServer comio = new JobDistributionIOTestServer(DroidManager.getInstance());
		comio.loadCDB(new File(cdb));
		comio.initDroids(threads);

		try {
			comio.initScheduler();
			comio.startScheduler();
		}
		catch (SchedulerStillRuningException ex) {
			Logger.getLogger(TestMain.class.getName()).log(Level.SEVERE, null, ex);
		}


		try {

			comio.join();
		}
		catch (InterruptedException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		comio.stopDroids();

	}
	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		Options opts = new Options();

		opts.addOption("h", "help", false, "Show this help");
		opts.addOption(OptionBuilder.withLongOpt("threads")
						.withDescription("da")
						.withType(Number.class)
						.hasArg()
						.withArgName("THREADS")
						.create("t"));

		CommandLineParser parser = new PosixParser();
		boolean showHelp = false;
		try {
			CommandLine cmd = parser.parse(opts, args);

			if(cmd.hasOption("h"))
			{
				showHelp = true;
			}
			else
			{
				int threads = 4;
				if (cmd.hasOption("t")) {
					threads = ((Number) cmd.getParsedOptionValue("t")).intValue();
				}
				if(cmd.getArgs().length == 1)
				{
					String cdb = cmd.getArgs()[0];
					runCDBTest(cdb, threads);
				}
				else
				{
					showHelp = true;
				}
			}
		}
		catch (ParseException ex) {
			System.out.println(ex.getMessage());
			showHelp = true;
		}
		catch (Exception ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		if(showHelp) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("testdist [options] CDB", opts);
		}
	}
}
