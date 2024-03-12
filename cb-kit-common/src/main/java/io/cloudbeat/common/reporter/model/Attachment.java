package io.cloudbeat.common.reporter.model;

import java.util.Calendar;
import java.util.UUID;

public class Attachment {
    String id;
    String mimeType;
    String fileName;
    String filePath;
    AttachmentType type;
    AttachmentSubType subtype;

    public Attachment(AttachmentType type) {
        this(type, null);
    }
    public Attachment(AttachmentType type, AttachmentSubType subtype) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.subtype = subtype;
    }

    public String getId() {
        return id;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public AttachmentType getType() {
        return type;
    }

    public AttachmentSubType getSubtype() {
        return subtype;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setType(AttachmentType type) {
        this.type = type;
    }

    public void setSubtype(AttachmentSubType subtype) {
        this.subtype = subtype;
    }
}
