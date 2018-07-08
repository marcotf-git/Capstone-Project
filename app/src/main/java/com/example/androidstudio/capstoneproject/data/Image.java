package com.example.androidstudio.capstoneproject.data;

public class Image {

    private Long part_id;
    private String local_uri;

    public Image() {
    }

    public Image(Long part_id, String local_uri) {
        this.part_id = part_id;
        this.local_uri = local_uri;
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

    @Override
    public String toString() {
        return "Image{" +
                "part_id=" + part_id +
                ", local_uri='" + local_uri + '\'' +
                '}';
    }
}
