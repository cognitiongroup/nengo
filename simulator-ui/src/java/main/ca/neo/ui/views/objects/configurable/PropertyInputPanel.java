package ca.neo.ui.views.objects.configurable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ca.neo.ui.style.Style;
import ca.neo.ui.views.objects.configurable.struct.PropertyStructure;

public abstract class PropertyInputPanel extends JPanel {
	PropertyStructure type;

	JLabel statusMessage;

	public PropertyInputPanel(PropertyStructure property) {
		super();
		this.type = property;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setAlignmentY(TOP_ALIGNMENT);

		setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

		add(property.getLabel());

		JPanel innerPanel = new JPanel();
		innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));
		innerPanel.setAlignmentX(LEFT_ALIGNMENT);

		add(innerPanel);
		init(innerPanel);

		statusMessage = new JLabel("");
		statusMessage.setForeground(Style.WARNING_COLOR);

		add(statusMessage);
	}

	public String getName() {
		return type.getName();
	}

	protected void setStatusMsg(String msg) {
		statusMessage.setText(msg);
	}

	public abstract boolean isValueSet();

	public abstract void setValue(Object value);

	public abstract Object getValue();

	public abstract void init(JPanel panel);

	public PropertyStructure getType() {
		return type;
	}

	public void setType(PropertyStructure type) {
		this.type = type;
	}

}
