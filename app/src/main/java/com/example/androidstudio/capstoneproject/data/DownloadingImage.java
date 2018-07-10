package com.example.androidstudio.capstoneproject.data;

public class DownloadingImage {

    String storageRefString;
    String fileUriString;
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

    public String getFileUriString() {
        return fileUriString;
    }

    public void setFileUriString(String fileUriString) {
        this.fileUriString = fileUriString;
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
                ", fileUriString='" + fileUriString + '\'' +
                ", partId=" + partId +
                ", imageType='" + imageType + '\'' +
                ", lessonId=" + lessonId +
                '}';
    }
}

