package io.cloudbeat.common.reporter.model;

import java.util.List;

public interface IResultWithAttachment {
    void addAttachment(Attachment attachment);
    List<Attachment> getAttachments();
}
