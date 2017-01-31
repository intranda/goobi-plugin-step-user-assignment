package de.intranda.goobi.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.goobi.beans.Step;
import org.goobi.beans.User;
import org.goobi.beans.Usergroup;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.plugin.interfaces.AbstractStepPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;
import org.primefaces.event.FileUploadEvent;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.NIOFileUtils;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.samples.BenutzergruppenTest;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import lombok.extern.log4j.Log4j;

@PluginImplementation
@Log4j
public class UserAssignmentPlugin extends AbstractStepPlugin implements IStepPlugin, IPlugin {

	private static final String PLUGIN_NAME = "intranda_step_user_assignment";
	private Step targetStep;
	private List<Usergroup> oldGroups;
	private List<UserWrapper> users;

	public void initialize(Step step, String returnPath) {
		super.returnPath = returnPath;
		super.myStep = step;
		oldGroups = new ArrayList<>();
		users = new ArrayList<UserWrapper>();

		// find the right step
		String targetStepTitle = "Scannen";
		// collect all users
		for (Step s : myStep.getProzess().getSchritte()) {
			if (s.getTitel().equals(targetStepTitle)) {
				targetStep = s;
				// get all current usergroups
				for (Usergroup ug : s.getBenutzergruppen()) {
					oldGroups.add(ug);
					for (User u : ug.getBenutzer()) {
						if (!userExistsInList(u))
							users.add(new UserWrapper(u, false));
					}
				}
			}
			// get all current users
			for (User u : s.getBenutzer()) {
				if (!userExistsInList(u))
					users.add(new UserWrapper(u, false));
			}
		}
	}

	private boolean userExistsInList(User u) {
		for (UserWrapper userWrapper : users) {
			if (userWrapper.getUser().equals(u)) {
				return true;
			}
		}
		return false;
	}

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

}
