package com.example.androidstudio.capstoneproject.data;

public class LessonPart {

    private long part_id;
    private long lesson_id;
    private String title;

    public LessonPart() {
    }

    public LessonPart(long part_id, long lesson_id, String title, String user_uid) {
        this.part_id = part_id;
        this.lesson_id = lesson_id;
        this.title = title;
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

    @Override
    public String toString() {
        return "LessonPart{" +
                "part_id=" + part_id +
                ", lesson_id=" + lesson_id +
                ", title='" + title + '\'' +
                '}';
    }
}
