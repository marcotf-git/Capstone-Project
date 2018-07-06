package com.example.androidstudio.capstoneproject.data;

public class LessonPart {

    private long part_id;
    private long lesson_id;
    private String title;
    private String text;
    private String local_image_uri;
    private String cloud_image_uri;
    private String local_video_uri;
    private String cloud_video_uri;

    public LessonPart() {
    }

    public LessonPart(long part_id,
                      long lesson_id,
                      String title,
                      String text,
                      String local_image_uri,
                      String cloud_image_uri,
                      String local_video_uri,
                      String cloud_video_uri) {

        this.part_id = part_id;
        this.lesson_id = lesson_id;
        this.title = title;
        this.text = text;
        this.local_image_uri = local_image_uri;
        this.cloud_image_uri = cloud_image_uri;
        this.local_video_uri = local_video_uri;
        this.cloud_video_uri = cloud_video_uri;
    }

    public long getPart_id() {
        return part_id;
    }

    public void setPart_id(long part_id) {
        this.part_id = part_id;
    }

    public long getLesson_id() {
        return lesson_id;
    }

    public void setLesson_id(long lesson_id) {
        this.lesson_id = lesson_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLocal_image_uri() {
        return local_image_uri;
    }

    public void setLocal_image_uri(String local_image_uri) {
        this.local_image_uri = local_image_uri;
    }

    public String getCloud_image_uri() {
        return cloud_image_uri;
    }

    public void setCloud_image_uri(String cloud_image_uri) {
        this.cloud_image_uri = cloud_image_uri;
    }

    public String getLocal_video_uri() {
        return local_video_uri;
    }

    public void setLocal_video_uri(String local_video_uri) {
        this.local_video_uri = local_video_uri;
    }

    public String getCloud_video_uri() {
        return cloud_video_uri;
    }

    public void setCloud_video_uri(String cloud_video_uri) {
        this.cloud_video_uri = cloud_video_uri;
    }

    @Override
    public String toString() {
        return "LessonPart{" +
                "part_id=" + part_id +
                ", lesson_id=" + lesson_id +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", local_image_uri='" + local_image_uri + '\'' +
                ", cloud_image_uri='" + cloud_image_uri + '\'' +
                ", local_video_uri='" + local_video_uri + '\'' +
                ", cloud_video_uri='" + cloud_video_uri + '\'' +
                '}';
    }
}
