package ca.nengo.ui.actions;

import org.python.util.PythonInterpreter;

import ca.nengo.ui.NengoGraphics;
import ca.nengo.ui.models.nodes.UINetwork;
import ca.shu.ui.lib.actions.ActionException;
import ca.shu.ui.lib.actions.StandardAction;

public class RunInteractivePlotstAction extends StandardAction {
	private static final long serialVersionUID = 1L;
	private UINetwork uiNetwork;

	public RunInteractivePlotstAction(UINetwork uiNetwork) {
		super("Run interactive plots","Interactive Plots");
		this.uiNetwork = uiNetwork;
	}

	protected void action() throws ActionException {
		PythonInterpreter pi = NengoGraphics.getInstance().getPythonInterpreter();
		pi.set("_interactive_network", uiNetwork);
		pi.exec("import timeview");
		pi.exec("reload(timeview)");
		pi.exec("timeview.View(_interactive_network.model,ui=_interactive_network.viewerEnsured)");
		pi.exec("del _interactive_network");
	}
}