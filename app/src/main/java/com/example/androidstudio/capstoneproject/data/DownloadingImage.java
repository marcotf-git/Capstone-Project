package com.example.androidstudio.capstoneproject.data;

public class DownloadingImage {

    String storageRefString;
    Long partId;
    String imageType;
    Long lessonId;

    public DownloadingImage() {
    }

    public String getStorageRefString() {
        return storageRefString;
    }

    public void setStorageRefString(String storageRefString) {
        this.storageRefString = storageRefString;
    }

    public Long getPartId() {
        return partId;
    }

    public void setPartId(Long partId) {
        this.partId = partId;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    @Override
    public String toString() {
        return "DownloadingImage{" +
                "storageRefString='" + storageRefString + '\'' +
                ", partId=" + partId +
                ", imageType='" + imageType + '\'' +
                ", lessonId=" + lessonId +
                '}';
    }
}
