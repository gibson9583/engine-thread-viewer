package com.mirth.connect.plugins.threadviewer.shared;

import java.io.Serializable;

/** Stable channel option shared by both administrators. */
public class ChannelInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String savedName;
    private boolean deployed;

    public ChannelInfo() {}

    public ChannelInfo(String id, String name, String savedName, boolean deployed) {
        this.id = id;
        this.name = name;
        this.savedName = savedName;
        this.deployed = deployed;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSavedName() { return savedName; }
    public void setSavedName(String savedName) { this.savedName = savedName; }
    public boolean isDeployed() { return deployed; }
    public void setDeployed(boolean deployed) { this.deployed = deployed; }
}
