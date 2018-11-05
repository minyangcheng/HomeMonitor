package com.min.home.monitor.bean;

import java.io.Serializable;

/**
 * Created by minych on 18-11-3.
 */

public class UserBean implements Serializable {

    public String name;
    public String time;

    public UserBean() {
    }

    public UserBean(String name, String time) {
        this.name = name;
        this.time = time;
    }

}
