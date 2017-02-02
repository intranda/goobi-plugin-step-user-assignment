package de.intranda.goobi.plugins;


import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Step;
import org.goobi.beans.User;
import org.goobi.beans.Usergroup;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.plugin.interfaces.AbstractStepPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.StepManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import lombok.extern.log4j.Log4j;

@PluginImplementation
@Log4j
public class UserAssignmentPlugin extends AbstractStepPlugin implements IStepPlugin, IPlugin {
	private static final String PLUGIN_NAME = "intranda_step_user_assignment";
	private Step targetStep;
	private List<Usergroup> oldGroups;
	private List<UserWrapper> users;
	private String configAssignmentStepName;
	private String configTargetStepName;
	
	/**
	 * initialise, read config etc.
	 */
	public void initialize(Step step, String returnPath) {
    	// get workflow name from properties
		String workflowName = "";
		for (Processproperty pp : step.getProzess().getEigenschaften()) {
			if (pp.getTitel().equals("Template")){
				workflowName = pp.getWert();
			}
		}
		
    	// get the correct configuration for the right workflow
		HierarchicalConfiguration myconfig = null;
    	List<HierarchicalConfiguration> configs = ConfigPlugins.getPluginConfig(this).configurationsAt("config");
        for (HierarchicalConfiguration hc : configs) {
        	List<HierarchicalConfiguration> workflows = hc.configurationsAt("workflow");
        	configAssignmentStepName = hc.getString("assignmentStep", "- no assignment configured -");
        	for (HierarchicalConfiguration workflow : workflows) {
            	if (myconfig == null || ((workflow.getString("").equals("*") || workflow.getString("").equals(workflowName)) && step.getTitel().equals(configAssignmentStepName))){
            		myconfig = hc;
            	}
            }
        }
    	
        // get right parameters from configuratino
        configAssignmentStepName = myconfig.getString("assignmentStep", "- no assignment configured -");
        configTargetStepName = myconfig.getString("targetStep", "- no assignment configured -");
		super.returnPath = returnPath;
		super.myStep = step;
		loadAllCurrentUsers();
	}

	/**
	 * load all current users and user groups
	 */
	private void loadAllCurrentUsers() {
		oldGroups = new ArrayList<>();
		users = new ArrayList<UserWrapper>();

		// collect all users
		for (Step s : myStep.getProzess().getSchritte()) {
			if (s.getTitel().equals(configTargetStepName)) {
				targetStep = s;
				// get all current usergroups
				for (Usergroup ug : s.getBenutzergruppen()) {
					oldGroups.add(ug);
					for (User u : ug.getBenutzer()) {
						if (!userExistsInList(u))
							users.add(new UserWrapper(u, false));
					}
				}
				// get all current users
				for (User u : s.getBenutzer()) {
					if (!userExistsInList(u))
						users.add(new UserWrapper(u, false));
				}
			}
		}
	}

	/**
	 * check if a user exists in our internal list to avoid duplicates
	 */
	private boolean userExistsInList(User u) {
		for (UserWrapper userWrapper : users) {
			if (userWrapper.getUser().equals(u)) {
				return true;
			}
		}
		return false;
	}


	/**
	 * assign the newly selected users to the target step now
	 */
	public void assignSelectedUsers(){
		// any user selected at all?
		boolean anySelected = false;
		for (UserWrapper userWrapper : users) {
			if (userWrapper.getMember()) {
				anySelected = true;
			}
		}
		// no user selected - stop now
		if (!anySelected){
			Helper.setFehlerMeldung("plugin_user_assignment_noUserForStepSelected");
			return;
		}
		
		// users where selected - assign these now to target step
		for (Usergroup ug : targetStep.getBenutzergruppen()) {
			StepManager.removeUsergroupFromStep(targetStep, ug);
		}
		for (User u : targetStep.getBenutzer()){
			StepManager.removeUserFromStep(targetStep, u);
		}
		targetStep.setBenutzer(new ArrayList<User>());
		targetStep.setBenutzergruppen(new ArrayList<Usergroup>());
		for (UserWrapper userWrapper : users) {
			if (userWrapper.getMember()) {
				targetStep.getBenutzer().add(userWrapper.getUser());
			}
		}
		try {
			StepManager.saveStep(targetStep);
		} catch (DAOException e) {
			Helper.setFehlerMeldung("Error while saving the target step", e);
			log.error("DAOException while saving the target steps", e);
		}
		
		// load again all new assigned user and show success message
		loadAllCurrentUsers();
		Helper.setMeldung("plugin_user_assignment_UserAssignmentSuccessfullyFinished");
		
	}

	/**
	 * Getter and setter
	 */
	public List<Usergroup> getOldGroups() {
		return oldGroups;
	}

	public List<UserWrapper> getUsers() {
		return users;
	}
	
	public void toggleUser(UserWrapper uw){
		uw.setMember(!uw.getMember());
	}

	@Override public boolean execute() {
		return false;
	}

	@Override
	public PluginGuiType getPluginGuiType() {
		return PluginGuiType.PART;
	}

	@Override
	public String getPagePath() {
		return null;
	}

	@Override
	public String getTitle() {
		return PLUGIN_NAME;
	}

	@Override
	public String getDescription() {
		return PLUGIN_NAME;
	}

	public String getConfigTargetStepName() {
		return configTargetStepName;
	}
}
