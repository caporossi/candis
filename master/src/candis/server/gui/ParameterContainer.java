package candis.server.gui;

import candis.distributed.parameter.UserParameter;
import candis.distributed.parameter.UserParameterCtrl;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class ParameterContainer {

	public final UserParameter mUserParameter;
	public JComponent mJComponent;
	public final JLabel mNameLabel;
	public final JLabel mDescriptionLabel;
	public final JLabel mErrorLabel;

	protected ParameterContainer(UserParameter mUserParameter) {
		this.mUserParameter = mUserParameter;
		this.mNameLabel = new JLabel(mUserParameter.getTitle());
		this.mDescriptionLabel = new JLabel(mUserParameter.getDescription());
		this.mErrorLabel = new JLabel(" ");
	}

	public abstract Object getValue();

	public void validate() {
		mUserParameter.SetData(this.getValue());
		mUserParameter.validate();
		mErrorLabel.setText(mUserParameter.getValidatorMessage() + " ");

	}

	public static ParameterContainer getContainer(UserParameter param) {
		UserParameterCtrl ctrl = param.getInputCtrl();

		switch (ctrl.getInputTupe()) {
			case FILE:
				break;
			case FLOAT:
				break;
			case INTEGER:
				break;
			case STRING:
				return new StringParameterContainer(param);
			case STRING_LIST:
				return new StringListParameterContainer(param);
		}


		return null;
	}

	public void addToDialog(JPanel panel, int position) {
		GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = position * 2;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		panel.add(mNameLabel, gridBagConstraints);

		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = position * 2;
		gridBagConstraints.weightx = 1;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		panel.add(mJComponent, gridBagConstraints);

		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = position * 2;
		gridBagConstraints.weightx = 0;
		gridBagConstraints.gridheight = 2;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		panel.add(mDescriptionLabel, gridBagConstraints);

		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = position * 2 + 1;
		gridBagConstraints.weightx = 0;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.gridheight = 1;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		panel.add(mErrorLabel, gridBagConstraints);
		mErrorLabel.setForeground(Color.red);
	}

	public static class StringListParameterContainer extends ParameterContainer {

		public StringListParameterContainer(UserParameter userParameter) {
			super(userParameter);
			JComboBox box = new JComboBox();
			box.setModel(new javax.swing.DefaultComboBoxModel(userParameter.getInputCtrl().getListElements()));
			box.setSelectedItem(userParameter.getData());
			box.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent ie) {
					mUserParameter.SetData(ie.getItem());
					validate();
				}
			});
			mJComponent = box;

		}

		@Override
		public Object getValue() {
			return ((JComboBox) mJComponent).getSelectedItem();
		}
	}

	public static class StringParameterContainer extends ParameterContainer {

		public StringParameterContainer(UserParameter userParameter) {
			super(userParameter);
			JTextField text = new JTextField(userParameter.getData().toString());
			mJComponent = text;
			Dimension d = mJComponent.getMinimumSize();
			text.addVetoableChangeListener(null);
			text.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent de) {
					validate();
				}

				@Override
				public void removeUpdate(DocumentEvent de) {
					validate();
				}

				@Override
				public void changedUpdate(DocumentEvent de) {
					validate();
				}
			});
			d.width = 300;
			mJComponent.setPreferredSize(d);

		}

		@Override
		public Object getValue() {
			return ((JTextField) mJComponent).getText();
		}
	}
}
