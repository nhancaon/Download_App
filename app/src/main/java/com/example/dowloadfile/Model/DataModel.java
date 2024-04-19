package com.example.dowloadfile.Model;

public class DataModel {
    private String imageURL, caption;

    public DataModel(){

    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public DataModel(String imageURL, String caption) {
        this.imageURL = imageURL;
        this.caption = caption;
    }
}