/*
 * The MIT License
 *
 * Copyright (c) <2012-2013> <Bruno P. Kinoshita>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.pbs.slaves;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.slaves.NodeProperty;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * PBS slave.
 * @since 0.1
 */
public class PBSSlave extends Slave {

	private static final long serialVersionUID = -6684643958815580629L;
	
	private static final Logger LOGGER = Logger.getLogger(PBSSlave.class.getName());

	/*
	 * Due to warning defined in the inherited types.
	 */
	@SuppressWarnings("rawtypes")
	@DataBoundConstructor
	public PBSSlave(String name, String nodeDescription, String remoteFS, String numExecutors, Mode mode, String labelString, ComputerLauncher launcher, RetentionStrategy retentionStrategy, List<? extends NodeProperty<?>> nodeProperties) throws Descriptor.FormException, IOException {
		super(name, nodeDescription, remoteFS, numExecutors, mode, labelString, launcher, retentionStrategy, nodeProperties);
	}
	
	/* (non-Javadoc)
	 * @see hudson.model.Slave#createComputer()
	 */
	@Override
	public Computer createComputer() {
		LOGGER.info("Creating a new PBS Slave");
		return new PBSSlaveComputer(this);
	}
	
	@Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
        public String getDisplayName() {
            return "PBS Slave";
        }
        
        /* (non-Javadoc)
         * @see hudson.slaves.NodeDescriptor#isInstantiable()
         */
        @Override
        public boolean isInstantiable() {
        	return true;
        }
    }

}
