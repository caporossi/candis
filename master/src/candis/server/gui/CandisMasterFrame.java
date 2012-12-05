package candis.server.gui;

import candis.common.Settings;
import candis.server.DroidManager;
import candis.server.Server;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Enrico Joerns
 */
public class CandisMasterFrame extends javax.swing.JFrame {

	private DroidlistTableModel mDroidlistTableModel;
	private DroidInfoTableModel mDroidInfoTableModel;
	private DroidManager mDroidManager;
	private OptionsDialog mOptionDialog;
	private CheckCodeShowDialog mCheckCodeShowDialog;
	private CandisLoggerHandler mLoggerHandler;

	/**
	 * Creates new form CandisMasterFrame.
	 */
	public CandisMasterFrame(DroidManager droidmanager, DroidlistTableModel droidlisttablemodel) {
		mDroidManager = droidmanager;
		mDroidlistTableModel = droidlisttablemodel;
		mDroidInfoTableModel = new DroidInfoTableModel();
		mOptionDialog = new OptionsDialog(this, false);
		mCheckCodeShowDialog = new CheckCodeShowDialog(this, false);
		mDroidManager.addListener(mCheckCodeShowDialog);
//		initComponents();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    jScrollPane2 = new javax.swing.JScrollPane();
    mLogTextArea = new javax.swing.JTextArea();
    jPanel1 = new javax.swing.JPanel();
    mBlacklistButton = new javax.swing.JToggleButton();
    mExecuteButton = new javax.swing.JButton();
    mDroidlistScrollPane = new javax.swing.JScrollPane();
    mDroidlistTable = new javax.swing.JTable();
    mOptionButton = new javax.swing.JButton();
    mUploadButton = new javax.swing.JButton();
    mStopButton = new javax.swing.JButton();
    jScrollPane3 = new javax.swing.JScrollPane();
    mDroidInfoTable = new javax.swing.JTable();
    mDeleteButton = new javax.swing.JButton();
    jButton1 = new javax.swing.JButton();
    jLabel1 = new javax.swing.JLabel();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    setTitle("Candis Master");
    getContentPane().setLayout(new java.awt.GridBagLayout());

    jScrollPane2.setPreferredSize(new java.awt.Dimension(440, 150));

    mLogTextArea.setEditable(false);
    mLogTextArea.setColumns(20);
    mLogTextArea.setRows(5);
    mLogTextArea.setPreferredSize(new java.awt.Dimension(435, 75));
    jScrollPane2.setViewportView(mLogTextArea);
    mLoggerHandler = new CandisLoggerHandler(mLogTextArea);
    mDroidManager.addLoggerHandler(mLoggerHandler);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
    getContentPane().add(jScrollPane2, gridBagConstraints);

    javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
    jPanel1.setLayout(jPanel1Layout);
    jPanel1Layout.setHorizontalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 0, Short.MAX_VALUE)
    );
    jPanel1Layout.setVerticalGroup(
      jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 0, Short.MAX_VALUE)
    );

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    getContentPane().add(jPanel1, gridBagConstraints);

    mBlacklistButton.setText("Blacklist");
    mBlacklistButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        mBlacklistButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
    getContentPane().add(mBlacklistButton, gridBagConstraints);

    mExecuteButton.setText("Execute");
    mExecuteButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        mExecuteButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
    getContentPane().add(mExecuteButton, gridBagConstraints);

    mDroidlistScrollPane.setPreferredSize(new java.awt.Dimension(453, 200));

    mDroidlistTable.setModel(mDroidlistTableModel);
    mDroidlistTable.getColumnModel().getColumn(0).setMinWidth(25);
    mDroidlistTable.getColumnModel().getColumn(0).setMaxWidth(25);
    mDroidlistTable.setFillsViewportHeight(true);
    mDroidlistTable.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        mDroidlistTableMouseClicked(evt);
      }
    });
    mDroidlistScrollPane.setViewportView(mDroidlistTable);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.gridheight = 2;
    getContentPane().add(mDroidlistScrollPane, gridBagConstraints);

    mOptionButton.setText("Optionen");
    mOptionButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        mOptionButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
    getContentPane().add(mOptionButton, gridBagConstraints);

    mUploadButton.setText("Open");
    mUploadButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        mUploadButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weighty = 0.1;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
    getContentPane().add(mUploadButton, gridBagConstraints);

    mStopButton.setText("Stop");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
    getContentPane().add(mStopButton, gridBagConstraints);

    jScrollPane3.setMaximumSize(new java.awt.Dimension(32767, 1024));
    jScrollPane3.setPreferredSize(new java.awt.Dimension(300, 110));

    mDroidInfoTable.setModel(mDroidInfoTableModel);
    mDroidInfoTable.getColumnModel().getColumn(0).setPreferredWidth(100);
    mDroidInfoTable.getColumnModel().getColumn(0).setMaxWidth(150);
    mDroidInfoTable.revalidate();
    mDroidInfoTable.setRequestFocusEnabled(false);
    mDroidInfoTable.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        mDroidInfoTableMouseClicked(evt);
      }
    });
    jScrollPane3.setViewportView(mDroidInfoTable);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.gridheight = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    getContentPane().add(jScrollPane3, gridBagConstraints);

    mDeleteButton.setText("Delete");
    mDeleteButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        mDeleteButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    getContentPane().add(mDeleteButton, gridBagConstraints);

    jButton1.setText("Disconnect");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    getContentPane().add(jButton1, gridBagConstraints);

    jLabel1.setText("Server Log:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    getContentPane().add(jLabel1, gridBagConstraints);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void mExecuteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mExecuteButtonActionPerformed
		// TODO add your handling code here:
  }//GEN-LAST:event_mExecuteButtonActionPerformed

  private void mDroidInfoTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mDroidInfoTableMouseClicked
		// TODO add your handling code here:
  }//GEN-LAST:event_mDroidInfoTableMouseClicked

  private void mDroidlistTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mDroidlistTableMouseClicked
		// TODO add your handling code here:
		String id = (String) mDroidlistTableModel.getValueAt(mDroidlistTable.getSelectedRow(), 3);
		mDroidInfoTableModel.update(mDroidManager, id);
		if (mDroidManager.getKnownDroids().containsKey(id)) {
			mBlacklistButton.getModel().setSelected(
							mDroidManager.getKnownDroids().get(id).getBlacklist());
		}
  }//GEN-LAST:event_mDroidlistTableMouseClicked

  private void mOptionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mOptionButtonActionPerformed
		// TODO add your handling code here:
		mOptionDialog.updateOptions();
		mOptionDialog.setVisible(true);
  }//GEN-LAST:event_mOptionButtonActionPerformed

  private void mUploadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mUploadButtonActionPerformed
		// TODO add your handling code here:
		JFileChooser fileChooser = new JFileChooser();
		FileFilter filter = new ExtensionFilter(".jar file", ".jar");
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setFileFilter(filter);
		fileChooser.showOpenDialog(this);
  }//GEN-LAST:event_mUploadButtonActionPerformed

  private void mBlacklistButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mBlacklistButtonActionPerformed
		// TODO add your handling code here:
		String id = (String) mDroidlistTableModel.getValueAt(mDroidlistTable.getSelectedRow(), 3);
		if (mBlacklistButton.getModel().isSelected()) {
			mDroidManager.blacklistDroid(id);
		} else {
			mDroidManager.whitelistDroid(id);
		}
		new SwingWorker<Void, Object>() {
			@Override
			protected Void doInBackground() throws Exception {
				try {
					Logger.getLogger("CMF").log(Level.INFO, "Saving database...");
					mDroidManager.store(new File(Settings.getString("droiddb.file")));
				} catch (FileNotFoundException ex) {
					Logger.getLogger(CandisMasterFrame.class.getName()).log(Level.SEVERE, null, ex);
				}
				return null;
			}
		}.execute();
  }//GEN-LAST:event_mBlacklistButtonActionPerformed

  private void mDeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mDeleteButtonActionPerformed
		// TODO add your handling code here:
		String id = (String) mDroidlistTableModel.getValueAt(mDroidlistTable.getSelectedRow(), 3);
		mDroidManager.deleteDroid(id);
		new SwingWorker<Void, Object>() {
			@Override
			protected Void doInBackground() throws Exception {
				try {
					Logger.getLogger("CMF").log(Level.INFO, "Saving database...");
					mDroidManager.store(new File(Settings.getString("droiddb.file")));
				} catch (FileNotFoundException ex) {
					Logger.getLogger(CandisMasterFrame.class.getName()).log(Level.SEVERE, null, ex);
				}
				return null;
			}
		}.execute();
  }//GEN-LAST:event_mDeleteButtonActionPerformed

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
		 * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(CandisMasterFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(CandisMasterFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(CandisMasterFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(CandisMasterFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		//</editor-fold>

		final DroidManager droidmanager = DroidManager.getInstance();
		final DroidlistTableModel dltm = new DroidlistTableModel();
		final CandisMasterFrame cmf = new CandisMasterFrame(droidmanager, dltm);
		/* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				cmf.initComponents();
				cmf.setVisible(true);
			}
		});
		droidmanager.addListener(dltm);
		new Server(droidmanager);

	}
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton jButton1;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JScrollPane jScrollPane3;
  private javax.swing.JToggleButton mBlacklistButton;
  private javax.swing.JButton mDeleteButton;
  private javax.swing.JTable mDroidInfoTable;
  private javax.swing.JScrollPane mDroidlistScrollPane;
  private javax.swing.JTable mDroidlistTable;
  private javax.swing.JButton mExecuteButton;
  private javax.swing.JTextArea mLogTextArea;
  private javax.swing.JButton mOptionButton;
  private javax.swing.JButton mStopButton;
  private javax.swing.JButton mUploadButton;
  // End of variables declaration//GEN-END:variables
}
