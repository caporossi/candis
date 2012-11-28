/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed.test;

import candis.common.Instruction;
import candis.common.Message;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;
import candis.distributed.DroidData;
import candis.distributed.droid.StaticProfile;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestDroid extends DroidData implements Runnable {

	private final String mID;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private ObjectOutputStream internalOos;
	private ObjectInputStream internalOis;
	private static final Logger LOGGER = Logger.getLogger(TestDroid.class.getName());
	private final DistributedTask task;

	public String getId() {
		return mID;
	}

	public TestDroid(int id, DistributedTask task) {
		super(false, new StaticProfile());
		LOGGER.log(Level.INFO, String.format("New Droid %d", id));
		this.task = task;
		mID = Integer.toString(id);
		try {
			PipedInputStream incomming = new PipedInputStream();
			PipedInputStream outgoing = new PipedInputStream();
			internalOos = new ObjectOutputStream(new PipedOutputStream(incomming));
			ois = new ObjectInputStream(incomming);
			oos = new ObjectOutputStream(new PipedOutputStream(outgoing));
			internalOis = new ObjectInputStream(outgoing);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

	}


	@Override
	public void run() {
		LOGGER.log(Level.INFO, String.format("TestDroid %s: start", mID));

		try {

			while (true) {
				try {
					Message m_in = (Message) internalOis.readObject();
					LOGGER.log(Level.FINEST, String.format("Droid received message: %s", m_in.getRequest()));
					if (m_in != null) {
						// Handle job
						switch (m_in.getRequest()) {
							case SEND_JOB:
								DistributedParameter parameters = (DistributedParameter) m_in.getData();
								DistributedResult result = runTask(parameters);
								Message m_result = new Message(Instruction.SEND_RESULT, result);
								internalOos.writeObject(m_result);
								break;
						};

					}
				} catch (ClassNotFoundException ex) {
					LOGGER.log(Level.SEVERE, null, ex);
				}
				Thread.sleep(10);
			}


		} catch (InterruptedException iex) {
			LOGGER.log(Level.INFO, String.format("TestDroid %s: interrupted => stop", mID));
		} catch (InterruptedIOException iex) {
			LOGGER.log(Level.INFO, String.format("TestDroid %s: interrupted => stop", mID));
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} finally {
			LOGGER.log(Level.INFO, String.format("TestDroid %s: stop", mID));
		}

	}

	private DistributedResult runTask(DistributedParameter param) {
		return task.run(param);
	}

	public ObjectInputStream getInputStream() {
		return ois;
	}

	public ObjectOutputStream getOutputStream() {
		return oos;
	}
}
