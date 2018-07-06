package com.example.androidstudio.capstoneproject.data;

public class Image {

    private Long part_id;
    private String cloud_uri;

    public Image() {
    }

    public Image(Long part_id, String cloud_uri) {
        this.part_id = part_id;
        this.cloud_uri = cloud_uri;
    }

    public Long getPart_id() {
        return part_id;
    }

    public void setPart_id(Long part_id) {
        this.part_id = part_id;
    }

    public String getCloud_uri() {
        return cloud_uri;
    }

    public void setCloud_uri(String cloud_uri) {
        this.cloud_uri = cloud_uri;
    }

    @Override
    public String toString() {
        return "Image{" +
                "part_id=" + part_id +
                ", cloud_uri='" + cloud_uri + '\'' +
                '}';
    }
}
