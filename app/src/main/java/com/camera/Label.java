package com.camera;


public class Label {
    private String name;

    public Label(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Label{" +
                "name='" + name + '\'' +
                '}';
    }
}
