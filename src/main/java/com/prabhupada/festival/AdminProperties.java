package com.prabhupada.festival;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "festival.admin")
public class AdminProperties {

    /**
     * Key that unlocks the admin page. Blank switches the admin API off entirely,
     * so an unset key can never be matched by an empty request.
     */
    private String key = "";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return !key.isBlank();
    }
}
