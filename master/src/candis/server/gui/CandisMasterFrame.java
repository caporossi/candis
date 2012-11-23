/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.server.gui;

import candis.server.DroidManager;
import candis.server.Server;
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Enrico Joerns
 */
public class CandisMasterFrame extends javax.swing.JFrame {

	private DroidlistTableModel mDroidlistTableModel;
	private DroidInfoTableModel mDroidInfoTableModel;
	private DroidManager mDroidManager;

	/**
	 * Creates new form CandisMasterFrame
	 */
	public CandisMasterFrame(DroidManager droidmanager, DroidlistTableModel droidlisttablemodel) {
		mDroidManager = droidmanager;
		mDroidlistTableModel = droidlisttablemodel;
		mDroidInfoTableModel = new DroidInfoTableModel();
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

    jButton3 = new javax.swing.JButton();
    jScrollPane2 = new javax.swing.JScrollPane();
    jTextArea1 = new javax.swing.JTextArea();
    jPanel1 = new javax.swing.JPanel();
    mBlacklistButton = new javax.swing.JToggleButton();
    mExecuteButton = new javax.swing.JButton();
    mDroidlistScrollPane = new javax.swing.JScrollPane();
    mDroidlistTable = new javax.swing.JTable();
    jButton2 = new javax.swing.JButton();
    mUploadButton = new javax.swing.JButton();
    mStopButton = new javax.swing.JButton();
    jScrollPane3 = new javax.swing.JScrollPane();
    mDroidInfoTable = new javax.swing.JTable();

    jButton3.setText("jButton3");

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    getContentPane().setLayout(new java.awt.GridBagLayout());

    jTextArea1.setColumns(20);
    jTextArea1.setRows(5);
    jScrollPane2.setViewportView(jTextArea1);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 3;
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
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    getContentPane().add(jPanel1, gridBagConstraints);

    mBlacklistButton.setText("Blacklist");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
    getContentPane().add(mBlacklistButton, gridBagConstraints);

    mExecuteButton.setText("Execute");
    mExecuteButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        mExecuteButtonActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
    getContentPane().add(mExecuteButton, gridBagConstraints);

    mDroidlistScrollPane.setPreferredSize(new java.awt.Dimension(453, 255));

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
    gridBagConstraints.gridheight = 2;
    getContentPane().add(mDroidlistScrollPane, gridBagConstraints);

    jButton2.setText("jButton2");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 5);
    getContentPane().add(jButton2, gridBagConstraints);

    mUploadButton.setText("Upload");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weighty = 0.1;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
    getContentPane().add(mUploadButton, gridBagConstraints);

    mStopButton.setText("Stop");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
    getContentPane().add(mStopButton, gridBagConstraints);

    jScrollPane3.setMaximumSize(new java.awt.Dimension(32767, 1024));
    jScrollPane3.setPreferredSize(new java.awt.Dimension(375, 175));

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
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridheight = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    getContentPane().add(jScrollPane3, gridBagConstraints);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void mExecuteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mExecuteButtonActionPerformed
		// TODO add your handling code here:
  }//GEN-LAST:event_mExecuteButtonActionPerformed

  private void mDroidInfoTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mDroidInfoTableMouseClicked
		// TODO add your handling code here:
		System.out.println("I WAS CLICKED!");
		Logger.getLogger("Foo").log(Level.WARNING, "I WAS CLICKED!");
  }//GEN-LAST:event_mDroidInfoTableMouseClicked

  private void mDroidlistTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mDroidlistTableMouseClicked
		// TODO add your handling code here:
		System.out.println("YEAH, I WAS CLICKED!");
		Logger.getLogger("Foo").log(Level.WARNING, "YEAH, I WAS CLICKED!");
		String id = (String) mDroidlistTableModel.getValueAt(mDroidlistTable.getSelectedRow(), 3);
		mDroidInfoTableModel.update(mDroidManager, id);
  }//GEN-LAST:event_mDroidlistTableMouseClicked

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

		DroidManager droidmanager = DroidManager.getInstance();
		DroidlistTableModel dltm = new DroidlistTableModel();
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
  private javax.swing.JButton jButton2;
  private javax.swing.JButton jButton3;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane2;
  private javax.swing.JScrollPane jScrollPane3;
  private javax.swing.JTextArea jTextArea1;
  private javax.swing.JToggleButton mBlacklistButton;
  private javax.swing.JTable mDroidInfoTable;
  private javax.swing.JScrollPane mDroidlistScrollPane;
  private javax.swing.JTable mDroidlistTable;
  private javax.swing.JButton mExecuteButton;
  private javax.swing.JButton mStopButton;
  private javax.swing.JButton mUploadButton;
  // End of variables declaration//GEN-END:variables
}