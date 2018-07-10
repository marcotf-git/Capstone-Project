package com.example.androidstudio.capstoneproject.data;

public class Image {

    private Long part_id;
    private Long lesson_id;
    private String local_uri;
    private String cloud_uri;
    private String imageType;

    public Image() {
    }

    public Long getPart_id() {
        return part_id;
    }

    public void setPart_id(Long part_id) {
        this.part_id = part_id;
    }

    public Long getLesson_id() {
        return lesson_id;
    }

    public void setLesson_id(Long lesson_id) {
        this.lesson_id = lesson_id;
    }

    public String getLocal_uri() {
        return local_uri;
    }

    public void setLocal_uri(String local_uri) {
        this.local_uri = local_uri;
    }

    public String getCloud_uri() {
        return cloud_uri;
    }

    public void setCloud_uri(String cloud_uri) {
        this.cloud_uri = cloud_uri;
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
                ", lesson_id=" + lesson_id +
                ", local_uri='" + local_uri + '\'' +
                ", cloud_uri='" + cloud_uri + '\'' +
                ", imageType='" + imageType + '\'' +
                '}';
    }
}
