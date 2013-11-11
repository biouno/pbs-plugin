package jenkins.plugins.pbs.slaves;

import hudson.Extension;
import hudson.model.RootAction;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.tupilabs.pbs.PBS;
import com.tupilabs.pbs.util.CommandOutput;

@Extension
public class DisplayPBSJobAction implements RootAction {

	private final int numberOfDays;

	public DisplayPBSJobAction(int numberOfDays) {
		this.numberOfDays = numberOfDays;
	}
	
	public String getIconFileName() {
		return null;
	}

	public String getDisplayName() {
		return "/pbsJob";
	}

	public String getUrlName() {
		return "/pbsJob";
	}
	
	public Object doDynamic(StaplerRequest request, StaplerResponse response) {
		// TODO: get logger
		String restOfPath = request.getRestOfPath();
		if (restOfPath != null && restOfPath.length() > 1) {
			if (restOfPath.startsWith("/"))
				restOfPath = restOfPath.substring(1, restOfPath.length());
			final String jobId = restOfPath;
			CommandOutput commandOutput = PBS.traceJob(jobId, numberOfDays);
			return new PBSJob(commandOutput.getOutput(), commandOutput.getError());
		}
		return null; // TODO: redirect to job page?
	}
	
}
