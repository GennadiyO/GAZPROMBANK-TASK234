package com.gazprombank.task4;

public class AddValute {
    private String vsValute;
    private String uprValute;
    private String lwrValute;

    public AddValute(String vsValute){
        this.vsValute = vsValute;
        setValutes();
    }
    public void setValutes(){
        String[] words = vsValute.split("/");
        uprValute = words[0];
        lwrValute = words[1];
    }

    public String getUprValute(){
        return uprValute;
    }
    public String getLwrValute(){
        return lwrValute;
    }

}
