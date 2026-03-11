package com.newproject.notification.dto;

public class SmtpRuntimeSettings {
    private Boolean smtpEnabled;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private Boolean smtpAuth;
    private Boolean smtpStarttls;
    private String mailFromEmail;
    private String mailFromName;

    public Boolean getSmtpEnabled() {
        return smtpEnabled;
    }

    public void setSmtpEnabled(Boolean smtpEnabled) {
        this.smtpEnabled = smtpEnabled;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public Boolean getSmtpAuth() {
        return smtpAuth;
    }

    public void setSmtpAuth(Boolean smtpAuth) {
        this.smtpAuth = smtpAuth;
    }

    public Boolean getSmtpStarttls() {
        return smtpStarttls;
    }

    public void setSmtpStarttls(Boolean smtpStarttls) {
        this.smtpStarttls = smtpStarttls;
    }

    public String getMailFromEmail() {
        return mailFromEmail;
    }

    public void setMailFromEmail(String mailFromEmail) {
        this.mailFromEmail = mailFromEmail;
    }

    public String getMailFromName() {
        return mailFromName;
    }

    public void setMailFromName(String mailFromName) {
        this.mailFromName = mailFromName;
    }
}
