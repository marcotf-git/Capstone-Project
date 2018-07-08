package com.example.androidstudio.capstoneproject.data;

public class Image {

    private Long part_id;
    private String local_uri;
    private String imageType;

     public Image() {
    }

    public Image(Long part_id, String local_uri, String imageType) {
        this.part_id = part_id;
        this.local_uri = local_uri;
        this.imageType = imageType;
    }

    public Long getPart_id() {
        return part_id;
    }

    public void setPart_id(Long part_id) {
        this.part_id = part_id;
    }

    public String getLocal_uri() {
        return local_uri;
    }

    public void setLocal_uri(String local_uri) {
        this.local_uri = local_uri;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    @Override
    public String toString() {
        return "Image{" +
                "part_id=" + part_id +
                ", local_uri='" + local_uri + '\'' +
                ", imageType='" + imageType + '\'' +
                '}';
    }
}
