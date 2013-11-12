package jenkins.plugins.pbs.slaves;

import hudson.model.ModelObject;

public class PBSJob implements ModelObject {

	private final String jobId;
	private final String out;
	private final String err;

	public PBSJob(String jobId, String out, String err) {
		super();
		this.jobId = jobId;
		this.out = out;
		this.err = err;
	}
	
	public String getJobId() {
		return jobId;
	}

	public String getOut() {
		return out;
	}

	public String getErr() {
		return err;
	}

	public String getDisplayName() {
		return "PBS Job " + jobId;
	}

}
