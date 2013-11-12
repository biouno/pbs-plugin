package jenkins.plugins.pbs.slaves;

import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

public class PBSBuilderDescriptor extends BuildStepDescriptor<Builder> {

	private Integer numberOfDays;
	private Long span;

	public PBSBuilderDescriptor() {
		super();
		load();
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject json)
			throws hudson.model.Descriptor.FormException {
		this.numberOfDays = json.getInt("numberOfDays");
		this.span = json.getLong("span");
		return true;
	}

	@SuppressWarnings("rawtypes")
	// Jenkins API
	@Override
	public boolean isApplicable(Class<? extends AbstractProject> jobType) {
		return true;
	}

	@Override
	public String getDisplayName() {
		return "Submit PBS job";
	}

	public Integer getNumberOfDays() {
		if (this.numberOfDays == null || this.numberOfDays < 0)
			return new Integer(1);
		return this.numberOfDays;
	}

	public Long getSpan() {
		if (this.span == null || this.span < 0)
			return new Long(300);
		return this.span;
	}

}
