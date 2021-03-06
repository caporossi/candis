package candis.server.gui;

import candis.server.DroidManager;
import candis.server.DroidManagerEvent;
import candis.server.DroidManagerListener;
import java.awt.EventQueue;
import java.awt.Frame;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Enrico Joerns
 */
public class CheckCodeHandler implements DroidManagerListener {

	private Frame mParent;
	private Map<String, CheckCodeShowDialog> mCheckCodeDialogs = new HashMap<String, CheckCodeShowDialog>();

	public CheckCodeHandler(Frame parent) {
		mParent = parent;
	}

	@Override
	public void handle(DroidManagerEvent event, final String droidID, final DroidManager manager) {
		if (event == DroidManagerEvent.CHECK_CODE) {
			System.out.println("Handler called for " + droidID + " with code " + manager.getCheckCode(droidID));
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					mCheckCodeDialogs.put(
									droidID,
									new CheckCodeShowDialog(mParent, true, manager.getCheckCode(droidID), manager.getCheckCode(droidID)));
					mCheckCodeDialogs.get(droidID).setVisible(true);

				}
			});
		}
		else if (event == DroidManagerEvent.CHECK_CODE_DONE) {
			if (mCheckCodeDialogs.containsKey(droidID)) {
				mCheckCodeDialogs.get(droidID).dispose();
			}
		}
	}
}
