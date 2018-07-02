package com.example.androidstudio.capstoneproject.data;

public class Lesson {

    private long lesson_id;
    private String user_uid;
    private String lesson_title;
    private String time_stamp;


    public Lesson() {
    }

    public Lesson(long lesson_id, String user_uid, String lesson_title, String time_stamp) {
        this.lesson_id = lesson_id;
        this.user_uid = user_uid;
        this.lesson_title = lesson_title;
        this.time_stamp = time_stamp;
    }

    public long getLesson_id() {
        return lesson_id;
    }

    public void setLesson_id(long lesson_id) {
        this.lesson_id = lesson_id;
    }

    public String getUser_uid() {
        return user_uid;
    }

    public void setUser_uid(String user_uid) {
        this.user_uid = user_uid;
    }

    public String getLesson_title() {
        return lesson_title;
    }

    public void setLesson_title(String lesson_title) {
        this.lesson_title = lesson_title;
    }

    public String getTime_stamp() {
        return time_stamp;
    }

    public void setTime_stamp(String time_stamp) {
        this.time_stamp = time_stamp;
    }
}


