package de.intranda.goobi.plugins;

import org.goobi.beans.User;

public class UserWrapper {
    private User user;
    private boolean member;

    public UserWrapper(User inUser, boolean inMember) {
        user = inUser;
        member = inMember;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean getMember() {
        return member;
    }

    public void setMember(boolean member) {
        this.member = member;
    }
}
