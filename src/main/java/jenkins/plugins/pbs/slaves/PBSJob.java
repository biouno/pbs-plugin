package jenkins.plugins.pbs.slaves;

public class PBSJob {

	private final String out;
	private final String err;

	public PBSJob(String out, String err) {
		super();
		this.out = out;
		this.err = err;
	}

	public String getOut() {
		return out;
	}

	public String getErr() {
		return err;
	}

}
